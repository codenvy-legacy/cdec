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
package com.codenvy.im.response;

import org.json.JSONException;
import org.json.JSONObject;

import static com.codenvy.im.response.Property.STATUS;

/**
 * @author Dmytro Nochevnov
 */
public enum ResponseCode {
    OK,
    ERROR;

    /** Checks if a response has the specific status. */
    public boolean in(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);

        String statusValue = (String)jsonResponse.get(STATUS.toString().toLowerCase());
        return statusValue != null && this.toString().equals(statusValue);
    }
}