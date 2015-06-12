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

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;

/** @author Dmytro Nochevnov */
@Singleton
public class ConfigManager {
    private static final Pattern VARIABLE_TEMPLATE = Pattern.compile("\\$\\{([^\\}]*)\\}"); // ${...}

    private final HttpTransport transport;
    private final String        updateEndpoint;
    private final String        puppetBaseDir;
    private final Path          puppetConfFile;

    @Inject
    public ConfigManager(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                         @Named("puppet.base_dir") String puppetBaseDir,
                         HttpTransport transport) {
        this.transport = transport;
        this.updateEndpoint = updateEndpoint;
        this.puppetBaseDir = puppetBaseDir;
        this.puppetConfFile = Paths.get(puppetBaseDir, Config.PUPPET_CONF_FILE_NAME).toAbsolutePath();
    }

    /** Loads properties from the given file. */
    public Map<String, String> loadConfigProperties(Path confFile) throws IOException {
        if (!exists(confFile)) {
            throw new FileNotFoundException(format("Configuration file '%s' not found", confFile.toString()));
        }

        try (InputStream in = newInputStream(confFile)) {
            return doLoad(in);
        } catch (IOException e) {
            throw new ConfigException(format("Can't load properties: %s", e.getMessage()), e);
        }
    }

    /** Loads properties from the given file. */
    public Map<String, String> loadConfigProperties(String confFile) throws IOException {
        return loadConfigProperties(Paths.get(confFile));
    }

    /** Loads default properties. */
    public Map<String, String> loadCodenvyDefaultProperties(Version version, InstallType installType) throws IOException {
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));

        String requestUrl = combinePaths(updateEndpoint, "/repository/public/download/codenvy-" +
                                                         (installType == InstallType.MULTI_SERVER ? "multi" : "single")
                                                         + "-server-properties/" + version);
        Path properties;
        try {
            properties = transport.download(requestUrl, tmpDir);
        } catch (IOException e) {
            throw new IOException("Can't download installation properties. " + e.getMessage(), e);
        }

        try (InputStream in = newInputStream(properties)) {
            return doLoad(in);
        } catch (IOException e) {
            throw new ConfigException(format("Can't load properties: %s", e.getMessage()), e);
        }
    }

    /**
     * Merges two bunches of the properties from current and new configurations.
     * As a rule method keeps the values of the old configuration except the {@link Config#VERSION} property
     * and cases where new default values came.
     */
    public Map<String, String> merge(Version curVersion,
                                     Map<String, String> curProps,
                                     Map<String, String> newProps) throws IOException {

        InstallType installType = detectInstallationType();
        Map<String, String> curDefaultProps = loadCodenvyDefaultProperties(curVersion, installType);

        Map<String, String> props = new HashMap<>(curProps);

        for (Map.Entry<String, String> e : newProps.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();

            if (!props.containsKey(name)) {
                props.put(name, value);
            } else {
                if (curDefaultProps.containsKey(name) && curProps.get(name).equals(curDefaultProps.get(name))) {
                    props.put(name, value);
                }
            }

            String aioOldName = "aio_" + name;
            if (props.containsKey(aioOldName)) {
                props.put(name, props.get(aioOldName));
                props.remove(aioOldName);
            }
        }

        props.remove(Config.VERSION);
        if (newProps.containsKey(Config.VERSION)) {
            props.put(Config.VERSION, newProps.get(Config.VERSION));
        }

        return props;
    }

    /**
     * Loads properties of the installed cdec artifact.
     * <p/>
     * The properties file has the follow format:
     * $property="value"
     * ...
     * <p/>
     * Finally method removes leading '$' for key name and quota characters for its value.
     */
    public Map<String, String> loadInstalledCodenvyProperties(InstallType installType) throws IOException {
        Map<String, String> properties = new HashMap<>();

        Iterator<Path> files = getCodenvyPropertiesFiles(installType);
        while (files.hasNext()) {
            Path propertiesFile = files.next();

            try (InputStream in = newInputStream(propertiesFile)) {
                Map<String, String> m = doLoad(in);

                for (Map.Entry<String, String> e : m.entrySet()) {
                    String key = e.getKey().trim();
                    if (key.startsWith("$")) {
                        key = key.substring(1); // removes '$'
                        String value = e.getValue().substring(1, e.getValue().length() - 1); // removes "

                        properties.put(key, value);
                    }
                }

            } catch (IOException e) {
                throw new ConfigException(format("Can't load Codenvy properties: %s", e.getMessage()), e);
            }
        }

        return properties;
    }

    protected Iterator<Path> getCodenvyPropertiesFiles(InstallType installType) {
        switch (installType) {
            case MULTI_SERVER:
                return ImmutableList.of(Paths.get(puppetBaseDir + File.separator + Config.MULTI_SERVER_PROPERTIES),
                                        Paths.get(puppetBaseDir + File.separator + Config.MULTI_SERVER_BASE_PROPERTIES)).iterator();

            case SINGLE_SERVER:
            default:
                return ImmutableList.of(Paths.get(puppetBaseDir + File.separator + Config.SINGLE_SERVER_PROPERTIES),
                                        Paths.get(puppetBaseDir + File.separator + Config.SINGLE_SERVER_BASE_PROPERTIES)).iterator();
        }
    }

    protected Map<String, String> doLoad(InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(in);

        Map<String, String> m = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString().toLowerCase();
            String value = entry.getValue().toString();

            m.put(key, value);
        }

        return m;
    }

    /** @return list of replacements for multi-node master puppet config file Config.MULTI_SERVER_NODES_PROPERTIES based on the node configs. */
    public static Map<String, String> getPuppetNodesConfigReplacement(List<NodeConfig> nodeConfigs) {
        Map<String, String> replacements = new HashMap<>(nodeConfigs.size());

        for (NodeConfig node : nodeConfigs) {
            NodeConfig.NodeType type = node.getType();
            switch (type) {
                case DATA:
                case API:
                case SITE:
                case DATASOURCE:
                case ANALYTICS: {
                    String replacingToken = format("%s.example.com", type.toString().toLowerCase());
                    String replacement = node.getHost();
                    replacements.put(replacingToken, replacement);
                    break;
                }

                case BUILDER: {
                    String replacingToken = "builder.*example.com";
                    String replacement = format("builder\\\\d+\\\\%s", getBaseNodeDomain(node));
                    replacements.put(replacingToken, replacement);
                    break;
                }

                case RUNNER: {
                    String replacingToken = "runner.*example.com";
                    String replacement = format("runner\\\\d+\\\\%s", getBaseNodeDomain(node));
                    replacements.put(replacingToken, replacement);
                    break;
                }

                case PUPPET_MASTER: {
                    String replacingToken = "puppet-master.example.com";
                    String replacement = node.getHost();
                    replacements.put(replacingToken, replacement);
                    break;
                }

                default:
                    break;
            }
        }

        return replacements;
    }

    public static String getBaseNodeDomain(NodeConfig node) {
        String regex = format("^%s\\d+", node.getType().toString().toLowerCase());
        return node.getHost().toLowerCase().replaceAll(regex, "");
    }

    /**
     * Loads appropriate Codenvy config for given installation type.
     */
    public Config loadInstalledCodenvyConfig(InstallType installType) throws IOException {
        Map<String, String> properties = loadInstalledCodenvyProperties(installType);
        return new Config(properties);
    }


    /**
     * Loads appropriate Codenvy config depending on installation type.
     */
    public Config loadInstalledCodenvyConfig() throws UnknownInstallationTypeException, IOException {
        return loadInstalledCodenvyConfig(detectInstallationType());
    }

    /**
     * Detects which Codenvy installation type is used. The main idea is in analyzing puppet.conf to figure out in which sections
     * 'certname' property exists.  If method can't detect installation type then an exception will be thrown. The result totally
     * depends on implementations of {@link com.codenvy.im.artifacts.helper.CDECSingleServerHelper} and
     * {@link com.codenvy.im.artifacts.helper.CDECMultiServerHelper}.
     * <p/>
     * SINGLE-node type configuration sample:
     * [master]
     * certname = host_name
     * ...
     * [main]
     * ...
     * [agent]
     * certname = host_name
     * <p/>
     * <p/>
     * MULTI-node type configuration sample:
     * [main]
     * server = host_name
     * ...
     * [agent]
     * certname = host_name
     * ...
     *
     * @throws UnknownInstallationTypeException
     *         if can't detect installation type
     */
    @Nonnull
    public InstallType detectInstallationType() throws UnknownInstallationTypeException {
        try {
            HierarchicalINIConfiguration iniFile = new HierarchicalINIConfiguration();
            iniFile.load(puppetConfFile.toFile());

            if (isSingleTypeConfig(iniFile)) {
                return InstallType.SINGLE_SERVER;
            } else if (isMultiTypeConfig(iniFile)) {
                return InstallType.MULTI_SERVER;
            }
            throw new UnknownInstallationTypeException();
        } catch (ConfigurationException e) {
            throw new UnknownInstallationTypeException(e);
        }
    }

    private boolean isSingleTypeConfig(HierarchicalINIConfiguration iniFile) {
        Set<String> sections = iniFile.getSections();
        return sections.contains("agent")
               && !iniFile.getSection("agent").getString("certname", "").isEmpty()
               && sections.contains("master")
               && !iniFile.getSection("master").getString("certname", "").isEmpty();
    }

    private boolean isMultiTypeConfig(HierarchicalINIConfiguration iniFile) {
        Set<String> sections = iniFile.getSections();
        return sections.contains("main")
               && !iniFile.getSection("main").getString("server", "").isEmpty()
               && sections.contains("agent")
               && !iniFile.getSection("agent").getString("certname", "").isEmpty();
    }

    /**
     * Reads puppet master host name from the puppet configuration file.
     * It is supposed that we have deal with multi-server configuration type.
     * <p/>
     * MULTI-node type configuration sample:
     * [master]
     * ...
     * [main]
     * certname = some_host_name
     * ...
     *
     * @throws java.io.IOException
     *         if any I/O errors occur
     * @throws java.lang.IllegalStateException
     *         if host name is not set
     */
    @Nonnull
    public String fetchMasterHostName() throws IOException {
        try {
            HierarchicalINIConfiguration iniFile = new HierarchicalINIConfiguration();
            iniFile.load(puppetConfFile.toFile());

            SubnodeConfiguration section = iniFile.getSection("main");
            if (section.getString("certname", "").isEmpty()) {
                // try to obtain host name from codenvy config
                Map<String, String> codenvyProperties = loadInstalledCodenvyConfig().getProperties();
                if (codenvyProperties.containsKey(Config.PUPPET_MASTER_HOST_NAME_PROPERTY)) {
                    return codenvyProperties.get(Config.PUPPET_MASTER_HOST_NAME_PROPERTY);
                }

                throw new IllegalStateException("There is no puppet master host name in the configuration");
            }
            return section.getString("certname");
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }
    }

    /**
     * Prepares installation properties depending on artifact and installation type.
     *
     * @param configFile
     *         file to read properties from, if absent the default properties will be loaded from the update server
     * @param installType
     *         installation type
     * @param artifact
     *         artifact to load properties for
     * @param version2Install
     *         version of the artifact
     * @return installation properties
     * @throws IOException
     *         if any I/O error occurred
     */
    public Map<String, String> prepareInstallProperties(@Nullable String configFile,
                                                        InstallType installType,
                                                        Artifact artifact,
                                                        Version version2Install,
                                                        boolean isInstall) throws IOException {
        switch (artifact.getName()) {
            case InstallManagerArtifact.NAME:
                return Collections.emptyMap();

            case CDECArtifact.NAME:
                Map<String, String> properties;

                if (isInstall) {
                    properties = configFile != null ? loadConfigProperties(configFile)
                                                    : loadCodenvyDefaultProperties(version2Install, installType);

                    if (installType == InstallType.MULTI_SERVER) {
                        setSSHAccessProperties(properties);
                    }
                } else { // update
                    properties = merge(artifact.getInstalledVersion(),
                                       loadInstalledCodenvyProperties(installType),
                                       configFile != null ? loadConfigProperties(configFile)
                                                          : loadCodenvyDefaultProperties(version2Install, installType));

                    if (installType == InstallType.MULTI_SERVER) {
                        properties.put(Config.PUPPET_MASTER_HOST_NAME_PROPERTY, fetchMasterHostName());  // set puppet master host name
                    }
                }

                properties.put(Config.VERSION, version2Install.toString());
                setTemplatesProperties(properties);

                return properties;
            default:
                throw new ArtifactNotFoundException(artifact);
        }
    }

    public Path getPuppetConfigFile(String configFilename) {
        return Paths.get(puppetBaseDir).resolve(configFilename);
    }

    /**
     * It's allowed to use ${} templates to set properties values.
     */
    private void setTemplatesProperties(Map<String, String> properties) {
        for (Map.Entry<String, String> e : properties.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();

            Matcher matcher = VARIABLE_TEMPLATE.matcher(value);
            if (matcher.find()) {
                String newValue = properties.get(matcher.group(1));
                properties.put(key, newValue);
            }
        }
    }

    /**
     * Sets properties needed for SSH access to other nodes.
     */
    protected void setSSHAccessProperties(Map<String, String> properties) throws IOException {
        String userName = System.getProperty("user.name");
        Path pathToIdRsa = Paths.get(System.getProperty("user.home")).resolve(".ssh").resolve("id_rsa");
        String sshKey = readSSHKey(pathToIdRsa);

        properties.put(Config.NODE_SSH_USER_NAME_PROPERTY, userName);  // set name of user to access the nodes
        properties.put(Config.NODE_SSH_USER_PRIVATE_KEY_PROPERTY, sshKey);  // set private key of ssh user
    }

    protected String readSSHKey(Path pathToIdRsa) throws IOException {
        if (!exists(pathToIdRsa)) {
            throw new RuntimeException("SSH private key not found: " + pathToIdRsa.toString());
        }
        return Files.toString(pathToIdRsa.toFile(), Charsets.UTF_8);
    }
}
