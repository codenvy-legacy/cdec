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

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;

import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class MongoStorage {
    public static final String ARTIFACTS_COLLECTION            = "artifacts";
    public static final String ARTIFACTS_DOWNLOADED_COLLECTION = "artifacts_downloads";

    private static final Logger LOG      = LoggerFactory.getLogger(MongoStorage.class);
    public static final  String ID       = "_id";
    public static final  String USER_ID  = "userId";
    public static final  String ARTIFACT = "artifact";
    public static final  String VERSION  = "version";
    public static final  String DATE     = "date";
    public static final  String SUCCESS  = "success";
    public static final  String FAIL     = "fail";
    public static final  String TOTAL    = "total";

    private final DB             db;
    private final String         dir;
    private final MongoClientURI uri;

    @Inject
    public MongoStorage(@Named("update-server.mongodb.url") String url,
                        @Named("update-server.mongodb.embedded") boolean embedded,
                        @Named("update-server.mongodb.embedded_dir") String dir) throws IOException {
        this.uri = new MongoClientURI(url);
        this.dir = dir;

        if (embedded) { // for testing purpose only
            try {
                initEmbeddedStorage();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                throw new IOException(e);
            }
        }

        db = connectToDB();
        initCollections();
    }

    /**
     * @return info about the latest installed artifact by user.
     */
    public Map getInstalledInfo(String userId, String artifact) throws MongoException, ArtifactNotFoundException {
        DBCollection collection = db.getCollection(ARTIFACTS_COLLECTION);

        DBObject clause = new BasicDBObject();
        clause.put(USER_ID, userId);
        clause.put(ARTIFACT, artifact);

        DBObject order = new BasicDBObject(DATE, -1);

        DBCursor cursor = collection.find(clause).sort(order).limit(1);
        if (cursor.hasNext()) {
            DBObject doc = cursor.next();
            doc.removeField(ID);
            return doc.toMap();
        } else {
            throw new ArtifactNotFoundException(artifact);
        }
    }


    /**
     * Saves info concerning installed artifact by user.
     */
    public void saveInstalledInfo(String userId, String artifact, String version) throws MongoException {
        DBCollection collection = db.getCollection(ARTIFACTS_COLLECTION);

        DBObject doc = new BasicDBObject();
        doc.put(USER_ID, userId);
        doc.put(ARTIFACT, artifact);
        doc.put(VERSION, version);
        doc.put(DATE, new Date());

        collection.save(doc);
    }

    /**
     * Saves info concerning downloaded artifact by user.
     */
    public void saveDownloadInfo(String userId, String artifact, String version, boolean isSuccessDownloading) throws MongoException {
        DBCollection collection = db.getCollection(ARTIFACTS_DOWNLOADED_COLLECTION);

        DBObject doc = new BasicDBObject();
        doc.put(USER_ID, userId);
        doc.put(ARTIFACT, artifact);
        doc.put(VERSION, version);
        doc.put(DATE, new Date());

        if (isSuccessDownloading) {
            doc.put(SUCCESS, 1);
        } else {
            doc.put(FAIL, 1);
        }

        collection.save(doc);
    }

    /**
     * @return statistics by users about downloading specific artifact with versions.
     * <p/>
     * Example:
     * {userId=user2, artifact=artifact2, version=1.0.1, success=2, fail=0}
     * {userId=user1, artifact=artifact2, version=1.0.2, success=1, fail=0}
     * {userId=user1, artifact=artifact2, version=1.0.1, success=0, fail=1}
     */
    public List<Map<String, String>> getDownloadsInfoByArtifact(String artifact) throws MongoException, ArtifactNotFoundException {
        AggregationOutput output = getDownloadsByArtifact(artifact);

        List<Map<String, String>> result = createResult(output, USER_ID, VERSION, SUCCESS, FAIL);
        addFieldInResult(result, ARTIFACT, artifact);

        return result;
    }

    private AggregationOutput getDownloadsByArtifact(String artifact) {
        DBCollection collection = db.getCollection(ARTIFACTS_DOWNLOADED_COLLECTION);
        DBObject match = new BasicDBObject("$match", new BasicDBObject(ARTIFACT, artifact));
        DBObject project1 = new BasicDBObject("$project", createProjectFields(USER_ID, VERSION, SUCCESS, FAIL));
        DBObject group = getGroupOperation(createGroupFields(USER_ID, VERSION));

        return collection.aggregate(match, project1, group);
    }

    /**
     * @return total downloads by artifact.
     * <p/>
     * Example:
     * {success=2, fail=1, total=3}
     */
    public Map<String, String> getTotalDownloadsInfoByArtifact(String artifact) {
        AggregationOutput output = getTotalDownloadsByArtifact(artifact);
        List<Map<String, String>> list = createResult(output, SUCCESS, FAIL);

        if (list.size() == 0)
        {
            return Collections.EMPTY_MAP;
        }

        Map<String, String> m = list.get(0);
        Long success = Long.valueOf(m.get(SUCCESS));
        Long fail = Long.valueOf(m.get(FAIL));
        m.put(TOTAL, String.valueOf(success + fail));

        return m;
    }

    private AggregationOutput getTotalDownloadsByArtifact(String artifact) {
        DBCollection collection = db.getCollection(ARTIFACTS_DOWNLOADED_COLLECTION);
        DBObject match = new BasicDBObject("$match", new BasicDBObject(ARTIFACT, artifact));
        DBObject project1 = new BasicDBObject("$project", createProjectFields(SUCCESS, FAIL));
        DBObject group = getGroupOperation(null);

        return collection.aggregate(match, project1, group);
    }

    /**
     * @return statistics by specific user about downloading artifacts with versions.
     * <p/>
     * Example :
     * {userId=user2, artifact=artifact3, version=1.0.1, success=1, fail=1}
     * {userId=user2, artifact=artifact2, version=1.0.1, success=2, fail=1}
     * {userId=user2, artifact=artifact1, version=1.0.1, success=1, fail=0}
     */
    public List<Map<String, String>> getDownloadsInfoByUserId(String userId) throws MongoException, ArtifactNotFoundException {
        AggregationOutput output = getDownloadsByUserId(userId);

        List<Map<String, String>> result = createResult(output, ARTIFACT, VERSION, SUCCESS, FAIL);
        addFieldInResult(result, USER_ID, userId);

        return result;
    }

    private AggregationOutput getDownloadsByUserId(String userId) {
        DBCollection collection = db.getCollection(ARTIFACTS_DOWNLOADED_COLLECTION);
        DBObject match = new BasicDBObject("$match", new BasicDBObject(USER_ID, userId));
        DBObject project1 = new BasicDBObject("$project", createProjectFields(ARTIFACT, VERSION, SUCCESS, FAIL));
        DBObject group = getGroupOperation(createGroupFields(ARTIFACT, VERSION));

        return collection.aggregate(match, project1, group);
    }

    /**
     * @return total downloads by user.
     * <p/>
     * Example:
     * {success=2, fail=1, total=3}
     */
    public Map<String, String> getTotalDownloadsInfoByUserId(String userId) {
        AggregationOutput output = getTotalDownloadsByUserId(userId);
        List<Map<String, String>> list = createResult(output, SUCCESS, FAIL);

        if (list.size() == 0)
        {
            return Collections.EMPTY_MAP;
        }

        Map<String, String> m = list.get(0);
        Long success = Long.valueOf(m.get(SUCCESS));
        Long fail = Long.valueOf(m.get(FAIL));
        m.put(TOTAL, String.valueOf(success + fail));

        return m;
    }

    private AggregationOutput getTotalDownloadsByUserId(String userId) {
        DBCollection collection = db.getCollection(ARTIFACTS_DOWNLOADED_COLLECTION);
        DBObject match = new BasicDBObject("$match", new BasicDBObject(USER_ID, userId));
        DBObject project1 = new BasicDBObject("$project", createProjectFields(SUCCESS, FAIL));
        DBObject group = getGroupOperation(null);

        return collection.aggregate(match, project1, group);
    }

    private Map<Object, Object> createGroupFields(String... fields) {
        Map<Object, Object> m = new HashMap<>();
        for (String field : fields) {
            m.put(field, "$" + field);
        }
        return m;
    }

    private BasicDBObject getGroupOperation(Map<Object, Object> m) {
        return new BasicDBObject("$group", new BasicDBObject(ID, m)
                .append(SUCCESS, new BasicDBObject("$sum", "$" + SUCCESS))
                .append(FAIL, new BasicDBObject("$sum", "$" + FAIL)));
    }

    private BasicDBObject createProjectFields(String... fields) {
        BasicDBObject projectField = new BasicDBObject(fields[0], 1);
        for (int i = 1; i < fields.length; i++) {
            projectField.append(fields[i], 1);
        }

        return projectField;
    }

    private List<Map<String, String>> createResult(AggregationOutput output, String... trackedFields) {
        List<Map<String, String>> result = new ArrayList<>();
        for (DBObject item : output.results()) {
            Map<String, String> itemMap = new LinkedHashMap<>();

            for (String field : trackedFields) {
                if (item.get(ID) != null && ((BasicDBObject)item.get(ID)).get(field) != null) {
                    itemMap.put(field, ((BasicDBObject)item.get(ID)).get(field).toString());
                } else {
                    itemMap.put(field, item.get(field).toString());
                }
            }

            result.add(itemMap);
        }
        return result;
    }

    private void addFieldInResult(List<Map<String, String>> result, String field, String value) {
        for (Map<String, String> m : result) {
            m.put(field, value);
        }
    }

    protected DB getDb() {
        return db;
    }

    private void initCollections() {
        DBCollection collection = db.getCollection(ARTIFACTS_COLLECTION);
        collection.ensureIndex(new BasicDBObject(USER_ID, 1).append(ARTIFACT, 1).append(DATE, -1));

        collection = db.getCollection(ARTIFACTS_DOWNLOADED_COLLECTION);
        collection.ensureIndex(new BasicDBObject(USER_ID, 1).append(ARTIFACT, 1).append(VERSION, 1).append(DATE, -1));
    }

    private DB connectToDB() throws IOException {
        final MongoClient mongoClient = new MongoClient(uri);

        DB db;
        try {
            db = mongoClient.getDB(uri.getDatabase());
            db.setWriteConcern(WriteConcern.ACKNOWLEDGED);
            db.setReadPreference(ReadPreference.primaryPreferred());

            if (isAuthRequired(uri)) {
                db.authenticate(uri.getUsername(), uri.getPassword());
            }
        } catch (MongoException e) {
            mongoClient.close();
            throw new IOException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                mongoClient.close();
            }
        });

        LOG.info("Connection to MongoDB has been initialized");
        return db;
    }

    private boolean isAuthRequired(MongoClientURI clientURI) {
        return clientURI.getUsername() != null && !clientURI.getUsername().isEmpty();
    }

    private void initEmbeddedStorage() throws IOException {
        if (isStarted()) {
            return;
        }

        LOG.info("Embedded MongoDB is starting up");

        Files.createDirectories(Paths.get(dir));

        MongodConfigBuilder mongodConfigBuilder = new MongodConfigBuilder();
        mongodConfigBuilder.net(new Net(12000, false));
        mongodConfigBuilder.replication(new Storage(dir, null, 0));
        mongodConfigBuilder.version(Version.V2_5_4);

        RuntimeConfigBuilder runtimeConfigBuilder = new RuntimeConfigBuilder().defaults(Command.MongoD);

        MongodStarter starter = MongodStarter.getInstance(runtimeConfigBuilder.build());
        final MongodExecutable mongoExe = starter.prepare(mongodConfigBuilder.build());

        try {
            mongoExe.start();
            LOG.info("Embedded MongoDB is started");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                mongoExe.stop();
            }
        });
    }

    /**
     * Checks if embedded storage is started. If connection can be opened, then it means storage is started. All JVM
     * share the same storage.
     */
    private boolean isStarted() {
        try {
            try (Socket sock = new Socket()) {
                URI url = new URI(uri.toString());

                int timeout = 500;
                InetAddress addr = InetAddress.getByName(url.getHost());
                SocketAddress sockAddr = new InetSocketAddress(addr, url.getPort());

                sock.connect(sockAddr, timeout);
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
