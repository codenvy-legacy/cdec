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

import com.google.common.collect.ImmutableMap;
import org.eclipse.che.commons.user.UserImpl;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class EventLoggerTest {

    public static final String TEST_TIME = String.valueOf(System.currentTimeMillis());

    @Mock
    private Logger mockLogger;

    @Mock
    private UserManager mockUserManager;

    private EventLogger spyEventLogger;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        spyEventLogger = spy(new EventLogger(mockUserManager));
        doReturn(TEST_TIME).when(spyEventLogger).getTime();
        doReturn(mockLogger).when(spyEventLogger).getLogger();

        when(mockUserManager.getCurrentUser()).thenReturn(new UserImpl("name", "id", "token", Collections.<String>emptyList(), false));
    }

    @Test
    public void testLog() throws Exception {
        Map<String, String> testEventParameters = ImmutableMap.of(
            "PARAM1", "param1-value",
            "PARAM2", "param2-value"
        );

        String testEventName = "name";

        spyEventLogger.log(testEventName, testEventParameters);


        verify(mockLogger).info(java.lang.String.format("EVENT#name PARAM2#param2-value# PARAM1#param1-value# TIME#%s# USER#id#", TEST_TIME));
    }

    @Test
    public void testGetTime() throws Exception {
        EventLogger eventLogger = new EventLogger(mockUserManager);
        String time = eventLogger.getTime();
        assertTrue(time.matches("\\d*"), time);
    }

    @Test
    public void testGetLogger() throws Exception {
        EventLogger eventLogger = new EventLogger(mockUserManager);
        Logger logger = eventLogger.getLogger();
        assertNotNull(logger);
        assertEquals(logger.getName(), EventLogger.class.getName());
    }
}
