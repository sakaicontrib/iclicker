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
package org.sakaiproject.iclicker.api.dao;

import org.sakaiproject.genericdao.api.GeneralGenericDao;

/**
 * This is a specialized DAO that allows the developer to extend the functionality of the generic dao package
 */
public interface IClickerDao extends GeneralGenericDao {

    /**
     * Allows a lock to be obtained that is system wide, this is primarily for ensuring something runs on a single server only in a cluster<br/>
     * <b>NOTE:</b> This intentionally returns a null on failure rather than an exception since exceptions will cause a rollback which makes the current session effectively dead, this also makes it
     * impossible to control the failure so instead we return null as a marker
     * 
     * @param lockId the name of the lock which we are seeking
     * @param holderId a unique id for the holder of this lock (normally a server id)
     * @param timePeriod the length of time (in milliseconds) that the lock should be valid for, set this very low for non-repeating processes (the length of time the process should take to run) and
     *            the length of the repeat period plus the time to run the process for repeating jobs
     * @return true if a lock was obtained, false if not, null if failure
     */
    Boolean obtainLock(String lockId, String executerId, long timePeriod);

    /**
     * Releases a lock that was being held, this is useful if you know a server is shutting down and you want to release your locks early<br/>
     * <b>NOTE:</b> This intentionally returns a null on failure rather than an exception since exceptions will cause a rollback which makes the current session effectively dead, this also makes it
     * impossible to control the failure so instead we return null as a marker
     * 
     * @param lockId the name of the lock which we are seeking
     * @param holderId a unique id for the holder of this lock (normally a server id)
     * @return true if a lock was released, false if not, null if failure
     */
    Boolean releaseLock(String lockId, String executerId);

}
