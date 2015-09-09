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
package com.codenvy.im.event;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Dmytro Nochevnov
 */
public class EventFactory {
    public static final String TIME_PARAM     = "TIME";
    public static final String USER_PARAM     = "USER";
    public static final String PLAN_PARAM     = "PLAN";
    public static final String ARTIFACT_PARAM = "ARTIFACT";
    public static final String VERSION_PARAM  = "VERSION";
    public static final String USER_IP_PARAM  = "USER-IP";


    public static Event create(Event.Type type, Map<String, String> initialEventParameters) {
        Map<String, String> eventParameters = new LinkedHashMap<>(initialEventParameters);

        eventParameters.put(TIME_PARAM, getTime());

        return new Event(type, eventParameters);
    }

    private static String getTime() {
        return String.valueOf(System.currentTimeMillis());
    }

}
