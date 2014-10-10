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

import com.codenvy.im.response.Property;
import com.codenvy.im.response.Status;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.json.JSONException;
import org.json.JSONObject;

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

            printProgress(getPercents(statusResponse));
            sleep(1000);

            if (isDownloadedStatusResponse(statusResponse)) { // TODO 100% better to determine ?
                cleanCurrentLine();

                printResponse(getDownloadResult(statusResponse));
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

    // TODO
    private boolean isDownloadedStatusResponse(String response) throws JSONException {
        JSONObject downloadStatusInfo = getJsonDownloadStatusInfo(response);

        if (downloadStatusInfo != null) {
            String downloadStatus = downloadStatusInfo.getString(Property.STATUS.toString().toLowerCase());
            return downloadStatus != null && Status.DOWNLOADED.toString().equals(downloadStatus);
        }

        return false;
    }

    // TODO print example
    private JSONObject getJsonDownloadStatusInfo(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.getJSONObject(Property.DOWNLOAD_INFO.toString().toLowerCase());
    }

    // TODO
    private int getPercents(String json) throws JSONException {
        JSONObject downloadStatusInfo = getJsonDownloadStatusInfo(json);

        if (downloadStatusInfo != null) {
            String percents = downloadStatusInfo.getString("percents");
            if (percents != null) {
                return Integer.valueOf(percents);
            }

            throw new IllegalStateException("Can't extract percents from DownloadStatusInfo :" + json);
        }

        throw new IllegalStateException("Can't extract DownloadStatusInfo from :" + json);
    }

    // TODO
    private String getDownloadResult(String json) throws JSONException {
        JSONObject downloadStatusInfo = getJsonDownloadStatusInfo(json);

        if (downloadStatusInfo != null) {
            String downloadResult = downloadStatusInfo.getString("downloadResult");
            if (downloadResult != null) {
                return downloadResult.replaceAll("\\\\", "");
            }

            throw new IllegalStateException("Can't extract downloadResult from DownloadStatusInfo :" + json);
        }

        throw new IllegalStateException("Can't extract DownloadStatusInfo from :" + json);
    }
}
