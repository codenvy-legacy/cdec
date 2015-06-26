#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/public/download/install-im-cli)
set -e

setRunOptions() {
    DIR="${HOME}/codenvy-im"
    ARTIFACT="installation-manager-cli"

    VERSION=`curl -s https://codenvy.com/update/repository/properties/${ARTIFACT} | sed 's/.*"version":"\([^"]*\)".*/\1/'`
    for var in "$@"; do
        if [[ "$var" =~ --version=.* ]]; then
            VERSION=`echo "$var" | sed -e "s/--version=//g"`
        fi
    done
}

validateOS() {
    if [ -f /etc/redhat-release ]; then
        OS="Red Hat"
    else
        printLn "Operation system isn't supported."
        exit 1
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
    printLn "Installing java"
    wget -q --no-cookies --no-check-certificate --header 'Cookie: oraclelicense=accept-securebackup-cookie' 'http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jre-8u45-linux-x64.tar.gz' --output-document=jre.tar.gz

    tar -xf jre.tar.gz -C ${DIR}
    mv ${DIR}/jre1.8.0_45 ${DIR}/jre

    rm jre.tar.gz
}

installIm() {
    printLn "Downloading Installation Manager"

    IM_URL="https://codenvy.com/update/repository/public/download/${ARTIFACT}/${VERSION}"
    IM_FILE=$(curl -sI  ${IM_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')

    curl -s -o ${IM_FILE} -L ${IM_URL}

    mkdir ${DIR}/codenvy-cli
    tar -xf ${IM_FILE} -C ${DIR}/codenvy-cli

    sed -i "2iJAVA_HOME=${HOME}/codenvy-im/jre" ${DIR}/codenvy-cli/bin/codenvy
}

printPrompt() {
    echo -en "\e[94m[CODENVY]\e[0m "
}

printLn() {
    printPrompt; echo "$1"
}

printPreInstallInfo() {
    printLn "Welcome. This program installs Codenvy Installation Manager."
    printLn ""
}

preconfigureSystem() {
    sudo yum clean all &> /dev/null
    installPackageIfNeed curl

    validateOS
    setRunOptions "$@"
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


clear
preconfigureSystem "$@"

printPreInstallInfo

doInstallStep1
doInstallStep2
doInstallStep3
