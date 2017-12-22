package cz.inqool.uas.sign.dto;

import eu.europa.esig.dss.EncryptionAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MessageDigest {
    private Instant created;
    private String message;
    private EncryptionAlgorithm algorithm;
}
