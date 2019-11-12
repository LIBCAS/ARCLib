package cz.cas.lib.core.config.change.checker;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Configuration
public class Runner {
    @Inject
    private Detector detector;

    /**
     * Runs the configuration change detection on application startup.
     */
    @PostConstruct
    public void init() {
        detector.check();
    }
}
