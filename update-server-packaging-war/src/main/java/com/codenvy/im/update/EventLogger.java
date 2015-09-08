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


import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
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

    private final UserManager userManager;

    @Inject
    public EventLogger(UserManager userManager) {
        this.userManager = userManager;
    }

    public void log(String eventName, Map<String, String> initialEventParameters) {
        Map<String, String> eventParameters = new HashMap<>(initialEventParameters);

        eventParameters.put(TIME_PARAM, getTime());

        String userId = userManager.getCurrentUser().getId();
        eventParameters.put(USER_PARAM, userId == null ? "" : userId);

        StringBuilder record = new StringBuilder("EVENT#" + eventName);

        for (Map.Entry<String, String> entry : eventParameters.entrySet()) {
            record.append(format(" %s#%s#", entry.getKey(), entry.getValue()));
        }

        getLogger().info(record.toString());
    }

    String getTime() {
        return String.valueOf(System.currentTimeMillis());
    }

    Logger getLogger() {
        return LoggerFactory.getLogger(EventLogger.class);
    }

}
