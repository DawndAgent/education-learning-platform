package com.xxedu.learning.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.common.enums.DeleteFlag;
import com.xxedu.learning.config.AuditMetaObjectHandler;
import com.xxedu.learning.security.LoginUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityAuditTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditAnnotationsMatchDatabaseConvention() throws Exception {
        assertFill("createdAt", FieldFill.INSERT);
        assertFill("updatedAt", FieldFill.INSERT_UPDATE);
        assertFill("createdBy", FieldFill.INSERT);
        assertFill("updatedBy", FieldFill.INSERT_UPDATE);
        assertFill("deleted", FieldFill.INSERT);
        assertThat(BaseEntity.class.getDeclaredField("deleted").getAnnotation(TableLogic.class)).isNotNull();
    }

    @Test
    void insertFillWritesAuditFieldsAndOperator() {
        LoginUser user = new LoginUser(42L, "auditor", ClientType.ADMIN, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        DemoEntity entity = new DemoEntity();

        new AuditMetaObjectHandler().insertFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedBy()).isEqualTo(42L);
        assertThat(entity.getUpdatedBy()).isEqualTo(42L);
        assertThat(entity.getDeleted()).isEqualTo(DeleteFlag.NOT_DELETED.getCode());
    }

    @Test
    void updateFillRefreshesUpdatedAtWithoutOverwritingCreatedAt() {
        DemoEntity entity = new DemoEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        new AuditMetaObjectHandler().insertFill(metaObject);
        var createdAt = entity.getCreatedAt();

        new AuditMetaObjectHandler().updateFill(metaObject);

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    private void assertFill(String fieldName, FieldFill expected) throws Exception {
        Field field = BaseEntity.class.getDeclaredField(fieldName);
        TableField tableField = field.getAnnotation(TableField.class);
        assertThat(tableField).isNotNull();
        assertThat(tableField.fill()).isEqualTo(expected);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class DemoEntity extends BaseEntity {
    }
}
