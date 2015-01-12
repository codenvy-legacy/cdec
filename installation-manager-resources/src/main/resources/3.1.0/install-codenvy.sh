#!/bin/bash

# bash <(curl -s https://start.codenvy.com/install-single)

ARTIFACT="cdec"
VERSION=$1
CONFIG="codenvy-single-server.properties"
DIR="${HOME}/codenvy-im"

checkOS() {
    if [ -f /etc/redhat-release ]; then
        OS="Red Hat"
    else
        printPrompt; echo "Operation system isn't supported."
        exit
    fi
}

# $1 - command name
installPackageIfNeed() {
    command -v $1 >/dev/null 2>&1 || { # check if requered command had been already installed earlier
        printPrompt; echo "Installing $1 "
        sudo yum install $1 -y -q
    }
}

installJava() {
    printPrompt; echo "Installing java"
    wget -q --no-cookies --no-check-certificate --header 'Cookie: oraclelicense=accept-securebackup-cookie' 'http://download.oracle.com/otn-pub/java/jdk/7u17-b02/jre-7u17-linux-x64.tar.gz' --output-document=jre.tar.gz

    tar -xf jre.tar.gz -C ${DIR}
    mv ${DIR}/jre1.7.0_17 ${DIR}/jre

    rm jre.tar.gz
}

installIm() {
    printPrompt; echo "Downloading Installation Manager"

    IM_URL="https://codenvy.com/update/repository/public/download/installation-manager-cli"
    IM_FILE=$(curl -sI  ${IM_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')

    curl -s -o ${IM_FILE} -L ${IM_URL}

    mkdir ${DIR}/codenvy-cli
    tar -xf ${IM_FILE} -C ${DIR}/codenvy-cli

    sed -i "2iJAVA_HOME=${HOME}/codenvy-im/jre" ${DIR}/codenvy-cli/bin/codenvy
    sed -i "2iJAVA_HOME=${HOME}/codenvy-im/jre" ${DIR}/codenvy-cli/bin/interactive-mode
}

printPrompt() {
    echo -en "\e[94m[CODENVY]\e[0m "
}

askProperty() {
    PROMPT=$1
    PROPERTY=$2

    printPrompt; echo -n "${PROMPT}: "
    read VALUE
    insertProperty ${PROPERTY} ${VALUE}
}

insertProperty() {
    sed -i s/$1=.*/$1=$2/g ${CONFIG}
}

askPassword() {
    printPrompt
    PROMPT="Codenvy password: "

    unset PASSWORD
    while IFS= read -p "$PROMPT" -r -s -n 1 CHAR
    do
        if [[ ${CHAR} == $'\0' ]]
        then
            break
        fi
        PROMPT='*'
        PASSWORD+="${CHAR}"
    done
    echo

    insertProperty "codenvy_password" ${PASSWORD}
}

askDNS() {
    printPrompt; echo -n "Please set the DNS hostname to be used by Codenvy: "
    read DNS
    insertProperty "aio_host_url" ${DNS}
    insertProperty "host_url" ${DNS}
    if ! sudo grep -Eq "127.0.0.1.*puppet" /etc/hosts; then
        echo '127.0.0.1 puppet' | sudo tee --append /etc/hosts > /dev/null
    fi
    if ! sudo grep -Fq "${DNS}" /etc/hosts; then
        echo "127.0.0.1 ${DNS}" | sudo tee --append /etc/hosts > /dev/null
    fi
}

prepareConfig() {
    if [ ! -f ${CONFIG} ]; then
        curl -s -o ${CONFIG} https://codenvy.com/update/repository/public/download/codenvy-single-server-properties/${VERSION}
    fi

    askProperty "Codenvy user name" "codenvy_user_name"
    askPassword
    askDNS
}

executeCliCommand() {
    if [ ! -z "$1" ]; then printPrompt; echo "$1"; fi
    ${DIR}/codenvy-cli/bin/codenvy $2 $3 $4 $5 $6 $7 $8

    RETVAL=$?
    [ ${RETVAL} -ne 0 ] && exit ${RETVAL}
}

executeIMCommand() {
    if [ ! -z "$1" ]; then printPrompt; echo "$1"; fi
    ${DIR}/codenvy-cli/bin/codenvy $2 $3 $4 $5 $6 $7 $8

    RETVAL=$?
    [ ${RETVAL} -ne 0 ] && exit ${RETVAL}
}

preconfigureSystem() {
    installPackageIfNeed curl
}

printPreInstallInfo() {
    checkOS

    availableRAM=`sudo cat /proc/meminfo | grep MemTotal | awk '{print $2}'`
    availableRAM=$(perl -E "say sprintf('%.1f',${availableRAM}/1024/1024)")

    availableDiskSpace=`sudo df -h ${HOME} | tail -1 | awk '{print $2}'`
    availableCores=`grep -c ^processor /proc/cpuinfo`

    printPrompt; echo "Welcome. This program installs Codenvy."
#    printPrompt; echo "When the installation is complete, the Codenvy URL will be displayed."
    printPrompt; echo
    printPrompt; echo "This program will:"
    printPrompt; echo "1. Configure the system"
    printPrompt; echo "2. Install Java and other required packages"
    printPrompt; echo "3. Install the Codenvy Installation Manager"
    printPrompt; echo "4. Download Codenvy"
    printPrompt; echo "5. Install Codenvy by installing Puppet and configuring system parameters"
    printPrompt; echo "6. Boot Codenvy"
    printPrompt; echo
    printPrompt; echo
    printPrompt; echo "By continuing, you accept the Codenvy Agreement @ http://codenvy.com/legal"
    printPrompt; echo
    printPrompt; echo "Press any key to continue"
    read -n1 -s
    clear

    printPrompt; echo "Checking for system pre-requisites..."
    printPrompt; echo "We have detected that this node is a ${OS} distribution."
    printPrompt; echo
    printPrompt; echo "RESOURCE      : MINIMUM : AVAILABLE"
    printPrompt; echo "RAM           : 8GB     : ${availableRAM}GB"
    printPrompt; echo "CPU           : 4 cores : ${availableCores} cores"
    printPrompt; echo "Disk Space    : 300GB   : ${availableDiskSpace}B"
    printPrompt; echo
    printPrompt; echo "Sizing Guide: http://docs.codenvy.com/onpremises"
    printPrompt; echo
    printPrompt; echo "Press any key to continue"
    read -n1 -s
    clear

    if [ ! -f ${CONFIG} ]; then
        printPrompt; echo "Configuration file : not detected - will download template"
        printPrompt; echo
        printPrompt; echo "Codenvy user name    : undetected - will prompt for entry"
        printPrompt; echo "Codenvy password     : undetected - will prompt for entry"
        printPrompt; echo "Codenvy DNS hostname : not set - will prompt for entry"
        printPrompt; echo
        printPrompt; echo "Create account or retrieve password: https://codenvy.com/site/create-account"
        printPrompt; echo
        printPrompt; echo "Press any key to continue"
        printPrompt; echo
        read -n1 -s
        prepareConfig
        printPrompt; echo
    else
        HOSTNAME=`grep host[_url]*=.* ${CONFIG} | cut -f2 -d '='`
        CODENVY_USER=`grep codenvy_user_name= ${CONFIG} | cut -d '=' -f2`
        CODENVY_PWD=`grep codenvy_password ${CONFIG} | cut -d '=' -f2`

        printPrompt; echo "Configuration file : "${CONFIG}
        printPrompt; echo
        printPrompt; echo "Codenvy user name    : "${CODENVY_USER}
        printPrompt; echo "Codenvy password     : ******"
        printPrompt; echo "Codenvy DNS hostname : "${HOSTNAME}
        printPrompt; echo
        printPrompt; echo "Create account or retrieve password: https://codenvy.com/site/create-account"
        printPrompt; echo
    fi

    printPrompt; echo -n "Continue installation [y/N]: "
    read ANSWER
    if [ ! "${ANSWER}" == "y" ]; then exit 1; fi
}

printPostInstallInfo() {
    printPrompt; echo
    HOSTNAME=`grep host[_url]*=.* ${CONFIG} | cut -f2 -d '='`
    printPrompt; echo "Codenvy is ready at http://"${HOSTNAME}"/"
    printPrompt; echo
    printPrompt; echo "Troubleshoot Installation Problems: http://docs.codenvy.com/onpremises/installation-single-node/#install-troubleshooting"
    printPrompt; echo "Upgrade & Configuration Docs: http://docs.codenvy.com/onpremises/installation-single-node/#upgrades"
}

doInstallStep1() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 1: CONFIGURE SYSTEM"

    if [ -d ${DIR} ]; then rm -rf ${DIR}; fi
    mkdir ${DIR}

    printPrompt; echo "COMPLETED STEP 1: CONFIGURE SYSTEM"
}

doInstallStep2() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 2: INSTALL JAVA AND OTHER REQUIRED PACKAGES"
    installPackageIfNeed tar
    installPackageIfNeed wget
    installPackageIfNeed unzip
    installJava
    printPrompt; echo "COMPLETED STEP 2: INSTALL JAVA AND OTHER REQUIRED PACKAGES"
}

doInstallStep3() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 3: INSTALL THE CODENVY INSTALLATION MANAGER"
    installIm
    printPrompt; echo "Codenvy Installation Manager is installed into ${DIR}/codenvy-cli directory"
    printPrompt; echo "COMPLETED STEP 3: INSTALL THE CODENVY INSTALLATION MANAGER"
}

doInstallStep4() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 4: DOWNLOAD CODENVY"

    CODENVY_USER=`grep codenvy_user_name= ${CONFIG} | cut -d '=' -f2`
    CODENVY_PWD=`grep codenvy_password ${CONFIG} | cut -d '=' -f2`

    executeCliCommand "Login to Codenvy Updater service" login --remote update-server ${CODENVY_USER} ${CODENVY_PWD}
    executeCliCommand "Downloading Codenvy binaries" im-download ${ARTIFACT} ${VERSION}
    executeCliCommand "Checking the list of downloaded binaries" im-download --list-local
    printPrompt; echo "COMPLETED STEP 4: DOWNLOAD CODENVY"
}

doInstallStep5() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 5: INSTALL CODENVY BY INSTALLING PUPPET AND CONFIGURING SYSTEM PARAMETERS"
    executeIMCommand "Installing the latest Codenvy version. Watch progress in /var/log/messages" im-install --step 1-8 --config ${CONFIG} ${ARTIFACT} ${VERSION}
    printPrompt; echo "COMPLETED STEP 5: INSTALL CODENVY BY INSTALLING PUPPET AND CONFIGURING SYSTEM PARAMETERS"
}

doInstallStep6() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 6: BOOT CODENVY"
    executeIMCommand "" im-install --step 9 --config ${CONFIG} ${ARTIFACT} ${VERSION}
    printPrompt; echo "COMPLETED STEP 6: BOOT CODENVY"
}

clear
preconfigureSystem

printPreInstallInfo

doInstallStep1
doInstallStep2
doInstallStep3
doInstallStep4
doInstallStep5
doInstallStep6

printPostInstallInfo
