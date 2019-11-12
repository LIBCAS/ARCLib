package cz.cas.lib.arclib.service.archivalStorage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.io.InputStream;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArchivalStorageResponse {
    private InputStream body;
    private HttpStatus statusCode;
}
