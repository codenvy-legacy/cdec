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
package com.codenvy.im.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.Files.setLastModifiedTime;
import static org.apache.commons.io.IOUtils.copy;

/** @author Dmytro Nochevnov */
public class TarUtils {

    public static void unpack(Path pack, Path dirToUnpack) throws IOException {
        try (TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(newInputStream(pack))))) {

            if (!Files.exists(dirToUnpack)) {
                createDirectories(dirToUnpack);
            }

            TarArchiveEntry tarEntry;
            while ((tarEntry = in.getNextTarEntry()) != null) {
                Path destPath = dirToUnpack.resolve(tarEntry.getName());

                if (tarEntry.isDirectory()) {
                    if (!Files.exists(destPath)) {
                        createDirectories(destPath);
                    }
                } else {
                    try (BufferedOutputStream out = new BufferedOutputStream(newOutputStream(destPath))) {
                        copy(in, out);
                        setLastModifiedTime(destPath, FileTime.fromMillis(tarEntry.getModTime().getTime()));
                    }
                }
            }
        }
    }

    public static void packFile(Path fileToPack, Path packageFile) throws IOException {
        if (!Files.exists(fileToPack)) {
            throw new IllegalArgumentException("Packing file doesn't exist");
        }

        if (Files.isDirectory(fileToPack)) {
            throw new IllegalArgumentException("Packing item is a directory");
        }

        try (TarArchiveOutputStream out = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(newOutputStream(packageFile))))) {
            out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            TarArchiveEntry tarEntry = new TarArchiveEntry(fileToPack.toFile(),
                                                           fileToPack.getFileName().toString());
            out.putArchiveEntry(tarEntry);
            IOUtils.copy(new FileInputStream(fileToPack.toFile()), out);
            out.closeArchiveEntry();
        }
    }
}
