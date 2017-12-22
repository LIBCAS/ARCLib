package cz.inqool.uas.report.index;

import cz.inqool.uas.index.IndexedDatedStore;

public class IndexedStoreImpl extends IndexedDatedStore<ReportTestEntity, QReportTestEntity, IndexedTestEntity> {
    public IndexedStoreImpl() {
        super(ReportTestEntity.class, QReportTestEntity.class, IndexedTestEntity.class);
    }

    @Override
    public IndexedTestEntity toIndexObject(ReportTestEntity obj) {
        IndexedTestEntity indexObject = super.toIndexObject(obj);
        indexObject.setStringAttribute(obj.getStringAttribute());

        return indexObject;
    }
}
