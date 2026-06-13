package io.teaql.core.sqlite;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import io.teaql.core.utils.CollStreamUtil;
import io.teaql.core.utils.CaseInsensitiveMap;
import io.teaql.core.utils.NamingCase;
import io.teaql.core.utils.StrUtil;

import io.teaql.core.BaseEntity;
import io.teaql.core.Entity;
import io.teaql.core.RepositoryException;
import io.teaql.core.UserContext;
import io.teaql.core.log.Markers;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.core.sql.SQLColumn;
import io.teaql.core.sql.SQLRepository;




public class SQLiteRepository<T extends Entity> extends SQLRepository<T> {
    public SQLiteRepository(EntityDescriptor entityDescriptor, DataSource dataSource) {
        super(entityDescriptor, wrapDataSource(dataSource));
        registerExpressionParser(SQLiteAggrExpressionParser.class);
        registerExpressionParser(SQLiteParameterParser.class);
        registerExpressionParser(SQLiteTwoOperatorExpressionParser.class);
    }

    /**
     * Wraps the DataSource with SingleConnectionDataSource if it's not already wrapped.
     * This prevents SQLITE_BUSY errors caused by multiple concurrent connections.
     */
    private static DataSource wrapDataSource(DataSource dataSource) {
        if (dataSource instanceof SingleConnectionDataSource) {
            return dataSource;
        }

        String url = extractJdbcUrl(dataSource);
        if (url != null && url.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlite:")) {
            return new SingleConnectionDataSource(url);
        }

        return dataSource;
    }

    private static String extractJdbcUrl(DataSource dataSource) {
        for (String methodName : List.of("getJdbcUrl", "getUrl")) {
            String url = invokeStringGetter(dataSource, methodName);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private static String invokeStringGetter(DataSource dataSource, String methodName) {
        try {
            Method method = dataSource.getClass().getMethod(methodName);
            Object value = method.invoke(dataSource);
            if (value instanceof String) {
                return (String) value;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
        return null;
    }

    //name

    protected String getSchemaColumnNameFieldName() {
        return "name";
    }

    public static class ParsedType {
        public String dataType;
        public Integer length; // nullable

        @Override
        public String toString() {
            return "dataType='" + dataType + '\'' + ", length=" + length;
        }
    }

    public static ParsedType parseType(String type) {
        ParsedType result = new ParsedType();

        // Regex match: e.g. VARCHAR(100)
        Pattern pattern = Pattern.compile("([a-zA-Z]+)\\s*\\(?\\s*(\\d*)\\s*\\)?");
        Matcher matcher = pattern.matcher(type.trim());

        if (matcher.find()) {
            result.dataType = matcher.group(1);
            String len = matcher.group(2);
            if (len != null && !len.isEmpty()) {
                result.length = Integer.parseInt(len);
            } else {
                result.length = null;
            }
        } else {
            result.dataType = type.trim();
            result.length = null;
        }

        return result;
    }
    @Override
    protected void processParametersForLoadInternal(UserContext userContext,Map<String, Object> params) {
        //do nothing here
        params.entrySet().forEach(stringObjectEntry -> {
            if(stringObjectEntry.getValue() instanceof java.sql.Timestamp){
                replaceTimeStampParameter(userContext,params,stringObjectEntry.getKey(),(java.sql.Timestamp)stringObjectEntry.getValue());
            }
            if(stringObjectEntry.getValue() instanceof java.util.Date){
                replaceDateParameter(userContext,params,stringObjectEntry.getKey(),(java.util.Date)stringObjectEntry.getValue());
            }
        });

    }

    private void replaceDateParameter(UserContext userContext, Map<String, Object> params, String key, java.util.Date value) {

        params.put(key,formatDateToLocal(value));

    }

    private Object formatDateToLocal(Date value) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return  sdf.format(value);
    }

    private void replaceTimeStampParameter(UserContext userContext, Map<String, Object> params, String key, Timestamp value) {

        params.put(key,formatTimeStampToLocal(value));

    }

    private String formatTimeStampToLocal(Timestamp value) {
        if (value == null) {
            return null;
        }

        // Use system default timezone
        Instant instant = value.toInstant();
        ZonedDateTime localDateTime = instant.atZone(ZoneId.systemDefault());

        // Format as ISO8601 string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return localDateTime.format(formatter);
    }

    protected boolean isTypeMatch(String dbType, String type) {
        if (dbType.equalsIgnoreCase(type)) {
            return true;
        }
        return dbType.equalsIgnoreCase("real") && type.toLowerCase().startsWith("numeric(");
    }
    protected void alterColumn(UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns, SQLColumn column) {

        String backupTableName = String.format("%s_backup_%d", table, System.currentTimeMillis());
        // build explicit column list from the new schema
        StringBuilder colList = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) colList.append(", ");
            colList.append(columns.get(i).getColumnName());
        }
        String columnNames = colList.toString();

        String renameSql = String.format("ALTER TABLE %s RENAME TO %s", table, backupTableName);
        String insertSql = String.format("INSERT INTO %s (%s) SELECT %s FROM %s", table, columnNames, columnNames, backupTableName);
        String dropSql = String.format("DROP TABLE %s", backupTableName);

        if (ensureTableEnabled(ctx)) {
            super.executeUpdate(ctx, renameSql);
            super.createTable(ctx, table, columns);
            super.executeUpdate(ctx, insertSql);
            super.executeUpdate(ctx, dropSql);
        } else {
            ctx.info(renameSql + ";");
            super.createTable(ctx, table, columns);
            ctx.info(insertSql + ";");
            ctx.info(dropSql + ";");
        }
    }

    protected String calculateDBType(Map<String, Object> columnInfo) {
        String dataType = (String) columnInfo.get("type");
        ParsedType result = parseType(dataType.toLowerCase());
        String lowercaseType = result.dataType;
        switch (lowercaseType) {
            case "bigint":
                return "bigint";
            case "tinyint":
            case "boolean":
                return "boolean";
            case "varchar":
            case "character varying":
                return StrUtil.format("varchar({})", result.length);
            case "date":
                return "date";
            case "int":
            case "integer":
                return "integer";
            case "decimal":
            case "numeric":
            case "real":
                return "real";
            case "text":
                return "text";
            case "time without time zone":
                return "time";
            case "timestamp":
            case "timestamp without time zone":
                return "timestamp";
            default:
                throw new RepositoryException("unsupported type:" + lowercaseType);
        }
    }
    @Override
    protected String getSQLForUpdateWhenPrepareId() {
        return "SELECT current_level from {} WHERE type_name = '{}'";
    }

    @Override
    protected void ensureIndexAndForeignKey(UserContext ctx) {

        this.getEntityDescriptor().getOwnProperties().forEach(propertyDescriptor -> {

            if(needToCreateIndex(ctx,propertyDescriptor)){
                ensureIndex(ctx,propertyDescriptor);
            }
        });


    }

    private Set<String> indexNames;



    protected boolean needToCreateIndex(UserContext ctx,PropertyDescriptor propertyDescriptor){

        if(indexNames==null){
            indexNames=new HashSet<>();
            List<Map<String, Object>> mapList = queryForList("select name from sqlite_master  where type='index'", Collections.emptyMap());
            mapList.forEach(entry->{
                indexNames.add(entry.get("name").toString());
            });

        }
        return !indexNames.contains(indexName(propertyDescriptor));
    }


    protected void ensureIndex(UserContext ctx, PropertyDescriptor propertyDescriptor) {
        if(propertyDescriptor.isId()){
            return;
        }

        if(!isToCreateIndexFor(propertyDescriptor)){
            return;
        }

        String statement=StrUtil.format("CREATE INDEX IF NOT EXISTS {} ON {}({})",
                indexName(propertyDescriptor),
                tableName(getEntityDescriptor().getType()),
                columnName(propertyDescriptor.getName())
        );

        this.executeUpdate(ctx,statement);
    }

    protected boolean isToCreateIndexFor(PropertyDescriptor propertyDescriptor) {

        if(propertyDescriptor.isId()){
            return false;
        }
        if(propertyDescriptor.isIdentifier()){
            return true;
        }
        if(propertyDescriptor.getType() instanceof SimplePropertyType type){

            if(Date.class.isAssignableFrom(type.javaType())){
                return true;
            }
            if(BaseEntity.class.isAssignableFrom(type.javaType())){
                return true;
            }
            if(Number.class.isAssignableFrom(type.javaType())){
                return true;
            }
            if(Timestamp.class.isAssignableFrom(type.javaType())){
                return true;
            }
            if(LocalDateTime.class.isAssignableFrom(type.javaType())){
                return true;
            }
            return false;
        }



        return  false;



    }

    protected String indexName(PropertyDescriptor propertyDescriptor){
        return StrUtil.format("idx_{}_of_{}",NamingCase.toUnderlineCase(propertyDescriptor.getName()),NamingCase.toUnderlineCase(getEntityDescriptor().getType()));
    }
    protected String columnName(String propertyName){
        return NamingCase.toUnderlineCase(propertyName);
    }

    @Override
    protected String getSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        return StrUtil.wrapIfMissing(String.valueOf(value), "'", "'");
    }
    @Override
    protected void ensure(
            UserContext ctx, List<Map<String, Object>> tableInfo, String table, List<SQLColumn> columns) {
        tableInfo = CollStreamUtil.toList(tableInfo, CaseInsensitiveMap::new);
        super.ensure(ctx, tableInfo, table, columns);
    }

    // protected String getPureColumnName(String columnName) {
    //     return StrUtil.unWrap(columnName, '`');
    // }
    /*SELECT
    cid,
    name,
    type AS data_type,  -- alias added here
    notnull,
    dflt_value,
    pk
FROM PRAGMA_table_info('table_name')*/
    @Override
    protected String findTableColumnsSql(DataSource dataSource, String table) {
        try (Connection connection = dataSource.getConnection()) {
            //String databaseName = connection.getCatalog();
            return String.format(
                    //"SELECT name FROM sqlite_master WHERE type='table' AND name='%s'",
                    "PRAGMA table_info(%s)",
                    table);
        }
        catch (SQLException pE) {
            throw new RuntimeException(pE);
        }
    }

    /**
     * Factory method to create SQLiteRepository with a URL.
     * Uses SingleConnectionDataSource to prevent SQLITE_BUSY errors.
     *
     * @param entityDescriptor the entity descriptor
     * @param url the SQLite database URL (e.g., "jdbc:sqlite:database.db")
     * @return a new SQLiteRepository instance
     */
    public static <T extends Entity> SQLiteRepository<T> create(EntityDescriptor entityDescriptor, String url) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url);
        return new SQLiteRepository<>(entityDescriptor, dataSource);
    }

    public static void main(String[] args) {
        //1762185600000, 1762271999999
        Date date=new Date();
        date.setTime(1762271999999l);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        System.out.println(sdf.format(date));


    }
}
