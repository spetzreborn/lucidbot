package internal.database;

import org.hibernate.dialect.MySQL5Dialect;

import java.sql.Types;

public class Mysql5BitBooleanDialect extends MySQL5Dialect {
    public Mysql5BitBooleanDialect() {
        registerColumnType(Types.BOOLEAN, "bit");
    }
}
