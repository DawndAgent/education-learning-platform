package com.xxedu.learning.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;

    private String passwordHash;

    private String nickname;

    private Integer status;
}
