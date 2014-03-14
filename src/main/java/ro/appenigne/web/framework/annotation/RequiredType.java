/**
 *
 */
package ro.appenigne.web.framework.annotation;


import ro.appenigne.web.framework.utils.AbstractUserType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredType {

    String[] value() default AbstractUserType.SuperAdministrator;

}
