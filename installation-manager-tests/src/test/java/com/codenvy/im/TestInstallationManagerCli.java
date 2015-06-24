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

package com.codenvy.im;


import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.newDirectoryStream;
import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallationManagerCli {

    @Test
    public void test() throws Exception {
        Path dir = Paths.get(getClass().getClassLoader().getResource("bin").getFile());

        doExecute(dir.toFile(), "chmod", "+x", "lib.sh");

        try (DirectoryStream<Path> testFiles = newDirectoryStream(dir)) {
            for (Path testFile : testFiles) {
                if (testFile.getFileName().toString().startsWith("test")) {
                    doExecute(dir.toFile(), "chmod", "+x", testFile.toString());
                    doExecute(dir.toFile(), "./" + testFile.getFileName().toString());
                }
            }
        }
    }

    private void doExecute(File directory, String... commands) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(directory);
        Process process = builder.start();
        process.waitFor();

        int exitCode = process.waitFor();

        String output = IOUtils.toString(process.getInputStream());
        System.out.println(output);

        output = IOUtils.toString(process.getErrorStream());
        System.out.println(output);

        assertEquals(exitCode, 0);
    }
}
