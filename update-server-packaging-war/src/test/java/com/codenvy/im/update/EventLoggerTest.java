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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class EventLoggerTest {

    public static final String TEST_TIME = String.valueOf(System.currentTimeMillis());
    public static final String TEST_EVENT = EventLogger.IM_ARTIFACT_DOWNLOADED;

    @Mock
    private Logger mockLogger;

    @Mock
    private UserManager mockUserManager;

    private EventLogger spyEventLogger;

    private Logger ORIGINAL_LOG = EventLogger.LOG;

    @BeforeClass
    public void classSetup() {
        ORIGINAL_LOG = EventLogger.LOG;
    }

    @BeforeMethod
    public void testSetup() throws Exception {
        MockitoAnnotations.initMocks(this);

        spyEventLogger = spy(new EventLogger(mockUserManager));
        doReturn(TEST_TIME).when(spyEventLogger).getTime();
        EventLogger.LOG = mockLogger;
        spyEventLogger.init();

        when(mockUserManager.getCurrentUser()).thenReturn(new UserImpl("name", "id", "token", Collections.<String>emptyList(), false));
    }

    @Test
    public void shouldLogEvent() throws Exception {
        Map<String, String> testEventParameters = ImmutableMap.of(
            "PARAM1", "param1-value",
            "PARAM2", "param2-value"
        );

        String testEventName = EventLogger.IM_SUBSCRIPTION_ADDED;

        spyEventLogger.log(testEventName, testEventParameters);

        Thread.sleep(2 * EventLogger.LogThread.SLEEP_ON_EMPTY_QUEUE_TIMEOUT_MILLIS);

        verify(mockLogger).info(format("EVENT#%s# PARAM2#param2-value# PARAM1#param1-value# TIME#%s# USER#id#", testEventName, TEST_TIME));
    }

    @Test
    public void shouldGetTime() throws Exception {
        EventLogger eventLogger = new EventLogger(mockUserManager);
        String time = eventLogger.getTime();
        assertTrue(time.matches("\\d*"), time);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Event 'null' is not accepted")
    public void shouldNotLogNullEvent() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        spyEventLogger.log(null, null);

        verify(spyEventLogger, never()).offerEvent(message.capture());
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Event 'arbitrary-event' is not accepted")
    public void shouldNotLogArbitraryEvent() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        spyEventLogger.log("arbitrary-event", null);

        verify(spyEventLogger, never()).offerEvent(message.capture());
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "The length of parameter value " +
                                            "012345678901234567890123456789012345678901234567891012345678901234567890123456789012345678901234567891012345678901" +
                                            "234567890123456789012345678901234567891 exceeded the length in 100 characters")
    public void shouldThrowExceptionIfValidationFailedForValue() throws UnsupportedEncodingException {
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("PARAM", "012345678901234567890123456789012345678901234567891" +
                         "012345678901234567890123456789012345678901234567891" +
                         "012345678901234567890123456789012345678901234567891");
        }};

        spyEventLogger.log(TEST_EVENT, parameters);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionIfValidationFailedForParam() throws UnsupportedEncodingException {
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("0123456789012345678901234567890123456789", "value");
        }};

        spyEventLogger.log(TEST_EVENT, parameters);
    }

    @Test
    public void shouldLogEventWithoutParameters() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        spyEventLogger.log(TEST_EVENT, new HashMap<>());

        verify(spyEventLogger, times(1)).offerEvent(message.capture());
        assertEquals(message.getValue(), format("EVENT#%s# TIME#%s# USER#id#", TEST_EVENT, TEST_TIME));
    }

    @Test
    public void shouldLogEventWithParametersSpecialCharactersUseCase1() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        Map<String, String> parameters = new LinkedHashMap<String, String>() {{
            put("p1", ",");
            put("p2", "=");
            put("p3", "#");
            put("p4", " ");
        }};

        spyEventLogger.log(TEST_EVENT, parameters);

        verify(spyEventLogger, times(1)).offerEvent(message.capture());
        assertEquals(message.getValue(), "EVENT#" + TEST_EVENT + "# p1#%2C# p2#%3D# TIME#" + TEST_TIME + "# p3#%23# p4#+# USER#id#");
    }


    @AfterMethod
    public void testTearDown() {
        spyEventLogger.destroy();
    }

    @AfterClass
    public void classTearDown() {
        EventLogger.LOG = ORIGINAL_LOG;
    }
}
