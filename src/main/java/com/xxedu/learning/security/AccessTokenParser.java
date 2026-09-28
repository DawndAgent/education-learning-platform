package com.xxedu.learning.security;

import java.util.Optional;

@FunctionalInterface
public interface AccessTokenParser {

    /**
     * 解析访问令牌。登录模块实现本接口后替换默认实现。
     * 无法识别时返回空，不得抛出异常。
     */
    Optional<LoginUser> parse(String token);
}
