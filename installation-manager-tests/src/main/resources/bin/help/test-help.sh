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

printAndLog "TEST CASE: Help command when CLI client is working in non-interractive mode"

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient
validateInstalledImCliClientVersion

executeIMCommand "help"

if [[ ! ${OUTPUT} =~ .*build.*create-factory.*create-project.*im-add-node.*Add.new.Codenvy.node.*login.*Login.to.a.remote.Codenvy.cloud.*update-factory.*REMOTES.*default*.*https://codenvy.com.*saas-server.*${SAAS_SERVER}.* ]]; then
    validateExitCode 1
fi

printAndLog "RESULT: PASSED"

vagrantDestroy
