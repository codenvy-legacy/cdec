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

vagrantUp ${MULTI_NODE_VAGRANT_FILE}

printAndLog "TEST CASE: Backup and restore multi-nodes Codenvy On Premise"

# install Codenvy
installCodenvy ${PREV_CODENVY_VERSION}
validateInstalledCodenvyVersion ${PREV_CODENVY_VERSION}
auth "admin" "password"

# backup
executeIMCommand "im-backup"
BACKUP=$(fetchJsonParameter "file")

# modify data: add accout, workspace, project, user, factory
executeIMCommand "im-password" "password" "new-password"
auth "admin" "new-password"

# set date on yesterday
executeSshCommand "sudo service ntpd stop"
executeSshCommand "sudo date -s \"$(date -d '-1 day')\""

doPost "application/json" "{\"name\":\"account-4\"}" "http://codenvy.onprem/api/account?token=${TOKEN}"
ACCOUNT_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"name\":\"workspace-4\",\"accountId\":\"${ACCOUNT_ID}\"}" "http://codenvy.onprem/api/workspace?token=${TOKEN}"
WORKSPACE_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"type\":\"blank\",\"visibility\":\"public\"}" "http://codenvy.onprem/api/project/${WORKSPACE_ID}?name=project-1&token=${TOKEN}"

doPost "application/json" "{\"name\":\"user-1\",\"password\":\"pwd123ABC\"}" "http://codenvy.onprem/api/user/create?token=${TOKEN}"
USER_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"userId\":\"${USER_ID}\",\"roles\":[\"account/owner\"]}" "http://codenvy.onprem/api/account/${ACCOUNT_ID}/members?token=${TOKEN}"
ACCOUNT_ID=$(fetchJsonParameter "id")

authOnSite "user-1" "pwd123ABC"

createDefaultFactory ${TOKEN}
FACTORY_ID=$(fetchJsonParameter "id")

# set date on today
executeSshCommand "sudo date -s \"$(date -d '1 day')\""

# analytics data
DATE=`date --date="yesterday" +"%Y%m%d"`
auth "admin" "new-password"
doGet "http://codenvy.onprem/analytics/api/service/launch/com.codenvy.analytics.services.PigRunnerFeature/${DATE}/${DATE}?token=${TOKEN}"   # takes about 40 minutes
doGet "http://codenvy.onprem/analytics/api/service/launch/com.codenvy.analytics.services.DataComputationFeature/${DATE}/${DATE}?token=${TOKEN}"
doGet "http://codenvy.onprem/analytics/api/service/launch/com.codenvy.analytics.services.DataIntegrityFeature/${DATE}/${DATE}?token=${TOKEN}"
doGet "http://codenvy.onprem/analytics/api/service/launch/com.codenvy.analytics.services.ViewBuilderFeature/${DATE}/${DATE}?token=${TOKEN}"

# check analytics: request total users = 1
doGet "http://codenvy.onprem/api/analytics/metric/total_users?token=${TOKEN}"
[[ ! ${OUTPUT} =~ .*\"value\"\:\"3\".* ]] && validateExitCode 1

# restore
executeIMCommand "im-restore" ${BACKUP}

# check data
auth "admin" "new-password"

doGet "http://codenvy.onprem/api/account/${ACCOUNT_ID}?token=${TOKEN}"
[[ ! ${OUTPUT} =~ .*Account.*not.found.* ]] && validateExitCode 1

doGet "http://codenvy.onprem/api/project/${WORKSPACE_ID}?token=${TOKEN}"
[[ ! ${OUTPUT} =~ .*Workspace.*not.found.* ]] && validateExitCode 1

doGet "http://codenvy.onprem/api/workspace/${WORKSPACE_ID}?token=${TOKEN}"
[[ ! ${OUTPUT} =~ .*Workspace.*not.found.* ]] && validateExitCode 1

doGet "http://codenvy.onprem/api/user/${USER_ID}?token=${TOKEN}"
[[ ! ${OUTPUT} =~ .*User.*not.found.* ]] && validateExitCode 1

doGet "http://codenvy.onprem/api/factory/${FACTORY_ID}?token=${TOKEN}"
[[ ! ${OUTPUT} =~ .*Factory.*not.found.* ]] && validateExitCode 1

# check analytics: request total users = 0
doGet "http://codenvy.onprem/api/analytics/metric/total_users?token=${TOKEN}"
[[ ! ${OUTPUT} =~ .*\"value\"\:\"0\".* ]] && validateExitCode 1

# update
executeIMCommand "im-download" "codenvy" "${LATEST_CODENVY_VERSION}"
executeIMCommand "im-install" "codenvy" "${LATEST_CODENVY_VERSION}"
validateInstalledCodenvyVersion ${LATEST_CODENVY_VERSION}

# restore
executeIMCommand "valid-exit-code=1" "im-restore" ${BACKUP}

printAndLog "RESULT: PASSED"
vagrantDestroy
