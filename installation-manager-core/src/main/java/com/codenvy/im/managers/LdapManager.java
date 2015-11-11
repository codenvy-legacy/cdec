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
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import static com.codenvy.im.managers.NodeConfig.extractConfigFrom;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Alexander Reshetnyak
 */
@Singleton
public class LdapManager {

    @Inject
    public LdapManager() {
    }

    public void updatePwd(byte[] newPassword, InitialDirContext ldapContext, Config config) throws NamingException, UnsupportedEncodingException {
        SSHAPasswordEncryptor sshaPasswordEncryptor = new SSHAPasswordEncryptor();
        String encryptedPwd = new String(sshaPasswordEncryptor.encrypt(newPassword), "UTF-8");

        ModificationItem[] mods = new ModificationItem[]{
                new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", encryptedPwd))
        };
        ldapContext.modifyAttributes(format("cn=%s,ou=users,dc=codenvycorp,dc=com", config.getValue("admin_ldap_user_name")), mods);
    }

    public InitialDirContext connect(InstallType installType, Config config) throws NamingException, IOException {
        String server = recogniseLDAPServer(installType, config);

        Hashtable<String, String> ldapEnv = new Hashtable<>(5);
        ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        ldapEnv.put(Context.PROVIDER_URL, format("ldap://%s:389", server));
        ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        ldapEnv.put(Context.SECURITY_PRINCIPAL, "cn=root,dc=codenvycorp,dc=com");
        ldapEnv.put(Context.SECURITY_CREDENTIALS, config.getValue("system_ldap_password"));
        return new InitialDirContext(ldapEnv);
    }

    public String recogniseLDAPServer(InstallType installType, Config config) throws IOException {
        if (installType == InstallType.SINGLE_SERVER) {
            return "localhost";
        } else {
            NodeConfig nodeConfig = extractConfigFrom(config, NodeConfig.NodeType.DATA);
            requireNonNull(nodeConfig);

            return nodeConfig.getHost();
        }
    }

    /*public void getTottalUsers(InitialDirContext ldapContext, Config config) throws NamingException, UnsupportedEncodingException {

        ldapContext.search(format("cn=%s,ou=users,dc=codenvycorp,dc=com", config.getValue("admin_ldap_user_name")), mods);
    }*/
}
