package com.xxedu.learning.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.auth.dto.LoginRequest;
import com.xxedu.learning.modules.auth.entity.SysUser;
import com.xxedu.learning.modules.auth.mapper.SysUserMapper;
import com.xxedu.learning.modules.auth.vo.CurrentUserVO;
import com.xxedu.learning.modules.auth.vo.LoginResponse;
import com.xxedu.learning.security.AuthContext;
import com.xxedu.learning.security.JwtProperties;
import com.xxedu.learning.security.JwtTokenService;
import com.xxedu.learning.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class AuthService {

    private static final int ENABLED = 1;
    private static final String TOKEN_TYPE = "Bearer";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername().trim()));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != ENABLED) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "密码错误");
        }
        CurrentUserVO currentUser = toCurrentUser(user);
        LoginUser loginUser = new LoginUser(
                user.getId(),
                user.getUsername(),
                ClientType.ADMIN,
                new LinkedHashSet<>(currentUser.getPermissions()));
        return new LoginResponse(
                jwtTokenService.issue(loginUser),
                TOKEN_TYPE,
                jwtProperties.getTtl().toSeconds(),
                currentUser);
    }

    public CurrentUserVO currentUser() {
        LoginUser session = AuthContext.currentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        SysUser user = sysUserMapper.selectById(session.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != ENABLED) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已失效");
        }
        return toCurrentUser(user);
    }

    public void logout() {
        if (AuthContext.currentUser().isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private CurrentUserVO toCurrentUser(SysUser user) {
        List<String> permissions = sysUserMapper.selectPermissionCodes(user.getId());
        return new CurrentUserVO(user.getId(), user.getUsername(), user.getNickname(), permissions);
    }
}
