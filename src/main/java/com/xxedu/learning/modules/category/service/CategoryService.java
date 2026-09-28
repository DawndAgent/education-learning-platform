package com.xxedu.learning.modules.category.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.common.log.BizLogger;
import com.xxedu.learning.common.util.TextValues;
import com.xxedu.learning.modules.category.convert.CategoryConverter;
import com.xxedu.learning.modules.category.dto.CategoryCreateRequest;
import com.xxedu.learning.modules.category.dto.CategoryUpdateRequest;
import com.xxedu.learning.modules.category.entity.Category;
import com.xxedu.learning.modules.category.enums.CategoryStatus;
import com.xxedu.learning.modules.category.mapper.CategoryMapper;
import com.xxedu.learning.modules.category.vo.CategoryTreeVO;
import com.xxedu.learning.modules.category.vo.CategoryVO;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
public class CategoryService {

    static final long ROOT_PARENT_ID = 0L;
    private static final String DUPLICATE_CODE = "分类编码已存在";
    private static final String NOT_FOUND = "分类不存在";
    private static final String PARENT_NOT_FOUND = "父分类不存在";
    private static final String CHILDREN_EXIST = "该分类下存在子分类，无法删除";
    private static final String CONTENT_EXIST = "该分类下存在内容，无法删除";
    private static final String SELF_PARENT = "不能选择自身作为父分类";
    private static final String CYCLE_PARENT = "不能把子分类设为父分类";
    private static final String PARENT_DISABLED = "父分类未启用";
    private static final String PARENT_NOT_ROOT = "只能选择一级分类作为父分类";
    private static final String HAS_CHILDREN_MOVE = "该分类已有子分类，不能改为二级分类";
    private static final String CONTENT_NEEDS_CHILD = "内容只能选择二级分类";
    private static final String CATEGORY_DISABLED = "分类已停用";
    private static final String CATEGORY_DISABLED_PUBLISH = "分类已停用，不能发布";

    private final CategoryMapper categoryMapper;
    private final CategoryConverter categoryConverter;
    private final ContentMapper contentMapper;

    public List<CategoryTreeVO> publicTree() {
        return buildTree(visiblePublic(listAll()), false);
    }

    @RequirePermission(PermissionCodes.CATEGORY_MANAGE)
    public List<CategoryTreeVO> adminTree() {
        return buildTree(listAll(), true);
    }

    public CategoryVO publicDetail(Long id) {
        List<Category> all = listAll();
        Map<Long, Category> byId = index(all);
        Category category = byId.get(id);
        if (category == null || !isPubliclyVisible(category, byId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return categoryConverter.toVo(category);
    }

    @RequirePermission(PermissionCodes.CATEGORY_MANAGE)
    @Transactional
    public CategoryVO create(@Valid CategoryCreateRequest request) {
        assertParent(null, request.getParentId());
        assertCodeAvailable(request.getCode(), null);
        Category category = new Category();
        category.setParentId(request.getParentId());
        category.setName(request.getName().trim());
        category.setCode(request.getCode());
        category.setIconUrl(TextValues.trimToNull(request.getIconUrl()));
        category.setDescription(TextValues.trimToNull(request.getDescription()));
        category.setSort(request.getSort());
        category.setStatus(request.getStatus());
        insert(category);
        BizLogger.info("category.create", "id={} code={}", category.getId(), category.getCode());
        return categoryConverter.toVo(category);
    }

    @RequirePermission(PermissionCodes.CATEGORY_MANAGE)
    @Transactional
    public CategoryVO update(Long id, @Valid CategoryUpdateRequest request) {
        Category category = requireExisting(id);
        assertParent(id, request.getParentId());
        assertCodeAvailable(request.getCode(), id);
        category.setParentId(request.getParentId());
        category.setName(request.getName().trim());
        category.setCode(request.getCode());
        category.setIconUrl(TextValues.trimToNull(request.getIconUrl()));
        category.setDescription(TextValues.trimToNull(request.getDescription()));
        category.setSort(request.getSort());
        category.setStatus(request.getStatus());
        try {
            categoryMapper.updateById(category);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DUPLICATE_CODE);
        }
        BizLogger.info("category.update", "id={} code={}", category.getId(), category.getCode());
        return categoryConverter.toVo(category);
    }

    @RequirePermission(PermissionCodes.CATEGORY_MANAGE)
    @Transactional
    public void delete(Long id) {
        requireExisting(id);
        Long children = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, CHILDREN_EXIST);
        }
        Long contents = contentMapper.selectCount(Wrappers.<Content>lambdaQuery().eq(Content::getCategoryId, id));
        if (contents != null && contents > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, CONTENT_EXIST);
        }
        categoryMapper.deleteById(id);
        BizLogger.info("category.delete", "id={}", id);
    }

    public Category requireExisting(Long id) {
        Category category = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
                .select(Category::getId, Category::getParentId, Category::getName, Category::getCode,
                        Category::getIconUrl, Category::getDescription, Category::getSort, Category::getStatus)
                .eq(Category::getId, id));
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND);
        }
        return category;
    }

    public Map<Long, String> namesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        List<Category> categories = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .select(Category::getId, Category::getName)
                .in(Category::getId, distinct));
        for (Category category : categories) {
            names.put(category.getId(), category.getName());
        }
        return names;
    }

    public void assertContentCategory(Long categoryId, boolean publishing) {
        Category category = requireExisting(categoryId);
        Long parentId = category.getParentId();
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, CONTENT_NEEDS_CHILD);
        }
        if (category.getStatus() != CategoryStatus.ENABLED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, publishing ? CATEGORY_DISABLED_PUBLISH : CATEGORY_DISABLED);
        }
    }

    public List<Long> selfAndDescendantIds(Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        List<Category> links = listLinks();
        boolean exists = links.stream().anyMatch(link -> categoryId.equals(link.getId()));
        if (!exists) {
            return List.of();
        }
        Map<Long, List<Long>> children = new HashMap<>();
        for (Category link : links) {
            children.computeIfAbsent(link.getParentId(), key -> new ArrayList<>()).add(link.getId());
        }
        List<Long> result = new ArrayList<>();
        Deque<Long> pending = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        pending.push(categoryId);
        while (!pending.isEmpty()) {
            Long current = pending.pop();
            if (!seen.add(current)) {
                continue;
            }
            result.add(current);
            for (Long childId : children.getOrDefault(current, List.of())) {
                pending.push(childId);
            }
        }
        return List.copyOf(result);
    }

    private void insert(Category category) {
        try {
            categoryMapper.insert(category);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DUPLICATE_CODE);
        }
    }

    private void assertCodeAvailable(String code, Long selfId) {
        Long count = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getCode, code)
                .ne(selfId != null, Category::getId, selfId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DUPLICATE_CODE);
        }
    }

    private void assertParent(Long selfId, Long parentId) {
        if (parentId == null || parentId < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, PARENT_NOT_FOUND);
        }
        if (parentId == ROOT_PARENT_ID) {
            return;
        }
        if (selfId != null && selfId.equals(parentId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, SELF_PARENT);
        }
        Map<Long, Category> byId = index(listAll());
        Category parent = byId.get(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, PARENT_NOT_FOUND);
        }
        if (selfId != null && isDescendant(parentId, selfId, byId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, CYCLE_PARENT);
        }
        if (parent.getStatus() != CategoryStatus.ENABLED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, PARENT_DISABLED);
        }
        Long parentParentId = parent.getParentId();
        if (parentParentId != null && parentParentId != ROOT_PARENT_ID) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, PARENT_NOT_ROOT);
        }
        if (selfId != null && hasChildren(selfId, byId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, HAS_CHILDREN_MOVE);
        }
    }

    private boolean isDescendant(Long candidateParentId, Long selfId, Map<Long, Category> byId) {
        Long cursor = candidateParentId;
        Set<Long> seen = new HashSet<>();
        while (cursor != null && cursor != ROOT_PARENT_ID) {
            if (selfId.equals(cursor)) {
                return true;
            }
            if (!seen.add(cursor)) {
                return true;
            }
            Category current = byId.get(cursor);
            if (current == null) {
                return false;
            }
            cursor = current.getParentId();
        }
        return false;
    }

    private boolean hasChildren(Long id, Map<Long, Category> byId) {
        for (Category category : byId.values()) {
            if (id.equals(category.getParentId())) {
                return true;
            }
        }
        return false;
    }

    private List<CategoryTreeVO> buildTree(List<Category> categories, boolean includeOrphans) {
        Map<Long, CategoryTreeVO> nodes = new LinkedHashMap<>();
        for (Category category : categories) {
            nodes.put(category.getId(), categoryConverter.toTree(category));
        }
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (Category category : categories) {
            CategoryTreeVO node = nodes.get(category.getId());
            Long parentId = category.getParentId();
            boolean root = parentId == null || parentId == ROOT_PARENT_ID || !nodes.containsKey(parentId);
            if (root) {
                if (includeOrphans || parentId == null || parentId == ROOT_PARENT_ID) {
                    roots.add(node);
                }
                continue;
            }
            nodes.get(parentId).addChild(node);
        }
        return List.copyOf(roots);
    }

    private List<Category> visiblePublic(List<Category> all) {
        Map<Long, Category> byId = index(all);
        List<Category> visible = new ArrayList<>();
        for (Category category : all) {
            if (isPubliclyVisible(category, byId)) {
                visible.add(category);
            }
        }
        return visible;
    }

    private boolean isPubliclyVisible(Category category, Map<Long, Category> byId) {
        if (category.getStatus() != CategoryStatus.ENABLED) {
            return false;
        }
        Long parentId = category.getParentId();
        Set<Long> seen = new HashSet<>();
        while (parentId != null && parentId != ROOT_PARENT_ID) {
            if (!seen.add(parentId)) {
                return false;
            }
            Category parent = byId.get(parentId);
            if (parent == null || parent.getStatus() != CategoryStatus.ENABLED) {
                return false;
            }
            parentId = parent.getParentId();
        }
        return true;
    }

    private Map<Long, Category> index(List<Category> categories) {
        Map<Long, Category> byId = new HashMap<>();
        for (Category category : categories) {
            byId.put(category.getId(), category);
        }
        return byId;
    }

    private List<Category> listAll() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .select(Category::getId, Category::getParentId, Category::getName, Category::getCode,
                        Category::getIconUrl, Category::getDescription, Category::getSort, Category::getStatus)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    private List<Category> listLinks() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .select(Category::getId, Category::getParentId));
    }
}
