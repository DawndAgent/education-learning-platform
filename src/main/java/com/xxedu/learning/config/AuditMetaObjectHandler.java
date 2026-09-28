package com.xxedu.learning.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.xxedu.learning.common.enums.DeleteFlag;
import com.xxedu.learning.security.AuthContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        fillIfNull(metaObject, "createdAt", now);
        fillIfNull(metaObject, "updatedAt", now);
        fillIfNull(metaObject, "deleted", DeleteFlag.NOT_DELETED.getCode());
        Long userId = AuthContext.currentUserId();
        fillIfNull(metaObject, "createdBy", userId);
        fillIfNull(metaObject, "updatedBy", userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updatedAt")) {
            metaObject.setValue("updatedAt", LocalDateTime.now());
        }
        Long userId = AuthContext.currentUserId();
        if (userId != null && metaObject.hasSetter("updatedBy")) {
            metaObject.setValue("updatedBy", userId);
        }
    }

    private void fillIfNull(MetaObject metaObject, String field, Object value) {
        if (value == null || !metaObject.hasSetter(field) || metaObject.getValue(field) != null) {
            return;
        }
        metaObject.setValue(field, value);
    }
}
