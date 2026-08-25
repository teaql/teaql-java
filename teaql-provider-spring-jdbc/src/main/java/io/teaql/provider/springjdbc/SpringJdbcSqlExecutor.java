package io.teaql.provider.springjdbc;

import io.teaql.dataservice.sql.SqlExecutionAdapter;
import io.teaql.dataservice.sql.SqlRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SpringJdbcSqlExecutor implements SqlExecutionAdapter {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public SpringJdbcSqlExecutor(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(
                Objects.requireNonNull(jdbcTemplate.getJdbcTemplate().getDataSource(),
                        "A DataSource is required for transactional SQL execution")));
    }

    @Override
    public <T> List<T> query(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
        return jdbcTemplate.query(sql, params, rowMapper::mapRow);
    }

    @Override
    public <T extends io.teaql.core.Entity> List<T> query(
            String sql, Object[] params, io.teaql.core.CompiledRowMapper<T> rowMapper) {
        return jdbcTemplate.getJdbcOperations().query(
                sql,
                (resultSet, rowNumber) -> rowMapper.map(new SpringJdbcDataRow(resultSet)),
                params == null ? new Object[0] : params);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, Map<String, Object> params, SqlRowMapper<T> rowMapper) {
        return jdbcTemplate.queryForStream(sql, params, rowMapper::mapRow);
    }

    @Override
    public Stream<Map<String, Object>> queryForStream(String sql, Object[] params) {
        return jdbcTemplate.getJdbcOperations().queryForStream(sql, (rs, rowNum) -> {
            Map<String,Object> row = new java.util.HashMap<>();
            for (int i=1;i<=rs.getMetaData().getColumnCount();i++) row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(), rs.getObject(i));
            return row;
        }, params);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, Object> params) {
        return jdbcTemplate.queryForList(sql, params);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object[] params) {
        return jdbcTemplate.getJdbcOperations().queryForList(sql, params);
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Map<String, Object> params) {
        return jdbcTemplate.queryForMap(sql, params);
    }

    @Override
    public <T> T queryForObject(String sql, Map<String, Object> params, Class<T> requiredType) {
        return jdbcTemplate.queryForObject(sql, params, requiredType);
    }

    @Override
    public void execute(String sql) {
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    @Override
    public int update(String sql, Map<String, Object> params) {
        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int update(String sql, Object[] params) {
        return jdbcTemplate.getJdbcTemplate().update(sql, params);
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> paramsList) {
        return jdbcTemplate.getJdbcTemplate().batchUpdate(sql, paramsList);
    }

    @Override
    public void executeInTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private record SpringJdbcDataRow(ResultSet resultSet) implements io.teaql.core.DataRow {
        @Override
        public Object get(int columnIndex) {
            try {
                return resultSet.getObject(columnIndex);
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to read JDBC column " + columnIndex, exception);
            }
        }

        @Override
        public <V> V get(int columnIndex, Class<V> type) {
            try {
                return convert(type, resultSet.getObject(columnIndex));
            } catch (SQLException exception) {
                throw new IllegalStateException(
                        "Failed to read JDBC column " + columnIndex + " as " + type.getName(), exception);
            }
        }

        @SuppressWarnings("unchecked")
        private static <V> V convert(Class<V> type, Object value) {
            if (value == null || type.isInstance(value)) return (V) value;
            if (value instanceof Number number) {
                if (type == Long.class) return (V) Long.valueOf(number.longValue());
                if (type == Integer.class) return (V) Integer.valueOf(number.intValue());
                if (type == Double.class) return (V) Double.valueOf(number.doubleValue());
                if (type == Float.class) return (V) Float.valueOf(number.floatValue());
                if (type == Short.class) return (V) Short.valueOf(number.shortValue());
                if (type == Byte.class) return (V) Byte.valueOf(number.byteValue());
                if (type == java.math.BigDecimal.class) {
                    return (V) (number instanceof java.math.BigInteger integer
                            ? new java.math.BigDecimal(integer)
                            : java.math.BigDecimal.valueOf(number.doubleValue()));
                }
                if (type == Boolean.class) return (V) Boolean.valueOf(number.longValue() != 0);
            }
            if (type == java.time.LocalDateTime.class && value instanceof java.sql.Timestamp timestamp) {
                return (V) timestamp.toLocalDateTime();
            }
            if (type == java.time.LocalDateTime.class) {
                return (V) java.time.LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
            }
            if (type == java.time.LocalDate.class && value instanceof java.sql.Date date) {
                return (V) date.toLocalDate();
            }
            if (type == java.time.LocalDate.class && value instanceof java.sql.Timestamp timestamp) {
                return (V) timestamp.toLocalDateTime().toLocalDate();
            }
            if (type == java.time.LocalDate.class) {
                String text = String.valueOf(value);
                return (V) java.time.LocalDate.parse(text.substring(0, Math.min(10, text.length())));
            }
            if (type == java.time.LocalTime.class && value instanceof java.sql.Time time) {
                return (V) time.toLocalTime();
            }
            if (type == java.time.LocalTime.class && value instanceof java.sql.Timestamp timestamp) {
                return (V) timestamp.toLocalDateTime().toLocalTime();
            }
            if (type == java.time.LocalTime.class) {
                String text = String.valueOf(value);
                int separator = Math.max(text.indexOf('T'), text.indexOf(' '));
                return (V) java.time.LocalTime.parse(separator < 0 ? text : text.substring(separator + 1));
            }
            if (type == String.class) return (V) String.valueOf(value);
            throw new IllegalArgumentException(
                    "Cannot convert JDBC value " + value.getClass().getName() + " to " + type.getName());
        }
    }
}
