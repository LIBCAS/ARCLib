package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.QUserSettings;
import cz.cas.lib.arclib.domain.UserSettings;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class UserSettingsStore extends DatedStore<UserSettings, QUserSettings> {
    public UserSettingsStore() {
        super(UserSettings.class, QUserSettings.class);
    }

    public UserSettings findSettingsOfUser(@NonNull String userId) {
        QUserSettings qUserSettings = qObject();
        UserSettings entity = query().select(qUserSettings).where(qUserSettings.belongsTo.id.eq(userId)).orderBy(qUserSettings.created.desc()).fetchFirst();
        detachAll();
        return entity;
    }
}
