package com.linkedin.test.hibernate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;

/**
 * Simulates a connection pool that's temporarily exhausted.
 * 
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class MockConnectionProvider implements ConnectionProvider {

    private static final Log log = LogFactory.getLog(MockConnectionProvider.class);
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
        // EasyMock.expect(connection.getAutoCommit()).andReturn(false).times(0, 1);
        EasyMock.expect(connection.getAutoCommit()).andAnswer(new LogStackAndReturn<Boolean>(false)).times(0, 1);
        // EasyMock.expect(connection.getWarnings()).andReturn(null).times(0, 1);
        // connection.clearWarnings(); EasyMock.expectLastCall().times(0, 1);
        connection.close();
        EasyMock.expectLastCall();
        EasyMock.replay(connection);
        mocks.add(connection);
        if (log.isTraceEnabled()) {
            connection = (Connection) Proxy.newProxyInstance(connection.getClass().getClassLoader(),
                    new Class[] { Connection.class }, new LogCall(connection));
        }
        return connection;
    }

    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    public void close() throws HibernateException {
        log.trace("close");
    }

    public static void verify() {
        EasyMock.verify(mocks.toArray());
    }

    /** Logs the current stack and then returns a given value. */
    private static class LogStackAndReturn<T> implements IAnswer<T> {

        private final T _answer;

        LogStackAndReturn(T answer) {
            _answer = answer;
        }

        public T answer() throws Throwable {
            log.debug("stack trace", new Exception());
            return _answer;
        }
    }

    /** Logs the method name and the returned value. */
    private static class LogCall implements InvocationHandler {

        private final Object _subject;

        LogCall(Object subject) {
            _subject = subject;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            log.trace("-> " + name);
            String valediction = "<- " + name + " ";
            try {
                Object result = method.invoke(_subject, args);
                valediction += "returned " + result;
                return result;
            } catch (InvocationTargetException e) {
                Throwable t = e.getCause();
                valediction += "threw " + t;
                throw t;
            } catch (Throwable t) {
                valediction += "threw " + t;
                throw t;
            } finally {
                log.trace(valediction);
            }
        }
    }

}
