/*
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
package com.codenvy.im.installer;

import java.util.List;
import java.util.UUID;

/**
 * @author Dmytro Nochevnov
 */
public class InstallOptions {
    private List<String>   commandsInfo;
    private UUID           id;
    private Installer.Type type;

    public List<String> getCommandsInfo() {
        return commandsInfo;
    }

    public UUID getId() {
        return id;
    }

    public Installer.Type getType() {
        return type;
    }

    public InstallOptions setCommandsInfo(List<String> commandsInfo) {
        this.commandsInfo = commandsInfo;
        return this;
    }

    public InstallOptions setId(UUID id) {
        this.id = id;
        return this;
    }

    public InstallOptions setType(Installer.Type type) {
        this.type = type;
        return this;
    }
}
