package com.kaba4cow.requesthandler.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking a method parameter as a required or optional path parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface RequestParam {

	/**
	 * Indicates whether the parameter is required.
	 * 
	 * @return {@code true} if the parameter is required; {@code false} otherwise
	 */
	boolean required() default true;

	/**
	 * The default value to use if the parameter is not provided.
	 * 
	 * @return the default value
	 */
	String defaultValue() default "";

}
