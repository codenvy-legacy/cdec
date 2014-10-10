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

/** @author Alexander Reshetnyak */
public class DownloadStatusInfo {

    private final Status status;
    private final long   percents;
    private final String downloadResult;

    /**
     * @param status
     *         the status of downloading process
     * @param percents
     *         the percentage of done downloading artifacts
     */
    public DownloadStatusInfo(Status status, long percents) {
        this(status, percents, null);
    }

    /**
     * @param status
     *         the status of downloading process
     * @param percents
     *         the percentage of done downloading artifacts
     * @param downloadResult
     *         the result of download.
     */
    public DownloadStatusInfo(Status status, long percents, String downloadResult) {
        this.status = status;
        this.percents = percents;
        this.downloadResult = downloadResult; // TODO what is it?
    }

    public Status getStatus() {
        return status;
    }

    public long getPercents() {
        return percents;
    }

    public String getDownloadResult() {
        return downloadResult;
    }

    /** Factory method. */
    public DownloadStatusInfo valueOf(String response) {
        return null; // TODO factory method
    }
}
