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

/** @author Alexander Reshetnyak */
public class DownloadStatusInfo {

    private final Status status;
    private final int    percents;
    private final String downloadResult;

    /**
     * @param status
     *         the status of downloading process
     * @param percents
     *         the percentage of done downloading artifacts
     */
    public DownloadStatusInfo(Status status, int percents) {
        this(status, percents, null);
    }

    /**
     * @param status
     *         the status of downloading process
     * @param percents
     *         the percentage of done downloading artifacts
     * @param downloadResult
     *         the result of downloading in json format
     */
    public DownloadStatusInfo(Status status, int percents, String downloadResult) {
        this.status = status;
        this.percents = percents;
        this.downloadResult = downloadResult;
    }

    public Status getStatus() {
        return status;
    }

    public int getPercents() {
        return percents;
    }

    public String getDownloadResult() {
        return downloadResult == null ? null : downloadResult.replaceAll("\\\\", "");
    }

    /** Factory method. */
    public static DownloadStatusInfo valueOf(String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        JSONObject downloadInfoJson = json.getJSONObject(Property.DOWNLOAD_INFO.toString().toLowerCase());

        Status status = Status.valueOf(downloadInfoJson.getString("status"));
        int percents = downloadInfoJson.getInt("percents");
        String downloadResult = downloadInfoJson.getString("downloadResult");

        return new DownloadStatusInfo(status, percents, downloadResult);
    }
}
