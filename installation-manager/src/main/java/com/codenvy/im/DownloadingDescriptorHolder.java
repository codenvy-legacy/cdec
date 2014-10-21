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
package com.codenvy.im;

import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Alexander Reshetnyak
 */
@Singleton
public class DownloadingDescriptorHolder {

    private final LinkedHashMap<String, DownloadingDescriptor> downloadingMap;

    public DownloadingDescriptorHolder() {
        final int MAX_ENTRIES = 10;

        this.downloadingMap = new LinkedHashMap<String, DownloadingDescriptor>(MAX_ENTRIES + 1) {
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }

    public DownloadingDescriptor get(String downloadDescriptorId) {
        return downloadingMap.get(downloadDescriptorId);
    }

    public void put(String downloadDescriptorId, DownloadingDescriptor downloadingDescriptor) throws IllegalStateException {
        if (!downloadingMap.containsKey(downloadDescriptorId)) {
            downloadingMap.put(downloadDescriptorId, downloadingDescriptor);
        } else {
            throw new IllegalStateException("Download descriptor already exists");
        }
    }
}
