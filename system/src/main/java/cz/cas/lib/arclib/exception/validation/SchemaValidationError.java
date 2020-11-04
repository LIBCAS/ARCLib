package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SchemaValidationError extends GeneralException {
    public SchemaValidationError(String message) {
        super(message);
    }

    public SchemaValidationError(String message, Throwable cause) {
        super(message, cause);
    }
}
