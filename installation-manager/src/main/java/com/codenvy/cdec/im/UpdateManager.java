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

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.user.UserCredentials;
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
import java.util.Map;

/**
 * Checks and downloads updates by schedule.
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class UpdateManager {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateManager.class);

    private final String              updateSchedule;
    private final boolean             downloadAutomatically;
    private final InstallationManager manager;

    private Scheduler scheduler;

    @Inject
    public UpdateManager(@Named("codenvy.installation-manager.check_update_schedule") String updateSchedule,
                         @Named("codenvy.installation-manager.download_automatically") boolean downloadAutomatically,
                         InstallationManager manager) throws IOException {
        this.manager = manager;
        this.updateSchedule = updateSchedule;
        this.downloadAutomatically = downloadAutomatically;
    }

    @PostConstruct
    public void init() throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();

        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(UpdateJob.class.getName()));
        jobDetail.setJobClass(UpdateJob.class);
        jobDetail.setDurability(true);

        scheduler.scheduleJob(jobDetail, TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(updateSchedule)).build());

    }

    @PreDestroy
    public void destroy() throws SchedulerException {
        if (scheduler != null) {
            scheduler.shutdown(true);
        }
    }

    /**
     * Job to check and download updates.
     */
    public class UpdateJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            LOG.info("Checking new updates started");

            try {
                UserCredentials userCredentials = getUserCredentials();

                Map<Artifact, String> updates = manager.getUpdates("auth token");

                if (!updates.isEmpty() && downloadAutomatically) {
                    for (Map.Entry<Artifact, String> entry : updates.entrySet()) {
                        Artifact artifact = entry.getKey();
                        String version = entry.getValue();

                        manager.download(userCredentials, artifact, version);
                    }
                }
            } catch (Exception e) {
                throw new JobExecutionException(e);
            } finally {
                LOG.info("Checking new updates finished");
            }
        }

        private UserCredentials getUserCredentials() {
            String authToken = getAuthToken();
            String accountId = getAccountId();
            
            return new UserCredentials(authToken, accountId);
        }
        
        private String getAuthToken() {
            return null; // TODO
        }
        
        private String getAccountId() {
            return null; // TODO
        }
    }
}
