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

printAndLog "TEST CASE: Backup and restore single-node Codenvy On Premise"

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

doPost "application/json" "{\"name\":\"account-1\"}" "http://codenvy.onprem/api/account?token=${TOKEN}"
ACCOUNT_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"name\":\"workspace-1\",\"accountId\":\"${ACCOUNT_ID}\"}" "http://codenvy.onprem/api/workspace?token=${TOKEN}"
WORKSPACE_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"type\":\"blank\",\"visibility\":\"public\"}" "http://codenvy.onprem/api/project/${WORKSPACE_ID}?name=project-1&token=${TOKEN}"
log ${OUTPUT}

doPost "application/json" "{\"name\":\"user-1\",\"password\":\"pwd123ABC\"}" "http://codenvy.onprem/api/user/create?token=${TOKEN}"
USER_ID=$(fetchJsonParameter "id")

doPost "application/json" "{\"userId\":\"${USER_ID}\",\"roles\":[\"account/owner\"]}" "http://codenvy.onprem/api/account/${ACCOUNT_ID}/members?token=${TOKEN}"
ACCOUNT_ID=$(fetchJsonParameter "id")

authOnSite "user-1" "pwd123ABC"

OUTPUT=$(curl 'http://codenvy.onprem/api/factory/?token='${TOKEN} -H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7yqwdS1Jq8TWiUAE'  --data-binary $'------WebKitFormBoundary7yqwdS1Jq8TWiUAE\r\nContent-Disposition: form-data; name="factoryUrl"\r\n\r\n{\r\n  "v": "2.1",\r\n  "project": {\r\n    "name": "my-minimalistic-factory",\r\n    "description": "Minimalistic Template"\r\n  },\r\n  "source": {\r\n    "project": {\r\n      "location": "https://github.com/codenvy/sdk",\r\n      "type": "git"\r\n    }\r\n  }\r\n}\r\n------WebKitFormBoundary7yqwdS1Jq8TWiUAE--\r\n')
FACTORY_ID=$(fetchJsonParameter "id")
log ${OUTPUT}

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

# update
executeIMCommand "im-download" "codenvy" "${LATEST_CODENVY_VERSION}"
executeIMCommand "im-install" "codenvy" "${LATEST_CODENVY_VERSION}"
validateInstalledCodenvyVersion ${LATEST_CODENVY_VERSION}

# restore
executeIMCommand "valid-exit-code=1" "im-restore" ${BACKUP}

printAndLog "RESULT: PASSED"
vagrantDestroy
