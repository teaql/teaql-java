package io.teaql.businessid.jdbc;

import io.teaql.core.Entity;
import io.teaql.core.UserContext;
import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import io.teaql.core.meta.SimplePropertyType;
import io.teaql.core.sql.portable.TeaQLDatabase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class JdbcBusinessIdGeneratorTest {

    private Connection connection;
    private JdbcBusinessIdGenerator generator;
    private EntityDescriptor entityDesc;
    private PropertyDescriptor propDesc;
    private UserContext dummyContext;
    private Entity dummyEntity;

    @Before
    public void setUp() throws Exception {
        // Use in-memory SQLite for testing
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        
        TeaQLDatabase testDatabase = new TeaQLDatabase() {
            @Override
            public List<Map<String, Object>> query(String sql, Object[] args) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            stmt.setObject(i + 1, args[i]);
                        }
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Map<String, Object>> result = new ArrayList<>();
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                            }
                            result.add(row);
                        }
                        return result;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int executeUpdate(String sql, Object[] args) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            stmt.setObject(i + 1, args[i]);
                        }
                    }
                    return stmt.executeUpdate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
                return new int[0];
            }

            @Override
            public void execute(String sql) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.execute();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void executeInTransaction(Runnable action) {
                try {
                    boolean autoCommit = connection.getAutoCommit();
                    connection.setAutoCommit(false);
                    try {
                        action.run();
                        connection.commit();
                    } catch (Exception e) {
                        connection.rollback();
                        throw new RuntimeException(e);
                    } finally {
                        connection.setAutoCommit(autoCommit);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public List<Map<String, Object>> getTableColumns(String tableName) {
                return new ArrayList<>();
            }
        };

        generator = new JdbcBusinessIdGenerator(testDatabase);

        entityDesc = new EntityDescriptor();
        entityDesc.setType("Order");
        
        propDesc = new PropertyDescriptor("orderNumber", new SimplePropertyType(String.class));
        
        dummyContext = null;
        dummyEntity = null;
    }
    
    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void testGenerateBusinessId_Success() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 6");

        String id1 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        String id2 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        Assert.assertEquals("ORD" + dateStr + "000001", id1);
        Assert.assertEquals("ORD" + dateStr + "000002", id2);
    }
    
    @Test
    public void testGenerateBusinessId_MultipleKeys() {
        propDesc.getAdditionalInfo().put("business_id_rule", "ORD, 4");
        String id1 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, propDesc);
        
        PropertyDescriptor anotherProp = new PropertyDescriptor("logisticsNumber", new SimplePropertyType(String.class));
        anotherProp.getAdditionalInfo().put("business_id_rule", "LOG, 4");
        String id2 = generator.generateBusinessId(dummyContext, dummyEntity, entityDesc, anotherProp);
        
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        Assert.assertEquals("ORD" + dateStr + "0001", id1);
        Assert.assertEquals("LOG" + dateStr + "0001", id2);
    }
}
