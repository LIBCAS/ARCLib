package cz.inqool.uas.config.change;

import cz.inqool.uas.store.DomainStore;
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
