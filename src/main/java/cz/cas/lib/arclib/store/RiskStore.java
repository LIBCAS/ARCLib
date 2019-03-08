package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.preservationPlanning.QRisk;
import cz.cas.lib.arclib.domain.preservationPlanning.Risk;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class RiskStore
        extends DatedStore<Risk, QRisk> {
    public RiskStore() {
        super(Risk.class, QRisk.class);
    }
}
