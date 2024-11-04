package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.UserSettings;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.UserSettingsStore;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserSettingsService {

    private UserSettingsStore store;
    private UserDetails userDetails;


    @Transactional
    public UserSettings save(@NonNull String settings) {
        UserSettings userSettings = get();
        userSettings.setSettings(settings);
        return store.save(userSettings);
    }

    @Transactional
    public UserSettings get() {
        UserSettings settingsOfUser = store.findSettingsOfUser(userDetails.getId());
        if (settingsOfUser == null) {
            return store.save(new UserSettings("{}", new User(userDetails.getId())));
        } else {
            return settingsOfUser;
        }
    }

    @Autowired
    public void setUserSettingsStore(UserSettingsStore userSettingsStore) {
        this.store = userSettingsStore;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
