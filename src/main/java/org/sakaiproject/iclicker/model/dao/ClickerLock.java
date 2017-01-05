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
package org.sakaiproject.iclicker.model.dao;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * This defines locks to allow for cluster operations
 */
public class ClickerLock implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter @Getter private Long id;
    @Setter @Getter private Date lastModified;

    /**
     * The name of the lock
     */
    @Setter @Getter private String name;

    /**
     * The holder (owner) of this lock
     */
    @Setter @Getter private String holder;

    public ClickerLock() {
    }

    public ClickerLock(String name, String holder) {
        this.lastModified = new Date();
        this.name = name;
        this.holder = holder;
    }

}
