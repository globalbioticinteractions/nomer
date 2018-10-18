package org.globalbioticinteractions.nomer.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyEnricherInfo {
    String description() default "no description";
    String name() default "no name";
}
