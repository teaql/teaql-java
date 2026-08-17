package io.teaql.core.sqlite;

import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * A SQLite-oriented data source that shares one connection while suppressing
 * caller-initiated {@link Connection#close()} calls.
 *
 * <p>Close this data source to release the underlying connection.
 */
public final class SingleConnectionDataSource implements DataSource, AutoCloseable {
    private final String url;
    private Connection connection;
    private Connection connectionProxy;
    private boolean closed;

    public SingleConnectionDataSource(String url) {
        this.url = url;
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("DataSource is closed");
        }
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url);
            connectionProxy = new CloseSuppressingConnection(connection);
        }
        return connectionProxy;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("SQLite JDBC does not expose a parent logger");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Cannot unwrap " + getClass().getName() + " as " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    @Override
    public synchronized void close() throws SQLException {
        closed = true;
        if (connection != null) {
            connection.close();
            connection = null;
            connectionProxy = null;
        }
    }

    /** Static JDBC decorator, deliberately avoiding reflection for native-image compatibility. */
    private static final class CloseSuppressingConnection implements Connection {
        private final Connection target;

        private CloseSuppressingConnection(Connection target) {
            this.target = target;
        }

        @Override public Statement createStatement() throws SQLException { return target.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return target.prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return target.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return target.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { target.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return target.getAutoCommit(); }
        @Override public void commit() throws SQLException { target.commit(); }
        @Override public void rollback() throws SQLException { target.rollback(); }
        @Override public void close() { /* The owning data source closes the physical connection. */ }
        @Override public boolean isClosed() throws SQLException { return target.isClosed(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return target.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { target.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return target.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { target.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return target.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { target.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return target.getTransactionIsolation(); }
        @Override public SQLWarning getWarnings() throws SQLException { return target.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { target.clearWarnings(); }
        @Override public Statement createStatement(int type, int concurrency) throws SQLException { return target.createStatement(type, concurrency); }
        @Override public PreparedStatement prepareStatement(String sql, int type, int concurrency) throws SQLException { return target.prepareStatement(sql, type, concurrency); }
        @Override public CallableStatement prepareCall(String sql, int type, int concurrency) throws SQLException { return target.prepareCall(sql, type, concurrency); }
        @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return target.getTypeMap(); }
        @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException { target.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { target.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return target.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return target.setSavepoint(); }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return target.setSavepoint(name); }
        @Override public void rollback(Savepoint savepoint) throws SQLException { target.rollback(savepoint); }
        @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { target.releaseSavepoint(savepoint); }
        @Override public Statement createStatement(int type, int concurrency, int holdability) throws SQLException { return target.createStatement(type, concurrency, holdability); }
        @Override public PreparedStatement prepareStatement(String sql, int type, int concurrency, int holdability) throws SQLException { return target.prepareStatement(sql, type, concurrency, holdability); }
        @Override public CallableStatement prepareCall(String sql, int type, int concurrency, int holdability) throws SQLException { return target.prepareCall(sql, type, concurrency, holdability); }
        @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return target.prepareStatement(sql, autoGeneratedKeys); }
        @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return target.prepareStatement(sql, columnIndexes); }
        @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return target.prepareStatement(sql, columnNames); }
        @Override public Clob createClob() throws SQLException { return target.createClob(); }
        @Override public Blob createBlob() throws SQLException { return target.createBlob(); }
        @Override public NClob createNClob() throws SQLException { return target.createNClob(); }
        @Override public SQLXML createSQLXML() throws SQLException { return target.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return target.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws SQLClientInfoException { target.setClientInfo(name, value); }
        @Override public void setClientInfo(Properties properties) throws SQLClientInfoException { target.setClientInfo(properties); }
        @Override public String getClientInfo(String name) throws SQLException { return target.getClientInfo(name); }
        @Override public Properties getClientInfo() throws SQLException { return target.getClientInfo(); }
        @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { return target.createArrayOf(typeName, elements); }
        @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { return target.createStruct(typeName, attributes); }
        @Override public void setSchema(String schema) throws SQLException { target.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return target.getSchema(); }
        @Override public void abort(Executor executor) throws SQLException { target.abort(executor); }
        @Override public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException { target.setNetworkTimeout(executor, milliseconds); }
        @Override public int getNetworkTimeout() throws SQLException { return target.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) return iface.cast(this);
            return target.unwrap(iface);
        }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return iface.isInstance(this) || target.isWrapperFor(iface);
        }
    }
}
