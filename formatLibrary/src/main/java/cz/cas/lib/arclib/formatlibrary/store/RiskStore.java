package cz.cas.lib.arclib.formatlibrary.store;

import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.formatlibrary.domain.QRisk;
import cz.cas.lib.arclib.formatlibrary.domain.Risk;
import org.springframework.stereotype.Repository;

@Repository
public class RiskStore
        extends DatedStore<Risk, QRisk> {
    public RiskStore() {
        super(Risk.class, QRisk.class);
    }
}
