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

printAndLog "TEST CASE: Install the latest multi-node Codenvy On Premise"
vagrantUp ${MULTI_NODE_VAGRANT_FILE}

installCodenvy
validateInstalledCodenvyVersion

auth "admin" "password"

# change admin's password
executeIMCommand "im-password" "password" "new-password"
auth "admin" "new-password"

# change Codenvy hostname
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts"
executeIMCommand "im-config" "--hostname" "${NEW_HOSTNAME}"

# verify changes on api node
executeSshCommand "sudo cat /home/codenvy/codenvy-data/conf/general.properties"  "api.codenvy"  # TODO [ndp] remove
sleep 2m                                                                                        # TODO [ndp] remove
executeSshCommand "sudo cat /home/codenvy/codenvy-data/conf/general.properties"  "api.codenvy"  # TODO [ndp] remove
executeSshCommand "sudo grep \"api.endpoint=http://${NEW_HOSTNAME}/api\" /home/codenvy/codenvy-data/conf/general.properties" "api.codenvy"

# verify changes on installation-manager service
executeSshCommand "sudo cat /home/codenvy-im/codenvy-im-data/conf/installation-manager.properties"  # TODO [ndp] remove
sleep 2m                                                                                            # TODO [ndp] remove
executeSshCommand "sudo cat /home/codenvy-im/codenvy-im-data/conf/installation-manager.properties"  # TODO [ndp] remove
executeSshCommand "sudo grep \"api.endpoint=http://${NEW_HOSTNAME}/api\" /home/codenvy-im/codenvy-im-data/conf/installation-manager.properties"

auth "admin" "new-password" "http://${NEW_HOSTNAME}"

# test re-install
# remove codenvy binaries

executeSshCommand "sudo rm -rf /home/codenvy/codenvy-tomcat/webapps" "api.codenvy"
executeSshCommand "sudo rm -rf /home/codenvy/codenvy-tomcat/webapps" "runner1.codenvy"
executeSshCommand "sudo rm -rf /home/codenvy-im/codenvy-im-tomcat/webapps"

# perform re-install
executeIMCommand "im-install" "--reinstall" "codenvy"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"status\".\:.\"SUCCESS\".*\"status\".\:.\"OK\".*"

validateInstalledCodenvyVersion

printAndLog "RESULT: PASSED"
vagrantDestroy
