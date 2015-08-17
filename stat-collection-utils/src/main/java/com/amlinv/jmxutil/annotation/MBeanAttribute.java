package com.amlinv.jmxutil.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates an mbean attribute with the given name; <b>must</b> be the setter.
 * Created by art on 3/31/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MBeanAttribute {
    String name();

    Class<?> type();
}
