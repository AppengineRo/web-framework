/**
 *
 */
package ro.appenigne.web.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adnotare pentru verificare impotriva XSS (cross site scripting)
 *
 * @XssCheck(false) pentru a nu face filtrare. (Pentru salvare Raport de ex)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface XssCheck {

    boolean value() default true;

}
