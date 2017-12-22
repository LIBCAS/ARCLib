package cz.inqool.uas.db;

import java.sql.Types;

/**
 * MSSQL Dialect for nationalezed data types without specifying @Nationalized explicitly.
 */
public class SQLServer2012Dialect extends org.hibernate.dialect.SQLServer2012Dialect {
    private static final int NVARCHAR_MAX_LENGTH = 4000;

    public SQLServer2012Dialect() {
        registerColumnType(Types.CHAR, "nchar(1)");
        registerColumnType( Types.VARCHAR, NVARCHAR_MAX_LENGTH, "nvarchar($l)" );
        registerColumnType( Types.VARCHAR, "nvarchar(MAX)" );
        registerColumnType( Types.CLOB, "nvarchar(MAX)" );
    }
}
