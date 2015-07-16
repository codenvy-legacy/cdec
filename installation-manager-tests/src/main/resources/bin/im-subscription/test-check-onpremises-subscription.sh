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

installImCliClient
validateInstalledImCliClientVersion

UUID_OWNER=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 4 | head -n 1)
PASSWORD="pwd123ABC"

# create account
doPost "application/json" "{\"name\":\"account-${UUID_OWNER}\"}" "${SAAS_SERVER}/api/account?token=${TOKEN}"
TMP=${OUTPUT}
fetchJsonParameter "id"
ACCOUNT_ID=${OUTPUT}

OUTPUT=${TMP}
fetchJsonParameter "name"
ACCOUNT_NAME=${OUTPUT}

# add user with [account/owner] role
doPost "application/json" "{\"name\":\"${UUID_OWNER}@codenvy.com\",\"password\":\"${PASSWORD}\"}" "${SAAS_SERVER}/api/user/create?token=${TOKEN}"
fetchJsonParameter "id"
USER_OWNER_ID=${OUTPUT}
doPost "application/json" "{\"userId\":\"${USER_OWNER_ID}\",\"roles\":[\"account/owner\"]}" "${SAAS_SERVER}/api/account/${ACCOUNT_ID}/members?token=${TOKEN}"

# test im-subscription without login
executeIMCommand "--valid-exit-code=1" "im-subscription"
validateExpectedString ".*\"message\".\:.\"Please.log.in.into.\'saas-server\'.remote\.\".*\"status\".\:.\"ERROR\".*"

executeIMCommand "--valid-exit-code=1" "login" "${UUID_OWNER}@codenvy.com" "${PASSWORD}"

# test im-subscription after login and adding OnPremises subscription
executeIMCommand "im-subscription"
validateUnExpectedString ".*\"subscription\".\:.\"OnPremises\".*\"message\".\:.\"Subscription is valid\".*\"status\".\:.\"OK\".*"

printAndLog "RESULT: PASSED"

vagrantDestroy
