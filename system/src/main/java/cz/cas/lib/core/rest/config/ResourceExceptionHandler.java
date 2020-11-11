package cz.cas.lib.core.rest.config;

import cz.cas.lib.arclib.domainbase.exception.*;
import cz.cas.lib.arclib.exception.AuthorialPackageLockedException;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.core.index.UnsupportedSearchParameterException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@link Exception} to HTTP codes mapping.
 *
 * <p>
 * Uses Spring functionality for mapping concrete {@link Exception} onto a returned HTTP code.
 * To create new mapping just create new method with {@link ResponseStatus} and {@link ExceptionHandler}
 * annotations.
 * </p>
 */
@Slf4j
@ControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler({
            Exception.class,
            ArchivalStorageException.class
    })
    public ResponseEntity handleAnyException(Exception e) {
        return errorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({
            MissingObject.class,
            MissingAttribute.class
    })
    public ResponseEntity notFound(Exception e) {
        return errorResponse(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            ConflictException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity conflict(Exception e) {
        if (e.getCause() instanceof ConstraintViolationException && e.getCause().getCause() instanceof PSQLException)
            return errorResponse(e.getCause().getCause(), HttpStatus.CONFLICT);
        return errorResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
            BadRequestException.class,
            BindException.class,
            BadArgument.class,
            InvalidAttribute.class,
            UnsupportedSearchParameterException.class
    })
    public ResponseEntity badRequest(Exception e) {
        return errorResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            ForbiddenException.class,
            ForbiddenOperation.class,
            ForbiddenObject.class
    })
    public ResponseEntity forbidden(Exception e) {
        return errorResponse(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthorialPackageLockedException.class)
    public ResponseEntity locked(AuthorialPackageLockedException e) {
        return errorResponse(e, HttpStatus.LOCKED);
    }

    /**
     * do not log stacktrace for this one
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity accessDeniedException(Exception e) {
        log.info(e.toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.toString());
    }

    private ResponseEntity errorResponse(Throwable throwable, HttpStatus status) {
        log.error("error caught: " + throwable.toString(), throwable);
        return ResponseEntity.status(status).body(throwable.toString());
    }
}
