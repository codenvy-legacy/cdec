#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/public/download/install-codenvy-single-server)

CONFIG="codenvy-single-server.properties"
DIR="${HOME}/codenvy-im"

installJava() {
    # check if requered program had already installed earlier for current user
    hash java 2>/dev/null || {
        printPrompt; echo "Installing java"
        wget -q --no-cookies --no-check-certificate --header 'Cookie: oraclelicense=accept-securebackup-cookie' 'http://download.oracle.com/otn-pub/java/jdk/7u17-b02/jre-7u17-linux-x64.tar.gz' --output-document=jre.tar.gz

        tar -xf jre.tar.gz -C ${DIR}
        mv ${DIR}/jre1.7.0_17 ${DIR}/jre

        rm jre.tar.gz
    }
}

# $1 - command name
installOnDebian() {
    sudo apt-get install $1 -y -q
}

# $1 - command name
installOnRedhat() {
    sudo yum install $1 -y -q
}

# $1 - command name
installOnOpensuse() {
    sudo -s zypper install $1 -y -q
}

detectOS() {
    if [ -f /etc/debian_version ]; then
        OS="Debian"
        OS_PRINTABLE="Debian"
    elif [ -f /etc/redhat-release ]; then
        OS="Redhat"
        OS_PRINTABLE="Red Hat"
    elif [ -f /etc/os-release ]; then
        OS="Opensuse"
        OS_PRINTABLE="Open Suse"
    else
        printPrompt; echo "Operation system isn't supported."
        exit
    fi
}

# $1 - command name
installPackageIfNeed() {
    command -v $1 >/dev/null 2>&1 || { # check if requered command had already installed earlier
        printPrompt; echo "Installing $1 "
        installOn${OS} $1
    }
}


installIm() {
    printPrompt; echo "Downloading Installation Manager"

    IM_URL="https://codenvy.com/update/repository/public/download/installation-manager"
    IM_FILE=$(curl -sI  ${IM_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')

    curl -s -o ${IM_FILE} -L ${IM_URL}

    mkdir ${DIR}/codenvy-cli
    tar -xf ${IM_FILE} -C ${DIR}/codenvy-cli

    sed -i "2iJAVA_HOME=${HOME}/codenvy-im/jre" ${DIR}/codenvy-cli/bin/codenvy
}

askProperty() {
    PROMPT=$1
    PROPERTY=$2

    if grep -Fq "${PROPERTY}=MANDATORY" ${CONFIG}; then
        printPrompt; echo -n "${PROMPT}: "
        read VALUE
        insertProperty ${PROPERTY} ${VALUE}
    fi
}

printPrompt() {
    echo -en "\e[94m[CODENVY]\e[0m "
}

insertProperty() {
    sed -i s/$1=.*/$1=$2/g ${CONFIG}
}

prepareConfig() {
    if [ ! -f ${CONFIG} ]; then
        curl -s -o codenvy-single-server.properties https://codenvy.com/update/repository/public/download/codenvy-single-server-properties
    fi

    insertProperty "aio_host_url" `hostname`

    if grep -Fq "=MANDATORY" ${CONFIG}; then
        printPrompt; echo "Please enter your Codenvy credentials"
        askProperty "Codenvy user name" "codenvy_user_name"
        askProperty "Codenvy password" "codenvy_password"

        printPrompt; echo -n "Continue installation [y/N]: "
        read ANSWER
        if [ ! "${ANSWER}" == "y" ]; then exit 1; fi
    fi
}

executeCliCommand() {
    if [ ! -z "$1" ]; then printPrompt; echo "$1"; fi
    ${DIR}/codenvy-cli/bin/codenvy $2 $3 $4 $5 $6 $7 $8

    RETVAL=$?
    [ ${RETVAL} -ne 0 ] && exit ${RETVAL}
}

# TODO
executeWithSudoCliCommand() {
    if [ ! -z "$1" ]; then printPrompt; echo "$1"; fi
    ${DIR}/codenvy-cli/bin/codenvy $2 $3 $4 $5 $6 $7 $8

    RETVAL=$?
    [ ${RETVAL} -ne 0 ] && exit ${RETVAL}
}

printPreInstallInfo() {
    detectOS

    availableRAM=`sudo cat /proc/meminfo | grep MemTotal | awk '{print $2}'`
    availableRAM=$(perl -E "say sprintf('%.1f',${availableRAM}/1024/1024)")

    availableDiskSpace=`sudo df -h ${HOME} | tail -1 | awk '{print $2}'`
    availableCores=`grep -c ^processor /proc/cpuinfo`

    printPrompt; echo "Welcome to Codenvy. This program will install Codenvy onto this node."
    printPrompt; echo "When the installation is complete, the Codenvy URL will be displayed."
    printPrompt; echo
    printPrompt; echo "This program will:"
    printPrompt; echo "1. Configure system"
    printPrompt; echo "2. Install Java and other required packages"
    printPrompt; echo "3. Install the Codenvy Installation Manager"
    printPrompt; echo "4. Download Codenvy"
    printPrompt; echo "5. Install Codenvy by installing Puppet and configuring system parameters"
    printPrompt; echo "6. Boot Codenvy"
    printPrompt; echo
    printPrompt; echo "We have detected that this node is a ${OS_PRINTABLE} distribution."
    printPrompt; echo
    printPrompt; echo "Configuration : Minimum : Available"
    printPrompt; echo "RAM           : 8GB     : ${availableRAM}GB"
    printPrompt; echo "CPU           : 4 cores : ${availableCores} cores"
    printPrompt; echo "Disk Space    : 300GB   : ${availableDiskSpace}B"
    printPrompt; echo
    printPrompt; echo "Sizing Guide: http://docs.codenvy.com/onpremises/installation/#sizing-single-node"
    printPrompt; echo
    printPrompt; echo "Codenvy will be configured based upon properties in ${CONFIG} file."
    printPrompt; echo "We will download this file if it does not exist."
    printPrompt; echo "We will interactively ask you to enter any MANDATORY parameters."
    printPrompt; echo
    printPrompt; echo "You will need to know your Codenvy user name and password."
    printPrompt; echo
    printPrompt; echo "Create account or retrieve password: "
    printPrompt; echo "Codenvy customer agreement & TOS: https://codenvy.com/legal"
    printPrompt; echo
    printPrompt; echo "Press any key to continue"
    read -n1 -s
}

printPostInstallInfo() {
    printPrompt; echo
    printPrompt; echo "Codenvy is ready at http://"`hostname`"/"
    printPrompt; echo
    printPrompt; echo "Troubleshoot Installation Problems:"
    printPrompt; echo "Upgrade & Configuration Docs:"
}

doInstallStep1() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 1: CONFIGURE SYSTEM"

    if [ -d ${DIR} ]; then rm -rf ${DIR}; fi
    mkdir ${DIR}

    installPackageIfNeed curl
    prepareConfig
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

    executeCliCommand "Login to Codenvy Updater manager" login --remote update-server ${CODENVY_USER} ${CODENVY_PWD}
    executeCliCommand "Downloading Codenvy binaries" im-download cdec
    executeCliCommand "Checking the list of downloaded binaries" im-download --list-local
    printPrompt; echo "COMPLETED STEP 4: DOWNLOAD CODENVY"
}

doInstallStep5() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 5: INSTALL CODENVY BY INSTALLING PUPPET AND CONFIGURING SYSTEM PARAMETERS"
    executeWithSudoCliCommand "Installing the latest Codenvy version. Watch progress in /var/log/message" im-install --step 0-8 --config ${CONFIG} cdec
    printPrompt; echo "COMPLETED STEP 5: INSTALL CODENVY BY INSTALLING PUPPET AND CONFIGURING SYSTEM PARAMETERS"
}

doInstallStep6() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 6: BOOT CODENVY"
    executeWithSudoCliCommand "" im-install --step 9 --config ${CONFIG} cdec
    printPrompt; echo "COMPLETED STEP 6: BOOT CODENVY"
}

printPreInstallInfo

doInstallStep1
doInstallStep2
doInstallStep3
doInstallStep4
doInstallStep5
doInstallStep6

printPostInstallInfo
