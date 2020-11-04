package helper.auth;

import cz.cas.lib.arclib.security.authorization.data.Permissions;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
    String username() default "user";

    String id() default "user";

    String roleName() default "SUPER_ADMIN";

    String roleDescription() default "Superadministrátor";

    String[] permissions() default {Permissions.SUPER_ADMIN_PRIVILEGE};
}
