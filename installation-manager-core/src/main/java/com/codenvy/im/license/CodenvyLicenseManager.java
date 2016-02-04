/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
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

package com.codenvy.im.license;

import com.codenvy.im.managers.PropertyNotFoundException;
import com.codenvy.im.managers.StorageManager;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.commons.annotation.Nullable;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Objects;

import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class CodenvyLicenseManager {
    protected static final String CODENVY_LICENSE_KEY = "codenvy-license-key";

    private final StorageManager        storageManager;
    private final CodenvyLicenseFactory licenseFactory;

    @Inject
    public CodenvyLicenseManager(StorageManager storageManager, CodenvyLicenseFactory licenseFactory) {
        this.storageManager = storageManager;
        this.licenseFactory = licenseFactory;
    }

    /**
     * Stores valid Codenvy license into the storage.
     *
     * @throws NullPointerException
     *         if {@code codenvyLicense} is null
     * @throws LicenseException
     *         if error occurred while storing
     */
    public void store(@NotNull CodenvyLicense codenvyLicense) throws LicenseException {
        Objects.requireNonNull(codenvyLicense, "license must not be null");

        try {
            storageManager.storeProperties(ImmutableMap.of(CODENVY_LICENSE_KEY, codenvyLicense.getLicenseText()));
        } catch (IOException e) {
            throw new LicenseException(e.getMessage(), e);
        }
    }

    /**
     * Loads Codenvy license out of underlying storage.
     *
     * @throws LicenseNotFoundException
     *         if license not found
     * @throws InvalidLicenseException
     *         if license not valid
     * @throws LicenseException
     *         if error occurred while loading license
     */
    @Nullable
    public CodenvyLicense load() throws LicenseException {
        String licenseText;
        try {
            licenseText = storageManager.loadProperty(CODENVY_LICENSE_KEY);
        } catch (PropertyNotFoundException e) {
            throw new LicenseNotFoundException("Codenvy license not found");
        } catch (IOException e) {
            throw new LicenseException(e.getMessage(), e);
        }

        if (isNullOrEmpty(licenseText)) {
            throw new LicenseNotFoundException("Codenvy license not found");
        }

        return licenseFactory.create(licenseText);
    }

    /**
     * Deletes Codenvy license from the storage.
     *
     * @throws LicenseException
     *         if error occurred while deleting license
     */
    @Nullable
    public void delete() throws LicenseException {
        try {
            storageManager.deleteProperty(CODENVY_LICENSE_KEY);
        } catch (PropertyNotFoundException e) {
            // ignore
        } catch (IOException e) {
            throw new LicenseException(e.getMessage(), e);
        }
    }
}
