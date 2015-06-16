#!/bin/bash

# bash <(curl -L -s https://start.codenvy.com/install-single)
#
# allowed options:
# --multi
# --silent
# --version=<VERSION TO INSTALL>
# --hostname=<CODENVY HOSTNAME>
# --systemAdminName=<SYSTEM ADMIN NAME>
# --systemAdminPassword=<SYSTEM ADMIN PASSWORD>
set -e

unset HOST_NAME
unset SYSTEM_ADMIN_NAME
unset SYSTEM_ADMIN_PASSWORD

setRunOptions() {
    DIR="${HOME}/codenvy-im"
    ARTIFACT="codenvy"
    CODENVY_TYPE="single"
    SILENT=false
    VERSION=`curl -s https://codenvy.com/update/repository/properties/${ARTIFACT} | sed 's/.*"version":"\([^"].*\)".*/\1/'`
    for var in "$@"; do
        if [[ "$var" == "--multi" ]]; then
            CODENVY_TYPE="multi"
        elif [[ "$var" == "--silent" ]]; then
            SILENT=true
        elif [[ "$var" =~ --version=.* ]]; then
            VERSION=`echo "$var" | sed -e "s/--version=//g"`
        elif [[ "$var" =~ --hostname=.* ]]; then
            HOST_NAME=`echo "$var" | sed -e "s/--hostname=//g"`
        elif [[ "$var" =~ --systemAdminName=.* ]]; then
            SYSTEM_ADMIN_NAME=`echo "$var" | sed -e "s/--systemAdminName=//g"`
        elif [[ "$var" =~ --systemAdminPassword=.* ]]; then
            SYSTEM_ADMIN_PASSWORD=`echo "$var" | sed -e "s/--systemAdminPassword=//g"`
        fi
    done
    CONFIG="codenvy-${CODENVY_TYPE}-server.properties"
}

downloadConfig() {
    curl -s -o ${CONFIG} https://codenvy.com/update/repository/public/download/codenvy-${CODENVY_TYPE}-server-properties/${VERSION}
}

validateOS() {
    if [ -f /etc/redhat-release ]; then
        OS="Red Hat"
    else
        printLn  "Operation system isn't supported."
        exit 1
    fi
    OS_VERSION=`cat /etc/redhat-release | sed 's/.* \([0-9.]*\) .*/\1/' | cut -f1 -d '.'`

    if [ "${VERSION}" == "3.1.0" ] && [ "${OS_VERSION}" != "6" ]; then
        printLn "Codenvy 3.1.0 can be installed onto CentOS 6.x only"
        exit 1
    fi

    if [ "${CODENVY_TYPE}" == "multi" ] && [ "${OS_VERSION}" != "7" ]; then
        printLn "Codenvy multi-node can be installed onto CentOS 7.x only"
        exit 1
    fi
}

# $1 - command name
installPackageIfNeed() {
    command -v $1 >/dev/null 2>&1 || { # check if requered command had been already installed earlier
        printLn "Installing $1 "
        sudo yum install $1 -y -q
    }
}


preconfigureSystem() {
    sudo yum clean all &> /dev/null
    installPackageIfNeed curl

    validateOS
    setRunOptions

    if [[ ! -f ${CONFIG} ]]; then
        downloadConfig
    fi
}

installJava() {
    printLn "Installing java"
    wget -q --no-cookies --no-check-certificate --header 'Cookie: oraclelicense=accept-securebackup-cookie' 'http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jre-8u45-linux-x64.tar.gz' --output-document=jre.tar.gz

    tar -xf jre.tar.gz -C ${DIR}
    mv ${DIR}/jre1.8.0_45 ${DIR}/jre

    rm jre.tar.gz
}

installIm() {
    printLn "Downloading Installation Manager"

    IM_URL="https://codenvy.com/update/repository/public/download/installation-manager-cli"
    IM_FILE=$(curl -sI  ${IM_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')

    curl -s -o ${IM_FILE} -L ${IM_URL}

    mkdir ${DIR}/codenvy-cli
    tar -xf ${IM_FILE} -C ${DIR}/codenvy-cli

    sed -i "2iJAVA_HOME=${HOME}/codenvy-im/jre" ${DIR}/codenvy-cli/bin/codenvy
}

printPrompt() {
    echo -en "\e[94m[CODENVY]\e[0m "
}

print() {
    printPrompt; echo -n "$1"
}

printLn() {
    printPrompt; echo "$1"
}

askProperty() {
    PROMPT=$1

    print "${PROMPT}: "
    read VALUE

    echo ${VALUE}
}

insertProperty() {
    sed -i s/$1=.*/$1=$2/g ${CONFIG}
}

updateHostsFile() {
    [ -z ${HOST_NAME} ] && HOST_NAME=`grep host[_url]*=.* ${CONFIG} | cut -f2 -d '='`

    if ! sudo grep -Eq "127.0.0.1.*puppet" /etc/hosts; then
        echo '127.0.0.1 puppet' | sudo tee --append /etc/hosts > /dev/null
    fi
    if ! sudo grep -Fq "${HOST_NAME}" /etc/hosts; then
        echo "127.0.0.1 ${HOST_NAME}" | sudo tee --append /etc/hosts > /dev/null
    fi
}

askAndInsertProperty() {
    PROMPT=$1
    VARIABLE=$2
    
    print "${PROMPT}: "
    read VALUE

    insertProperty "${VARIABLE}" ${VALUE}
}

executeIMCommand() {
    ${DIR}/codenvy-cli/bin/codenvy $1 $2 $3 $4 $5 $6 $7 $8 $9
}

pressAnyKeyToContinueAndClearConsole() {
    if [[ ${SILENT} == false ]]; then
        printLn  "Press any key to continue"
        read -n1 -s
        clear
    fi
}

pressAnyKeyToContinue() {
    if [[ ${SILENT} == false ]]; then
        printLn  "Press any key to continue"
        read -n1 -s
    fi
}

pressYKeyToContinue() {
    if [[ ${SILENT} == false ]]; then
        print  "Continue installation [y/N]: "
        read ANSWER
        if [ ! "${ANSWER}" == "y" ]; then exit 1; fi
    fi
}

printPreInstallInfo_single() {
    availableRAM=`cat /proc/meminfo | grep MemTotal | awk '{tmp = $2/1024/1024; printf"%0.1f",tmp}'`
    availableDiskSpace=`sudo df -h ${HOME} | tail -1 | awk '{print $2}'`
    availableCores=`grep -c ^processor /proc/cpuinfo`

    printLn "Welcome. This program installs a single node version of Codenvy On-Prem."
    printLn ""
    printLn "This program will:"
    printLn "1. Configure the system"
    printLn "2. Install Java and other required packages"
    printLn "3. Install the Codenvy Installation Manager"
    printLn "4. Download Codenvy"
    printLn "5. Install Codenvy by installing Puppet and configuring system parameters"
    printLn "6. Boot Codenvy"
    printLn ""
    printLn ""
    pressAnyKeyToContinueAndClearConsole

    printLn "Checking for system pre-requisites..."
    printLn "We have detected that this node is a ${OS} distribution."
    printLn ""
    printLn "RESOURCE      : MINIMUM : AVAILABLE"
    printLn "RAM           : 8GB     : ${availableRAM}GB"
    printLn "CPU           : 4 cores : ${availableCores} cores"
    printLn "Disk Space    : 300GB   : ${availableDiskSpace}B"
    printLn ""
    printLn "Sizing Guide: http://docs.codenvy.com/onpremises"
    printLn ""
    pressAnyKeyToContinueAndClearConsole

    printLn "Configuration File: "${CONFIG}
    printLn ""

    if [[ ${SILENT} == true ]]; then
        [ ! -z ${SYSTEM_ADMIN_NAME} ] && insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
        [ ! -z ${SYSTEM_ADMIN_PASSWORD} ] && insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
        [ ! -z ${HOST_NAME} ] && insertProperty "host_url" ${HOST_NAME}
    else
        [ -z ${SYSTEM_ADMIN_NAME} ] && printLn "System admin user name : will prompt for entry"
        [ -z ${SYSTEM_ADMIN_PASSWORD} ] && printLn "System admin password  : will prompt for entry"
        [ -z ${HOST_NAME} ] && printLn "Codenvy DNS hostname   : will prompt for entry"

        printLn ""
        pressAnyKeyToContinue

        [ -z ${SYSTEM_ADMIN_NAME} ] && SYSTEM_ADMIN_NAME=$(askProperty "System admin user name")
        [ -z ${SYSTEM_ADMIN_PASSWORD} ] && SYSTEM_ADMIN_PASSWORD=$(askProperty "System admin password (might be changed after installation)")
        [ -z ${HOST_NAME} ] && HOST_NAME=$(askProperty "Codenvy DNS hostname")

        insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
        insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
        insertProperty "host_url" ${HOST_NAME}

        printLn ""
    fi

    updateHostsFile

    pressYKeyToContinue
}

printPreInstallInfo_multi() {
    availableRAM=`cat /proc/meminfo | grep MemTotal | awk '{tmp = $2/1024/1024; printf"%0.1f",tmp}'`
    availableDiskSpace=`sudo df -h ${HOME} | tail -1 | awk '{print $2}'`
    availableCores=`grep -c ^processor /proc/cpuinfo`

    printLn "Welcome. This program installs a multi-node version of Codenvy On-Prem."
    printLn ""
    printLn "This program will:"
    printLn "1. Configure the system"
    printLn "2. Install Java and other required packages"
    printLn "3. Install the Codenvy Installation Manager"
    printLn "4. Download Codenvy"
    printLn "5. Install Codenvy by installing Puppet and configuring system parameters"
    printLn "6. Boot Codenvy"
    printLn ""
    printLn ""
    pressAnyKeyToContinueAndClearConsole

    printLn "Checking for system pre-requisites..."
    printLn "We have detected that this node is a ${OS} distribution."
    printLn ""
    printLn "Minimum requirements for the nodes:"
    printLn "RAM         : 1GB"
    printLn "Disk Space  : 14GB"
    printLn "OS          : CentOS 7"
    printLn ""
    printLn "Minimum requirements for the Runner node:"
    printLn "RAM         : 1.5GB"
    printLn "Disk Space  : 50GB"
    printLn "OS          : CentOS 7"
    printLn ""
    printLn "Sizing Guide: http://docs.codenvy.com/onpremises"
    printLn ""
    pressAnyKeyToContinueAndClearConsole

    printLn "Configuration File: "${CONFIG}
    printLn ""

    if [[ ${SILENT} == true ]]; then
        [ ! -z ${SYSTEM_ADMIN_NAME} ] && insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
        [ ! -z ${SYSTEM_ADMIN_PASSWORD} ] && insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
        [ ! -z ${HOST_NAME} ] && insertProperty "host_url" ${HOST_NAME}

        HOST_NAME=`grep host[_url]*=.* ${CONFIG} | cut -f2 -d '='`
        PUPPET_MASTER_HOST_NAME=`grep puppet_master_host_name=.* ${CONFIG} | cut -f2 -d '='`
        DATA_HOST_NAME=`grep data_host_name=.* ${CONFIG} | cut -f2 -d '='`
        API_HOST_NAME=`grep api_host_name=.* ${CONFIG} | cut -f2 -d '='`
        BUILDER_HOST_NAME=`grep builder_host_name=.* ${CONFIG} | cut -f2 -d '='`
        RUNNER_HOST_NAME=`grep runner_host_name=.* ${CONFIG} | cut -f2 -d '='`
        DATASOURCE_HOST_NAME=`grep datasource_host_name=.* ${CONFIG} | cut -f2 -d '='`
        ANALYTICS_HOST_NAME=`grep analytics_host_name=.* ${CONFIG} | cut -f2 -d '='`
        SITE_HOST_NAME=`grep site_host_name=.* ${CONFIG} | cut -f2 -d '='`

        printLn "Codenvy DNS hostname                    : "${HOST_NAME}
        printLn "Codenvy Puppet Master node DNS hostname : "${PUPPET_MASTER_HOST_NAME}
        printLn "Codenvy Data node DNS hostname          : "${DATA_HOST_NAME}
        printLn "Codenvy API node DNS hostname           : "${API_HOST_NAME}
        printLn "Codenvy Builder node DNS hostname       : "${BUILDER_HOST_NAME}
        printLn "Codenvy Runner node DNS hostname        : "${RUNNER_HOST_NAME}
        printLn "Codenvy Datasource node DNS hostname    : "${DATASOURCE_HOST_NAME}
        printLn "Codenvy Analytics node DNS hostname     : "${ANALYTICS_HOST_NAME}
        printLn "Codenvy Site node DNS hostname          : "${SITE_HOST_NAME}
        printLn ""

    else
        [ -z ${SYSTEM_ADMIN_NAME} ] && printLn "System admin user name : will prompt for entry"
        [ -z ${SYSTEM_ADMIN_PASSWORD} ] && printLn "System admin password  : will prompt for entry"
        printLn "Codenvy nodes' DNS hostnames : will prompt for entry"

        printLn ""
        pressAnyKeyToContinue

        [ -z ${SYSTEM_ADMIN_NAME} ] && SYSTEM_ADMIN_NAME=$(askProperty "System admin user name")
        [ -z ${SYSTEM_ADMIN_PASSWORD} ] && SYSTEM_ADMIN_PASSWORD=$(askProperty "System admin password (might be changed after installation)")

        insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
        insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}

        askAndInsertProperty "Please set the DNS hostname to be used by Codenvy" "host_url"
        askAndInsertProperty "Please set the DNS hostname of the Puppet Master node" "puppet_master_host_name"
        askAndInsertProperty "Please set the DNS hostname of the Data node" "data_host_name"
        askAndInsertProperty "Please set the DNS hostname of the API node" "api_host_name"
        askAndInsertProperty "Please set the DNS hostname of the Builder node" "builder_host_name"
        askAndInsertProperty "Please set the DNS hostname of the Runner node" "runner_host_name"
        askAndInsertProperty "Please set the DNS hostname of the Datasource node" "datasource_host_name"
        askAndInsertProperty "Please set the DNS hostname of the Analytics node" "analytics_host_name"
        askAndInsertProperty "Please set the DNS hostname of the Site node" "site_host_name"

        printLn ""
    fi

    pressYKeyToContinue
}

doInstallStep1() {
    printLn ""
    printLn "STEP 1: CONFIGURE SYSTEM"

    if [ -d ${DIR} ]; then rm -rf ${DIR}; fi
    mkdir ${DIR}
}

doInstallStep2() {
    printLn ""
    printLn "STEP 2: INSTALL JAVA AND OTHER REQUIRED PACKAGES"

    installPackageIfNeed tar
    installPackageIfNeed wget
    installPackageIfNeed unzip
    installJava
}

doInstallStep3() {
    printLn ""
    printLn "STEP 3: INSTALL THE CODENVY INSTALLATION MANAGER"

    installIm

    printLn "Codenvy Installation Manager is installed into ${DIR}/codenvy-cli directory"
}

doInstallStep4() {
    printLn ""
    printLn "STEP 4: DOWNLOAD CODENVY"

    printLn "Downloading Codenvy binaries"
    executeIMCommand im-download ${ARTIFACT} ${VERSION}

    printLn "Checking the list of downloaded binaries"
    executeIMCommand im-download --list-local
}

doInstallStep5_single() {
    printLn ""
    printLn "BEGINNING STEP 5: INSTALL CODENVY"
    printLn "Installing the latest Codenvy version. Watch progress in /var/log/messages"
    executeIMCommand im-install --step 1-8 --force --config ${CONFIG} ${ARTIFACT} ${VERSION}
}

doInstallStep6_single() {
    printLn ""
    printLn "STEP 6: BOOT CODENVY"
    executeIMCommand im-install --step 9 --force --config ${CONFIG} ${ARTIFACT} ${VERSION}
}

doInstallStep5_multi() {
    printLn ""
    printLn "STEP 5: INSTALL CODENVY ON MULTIPLE NODES"
    printLn "Installing the latest Codenvy version. Watch progress in /var/log/messages on each node";
    executeIMCommand im-install --step 1-8 --force --multi --config ${CONFIG} ${ARTIFACT} ${VERSION}
}

doInstallStep6_multi() {
    printLn ""
    printLn "STEP 6: BOOT CODENVY ON MULTIPLE NODES"
    executeIMCommand im-install --step 9 --force --multi --config ${CONFIG} ${ARTIFACT} ${VERSION}
}

printPostInstallInfo() {
    [ -z ${SYSTEM_ADMIN_NAME} ] && SYSTEM_ADMIN_NAME=`grep admin_ldap_user_name= ${CONFIG} | cut -d '=' -f2`
    [ -z ${SYSTEM_ADMIN_PASSWORD} ] && SYSTEM_ADMIN_PASSWORD=`grep system_ldap_password= ${CONFIG} | cut -d '=' -f2`
    [ -z ${HOST_NAME} ] && HOST_NAME=`grep host[_url]*=.* ${CONFIG} | cut -f2 -d '='`

    printLn ""
    printLn "Codenvy is ready at http://"${HOST_NAME}
    printLn ""
    printLn "Administrator dashboard ready a http://"${HOST_NAME}"/admin"
    printLn "System admin user name : "${SYSTEM_ADMIN_NAME}
    printLn "System admin password  : "${SYSTEM_ADMIN_NAME}
    printLn ""
    printLn "Installation & Troubleshooting Docs: http://docs.codenvy.com/onpremises/installation-${CODENVY_TYPE}-node/#install-troubleshooting"
    printLn "Upgrade & Configuration Docs: http://docs.codenvy.com/onpremises/installation-${CODENVY_TYPE}-node/#upgrades"
}

clear
preconfigureSystem

printPreInstallInfo_${CODENVY_TYPE}

doInstallStep1
doInstallStep2
doInstallStep3
doInstallStep4
doInstallStep5_${CODENVY_TYPE}
doInstallStep6_${CODENVY_TYPE}

printPostInstallInfo
