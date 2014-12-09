# TODO same user
#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/public/download/install-script) codenvy-single-server.properties

CONFIG="codenvy-single-server.properties"
CODENVY_USER=codenvy
CODENVY_GROUP=codenvy
CODENVY_HOME=/home/${CODENVY_USER}
APP_DIR=${CODENVY_HOME}/installation-manager
SCRIPT_NAME=installation-manager
SERVICE_NAME=codenvy-${SCRIPT_NAME}
RESOURCE_DIR=/home/codenvy-shared

# $1 - username; $2 - uid/gid
addUserOnDebian() {
    sudo adduser --quiet --shell /bin/bash --uid $2 --gid $2 --disabled-password --gecos "" $1
    sudo passwd -q -d -l $1 > /dev/null
    sudo su -c "echo '${CODENVY_GROUP}   ALL=(ALL)   NOPASSWD: ALL' >> /etc/sudoers"
}

# $1 - username; $2 - uid/gid
addUserOnRedhat() {
    sudo useradd --create-home --shell /bin/bash --uid $2 --gid $2 $1
    sudo passwd -l $1 > /dev/null
    sudo su -c "echo '${CODENVY_GROUP}   ALL=(ALL)   NOPASSWD: ALL' >> /etc/sudoers"
}

# $1 - username; $2 - uid/gid
addUserOnOpensuse() {
    sudo -s useradd --create-home --shell /bin/bash --uid $2 --gid $2 $1
    sudo -s passwd -q -l $1 > /dev/null
    sudo su -c "echo '${CODENVY_GROUP}   ALL=(ALL)   NOPASSWD: ALL' >> /etc/sudoers"
}

# $1 - groupname; $2 - gid (optional)
addGroupOnDebian() {
    if [ -z  "$2" ]; then
        sudo addgroup $1
    else
        sudo addgroup --quiet --gid $2 $1
    fi
}

# $1 - groupname; $2 - gid (optional)
addGroupOnRedhat() {
    if [ -z  "$2" ]; then
        sudo groupadd $1
    else
        sudo groupadd -g$2 $1
    fi
}

# $1 - groupname; $2 - gid (optional)
addGroupOnOpensuse() {
    if [ -z  "$2" ]; then
        sudo -s groupadd $1
    else
        sudo -s groupadd -g$2 $1
    fi
}

createCodenvyUserAndGroup() {
    sudo cp /etc/sudoers /etc/sudoers.back

    if [ `grep -c "^${CODENVY_USER}" /etc/group` == 0 ]; then
        printPrompt; echo "Creating group ${CODENVY_GROUP}"
        printPrompt; echo "Creating group ${CODENVY_GROUP}"
        addGroupOn${os} ${CODENVY_GROUP} 5001
    fi

    if [ `grep -c "^${CODENVY_USER}:" /etc/passwd` == 0 ]; then
        printPrompt; echo "Creating user ${CODENVY_USER}"
        addUserOn${os} ${CODENVY_USER} 5001
    fi

    sudo su - ${CODENVY_USER} -c "if [ ! -f "${CODENVY_HOME}/.bashrc" ]; then echo -e "\n" ${CODENVY_HOME}/.bashrc; fi"
    if [ ! -d ${RESOURCE_DIR} ]; then sudo mkdir ${RESOURCE_DIR}; fi
}

installJava() {
    source ~/.bashrc  # reload shell to get changes from previous executing of install-codenvy-single-server.sh at the same instance of console

    # check if requered program had already installed earlier for current user
    hash java 2>/dev/null || {
        printPrompt; echo "Installing java"

        wget -q --no-cookies --no-check-certificate --header 'Cookie: oraclelicense=accept-securebackup-cookie' 'http://download.oracle.com/otn-pub/java/jdk/7u17-b02/jre-7u17-linux-x64.tar.gz' --output-document=jre.tar.gz

        sudo tar -xf jre.tar.gz -C ${RESOURCE_DIR}
        rm jre.tar.gz

        if [ ! -f "~/.bashrc" ]; then echo -e "\n" > ~/.bashrc; fi

        sed -i '1i\export JAVA_HOME=/home/codenvy-shared/jre1.7.0_17' ${HOME}/.bashrc
        sed -i '2i\export PATH=$PATH:/home/codenvy-shared/jre1.7.0_17/bin' ${HOME}/.bashrc
        sudo su - ${CODENVY_USER} -c "sed -i '1i\export JAVA_HOME=/home/codenvy-shared/jre1.7.0_17' ${CODENVY_HOME}/.bashrc"
        sudo su - ${CODENVY_USER} -c "sed -i '2i\export PATH=$PATH:/home/codenvy-shared/jre1.7.0_17/bin' ${CODENVY_HOME}/.bashrc"
    }
}

registerImServiceOnDebian() {
    # http://askubuntu.com/questions/99232/how-to-make-a-jar-file-run-on-startup-and-when-you-log-out
    printPrompt; echo "Register Codenvy Installation Service"
    sudo update-rc.d ${SERVICE_NAME} defaults &>/dev/null
}

registerImServiceOnRedhat() {
    # http://www.abhigupta.com/2010/06/how-to-auto-start-services-on-boot-in-redhat-redhat/
    printPrompt; echo "Registering Codenvy Installation Service"
    sudo chkconfig --add ${SERVICE_NAME} &>/dev/null
    sudo chkconfig ${SERVICE_NAME} on &>/dev/null
}

registerImServiceOnOpensuse() {
    # http://www.abhigupta.com/2010/06/how-to-auto-start-services-on-boot-in-redhat-redhat/
    printPrompt; echo "Registering Codenvy Installation Service"
    sudo -s chkconfig --add ${SERVICE_NAME} &>/dev/null
    sudo -s chkconfig ${SERVICE_NAME} on &>/dev/null
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

# $1 - command name
installPackageIfNeed() {
    command -v $1 >/dev/null 2>&1 || { # check if requered command had already installed earlier
        printPrompt; echo "Installing $1 "
        installOn${os} $1
    }
}

installImCli() {
    printPrompt; echo "Downloading Installation Manager"

    DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager"

    imFileName=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')
    curl -s -o ${imFileName} -L ${DOWNLOAD_URL}

    imCLIFileName=$(tar -tf ${imFileName} | grep cli)

    # removes existed files and creates new directory
    sudo rm ${APP_DIR} -rf
    sudo mkdir ${APP_DIR}

    # untar archive
    sudo tar -xf ${imFileName} -C /tmp
    sudo tar -xf /tmp/${imFileName} -C ${APP_DIR}

    # create symbol link to installation-manager script into /etc/init.d
    if [ ! -L /etc/init.d/${SERVICE_NAME} ]; then
        sudo ln -s ${APP_DIR}/${SCRIPT_NAME} /etc/init.d/${SERVICE_NAME}
    fi

    # make it possible to write files into the APP_DIR
    sudo chmod 757 ${APP_DIR}

    # make the user ${CODENVY_USER} an owner all files into the APP_DIR
    sudo chown -R ${CODENVY_USER}:${CODENVY_GROUP} ${APP_DIR}

    # removes existed files and creates new directory
    cliinstalled=${HOME}/codenvy-cli
    rm ${cliinstalled} -rf &>/dev/null
    mkdir ${cliinstalled}

    tar -xf /tmp/${imCLIFileName} -C ${cliinstalled}

    CODENVY_SHARE_GROUP=codenvyshare
    USER_GROUP=$(groups | cut -d ' ' -f1)
    if [ `grep -c "^${CODENVY_SHARE_GROUP}" /etc/group` == 0 ]; then
        addGroupOn${os} ${CODENVY_SHARE_GROUP}
    fi

    sudo chown -R root.${CODENVY_SHARE_GROUP} ${RESOURCE_DIR}
    sudo gpasswd -a ${CODENVY_USER} ${CODENVY_SHARE_GROUP} > /dev/null
    sudo gpasswd -a ${USER} ${CODENVY_SHARE_GROUP} > /dev/null
    sudo chmod ug+rwx -R ${RESOURCE_DIR}
    sudo chmod g+s ${RESOURCE_DIR}

    sudo su - ${CODENVY_USER} -c "sed -i '1i\umask 002' ${CODENVY_HOME}/.bashrc"

    # stores parameters of installed Installation Manager CLI.
    sudo su - ${CODENVY_USER} -c "if [ ! -d ${CODENVY_HOME}/.codenvy ]; then mkdir ${CODENVY_HOME}/.codenvy; fi"
    sudo su -c "echo -e '${cliinstalled}\n${RESOURCE_DIR}\n${CODENVY_SHARE_GROUP}\n${USER}\n${USER_GROUP}' > ${CODENVY_HOME}/.codenvy/codenvy-cli-installed"
    sudo su -c "chown -R ${CODENVY_USER}:${CODENVY_GROUP} ${CODENVY_HOME}/.codenvy"

    # creates Codenvy configuration directory
    sudo su - ${CODENVY_USER} -c "sed -i '1i\export CODENVY_CONF=${CODENVY_HOME}/codenvy_conf' ${CODENVY_HOME}/.bashrc"
    sudo su - ${CODENVY_USER} -c "if [ ! -d ${CODENVY_HOME}/codenvy_conf ]; then mkdir ${CODENVY_HOME}/codenvy_conf; fi"
    if [ ! -f  "${CODENVY_HOME}/codenvy_conf/im.properties" ]; then
        sudo su - ${CODENVY_USER} -c "touch ${CODENVY_HOME}/codenvy_conf/im.properties"
    fi
}

launchingImService() {
    printPrompt; echo "Launching Codenvy Installation Service"
    if sudo service codenvy-installation-manager status | grep -Fq "running"; then
        sudo /etc/init.d/${SERVICE_NAME} restart > /dev/null
    else
        sudo /etc/init.d/${SERVICE_NAME} start > /dev/null
    fi
}

detectOS() {
    if [ -f /etc/debian_version ]; then
        os="Debian"
        osPrintableValue="Debian"
    elif [ -f /etc/redhat-release ]; then
        os="Redhat"
        osPrintableValue="Red Hat"
    elif [ -f /etc/os-release ]; then
        os="Opensuse"
        osPrintableValue="Open Suse"
    else
        printPrompt; echo "Operation system isn't supported."
        exit
    fi

    cd ~
}

askProperty() {
    prompt=$1
    property=$2

    if grep -Fq "${property}=MANDATORY" ${CONFIG}; then
        printPrompt; echo -n "${prompt}: "
        read value
        insertProperty ${property} ${value}
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
        read answer
        if [ ! "${answer}" == "y" ]; then exit 1; fi
    fi
}

execuetCliCommand() {
    if [ ! -z "$1" ]; then printPrompt; echo "$1"; fi
    ~/codenvy-cli/bin/codenvy $2 $3 $4 $5 $6 $7 $8

    RETVAL=$?
    [ ${RETVAL} -ne 0 ] && exit ${RETVAL}
}

printPreInstallInfo() {
    detectOS

    availableRAM=`sudo cat /proc/meminfo | grep MemTotal | awk '{print $2}'`
    availableRAM=$(perl -E "say sprintf('%.1f',${availableRAM}/1024/1024)")

    availableDiskSpace=`sudo df -h /home/${USER} | tail -1 | awk '{print $2}'`
    availableCores=`grep -c ^processor /proc/cpuinfo`

    printPrompt; echo "Welcome to Codenvy. This program will install Codenvy onto this node."
    printPrompt; echo "When the installation is complete, the Codenvy URL will be displayed."
    printPrompt; echo
    printPrompt; echo "This program will:"
    printPrompt; echo "1. Configure system"
    printPrompt; echo "2. Install Java and other required packages"
    printPrompt; echo "3. Install the Codenvy Installation Service"
    printPrompt; echo "4. Download Codenvy"
    printPrompt; echo "5. Install Codenvy by installing Puppet and configuring system parameters"
    printPrompt; echo "6. Boot Codenvy"
    printPrompt; echo
    printPrompt; echo "We have detected that this node is a ${osPrintableValue} distribution."
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
    installPackageIfNeed curl
    prepareConfig
    createCodenvyUserAndGroup
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
    printPrompt; echo "BEGINNING STEP 3: INSTALL THE CODENVY INSTALLATION SERVICE"
    installImCli
    registerImServiceOn${os}
    launchingImService
    printPrompt; echo "COMPLETED STEP 3: INSTALL THE CODENVY INSTALLATION SERVICE"
}

doInstallStep4() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 4: DOWNLOAD CODENVY"

    codenvyUser=`grep codenvy_user_name= ${CONFIG} | cut -d '=' -f2`
    codenvyPwd=`grep codenvy_password ${CONFIG} | cut -d '=' -f2`

    execuetCliCommand "Login to Codenvy Updater Service" login --remote update-server ${codenvyUser} ${codenvyPwd}
    execuetCliCommand "Downloading Codenvy binaries" im-download cdec
    execuetCliCommand "Checking the list of downloaded binaries" im-download --list-local
    printPrompt; echo "COMPLETED STEP 4: DOWNLOAD CODENVY"
}

doInstallStep5() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 5: INSTALL CODENVY BY INSTALLING PUPPET AND CONFIGURING SYSTEM PARAMETERS"
    execuetCliCommand "Installing the latest Codenvy version. Watch progress in /var/log/message" im-install --step 0-8 --config ${CONFIG} cdec
    printPrompt; echo "COMPLETED STEP 5: INSTALL CODENVY BY INSTALLING PUPPET AND CONFIGURING SYSTEM PARAMETERS"
}

doInstallStep6() {
    printPrompt; echo
    printPrompt; echo "BEGINNING STEP 6: BOOT CODENVY"
    execuetCliCommand "" im-install --step 9 --config ${CONFIG} cdec
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
