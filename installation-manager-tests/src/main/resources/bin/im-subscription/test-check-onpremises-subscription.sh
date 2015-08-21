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

[ -f "./lib.sh" ] && . ./lib.sh
[ -f "../lib.sh" ] && . ../lib.sh

printAndLog "TEST CASE: Check subscription"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

# TODO [ndp] Test is failing so as on hosted Codenvy server "https://nightly.codenvy-stg.com" autorization doesn't work:
# POST requests to adding subscription method https://updater-nightly.codenvy-dev.com/update/repository/subscription return error message about invalid token.

#UPDATE_SERVER="http://updater.codenvy-stg.com"
#UPDATE_SERVICE="https://codenvy-stg.com/update"
#SAAS_SERVER="https://codenvy-stg.com"

AVAILABLE_IM_CLI_CLIENT_VERSIONS=$(curl -s -X GET ${UPDATE_SERVICE}/repository/updates/installation-manager-cli)
LATEST_IM_CLI_CLIENT_VERSION=`echo ${AVAILABLE_IM_CLI_CLIENT_VERSIONS} | sed 's/.*"\([^"]*\)".*/\1/'`

#executeSshCommand "echo 'export CHE_LOCAL_CONF_DIR=/home/vagrant/codenvy_conf' >> .bashrc"
#executeSshCommand "mkdir /home/vagrant/codenvy_conf"
#executeSshCommand "echo 'saas.api.endpoint=https://codenvy-stg.com/api' > /home/vagrant/codenvy_conf/im.properties"
#executeSshCommand "echo 'installation-manager.update_server_endpoint=https://codenvy-stg.com/update' >> /home/vagrant/codenvy_conf/im.properties"

installImCliClient
validateInstalledImCliClientVersion

UUID_OWNER=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 4 | head -n 1)

auth "prodadmin" "CodenvyAdmin" "${SAAS_SERVER}"

# create account
doPost "application/json" "{\"name\":\"account-${UUID_OWNER}\"}" "${SAAS_SERVER}/api/account?token=${TOKEN}"
TMP=${OUTPUT}
fetchJsonParameter "id"
ACCOUNT_ID=${OUTPUT}

OUTPUT=${TMP}
fetchJsonParameter "name"
ACCOUNT_NAME=${OUTPUT}

PASSWORD="pwd123ABC"

# add user with [account/owner] role
doPost "application/json" "{\"name\":\"${UUID_OWNER}@codenvy.com\",\"password\":\"${PASSWORD}\"}" "${SAAS_SERVER}/api/user/create?token=${TOKEN}"
fetchJsonParameter "id"
USER_OWNER_ID=${OUTPUT}
doPost "application/json" "{\"userId\":\"${USER_OWNER_ID}\",\"roles\":[\"account/owner\"]}" "${SAAS_SERVER}/api/account/${ACCOUNT_ID}/members?token=${TOKEN}"

# test im-subscription without login
executeIMCommand "--valid-exit-code=1" "im-subscription"
validateExpectedString ".*Please.log.in.into..saas-server..remote.*"

log "Expected failure"
executeIMCommand "login" "${UUID_OWNER}@codenvy.com" "${PASSWORD}" "${ACCOUNT_NAME}"

# test im-subscription after login and adding OnPremises subscription
executeIMCommand "im-subscription"
validateExpectedString ".*\"subscription\".\:.\"OnPremises\".*\"message\".\:.\"Subscription.is.valid\".*"

printAndLog "RESULT: PASSED"

vagrantDestroy
