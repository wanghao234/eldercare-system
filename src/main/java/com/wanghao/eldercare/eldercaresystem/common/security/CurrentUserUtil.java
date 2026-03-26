package com.wanghao.eldercare.eldercaresystem.common.security;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
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
