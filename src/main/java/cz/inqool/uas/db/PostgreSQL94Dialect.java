package cz.inqool.uas.db;

import java.sql.Types;

/**
 * Dialect for PostgreSql 9.4 fixing support for NCHAR, NVARCHAR and NCLOB hibernate data types.
 * Those types can be used with @Nationalized annotation
 */
public class PostgreSQL94Dialect extends org.hibernate.dialect.PostgreSQL94Dialect {
    public PostgreSQL94Dialect() {
        super();

        registerColumnType( Types.NCHAR, "char(1)" );
        registerColumnType( Types.NVARCHAR, "varchar($l)" );
        registerColumnType( Types.NCLOB, "text" );
    }
}
