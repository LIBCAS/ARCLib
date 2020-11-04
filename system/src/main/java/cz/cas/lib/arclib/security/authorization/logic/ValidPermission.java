package cz.cas.lib.arclib.security.authorization.logic;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPermissionValidator.class)
public @interface ValidPermission {
    String message() default "Permission does not exist: '${validatedValue}'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
