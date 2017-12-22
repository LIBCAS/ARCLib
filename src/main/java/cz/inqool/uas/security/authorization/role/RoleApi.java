package cz.inqool.uas.security.authorization.role;

import cz.inqool.uas.rest.NamedApi;
import io.swagger.annotations.Api;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@ConditionalOnProperty(prefix = "security.roles.internal", name = "enabled", havingValue = "true")
@RestController
@Api(value = "role", description = "Api for roles management (main attribute: name).")
@RequestMapping("/api/roles")
public class RoleApi implements NamedApi<Role> {
    @Getter
    private String nameAttribute = "name";

    @Getter
    private RoleStore adapter;

    @Inject
    public void setAdapter(RoleStore adapter) {
        this.adapter = adapter;
    }
}
