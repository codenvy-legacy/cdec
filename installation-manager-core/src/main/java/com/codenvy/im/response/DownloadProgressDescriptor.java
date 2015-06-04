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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Is used when downloading in progress.
 *
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@JsonPropertyOrder({"artifacts", "percents", "message", "status"})
public class DownloadProgressDescriptor {

    private int                        percents;
    private String                     message;
    private DownloadArtifactStatus     status;
    private List<DownloadArtifactInfo> artifacts;

    public DownloadProgressDescriptor() {
    }

    /**
     * @param status
     *         the status of downloading process
     * @param message
     *         might contain message in case of error
     * @param percents
     *         the percentage of done downloading artifacts
     * @param artifacts
     *         the result of downloaded artifacts
     */
    public DownloadProgressDescriptor(@Nonnull DownloadArtifactStatus status,
                                      @Nullable String message,
                                      @Nonnegative int percents,
                                      @Nonnull List<DownloadArtifactInfo> artifacts) {
        this.status = status;
        this.percents = percents;
        this.message = message;
        this.artifacts = new ArrayList<>(artifacts);
    }

    /**
     * @param status
     *         the status of downloading process
     * @param percents
     *         the percentage of done downloading artifacts
     * @param artifacts
     *         the result of downloaded artifacts
     */
    public DownloadProgressDescriptor(@Nonnull DownloadArtifactStatus status,
                                      @Nonnegative int percents,
                                      @Nonnull List<DownloadArtifactInfo> artifacts) {
        this(status, null, percents, artifacts);
    }

    public DownloadArtifactStatus getStatus() {
        return status;
    }

    public int getPercents() {
        return percents;
    }

    public List<DownloadArtifactInfo> getArtifacts() {
        return artifacts;
    }

    public String getMessage() {
        return message;
    }

    public void setPercents(int percents) {
        this.percents = percents;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(DownloadArtifactStatus status) {
        this.status = status;
    }

    public void setArtifacts(List<DownloadArtifactInfo> artifacts) {
        this.artifacts = artifacts;
    }
}
