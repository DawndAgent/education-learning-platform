package com.xxedu.learning.modules.video.convert;

import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.video.entity.Video;
import com.xxedu.learning.modules.video.vo.VideoDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VideoConverter {

    @Mapping(target = "contentId", source = "content.id")
    @Mapping(target = "title", source = "content.title")
    @Mapping(target = "categoryId", source = "content.categoryId")
    @Mapping(target = "coverUrl", source = "content.coverUrl")
    @Mapping(target = "summary", source = "content.summary")
    @Mapping(target = "status", source = "content.status")
    @Mapping(target = "sort", source = "content.sort")
    @Mapping(target = "publishTime", source = "content.publishTime")
    @Mapping(target = "sourceType", source = "video.sourceType")
    @Mapping(target = "videoUrl", source = "video.videoUrl")
    @Mapping(target = "qrCodeUrl", source = "video.qrCodeUrl")
    @Mapping(target = "duration", source = "video.duration")
    VideoDetailVO toDetail(Content content, Video video);
}
