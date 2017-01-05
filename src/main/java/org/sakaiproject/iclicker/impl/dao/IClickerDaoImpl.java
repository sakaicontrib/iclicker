/**
 * Copyright (c) 2009 i>clicker (R) <http://www.iclicker.com/dnn/>
 *
 * This file is part of i>clicker Sakai integrate.
 *
 * i>clicker Sakai integrate is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * i>clicker Sakai integrate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with i>clicker Sakai integrate.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sakaiproject.iclicker.impl.dao;

import org.apache.commons.lang.StringUtils;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.sakaiproject.genericdao.api.search.Search;
import org.sakaiproject.genericdao.hibernate.HibernateGeneralGenericDao;
import org.sakaiproject.iclicker.api.dao.IClickerDao;
import org.sakaiproject.iclicker.model.dao.ClickerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Implementations of any specialized DAO methods from the specialized DAO that allows the developer to extend the functionality of the generic dao package, this handles all data persistence for the
 * application
 */
public class IClickerDaoImpl extends HibernateGeneralGenericDao implements IClickerDao {

    private static final Logger log = LoggerFactory.getLogger(IClickerDaoImpl.class);

    public void init() {
        log.debug("init");
        try {
            // attempt to alter the iclicker_registration table
            Dialect dialect = ((SessionFactoryImplementor) getSessionFactory()).getDialect();
            if (dialect instanceof MySQLDialect) {
                getSession().createSQLQuery(
                                "ALTER TABLE `iclicker_registration` CHANGE COLUMN `clickerId` `clickerId` VARCHAR(16) NOT NULL")
                                .executeUpdate();
                log.info("Updated the iclicker_registration table in MYSQL");
            } else if (dialect instanceof Oracle10gDialect) {
                getSession().createSQLQuery("ALTER TABLE iclicker_registration MODIFY clickerId VARCHAR2(16)")
                                .executeUpdate();
                log.info("Updated the iclicker_registration table in ORACLE");
            }
        } catch (Exception e) {
            log.error("Unable to alter i>clicker iclicker_registration table, you will need to manually alter the clickerId column to 16 chars long (from 8 chars)",
                            e);
        }

    }

    /**
     * Allows a lock to be obtained that is system wide, this is primarily for ensuring something runs on a single server only in a cluster<br/>
     * <b>NOTE:</b> This intentionally returns a null on failure rather than an exception since exceptions will cause a rollback which makes the current session effectively dead, this also makes it
     * impossible to control the failure so instead we return null as a marker
     *
     * @param lockId the name of the lock which we are seeking
     * @param executerId a unique id for the holder of this lock (normally a server id)
     * @param timePeriod the length of time (in milliseconds) that the lock should be valid for, set this very low for non-repeating processes (the length of time the process should take to run) and
     *            the length of the repeat period plus the time to run the process for repeating jobs
     * @return true if a lock was obtained, false if not, null if failure
     */
    public Boolean obtainLock(String lockId, String executerId, long timePeriod) {
        if (StringUtils.isBlank(executerId)) {
            throw new IllegalArgumentException("The executer Id must be set");
        }
        if (StringUtils.isBlank(lockId)) {
            throw new IllegalArgumentException("The lock Id must be set");
        }

        // basically we are opening a transaction to get the current lock and set it if it is not there
        Boolean obtainedLock;
        try {
            // check the lock
            List<ClickerLock> locks = findBySearch(ClickerLock.class, new Search("name", lockId));

            if (locks.size() > 0) {
                // check if this is my lock, if not, then exit, if so then go ahead
                ClickerLock lock = locks.get(0);

                if (lock.getHolder().equals(executerId)) {
                    obtainedLock = true;
                    // if this is my lock then update it immediately
                    lock.setLastModified(new Date());
                    getHibernateTemplate().save(lock);
                    getHibernateTemplate().flush(); // this should commit the data immediately
                } else {
                    // not the lock owner but we can still get the lock
                    long validTime = lock.getLastModified().getTime() + timePeriod + 100;

                    if (System.currentTimeMillis() > validTime) {
                        // the old lock is no longer valid so we are taking it
                        obtainedLock = true;
                        lock.setLastModified(new Date());
                        lock.setHolder(executerId);
                        getHibernateTemplate().save(lock);
                        getHibernateTemplate().flush(); // this should commit the data immediately
                    } else {
                        // someone else is holding a valid lock still
                        obtainedLock = false;
                    }
                }
            } else {
                // obtain the lock
                ClickerLock lock = new ClickerLock(lockId, executerId);
                getHibernateTemplate().save(lock);
                getHibernateTemplate().flush(); // this should commit the data immediately
                obtainedLock = true;
            }
        } catch (RuntimeException e) {
            obtainedLock = null; // null indicates the failure
            cleanupLockAfterFailure(lockId);
            log.error("Lock obtaining failure for lock ({}): {}", lockId, e.getMessage(), e);
        }

        return obtainedLock;
    }

    /**
     * Releases a lock that was being held, this is useful if you know a server is shutting down and you want to release your locks early<br/>
     * <b>NOTE:</b> This intentionally returns a null on failure rather than an exception since exceptions will cause a rollback which makes the current session effectively dead, this also makes it
     * impossible to control the failure so instead we return null as a marker
     *
     * @param lockId the name of the lock which we are seeking
     * @param executerId a unique id for the holder of this lock (normally a server id)
     * @return true if a lock was released, false if not, null if failure
     */
    public Boolean releaseLock(String lockId, String executerId) {
        if (StringUtils.isBlank(executerId)) {
            throw new IllegalArgumentException("The executer Id must be set");
        }
        if (StringUtils.isBlank(lockId)) {
            throw new IllegalArgumentException("The lock Id must be set");
        }

        // basically we are opening a transaction to get the current lock and set it if it is not there
        Boolean releasedLock = false;
        try {
            // check the lock
            List<ClickerLock> locks = findBySearch(ClickerLock.class, new Search("name", lockId));

            if (locks.size() > 0) {
                // check if this is my lock, if not, then exit, if so then go ahead
                ClickerLock lock = locks.get(0);

                if (lock.getHolder().equals(executerId)) {
                    releasedLock = true;
                    // if this is my lock then remove it immediately
                    getHibernateTemplate().delete(lock);
                    getHibernateTemplate().flush(); // this should commit the data immediately
                } else {
                    releasedLock = false;
                }
            }
        } catch (RuntimeException e) {
            releasedLock = null; // null indicates the failure
            cleanupLockAfterFailure(lockId);
            log.error("Lock releasing failure for lock ({}): {}", lockId, e.getMessage(), e);
        }

        return releasedLock;
    }

    /**
     * Cleans up lock if there was a failure
     *
     * @param lockId the name of the lock which we are seeking
     */
    private void cleanupLockAfterFailure(String lockId) {
        getHibernateTemplate().clear(); // cancel any pending operations

        // try to clear the lock if things died
        try {
            List<ClickerLock> locks = findBySearch(ClickerLock.class, new Search("name", lockId));
            getHibernateTemplate().deleteAll(locks);
            getHibernateTemplate().flush();
        } catch (Exception ex) {
            log.error("Could not cleanup the lock ({}) after failure: {}", lockId, ex.getMessage(), ex);
        }
    }

}
