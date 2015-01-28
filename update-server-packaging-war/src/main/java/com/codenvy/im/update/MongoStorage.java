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
import com.codenvy.im.utils.AccountUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.codenvy.im.utils.AccountUtils.ON_PREMISES;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class MongoStorage {
    public static final String SUBSCRIPTIONS       = "subscriptions";
    public static final String DOWNLOAD_STATISTICS = "download_statistics";

    private static final Logger LOG = LoggerFactory.getLogger(MongoStorage.class);

    private final Timer timer;

    // mongoDB fields
    public static final String ID = "_id";

    public static final String USER_ID   = "userId";
    public static final String ARTIFACT  = "artifact";
    public static final String ARTIFACTS = "artifacts";
    public static final String VERSION   = "version";
    public static final String VERSIONS  = "versions";
    public static final String DATE      = "date";
    public static final String FAIL      = "fail";
    public static final String TOTAL     = "total";
    public static final String SUCCESS   = "success";

    public static final String ACCOUNT_ID = "accountId";
    public static final String SUBSCRIPTION    = "subscription";
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String START_DATE      = "startDate";
    public static final String END_DATE        = "endDate";
    public static final String VALID           = "valid";

    private final DB             db;
    private final String         dir;
    private final MongoClientURI uri;
    private final int invalidationDelay;

    @Inject
    public MongoStorage(@Named("update-server.mongodb.url") String url,
                        @Named("update-server.mongodb.embedded") boolean embedded,
                        @Named("update-server.mongodb.embedded_dir") String dir,
                        @Named("update-server.subscription.invalidation_delay") int invalidationDelay) throws IOException {
        this.timer = new Timer();
        this.invalidationDelay = invalidationDelay;
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

    @PostConstruct
    public void init() {
        timer.schedule(new SubscriptionInvalidator(), invalidationDelay);
    }

    @PreDestroy
    public void destroy() {
        timer.cancel();
    }

    /** Adds info that subscription was added to user account */
    public void addSubscriptionInfo(String userId, AccountUtils.SubscriptionInfo subscriptionInfo) {
        DBCollection collection = db.getCollection(SUBSCRIPTIONS);

        DBObject doc = new BasicDBObject();
        doc.put(USER_ID, userId);
        doc.put(ACCOUNT_ID, subscriptionInfo.getAccountId());
        doc.put(SUBSCRIPTION, subscriptionInfo.getServiceId());
        doc.put(SUBSCRIPTION_ID, subscriptionInfo.getSubscriptionId());
        doc.put(START_DATE, subscriptionInfo.getStartDate().getTime());
        doc.put(END_DATE, subscriptionInfo.getEndDate().getTime());
        doc.put(VALID, true);

        collection.save(doc);
    }

    /** @return all active subscriptions */
    public Set<String> getExpiredSubscriptions(String subscription) {
        DBObject doc = new BasicDBObject();
        doc.put(VALID, true);
        doc.put(SUBSCRIPTION, subscription);
        doc.put(END_DATE, new BasicDBObject("$lt", Calendar.getInstance().getTime()));

        DBCursor cursor = db.getCollection(SUBSCRIPTIONS).find(doc);
        Set<String> subscriptionIds = new HashSet<>(cursor.size());
        while (cursor.hasNext()) {
            subscriptionIds.add(cursor.next().get(SUBSCRIPTION_ID).toString());
        }

        return subscriptionIds;
    }

    /** Sets the flag that subscription is not valid anymore */
    public void invalidateSubscription(String subscriptionId) {
        DBCollection collection = db.getCollection(SUBSCRIPTIONS);

        DBObject query = new BasicDBObject();
        query.put(SUBSCRIPTION_ID, subscriptionId);

        DBObject doc = new BasicDBObject();
        doc.put("$set", new BasicDBObject(VALID, false));

        collection.update(query, doc, false, true);
    }

    /** Indicates if user already has subscription */
    public boolean hasSubscription(String accountId, String subscription) {
        DBCollection collection = db.getCollection(SUBSCRIPTIONS);
        DBObject query = new BasicDBObject();
        query.put(ACCOUNT_ID, accountId);
        query.put(SUBSCRIPTION, subscription);

        return collection.findOne(query) != null;
    }

    /** Saves info concerning downloaded artifact by user. */
    public void updateDownloadStatistics(String userId, String artifact, String version, boolean isSuccessfullyDownloaded) throws MongoException {
        DBCollection collection = db.getCollection(DOWNLOAD_STATISTICS);

        DBObject doc = new BasicDBObject();
        doc.put(USER_ID, userId);
        doc.put(ARTIFACT, artifact);
        doc.put(VERSION, version);
        doc.put(DATE, new Date());

        if (isSuccessfullyDownloaded) {
            doc.put(SUCCESS, 1);
        } else {
            doc.put(FAIL, 1);
        }

        collection.save(doc);
    }

    /**
     * @return statistics by users about downloading specific artifact with versions, for instance
     * {userId=artifact, total=3, versions={version=1.0.1, success=1, fail=2}}
     */
    public Map<String, Object> getDownloadsInfoByArtifact(String artifact) throws MongoException, ArtifactNotFoundException {
        AggregationOutput output = aggregateByArtifact(artifact);

        List<Map<String, Object>> statByVersions = fetchResult(output);

        long success = calculateTotal(statByVersions, SUCCESS);
        long fail = calculateTotal(statByVersions, FAIL);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put(ARTIFACT, artifact);
        m.put(SUCCESS, success);
        m.put(FAIL, fail);
        m.put(TOTAL, success + fail);
        m.put(VERSIONS, statByVersions);

        return m;
    }

    private AggregationOutput aggregateByArtifact(String artifact) {
        DBCollection collection = db.getCollection(DOWNLOAD_STATISTICS);

        DBObject match = new BasicDBObject("$match", new BasicDBObject(ARTIFACT, artifact));
        DBObject group = createGroupWithSumOfDownloads(createGroupIdWithFields(VERSION));
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject(ID, -1));

        return collection.aggregate(match, group, sort);
    }

    /**
     * @return statistics by specific user about downloaded artifacts, for instance
     * {userId=user2, total=3, artifacts={artifact=artifact3, version=1.0.1, success=1, fail=2}}
     */
    public Map<String, Object> getDownloadsInfoByUserId(String userId) throws MongoException, ArtifactNotFoundException {
        AggregationOutput output = aggregateByUserId(userId);

        List<Map<String, Object>> statByArtifacts = fetchResult(output);

        long success = calculateTotal(statByArtifacts, SUCCESS);
        long fail = calculateTotal(statByArtifacts, FAIL);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put(USER_ID, userId);
        m.put(SUCCESS, success);
        m.put(FAIL, fail);
        m.put(TOTAL, success + fail);
        m.put(ARTIFACTS, statByArtifacts);

        return m;
    }

    private long calculateTotal(List<Map<String, Object>> stats, String field) {
        long total = 0;

        for (Map<String, Object> s : stats) {
            total += ((Number)s.get(field)).longValue();
        }

        return total;
    }

    private AggregationOutput aggregateByUserId(String userId) {
        DBCollection collection = db.getCollection(DOWNLOAD_STATISTICS);

        DBObject match = createMatchWithUserId(userId);
        DBObject group = createGroupWithSumOfDownloads(createGroupIdWithFields(ARTIFACT, VERSION));
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject(ID + "." + ARTIFACT, 1).append(ID + "." + VERSION, 1));

        return collection.aggregate(match, group, sort);
    }

    private Map<Object, Object> createGroupIdWithFields(String... fields) {
        Map<Object, Object> m = new HashMap<>();
        for (String field : fields) {
            m.put(field, "$" + field);
        }
        return m;
    }

    private BasicDBObject createGroupWithSumOfDownloads(Object id) {
        BasicDBObject group = new BasicDBObject(ID, id);
        addSumOperationsForDownloads(group);
        return new BasicDBObject("$group", group);
    }

    private void addSumOperationsForDownloads(BasicDBObject group) {
        group.put(SUCCESS, new BasicDBObject("$sum", "$" + SUCCESS));
        group.put(FAIL, new BasicDBObject("$sum", "$" + FAIL));
        group.put(TOTAL, new BasicDBObject("$sum", 1));
    }

    private BasicDBObject createMatchWithUserId(String userId) {
        return new BasicDBObject("$match", new BasicDBObject(USER_ID, userId));
    }

    private List<Map<String, Object>> fetchResult(AggregationOutput output) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (DBObject doc : output.results()) {
            Map<String, Object> m = new LinkedHashMap<>();

            for (String key : doc.keySet()) {
                Object value = doc.get(key);

                if (key.equals(ID)) {
                    if (value instanceof DBObject) {
                        DBObject idDoc = (DBObject)value;
                        for (String idKey : idDoc.keySet()) {
                            m.put(idKey, idDoc.get(idKey));
                        }
                    }
                } else {
                    m.put(key, value);
                }
            }

            result.add(m);
        }

        return result;
    }

    protected DB getDb() {
        return db;
    }

    private void initCollections() {
        DBCollection collection = db.getCollection(DOWNLOAD_STATISTICS);
        addIndex(collection, USER_ID, ARTIFACT);
        addIndex(collection, ARTIFACT);

        collection = db.getCollection(SUBSCRIPTIONS);
        addIndex(collection, SUBSCRIPTION_ID);
        addIndex(collection, ACCOUNT_ID, SUBSCRIPTION);
        addIndex(collection, VALID, SUBSCRIPTION, END_DATE);
    }

    protected void addIndex(DBCollection collection, String... fields) {
        String indName = fields[0];
        BasicDBObject keys = new BasicDBObject(fields[0], 1);

        for (int i = 1; i < fields.length; i++) {
            indName += "-" + fields[i];
            keys.append(fields[i], 1);
        }

        collection.ensureIndex(keys, indName);
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

    protected void invalidateExpiredSubscriptions() throws IOException {
        Set<String> ids = getExpiredSubscriptions(ON_PREMISES);
        if (!ids.isEmpty()) {
            for (String subscriptionId : ids) {
                try {
                    invalidateSubscription(subscriptionId);
                } catch (Exception e) {
                    LOG.error("Can't invalidate subscriptions.", e);
                }
            }
        }
    }

    protected class SubscriptionInvalidator extends TimerTask {
        @Override
        public void run() {
            LOG.info("Subscription invalidator has been started");

            try {
                invalidateExpiredSubscriptions();
            } catch (Exception e) {
                LOG.error("Can't invalidate subscriptions.", e);
            }
        }
    }
}
