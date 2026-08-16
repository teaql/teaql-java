package io.teaql.sqlite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;

import io.teaql.core.sqlite.SingleConnectionDataSource;

public class SingleConnectionDataSourceTest {

    @Test
    public void callerCloseKeepsSharedConnectionUntilDataSourceCloses() throws Exception {
        SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource("jdbc:sqlite::memory:");

        Connection first = dataSource.getConnection();
        first.close();

        assertFalse(first.isClosed());
        assertSame(first, dataSource.getConnection());

        dataSource.close();
        assertTrue(first.isClosed());
        assertThrows(SQLException.class, dataSource::getConnection);
    }
}
