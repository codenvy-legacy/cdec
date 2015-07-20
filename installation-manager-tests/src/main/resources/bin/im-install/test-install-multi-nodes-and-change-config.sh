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

vagrantUp ${MULTI_NODE_VAGRANT_FILE}

printAndLog "TEST CASE: Install the latest multi-node Codenvy On Premise"

installCodenvy
validateInstalledCodenvyVersion

auth "admin" "password"
executeIMCommand "im-password" "password" "new-password"
auth "admin" "new-password"

executeIMCommand "im-config" "--hostname" "${NEW_HOSTNAME}"
executeSshCommand "sudo sed -i 's/ codenvy.onprem/ test.codenvy.onprem/' /etc/hosts"

# verify changes on api node
executeSshCommand "sudo grep \"api.endpoint=http://${NEW_HOSTNAME}/api\" /home/codenvy/codenvy-data/conf/general.properties" "api.codenvy.onprem"

# verify changes on installation-manager service
executeSshCommand "sudo grep \"api.endpoint=http://${NEW_HOSTNAME}/api\" /home/codenvy-im/codenvy-im-data/conf/installation-manager.properties"

auth "admin" "new-password" "${NEW_HOSTNAME}"


printAndLog "RESULT: PASSED"
vagrantDestroy