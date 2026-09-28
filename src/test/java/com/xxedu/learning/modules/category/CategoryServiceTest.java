package com.xxedu.learning.modules.category;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.category.dto.CategoryCreateRequest;
import com.xxedu.learning.modules.category.dto.CategoryUpdateRequest;
import com.xxedu.learning.modules.category.enums.CategoryStatus;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.category.vo.CategoryTreeVO;
import com.xxedu.learning.modules.category.vo.CategoryVO;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CategoryServiceTest extends IntegrationTestSupport {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ContentMapper contentMapper;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void publicTreeContainsSeedRootsAndChildren() {
        List<CategoryTreeVO> tree = categoryService.publicTree();

        assertThat(tree).extracting(CategoryTreeVO::getCode).containsExactly("CAMBRIDGE", "MATH", "JUNIOR");
        CategoryTreeVO cambridge = tree.get(0);
        assertThat(cambridge.getChildren()).extracting(CategoryTreeVO::getCode)
                .containsExactly("CAMBRIDGE_KET", "CAMBRIDGE_READING", "CAMBRIDGE_LISTENING", "CAMBRIDGE_ARCHIVE");
    }

    @Test
    void disabledParentHidesChildrenFromPublicTree() {
        categoryService.update(1L, update(0L, "剑桥英语", "CAMBRIDGE", 1, CategoryStatus.DISABLED));

        assertThat(categoryService.publicTree()).extracting(CategoryTreeVO::getCode)
                .containsExactly("MATH", "JUNIOR");
        assertThat(categoryService.adminTree()).extracting(CategoryTreeVO::getCode)
                .contains("CAMBRIDGE", "MATH", "JUNIOR");
        assertThatThrownBy(() -> categoryService.publicDetail(11L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        categoryService.update(1L, update(0L, "剑桥英语", "CAMBRIDGE", 1, CategoryStatus.ENABLED));
        assertThat(categoryService.publicTree()).extracting(CategoryTreeVO::getCode)
                .contains("CAMBRIDGE");
    }

    @Test
    void duplicateCodeIsRejected() {
        assertThatThrownBy(() -> categoryService.create(create(0L, "重复", "CAMBRIDGE")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类编码已存在");
    }

    @Test
    void childrenBlockDeleteAndMissingCategoryIsNotFound() {
        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该分类下存在子分类，无法删除");
        assertThatThrownBy(() -> categoryService.delete(9999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类不存在");
        assertThatThrownBy(() -> categoryService.update(9999L, update(0L, "不存在", "MISSING_NODE", 1, CategoryStatus.ENABLED)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void cannotMoveCategoryUnderItsDescendant() {
        assertThatThrownBy(() -> categoryService.create(create(11L, "下级", "CAMBRIDGE_KET_CHILD")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只能选择一级分类作为父分类");
        assertThatThrownBy(() -> categoryService.update(1L, update(11L, "剑桥英语", "CAMBRIDGE", 1, CategoryStatus.ENABLED)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能把子分类设为父分类");
        assertThatThrownBy(() -> categoryService.update(11L, update(11L, "KET/PET备考资料", "CAMBRIDGE_KET", 1, CategoryStatus.ENABLED)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能选择自身作为父分类");
    }

    @Test
    void createRootAndChildThenEdit() {
        CategoryVO root = categoryService.create(create(0L, "临时一级", "TEMP_ROOT"));
        CategoryCreateRequest child = create(root.getId(), "临时二级", "TEMP_CHILD");
        child.setSort(2);
        CategoryVO createdChild = categoryService.create(child);

        CategoryVO edited = categoryService.update(createdChild.getId(),
                update(root.getId(), "临时二级改", "TEMP_CHILD", 2, CategoryStatus.ENABLED));

        assertThat(edited.getName()).isEqualTo("临时二级改");
        assertThat(categoryService.adminTree()).extracting(CategoryTreeVO::getCode).contains("TEMP_ROOT");
    }

    @Test
    void blankNameAndMissingOrDisabledParentAreRejected() {
        assertThatThrownBy(() -> categoryService.create(create(0L, "  ", "BLANK_NAME")))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> categoryService.create(create(8888L, "孤儿", "ORPHAN_PARENT")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("父分类不存在");

        categoryService.update(1L, update(0L, "剑桥英语", "CAMBRIDGE", 1, CategoryStatus.DISABLED));
        assertThatThrownBy(() -> categoryService.create(create(1L, "停用下新增", "CAMBRIDGE_OFF_CHILD")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("父分类未启用");
    }

    @Test
    void contentBlocksDeleteAndRootWithChildrenCannotBecomeChild() {
        Content content = new Content();
        content.setTitle("测试内容");
        content.setContentType(ContentType.ARTICLE);
        content.setCategoryId(14L);
        content.setStatus(ContentStatus.DRAFT);
        content.setSort(0);
        content.setViewCount(0L);
        content.setFavoriteCount(0L);
        contentMapper.insert(content);

        assertThatThrownBy(() -> categoryService.delete(14L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该分类下存在内容，无法删除");
        assertThatThrownBy(() -> categoryService.update(1L, update(2L, "剑桥英语", "CAMBRIDGE", 1, CategoryStatus.ENABLED)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该分类已有子分类，不能改为二级分类");
    }

    @Test
    void categoryManagePermissionIsRequired() {
        TestAuth.login(PermissionCodes.CONTENT_VIEW);

        assertThatThrownBy(() -> categoryService.adminTree())
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> categoryService.create(create(0L, "无权", "NO_PERM_ROOT")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void adminTreeRequiresPermission() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> categoryService.adminTree())
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private CategoryCreateRequest create(Long parentId, String name, String code) {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setParentId(parentId);
        request.setName(name);
        request.setCode(code);
        request.setSort(1);
        request.setStatus(CategoryStatus.ENABLED);
        return request;
    }

    private CategoryUpdateRequest update(Long parentId, String name, String code, int sort, CategoryStatus status) {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setParentId(parentId);
        request.setName(name);
        request.setCode(code);
        request.setSort(sort);
        request.setStatus(status);
        return request;
    }
}
