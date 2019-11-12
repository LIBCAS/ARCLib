package cz.cas.lib.core.store;

import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.springframework.transaction.annotation.Propagation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Replacement for Spring Transactional with no rollback for GeneralException and requirement of new transaction.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@org.springframework.transaction.annotation.Transactional(noRollbackFor = {GeneralException.class, IncidentException.class, BpmnError.class}, propagation = Propagation.REQUIRES_NEW)
public @interface TransactionalNew {
}
