package com.xxedu.learning.modules.file;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.file.support.ImageUploadValidator;
import com.xxedu.learning.modules.file.service.FileService;
import com.xxedu.learning.modules.file.vo.FileUploadVO;
import com.xxedu.learning.security.JwtTokenService;
import com.xxedu.learning.security.LoginUser;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.storage.LocalStorageService;
import com.xxedu.learning.storage.StorageService;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileUploadApiTest extends IntegrationTestSupport {

    @Autowired
    private FileService fileService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void uploadsAllowedImageFormats() throws Exception {
        TestAuth.loginOperator();
        assertUploadOk(png("a.png"), "image/png");
        assertUploadOk(jpeg("b.jpg"), "image/jpeg");
        assertUploadOk(gif("c.gif"), "image/gif");
        assertUploadOk(webp("d.webp"), "image/webp");
    }

    @Test
    void rejectsIllegalAndDisguisedFiles() throws Exception {
        TestAuth.loginOperator();
        assertRejected(text("notes.txt", "hello"), "仅支持 JPEG、PNG、WEBP、GIF 图片");
        assertRejected(text("page.html", "<html></html>"), "仅支持 JPEG、PNG、WEBP、GIF 图片");
        assertRejected(text("app.js", "alert(1)"), "仅支持 JPEG、PNG、WEBP、GIF 图片");
        assertRejected(text("run.exe", "MZ"), "仅支持 JPEG、PNG、WEBP、GIF 图片");
        assertRejected(text("pack.zip", "PK"), "仅支持 JPEG、PNG、WEBP、GIF 图片");
        assertRejected(text("doc.pdf", "%PDF"), "仅支持 JPEG、PNG、WEBP、GIF 图片");
        assertRejected(new MockMultipartFile("file", "evil.png", "image/png",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)), "文件内容不是允许的图片格式");
        assertRejected(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]), "文件不能为空");
        assertRejected(oversizedPng(), "文件大小不能超过10MB");
    }

    @Test
    void pathTraversalNameStillStoresUnderUploads() throws Exception {
        TestAuth.loginOperator();
        FileUploadVO uploaded = fileService.uploadImage(png("../../escape.png"), null);
        assertThat(uploaded.getObjectKey()).doesNotContain("..");
        assertThat(uploaded.getUrl()).startsWith("/uploads/");
        if (storageService instanceof LocalStorageService local) {
            Path stored = local.resolveUnderRoot(uploaded.getObjectKey());
            assertThat(stored.startsWith(local.rootPath())).isTrue();
            assertThat(Files.exists(stored)).isTrue();
        }
    }

    @Test
    void concurrentUploadsDoNotOverwrite() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<FileUploadVO>> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tasks.add(() -> {
                    TestAuth.loginOperator();
                    return fileService.uploadImage(png("c.png"), null);
                });
            }
            List<Future<FileUploadVO>> futures = pool.invokeAll(tasks);
            List<String> keys = new ArrayList<>();
            for (Future<FileUploadVO> future : futures) {
                keys.add(future.get().getObjectKey());
            }
            assertThat(keys).doesNotHaveDuplicates().hasSize(10);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void uploadRequiresFileUploadPermission() throws Exception {
        String token = jwtTokenService.issue(new LoginUser(1L, "admin", ClientType.ADMIN,
                Set.of(PermissionCodes.CONTENT_VIEW)));
        mockMvc.perform(multipart("/admin/api/files/upload")
                        .file(png("deny.png"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));

        String allowed = jwtTokenService.issue(new LoginUser(1L, "admin", ClientType.ADMIN,
                Set.of(PermissionCodes.FILE_UPLOAD)));
        mockMvc.perform(multipart("/admin/api/files/upload")
                        .file(png("ok.png"))
                        .param("scene", "article")
                        .header("Authorization", "Bearer " + allowed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/")))
                .andExpect(jsonPath("$.data.objectKey").value(org.hamcrest.Matchers.containsString("articles/")))
                .andExpect(jsonPath("$.data.contentType").value("image/png"));
    }

    @Test
    void uploadsMp4AsVideoFile() throws Exception {
        TestAuth.loginOperator();
        FileUploadVO vo = fileService.uploadImage(mp4("lesson.mp4"),
                com.xxedu.learning.modules.file.enums.UploadScene.VIDEO_FILE);
        assertThat(vo.getContentType()).isEqualTo("video/mp4");
        assertThat(vo.getUrl()).startsWith("/uploads/videos/local/");
        assertThat(vo.getObjectKey()).startsWith("videos/local/");
        if (storageService instanceof LocalStorageService local) {
            assertThat(Files.exists(local.resolveUnderRoot(vo.getObjectKey()))).isTrue();
        }
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(vo.getUrl()))
                .andExpect(status().isOk());
        String token = jwtTokenService.issue(new LoginUser(1L, "admin", ClientType.ADMIN,
                Set.of(PermissionCodes.FILE_UPLOAD)));
        mockMvc.perform(multipart("/admin/api/files/upload")
                        .file(mp4("api.mp4"))
                        .param("scene", "VIDEO_FILE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.contentType").value("video/mp4"))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/videos/local/")));
    }

    @Test
    void rejectsNonMp4ForVideoFileScene() throws Exception {
        TestAuth.loginOperator();
        assertThatThrownBy(() -> fileService.uploadImage(png("cover.png"),
                com.xxedu.learning.modules.file.enums.UploadScene.VIDEO_FILE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅支持 MP4 视频");
    }

    private void assertUploadOk(MockMultipartFile file, String contentType) throws Exception {
        TestAuth.loginOperator();
        FileUploadVO vo = fileService.uploadImage(file, null);
        assertThat(vo.getContentType()).isEqualTo(contentType);
        assertThat(vo.getUrl()).startsWith("/uploads/images/");
        assertThat(vo.getObjectKey()).startsWith("images/");
        assertThat(vo.getSize()).isPositive();
        if (storageService instanceof LocalStorageService local) {
            assertThat(Files.exists(local.resolveUnderRoot(vo.getObjectKey()))).isTrue();
        }
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(vo.getUrl()))
                .andExpect(status().isOk());
        TestAuth.loginOperator();
    }

    private void assertRejected(MultipartFile file, String message) {
        assertThatThrownBy(() -> fileService.uploadImage(file, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(message)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    private MockMultipartFile mp4(String name) {
        byte[] bytes = new byte[] {
                0x00, 0x00, 0x00, 0x18,
                0x66, 0x74, 0x79, 0x70,
                0x69, 0x73, 0x6F, 0x6D,
                0x00, 0x00, 0x02, 0x00,
                0x69, 0x73, 0x6F, 0x6D,
                0x6D, 0x70, 0x34, 0x31
        };
        return new MockMultipartFile("file", name, "video/mp4", bytes);
    }

    private MockMultipartFile png(String name) throws Exception {
        return image(name, "png", "image/png");
    }

    private MockMultipartFile jpeg(String name) throws Exception {
        return image(name, "jpg", "image/jpeg");
    }

    private MockMultipartFile gif(String name) throws Exception {
        return image(name, "gif", "image/gif");
    }

    private MockMultipartFile webp(String name) {
        byte[] bytes = new byte[30];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 4);
        bytes[4] = 22;
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, bytes, 8, 4);
        System.arraycopy("VP8L".getBytes(StandardCharsets.US_ASCII), 0, bytes, 12, 4);
        return new MockMultipartFile("file", name, "image/webp", bytes);
    }

    private MockMultipartFile image(String name, String format, String contentType) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return new MockMultipartFile("file", name, contentType, output.toByteArray());
    }

    private MockMultipartFile text(String name, String body) {
        return new MockMultipartFile("file", name, MediaType.TEXT_PLAIN_VALUE, body.getBytes(StandardCharsets.UTF_8));
    }

    private MultipartFile oversizedPng() throws Exception {
        byte[] tiny = png("tiny.png").getBytes();
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return "big.png";
            }

            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return ImageUploadValidator.MAX_BYTES + 1;
            }

            @Override
            public byte[] getBytes() {
                return tiny;
            }

            @Override
            public java.io.InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(tiny);
            }

            @Override
            public void transferTo(java.io.File dest) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
