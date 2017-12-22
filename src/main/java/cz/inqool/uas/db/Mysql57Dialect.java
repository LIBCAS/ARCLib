package cz.inqool.uas.db;

import java.sql.Types;

/**
 * Dialect for Mysql 5.7 fixing support for NCHAR, NVARCHAR and NCLOB hibernate data types.
 * Those types can be used with @Nationalized annotation
 */
public class Mysql57Dialect extends org.hibernate.dialect.MySQL57InnoDBDialect {
    public Mysql57Dialect() {
        super();

        registerColumnType( Types.NVARCHAR, "longtext" );
        registerColumnType( Types.NVARCHAR, 65535, "varchar($l)" );
        registerColumnType( Types.LONGNVARCHAR, "longtext" );

        registerColumnType( Types.NCHAR, "char(1)" );
        registerColumnType( Types.NCLOB, "longtext" );

    }
}
