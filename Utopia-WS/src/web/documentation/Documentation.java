package web.documentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Documentation {
    /**
     * @return the documentation text
     */
    String value();

    /**
     * @return the name of the documented item (usually the name of a parameter, since those are lost at runtime otherwise)
     */
    String itemName() default "";
}
