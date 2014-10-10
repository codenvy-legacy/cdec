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
package com.codenvy.im.cli.command;

import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.Status;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.json.JSONException;

import java.util.UUID;

import static com.codenvy.im.response.ResponseCode.OK;
import static java.lang.Thread.sleep;

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "codenvy", name = "im-download", description = "Download artifacts")
public class DownloadCommand extends AbstractIMCommand {

    @Argument(index = 0, name = "artifact", description = "The name of the artifact to download", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to download", required = false, multiValued = false)
    private String version;

    @Option(name = "--list-local", aliases = "-l", description = "To show the downloaded list of artifacts", required = false)
    private boolean listLocal;

    @Option(name = "--check-remote", aliases = "-c", description = "To check on remote versions to see if new version is available",
            required = false)
    private boolean checkRemote;

    @Override
    protected Void doExecute() {
        try {
            init();

            if (listLocal) {
                doList();

            } else if (checkRemote) {
                doCheck();

            } else {
                doDownload();
            }
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }

    private void doDownload() throws JSONException, InterruptedException {
        printInfo("Downloading might takes several minutes depending on your internet connection. Please wait. \n");

        final String downloadDescriptorId = generateDownloadDescriptorId();

        String startResponse;
        if (artifactName != null && version != null) {
            startResponse = installationManagerProxy.startDownload(artifactName, version, downloadDescriptorId, getCredentialsRep());
        } else if (artifactName != null) {
            startResponse = installationManagerProxy.startDownload(artifactName, downloadDescriptorId, getCredentialsRep());
        } else {
            startResponse = installationManagerProxy.startDownload(downloadDescriptorId, getCredentialsRep());
        }

        if (!OK.in(startResponse)) {
            printResponse(startResponse);
            return;
        }

        for (; ; ) {
            String statusResponse = installationManagerProxy.downloadStatus(downloadDescriptorId);

            if (!OK.in(startResponse)) {
                printResponse(statusResponse);
                break;
            }

            DownloadStatusInfo downloadStatusInfo = DownloadStatusInfo.valueOf(statusResponse);

            printProgress(downloadStatusInfo.getPercents());
            sleep(1000);

            if (downloadStatusInfo.getStatus() == Status.DOWNLOADED) {
                cleanCurrentLine();

                printResponse(downloadStatusInfo.getDownloadResult());
                break;
            }
        }
    }

    private void doCheck() {
        printResponse(installationManagerProxy.getUpdates(getCredentialsRep()));
    }

    private void doList() {
        if (artifactName != null && version != null) {
            printResponse(installationManagerProxy.getDownloads(artifactName, version));
        } else if (artifactName != null) {
            printResponse(installationManagerProxy.getDownloads(artifactName));
        } else {
            printResponse(installationManagerProxy.getDownloads());
        }
    }

    protected String generateDownloadDescriptorId() {
        return UUID.randomUUID().toString();
    }
}
