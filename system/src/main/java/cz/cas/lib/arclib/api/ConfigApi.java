package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.security.ArclibAuthenticationType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "config", description = "Api to read backend configuration")
@RequestMapping("/api/config")
public class ConfigApi {

    public static final String KEY_AUTHENTICATION = "AUTHENTICATION";

    @Value("${security.ldap.enabled}")
    private boolean ldapAuth;
    @Value("${security.local.enabled}")
    private boolean localAuth;

    @RequestMapping(method = RequestMethod.GET)
    public Map<String, String> get() {
        ArclibAuthenticationType authType = ldapAuth ? ArclibAuthenticationType.LDAP : ArclibAuthenticationType.LOCAL;
        return Map.of(KEY_AUTHENTICATION, authType.toString());
    }
}
