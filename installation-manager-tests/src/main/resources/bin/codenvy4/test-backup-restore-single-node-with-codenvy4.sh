#!/bin/bash
#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2015] Codenvy, S.A.
# All Rights Reserved.
# NOTICE: All information contained herein is, and remains
# the property of Codenvy S.A. and its suppliers,
# if any. The intellectual and technical concepts contained
# herein are proprietary to Codenvy S.A.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden unless prior written permission is obtained
# from Codenvy S.A..
#

. ./lib.sh

printAndLog "TEST CASE: Backup and restore single-node Codenvy 4.x On Premise"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

# install Codenvy 4.x
installCodenvy ${LATEST_CODENVY4_VERSION}
validateInstalledCodenvyVersion ${LATEST_CODENVY4_VERSION}
authWithoutRealmAndServerDns "admin" "password"

# backup at start
executeIMCommand "im-backup"
fetchJsonParameter "file"
BACKUP_AT_START=${OUTPUT}

# modify data: add account, workspace, project, user
executeIMCommand "im-password" "password" "new-password"
authWithoutRealmAndServerDns "admin" "new-password"

# create user "cdec.im.test@gmail.com"
doPost "application/json" "{\"name\":\"cdec.im.test@gmail.com\",\"password\":\"pwd123ABC\"}" "http://${HOST_URL}/api/user/create?token=${TOKEN}"
fetchJsonParameter "id"
USER_ID=${OUTPUT}

authWithoutRealmAndServerDns "cdec.im.test@gmail.com" "pwd123ABC"

# create workspace "workspace-1"
doPost "application/json" "{\"environments\":[{\"name\":\"workspace-1\",\"machineConfigs\":[{\"links\":[],\"limits\":{\"ram\":1000},\"name\":\"ws-machine\",\"type\":\"docker\",\"source\":{\"location\":\"http://${HOST_URL}/api/recipe/recipe_ubuntu/script\",\"type\":\"recipe\"},\"dev\":true}]}],\"defaultEnv\":\"workspace-1\",\"projects\":[],\"name\":\"workspace-1\",\"attributes\":{},\"temporary\":false}" "http://${HOST_URL}/api/workspace/config?token=${TOKEN}"
fetchJsonParameter "id"
WORKSPACE_ID=${OUTPUT}

# run workspace "workspace-1"
doPost "application/json" "{}" "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}/runtime?token=${TOKEN}"

# verify is workspace running
doSleep "10m"  "Wait until workspace starts to avoid 'java.lang.NullPointerException' error on verifying workspace state"
doGet "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}/runtime?token=${TOKEN}"
validateExpectedString ".*\"status\":\"RUNNING\".*"

# create project "project-1" in workspace "workspace-1"
doPost "application/json" "{\"links\":[], \"name\":\"project-1\", \"attributes\":{\"maven.version\":[\"1.0-SNAPSHOT\"], \"maven.packaging\":[\"jar\"], \"maven.source.folder\":[\"src/main/java\"], \"maven.test.source.folder\":[\"src/test/java\"], \"maven.artifactId\":[\"project-1\"], \"maven.groupId\":[\"project-1\"]}, \"type\":\"maven\", \"source\":{\"location\":null, \"type\":null, \"parameters\":{}}, \"contentRoot\":null, \"modules\":[], \"path\":null, \"description\":null, \"problems\":[], \"mixins\":[]}" "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}/project?token=${TOKEN}"
doPost "application/json" "{\"links\":[], \"name\":\"project-1\", \"attributes\":{\"maven.version\":[\"1.0-SNAPSHOT\"], \"maven.packaging\":[\"jar\"], \"maven.source.folder\":[\"src/main/java\"], \"maven.test.source.folder\":[\"src/test/java\"], \"maven.artifactId\":[\"project-1\"], \"maven.groupId\":[\"project-1\"]}, \"type\":\"maven\", \"source\":{\"location\":null, \"type\":null, \"parameters\":{}}, \"contentRoot\":null, \"modules\":[], \"path\":null, \"description\":null, \"problems\":[], \"mixins\":[]}" "http://${HOST_URL}/api/ext/project/${WORKSPACE_ID}?name=project-1&token=${TOKEN}"

# create factory from template "minimal"
doPost "application/json" "{\"v\": \"4.0\",\"workspace\": {\"projects\": [{\"links\": [],\"name\": \"Spring\",\"attributes\": {\"languageVersion\": [\"1.6\"],\"language\": [\"java\"]},\"type\": \"maven\", \"source\": {\"location\": \"https://github.com/codenvy-templates/web-spring-java-simple.git\",\"type\": \"git\",\"parameters\": {\"keepVcs\": \"false\", \"branch\": \"3.1.0\"}},\"modules\": [],\"path\": \"/Spring\",\"mixins\": [\"git\"],\"problems\": []}], \"defaultEnv\": \"wss\",\"name\": \"wss\",\"environments\": [{\"machineConfigs\": [{\"dev\": true,\"limits\": {\"ram\":2048},\"source\": {\"location\": \"http://${HOST_URL}/api/recipe/recipe_ubuntu/script\",\"type\": \"recipe\"}, \"name\": \"dev-machine\",\"type\": \"docker\"}],\"name\": \"wss\"}],\"links\": []}}" "http://${HOST_URL}/api/factory?token=${TOKEN}"
fetchJsonParameter "id"
FACTORY_ID=${OUTPUT}

# backup with modifications
executeIMCommand "im-backup"
fetchJsonParameter "file"
BACKUP_WITH_MODIFICATIONS=${OUTPUT}

# restore initial state
executeIMCommand "im-restore" ${BACKUP_AT_START}

# check if data at start was restored correctly
authWithoutRealmAndServerDns "admin" "password"

doGet "http://${HOST_URL}/api/user/${USER_ID}?token=${TOKEN}"
validateExpectedString ".*User.*not.found.*"

doGet "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*Workspace.not.found.*"

# verify that there is no project-1 on file system
executeSshCommand "sudo ls /home/codenvy/codenvy-data/fs"
validateExpectedString ""

doGet "http://${HOST_URL}/api/factory/${FACTORY_ID}?token=${TOKEN}"
validateExpectedString ".*Factory.*not.found.*"

# restore state after modifications
executeIMCommand "im-restore" ${BACKUP_WITH_MODIFICATIONS}

# check if modified data was restored correctly
authWithoutRealmAndServerDns "admin" "new-password"

doGet "http://${HOST_URL}/api/user/${USER_ID}?token=${TOKEN}"
validateExpectedString ".*cdec.im.test@gmail.com.*"

authWithoutRealmAndServerDns "cdec.im.test@gmail.com" "pwd123ABC"

doGet "http://${HOST_URL}/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*project-1.*workspace-1.*"

# verify that there is project-1 on file system
executeSshCommand "sudo ls -R /home/codenvy/codenvy-data/fs"
validateExpectedString ".*/home/codenvy/codenvy-data/fs/[0-9a-z/]*/${WORKSPACE_ID}.*project-1\:\spom.xml.src.*"

doGet "http://${HOST_URL}/api/factory/${FACTORY_ID}?token=${TOKEN}"
validateExpectedString ".*\"name\"\:\"wss\".*"

printAndLog "RESULT: PASSED"
vagrantDestroy