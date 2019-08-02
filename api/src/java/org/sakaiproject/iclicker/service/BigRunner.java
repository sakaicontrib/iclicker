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
package org.sakaiproject.iclicker.service;

import java.util.Observable;

/**
 * Special class for handling long running operations.
 */
public interface BigRunner extends Runnable {
    String RUNNER_TYPE_ADD = "add";
    String RUNNER_TYPE_REMOVE = "remove";
    String RUNNER_TYPE_SYNC = "sync";
    String RUNNER_LOCK = "bigRunnerLock";

    /**
     * Get total items.
     *
     * @return total item count
     */
    int getTotalItems();

    /**
     * Get total items completed.
     *
     * @return total item completed count
     */
    int getItemsCompleted();

    /**
     * Get total items percent.
     *
     * @return total item percent
     */
    int getPercentCompleted();

    /**
     * Is this complete?
     *
     * @return true if complete
     */
    boolean isComplete();

    /**
     * Is this in error?
     *
     * @return true if in error
     */
    boolean isError();

    /**
     * Sets the failure exception.
     *
     * @param e the exception
     */
    void setFailure(Exception e);

    /**
     * Get the type.
     *
     * @return the type
     */
    String getType();

    /**
     * Get the observable.
     *
     * @return the observable
     */
    Observable getObservable();

}
