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

# add node1
executeIMCommand "im-add-node" "node1.codenvy"
validateExpectedString ".*\"type\".\:.\"MACHINE\".*\"host\".\:.\"node1.codenvy\".*"

executeSshCommand "sudo systemctl stop iptables"  # open port 23750
doGet "http://${HOST_URL}:23750/info"
validateExpectedString ".*\[\"node1.codenvy\",\"node1.codenvy:2375\"\].*"

# add node2
executeIMCommand "im-add-node" "node2.codenvy"
validateExpectedString ".*\"type\".\:.\"MACHINE\".*\"host\".\:.\"node2.codenvy\".*"

executeSshCommand "sudo systemctl stop iptables"  # open port 23750
doGet "http://${HOST_URL}:23750/info"
validateExpectedString ".*[\"node2.codenvy\",\"node2.codenvy:2375\"].*"

# Incorrect name
executeIMCommand "--valid-exit-code=1" "im-add-node" "bla-bla-bla"
validateExpectedString ".*Correct.name.template.is...prefix..number..base_node_domain.*"

# Host is not reachable
executeIMCommand "--valid-exit-code=1" "im-add-node" "node3.codenvy"
validateExpectedString ".*Can.t.connect.to.host..vagrant@node3.codenvy:22.*"

# Runner has been already set up
executeIMCommand "--valid-exit-code=1" "im-add-node" "node2.codenvy"
validateExpectedString ".*Node..node2.codenvy..has.been.already.used.*"

# remove node1
executeIMCommand "im-remove-node" "node1.codenvy"
validateExpectedString ".*\"type\".\:.\"MACHINE\".*\"host\".\:.\"node1.codenvy\".*"
doSleep "sleep 1m"  "Wait until Docker machine takes into account /usr/local/swarm/node_list config"

executeSshCommand "sudo systemctl stop iptables"  # open port 23750
doGet "http://${HOST_URL}:23750/info"
validateExpectedString ".*Nodes\",\"2\".*"

# remove node2
executeIMCommand "im-remove-node" "node2.codenvy"
validateExpectedString ".*\"type\".\:.\"MACHINE\".*\"host\".\:.\"node2.codenvy\".*"
doSleep "sleep 1m"  "Wait until Docker machine takes into account /usr/local/swarm/node_list config"

executeSshCommand "sudo systemctl stop iptables"  # open port 23750
doGet "http://${HOST_URL}:23750/info"
validateExpectedString ".*Nodes\",\"1\".*"

# remove already removed node1
executeIMCommand "--valid-exit-code=1" "im-remove-node" "node1.codenvy"
validateExpectedString ".*Node..node1.codenvy..is.not.found.*"

printAndLog "RESULT: PASSED"
vagrantDestroy
