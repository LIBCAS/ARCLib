package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.export.ExportTemplate;
import cz.cas.lib.arclib.domain.export.QExportTemplate;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.List;

@Repository
public class ExportTemplateStore extends NamedStore<ExportTemplate, QExportTemplate> {
    public ExportTemplateStore() {
        super(ExportTemplate.class, QExportTemplate.class);
    }

    @Transactional
    public List<ExportTemplate> findByProducerId(String producerId) {
        QExportTemplate exportTemplate = qObject();

        List<ExportTemplate> fetch = query()
                .select(exportTemplate)
                .where(exportTemplate.producer.id.eq(producerId))
                .where(exportTemplate.deleted.isNull())
                .fetch();

        detachAll();
        return fetch;
    }

}
