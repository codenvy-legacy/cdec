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
package com.codenvy.cdec.utils;

import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.cdec.ArtifactNotFoundException;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Anatoliy Bazko
 */
public class TestCommons {

    public static final Path DOWNLOAD_DIR = Paths.get("target", "download");

    @Test
    public void testGetPrettyPrintJson() throws Exception {
        String value = Commons.getPrettyPrintingJson("{a:1,b:{bb:2},c:[{c1:1},{c2:2}]}");
        assertEquals(value, "{\n" +
                            "  \"a\": 1,\n" +
                            "  \"b\": {\"bb\": 2},\n" +
                            "  \"c\": [\n" +
                            "    {\"c1\": 1},\n" +
                            "    {\"c2\": 2}\n" +
                            "  ]\n" +
                            "}");
    }

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
    public void testMapFromJson() throws Exception {
        Map m = Commons.fromJson("{a=b,c=d}", Map.class);
        assertEquals(m.size(), 2);
        assertEquals(m.get("a"), "b");
        assertEquals(m.get("c"), "d");
    }
}
