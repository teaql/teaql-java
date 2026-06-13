package io.teaql.core.sqlite;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * A DataSource wrapper that returns the same connection for all callers.
 * This prevents SQLITE_BUSY errors caused by multiple concurrent connections.
 *
 * Usage:
 * SingleConnectionDataSource ds = new SingleConnectionDataSource("jdbc:sqlite:database.db");
 */
public class SingleConnectionDataSource implements DataSource {
    private final String url;
    private Connection connection;
    private Connection connectionProxy;
    private boolean closed = false;

    public SingleConnectionDataSource(String url) {
        this.url = url;
    }

    @Override
    public Connection getConnection() throws SQLException {
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
        throw new SQLFeatureNotSupportedException("Not implemented");
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not implemented");
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException("Not implemented");
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Not implemented");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        }
        throw new SQLException("DataSource of type " + getClass().getName() + " cannot be unwrapped as " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isAssignableFrom(getClass());
    }

    public void close() {
        closed = true;
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    private Connection createConnectionProxy(Connection target) {
        return (Connection)
                Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class[] {Connection.class},
                        new SuppressCloseInvocationHandler(target));
    }

    private static class SuppressCloseInvocationHandler implements InvocationHandler {
        private final Connection target;

        private SuppressCloseInvocationHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                return null;
            }
            return method.invoke(target, args);
        }
    }
}
