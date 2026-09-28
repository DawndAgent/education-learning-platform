package com.xxedu.learning.modules.topic.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PublicTopicDetailVO {

    private Long id;
    private String name;
    private String coverUrl;
    private String summary;
    private PublicTopicCategoryVO category;
    private List<PublicTopicContentVO> contents = new ArrayList<>();
}
