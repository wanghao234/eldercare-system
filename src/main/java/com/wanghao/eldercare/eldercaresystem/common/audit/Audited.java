package com.wanghao.eldercare.eldercaresystem.common.audit;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();

    String failAction() default "";

    String entityType();

    String entityIdArg() default "";

    String responseIdPath() default "";

    String fromField() default "";

    String toField() default "";

    String fromValue() default "";

    String toValue() default "";

    String[] requestFields() default {};

    boolean sensitive() default false;
}

