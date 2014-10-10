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

/**
 * @author Alexander Reshetnyak
 */
public class DownloadStatusInfo {

    private Status status;
    private long   percents;
    private String downloadResult;

    /**
     * Constructor.
     *
     * @param status  Status
     *          Status of downloading process.
     * @param percents  long
     *          percents of done downloading artifacts.
     */
    public DownloadStatusInfo(Status status, long percents) {
        this(status, percents, null);
    }

    /**
     * Constructor.
     *
     * @param status
     *          Status of downloading process.
     * @param percents long
     *          percents of done downloading artifacts.
     * @param downloadResult String
     *          the result of download.
     *
     */
    public DownloadStatusInfo(Status status, long percents, String downloadResult) {
        this.status = status;
        this.percents = percents;
        this.downloadResult = downloadResult;
    }

    public Status getStatus() {
        return status;
    }

    public long getPercents() { return percents; }

    public String getDownloadResult() { return downloadResult; }
}
