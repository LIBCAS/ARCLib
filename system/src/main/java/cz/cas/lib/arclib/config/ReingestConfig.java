package cz.cas.lib.arclib.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "arclib.reingest")
public class ReingestConfig {
    private Integer transferAreaKeepFreeMb;
    private Integer workspaceKeepFreeMb;
    private boolean sharedStorage;
    private String exportCron;
}
