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

printAndLog "TEST CASE: Login with username and password"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient
validateInstalledImCliClientVersion

# test im-subscription without login
executeIMCommand "--valid-exit-code=1" "im-subscription"
if [[ ! ${OUTPUT} =~ .*\"message\".\:.\"Please.log.in.into.\'saas-server\'.remote\.\".*\"status\".\:.\"ERROR\".* ]]; then
    validateExitCode 1
fi

executeIMCommand "login" "${CODENVY_SAAS_USER_WITHOUT_SUBSCRIPTION_NAME}" "${CODENVY_SAAS_USER_WITHOUT_SUBSCRIPTION_PASSWORD}"

# test im-subscription after login and adding OnPremises subscription
executeIMCommand "im-subscription"
if [[ ! ${OUTPUT} =~ .*\"subscription\".\:.\"OnPremises\".*\"message\".\:.\"Subscription is valid\".*\"status\".\:.\"OK\".* ]]; then
    validateExitCode 1
fi

printAndLog "RESULT: PASSED"

vagrantDestroy
