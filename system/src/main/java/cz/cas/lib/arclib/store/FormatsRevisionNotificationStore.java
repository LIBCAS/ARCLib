package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.FormatsRevisionNotification;
import cz.cas.lib.arclib.domain.QFormatsRevisionNotification;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class FormatsRevisionNotificationStore
        extends DatedStore<FormatsRevisionNotification, QFormatsRevisionNotification> {
    public FormatsRevisionNotificationStore() {
        super(FormatsRevisionNotification.class, QFormatsRevisionNotification.class);
    }
}
