package com.kaba4cow.requesthandler.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mapping a class or method to a specific request path.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
public @interface RequestMapping {

	/**
	 * The path to which the annotated class or method is mapped.
	 * 
	 * @return the request path
	 */
	public String path();

}
