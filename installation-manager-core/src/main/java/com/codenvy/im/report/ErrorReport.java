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
package com.codenvy.im.report;

import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.MacroCommand;
import com.codenvy.im.commands.decorators.PuppetErrorInterrupter;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.utils.InjectorBootstrap;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.commons.io.FileUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.codenvy.im.commands.CommandLibrary.createCopyFromRemoteToLocalCommand;
import static com.codenvy.im.commands.CommandLibrary.createPackCommand;
import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class ErrorReport {
    private static final Path   REPORT_DIR                 = Paths.get(InjectorBootstrap.INJECTOR.getInstance(Key.get(String.class, Names.named("installation-manager.report_dir"))));
    private static final String ERROR_REPORT_NAME_TEMPLATE = "error_report_%s.tar.gz";

    protected static     String REPORT_NAME_TIME_FORMAT    = "dd-MMM-yyyy_HH-mm-ss";
    public static        Path   BASE_TMP_DIRECTORY         = Paths.get(System.getProperty("java.io.tmpdir")).resolve("codenvy");

    private static final Logger LOG = Logger.getLogger(ErrorReport.class.getSimpleName());

    /**
     * @return path to created error report, or null in case of problems with report creation
     */
    @Nullable
    public static Path create() {
        return create(null);
    }

    public static Path create(@Nullable NodeConfig node) {
        Path reportFile = createPathToErrorReport();

        try {
            // create local temp dir
            FileUtils.deleteQuietly(BASE_TMP_DIRECTORY.toFile());
            Files.createDirectory(BASE_TMP_DIRECTORY);

            // create report dir
            if (!Files.exists(reportFile.getParent())) {
                Files.createDirectory(reportFile.getParent());
            }

            Command createReportCommands = createConsolidateLogsCommand(reportFile, node);
            createReportCommands.execute();

            // remove temp dir
            FileUtils.deleteQuietly(BASE_TMP_DIRECTORY.toFile());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, format("Error report creation error: %s", e.getMessage()), e);
            return null;
        }

        return reportFile;
    }

    protected static Path createPathToErrorReport() {
        DateFormat dateFormat = new SimpleDateFormat(REPORT_NAME_TIME_FORMAT);
        String currentTime = dateFormat.format(getCurrentDate());

        String fileName = format(ERROR_REPORT_NAME_TEMPLATE, currentTime);

        return REPORT_DIR.resolve(fileName);
    }

    protected static Date getCurrentDate() {
        return new Date();
    }

    private static Command createConsolidateLogsCommand(Path reportFile, @Nullable NodeConfig node) throws IOException {
        List<Command> commands = new ArrayList<>();

        if (node == null) {
            // copy log into temp dir
            commands.add(createCommand(format("cp %s %s", PuppetErrorInterrupter.PUPPET_LOG_FILE_PATH, BASE_TMP_DIRECTORY)));

        } else {
            // copy remote log into local_temp_dir/{node_type}
            Path tempDirForNodeLog = BASE_TMP_DIRECTORY.resolve(node.getType().toString().toLowerCase());
            Files.createDirectory(tempDirForNodeLog);

            commands.add(createCopyFromRemoteToLocalCommand(PuppetErrorInterrupter.PUPPET_LOG_FILE_PATH,
                                                            tempDirForNodeLog,
                                                            node));
        }

        // pack logs into report file
        commands.add(createPackCommand(BASE_TMP_DIRECTORY, reportFile, ".", false));

        return new MacroCommand(commands, "Commands to create error report");
    }
}
