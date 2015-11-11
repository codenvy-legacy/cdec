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

import com.codenvy.im.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;

import javax.naming.directory.InitialDirContext;
import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class PasswordManager {

    private final ConfigManager configManager;
    private final HttpTransport httpTransport;
    private final LdapManager   ldapManager;

    @Inject
    public PasswordManager(ConfigManager configManager,
                           HttpTransport httpTransport,
                           LdapManager ldapManager) throws IOException {
        this.configManager = configManager;
        this.httpTransport = httpTransport;
        this.ldapManager = ldapManager;
    }

    /**
     * Modify Codenvy admin password.
     *
     * @throws IOException
     *         if any error occurred
     * @throws java.lang.IllegalStateException
     *         if current password is invalid
     */
    public void changeAdminPassword(byte[] currentPassword, byte[] newPassword) throws IOException, IllegalStateException {
        InstallType installType = configManager.detectInstallationType();
        Config config = configManager.loadInstalledCodenvyConfig(installType);

        validateCurrentPassword(currentPassword, config);

        try {
            InitialDirContext ldapContext = ldapManager.connect(installType, config);
            try {
                ldapManager.updatePwd(newPassword, ldapContext, config);
            } finally {
                ldapContext.close();
            }
        } catch (Exception e) {
            throw new IOException("Password can't be modified", e);
        }
    }

    protected void validateCurrentPassword(byte[] currentPassword, Config config) throws IOException {
        Credentials credentials = new DtoServerImpls.CredentialsImpl();
        credentials.setPassword(new String(currentPassword, "UTF-8"));
        credentials.setUsername(config.getValue("admin_ldap_user_name"));
        credentials.setRealm("sysldap");

        String requestUrl = combinePaths(configManager.getApiEndpoint(), "/auth/login");
        try {
            httpTransport.doPost(requestUrl, credentials);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid current password");
        }
    }


}
