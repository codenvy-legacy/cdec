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

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.Status;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.json.JSONException;
import org.restlet.ext.jackson.JacksonRepresentation;

import java.util.UUID;

import static com.codenvy.im.response.Response.isError;
import static java.lang.Thread.sleep;

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "codenvy", name = "im-download", description = "Download artifacts or print the list of installed ones")
public class DownloadCommand extends AbstractIMCommand {

    @Argument(index = 0, name = "artifact", description = "The name of the artifact to download", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to download", required = false, multiValued = false)
    private String version;

    @Option(name = "--list-local", aliases = "-l", description = "To show the list of downloaded artifacts", required = false)
    private boolean listLocal;

    @Option(name = "--check-remote", aliases = "-c", description = "To check on remote versions to see if new version is available",
            required = false)
    private boolean checkRemote;

    @Override
    protected Void execute() {
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
            console.printError(e);
        }

        return null;
    }

    // TODO [AB] test interrupt

    private void doDownload() throws InterruptedException, JsonParseException, JSONException {
        console.printLn("Downloading might take several minutes depending on your internet connection. Please wait.");

        final String downloadDescriptorId = generateDownloadDescriptorId();

        String startResponse;
        if (artifactName != null && version != null) {
            startResponse = installationManagerProxy.startDownload(artifactName, version, downloadDescriptorId, getCredentialsRep());
        } else if (artifactName != null) {
            startResponse = installationManagerProxy.startDownload(artifactName, downloadDescriptorId, getCredentialsRep());
        } else {
            startResponse = installationManagerProxy.startDownload(downloadDescriptorId, getCredentialsRep());
        }

        if (isError(startResponse)) {
            console.printErrorEndExit(startResponse, DownloadCommand.this);
            return;
        }

        boolean isCanceled = false;

        for (; ; ) {
            String response = installationManagerProxy.downloadStatus(downloadDescriptorId);
            if (isError(response)) {
                console.cleanCurrentLine();
                console.printErrorEndExit(response, DownloadCommand.this);
                break;
            }

            DownloadStatusInfo downloadStatusInfo = Response.fromJson(response).getDownloadInfo();

            if (!isCanceled) {
                console.printProgress(downloadStatusInfo.getPercents());
            }

            try {
                sleep(1000);
            } catch (InterruptedException ie) {
                installationManagerProxy.stopDownload(downloadDescriptorId);
                console.cleanLineAbove();
                isCanceled = true;
            }

            if (downloadStatusInfo.getStatus() == Status.DOWNLOADED || downloadStatusInfo.getStatus() == Status.FAILURE) {
                console.cleanCurrentLine();
                String downloadResult = downloadStatusInfo.getDownloadResult().toJson();
                if (downloadStatusInfo.getStatus() == Status.FAILURE) {
                    console.printErrorEndExit(downloadResult, DownloadCommand.this);
                } else {
                    console.printLn(downloadResult);
                }
                break;
            }
        }
    }

    private void doCheck() throws JsonParseException {
        console.printResponse(installationManagerProxy.getUpdates(getCredentialsRep()), DownloadCommand.this);
    }

    private void doList() throws JsonParseException {
        JacksonRepresentation<Request> requestRep = new Request()
                .setArtifactName(artifactName)
                .setVersion(version)
                .setUserCredentials(getCredentials())
                .toRepresentation();

        console.printResponse(installationManagerProxy.getDownloads(requestRep), DownloadCommand.this);
    }

    protected String generateDownloadDescriptorId() {
        return UUID.randomUUID().toString();
    }
}
