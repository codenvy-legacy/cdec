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
package com.codenvy.im.commands.decorators;

import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.MacroCommand;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.utils.InjectorBootstrap;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.commons.io.FileUtils;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codenvy.im.commands.CommandLibrary.createChmodCommand;
import static com.codenvy.im.commands.CommandLibrary.createCopyCommand;
import static com.codenvy.im.commands.CommandLibrary.createCopyFromRemoteToLocalCommand;
import static com.codenvy.im.commands.CommandLibrary.createPackCommand;
import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class PuppetErrorReport {
    private static final Path   REPORT_DIR                 = Paths.get(InjectorBootstrap.INJECTOR.getInstance(Key.get(String.class, Names.named("installation-manager.report_dir"))));
    private static final String ERROR_REPORT_NAME_TEMPLATE = "error_report_%s.tar.gz";
    private static final Logger LOG                        = Logger.getLogger(PuppetErrorReport.class.getSimpleName());

    protected static     String REPORT_NAME_TIME_FORMAT    = "dd-MMM-yyyy_HH-mm-ss";

    public static        Path   BASE_TMP_DIRECTORY         = Paths.get(System.getProperty("java.io.tmpdir")).resolve("codenvy");

    private static final Path CLI_CLIENT_INTERACTIVE_MODE_LOG = Paths.get(System.getProperty("java.io.tmpdir")).getParent().resolve("im-interactive.log");
    protected static final Path CLI_CLIENT_NON_INTERACTIVE_MODE_LOG = Paths.get(System.getProperty("java.io.tmpdir")).getParent().resolve("im-non-interactive.log");
    private static final Path INSTALLATION_MANAGER_SERVER_LOG = (System.getProperty("catalina.base") != null) ?
        Paths.get(System.getProperty("catalina.base")).resolve("logs").resolve("catalina.out") : null;

    protected static boolean useSudo = true;  // for testing

    /**
     * @return path to created error report, or null in case of problems with report creation
     */
    @Nullable
    public static Path create() {
        return create(null);
    }

    @Nullable
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
        } catch (IOException e) {
            LOG.log(Level.SEVERE, format("Error report creation error: %s", e.getMessage()), e);
            return null;
        } finally {
            // remove temp dir
            FileUtils.deleteQuietly(BASE_TMP_DIRECTORY.toFile());
        }

        return reportFile;
    }

    private static Path createPathToErrorReport() {
        DateFormat dateFormat = new SimpleDateFormat(REPORT_NAME_TIME_FORMAT);
        String currentTime = dateFormat.format(new Date());

        String fileName = format(ERROR_REPORT_NAME_TEMPLATE, currentTime);

        return REPORT_DIR.resolve(fileName);
    }

    /**
     * Given:
     * - path to report file
     * - node config
     * @return command which:
     * I. If node == null:
     * - copy puppet log file into local temp dir
     * - copy installation manager logs
     * - pack temp dir into report file
     *
     * II. If node != null:
     * - create local temp dir for puppet log file from node with name = type_of_node
     * - create remote temp dir
     * - copy file with puppet logs into the remote temp dir
     * - change permission of file with puppet logs to the 666 to be able to copy it to local machine
     * - copy remote puppet log file into the local_temp_dir/{node_type}/ directory
     * - cleanup remote temp dir
     * - copy installation manager logs
     * - pack local temp dir into report file
     */
    private static Command createConsolidateLogsCommand(Path reportFile, @Nullable NodeConfig node) throws IOException {
        List<Command> commands = new ArrayList<>();

        if (node == null) {
            // copy puppet log file into temp dir
            commands.add(createCopyCommand(PuppetErrorInterrupter.PUPPET_LOG_FILE_PATH, BASE_TMP_DIRECTORY));

        } else {
            // create local temp dir for puppet log file from node with name = type_of_node
            Path tempDirForNodeLog = BASE_TMP_DIRECTORY.resolve(node.getType().toString().toLowerCase());
            commands.add(createCommand(format("mkdir -p %s", tempDirForNodeLog)));

            // create remote temp dir
            Path remoteTempDir = Paths.get("/tmp/codenvy");
            commands.add(createCommand(format("mkdir -p %s", remoteTempDir), node));

            // copy file with puppet logs into the remote temp dir
            commands.add(createCopyCommand(PuppetErrorInterrupter.PUPPET_LOG_FILE_PATH, remoteTempDir, node, useSudo));

            // change permission of file with puppet logs to the 666 to be able to copy it to local machine
            Path remoteLogFile = remoteTempDir.resolve(PuppetErrorInterrupter.PUPPET_LOG_FILE_PATH.getFileName());
            commands.add(createChmodCommand("666", remoteLogFile, node, useSudo));

            // copy remote puppet log file into the local_temp_dir/{node_type}/ directory
            commands.add(createCopyFromRemoteToLocalCommand(remoteLogFile,
                                                            tempDirForNodeLog,
                                                            node));

            // cleanup remote temp dir
            commands.add(createCommand(format("rm -rf %s", remoteTempDir), node));
        }

        // copy installation manager logs
        if (Files.exists(CLI_CLIENT_INTERACTIVE_MODE_LOG)) {
            commands.add(createCopyCommand(CLI_CLIENT_INTERACTIVE_MODE_LOG, BASE_TMP_DIRECTORY));
        }

        if (Files.exists(CLI_CLIENT_NON_INTERACTIVE_MODE_LOG)) {
            commands.add(createCopyCommand(CLI_CLIENT_NON_INTERACTIVE_MODE_LOG, BASE_TMP_DIRECTORY));
        }

        if (INSTALLATION_MANAGER_SERVER_LOG != null && Files.exists(INSTALLATION_MANAGER_SERVER_LOG)) {
            commands.add(createCopyCommand(INSTALLATION_MANAGER_SERVER_LOG, BASE_TMP_DIRECTORY));
        }

        // pack local temp dir into report file
        commands.add(createPackCommand(BASE_TMP_DIRECTORY, reportFile, ".", false));

        return new MacroCommand(commands, "Commands to create error report");
    }

}
