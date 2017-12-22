package cz.inqool.uas.store.revision;


import cz.inqool.uas.util.ApplicationContextUtils;
import org.hibernate.envers.RevisionType;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.HashSet;

import static cz.inqool.uas.util.Utils.unwrap;

public class RevisionListener implements org.hibernate.envers.EntityTrackingRevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        Revision revision = (Revision) revisionEntity;

        ApplicationContext context = ApplicationContextUtils.getApplicationContext();

        // this could be null if we are in test and not in spring
        if (context != null) {
            UserDetails user = unwrap(context.getBean(UserDetails.class));

            if (user != null) {
                revision.setAuthor(user.getUsername());
            }
        }
    }

    @Override
    public void entityChanged(Class entityClass, String entityName, Serializable entityId, RevisionType revisionType, Object revisionEntity) {
        Revision revision = (Revision) revisionEntity;

        if (revision.getItems() == null) {
            revision.setItems(new HashSet<>());
        }

        revision.getItems().add(new RevisionItem((String)entityId, revisionType));
    }
}
