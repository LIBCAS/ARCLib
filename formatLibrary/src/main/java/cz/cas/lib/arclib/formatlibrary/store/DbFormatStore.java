package cz.cas.lib.arclib.formatlibrary.store;

import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.QFormat;
import cz.cas.lib.arclib.formatlibrary.domain.QRisk;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DbFormatStore extends DatedStore<Format, QFormat> implements FormatStore {
    public DbFormatStore() {
        super(Format.class, QFormat.class);
    }

    @Override
    public Format create(Format entity) {
        return save(entity);
    }

    @Override
    public Format update(Format entity) {
        return save(entity);
    }

    public Format findByFormatId(Integer formatId) {
        Format format1 = query().select(qObject())
                .where(qObject().formatId.eq(formatId))
                .fetchFirst();
        detachAll();
        return format1;
    }

    @Override
    public List<Format> findFormatsOfRisk(String riskId) {
        QRisk risk = QRisk.risk;
        List<Format> fetch = query().select(qObject()).
                from(qObject()).
                innerJoin(qObject().relatedRisks, risk).
                where(risk.id.eq(riskId)).
                fetch();
        detachAll();
        return fetch;
    }
}
