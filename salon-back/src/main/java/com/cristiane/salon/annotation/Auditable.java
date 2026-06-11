package com.cristiane.salon.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    
    String action() default "";
    
    String entityType() default "";
    
    boolean captureArgs() default false;
}
