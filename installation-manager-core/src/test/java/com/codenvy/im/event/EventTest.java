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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;

/**
 * @author Anatoly Bazko
 * @author Dmytro Nochevnov
 */
public class EventTest {

    public static final Event.Type TEST_EVENT = Event.Type.IM_ARTIFACT_DOWNLOADED;
    public static final String STRING_LENGTH_21   = StringUtils.repeat("1", 21);
    public static final String STRING_LENGTH_101  = StringUtils.repeat("1", 101);
    public static final String STRING_LENGTH_1001 = StringUtils.repeat("1", 1001);

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "The length of parameter PARAM value '[1]{101}' exceeded the length in " + Event.MAX_PARAM_VALUE_LENGTH + " characters")
    public void shouldThrowExceptionIfValidationFailedForValue() throws UnsupportedEncodingException {
        Map<String, String> parameters = ImmutableMap.of("PARAM", STRING_LENGTH_21);
        new Event(TEST_EVENT, parameters);   // there should be no exceptions here

        parameters = ImmutableMap.of("PARAM", STRING_LENGTH_101);
        new Event(TEST_EVENT, parameters);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "The length of parameter " + Event.ERROR_MESSAGE_PARAM + " value '[1]{1001}' exceeded the length in " + Event.MAX_LONG_PARAM_VALUE_LENGTH + " characters")
    public void shouldThrowExceptionOnLongErrorMessageParameter() throws UnsupportedEncodingException {
        Map<String, String> parameters = ImmutableMap.of(Event.ERROR_MESSAGE_PARAM, STRING_LENGTH_101);
        new Event(TEST_EVENT, parameters);   // there should be no exceptions here

        parameters = ImmutableMap.of(Event.ERROR_MESSAGE_PARAM, STRING_LENGTH_1001);
        new Event(TEST_EVENT, parameters);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "The length of parameter name '[1]{21}' exceeded the length in " + Event.MAX_PARAM_NAME_LENGTH + " characters")
    public void shouldThrowExceptionIfValidationFailedForParam() throws UnsupportedEncodingException {
        Map<String, String> parameters = new HashMap<String, String>() {{
            put(STRING_LENGTH_21, "value");
        }};

        new Event(TEST_EVENT, parameters);
    }

    @Test
    public void shouldLogEventWithoutParameters() throws UnsupportedEncodingException {
        Event event = new Event(TEST_EVENT, new HashMap<>());
        assertEquals(event.toString(), format("EVENT#%s#", TEST_EVENT));
    }

    @Test
    public void shouldLogEventWithParametersSpecialCharactersUseCase1() throws UnsupportedEncodingException {
        Map<String, String> parameters = new LinkedHashMap<String, String>() {{
            put("p1", ",");
            put("p2", "=");
            put("p3", "#");
            put("p4", " ");
        }};

        Event event = new Event(TEST_EVENT, parameters);
        assertEquals(event.toString(), "EVENT#" + TEST_EVENT + "# p1#,# p2#=# p3### p4# #");
    }

}
