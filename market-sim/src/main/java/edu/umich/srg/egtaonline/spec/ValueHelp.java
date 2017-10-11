package edu.umich.srg.egtaonline.spec;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/** Assign help text to a value. */
@Documented
@Retention(RUNTIME)
public @interface ValueHelp {
  /** The help text to assign. */
  String value();
}
