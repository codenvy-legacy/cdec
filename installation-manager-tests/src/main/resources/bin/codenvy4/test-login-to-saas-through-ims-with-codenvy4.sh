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

printAndLog "TEST CASE: Check login to Codenvy SaaS through installation manager service with Codenvy 4"
#vagrantUp ${SINGLE_NODE_VAGRANT_FILE}
#
#installCodenvy ${LATEST_CODENVY4_VERSION}
#validateInstalledCodenvyVersion

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

ACCOUNT_PASSWORD="pwd123ABC"

# add user with [account/owner] role
doPost "application/json" "{\"name\":\"${UUID_OWNER}@codenvy.com\",\"password\":\"${ACCOUNT_PASSWORD}\"}" "${SAAS_SERVER}/api/user/create?token=${TOKEN}"
fetchJsonParameter "id"
USER_OWNER_ID=${OUTPUT}
doPost "application/json" "{\"userId\":\"${USER_OWNER_ID}\",\"roles\":[\"account/owner\"]}" "${SAAS_SERVER}/api/account/${ACCOUNT_ID}/members?token=${TOKEN}"

authWithoutRealmAndServerDns "admin" "password"

# need to ensure logout state
doHttpRequest --method=POST \
              --url=http://${HOST_URL}/im/logout?token=${TOKEN} \
              --output-http-code
validateExpectedString "200"

# test login
doHttpRequest --method=POST \
              --content-type="application/json" \
              --body="{\"username\":\"${UUID_OWNER}@codenvy.com\",\"password\":\"${ACCOUNT_PASSWORD}\"}" \
              --url="http://${HOST_URL}/im/login?token=${TOKEN}" \
              --output-http-code
validateExpectedString "200"

# test logout
doHttpRequest --method=POST \
              --url=http://${HOST_URL}/im/logout?token=${TOKEN} \
              --output-http-code
validateExpectedString "200"

printAndLog "RESULT: PASSED"

vagrantDestroy
