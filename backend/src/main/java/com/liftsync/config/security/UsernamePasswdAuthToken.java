package com.liftsync.config.security;

import com.liftsync.model.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public record UsernamePasswdAuthToken(User principal,
                                      Object credentials,
                                      List<GrantedAuthority> authorities) implements Authentication {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Cannot set authentication status for UsernamePasswdAuthToken");
    }

    @Override
    public String getName() {
        return principal.getUsername();
    }
}
