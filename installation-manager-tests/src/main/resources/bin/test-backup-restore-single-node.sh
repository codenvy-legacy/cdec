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

# modify data
executeIMCommand "im-password" "password" "new-password"

OUTPUT=$(curl -H "Content-Type: application/json" -d '{"name":"account-1"}' -X POST 'http://codenvy.onprem/api/account?token='${TOKEN})
ACCOUNT_ID=$(fetchJsonParameter "id")
log ${OUTPUT}

OUTPUT=$(curl -H "Content-Type: application/json" -d '{"name":"workspace-1", "accountId":"'${ACCOUNT_ID}'"}' -X POST 'http://codenvy.onprem/api/workspace?token='${TOKEN})
WORKSPACE_ID=$(fetchJsonParameter "id")
log ${OUTPUT}

OUTPUT=$(curl -H "Content-Type: application/json" -d '{"type":"blank", "visibility":"public"}' -X POST 'http://codenvy.onprem/api/project/'${WORKSPACE_ID}'?name=project-1&token='${TOKEN})
log ${OUTPUT}

FACTORY_DATA='{
  "v": "2.1",
  "project": {
    "name": "my-minimalistic-factory",
    "description": "Minimalistic Template"
  },
  "source": {
    "project": {
      "location": "https://github.com/codenvy/sdk",
      "type": "git"
    }
  }
}'
echo "${FACTORY_DATA}" > f
OUTPUT=$(curl -H "Content-Type: multipart/form-data; boundary=----WebKitFormBoundary9zsnEVSuJC5kDWIq" --data-binary @f -X POST 'http://codenvy.onprem/api/factory?token='${TOKEN})
FACTORY_ID=$(fetchJsonParameter "id")
log ${OUTPUT}


# update
executeIMCommand "im-download" "codenvy" "${LATEST_CODENVY_VERSION}"
executeIMCommand "im-install" "codenvy" "${LATEST_CODENVY_VERSION}"
validateInstalledCodenvyVersion ${LATEST_CODENVY_VERSION}

# restore
executeIMCommand "im-restore" ${BACKUP}

printAndLog "RESULT: PASSED"
vagrantDestroy
