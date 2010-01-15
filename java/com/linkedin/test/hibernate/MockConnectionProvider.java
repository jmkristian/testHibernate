package com.linkedin.test.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import org.easymock.EasyMock;
import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;

/**
* Simulates a connection pool that's temporarily exhausted.
* @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
*/
public class MockConnectionProvider implements ConnectionProvider {

    private static final Collection<Object> mocks = new ArrayList<Object>();

    public void configure(Properties props) throws HibernateException {
    }

    public boolean supportsAggressiveRelease() {
        return true;
    }

    public Connection getConnection() throws SQLException {
        SQLException poolExhausted = new SQLException("pool exhausted");
        Connection connection = EasyMock.createStrictMock(Connection.class);
        EasyMock.expect(connection.getAutoCommit()).andThrow(poolExhausted);
        {
            // This works: EasyMock.expect(connection.isClosed()).andReturn(true);
            // This works: EasyMock.expect(connection.isClosed()).andReturn(false);
            // But this causes a leak:
            EasyMock.expect(connection.isClosed()).andThrow(poolExhausted);
            EasyMock.expect(connection.isClosed()).andReturn(false);
        }
        EasyMock.expect(connection.getAutoCommit()).andReturn(false);
        connection.close(); EasyMock.expectLastCall();
        EasyMock.replay(connection);
        mocks.add(connection);
        return connection;
    }

    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    public void close() throws HibernateException {
    }

    public static void verify() {
        EasyMock.verify(mocks.toArray());
    }

}