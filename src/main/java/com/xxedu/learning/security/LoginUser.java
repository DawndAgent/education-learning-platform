package com.xxedu.learning.security;

import com.xxedu.learning.common.enums.ClientType;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Getter
public final class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final ClientType clientType;
    private final Set<String> permissions;

    public LoginUser(Long userId, String username, ClientType clientType, Set<String> permissions) {
        if (userId == null || username == null || username.isBlank() || clientType == null) {
            throw new IllegalArgumentException("login user is incomplete");
        }
        this.userId = userId;
        this.username = username;
        this.clientType = clientType;
        this.permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
