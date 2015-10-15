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

printAndLog "TEST CASE: Migration Data"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

# install Codenvy
installCodenvy
validateInstalledCodenvyVersion
auth "admin" "password"

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

# backup
executeIMCommand "im-backup"
fetchJsonParameter "file"
BACKUP=${OUTPUT}

executeSshCommand "cp ${BACKUP} /vagrant/backup.tar.gz"
vagrantDestroy

# restore
vagrantUp ${MULTI_NODE_VAGRANT_FILE}
installCodenvy
validateInstalledCodenvyVersion
executeSshCommand "mkdir /home/vagrant/codenvy-im-data/backups"
executeSshCommand "cp /vagrant/backup.tar.gz ${BACKUP}"
executeIMCommand "im-restore" ${BACKUP}

# check data
auth "admin" "new-password"

doGet "http://codenvy/api/account/${ACCOUNT_ID}?token=${TOKEN}"
fetchJsonParameter "id"

doGet "http://codenvy/api/project/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*project-1.*"

doGet "http://codenvy/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
fetchJsonParameter "id"

doGet "http://codenvy/api/user/${USER_ID}?token=${TOKEN}"
fetchJsonParameter "id"

doGet "http://codenvy/api/factory/${FACTORY_ID}?token=${TOKEN}"
fetchJsonParameter "id"

authOnSite "user-1" "pwd123ABC"

printAndLog "RESULT: PASSED"
vagrantDestroy
