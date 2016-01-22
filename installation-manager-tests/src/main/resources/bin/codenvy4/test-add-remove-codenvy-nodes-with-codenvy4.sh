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

printAndLog "TEST CASE: Add and remove Codenvy 4.x All-In-One nodes"
vagrantUp ${SINGLE_CODENVY4_WITH_ADDITIONAL_NODES_VAGRANT_FILE}

# install Codenvy 4.x
installCodenvy ${LATEST_CODENVY4_VERSION}
validateInstalledCodenvyVersion ${LATEST_CODENVY4_VERSION}

# copy ssh key to codenvy node
scp -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key -P 2222 ~/.vagrant.d/insecure_private_key vagrant@127.0.0.1:./.ssh/id_rsa >> ${TEST_LOG}

# throw error that dns is incorrect
executeIMCommand "--valid-exit-code=1" "im-add-node" "bla-bla-bla"
validateExpectedString ".*Illegal.DNS.name.'bla-bla-bla'.of.additional.node..Correct.DNS.name.templates\:.\['node<number>.${HOST_URL}'\].*"

# throw error that host is not reachable
executeIMCommand "--valid-exit-code=1" "im-add-node" "node3.codenvy"
validateExpectedString ".*Can.t.connect.to.host..vagrant@node3.codenvy:22.*"

# add node1
executeIMCommand "im-add-node" "--codenvyIp 192.168.56.110" "node1.${HOST_URL}"
validateExpectedString ".*\"type\".\:.\"MACHINE\".*\"host\".\:.\"node1.${HOST_URL}\".*"

executeSshCommand "sudo systemctl stop iptables"  # open port 23750
doGet "http://${HOST_URL}:23750/info"
validateExpectedString ".*Nodes\",\"2\".*\[\"master.${HOST_URL}\",\"${HOST_URL}:2375\"\].*\[\"node1.${HOST_URL}\",\"node1.${HOST_URL}:2375\"\].*"

# throw error that node has been already used
executeIMCommand "--valid-exit-code=1" "im-add-node" "node1.codenvy"
validateExpectedString ".*Node..node1.${HOST_URL}..has.been.already.used.*"

# change Codenvy hostname to verify if puppet.conf file will be successfully updated
executeSshCommand "sudo sed -i 's/ ${HOST_URL}/ ${NEW_HOST_URL}/' /etc/hosts"
executeSshCommand "sudo sed -i 's/ ${HOST_URL}/ ${NEW_HOST_URL}/' /etc/hosts" "node1.${HOST_URL}"
executeSshCommand "sudo sed -i 's/ ${HOST_URL}/ ${NEW_HOST_URL}/' /etc/hosts" "node2.${NEW_HOST_URL}"
executeIMCommand "im-config" "--hostname" "${NEW_HOST_URL}"

# add node2
executeIMCommand "im-add-node" "node2.${NEW_HOST_URL}"
validateExpectedString ".*\"type\".\:.\"MACHINE\".*\"host\".\:.\"node2.${NEW_HOST_URL}\".*"

executeSshCommand "sudo systemctl stop iptables"  # open port 23750
doGet "http://${HOST_URL}:23750/info"
validateExpectedString ".*Nodes\",\"3\".*\[\"master.${HOST_URL}\",\"${NEW_HOST_URL}:2375\"\].*\[\"node1.${HOST_URL}\",\"node1.${HOST_URL}:2375\"\].*[\"node2.${NEW_HOST_URL}\",\"node2.${NEW_HOST_URL}:2375\"].*"

# remove node1
executeIMCommand "im-remove-node" "node1.${HOST_URL}"
validateExpectedString ".*\"type\".\:.\"MACHINE\".*\"host\".\:.\"node1.${HOST_URL}\".*"
doSleep "1m"  "Wait until Docker machine takes into account /usr/local/swarm/node_list config"

executeSshCommand "sudo systemctl stop iptables"  # open port 23750
doGet "http://${HOST_URL}:23750/info"
validateExpectedString ".*Nodes\",\"2\".*\[\"master.${HOST_URL}\",\"${NEW_HOST_URL}:2375\"\].*\[\"node2.${NEW_HOST_URL}\",\"node2.${NEW_HOST_URL}:2375\"\].*"

# remove node2
executeIMCommand "im-remove-node" "node2.${NEW_HOST_URL}"
validateExpectedString ".*\"type\".\:.\"MACHINE\".*\"host\".\:.\"node2.${NEW_HOST_URL}\".*"
doSleep "1m"  "Wait until Docker machine takes into account /usr/local/swarm/node_list config"

executeSshCommand "sudo systemctl stop iptables"  # open port 23750
doGet "http://${NEW_HOST_URL}:23750/info"
validateExpectedString ".*Nodes\",\"1\".*\[\"master.${HOST_URL}\",\"${NEW_HOST_URL}:2375\"\].*"

# remove already removed node1
executeIMCommand "--valid-exit-code=1" "im-remove-node" "node1.${HOST_URL}"
validateExpectedString ".*Node..node1.${HOST_URL}..is.not.found.*"

printAndLog "RESULT: PASSED"
vagrantDestroy
