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

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;

import com.codenvy.cdec.ArtifactNotFoundException;
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
import java.util.Date;
import java.util.Map;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class MongoStorage {
    public static final String ARTIFACTS_COLLECTION = "artifacts";

    private static final Logger LOG = LoggerFactory.getLogger(MongoStorage.class);

    private final DB             db;
    private final String dir;
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
        clause.put("userId", userId);
        clause.put("artifact", artifact);

        DBObject order = new BasicDBObject("date", -1);

        DBCursor cursor = collection.find(clause).sort(order).limit(1);
        if (cursor.hasNext()) {
            DBObject doc = cursor.next();
            doc.removeField("_id");
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
        doc.put("userId", userId);
        doc.put("artifact", artifact);
        doc.put("version", version);
        doc.put("date", new Date());

        collection.save(doc);
    }

    protected DB getDb() {
        return db;
    }

    private void initCollections() {
        DBCollection collection = db.getCollection(ARTIFACTS_COLLECTION);
        collection.ensureIndex(new BasicDBObject("userId", 1).append("artifact", 1).append("date", -1));
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
