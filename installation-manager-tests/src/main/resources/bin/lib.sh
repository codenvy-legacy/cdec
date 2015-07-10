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

log() {
    echo "TEST: "$@ >> ${TEST_LOG}
}

validateExitCode() {
    EXIT_CODE=$1
    VALID_CODE=$2
    if [[ ! -z ${VALID_CODE} ]]; then
        if [[ ! ${EXIT_CODE} == ${VALID_CODE} ]];then
            printAndLog "RESULT: FAILED"
            vagrantDestroy
            exit 1
        fi
    else
        if [[ ! ${EXIT_CODE} == "0" ]];then
            printAndLog "RESULT: FAILED"
            vagrantDestroy
            exit 1
        fi
    fi
}

vagrantDestroy() {
    vagrant destroy -f >> ${TEST_LOG}
}

validateInstalledCodenvyVersion() {
    VERSION=$1

    [[ -z ${VERSION} ]] && VERSION=${LATEST_CODENVY_VERSION}
    log "validateInstalledCodenvyVersion "${VERSION}

    OUTPUT=$(curl -X OPTIONS http://codenvy.onprem/api/)
    [[ ! ${OUTPUT} =~ .*\"ideVersion\"\:\"${VERSION}\".* ]] && validateExitCode 1

    log "validateInstalledCodenvyVersion: OK"
}

validateInstalledImCliClientVersion() {
    VERSION=$1

    [[ -z ${VERSION} ]] && VERSION=${LATEST_IM_CLI_CLIENT_VERSION}

    log "validateInstalledImCliClientVersion "${VERSION}

    executeIMCommand "im-install" "--list"

    [[ ! ${OUTPUT} =~ .*\"artifact\".*\:.*\"installation-manager-cli\".*\"version\".*\:.*\"${VERSION}\".*\"status\".*\:.*\"SUCCESS\".* ]] &&  validateExitCode 1

    log "validateInstalledImCliClientVersion: OK"
}

retrieveInstallLog() {
    scp -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem:install.log tmp.log
    if [ -f "tmp.log" ]; then
        if [ -f "install.log" ]; then
            cat tmp.log >> install.log
        else
            cp tmp.log install.log
        fi
        rm tmp.log
    fi
}

installCodenvy() {
    MULTI_OPTION=""
    VERSION_OPTION=""
    INSTALL_ON_NODE=$(detectMasterNode)

    if [[ ${INSTALL_ON_NODE} == "master.codenvy.onprem" ]]; then
        scp -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key -P 2222 ~/.vagrant.d/insecure_private_key vagrant@127.0.0.1:./.ssh/id_rsa >> ${TEST_LOG}
        MULTI_OPTION="--multi"
    fi

    VERSION=$1
    [[ ! -z ${VERSION} ]] && VERSION_OPTION="--version="${VERSION}

    log "installCodenvy "$@

    ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${INSTALL_ON_NODE} 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVICE}'/repository/public/download/install-codenvy) --silent '${MULTI_OPTION}' '${VERSION_OPTION} >> ${TEST_LOG}
    EXIT_CODE=$?
    retrieveInstallLog
    validateExitCode ${EXIT_CODE}

    log "installCodenvy: OK"
}

installImCliClient() {
    log "installImCliClient "$@

    VERSION=$1
    VERSION_OPTION=""
    [[ ! -z ${VERSION} ]] && VERSION_OPTION="--version="${VERSION}

    ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVICE}'/repository/public/download/install-im-cli) '${VERSION_OPTION} >> ${TEST_LOG}
    validateExitCode $?

    log "installImCliClient: OK"
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
    log "auth "$@

    USERNAME=$1
    PASSWORD=$2
    REALM=$3
    SERVER_DNS=$4

    [[ -z ${SERVER_DNS} ]] && SERVER_DNS="codenvy.onprem"

    OUTPUT=$(curl -s -X POST -H "Content-Type: application/json" -d '{"username":"'${USERNAME}'", "password":"'${PASSWORD}'", "realm":"'${REALM}'"}' http://${SERVER_DNS}/api/auth/login)
    EXIT_CODE=$?

    log ${OUTPUT}
    validateExitCode $?

    [[ ! ${OUTPUT} =~ .*value.* ]] && validateExitCode 1
    TOKEN=$(fetchJsonParameter "value")

    log "auth: OK"
}


executeIMCommand() {
    log "executeIMCommand "$@

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

    log "executeIMCommand: OK"
}

executeSshCommand() {
    log "executeSshCommand "$@

    COMMAND=$1

    EXECUTE_ON_NODE=$2
    [[ -z ${EXECUTE_ON_NODE} ]] && EXECUTE_ON_NODE=$(detectMasterNode)

    OUTPUT=$(ssh -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key vagrant@${EXECUTE_ON_NODE} "${COMMAND}")
    EXIT_CODE=$?

    log ${OUTPUT}
    validateExitCode ${EXIT_CODE} 0

    log "executeSshCommand: OK"
}

detectMasterNode() {
    ping -c1 -q "master.codenvy.onprem" >> ${TEST_LOG}
    if [[ $? == 0 ]]; then
        echo "master.codenvy.onprem"
    else
        ping -c1 -q "codenvy.onprem" >> ${TEST_LOG}
        if [[ $? == 0 ]]; then
            echo "codenvy.onprem"
        else
            validateExitCode 1
        fi
    fi
}

fetchJsonParameter() {
    if [[ ! ${OUTPUT} =~ .*"$1".* ]]; then
        validateExitCode 1
    fi
    echo `echo ${OUTPUT} | sed 's/.*"'$1'"\W*:\W*"\([^"]*\)*".*/\1/'`
}

doPost() {
    log "POST "$@

    CONTENT_TYPE=$1
    BODY=$2
    URL=$3

    OUTPUT=$(curl -H "Content-Type: ${CONTENT_TYPE}" -d ${BODY} -X POST ${URL})
    log ${OUTPUT}

    log "curl: OK"
}

doGet() {
    log "GET "$@

    URL=$1

    OUTPUT=$(curl -X GET ${URL})
    log ${OUTPUT}

    log "curl: OK"
}

