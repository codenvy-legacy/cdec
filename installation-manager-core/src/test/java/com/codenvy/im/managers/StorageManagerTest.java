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
package com.codenvy.im.managers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class StorageManagerTest {

    private StorageManager storageManager;
    private Path           storageDir;

    @BeforeMethod
    public void setUp() throws Exception {
        storageDir = Paths.get("target").resolve("storage");
        deleteDirectory(storageDir.toFile());

        storageManager = new StorageManager(storageDir.toString());
    }

    @Test
    public void testStoreLoadProperties() throws Exception {
        storageManager.storeProperties(ImmutableMap.of("x1", "y1", "x2", "y2"));

        Map<String, String> m = storageManager.loadProperties(ImmutableSet.of("x1", "x2"));
        assertEquals(m.size(), 2);
        assertEquals(m.get("x1"), "y1");
        assertEquals(m.get("x2"), "y2");

        storageManager.storeProperties(ImmutableMap.of("x1", "y2"));

        m = storageManager.loadProperties(ImmutableSet.of("x1", "x2"));
        assertEquals(m.size(), 2);
        assertEquals(m.get("x1"), "y2");
        assertEquals(m.get("x2"), "y2");
    }

    @Test
    public void testStoreEmptyMap() throws Exception {
        storageManager.storeProperties(Collections.<String, String>emptyMap());
    }

    @Test
    public void testLoadUnexistedProperties() throws Exception {
        storageManager.storeProperties(ImmutableMap.of("x1", "y1", "x2", "y2"));

        Map<String, String> m = storageManager.loadProperties(ImmutableSet.of("x3"));
        assertTrue(m.isEmpty());
    }

    @Test
    public void testLoadEmptyMap() throws Exception {
        Map<String, String> m = storageManager.loadProperties(Collections.<String>emptyList());
        assertTrue(m.isEmpty());
    }
}