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
package com.codenvy.cdec.im;

import com.codenvy.cdec.Artifact;
import com.codenvy.cdec.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.codenvy.cdec.utils.Commons.*;
import static com.codenvy.cdec.utils.Version.compare;

/**
 * Checks and downloads updates by schedule.
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class UpdateChecker {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);

    private final String        codenvyApiEndpoint;
    private final String        codenvyUpdateEndpoint;
    private final Path          downloadDir;
    private final String        updateSchedule;
    private final boolean       downloadAutomatically;
    private final HttpTransport transport;

    private Scheduler scheduler;

    @Inject
    public UpdateChecker(@Named("codenvy.installation-manager.codenvy_api_endpoint") String codenvyApiEndpoint,
                         @Named("codenvy.installation-manager.codenvy_update_endpoint") String codenvyUpdateEndpoint,
                         @Named("codenvy.installation-manager.download_dir") String downloadDir,
                         @Named("codenvy.installation-manager.check_update_schedule") String updateSchedule,
                         @Named("codenvy.installation-manager.download_automatically") boolean downloadAutomatically,
                         HttpTransport transport) throws IOException {
        this.codenvyApiEndpoint = codenvyApiEndpoint;
        this.codenvyUpdateEndpoint = codenvyUpdateEndpoint;
        this.downloadDir = Paths.get(downloadDir);
        this.updateSchedule = updateSchedule;
        this.downloadAutomatically = downloadAutomatically;
        this.transport = transport;

        if (!Files.exists(this.downloadDir)) {
            Files.createDirectories(this.downloadDir);
        }
    }

    @PostConstruct
    public void init() throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();

        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(CheckUpdates.class.getName()));
        jobDetail.setJobClass(CheckUpdates.class);
        jobDetail.setDurability(true);

        scheduler.scheduleJob(jobDetail, TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(updateSchedule)).build());

    }

    @PreDestroy
    public void destroy() throws SchedulerException {
        scheduler.shutdown(true);
    }

    /**
     * Job to check updates.
     */
    public class CheckUpdates implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            LOG.info("Checking started");

            try {
                if (isValidSubscription(transport, codenvyApiEndpoint)) {
                    Map<String, String> newVersions = getNewVersions();
                    if (!newVersions.isEmpty() && downloadAutomatically) {
                        downloadUpdates(newVersions);
                    }
                }
            } catch (Exception e) {
                throw new JobExecutionException(e);
            }
        }

        /**
         * Downloads updates.
         */
        public void downloadUpdates(Map<String, String> artifacts) throws IOException {
            for (Map.Entry<String, String> entry : artifacts.entrySet()) {
                transport.download(combinePaths(codenvyUpdateEndpoint,
                                                "/repository/download/" + entry.getKey() + "/" + entry.getValue()), downloadDir);
            }
        }

        /**
         * @return the list of artifacts with newer versions than currently installed
         * @throws IOException
         *         if any exception occurred
         * @throws IllegalArgumentException
         *         if can't parse version of artifact
         */
        public Map<String, String> getNewVersions() throws IOException, IllegalArgumentException {
            Map<String, String> newVersions = new HashMap<>();
            Map<String, String> existed = getExistedArtifacts();
            Map<String, String> available2Download = getAvailable2DownloadArtifacts();

            for (String artifact : available2Download.keySet()) {
                if (!existed.containsKey(artifact) || compare(available2Download.get(artifact), existed.get(artifact)) > 0) {
                    newVersions.put(artifact, available2Download.get(artifact));
                }
            }

            return newVersions;
        }

        /**
         * Scans all available artifacts and returns their last versions from Update Server.
         */
        public Map<String, String> getAvailable2DownloadArtifacts() throws IOException {
            Map<String, String> available2Download = new HashMap<>();

            for (Artifact artifact : Artifact.values()) {
                try {
                    Map m = fromJson(transport.doGetRequest(combinePaths(codenvyUpdateEndpoint, "repository/version/" + artifact)), Map.class);

                    if (m != null && m.containsKey("value")) {
                        available2Download.put(artifact.toString(), (String)m.get("value"));
                    }
                } catch (IOException e) {
                    LOG.error("Can't retrieve the last version of " + artifact, e);
                }
            }

            return available2Download;
        }

        /**
         * Scans all available artifacts and returns their current versions.
         */
        public Map<String, String> getExistedArtifacts() throws IOException {
            Map<String, String> existed = new HashMap<>();
            for (Artifact artifact : Artifact.values()) {
                try {
                    existed.put(artifact.toString(), artifact.getVersion());
                } catch (IOException e) {
                    throw new IOException("Can't find out current version of " + artifact, e);
                }
            }

            return existed;
        }
    }
}
