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
import java.util.Map;

/**
 * Checks and downloads updates by schedule.
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class UpdateChecker {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);

    private final String  updateServerEndpoint;
    private final Path    downloadDir;
    private final String  updateSchedule;
    private final boolean downloadAutomatically;

    private Scheduler scheduler;

    // TODO default values
    @Inject
    public UpdateChecker(@Named("codenvy.installation-manager.update_server_endpoint") String updateServerEndpoint,
                         @Named("codenvy.installation-manager.download_dir") String downloadDir,
                         @Named("codenvy.installation-manager.check_update_schedule") String updateSchedule,
                         @Named("codenvy.installation-manager.download_automatically") boolean downloadAutomatically) throws IOException {
        this.updateServerEndpoint = updateServerEndpoint;
        this.downloadDir = Paths.get(downloadDir);
        this.updateSchedule = updateSchedule;
        this.downloadAutomatically = downloadAutomatically;

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

        scheduler.scheduleJob(jobDetail,
                              TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(updateSchedule)).build());
    }

    @PreDestroy
    public void destroy() throws SchedulerException {
        scheduler.shutdown(true);
    }

    /**
     * Checks new updates from Update Server
     */
    private class CheckUpdates implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            if (updatesAvailable()) {
                if (downloadAutomatically) {
                    downloadUpdates();
                }
            }
        }

        private void downloadUpdates() {
        }

        private boolean updatesAvailable() {
            boolean updatesAvailable = false;

            Map<String, String> artifacts = getArtifact();
            Map<String, String> newArtifacts = getAvailableArtifacts();

            for (Map.Entry<String, String> newEntry : newArtifacts.entrySet()) {
                if (!artifacts.containsKey(newEntry.getKey())) {
                    updatesAvailable = true;
                } else {

                }
            }

            return updatesAvailable;
        }

        private Map<String, String> getAvailableArtifacts() {
            return null;
        }

        private Map<String, String> getArtifact() {
            return null;
        }
    }
}
