package com.xxedu.learning.modules.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxedu.learning.modules.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
