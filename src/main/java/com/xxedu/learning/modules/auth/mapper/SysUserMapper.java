package com.xxedu.learning.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxedu.learning.modules.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    List<String> selectPermissionCodes(@Param("userId") Long userId);
}
