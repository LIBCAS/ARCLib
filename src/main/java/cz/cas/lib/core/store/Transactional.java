package cz.cas.lib.core.store;

import cz.cas.lib.core.exception.GeneralException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Replacement for Spring Transactional with no rollback for GeneralException.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@org.springframework.transaction.annotation.Transactional(noRollbackFor = GeneralException.class)
public @interface Transactional {
}
