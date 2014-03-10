/**
 * 
 */
package ro.google.appenigne.annotation;


import ro.google.appenigne.utils.UserType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Bogdan Nourescu
 * 
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredType {
	
	UserType[] value() default UserType.SuperAdministrator;
	
}
