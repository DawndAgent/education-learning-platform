package com.xxedu.learning.common.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsDefaultPage() {
        assertThat(validator.validate(new PageQuery())).isEmpty();
    }

    @Test
    void rejectsPageSizeOverLimit() {
        PageQuery query = new PageQuery();
        query.setPageNum(0);
        query.setPageSize(PageQuery.MAX_PAGE_SIZE + 1);

        assertThat(validator.validate(query)).hasSize(2);
    }
}
