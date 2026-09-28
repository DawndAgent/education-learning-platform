package com.xxedu.learning.modules.category.convert;

import com.xxedu.learning.modules.category.entity.Category;
import com.xxedu.learning.modules.category.vo.CategoryTreeVO;
import com.xxedu.learning.modules.category.vo.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryConverter {

    CategoryVO toVo(Category category);

    @Mapping(target = "children", ignore = true)
    CategoryTreeVO toTree(Category category);
}
