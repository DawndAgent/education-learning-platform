package com.xxedu.learning.modules.auth.vo;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final String token;
    private final String tokenType;
    private final long expiresIn;
    private final CurrentUserVO user;

    public LoginResponse(String token, String tokenType, long expiresIn, CurrentUserVO user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
