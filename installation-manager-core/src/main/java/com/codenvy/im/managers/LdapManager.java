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

import com.codenvy.api.dao.authentication.SSHAPasswordEncryptor;
import com.codenvy.im.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import static com.codenvy.im.managers.NodeConfig.extractConfigFrom;
import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Alexander Reshetnyak
 */
@Singleton
public class LdapManager {

    private final ConfigManager configManager;
    private final HttpTransport httpTransport;

    @Inject
    public LdapManager(ConfigManager configManager, HttpTransport httpTransport) throws IOException {
        this.configManager = configManager;
        this.httpTransport = httpTransport;
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
            InitialDirContext ldapContext = connect(installType, config);

            try {
                SSHAPasswordEncryptor sshaPasswordEncryptor = new SSHAPasswordEncryptor();
                String encryptedPwd = new String(sshaPasswordEncryptor.encrypt(newPassword), "UTF-8");

                ModificationItem[] mods = new ModificationItem[]{
                    new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", encryptedPwd))
                };

                ldapContext.modifyAttributes(format("cn=%s,ou=users,dc=codenvycorp,dc=com", config.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY)), mods);
            } finally {
                ldapContext.close();
            }
        } catch (Exception e) {
            throw new IOException("Error in changing an admin password", e);
        }
    }

    /**
     * Shell command to get number of users:
     * sudo yum install openldap-clients
     * ldapsearch -D "cn=Admin,dc=codenvy-enterprise,dc=com" -w password -b "ou=People,dc=codenvy-enterprise,dc=com" -s base '(objectclass=*)'
     *
     * Properties of ldap: /home/codenvy/codenvy-data/cloud-ide-local-configuration/ldap.properties
     */
    public long getNumberOfUsers() throws IOException {
        InstallType installType = configManager.detectInstallationType();
        Config config = configManager.loadInstalledCodenvyConfig(installType);

        long resultCounter = 0;

        try {
            InitialDirContext ldapContext = connect(installType, config);
            String ldapSearchBase = "ou=users,dc=codenvycorp,dc=com";
            String ldapSearchFilter = "(objectclass=*)";

            SearchControls ldapControls = new SearchControls();
            ldapControls.setSearchScope(SearchControls.ONELEVEL_SCOPE );
            ldapControls.setReturningAttributes(new String[] { "+", "*" });

            try {
                NamingEnumeration<SearchResult> results = ldapContext.search(ldapSearchBase, ldapSearchFilter, ldapControls);
                while (results.hasMore()) {
                    resultCounter++;
                    results.next();
                }
            } finally {
                ldapContext.close();
            }
        } catch (Exception e) {
            throw new IOException("Error in getting a number of users", e);
        }

        return resultCounter;
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

    private InitialDirContext connect(InstallType installType, Config config) throws NamingException, IOException {
        String server = recogniseLDAPServer(installType, config);

        Hashtable<String, String> ldapEnv = new Hashtable<>(5);
        ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        ldapEnv.put(Context.PROVIDER_URL, format("ldap://%s:389", server));
        ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        ldapEnv.put(Context.SECURITY_PRINCIPAL, "cn=root,dc=codenvycorp,dc=com");
        ldapEnv.put(Context.SECURITY_CREDENTIALS, config.getValue(Config.SYSTEM_LDAP_PASSWORD));
        return new InitialDirContext(ldapEnv);
    }

    private String recogniseLDAPServer(InstallType installType, Config config) throws IOException {
        if (installType == InstallType.SINGLE_SERVER) {
            return "localhost";
        } else {
            NodeConfig nodeConfig = extractConfigFrom(config, NodeConfig.NodeType.DATA);
            requireNonNull(nodeConfig);

            return nodeConfig.getHost();
        }
    }

}
