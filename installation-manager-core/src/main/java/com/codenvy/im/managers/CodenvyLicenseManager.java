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

package com.codenvy.im.managers;

import com.codenvy.im.exceptions.IllegalFormatLicenseException;
import com.codenvy.im.exceptions.InvalidLicenseException;
import com.codenvy.im.exceptions.LicenseException;
import com.codenvy.im.exceptions.LicenseNotFoundException;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.license4j.License;
import com.license4j.LicenseValidator;
import com.license4j.ValidationStatus;

import org.eclipse.che.commons.annotation.Nullable;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class CodenvyLicenseManager {
    protected static final String CODENVY_LICENSE_KEY    = "codenvy-license-key";
    protected static final String EXPIRATION_DATE_FORMAT = "yyyy/MM/dd";

    private final StorageManager storageManager;
    private final String         productId;
    private final String         publicKey;
    private final String         internalHiddenString;

    @Inject
    public CodenvyLicenseManager(@Named("license-manager.product_id") String productId,
                                 @Named("license-manager.public_key") String publicKey,
                                 @Named("license-manager.internal_hidden_string") String internalHiddenString,
                                 StorageManager storageManager) {
        this.storageManager = storageManager;
        this.productId = productId;
        this.publicKey = publicKey;
        this.internalHiddenString = internalHiddenString;
    }

    /**
     * Stores Codenvy license into the underlying storage.
     *
     * @throws NullPointerException
     *         if {@code license} is null
     * @throws InvalidLicenseException
     *         if license not valid
     * @throws IllegalFormatLicenseException
     *         if one of the {@link LicenseFeature} has illegal format in Codenvy license
     * @throws LicenseException
     *         if error occurred while storing
     */
    public void store(@NotNull String licenseText) throws LicenseException {
        Objects.requireNonNull(licenseText, "license must not be null");

        License license = LicenseValidator.validate(licenseText, publicKey, productId, null, null, null, null);
        ValidationStatus licenseValidationStatus = license.getValidationStatus();
        if (licenseValidationStatus != ValidationStatus.LICENSE_VALID) {
            throw new InvalidLicenseException("Codenvy license is not valid");
        }

        HashMap<String, String> customFeatures = license.getLicenseText().getCustomSignedFeatures();
        for (LicenseFeature feature : LicenseFeature.values()) {
            doGetFeature(customFeatures, feature);
        }

        try {
            storageManager.storeProperties(ImmutableMap.of(CODENVY_LICENSE_KEY, licenseText));
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
    public String load() throws LicenseNotFoundException, LicenseException {
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

        return licenseText;
    }

    /**
     * Deletes Codenvy license from the system.
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

    /**
     * Checks if Codenvy license is valid and has an appropriate format.
     * It means all necessary properties are set.
     *
     * @throws LicenseNotFoundException
     *         if license not found
     * @throws IllegalFormatLicenseException
     *         if one of the {@link LicenseFeature} has illegal format in Codenvy license
     * @throws InvalidLicenseException
     *         if license not valid
     * @throws LicenseException
     *         if other errors occurred
     */
    public void validate() throws LicenseNotFoundException,
                                  IllegalFormatLicenseException,
                                  InvalidLicenseException,
                                  LicenseException {
        doLoadAndValidate();
    }

    /**
     * Indicates if license has been expired or hasn't.
     *
     * @throws LicenseNotFoundException
     *         if license not found
     * @throws InvalidLicenseException
     *         if license not valid
     * @throws IllegalFormatLicenseException
     *         if {@link LicenseFeature#EXPIRATION_DATE} has illegal format in Codenvy license text
     * @throws LicenseException
     *         if other errors occurred
     */
    public boolean isLicenseExpired() {
        Date expirationDate = getExpirationDate();
        return !expirationDate.after(Calendar.getInstance().getTime());
    }

    protected License doLoadAndValidate() throws LicenseNotFoundException,
                                                 IllegalFormatLicenseException,
                                                 InvalidLicenseException,
                                                 LicenseException {
        String licenseText = load();
        return doValidate(licenseText);
    }

    protected License doValidate(String licenseText) throws IllegalFormatLicenseException, InvalidLicenseException {
        License license = LicenseValidator.validate(licenseText, publicKey, productId, null, null, null, null);
        ValidationStatus licenseValidationStatus = license.getValidationStatus();
        if (licenseValidationStatus != ValidationStatus.LICENSE_VALID) {
            throw new InvalidLicenseException("Codenvy license is not valid");
        }

        HashMap<String, String> customFeatures = license.getLicenseText().getCustomSignedFeatures();
        for (LicenseFeature feature : LicenseFeature.values()) {
            doGetFeature(customFeatures, feature);
        }

        return license;
    }

    /**
     * @return {@link LicenseFeature#EXPIRATION_DATE} feature value
     *
     * @throws LicenseNotFoundException
     *         if license not found
     * @throws InvalidLicenseException
     *         if license not valid
     * @throws IllegalFormatLicenseException
     *         if {@link LicenseFeature#EXPIRATION_DATE} has illegal format in Codenvy license
     * @throws LicenseException
     *         if other errors occurred
     */
    public Date getExpirationDate() throws LicenseNotFoundException, InvalidLicenseException, LicenseException {
        License license = doLoadAndValidate();

        Map<String, String> customFeatures = license.getLicenseText().getCustomSignedFeatures();
        String expirationDate = doGetFeature(customFeatures, LicenseFeature.EXPIRATION_DATE);

        DateFormat df = new SimpleDateFormat(EXPIRATION_DATE_FORMAT);
        try {
            return df.parse(expirationDate);
        } catch (ParseException e) {
            throw new IllegalFormatLicenseException(
                    format("Unrecognizable Codenvy license. Invalid expiration date format: '%s'", expirationDate));
        }
    }

    /**
     * @return {@link LicenseFeature#NUMBER_OF_USERS} feature value
     * @throws LicenseNotFoundException
     *         if license not found
     * @throws InvalidLicenseException
     *         if license not valid
     * @throws IllegalFormatLicenseException
     *         if {@link LicenseFeature#NUMBER_OF_USERS} has illegal format in Codenvy license
     * @throws LicenseException
     *         if other errors occurred
     */
    public int getNumberOfUsers() throws LicenseNotFoundException, InvalidLicenseException {
        License license = doLoadAndValidate();

        Map<String, String> customFeatures = license.getLicenseText().getCustomSignedFeatures();
        String numberOfUsers = doGetFeature(customFeatures, LicenseFeature.NUMBER_OF_USERS);

        try {
            return Integer.parseInt(numberOfUsers);
        } catch (NumberFormatException e) {
            throw new IllegalFormatLicenseException(
                    format("Unrecognizable Codenvy license. Invalid number of users format: '%s'", numberOfUsers));
        }
    }

    /**
     * @return {@link LicenseFeature#LICENSE_TYPE} feature value
     * @throws LicenseNotFoundException
     *         if license not found
     * @throws InvalidLicenseException
     *         if license not valid
     * @throws IllegalFormatLicenseException
     *         if {@link LicenseFeature#LICENSE_TYPE} has illegal format in Codenvy license
     * @throws LicenseException
     *         if other errors occurred
     */
    public LicenseType getLicenseType() throws LicenseNotFoundException,
                                               InvalidLicenseException,
                                               IllegalFormatLicenseException,
                                               LicenseException {
        License license = doLoadAndValidate();

        Map<String, String> customFeatures = license.getLicenseText().getCustomSignedFeatures();
        String licenseType = doGetFeature(customFeatures, LicenseFeature.LICENSE_TYPE);

        try {
            return LicenseType.valueOf(licenseType.replace(" ", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalFormatLicenseException(format("Unrecognizable Codenvy license. Unknown license type: '%s'", licenseType));
        }
    }


    /**
     * @return Codenvy License type {@link LicenseType}
     * @throws LicenseNotFoundException
     *         if license not found
     * @throws InvalidLicenseException
     *         if license not valid
     * @throws LicenseException
     *         if other errors occurred
     */
    public Map<LicenseFeature, String> getCustomFeatures() throws LicenseNotFoundException,
                                                                  IllegalFormatLicenseException,
                                                                  InvalidLicenseException,
                                                                  LicenseException {
        License license = doLoadAndValidate();
        Map<String, String> features = license.getLicenseText().getCustomSignedFeatures();

        return features.entrySet()
                       .stream()
                       .map(entry -> new AbstractMap.SimpleEntry<>(LicenseFeature.fromString(entry.getKey()), entry.getValue()))
                       .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String doGetFeature(Map<String, String> customFeatures, LicenseFeature licenseFeature) throws IllegalFormatLicenseException {
        String featureName = licenseFeature.toString();
        String feature = customFeatures.get(featureName);
        if (isNullOrEmpty(feature)) {
            throw new IllegalFormatLicenseException(format("Codenvy license does not contain '%s' feature", featureName));
        }

        return feature;
    }

    /**
     * Codenvy license type.
     */
    public enum LicenseType {
        PRODUCT_KEY,
        EVALUATION_PRODUCT_KEY
    }

    /**
     * Codenvy license custom features.
     */
    public enum LicenseFeature {
        LICENSE_TYPE,
        EXPIRATION_DATE,
        NUMBER_OF_USERS;

        @Override
        public String toString() {
            return super.toString().toLowerCase().replace("_", "-");
        }

        public static LicenseFeature fromString(String value) {
            return LicenseFeature.valueOf(value.toUpperCase().replace("-", "_"));
        }
    }
}
