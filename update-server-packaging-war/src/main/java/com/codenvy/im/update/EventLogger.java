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


import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 * @see {org.eclipse.che.api.analytics.logger.EventLogger}
 */
@Singleton
public class EventLogger {
    public static final String TIME_PARAM             = "TIME";
    public static final String USER_PARAM             = "USER";
    public static final String PLAN_PARAM             = "PLAN";
    public static final String ARTIFACT_PARAM         = "ARTIFACT";
    public static final String VERSION_PARAM          = "VERSION";
    public static final String USER_IP                = "USER-IP";
    public static final String IM_ARTIFACT_DOWNLOADED = "im-artifact-downloaded";
    public static final String IM_SUBSCRIPTION_ADDED  = "im-subscription-added";

    private static final int MAX_EXTENDED_PARAMS_NUMBER = 3;
    private static final int RESERVED_PARAMS_NUMBER     = 6;
    private static final int MAX_PARAM_NAME_LENGTH      = 20;
    private static final int MAX_PARAM_VALUE_LENGTH     = 100;
    private static final int QUEUE_MAX_CAPACITY         = 10000;

    private static final Set<String> ALLOWED_EVENTS = ImmutableSet.of(IM_ARTIFACT_DOWNLOADED,
                                                                      IM_SUBSCRIPTION_ADDED);

    private final Thread        logThread;
    private final Queue<String> queue;
    private final UserManager   userManager;

    static Logger LOG = LoggerFactory.getLogger(EventLogger.class);   // is not final for testing propose

    /**
     * Stores the number of ignored events due to maximum queue capacity
     */
    private long ignoredEvents;


    @Inject
    public EventLogger(UserManager userManager) {
        this.userManager = userManager;
        this.queue = new LinkedBlockingQueue<>(QUEUE_MAX_CAPACITY);
        this.ignoredEvents = 0;

        logThread = new LogThread();
        logThread.setDaemon(true);
    }

    @PostConstruct
    public void init() {
        logThread.start();
    }

    @PreDestroy
    public void destroy() {
        logThread.interrupt();
    }

    public void log(String eventName, Map<String, String> eventParameters) throws UnsupportedEncodingException {
        if (!ALLOWED_EVENTS.contains(eventName)) {
            throw new IllegalArgumentException(format("Event '%s' is not accepted", eventName));
        }

        validate(eventParameters);

        String message = createMessage(eventName, eventParameters);
        if (!offerEvent(message)) {
            if (ignoredEvents++ % 1000 == 0) {
                LOG.warn("Ignored " + ignoredEvents + " events due to maximum queue capacity");
            }
        }
    }

    boolean offerEvent(String message) {
        return queue.offer(message);
    }

    private String createMessage(String eventName, Map<String, String> initialEventParameters) throws UnsupportedEncodingException {
        Map<String, String> eventParameters = new HashMap<>(initialEventParameters);

        eventParameters.put(TIME_PARAM, getTime());

        String userId = userManager.getCurrentUser().getId();
        eventParameters.put(USER_PARAM, userId == null ? "" : userId);

        StringBuilder record = new StringBuilder(format("EVENT#%s#", eventName));

        for (Map.Entry<String, String> entry : eventParameters.entrySet()) {
            record.append(format(" %s#%s#", URLEncoder.encode(entry.getKey(), "UTF-8"),
                                            URLEncoder.encode(entry.getValue(), "UTF-8")));
        }

        return record.toString();
    }

    private void validate(Map<String, String> additionalParams) throws IllegalArgumentException, UnsupportedEncodingException {
        if (additionalParams.size() > MAX_EXTENDED_PARAMS_NUMBER + RESERVED_PARAMS_NUMBER) {
            throw new IllegalArgumentException("The number of parameters exceeded the limit in " +
                                               MAX_EXTENDED_PARAMS_NUMBER);
        }

        for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
            String param = entry.getKey();
            String value = entry.getValue();

            if (param.length() > MAX_PARAM_NAME_LENGTH) {
                throw new IllegalArgumentException(
                    "The length of parameter name " + param + " exceeded the length in " + MAX_PARAM_NAME_LENGTH +
                    " characters");

            } else if (value.length() > MAX_PARAM_VALUE_LENGTH) {
                throw new IllegalArgumentException(
                    "The length of parameter value " + value + " exceeded the length in " + MAX_PARAM_VALUE_LENGTH +
                    " characters");
            }
        }
    }

    String getTime() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * Is responsible for logging events.
     * Rate-limit is 50 messages per second.
     */
    class LogThread extends Thread {

        static final int LOG_MESSAGE_TIMEOUT_MILLIS = 20;
        static final int SLEEP_ON_EMPTY_QUEUE_TIMEOUT_MILLIS = 1000;

        private LogThread() {
            super("Installation Manager Event Logger");
        }

        @Override
        public void run() {
            LOG.info(getName() + " thread is started, queue is initialized for " + QUEUE_MAX_CAPACITY + " messages");
            while (!isInterrupted()) {
                String message = queue.poll();

                try {
                    if (message != null) {
                        LOG.info(message);
                        sleep(LOG_MESSAGE_TIMEOUT_MILLIS);
                    } else {
                        sleep(SLEEP_ON_EMPTY_QUEUE_TIMEOUT_MILLIS);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }

            LOG.info(getName() + " thread is stopped");
        }
    }
}
