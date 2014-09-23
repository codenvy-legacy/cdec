#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2014] Codenvy, S.A.
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

#!/bin/bash
if [ -z "$1" ] || [ "$1" == "prod" ]; then
    SSH_KEY_NAME=cl-server-prod-20130219
    SSH_AS_USER_NAME=codenvy
    AS_IP=update.codenvycorp.com
    echo "Uploading on production"
elif [ "$1" == "stg" ]; then
    SSH_KEY_NAME=as1-cldide_cl-server.skey
    SSH_AS_USER_NAME=codenvy
    AS_IP=syslog.codenvy-stg.com
    echo "Uploading on staging"
else
    echo "Unknown server destination"
    exit
fi

makeBundle() {
    IM=installation-manager
    IM_CLI=installation-manager-cli

    IM_FILENAME=`ls ${IM}/target | grep -G ${IM}-.*-binary[.]tar.gz`
    IM_SOURCE=${IM}/target/${IM_FILENAME}

    IM_CLI_FILENAME=`ls ${IM_CLI}/target | grep -G ${IM_CLI}-.*-binary[.]tar.gz`
    IM_CLI_SOURCE=${IM_CLI}/target/${IM_CLI_FILENAME}

    BUNDLE_DIR=${IM}/target/im_and_im_cli
    rm -rf ${BUNDLE_DIR}
    mkdir ${BUNDLE_DIR}

    cp ${IM_SOURCE} ${BUNDLE_DIR}
    cp ${IM_CLI_SOURCE} ${BUNDLE_DIR}

    pushd ${BUNDLE_DIR}
    tar -zcf ../${IM_FILENAME} *
    popd

    rm -rf ${BUNDLE_DIR}
}

uploadArtifact() {
    ARTIFACT=$1

    FILENAME=`ls ${ARTIFACT}/target | grep -G ${ARTIFACT}-.*-binary[.]tar.gz`
    VERSION=`ls ${ARTIFACT}/target | grep -G ${ARTIFACT}-.*[.]jar | grep -vE 'sources|original' | sed 's/'${ARTIFACT}'-//' | sed 's/.jar//'`
    SOURCE=${ARTIFACT}/target/${FILENAME}

    doUpload
}

uploadInstallScript() {
    ARTIFACT=install-script
    FILENAME=install-script.sh
    SOURCE=installation-manager/bin/${FILENAME}

    doUpload

    if [ "${AS_IP}" == "syslog.codenvy-stg.com" ]; then
        ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "sed -i 's/codenvy.com/codenvy-stg.com/g' ${DESTINATION}/${FILENAME}"
    fi
}

doUpload() {
    DESTINATION=update-server-repository/${ARTIFACT}/${VERSION}

    echo "file=${FILENAME}" > .properties
    echo "artifact=${ARTIFACT}" >> .properties
    echo "version=${VERSION}" >> .properties
    echo "authentication-required=false" >> .properties
    echo "builtime="`stat -c %y ${SOURCE}` >> .properties
    ssh -i ~/.ssh/${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "mkdir -p /home/${SSH_AS_USER_NAME}/${DESTINATION}"
    scp -o StrictHostKeyChecking=no -i ~/.ssh/${SSH_KEY_NAME} ${SOURCE} ${SSH_AS_USER_NAME}@${AS_IP}:${DESTINATION}/${FILENAME}
    scp -o StrictHostKeyChecking=no -i ~/.ssh/${SSH_KEY_NAME} .properties ${SSH_AS_USER_NAME}@${AS_IP}:${DESTINATION}/.properties

    rm .properties
}

makeBundle
uploadArtifact installation-manager
uploadArtifact installation-manager-cli
uploadInstallScript

