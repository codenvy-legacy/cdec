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

installCodenvy ${LATEST_CODENVY3_VERSION}
validateInstalledCodenvyVersion

auth "admin" "password"

# change admin's password
executeIMCommand "im-password" "password" "new-password"
auth "admin" "new-password"

# change Codenvy hostname
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts" "data.codenvy"
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts" "api.codenvy"
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts" "site.codenvy"
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts" "runner1.codenvy"
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts" "builder1.codenvy"
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts" "datasource.codenvy"
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts" "analytics.codenvy"
executeSshCommand "sudo sed -i 's/ codenvy/ test.codenvy/' /etc/hosts" "master.codenvy"

executeIMCommand "im-config" "--hostname" "${NEW_HOST_URL}"

# verify changes on api node
executeSshCommand "sudo cat /home/codenvy/codenvy-data/conf/general.properties" "api.codenvy"
sleep 10m
executeSshCommand "sudo cat /home/codenvy/codenvy-data/conf/general.properties" "api.codenvy"
executeSshCommand "sudo grep \"api.endpoint=http://${NEW_HOST_URL}/api\" /home/codenvy/codenvy-data/conf/general.properties" "api.codenvy"

# verify changes on installation-manager service
executeSshCommand "sudo cat /home/codenvy-im/codenvy-im-data/conf/installation-manager.properties"
sleep 10m
executeSshCommand "sudo cat /home/codenvy-im/codenvy-im-data/conf/installation-manager.properties"

executeSshCommand "sudo grep \"api.endpoint=http://${NEW_HOST_URL}/api\" /home/codenvy-im/codenvy-im-data/conf/installation-manager.properties"

auth "admin" "new-password" "http://${NEW_HOST_URL}"

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
