package cz.inqool.uas.notification;

import cz.inqool.uas.index.IndexedDatedStore;
import cz.inqool.uas.index.IndexedStore;
import cz.inqool.uas.index.LabeledReference;
import org.springframework.stereotype.Repository;

/**
 * Implementation of {@link IndexedStore} for storing {@link Notification} and indexing {@link IndexedNotification}.
 */
@Repository
public class NotificationStore extends IndexedDatedStore<Notification, QNotification, IndexedNotification> {

    public NotificationStore() {
        super(Notification.class, QNotification.class, IndexedNotification.class);
    }

    @Override
    public IndexedNotification toIndexObject(Notification o) {
        IndexedNotification indexed = super.toIndexObject(o);

        indexed.setTitle(o.getTitle());

        indexed.setAuthor(new LabeledReference(o.getAuthorId(), o.getAuthorName()));
        indexed.setRecipient(new LabeledReference(o.getRecipientId(), o.getRecipientName()));

        indexed.setFlash(o.getFlash());
        indexed.setRead(o.getRead());
        indexed.setEmailing(o.getEmailing());

        return indexed;
    }
}
