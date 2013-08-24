package internal.database;

import org.hibernate.dialect.MySQL5Dialect;

import java.sql.Types;

public class Mysql5CustomDialect extends MySQL5Dialect {
    public Mysql5CustomDialect() {
        registerColumnType(Types.BOOLEAN, "bit");
        registerColumnType(Types.VARCHAR, "longtext");
        registerColumnType(Types.VARCHAR, 16777215, "mediumtext");
        registerColumnType(Types.VARCHAR, 65535, "text");
        registerColumnType(Types.VARCHAR, 255, "varchar($l)");
    }
}
