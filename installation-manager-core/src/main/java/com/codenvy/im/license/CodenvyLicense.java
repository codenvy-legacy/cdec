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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.codenvy.im.license.LicenseFeature.EXPIRATION;
import static com.codenvy.im.license.LicenseFeature.TYPE;
import static com.codenvy.im.license.LicenseFeature.USERS;

/**
 * Represents valid Codenvy license.
 *
 * @author Anatoliy Bazko
 */
public class CodenvyLicense {
    public static final DateFormat EXPIRATION_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");

    private final Map<LicenseFeature, String> features;
    private final String                      licenseText;

    CodenvyLicense(String licenseText, Map<LicenseFeature, String> features) {
        this.features = features;
        this.licenseText = licenseText;
    }

    public String getLicenseText() {
        return licenseText;
    }

    /**
     * @return unmodifiable list of Codenvy license features
     */
    public Map<LicenseFeature, String> getFeatures() {
        return Collections.unmodifiableMap(features);
    }

    /**
     * Indicates if license has been expired or hasn't.
     */
    public boolean isExpired() {
        Date expirationDate = getExpirationDate();
        return !expirationDate.after(Calendar.getInstance().getTime());
    }

    /**
     * @return {@link LicenseFeature#EXPIRATION} feature value
     */
    public Date getExpirationDate() {
        return (Date)doGetFeature(EXPIRATION);
    }

    /**
     * @return {@link LicenseFeature#USERS} feature value
     */
    public int getNumberOfUsers() {
        return (int)doGetFeature(USERS);
    }

    /**
     * @return {@link LicenseFeature#TYPE} feature value
     */
    public LicenseType getLicenseType() {
        return (LicenseType)doGetFeature(TYPE);
    }

    private Object doGetFeature(LicenseFeature feature) {
        return feature.parseValue(features.get(feature));
    }

    /**
     * Codenvy license type.
     */
    public enum LicenseType {
        PRODUCT_KEY,
        EVALUATION_PRODUCT_KEY
    }
}
