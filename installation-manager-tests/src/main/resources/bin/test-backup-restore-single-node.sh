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

printAndLog "TEST CASE: Backup and restore single-node Codenvy On Premise"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

# install Codenvy
installCodenvy ${PREV_CODENVY_VERSION}
validateInstalledCodenvyVersion ${PREV_CODENVY_VERSION}
auth "admin" "password"

# backup at start
executeIMCommand "im-backup"
fetchJsonParameter "file"
BACKUP_AT_START=${OUTPUT}

# modify data: add accout, workspace, project, user, factory
executeIMCommand "im-password" "password" "new-password"
auth "admin" "new-password"

doPost "application/json" "{\"name\":\"account-1\"}" "http://codenvy/api/account?token=${TOKEN}"
fetchJsonParameter "id"
ACCOUNT_ID=${OUTPUT}

doPost "application/json" "{\"name\":\"workspace-1\",\"accountId\":\"${ACCOUNT_ID}\"}" "http://codenvy/api/workspace?token=${TOKEN}"
fetchJsonParameter "id"
WORKSPACE_ID=${OUTPUT}

doPost "application/json" "{\"type\":\"blank\",\"visibility\":\"public\"}" "http://codenvy/api/project/${WORKSPACE_ID}?name=project-1&token=${TOKEN}"

doPost "application/json" "{\"name\":\"user-1\",\"password\":\"pwd123ABC\"}" "http://codenvy/api/user/create?token=${TOKEN}"
fetchJsonParameter "id"
USER_ID=${OUTPUT}

doPost "application/json" "{\"userId\":\"${USER_ID}\",\"roles\":[\"account/owner\"]}" "http://codenvy/api/account/${ACCOUNT_ID}/members?token=${TOKEN}"
fetchJsonParameter "id"
ACCOUNT_ID=${OUTPUT}

authOnSite "user-1" "pwd123ABC"

createDefaultFactory ${TOKEN}
fetchJsonParameter "id"
FACTORY_ID=${OUTPUT}

# backup with modifications
executeIMCommand "im-backup"
fetchJsonParameter "file"
BACKUP_WITH_MODIFICATIONS=${OUTPUT}

# restore initial state
executeIMCommand "im-restore" ${BACKUP_AT_START}

# check if data at start was restored correctly
auth "admin" "password"

doGet "http://codenvy/api/account/${ACCOUNT_ID}?token=${TOKEN}"
validateExpectedString ".*Account.with.id.${ACCOUNT_ID}.was.not.found.*"

doGet "http://codenvy/api/project/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*Workspace.*not.found.*"

doGet "http://codenvy/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*Workspace.*not.found.*"

doGet "http://codenvy/api/user/${USER_ID}?token=${TOKEN}"
validateExpectedString ".*User.*not.found.*"

doGet "http://codenvy/api/factory/${FACTORY_ID}?token=${TOKEN}"
validateExpectedString ".*Factory.*not.found.*"

# restore state after modifications
executeIMCommand "im-restore" ${BACKUP_WITH_MODIFICATIONS}

# check if modified data was restored correctly
auth "admin" "new-password"

doGet "http://codenvy/api/account/${ACCOUNT_ID}?token=${TOKEN}"
validateExpectedString ".*account-1.*"

doGet "http://codenvy/api/project/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*project-1.*"

doGet "http://codenvy/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*workspace-1.*"

doGet "http://codenvy/api/user/${USER_ID}?token=${TOKEN}"
validateExpectedString ".*user-1.*"

doGet "http://codenvy/api/factory/${FACTORY_ID}?token=${TOKEN}"
validateExpectedString ".*\"name\"\:\"my-minimalistic-factory\".*"

authOnSite "user-1" "pwd123ABC"

# update
executeIMCommand "im-download" "codenvy" "${LATEST_CODENVY_VERSION}"
executeIMCommand "im-install" "codenvy" "${LATEST_CODENVY_VERSION}"
validateInstalledCodenvyVersion ${LATEST_CODENVY_VERSION}

# restore state at start
executeIMCommand "--valid-exit-code=1" "im-restore" ${BACKUP_AT_START}
validateExpectedString ".*\"Version.of.backed.up.artifact.'${PREV_CODENVY_VERSION}'.doesn't.equal.to.restoring.version.'${LATEST_CODENVY_VERSION}'\".*\"status\".\:.\"ERROR\".*"

printAndLog "RESULT: PASSED"
vagrantDestroy
