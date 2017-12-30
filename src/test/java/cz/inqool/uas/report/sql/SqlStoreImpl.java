package cz.inqool.uas.report.sql;


import cz.inqool.uas.store.DomainStore;

public class SqlStoreImpl extends DomainStore<SqlTestEntity, QSqlTestEntity> {

    public SqlStoreImpl() {
        super(SqlTestEntity.class, QSqlTestEntity.class);
    }
}
