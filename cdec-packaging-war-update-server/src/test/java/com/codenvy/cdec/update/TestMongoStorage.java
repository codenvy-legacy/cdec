/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.cdec.update;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 */
public class TestMongoStorage {

    private MongoStorage mongoStorage;

    @BeforeTest
    public void prepare() throws Exception {
        mongoStorage = new MongoStorage("mongodb://localhost:12000/update", true);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        DBCollection collection = mongoStorage.getDb().getCollection(MongoStorage.ARTIFACTS_COLLECTION);
        collection.remove(new BasicDBObject());
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void testGetInstalledVersionErrorIfArtifactAbsent() throws Exception {
        mongoStorage.getInstalledInfo("user", "artifact");
    }

    @Test
    public void testGetInstalledVersion() throws Exception {
        mongoStorage.saveInstalledInfo("user", "artifact", "1.0.1");
        assertEquals(mongoStorage.getInstalledInfo("user", "artifact"), "1.0.1");

        Thread.sleep(1000);

        mongoStorage.saveInstalledInfo("user", "artifact", "1.0.2");
        assertEquals(mongoStorage.getInstalledInfo("user", "artifact"), "1.0.2");
    }
}
