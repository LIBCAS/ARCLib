package cz.cas.lib.core.config.change;

import cz.cas.lib.arclib.domainbase.store.DomainStore;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigStore extends DomainStore<ConfigFile, QConfigFile> {

    public ConfigStore() {
        super(ConfigFile.class, QConfigFile.class);
    }

    public ConfigFile getLast() {
        QConfigFile qConfigFile = qObject();

        ConfigFile configFile = query()
                .select(qConfigFile)
                .orderBy(qConfigFile.created.desc())
                .limit(1)
                .fetchFirst();

        detachAll();

        return configFile;
    }
}
