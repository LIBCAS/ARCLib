package cz.cas.lib.core.config.change.checker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.core.config.change.ConfigFile;
import cz.cas.lib.core.config.change.ConfigStore;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Map;

/**
 * Configuration change detector.
 *
 * <p>
 * Gathering of all used configuration attributes is done through {@link Gatherer}.
 * </p>
 */
@Slf4j
@Service
public class Detector {
    private Gatherer gatherer;

    private ObjectMapper om;

    private ConfigStore store;

    /**
     * Detects configuration change and creates snapshot.
     */
    @Transactional
    public void check() {
        ConfigFile lastConfig = store.getLast();

        Map<String, Object> properties = gatherer.extract();

        try {
            String configValue = om.writeValueAsString(properties);

            if (lastConfig == null || !configValue.equals(lastConfig.getValue())) {
                log.info("New configuration detected.");

                ConfigFile config = new ConfigFile();
                config.setCreated(Instant.now());
                config.setValue(configValue);
                store.save(config);
            }

        } catch (JsonProcessingException e) {
            log.error("Can not convert current configuration to JSON.");
            throw new GeneralException(e);
        }
    }

    @Inject
    public void setGatherer(Gatherer gatherer) {
        this.gatherer = gatherer;
    }

    @Inject
    public void setOm(ObjectMapper om) {
        this.om = om;
    }

    @Inject
    public void setStore(ConfigStore store) {
        this.store = store;
    }
}
