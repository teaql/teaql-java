package io.teaql.core.sqlite;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
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
            connectionProxy = createConnectionProxy(connection);
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

    private static Connection createConnectionProxy(Connection target) {
        return (Connection)
                Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[] {Connection.class},
                        (proxy, method, args) -> {
                            if ("close".equals(method.getName())) {
                                return null;
                            }
                            try {
                                return method.invoke(target, args);
                            } catch (InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                        });
    }
}
