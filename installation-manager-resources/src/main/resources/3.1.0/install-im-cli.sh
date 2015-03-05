#!/bin/bash

# bash <(curl -L -s https://codenvy.com/update/repository/public/download/install-im-cli)
set -e

DIR="${HOME}/codenvy-im"

checkOS() {
    if [ -f /etc/redhat-release ]; then
        OS="Red Hat"
    else
        printPrompt; echo "Operation system isn't supported."
        exit
    fi
    OS_VERSION=`cat /etc/redhat-release | sed 's/.* \([0-9.]*\) .*/\1/' | cut -f1 -d '.'`
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

printPreInstallInfo() {
    checkOS

    printPrompt; echo "Welcome. This program installs Codenvy Installation Manager."
    printPrompt; echo
    printPrompt; echo "This program will:"
    printPrompt; echo "1. Configure the system"
    printPrompt; echo "2. Install Java and other required packages"
    printPrompt; echo "3. Install the Codenvy Installation Manager"
    printPrompt; echo
    printPrompt; echo
    printPrompt; echo "By continuing, you accept the Codenvy Agreement @ http://codenvy.com/legal"
    printPrompt; echo
    printPrompt; echo "Press any key to continue"
    read -n1 -s
}

preconfigureSystem() {
    sudo yum clean all &> /dev/null
    installPackageIfNeed curl
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


clear
preconfigureSystem

printPreInstallInfo

doInstallStep1
doInstallStep2
doInstallStep3