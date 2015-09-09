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

import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;

public class EventTest {

    public static final Event.Type TEST_EVENT = Event.Type.IM_ARTIFACT_DOWNLOADED;

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

        new Event(TEST_EVENT, parameters);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionIfValidationFailedForParam() throws UnsupportedEncodingException {
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("0123456789012345678901234567890123456789", "value");
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
