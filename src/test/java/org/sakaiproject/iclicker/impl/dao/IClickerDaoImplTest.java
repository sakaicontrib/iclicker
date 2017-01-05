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

import org.junit.Assert;

import org.sakaiproject.genericdao.api.search.Restriction;
import org.sakaiproject.genericdao.api.search.Search;
import org.sakaiproject.iclicker.api.dao.IClickerDao;
import org.sakaiproject.iclicker.impl.logic.FakeDataPreload;
import org.sakaiproject.iclicker.model.dao.ClickerRegistration;
import org.sakaiproject.iclicker.model.dao.ClickerUserKey;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.AbstractTransactionalSpringContextTests;

/**
 * Testing for the specialized DAO methods (do not test the Generic Dao methods)
 */
public class IClickerDaoImplTest extends AbstractTransactionalSpringContextTests {

    protected IClickerDao dao;
    private FakeDataPreload tdp;

    private ClickerRegistration item;
    private ClickerRegistration item2;

    private static final String ITEM_TITLE = "New Title";
    private static final String ITEM_OWNER = "11111111";
    private static final String ITEM_SITE = "22222222";

    protected String[] getConfigLocations() {
        // point to the needed spring config files, must be on the classpath
        // (add component/src/webapp/WEB-INF to the build path in Eclipse),
        // they also need to be referenced in the project.xml file
        return new String[] {"hibernate-test.xml", "spring-hibernate.xml"};
    }

    // run this before each test starts
    protected void onSetUpBeforeTransaction() throws Exception {
        // create test objects
        item = new ClickerRegistration(ITEM_TITLE, ITEM_OWNER, ITEM_SITE);
        item2 = new ClickerRegistration(ITEM_TITLE, ITEM_OWNER);
    }

    // run this before each test starts and as part of the transaction
    protected void onSetUpInTransaction() {
        // load the spring created dao class bean from the Spring Application Context
        dao = (IClickerDao) applicationContext.getBean("org.sakaiproject.iclicker.api.dao.IClickerDao");
        if (dao == null) {
            throw new NullPointerException("DAO could not be retrieved from spring context");
        }

        // load up the test data preloader from spring
        tdp = (FakeDataPreload) applicationContext.getBean("org.sakaiproject.iclicker.impl.logic.test.FakeDataPreload");

        if (tdp == null) {
            throw new NullPointerException("FakeDataPreload could not be retrieved from spring context");
        }

        // check the preloaded data
        Assert.assertTrue("Error preloading data", dao.countAll(ClickerRegistration.class) > 0);

        // preload data if desired
        dao.save(item);
        dao.save(item2);
    }

    /**
     * ADD unit tests below here, use testMethod as the name of the unit test, Note that if a method is overloaded you should include the arguments in the test name like so: testMethodClassInt (for
     * method(Class, int);
     */

    // THESE TESTS VALIDATE THE HIBERNATE CONFIG
    /**
     * Test method for {@link org.sakaiproject.iclicker.dao.impl.GenericHibernateDao#save(java.lang.Object)}.
     */
    public void testSave() {
        ClickerRegistration item1 = new ClickerRegistration("New item1", ITEM_OWNER);
        dao.save(item1);
        Long itemId = item1.getId();
        Assert.assertNotNull(itemId);
        Assert.assertTrue(dao.countAll(ClickerRegistration.class) > 8);
    }

    /**
     * Test method for {@link org.sakaiproject.iclicker.dao.impl.GenericHibernateDao#delete(java.lang.Object)}.
     */
    public void testDelete() {
        int count = dao.countAll(ClickerRegistration.class);
        Assert.assertTrue(count > 6);
        dao.delete(item);
        Assert.assertEquals(dao.countAll(ClickerRegistration.class), count - 1);
    }

    /**
     * Test method for {@link org.sakaiproject.iclicker.dao.impl.GenericHibernateDao#findById(java.lang.Class, java.io.Serializable)} .
     */
    public void testFindById() {
        Long id = item.getId();
        Assert.assertNotNull(id);
        ClickerRegistration item1 = (ClickerRegistration) dao.findById(ClickerRegistration.class, id);
        Assert.assertNotNull(item1);
        Assert.assertEquals(item, item1);
    }

    public void testObtainLock() {
        // check I can get a lock
        assertTrue(dao.obtainLock("AZ.my.lock", "AZ1", 100));

        // check someone else cannot get my lock
        assertFalse(dao.obtainLock("AZ.my.lock", "AZ2", 100));

        // check I can get my own lock again
        assertTrue(dao.obtainLock("AZ.my.lock", "AZ1", 100));

        // allow the lock to expire
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // nothing here but a fail
            fail("sleep interrupted?");
        }

        // check someone else can get my lock
        assertTrue(dao.obtainLock("AZ.my.lock", "AZ2", 100));

        // check invalid arguments cause failure
        try {
            dao.obtainLock("AZ.my.lock", null, 1000);
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            dao.obtainLock(null, "AZ1", 1000);
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    public void testReleaseLock() {
        // check I can get a lock
        assertTrue(dao.obtainLock("AZ.R.lock", "AZ1", 1000));

        // check someone else cannot get my lock
        assertFalse(dao.obtainLock("AZ.R.lock", "AZ2", 1000));

        // check I can release my lock
        assertTrue(dao.releaseLock("AZ.R.lock", "AZ1"));

        // check someone else can get my lock now
        assertTrue(dao.obtainLock("AZ.R.lock", "AZ2", 1000));

        // check I cannot get the lock anymore
        assertFalse(dao.obtainLock("AZ.R.lock", "AZ1", 1000));

        // check they can release it
        assertTrue(dao.releaseLock("AZ.R.lock", "AZ2"));

        // check invalid arguments cause failure
        try {
            dao.releaseLock("AZ.R.lock", null);
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }

        try {
            dao.releaseLock(null, "AZ1");
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    public void testUserKeys() {
        ClickerUserKey key1 = new ClickerUserKey("123456789", "aaronz");
        dao.save(key1);
        Long keyId = key1.getId();
        assertNotNull(keyId);
        assertTrue(dao.countAll(ClickerUserKey.class) > 0);

        ClickerUserKey keyFind = dao.findOneBySearch(ClickerUserKey.class, new Search(new Restriction("userId", "aaronz")));
        assertNotNull(keyFind);
        assertEquals(keyFind, key1);

        // update the key should work
        key1.setUserKey("aaaaaabbbbb");
        dao.save(key1);

        // trying to save another key for this user should fail
        ClickerUserKey key2 = new ClickerUserKey("abcdefgh", "aaronz");

        try {
            dao.save(key2);
            fail("Should have thrown an exception");
        } catch (DataIntegrityViolationException e) {
            assertNotNull(e.getMessage());
        }

        assertNull(key2.getId());
    }

    public void testUserKeysDelete() {
        ClickerUserKey key1 = new ClickerUserKey("123456789", "aaronz");
        dao.save(key1);
        Long keyId = key1.getId();
        assertNotNull(keyId);
        assertTrue(dao.countAll(ClickerUserKey.class) > 0);

        ClickerUserKey keyFind = dao.findOneBySearch(ClickerUserKey.class, new Search(new Restriction("userId", "aaronz")));
        assertNotNull(keyFind);
        assertEquals(keyFind, key1);

        // delete the key should work
        dao.delete(key1);
        keyFind = dao.findOneBySearch(ClickerUserKey.class, new Search(new Restriction("userId", "aaronz")));
        assertNull(keyFind);

        // adding key2 should work since key1 is gone
        ClickerUserKey key2 = new ClickerUserKey("abcdefgh", "aaronz");
        dao.save(key2);
        assertNotNull(key2.getId());
    }

}
