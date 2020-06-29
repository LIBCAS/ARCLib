package cz.cas.lib.arclib.service.archivalStorage;

import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * Thrown if case of any error related with requests to Archival Storage. If the Archival Storage was not reached due to
 * connection error, {@link #responseStatus} and {@link #responseBody} are not filled. If the Archival Storage responds with
 * any other return code than 2xx, then {@link #responseStatus} and {@link #responseBody} is filled.
 */
@Getter
public class ArchivalStorageException extends Exception {

    private HttpStatus responseStatus;
    private String responseBody;

    public ArchivalStorageException(String message) {
        super(message);
    }

    public ArchivalStorageException(Throwable cause) {
        super(cause);
    }

    public ArchivalStorageException(ArchivalStorageResponse response) {
        if (response != null) {
            responseStatus = response.getStatusCode();
            try {
                responseBody = IOUtils.toString(response.getBody());
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public String toString() {
        if (responseStatus == null)
            return super.toString();
        return "ArchivalStorageException{" +
                "status=" + responseStatus.value() +
                ", body=" + responseBody +
                '}';
    }

    @Override
    public String getMessage() {
        if (responseStatus != null)
            return toString();
        return super.getMessage();
    }
}
