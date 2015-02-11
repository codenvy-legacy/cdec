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

#!/bin/bash
if [ -z "$1" ] || [ "$1" == "prod" ]; then
    SSH_KEY_NAME=$2
    SSH_AS_USER_NAME=codenvy
    AS_IP=update.codenvycorp.com
    echo "=========> Uploading on production"
elif [ "$1" == "stg" ]; then
    SSH_KEY_NAME=~/.ssh/as1-cldide_cl-server.skey
    SSH_AS_USER_NAME=codenvy
    AS_IP=updater.codenvy-stg.com
    echo "=========> Uploading on staging"
else
    echo "Unknown server destination"
    exit 1
fi

if [ ! -z "$2" ]; then
    SSH_KEY_NAME=$2
fi

uploadInstallationManagerCli() {
    FILE="installation-manager"
    ARTIFACT=${FILE}"-cli"

    FILENAME=`ls ${ARTIFACT}-assembly/target | grep -G ${FILE}-.*-binary[.]tar.gz`
    VERSION=`ls ${ARTIFACT}-assembly/target | grep -G ${FILE}-.*[.]jar | grep -vE 'sources|original' | sed 's/'${FILE}'-//' | sed 's/.jar//'`
    SOURCE=${ARTIFACT}-assembly/target/${FILENAME}

    doUpload
}

uploadCodenvyServerInstallScript() {
    ARTIFACT=install-codenvy
    FILENAME=install-codenvy.sh
    VERSION=$1
    SOURCE=installation-manager-resources/src/main/resources/${VERSION}/${FILENAME}
    doUpload

    if [ "${AS_IP}" == "updater.codenvy-stg.com" ]; then
        ssh -i ${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "sed -i 's/codenvy.com/codenvy-stg.com/g' ${DESTINATION}/${FILENAME}"
    fi
}

uploadCodenvySingleServerInstallProperties() {
    ARTIFACT=codenvy-single-server-properties
    FILENAME=codenvy-single-server.properties
    VERSION=$1
    SOURCE=installation-manager-resources/src/main/resources/${VERSION}/${FILENAME}
    doUpload
}

uploadCodenvyMultiServerInstallProperties() {
    ARTIFACT=codenvy-multi-server-properties
    FILENAME=codenvy-multi-server.properties
    VERSION=$1
    SOURCE=installation-manager-resources/src/main/resources/${VERSION}/${FILENAME}
    doUpload
}

doUpload() {
    DESTINATION=update-server-repository/${ARTIFACT}/${VERSION}

    MD5=`md5sum ${SOURCE} | cut -d ' ' -f 1`
    SIZE=`du -b ${SOURCE} | cut -f1`
    BUILD_TIME=`stat -c %y ${SOURCE}`
    BUILD_TIME=${BUILD_TIME:0:19}

    echo "file=${FILENAME}" > .properties
    echo "artifact=${ARTIFACT}" >> .properties
    echo "version=${VERSION}" >> .properties
    echo "authentication-required=false" >> .properties
    echo "build-time="${BUILD_TIME} >> .properties
    echo "md5=${MD5}" >> .properties
    echo "size=${SIZE}" >> .properties

    ssh -i ${SSH_KEY_NAME} ${SSH_AS_USER_NAME}@${AS_IP} "mkdir -p /home/${SSH_AS_USER_NAME}/${DESTINATION}"
    scp -o StrictHostKeyChecking=no -i ${SSH_KEY_NAME} ${SOURCE} ${SSH_AS_USER_NAME}@${AS_IP}:${DESTINATION}/${FILENAME}
    scp -o StrictHostKeyChecking=no -i ${SSH_KEY_NAME} .properties ${SSH_AS_USER_NAME}@${AS_IP}:${DESTINATION}/.properties

    rm .properties
}

uploadInstallationManagerCli

for VERSION in 3.1.0; do
    uploadCodenvyServerInstallScript ${VERSION}
done

for VERSION in 3.1.0 3.3.0 3.5.0; do
    uploadCodenvySingleServerInstallProperties ${VERSION}
done

for VERSION in 3.5.0; do
    uploadCodenvyMultiServerInstallProperties ${VERSION}
done
