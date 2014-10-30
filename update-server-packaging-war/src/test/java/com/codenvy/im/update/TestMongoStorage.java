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
package com.codenvy.im.update;

import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 */
public class TestMongoStorage {

    private MongoStorage mongoStorage;

    @BeforeTest
    public void prepare() throws Exception {
        mongoStorage = new MongoStorage("mongodb://localhost:12000/update", true, "target");
    }

    @BeforeMethod
    public void setUp() throws Exception {
        DBCollection collection = mongoStorage.getDb().getCollection(MongoStorage.ARTIFACTS_COLLECTION);
        collection.remove(new BasicDBObject());

        collection = mongoStorage.getDb().getCollection(MongoStorage.ARTIFACTS_DOWNLOADED_COLLECTION);
        collection.remove(new BasicDBObject());
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void testGetInstalledVersionErrorIfArtifactAbsent() throws Exception {
        mongoStorage.getInstalledInfo("user", "artifact");
    }

    @Test
    public void testGetInstalledVersion() throws Exception {
        mongoStorage.saveInstalledInfo("user", "artifact", "1.0.1");
        assertEquals(mongoStorage.getInstalledInfo("user", "artifact").get("version"), "1.0.1");

        Thread.sleep(1000);

        mongoStorage.saveInstalledInfo("user", "artifact", "1.0.2");
        assertEquals(mongoStorage.getInstalledInfo("user", "artifact").get("version"), "1.0.2");
    }

    @Test
    public void testGetDownloadStatisticsForSpecificUser() throws Exception {
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", false);

        mongoStorage.saveDownloadInfo("user2", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact3", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user2", "artifact3", "1.0.1", true);

        List downloadedInfo = mongoStorage.getDownloadsInfoByUserId("user1");
        assertEquals(downloadedInfo.size(), 2);
        assertEquals(downloadedInfo.get(0).toString(),"{artifact=artifact2, version=1.0.1, success=2, fail=1, userId=user1}");
        assertEquals(downloadedInfo.get(1).toString(),"{artifact=artifact1, version=1.0.1, success=2, fail=2, userId=user1}");

        downloadedInfo = mongoStorage.getDownloadsInfoByUserId("user2");
        assertEquals(downloadedInfo.size(), 3);
        assertEquals(downloadedInfo.get(0).toString(),"{artifact=artifact3, version=1.0.1, success=1, fail=1, userId=user2}");
        assertEquals(downloadedInfo.get(1).toString(),"{artifact=artifact2, version=1.0.1, success=2, fail=1, userId=user2}");
        assertEquals(downloadedInfo.get(2).toString(),"{artifact=artifact1, version=1.0.1, success=1, fail=0, userId=user2}");

        downloadedInfo = mongoStorage.getDownloadsInfoByUserId("user3");
        assertEquals(downloadedInfo.size(), 0);
    }

    @Test
    public void testGetTotalDownloadsForSpecificUser() throws Exception {
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", false);

        mongoStorage.saveDownloadInfo("user2", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", true);
         mongoStorage.saveDownloadInfo("user2", "artifact3", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user2", "artifact3", "1.0.1", true);

        Map<String, String> downloadedInfo = mongoStorage.getTotalDownloadsInfoByUserId("user1");
        assertEquals(downloadedInfo.toString(),"{success=4, fail=3, total=7}");

        downloadedInfo = mongoStorage.getTotalDownloadsInfoByUserId("user2");
        assertEquals(downloadedInfo.toString(),"{success=4, fail=2, total=6}");

        downloadedInfo = mongoStorage.getTotalDownloadsInfoByUserId("user3");
        assertEquals(downloadedInfo.toString(),"{}");
    }

    @Test
    public void testGetDownloadStatisticsByArtifact() throws Exception {
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.2", true);

        mongoStorage.saveDownloadInfo("user2", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact3", "1.0.2", true);

        List downloadedInfo = mongoStorage.getDownloadsInfoByArtifact("artifact1");
        assertEquals(downloadedInfo.size(), 2);
        assertEquals(downloadedInfo.get(0).toString(),"{userId=user2, version=1.0.1, success=1, fail=0, artifact=artifact1}");
        assertEquals(downloadedInfo.get(1).toString(),"{userId=user1, version=1.0.1, success=1, fail=0, artifact=artifact1}");

        downloadedInfo = mongoStorage.getDownloadsInfoByArtifact("artifact2");
        assertEquals(downloadedInfo.size(), 3);
        assertEquals(downloadedInfo.get(0).toString(),"{userId=user2, version=1.0.1, success=2, fail=0, artifact=artifact2}");
        assertEquals(downloadedInfo.get(1).toString(),"{userId=user1, version=1.0.2, success=1, fail=0, artifact=artifact2}");
        assertEquals(downloadedInfo.get(2).toString(),"{userId=user1, version=1.0.1, success=0, fail=1, artifact=artifact2}");

        downloadedInfo = mongoStorage.getDownloadsInfoByArtifact("artifact3");
        assertEquals(downloadedInfo.size(), 1);
        assertEquals(downloadedInfo.get(0).toString(),"{userId=user2, version=1.0.2, success=1, fail=0, artifact=artifact3}");

        downloadedInfo = mongoStorage.getDownloadsInfoByArtifact("artifact4");
        assertEquals(downloadedInfo.size(), 0);
    }

    @Test
    public void testGetTotalDownloadsForSpecificArtifact() throws Exception {
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user1", "artifact1", "1.0.1", false);

        mongoStorage.saveDownloadInfo("user2", "artifact1", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user2", "artifact2", "1.0.1", true);
        mongoStorage.saveDownloadInfo("user2", "artifact3", "1.0.1", false);
        mongoStorage.saveDownloadInfo("user2", "artifact3", "1.0.1", true);

        Map<String, String> downloadedInfo = mongoStorage.getTotalDownloadsInfoByArtifact("artifact1");
        assertEquals(downloadedInfo.toString(), "{success=3, fail=2, total=5}");

        downloadedInfo = mongoStorage.getTotalDownloadsInfoByArtifact("artifact2");
        assertEquals(downloadedInfo.toString(),"{success=4, fail=2, total=6}");

        downloadedInfo = mongoStorage.getTotalDownloadsInfoByArtifact("artifact3");
        assertEquals(downloadedInfo.toString(),"{success=1, fail=1, total=2}");

        downloadedInfo = mongoStorage.getTotalDownloadsInfoByArtifact("artifact4");
        assertEquals(downloadedInfo.toString(),"{}");
    }
}
