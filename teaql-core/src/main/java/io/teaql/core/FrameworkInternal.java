package io.teaql.core;

import java.lang.annotation.*;

/**
 * Marks a method as framework-internal. Business code must not call methods
 * annotated with @FrameworkInternal directly.
 *
 * IDE plugins and static analysis tools should flag any call to a
 * @FrameworkInternal method from outside framework packages as an error.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Documented
public @interface FrameworkInternal {
    /** Guidance message for developers who encounter this annotation. */
    String value() default "This method is for framework infrastructure use only.";
}
