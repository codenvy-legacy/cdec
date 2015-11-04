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

. ./config.sh

trap cleanUp EXIT

cleanUp() {
    vagrantDestroy
}

printAndLog() {
    echo $@
    log $@
}

logStartCommand() {
    log
    log "=== [ "`date`" ] COMMAND STARTED: "$@
}

logEndCommand() {
    log "=================================== COMMAND COMPLETED: "$@
    log
}

log() {
    echo "TEST: "$@ >> ${TEST_LOG}
}

validateExitCode() {
    EXIT_CODE=$1
    VALID_CODE=$2
    IS_INSTALL_CODENVY=$3

    if [[ ! -z ${VALID_CODE} ]]; then
        if [[ ${EXIT_CODE} == ${VALID_CODE} ]]; then
            return
        fi
    else
        if [[ ${EXIT_CODE} == "0" ]]; then
            return
        fi
    fi

    printAndLog "RESULT: FAILED"
    $(retrieveTestLogs)

    if [[ ! -z ${IS_INSTALL_CODENVY} ]]; then
        installLogContent=$(ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@$(detectMasterNode) "cat install.log | sed 's/-\\\|\\///g'" | sed 's/\r//')
        printAndLog "============= Install.log file content ========="
        printAndLog "${installLogContent}"
        printAndLog "================================================="
    fi

    vagrantDestroy
    exit 1
}

retrieveTestLogs() {
    INSTALL_ON_NODE=$(detectMasterNode)
    logDirName="logs/`basename "$0" | sed 's/\\.sh//g'`"
    log "Name of directory with logs: "${logDirName}

    mkdir --parent ${logDirName}

    if [[ ${INSTALL_ON_NODE} == "master.codenvy" ]]; then
        for HOST in master api analytics data site runner1 builder1 datasource; do
            $(ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${HOST}.codenvy "sudo chown -R root:root /var/log/puppet")
            $(ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${HOST}.codenvy "sudo chmod 777 /var/log/puppet")
            scp -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${HOST}.codenvy:/var/log/puppet/puppet-agent.log ${logDirName}/puppet-agent-${HOST}.log
        done
    else
        $(ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@$(detectMasterNode) "sudo chown -R root:root /var/log/puppet")
        $(ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@$(detectMasterNode) "sudo chmod 777 /var/log/puppet")
        scp -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@codenvy:/var/log/puppet/puppet-agent.log ${logDirName}/puppet-agent.log
    fi

    scp -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${INSTALL_ON_NODE}:/home/vagrant/install.log ${logDirName}/install.log
    scp -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${INSTALL_ON_NODE}:/home/vagrant/codenvy-im/codenvy-cli/data/tmp/im-non-interactive.log ${logDirName}/im-non-interactive.log
}

vagrantDestroy() {
    vagrant destroy -f >> ${TEST_LOG}
}

validateInstalledCodenvyVersion() {
    VERSION=$1

    [[ -z ${VERSION} ]] && VERSION=${LATEST_CODENVY_VERSION}
    logStartCommand "validateInstalledCodenvyVersion "${VERSION}

    executeIMCommand "im-install" "--list"
    validateExpectedString ".*\"artifact\".*\:.*\"codenvy\".*\"version\".*\:.*\"${VERSION}\".*\"status\".*\:.*\"SUCCESS\".*"

    logEndCommand "validateInstalledCodenvyVersion: OK"
}

validateInstalledImCliClientVersion() {
    VERSION=$1

    [[ -z ${VERSION} ]] && VERSION=${LATEST_IM_CLI_CLIENT_VERSION}

    logStartCommand "validateInstalledImCliClientVersion "${VERSION}

    executeIMCommand "im-install" "--list"
    validateExpectedString ".*\"artifact\".*\:.*\"installation-manager-cli\".*\"version\".*\:.*\"${VERSION}\".*\"status\".*\:.*\"SUCCESS\".*"

    logEndCommand "validateInstalledImCliClientVersion: OK"
}

installCodenvy() {
    MULTI_OPTION=""
    VERSION_OPTION=""
    INSTALL_ON_NODE=$(detectMasterNode)

    VALID_CODE=0
    if [[ $1 =~ --valid-exit-code=.* ]]; then
        VALID_CODE=`echo "$1" | sed -e "s/--valid-exit-code=//g"`
        shift
    fi

    if [[ ${INSTALL_ON_NODE} == "master.codenvy" ]]; then
        scp -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key -P 2222 ~/.vagrant.d/insecure_private_key vagrant@127.0.0.1:./.ssh/id_rsa >> ${TEST_LOG}
        MULTI_OPTION="--multi"
    fi

    VERSION=$1
    [[ ! -z ${VERSION} ]] && VERSION_OPTION="--version="${VERSION}

    logStartCommand "installCodenvy "$@

    ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${INSTALL_ON_NODE} 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVICE}'/repository/public/download/install-codenvy) --silent '${MULTI_OPTION}' '${VERSION_OPTION} >> ${TEST_LOG}
    EXIT_CODE=$?
    validateExitCode ${EXIT_CODE} ${VALID_CODE} --installCodenvy

    sleep 5m
    logEndCommand "installCodenvy: OK"
}

installImCliClient() {
    logStartCommand "installImCliClient "$@

    VERSION=$1
    VERSION_OPTION=""
    [[ ! -z ${VERSION} ]] && VERSION_OPTION="--version="${VERSION}

    ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@codenvy 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVICE}'/repository/public/download/install-im-cli) '${VERSION_OPTION} >> ${TEST_LOG}
    validateExitCode $?

    logEndCommand "installImCliClient: OK"
}

vagrantUp() {
    VAGRANT_FILE=$1

    cp ${VAGRANT_FILE} Vagrantfile

    vagrant up >> ${TEST_LOG}
    validateExitCode $?
}

auth() {
    doAuth $1 $2 "sysldap" $3
}

authOnSite() {
    doAuth $1 $2 "org" $3
}

doAuth() {
    logStartCommand "auth "$@

    USERNAME=$1
    PASSWORD=$2
    REALM=$3
    SERVER_DNS=$4

    [[ -z ${SERVER_DNS} ]] && SERVER_DNS="http://codenvy"

    OUTPUT=$(curl -s -X POST -H "Content-Type: application/json" -d '{"username":"'${USERNAME}'", "password":"'${PASSWORD}'", "realm":"'${REALM}'"}' ${SERVER_DNS}/api/auth/login)
    EXIT_CODE=$?

    log ${OUTPUT}
    validateExitCode $?

    fetchJsonParameter "value"
    TOKEN=${OUTPUT}

    logEndCommand "auth: OK"
}

executeIMCommand() {
    logStartCommand "executeIMCommand "$@

    VALID_CODE=0
    if [[ $1 =~ --valid-exit-code=.* ]]; then
        VALID_CODE=`echo "$1" | sed -e "s/--valid-exit-code=//g"`
        shift
    fi
    EXECUTE_ON_NODE=$(detectMasterNode)

    OUTPUT=$(ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${EXECUTE_ON_NODE} "/home/vagrant/codenvy-im/codenvy-cli/bin/codenvy $@")
    EXIT_CODE=$?

    log ${OUTPUT}
    validateExitCode ${EXIT_CODE} ${VALID_CODE}

    logEndCommand "executeIMCommand: OK"
}

executeSshCommand() {
    logStartCommand "executeSshCommand "$@

    VALID_CODE=0
    if [[ $1 =~ --valid-exit-code=.* ]]; then
        VALID_CODE=`echo "$1" | sed -e "s/--valid-exit-code=//g"`
        shift
    fi

    COMMAND=$1

    EXECUTE_ON_NODE=$2
    [[ -z ${EXECUTE_ON_NODE} ]] && EXECUTE_ON_NODE=$(detectMasterNode)

    OUTPUT=$(ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${EXECUTE_ON_NODE} "${COMMAND}")
    EXIT_CODE=$?

    log ${OUTPUT}
    validateExitCode ${EXIT_CODE} ${VALID_CODE}

    logEndCommand "executeSshCommand: OK"
}

detectMasterNode() {
    ping -c1 -q "master.codenvy" >> /dev/null
    if [[ $? == 0 ]]; then
        echo "master.codenvy"
    else
        ping -c1 -q "codenvy" >> /dev/null
        if [[ $? == 0 ]]; then
            echo "codenvy"
        else
            validateExitCode 1
        fi
    fi
}

fetchJsonParameter() {
    validateExpectedString ".*\"$1\".*"
    OUTPUT=`echo ${OUTPUT} | sed 's/.*"'$1'"\s*:\s*"\([^"]*\)*".*/\1/'`
}

doPost() {
    logStartCommand "POST "$@

    CONTENT_TYPE=$1
    BODY=$2
    URL=$3

    OUTPUT=$(curl -H "Content-Type: ${CONTENT_TYPE}" -d "${BODY}" -X POST ${URL})
    EXIT_CODE=$?
    log ${OUTPUT}

    validateExitCode ${EXIT_CODE}

    logEndCommand "curl: OK"
}

doGet() {
    logStartCommand "GET "$@

    URL=$1

    OUTPUT=$(curl -X GET ${URL})
    EXIT_CODE=$?
    log ${OUTPUT}

    validateExitCode ${EXIT_CODE}

    logEndCommand "curl: OK"
}

createDefaultFactory() {
    logStartCommand "createDefaultFactory"

    TOKEN=$1

    OUTPUT=$(curl 'http://codenvy/api/factory/?token='${TOKEN} -H 'Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7yqwdS1Jq8TWiUAE'  --data-binary $'------WebKitFormBoundary7yqwdS1Jq8TWiUAE\r\nContent-Disposition: form-data; name="factoryUrl"\r\n\r\n{\r\n  "v": "2.1",\r\n  "project": {\r\n    "name": "my-minimalistic-factory",\r\n    "description": "Minimalistic Template"\r\n  },\r\n  "source": {\r\n    "project": {\r\n      "location": "https://github.com/codenvy/sdk",\r\n      "type": "git"\r\n    }\r\n  }\r\n}\r\n------WebKitFormBoundary7yqwdS1Jq8TWiUAE--\r\n')
    EXIT_CODE=$?
    log ${OUTPUT}

    validateExitCode ${EXIT_CODE}

    logEndCommand "createDefaultFactory: OK"
}

validateExpectedString() {
    logStartCommand "validateRegex "$@

    [[ ${OUTPUT} =~ $1 ]] || validateExitCode 1

    logEndCommand "validateRegex: OK"
}

validateErrorString() {
    logStartCommand "validateErrorRegex "$@

    [[ ${OUTPUT} =~ $1 ]] && validateExitCode 1

    logEndCommand "validateRegex: OK"
}
