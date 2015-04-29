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


import com.codenvy.im.request.Request;
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.eclipse.che.commons.json.JsonParseException;

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
    protected void doExecuteCommand() throws Exception {
        if (listLocal) {
            doList();

        } else if (checkRemote) {
            doCheck();

        } else {
            doDownload();
        }
    }

    private void doDownload() throws InterruptedException, JsonParseException {
        console.println("Downloading might take several minutes depending on your internet connection. Please wait.");

        Request request = createRequest(artifactName, version);
        String startResponse = facade.startDownload(request);

        Response responseObj = Response.fromJson(startResponse);
        if (responseObj.getStatus() != ResponseCode.OK) {
            console.printErrorAndExit(startResponse);
            return;
        }

        boolean isCanceled = false;

        for (; ; ) {
            String response = facade.getDownloadStatus();
            if (Response.fromJson(startResponse).getStatus() != ResponseCode.OK) {
                console.cleanCurrentLine();
                console.printErrorAndExit(response);
                break;
            }

            DownloadStatusInfo downloadStatusInfo = Response.fromJson(response).getDownloadInfo();

            if (!isCanceled) {
                console.printProgress(downloadStatusInfo.getPercents());
            }

            try {
                sleep(1000);
            } catch (InterruptedException ie) {
                facade.stopDownload();
                console.cleanLineAbove();
                isCanceled = true;
            }

            if (downloadStatusInfo.getStatus() == Status.DOWNLOADED || downloadStatusInfo.getStatus() == Status.FAILURE) {
                console.cleanCurrentLine();
                String downloadResult = downloadStatusInfo.getDownloadResult().toJson();
                if (downloadStatusInfo.getStatus() == Status.FAILURE) {
                    console.printErrorAndExit(downloadResult);
                } else {
                    console.println(downloadResult);
                }
                break;
            }
        }
    }

    private void doCheck() throws JsonParseException {
        console.printResponse(facade.getUpdates());
    }

    private void doList() throws JsonParseException {
        Request request = createRequest(artifactName, version);
        console.printResponse(facade.getDownloads(request));
    }
}
