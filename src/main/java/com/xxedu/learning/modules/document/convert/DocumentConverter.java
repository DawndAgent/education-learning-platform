package com.xxedu.learning.modules.document.convert;

import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.document.entity.Document;
import com.xxedu.learning.modules.document.vo.DocumentDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentConverter {

    @Mapping(target = "contentId", source = "content.id")
    @Mapping(target = "title", source = "content.title")
    @Mapping(target = "categoryId", source = "content.categoryId")
    @Mapping(target = "coverUrl", source = "content.coverUrl")
    @Mapping(target = "summary", source = "content.summary")
    @Mapping(target = "status", source = "content.status")
    @Mapping(target = "sort", source = "content.sort")
    @Mapping(target = "publishTime", source = "content.publishTime")
    @Mapping(target = "fileUrl", source = "document.fileUrl")
    @Mapping(target = "fileName", source = "document.fileName")
    @Mapping(target = "fileSize", source = "document.fileSize")
    @Mapping(target = "fileType", source = "document.fileType")
    @Mapping(target = "downloadUrl", source = "document.downloadUrl")
    @Mapping(target = "previewUrl", source = "document.previewUrl")
    @Mapping(target = "description", source = "document.description")
    DocumentDetailVO toDetail(Content content, Document document);
}
