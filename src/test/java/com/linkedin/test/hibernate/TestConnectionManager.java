package com.linkedin.test.hibernate;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.JDBCException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import org.junit.runner.JUnitCore;

/**
 * Test org.hibernate.jdbc.ConnectionManager.
 * 
 * @author <a href="mailto:jkristian@linkedin.com">John Kristian</a>
 */
public class TestConnectionManager extends TestCase {

    public static void main(String args[]) {
        JUnitCore.main(TestConnectionManager.class.getName());
    }

    private static final Log log = LogFactory.getLog(TestConnectionManager.class);

    public TestConnectionManager(String name) {
        super(name);
    }

    /** Handle exceptions when beginTransaction can't get a database connection. */
    public void testBeginFailed() {
        SessionFactory sessionFactory = new Configuration().buildSessionFactory();
        try {
            Session session = sessionFactory.openSession();
            try {
                Transaction tx = session.beginTransaction();
                tx.commit();
                fail("expected TransactionException");
            } catch (TransactionException expected) {
                log.debug("session.beginTransaction", expected);
            } finally {
                try {
                    session.close();
                    fail("expected JDBCException");
                } catch (JDBCException expected) {
                    log.debug("session.close", expected);
                }
                MockConnectionProvider.verify();
            }
        } catch (AssertionError e) {
            log.error(e);
            throw e;
        } finally {
            sessionFactory.close();
        }
    }
}
