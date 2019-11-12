package cz.cas.lib.core.rest.config;

import cz.cas.lib.arclib.domainbase.exception.*;
import cz.cas.lib.arclib.exception.AuthorialPackageLockedException;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.core.index.UnsupportedSearchParameterException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleAnyException(Exception e) {
        return errorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MissingObject.class)
    public ResponseEntity missingObject(MissingObject e) {
        return errorResponse(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MissingAttribute.class)
    public ResponseEntity missingAttribute(MissingAttribute e) {
        return errorResponse(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidAttribute.class)
    public ResponseEntity invalidAttribute(InvalidAttribute e) {
        return errorResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadArgument.class)
    public ResponseEntity badArgument(BadArgument e) {
        return errorResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenObject.class)
    public ResponseEntity forbiddenObject(ForbiddenObject e) {
        return errorResponse(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ForbiddenOperation.class)
    public ResponseEntity forbiddenOperation(ForbiddenOperation e) {
        return errorResponse(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity conflictException(ConflictException e) {
        return errorResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity bindException(BindException e) {
        return errorResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity badRequestException(BadRequestException e) {
        return errorResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedSearchParameterException.class)
    public ResponseEntity unsupportedSearchParameterException(UnsupportedSearchParameterException e) {
        return errorResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity dataIntegrityViolationException(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException && e.getCause().getCause() instanceof PSQLException)
            return errorResponse(e.getCause().getCause(), HttpStatus.CONFLICT);
        return errorResponse(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity forbiddenException(ForbiddenException e) {
        return errorResponse(e, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthorialPackageLockedException.class)
    public ResponseEntity authorialPackageLockedException(AuthorialPackageLockedException e) {
        return errorResponse(e, HttpStatus.LOCKED);
    }

    private ResponseEntity errorResponse(Throwable throwable, HttpStatus status) {
        log.error("error caught: " + throwable.getMessage(), throwable);
        return ResponseEntity.status(status).body(throwable.toString());
    }
}
