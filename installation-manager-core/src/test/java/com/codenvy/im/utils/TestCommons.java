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
package com.codenvy.im.utils;

import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.im.exceptions.ArtifactNotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.currentThread;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Anatoliy Bazko
 */
public class TestCommons {

    public static final Path DOWNLOAD_DIR = Paths.get("target", "download");

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void testGetLatestVersionThrowExceptionIfArtifactDirectoryAbsent() throws Exception {
        Commons.getLatestVersion("installation-manager", DOWNLOAD_DIR);
    }

    @Test
    public void testGetLatestVersion() throws Exception {
        Files.createDirectories(DOWNLOAD_DIR.resolve("installation-manager").resolve("1.0.1"));
        Files.createDirectories(DOWNLOAD_DIR.resolve("installation-manager").resolve("1.0.2"));
        Files.createDirectories(DOWNLOAD_DIR.resolve("installation-manager").resolve("3.1.1"));
        Files.createDirectories(DOWNLOAD_DIR.resolve("installation-manager").resolve("4.4.1"));

        assertEquals(Commons.getLatestVersion("installation-manager", DOWNLOAD_DIR.resolve("installation-manager")), "4.4.1");
    }

    @Test
    public void testExtractVersion() throws Exception {
        assertEquals(Commons.extractVersion(Paths.get("repository", "artifact", "version", "file")), "version");
    }

    @Test
    public void testExtractArtifactName() throws Exception {
        assertEquals(Commons.extractArtifactName(Paths.get("repository", "artifact", "version", "file")), "artifact");
    }

    @Test
    public void testCombinePath() throws Exception {
        assertEquals(Commons.combinePaths("api", "update"), "api/update");
        assertEquals(Commons.combinePaths("api", "/update"), "api/update");
        assertEquals(Commons.combinePaths("api/", "update"), "api/update");
        assertEquals(Commons.combinePaths("api/", "/update"), "api/update");
    }

    @Test
    public void testAddQueryParameter() throws Exception {
        assertEquals(Commons.addQueryParam("api", "a", "b"), "api?a=b");
        assertEquals(Commons.addQueryParam("api?a=b", "c", "d"), "api?a=b&c=d");
    }

    @Test
    public void testCreateListDtoFromJson() throws Exception {
        List<MemberDescriptor> descriptors =
                Commons.createListDtoFromJson("[{userId:id,accountReference:{id:accountId,name:accountName}}]", MemberDescriptor.class);
        assertEquals(descriptors.size(), 1);

        MemberDescriptor d = descriptors.get(0);
        assertEquals(d.getUserId(), "id");
        assertNotNull(d.getAccountReference());
        assertEquals(d.getAccountReference().getId(), "accountId");
        assertEquals(d.getAccountReference().getName(), "accountName");
    }

    @Test
    public void testCreateDtoFromJson() throws Exception {
        MemberDescriptor d = Commons.createDtoFromJson("{userId:id,accountReference:{id:accountId,name:accountName}}", MemberDescriptor.class);

        assertEquals(d.getUserId(), "id");
        assertNotNull(d.getAccountReference());
        assertEquals(d.getAccountReference().getId(), "accountId");
        assertEquals(d.getAccountReference().getName(), "accountName");
    }

    @Test
    public void testMapFromJson() throws Exception {
        Map<String, String> m = Commons.fromJson("{\"a\":\"b\",\"c\":\"d\"}", Map.class);
        assertEquals(m.size(), 2);
        assertEquals(m.get("a"), "b");
        assertEquals(m.get("c"), "d");
    }

    @Test
    public void testMapToJson() throws Exception {
        AccountReference ar = DtoFactory.getInstance().createDto(AccountReference.class);
        ar.setId("id");
        ar.setName(null);

        String json = Commons.toJson(ar);
        assertEquals(json, "{\n"
                           + "  \"links\" : [ ],\n"
                           + "  \"id\" : \"id\"\n"
                           + "}");
    }

    @Test
    public void testAsMap() throws Exception {
        Map m = Commons.asMap("{\n"
                              + "  \"name\" : \"name\",\n"
                              + "  \"id\" : \"id\"\n"
                              + "}");
        assertEquals(m.size(), 2);
        assertEquals(m.get("name"), "name");
        assertEquals(m.get("id"), "id");
    }

    @Test
    public void testCalculateMD5Sum() throws Exception {
        Path testFile = Paths.get("target", "testFile");
        Files.write(testFile, "SomeText".getBytes());

        String expectedMD5Sum = "5b6865e9622804fd70324543d06869c4"; // calculated by md5sum linux command
        assertEquals(Commons.calculateMD5Sum(testFile), expectedMD5Sum);
    }

    @Test
    public void testCopyInterruptable() throws Exception {
        String context = new String(Arrays.copyOf("test content".getBytes("UTF-8"), 10000), "UTF-8");

        Path inFile = Paths.get("target", "inFile");
        Path outFile = Paths.get("target", "outFile");
        FileOutputStream out = new FileOutputStream(outFile.toFile());

        FileUtils.write(inFile.toFile(), context);

        Commons.copyInterruptable(new FileInputStream(inFile.toFile()), out);
        String output = FileUtils.readFileToString(outFile.toFile());

        assertEquals(output, context);

    }

    @Test(expectedExceptions = CopyStreamInterruptedException.class, expectedExceptionsMessageRegExp = "The copying was interrupted.")
    public void testFailCopyInterruptable() throws Exception {
        final Path inFile = Paths.get("target", "inFile");
        final Path outFile = Paths.get("target", "outFile");

        IOUtils.write(Arrays.copyOf("test content".getBytes(), 10000), new FileOutputStream(inFile.toFile()));

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try (FileOutputStream out = new FileOutputStream(outFile.toFile()); FileInputStream in = new FileInputStream(inFile.toFile())) {
                    currentThread().interrupt();
                    Commons.copyInterruptable(in, out);
                } catch (Exception ex) {
                    exception.set(ex);
                }

                latch.countDown();
            }
        });
        t.start();
        latch.await();

        assertNotNull(exception.get());
        throw exception.get();
    }

    @Test
    public void testMapToSortedAndAlignedJson() throws Exception {
        Map<String, String> testProperties = new LinkedHashMap<String, String>(){{
            put("short_prop", "1");
            put("longer_prop", "20");
            put("longest_prop", "300");
            put("null_prop", null);
        }};

        String json = Commons.toJsonWithSortedAndAlignedProperties(testProperties);
        assertEquals(json, "{\n"
                           + "  \"longer_prop\"  : \"20\",\n"
                           + "  \"longest_prop\" : \"300\",\n"
                           + "  \"short_prop\"   : \"1\"\n"
                           + "}");
    }

    @Test
    public void testEmptyMapToSortedAndAlignedJson() throws Exception {
        Map<String, String> testProperties = new LinkedHashMap<>();

        String json = Commons.toJsonWithSortedAndAlignedProperties(testProperties);
        assertEquals(json, "{ }");
    }

    @Test
    public void testOneEntryMapToSortedAndAlignedJson() throws Exception {
        Map<String, String> testProperties = new LinkedHashMap<String, String>(){{
            put("prop", "value");
        }};

        String json = Commons.toJsonWithSortedAndAlignedProperties(testProperties);
        assertEquals(json, "{\n"
                           + "  \"prop\" : \"value\"\n"
                           + "}");
    }
}
