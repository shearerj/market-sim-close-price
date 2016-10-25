package edu.umich.srg.egtaonline.spec;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

/** Ignore value, so it can't be used. */
@Retention(RUNTIME)
public @interface IgnoreValue {
}
