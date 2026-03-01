package com.wanghao.eldercare.eldercaresystem.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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

