package cz.cas.lib.arclib.service.arclibxml.systemWideValidation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class SystemWideValidationNodeConfig {
    private String xpathRoot;
    private String xpathRelative;
    private boolean xsltSource;
}
