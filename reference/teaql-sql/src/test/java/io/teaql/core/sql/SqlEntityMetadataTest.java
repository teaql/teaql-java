package io.teaql.core.sql;

import io.teaql.core.meta.EntityDescriptor;
import io.teaql.core.meta.PropertyDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
public class SqlEntityMetadataTest {

    @Mock
    private EntityDescriptor mockDescriptor;

    @Test
    public void testInitSQLMeta() {
        // Arrange
        // Create a mock that implements both PropertyDescriptor and SQLProperty
        PropertyDescriptor mockProperty = mock(PropertyDescriptor.class, withSettings().extraInterfaces(SQLProperty.class));
        SQLProperty mockSqlProperty = (SQLProperty) mockProperty;

        when(mockDescriptor.getType()).thenReturn("TestUser");
        when(mockDescriptor.getProperties()).thenReturn(Collections.singletonList(mockProperty));
        
        SQLColumn mockColumn = new SQLColumn("test_table", "id");
        mockColumn.setType("VARCHAR(255)");
        
        when(mockSqlProperty.columns()).thenReturn(Collections.singletonList(mockColumn));
        when(mockProperty.isId()).thenReturn(true);
        when(mockProperty.getOwner()).thenReturn(mockDescriptor);

        // Act
        SqlEntityMetadata metadata = new SqlEntityMetadata(mockDescriptor);

        // Assert
        assertEquals("TestUser", metadata.getTypes().get(0));
        assertEquals(1, metadata.getPrimaryTableNames().size());
        assertEquals("test_table", metadata.getPrimaryTableNames().get(0));
        assertEquals("test_table", metadata.getThisPrimaryTableName());
        assertTrue(metadata.getAllTableNames().contains("test_table"));
        assertTrue(metadata.getAuxiliaryTableNames().isEmpty());
    }
}
