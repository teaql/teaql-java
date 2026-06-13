package io.teaql.core.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.MDC;
import io.teaql.core.utils.ObjectUtil;

public class TracedDataSource {

    public static DataSource wrap(DataSource original) {
        if (original == null) {
            return null;
        }
        return (DataSource) Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            new DataSourceInvocationHandler(original)
        );
    }

    private static class DataSourceInvocationHandler implements InvocationHandler {
        private final DataSource original;

        public DataSourceInvocationHandler(DataSource original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = method.invoke(original, args);
            if (result instanceof Connection) {
                return wrapConnection((Connection) result);
            }
            return result;
        }
    }

    private static Connection wrapConnection(Connection original) {
        List<Class<?>> interfaces = new ArrayList<>();
        interfaces.add(Connection.class);
        for (Class<?> iface : original.getClass().getInterfaces()) {
            if (!interfaces.contains(iface)) {
                interfaces.add(iface);
            }
        }
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            interfaces.toArray(new Class<?>[0]),
            new ConnectionInvocationHandler(original)
        );
    }

    private static class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection original;

        public ConnectionInvocationHandler(Connection original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (("prepareStatement".equals(name) || "prepareCall".equals(name)) 
                    && args != null && args.length > 0 && args[0] instanceof String) {
                args[0] = prependComment((String) args[0]);
            }
            Object result = method.invoke(original, args);
            if (result instanceof PreparedStatement) {
                return wrapStatement((PreparedStatement) result, PreparedStatement.class);
            } else if (result instanceof CallableStatement) {
                return wrapStatement((CallableStatement) result, CallableStatement.class);
            } else if (result instanceof Statement) {
                return wrapStatement((Statement) result, Statement.class);
            }
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Statement> T wrapStatement(T original, Class<T> mainInterface) {
        List<Class<?>> interfaces = new ArrayList<>();
        interfaces.add(mainInterface);
        if (original instanceof CallableStatement && !interfaces.contains(CallableStatement.class)) {
            interfaces.add(CallableStatement.class);
        }
        if (original instanceof PreparedStatement && !interfaces.contains(PreparedStatement.class)) {
            interfaces.add(PreparedStatement.class);
        }
        if (original instanceof Statement && !interfaces.contains(Statement.class)) {
            interfaces.add(Statement.class);
        }
        for (Class<?> iface : original.getClass().getInterfaces()) {
            if (!interfaces.contains(iface)) {
                interfaces.add(iface);
            }
        }
        return (T) Proxy.newProxyInstance(
            Statement.class.getClassLoader(),
            interfaces.toArray(new Class<?>[0]),
            new StatementInvocationHandler(original)
        );
    }

    private static class StatementInvocationHandler implements InvocationHandler {
        private final Statement original;

        public StatementInvocationHandler(Statement original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (("execute".equals(name) || "executeQuery".equals(name) || "executeUpdate".equals(name) || "addBatch".equals(name))
                    && args != null && args.length > 0 && args[0] instanceof String) {
                args[0] = prependComment((String) args[0]);
            }
            return method.invoke(original, args);
        }
    }

    public static String prependComment(String sql) {
        String comment = MDC.get("comment");
        String userId = MDC.get("TRACE_USER_ID");
        
        String finalComment = null;
        if (ObjectUtil.isNotEmpty(userId) && ObjectUtil.isNotEmpty(comment)) {
            finalComment = "[" + userId + "] " + comment;
        } else if (ObjectUtil.isNotEmpty(userId)) {
            finalComment = "[" + userId + "]";
        } else if (ObjectUtil.isNotEmpty(comment)) {
            finalComment = comment;
        }
        
        if (ObjectUtil.isNotEmpty(finalComment)) {
            String escaped = finalComment.replace("*/", "* /");
            return "/* " + escaped + " */ " + sql;
        }
        return sql;
    }
}
