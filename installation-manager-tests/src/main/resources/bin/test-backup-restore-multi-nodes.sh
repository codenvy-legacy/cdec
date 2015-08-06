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

printAndLog "TEST CASE: Backup and restore multi-nodes Codenvy On Premise"
vagrantUp ${MULTI_NODE_VAGRANT_FILE}

# install Codenvy
installCodenvy ${PREV_CODENVY_VERSION}
validateInstalledCodenvyVersion ${PREV_CODENVY_VERSION}
auth "admin" "password"

# backup
executeIMCommand "im-backup"
fetchJsonParameter "file"
BACKUP=${OUTPUT}

# modify data: add accout, workspace, project, user, factory
executeIMCommand "im-password" "password" "new-password"
auth "admin" "new-password"

doPost "application/json" "{\"name\":\"account-1\"}" "http://codenvy.onprem/api/account?token=${TOKEN}"
fetchJsonParameter "id"
ACCOUNT_ID=${OUTPUT}

doPost "application/json" "{\"name\":\"workspace-1\",\"accountId\":\"${ACCOUNT_ID}\"}" "http://codenvy.onprem/api/workspace?token=${TOKEN}"
fetchJsonParameter "id"
WORKSPACE_ID=${OUTPUT}

doPost "application/json" "{\"type\":\"blank\",\"visibility\":\"public\"}" "http://codenvy.onprem/api/project/${WORKSPACE_ID}?name=project-1&token=${TOKEN}"

doPost "application/json" "{\"name\":\"user-1\",\"password\":\"pwd123ABC\"}" "http://codenvy.onprem/api/user/create?token=${TOKEN}"
fetchJsonParameter "id"
USER_ID=${OUTPUT}

doPost "application/json" "{\"userId\":\"${USER_ID}\",\"roles\":[\"account/owner\"]}" "http://codenvy.onprem/api/account/${ACCOUNT_ID}/members?token=${TOKEN}"
fetchJsonParameter "id"
ACCOUNT_ID=${OUTPUT}

authOnSite "user-1" "pwd123ABC"

createDefaultFactory ${TOKEN}
fetchJsonParameter "id"
FACTORY_ID=${OUTPUT}

# set date on tomorrow (repeate 3 times for sure)
executeSshCommand "sudo service ntpd stop" "analytics.codenvy.onprem"
TOMORROW_DATE=$(LC_TIME="uk_US.UTF-8" date -d '1 day')
executeSshCommand "sudo LC_TIME=\"uk_US.UTF-8\" date -s \"${TOMORROW_DATE}\"" "analytics.codenvy.onprem"

# analytics data
DATE=`date +"%Y%m%d"`
auth "admin" "new-password"
doGet "http://codenvy.onprem/analytics/api/service/launch/com.codenvy.analytics.services.PigRunnerFeature/${DATE}/${DATE}?token=${TOKEN}"   # takes about 20 minutes
doGet "http://codenvy.onprem/analytics/api/service/launch/com.codenvy.analytics.services.DataComputationFeature/${DATE}/${DATE}?token=${TOKEN}"
doGet "http://codenvy.onprem/analytics/api/service/launch/com.codenvy.analytics.services.DataIntegrityFeature/${DATE}/${DATE}?token=${TOKEN}"
doGet "http://codenvy.onprem/analytics/api/service/launch/com.codenvy.analytics.services.ViewBuilderFeature/${DATE}/${DATE}?token=${TOKEN}"

# check analytics: request users profiles = 1
doGet "http://codenvy.onprem/api/analytics/metric/users_profiles?token=${TOKEN}"
validateExpectedString ".*\"value\"\:\"1\".*"

# restore
executeIMCommand "im-restore" ${BACKUP}

# check data
auth "admin" "password"

doGet "http://codenvy.onprem/api/account/${ACCOUNT_ID}?token=${TOKEN}"
validateExpectedString ".*Account.*not.found.*"

doGet "http://codenvy.onprem/api/project/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*Workspace.*not.found.*"

doGet "http://codenvy.onprem/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
validateExpectedString ".*Workspace.*not.found.*"

doGet "http://codenvy.onprem/api/user/${USER_ID}?token=${TOKEN}"
validateExpectedString ".*User.*not.found.*"

doGet "http://codenvy.onprem/api/factory/${FACTORY_ID}?token=${TOKEN}"
validateExpectedString ".*Factory.*not.found.*"

# check analytics: request users profiles  = 0
doGet "http://codenvy.onprem/api/analytics/metric/users_profiles?token=${TOKEN}"
validateExpectedString ".*\"value\"\:\"0\".*"

# update
executeIMCommand "im-download" "codenvy" "${LATEST_CODENVY_VERSION}"
executeIMCommand "im-install" "--multi" "codenvy" "${LATEST_CODENVY_VERSION}"
validateInstalledCodenvyVersion ${LATEST_CODENVY_VERSION}

# restore
executeIMCommand "--valid-exit-code=1" "im-restore" ${BACKUP}
validateExpectedString ".*\"Version.of.backed.up.artifact.'${PREV_CODENVY_VERSION}'.doesn't.equal.to.restoring.version.'${LATEST_CODENVY_VERSION}'\".*\"status\".\:.\"ERROR\".*"

printAndLog "RESULT: PASSED"
vagrantDestroy
