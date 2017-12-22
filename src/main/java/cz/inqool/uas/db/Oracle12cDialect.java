package cz.inqool.uas.db;

import java.sql.Types;

/**
 * Oracle Dialect for nationalezed data types without specifying @Nationalized explicitly.
 */
public class Oracle12cDialect extends org.hibernate.dialect.Oracle12cDialect {
    private static final int NVARCHAR_MAX_LENGTH = 4000;

    public Oracle12cDialect() {
        registerColumnType(Types.CHAR, "nchar2(1)");
        registerColumnType( Types.VARCHAR, NVARCHAR_MAX_LENGTH, "nvarchar2($l)" );
        registerColumnType( Types.VARCHAR, "nvarchar2(MAX)" );
        registerColumnType( Types.CLOB, "nclob" );
    }
}
