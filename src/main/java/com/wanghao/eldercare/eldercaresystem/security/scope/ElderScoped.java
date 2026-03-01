package com.wanghao.eldercare.eldercaresystem.security.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ElderScoped {
    String elderIdParam() default "elderId";
}
