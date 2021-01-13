package cz.cas.lib.arclib.report;

import cz.cas.lib.arclib.index.solr.entity.IndexedReport;
import cz.cas.lib.core.index.solr.IndexedDomainStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class ReportStore extends IndexedDomainStore<Report, QReport, IndexedReport> {

    public ReportStore() { super(Report.class, QReport.class, IndexedReport.class); }

    @Getter
    private final String indexType = "report";

    @Override
    public IndexedReport toIndexObject(Report obj) {
        return super.toIndexObject(obj);
    }

}
