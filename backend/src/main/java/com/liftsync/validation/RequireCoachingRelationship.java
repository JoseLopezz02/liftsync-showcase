package com.liftsync.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCoachingRelationship {
    /**
     * Name of the @PathVariable that carries the athlete public id.
     */
    String athleteIdParam() default "athleteId";
}
