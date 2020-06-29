package cz.cas.lib.arclib.report;

import cz.cas.lib.arclib.domainbase.store.DomainStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class ReportStore extends DomainStore<Report, QReport> {
    public ReportStore() {
        super(Report.class, QReport.class);
    }
}
