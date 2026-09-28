package com.xxedu.learning.modules.auth.vo;

import java.util.List;

public class CurrentUserVO {

    private final Long id;
    private final String username;
    private final String nickname;
    private final List<String> permissions;

    public CurrentUserVO(Long id, String username, String nickname, List<String> permissions) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.permissions = List.copyOf(permissions == null ? List.of() : permissions);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public List<String> getPermissions() {
        return List.copyOf(permissions);
    }
}
