package com.xxedu.learning.modules.content.convert;

import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.content.vo.ContentListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContentConverter {

    @Mapping(target = "categoryName", ignore = true)
    ContentListVO toList(Content content);

    @Mapping(target = "categoryName", ignore = true)
    ContentDetailVO toDetail(Content content);
}
