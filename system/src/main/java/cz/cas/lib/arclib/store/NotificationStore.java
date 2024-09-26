package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.notification.Notification;
import cz.cas.lib.arclib.domain.notification.NotificationElement;
import cz.cas.lib.arclib.domain.notification.QNotification;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.report.Report;
import cz.cas.lib.arclib.report.ReportStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class NotificationStore extends DatedStore<Notification, QNotification> {
    private IndexedFormatStore formatStore;
    private ReportStore reportStore;

    public NotificationStore() {
        super(Notification.class, QNotification.class);
    }


    /**
     * Save instance to DB. Perform validity check for IDs of related entities.
     */
    @Override
    public Notification save(Notification entity) {
        identifyRelatedEntities(entity);
        return super.save(entity);
    }

    /**
     * Save instance to DB. Perform validity check for IDs of related entities.
     */
    @Override
    public Collection<? extends Notification> save(Collection<? extends Notification> entities) {
        entities.forEach(this::identifyRelatedEntities);
        return super.save(entities);
    }

    /**
     * Method will overwrite entities that do not meet requirements.
     * Notification of FORMAT_REVISION type must contain only valid {@link Format} entities.
     * The same applies for REPORT type and {@link Report} entities.
     */
    public void identifyRelatedEntities(Notification entity) {
        List<String> ids = entity.obtainRelatedEntitiesIds();
        switch (entity.getType()) {
            case FORMAT_REVISION:
                List<Format> formats = formatStore.findAllInList(ids);
                entity.setRelatedEntities(formats.stream()
                        .map(format -> new NotificationElement(format.getId(), format.getFormatName()))
                        .collect(Collectors.toList())
                );
                break;
            case REPORT:
                List<Report> reports = reportStore.findAllInList(ids);
                entity.setRelatedEntities(reports.stream()
                        .map(report -> new NotificationElement(report.getId(), report.getName()))
                        .collect(Collectors.toList())
                );
                break;
        }
    }


    @Autowired
    public void setFormatStore(IndexedFormatStore formatStore) {
        this.formatStore = formatStore;
    }

    @Autowired
    public void setReportStore(ReportStore reportStore) {
        this.reportStore = reportStore;
    }
}
