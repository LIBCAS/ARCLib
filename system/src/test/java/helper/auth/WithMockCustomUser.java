package helper.auth;

import cz.cas.lib.arclib.security.authorization.Roles;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
    String username() default "user";

    String id() default "user";

    String role() default Roles.SUPER_ADMIN;
}
