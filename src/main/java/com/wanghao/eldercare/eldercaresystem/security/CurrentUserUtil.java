package com.wanghao.eldercare.eldercaresystem.security;

import org.springframework.stereotype.Component;

@Component
public class CurrentUserUtil {

    private final CurrentUserUtils delegate;

    public CurrentUserUtil(CurrentUserUtils delegate) {
        this.delegate = delegate;
    }

    public CurrentUser getCurrentUser() {
        return delegate.getCurrentUser();
    }
}
