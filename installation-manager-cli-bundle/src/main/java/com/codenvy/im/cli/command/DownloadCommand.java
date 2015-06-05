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
package com.codenvy.im.cli.command;


import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.managers.DownloadAlreadyStartedException;
import com.codenvy.im.managers.DownloadNotStartedException;
import com.codenvy.im.response.DownloadArtifactInfo;
import com.codenvy.im.response.DownloadArtifactStatus;
import com.codenvy.im.response.DownloadProgressDescriptor;
import com.codenvy.im.response.DownloadResult;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.UpdatesArtifactInfo;
import com.codenvy.im.response.UpdatesResult;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.eclipse.che.commons.json.JsonParseException;

import java.io.IOException;
import java.util.Collection;

import static com.codenvy.im.utils.Commons.createArtifactOrNull;
import static com.codenvy.im.utils.Commons.createVersionOrNull;
import static com.codenvy.im.utils.Commons.toJson;
import static java.lang.Thread.sleep;

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "codenvy", name = "im-download", description = "Download artifacts or print the list of installed ones")
public class DownloadCommand extends AbstractIMCommand {

    @Argument(index = 0, name = "artifact", description = "The name of the artifact to download", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to download", required = false, multiValued = false)
    private String versionNumber;

    @Option(name = "--list-local", aliases = "-l", description = "To show the list of downloaded artifacts", required = false)
    private boolean listLocal;

    @Option(name = "--check-remote", aliases = "-c", description = "To check on remote versions to see if new version is available", required = false)
    private boolean checkRemote;

    @Override
    protected void doExecuteCommand() throws Exception {
        if (listLocal) {
            doList();

        } else if (checkRemote) {
            doCheck();

        } else {
            doDownload();
        }
    }

    private void doDownload() throws InterruptedException,
                                     JsonParseException,
                                     IOException,
                                     DownloadAlreadyStartedException,
                                     DownloadNotStartedException {
        console.println("Downloading might take several minutes depending on your internet connection. Please wait.");

        facade.startDownload(createArtifactOrNull(artifactName), createVersionOrNull(versionNumber));

        boolean isCanceled = false;

        for (; ; ) {
            DownloadProgressDescriptor downloadProgressDescriptor = facade.getDownloadProgress();
            DownloadResult downloadResult = new DownloadResult(downloadProgressDescriptor);

            if (downloadProgressDescriptor.getStatus() == DownloadArtifactStatus.FAILED) {
                console.cleanCurrentLine();
                console.printErrorAndExit(toJson(downloadResult));
                break;
            }

            if (!isCanceled) {
                console.printProgress(downloadProgressDescriptor.getPercents());
            }

            try {
                sleep(1000);
            } catch (InterruptedException ie) {
                facade.stopDownload();
                console.cleanLineAbove();
                isCanceled = true;
            }

            if (downloadProgressDescriptor.getStatus() != DownloadArtifactStatus.DOWNLOADING) {
                console.cleanCurrentLine();
                if (downloadProgressDescriptor.getStatus() == DownloadArtifactStatus.DOWNLOADED) {
                    console.printErrorAndExit(toJson(downloadResult));
                } else {
                    console.println(toJson(downloadResult));
                }
                break;
            }
        }
    }

    private void doCheck() throws JsonParseException, IOException {
        Collection<UpdatesArtifactInfo> updates = facade.getUpdates();
        UpdatesResult updatesResult = new UpdatesResult();
        updatesResult.setArtifacts(updates);
        updatesResult.setStatus(ResponseCode.OK);
        console.printResponse(updatesResult);
    }

    private void doList() throws JsonParseException, IOException {
        Artifact artifact = Commons.createArtifactOrNull(artifactName);
        Version version = Commons.createVersionOrNull(versionNumber);

        Collection<DownloadArtifactInfo> downloads = facade.getDownloads(artifact, version);

        DownloadResult downloadResult = new DownloadResult();
        downloadResult.setStatus(ResponseCode.OK);
        downloadResult.setArtifacts(downloads);

        console.printResponse(downloadResult);
    }
}
