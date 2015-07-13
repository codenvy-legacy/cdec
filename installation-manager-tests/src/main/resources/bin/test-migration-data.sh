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

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

printAndLog "TEST CASE: Migration Data"

# install Codenvy
installCodenvy
validateInstalledCodenvyVersion
auth "admin" "password"

# modify data: add accout, workspace, project, user, factory
executeIMCommand "im-password" "password" "new-password"
auth "admin" "new-password"

doPost "application/json" "{\"name\":\"account-1\"}" "http://codenvy.onprem/api/account?token=${TOKEN}"
ACCOUNT_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"name\":\"workspace-1\",\"accountId\":\"${ACCOUNT_ID}\"}" "http://codenvy.onprem/api/workspace?token=${TOKEN}"
WORKSPACE_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"type\":\"blank\",\"visibility\":\"public\"}" "http://codenvy.onprem/api/project/${WORKSPACE_ID}?name=project-1&token=${TOKEN}"

doPost "application/json" "{\"name\":\"user-1\",\"password\":\"pwd123ABC\"}" "http://codenvy.onprem/api/user/create?token=${TOKEN}"
USER_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"userId\":\"${USER_ID}\",\"roles\":[\"account/owner\"]}" "http://codenvy.onprem/api/account/${ACCOUNT_ID}/members?token=${TOKEN}"
ACCOUNT_ID=$(fetchJsonParameter "id")

authOnSite "user-1" "pwd123ABC"

createDefaultFactory ${TOKEN}
FACTORY_ID=$(fetchJsonParameter "id")

# backup
executeIMCommand "im-backup"
BACKUP=$(fetchJsonParameter "file")

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
auth "admin" "password"

doGet "http://codenvy.onprem/api/account/${ACCOUNT_ID}?token=${TOKEN}"
fetchJsonParameter "id"

doGet "http://codenvy.onprem/api/project/${WORKSPACE_ID}?token=${TOKEN}"
[[ ! ${OUTPUT} =~ .*project-1.* ]] && validateExitCode 1

doGet "http://codenvy.onprem/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
fetchJsonParameter "id"

doGet "http://codenvy.onprem/api/user/${USER_ID}?token=${TOKEN}"
fetchJsonParameter "id"

doGet "http://codenvy.onprem/api/factory/${FACTORY_ID}?token=${TOKEN}"
fetchJsonParameter "id"

printAndLog "RESULT: PASSED"
vagrantDestroy
