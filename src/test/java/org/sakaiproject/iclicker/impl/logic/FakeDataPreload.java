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
package org.sakaiproject.iclicker.impl.logic;

import java.lang.reflect.Field;

import org.sakaiproject.genericdao.api.GenericDao;
import org.sakaiproject.iclicker.model.dao.ClickerRegistration;

/**
 * Contains test data for preloading and test constants
 */
public class FakeDataPreload {

    /**
     * current user, access level user in LOCATION_ID1
     */
    public static final String USER_ID = "user-11111111";
    public static final String USER_DISPLAY = "Aaron Zeckoski";
    /**
     * access level user in LOCATION1_ID
     */
    public static final String ACCESS_USER_ID = "access-2222222";
    public static final String ACCESS_USER_DISPLAY = "Regular User";
    /**
     * maintain level user in LOCATION1_ID
     */
    public static final String MAINT_USER_ID = "maint-33333333";
    public static final String MAINT_USER_DISPLAY = "Maint User";
    /**
     * super admin user
     */
    public static final String ADMIN_USER_ID = "admin";
    public static final String ADMIN_USER_DISPLAY = "Administrator";
    /**
     * Invalid user (also can be used to simulate the anonymous user)
     */
    public static final String INVALID_USER_ID = "invalid-UUUUUU";

    /**
     * current location
     */
    public static final String LOCATION1_ID = "/site/ref-1111111";
    public static final String LOCATION1_TITLE = "Location 1 title";
    public static final String LOCATION2_ID = "/site/ref-22222222";
    public static final String LOCATION2_TITLE = "Location 2 title";
    public static final String INVALID_LOCATION_ID = "invalid-LLLLLLLL";

    // testing data objects here

    public ClickerRegistration item1 = new ClickerRegistration("AAAAAAAA", USER_ID);
    public ClickerRegistration item2 = new ClickerRegistration("BBBBBBBB", USER_ID, LOCATION1_ID);
    public ClickerRegistration item3 = new ClickerRegistration("CCCCCCCC", USER_ID, LOCATION2_ID);
    public ClickerRegistration accessitem = new ClickerRegistration("AAAAAAAA", ACCESS_USER_ID);
    public ClickerRegistration maintitem = new ClickerRegistration("MMMMMMMM", MAINT_USER_ID);
    public ClickerRegistration adminitem = new ClickerRegistration("DDDDDDDD", ADMIN_USER_ID);

    public GenericDao dao;

    public void setDao(GenericDao dao) {
        this.dao = dao;
    }

    public void init() {
        preloadTestData();
    }

    /**
     * Preload a bunch of test data into the database
     */
    public void preloadTestData() {
        /*
         * This iterates over the fields in FakeDataPreload and finds all the ones which are of the type ClickerRegistration, then it runs the dao save method on them:
         * dao.findById(ClickerRegistration.class, item1.getId()); This does the same thing as writing a bunch of these: dao.save(item1);
         */
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(ClickerRegistration.class)) {
                try {
                    dao.save((ClickerRegistration) field.get(this));
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Reload the test data back into the current session so they can be tested correctly, if this is not done then the preloaded data is in a separate session and equality tests will not work
     */
    public void reloadTestData() {
        /*
         * This iterates over the fields in FakeDataPreload and finds all the ones which are of the type ClickerRegistration, then it sets the field equal to the method:
         * dao.findById(ClickerRegistration.class, item1.getId()); This does the same thing as writing a bunch of these: item1 = (ClickerRegistration) dao.findById(ClickerRegistration.class,
         * item1.getId());
         */
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(ClickerRegistration.class)) {
                try {
                    field.set(this, dao.findById(ClickerRegistration.class, ((ClickerRegistration) field.get(this)).getId()));
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }

}
