package com.bike.openapi.admin.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录主体：身份 + 角色 + 权限码。
 */
@Getter
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String password;
    private final String nickname;
    private final boolean enabled;
    private final boolean mustChangePassword;
    private final int tokenVersion;
    private final Set<String> roleCodes;
    private final Set<String> perms;

    public LoginUser(Long userId, String username, String password, String nickname,
                     boolean enabled, boolean mustChangePassword, int tokenVersion,
                     Set<String> roleCodes, Set<String> perms) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.enabled = enabled;
        this.mustChangePassword = mustChangePassword;
        this.tokenVersion = tokenVersion;
        this.roleCodes = roleCodes;
        this.perms = perms;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return perms.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
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
        return enabled;
    }
}
