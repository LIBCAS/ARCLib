package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.UserSettings;
import cz.cas.lib.arclib.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "userSettings", description = "Api for interaction with userSettings")
@RequestMapping("/api/user_settings")
public class UserSettingsApi {

    private UserSettingsService service;

    @Operation(summary = "Vrací nastavení pro přihlášeného uživatele")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = UserSettings.class))),
    })
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET)
    public String get() {
        return service.get().getSettings();
    }

    @Operation(summary = "Uloží nastavení pro přihlášeného uživatele")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = UserSettings.class))),
    })
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.PUT)
    public void update(@RequestBody String settings) {
        service.save(settings);
    }


    @Autowired
    public void setService(UserSettingsService service) {
        this.service = service;
    }
}
