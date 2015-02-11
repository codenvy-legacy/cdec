/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.update;

import com.codenvy.im.utils.AccountUtils.SubscriptionInfo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.im.utils.AccountUtils.ON_PREMISES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class TestMongoStorage {

    private MongoStorage mongoStorage;

    @BeforeTest
    public void prepare() throws Exception {
        mongoStorage = new MongoStorage("mongodb://localhost:12000/update", true, "target", 2000);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        DBCollection collection = mongoStorage.getDb().getCollection(MongoStorage.DOWNLOAD_STATISTICS);
        collection.remove(new BasicDBObject());

        collection = mongoStorage.getDb().getCollection(MongoStorage.SUBSCRIPTIONS);
        collection.remove(new BasicDBObject());
    }

    @Test
    public void testSubscriptionsInfo() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        mongoStorage.addSubscriptionInfo("user1", new SubscriptionInfo("account1", "id1", ON_PREMISES, calendar, calendar));
        mongoStorage.addSubscriptionInfo("user2", new SubscriptionInfo("account2", "id2", ON_PREMISES, calendar, calendar));

        calendar.add(Calendar.DAY_OF_MONTH, 2);

        mongoStorage.addSubscriptionInfo("user3", new SubscriptionInfo("account3", "id3", ON_PREMISES, calendar, calendar));
        mongoStorage.addSubscriptionInfo("user4", new SubscriptionInfo("account4", "id4", ON_PREMISES, calendar, calendar));

        DBCollection collection = mongoStorage.getDb().getCollection(MongoStorage.SUBSCRIPTIONS);
        assertEquals(collection.count(), 4);

        Set<String> ids = mongoStorage.getExpiredSubscriptions("OnPremises");
        assertEquals(ids.size(), 2);
        assertTrue(ids.contains("id1"));
        assertTrue(ids.contains("id2"));

        mongoStorage.invalidateSubscription("id1");
        mongoStorage.invalidateSubscription("id2");

        ids = mongoStorage.getExpiredSubscriptions("OnPremises");
        assertTrue(ids.isEmpty());

        assertTrue(mongoStorage.hasSubscription("account1", "OnPremises"));
        assertFalse(mongoStorage.hasSubscription("account1", "Subscription"));
        assertFalse(mongoStorage.hasSubscription("account5", "OnPremises"));
    }

    @Test
    public void testGetDownloadStatisticsForSpecificUser() throws Exception {
        mongoStorage.updateDownloadStatistics("user1", "artifact1", "1.0.1", true);
        mongoStorage.updateDownloadStatistics("user1", "artifact1", "1.0.1", true);
        mongoStorage.updateDownloadStatistics("user1", "artifact2", "1.0.1", false);
        mongoStorage.updateDownloadStatistics("user1", "artifact2", "1.0.1", true);
        mongoStorage.updateDownloadStatistics("user1", "artifact2", "1.0.1", true);
        mongoStorage.updateDownloadStatistics("user1", "artifact1", "1.0.1", false);
        mongoStorage.updateDownloadStatistics("user1", "artifact1", "1.0.1", false);

        Map<String, Object> stat = mongoStorage.getDownloadsInfoByUserId("user1");
        assertEquals(stat.size(), 5);
        assertEquals(stat.get(MongoStorage.USER_ID), "user1");
        assertEquals(stat.get(MongoStorage.TOTAL), 7L);
        assertEquals(stat.get(MongoStorage.SUCCESS), 4L);
        assertEquals(stat.get(MongoStorage.FAIL), 3L);
        assertNotNull(stat.get(MongoStorage.ARTIFACTS));

        List l = (List)stat.get(MongoStorage.ARTIFACTS);
        assertEquals(l.size(), 2);

        Map m = (Map)l.get(0);
        assertEquals(m.get(MongoStorage.ARTIFACT), "artifact1");
        assertEquals(m.get(MongoStorage.VERSION), "1.0.1");
        assertEquals(m.get(MongoStorage.SUCCESS), 2);
        assertEquals(m.get(MongoStorage.FAIL), 2);
        assertEquals(m.get(MongoStorage.TOTAL), 4);

        m = (Map)l.get(1);
        assertEquals(m.get(MongoStorage.ARTIFACT), "artifact2");
        assertEquals(m.get(MongoStorage.VERSION), "1.0.1");
        assertEquals(m.get(MongoStorage.SUCCESS), 2);
        assertEquals(m.get(MongoStorage.FAIL), 1);
        assertEquals(m.get(MongoStorage.TOTAL), 3);
    }

    @Test
    public void testGetDownloadStatisticsByArtifact() throws Exception {
        mongoStorage.updateDownloadStatistics("user1", "artifact2", "1.0.1", false);
        mongoStorage.updateDownloadStatistics("user1", "artifact2", "1.0.2", true);
        mongoStorage.updateDownloadStatistics("user2", "artifact2", "1.0.1", true);
        mongoStorage.updateDownloadStatistics("user2", "artifact2", "1.0.1", true);

        Map<String, Object> stat = mongoStorage.getDownloadsInfoByArtifact("artifact2");
        assertEquals(stat.size(), 5);
        assertEquals(stat.get(MongoStorage.ARTIFACT), "artifact2");
        assertEquals(stat.get(MongoStorage.TOTAL), 4L);
        assertEquals(stat.get(MongoStorage.SUCCESS), 3L);
        assertEquals(stat.get(MongoStorage.FAIL), 1L);
        assertNotNull(stat.get(MongoStorage.VERSIONS));

        List l = (List)stat.get(MongoStorage.VERSIONS);
        assertEquals(l.size(), 2);

        Map m = (Map)l.get(0);
        assertEquals(m.get(MongoStorage.VERSION), "1.0.2");
        assertEquals(m.get(MongoStorage.SUCCESS), 1);
        assertEquals(m.get(MongoStorage.FAIL), 0);
        assertEquals(m.get(MongoStorage.TOTAL), 1);

        m = (Map)l.get(1);
        assertEquals(m.get(MongoStorage.VERSION), "1.0.1");
        assertEquals(m.get(MongoStorage.SUCCESS), 2);
        assertEquals(m.get(MongoStorage.FAIL), 1);
        assertEquals(m.get(MongoStorage.TOTAL), 3);
    }

    @Test
    public void testInvalidateExpiredSubscriptions() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        assertTrue(mongoStorage.getExpiredSubscriptions(ON_PREMISES).isEmpty());

        mongoStorage.addSubscriptionInfo("user1", new SubscriptionInfo("account1", "id1", ON_PREMISES, calendar, calendar));
        mongoStorage.addSubscriptionInfo("user2", new SubscriptionInfo("account2", "id2", ON_PREMISES, calendar, calendar));

        assertEquals(mongoStorage.getExpiredSubscriptions(ON_PREMISES).size(), 2);

        mongoStorage.invalidateExpiredSubscriptions();

        assertTrue(mongoStorage.getExpiredSubscriptions(ON_PREMISES).isEmpty());
    }
}
