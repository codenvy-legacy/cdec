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
package com.codenvy.im.command;

import com.codenvy.im.utils.TarUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.Files.setLastModifiedTime;
import static org.apache.commons.io.IOUtils.copy;

/**
 * This command unpacks archives of "tar.gz" type into the directory to unpack.
 * If directory to unpack is non-existed, it will be created.
 *
 * @author Dmytro Nochevnov
 */
public class UnpackCommand implements Command {
    private final String description;
    private final Path   pack;
    private final Path   dirToUnpack;

    private static final Logger LOG = Logger.getLogger(UnpackCommand.class.getSimpleName());

    public UnpackCommand(Path pack, Path dirToUnpack, String description) {
        this.pack = pack;
        this.dirToUnpack = dirToUnpack;
        this.description = description;
    }

    /** {@inheritDoc} */
    @Override
    public String execute() throws CommandException {
        LOG.log(Level.INFO, toString());
        try {
            TarUtils.unpack(pack, dirToUnpack);
        } catch (IOException e) {
            throw new CommandException(e.getMessage(), e);
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return description;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return format("Unpack '%s' into '%s'", pack.toString(), dirToUnpack.toString());
    }
}
