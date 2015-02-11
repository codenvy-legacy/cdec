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
package com.codenvy.im.response;

/** @author Alexander Reshetnyak */
public class DownloadStatusInfo {

    private Status   status;
    private int      percents;
    private Response downloadResult;

    public DownloadStatusInfo() {
    }

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
     *         the result of downloading
     */
    public DownloadStatusInfo(Status status, int percents, Response downloadResult) {
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

    public Response getDownloadResult() {
        return downloadResult;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setPercents(int percents) {
        this.percents = percents;
    }

    public void setDownloadResult(Response downloadResult) {
        this.downloadResult = downloadResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DownloadStatusInfo)) return false;

        DownloadStatusInfo that = (DownloadStatusInfo)o;

        if (percents != that.percents) return false;
        if (!downloadResult.toJson().equals(that.downloadResult.toJson())) return false;
        return status == that.status;
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + percents;
        result = 31 * result + downloadResult.hashCode();
        return result;
    }
}
