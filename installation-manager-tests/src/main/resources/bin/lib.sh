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
    if [[ ! -z ${EXIT_CODE} ]] && [[ ! ${EXIT_CODE} == "0" ]]; then
	    printAndLog "RESULT: FAILED"
	    vagrantDestroy
        exit ${EXIT_CODE}
    fi
}

vagrantDestroy() {
    vagrant destroy -f >> ${TEST_LOG}
}

validateInstalledCodenvyVersion() {
    VERSION=$1

    log
    log "validation installed version "${VERSION}

    OUTPUT=$(curl -X OPTIONS http://codenvy.onprem/api/)
    if [[ ! ${OUTPUT} =~ .*\"ideVersion\"\:\"${VERSION}\".* ]]; then
        validateExitCode 1
    fi

    log "OK"
}

validateInstalledImCliClientVersion() {
    log ">>> validateInstalledImCliClientVersion()"

    VERSION=$1
    executeIMCommand "im-install" "--list"

    if [[ ! ${OUTPUT} =~ .*\"artifact\".*\:.*\"installation-manager-cli\".*\"version\".*\:.*\"${VERSION}\".*\"status\".*\:.*\"SUCCESS\".* ]]; then
        validateExitCode 1
    fi

    log "OK"
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
    VERSION=$1
    MULTI=$2

    log
    log "Codenvy installation "${VERSION}" "${MULTI}

    if [ ! -z ${MULTI} ]; then
        scp -o StrictHostKeyChecking=no -i ~/.vagrant.d/insecure_private_key -P 2222 ~/.vagrant.d/insecure_private_key vagrant@127.0.0.1:./.ssh/id_rsa >> ${TEST_LOG}
    fi

    if [ -z ${VERSION} ]; then
        ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVER}'/repository/public/download/install-codenvy) --silent '${MULTI} >> ${TEST_LOG}
    else
        ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVER}'/repository/public/download/install-codenvy) --silent --version='${VERSION}' '${MULTI} >> ${TEST_LOG}
    fi

    retrieveInstallLog
    validateExitCode $?

    log "OK"
}

installImCliClient() {
    log ">>> installImCliClient()"

    VERSION=$1
    if [ -z ${VERSION} ]; then
        ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVER}'/repository/public/download/install-im-cli)' >> ${TEST_LOG}
    else
        ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem 'export TERM="xterm" && bash <(curl -L -s '${UPDATE_SERVER}'/repository/public/download/install-im-cli) --version='${VERSION} >> ${TEST_LOG}
    fi
    validateExitCode $?

    log "OK"
}

vagrantUp() {
    VAGRANT_FILE=$1

    cp ${VAGRANT_FILE} Vagrantfile

    vagrant up >> ${TEST_LOG}
    validateExitCode $?
}

auth() {
    USERNAME=$1
    PASSWORD=$2

    log
    log "authentication "${USERNAME}" "${PASSWORD}

    OUTPUT=$(curl -s -X POST -H "Content-Type: application/json" -d '{"username":"'${USERNAME}'", "password":"'${PASSWORD}'", "realm":"sysldap"}' http://codenvy.onprem/api/auth/login)
    validateExitCode $?

    log ${OUTPUT}

    if [[ ! ${OUTPUT} =~ .*value.* ]]; then
        validateExitCode 1
    fi

    log "OK"
}

executeIMCommand() {
    log
    log "executing command "$@

    OUTPUT=$(ssh -i ~/.vagrant.d/insecure_private_key vagrant@codenvy.onprem "/home/vagrant/codenvy-im/codenvy-cli/bin/codenvy $@")
    log ${OUTPUT}
    validateExitCode $?

    log "OK"
}
