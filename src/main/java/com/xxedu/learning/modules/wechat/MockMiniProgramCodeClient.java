package com.xxedu.learning.modules.wechat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 本地/测试用：生成普通二维码 PNG。内容为 scene，便于联调插入与展示；
 * 微信相机扫普通码不会打开小程序，生产必须关闭 mock 并配置真实 AppID/AppSecret。
 */
public class MockMiniProgramCodeClient implements MiniProgramCodeClient {

    private static final int SIZE = 430;

    @Override
    public byte[] createUnlimitedCode(String scene, String page, String envVersion) {
        String payload = "mp:" + page + "?scene=" + scene;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, SIZE, SIZE,
                    Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                            EncodeHintType.MARGIN, 1));
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            BizLogger.info("wechat.wxacode.mock", "scene={} page={} env={}", scene, page, envVersion);
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成测试二维码失败");
        }
    }
}
