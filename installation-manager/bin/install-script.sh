#!/bin/bash

# bash <(curl -s https://codenvy.com/update/repository/public/download/install-script)

CODENVY_USER=codenvy
CODENVY_HOME=/home/${CODENVY_USER}
APP_DIR=${CODENVY_HOME}/installation-manager
SCRIPT_NAME=installation-manager
SERVICE_NAME=codenvy-${SCRIPT_NAME}

# $1 - username; $2 - uid/gid
addUserOnDebian() {
    sudo adduser --quiet --shell /bin/bash --uid $2 --gid $2 --disabled-password --gecos "" $1
    sudo passwd -q -d -l $1
}

# $1 - username; $2 - uid/gid
addUserOnRedhat() {
    sudo useradd --create-home --shell /bin/bash --uid $2 --gid $2 $1
    sudo passwd -l $1
}

# $1 - username; $2 - uid/gid
addUserOnOpensuse() {
    sudo -s useradd --create-home --shell /bin/bash --uid $2 --gid $2 $1
    sudo -s passwd -q -l $1
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
    if [ `grep -c "^${CODENVY_USER}" /etc/group` == 0 ]; then
        echo "> Creating group codenvy"
        addGroupOn${os} ${CODENVY_USER} 5001
    fi

    if [ `grep -c "^${CODENVY_USER}:" /etc/passwd` == 0 ]; then
        echo "> Creating user codenvy"
        addUserOn${os} ${CODENVY_USER} 5001
    fi

    sudo su - ${CODENVY_USER} -c "if [ ! -f ${CODENVY_HOME}/.bashrc ]; then echo -e "\n" ${CODENVY_HOME}/.bashrc; fi"
}

installJava() {
    # check if requered program had already installed earlier for current user
    hash java 2>/dev/null || {
        echo ""
        echo "> Installing java"

        wget --no-cookies --no-check-certificate --header 'Cookie: oraclelicense=accept-securebackup-cookie' 'http://download.oracle.com/otn-pub/java/jdk/7u17-b02/jdk-7u17-linux-x64.tar.gz' --output-document=jdk.tar.gz

        echo "> Unpacking JDK binaries to /etc"

        sudo tar -xf jdk.tar.gz -C /etc
        rm jdk.tar.gz

        if [ ! -f "~/.bashrc" ]; then echo -e "\n" ~/.bashrc; fi

        sed -i '1i\export JAVA_HOME=/etc/jdk1.7.0_17' ${HOME}/.bashrc
        sed -i '2i\export PATH=$PATH:/etc/jdk1.7.0_17/bin' ${HOME}/.bashrc
        sudo su - ${CODENVY_USER} -c "sed -i '1i\export JAVA_HOME=/etc/jdk1.7.0_17' ${CODENVY_HOME}/.bashrc"
        sudo su - ${CODENVY_USER} -c "sed -i '2i\export PATH=$PATH:/etc/jdk1.7.0_17/bin' ${CODENVY_HOME}/.bashrc"

        echo "> Java has been installed"
    }
}

registerIMServiceOnDebian() {
    # http://askubuntu.com/questions/99232/how-to-make-a-jar-file-run-on-startup-and-when-you-log-out
    echo "> Register Codenvy Installation Manage Service"
    sudo update-rc.d ${SERVICE_NAME} defaults &>/dev/null
}

registerIMServiceOnRedhat() {
    # http://www.abhigupta.com/2010/06/how-to-auto-start-services-on-boot-in-redhat-redhat/
    echo "> Registering Codenvy Installation Manage Service"
    sudo chkconfig --add ${SERVICE_NAME} &>/dev/null
    sudo chkconfig ${SERVICE_NAME} on &>/dev/null
}

registerIMServiceOnOpensuse() {
    # http://www.abhigupta.com/2010/06/how-to-auto-start-services-on-boot-in-redhat-redhat/
    echo "> Registering Codenvy Installation Manage Service"
    sudo -s chkconfig --add ${SERVICE_NAME} &>/dev/null
    sudo -s chkconfig ${SERVICE_NAME} on &>/dev/null
}

# $1 - command name
installOnDebian() {
    sudo apt-get install $1 -y
}

# $1 - command name
installOnRedhat() {
    sudo yum install $1 -y
}

# $1 - command name
installOnOpensuse() {
    sudo -s zypper install $1 -y
}

# $1 - command name
installIfNeedCommand() {
    command -v $1 >/dev/null 2>&1 || {     # check if requered command had already installed earlier
        echo "> Installation $1 "
        installOn${os} $1
        echo "> $1 has been installed"
    }
}

installIMCli() {
    echo "> Downloading Installation Manager"

    DOWNLOAD_URL="https://codenvy.com/update/repository/public/download/installation-manager"

    imFileName=$(curl -sI  ${DOWNLOAD_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')
    curl -o ${imFileName} -L ${DOWNLOAD_URL}

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
    sudo chown -R ${CODENVY_USER}:${CODENVY_USER} ${APP_DIR}

    # removes existed files and creates new directory
    cliinstalled=${HOME}/codenvy-cli
    rm ${cliinstalled} -rf &>/dev/null
    mkdir ${cliinstalled}

    tar -xf /tmp/${imCLIFileName} -C ${cliinstalled}

    # create shared directory between 'codenvy' and current user
    cliupdatedir=/home/codenvy-shared
    if [ ! -d ${cliupdatedir} ]; then
        sudo mkdir ${cliupdatedir}
    fi

    CODENVY_SHARE_GROUP=codenvyshare
    USER_GROUP=$(groups | cut -d ' ' -f1)
    if [ `grep -c "^${CODENVY_SHARE_GROUP}" /etc/group` == 0 ]; then
        addGroupOn${os} ${CODENVY_SHARE_GROUP}
    fi

    sudo chown -R root.${CODENVY_SHARE_GROUP} ${cliupdatedir}
    sudo gpasswd -a ${CODENVY_USER} ${CODENVY_SHARE_GROUP}
    sudo gpasswd -a ${USER} ${CODENVY_SHARE_GROUP}
    sudo chmod ug+rwx -R ${cliupdatedir}
    sudo chmod g+s ${cliupdatedir}

    sudo su - ${CODENVY_USER} -c "sed -i '1i\umask 002' ${CODENVY_HOME}/.bashrc"

    # stores parameters of installed Installation Manager CLI.
    sudo su - ${CODENVY_USER} -c "if [ ! -d ${CODENVY_HOME}/.codenvy ]; then mkdir ${CODENVY_HOME}/.codenvy; fi"
    sudo su -c "echo -e '${cliinstalled}\n${cliupdatedir}\n${CODENVY_SHARE_GROUP}\n${USER}\n${USER_GROUP}' > ${CODENVY_HOME}/.codenvy/codenvy-cli-installed"
    sudo su -c "chown -R ${CODENVY_USER}:${CODENVY_USER} ${CODENVY_HOME}/.codenvy"

    # creates Codenvy configuration directory
    sudo su - ${CODENVY_USER} -c "sed -i '1i\export CODENVY_CONF=${CODENVY_HOME}/codenvy_conf' ${CODENVY_HOME}/.bashrc"
    if [ ! -d  "${CODENVY_HOME}/codenvy_conf" ]; then
        sudo su - ${CODENVY_USER} -c "mkdir ${CODENVY_HOME}/codenvy_conf"
    fi
    if [ ! -f  "${CODENVY_HOME}/codenvy_conf/im.properties" ]; then
        sudo su - ${CODENVY_USER} -c "touch ${CODENVY_HOME}/codenvy_conf/im.properties"
    fi
}

launchingIMService() {
    echo "> Launching Codenvy Installation Manage Service"
    # try to stop exists installation-manager
    sudo /etc/init.d/${SERVICE_NAME} stop
    sudo kill -9 $(ps aux | grep [i]nstallation-manager | cut -d" " -f4) &>/dev/null  # kill manager if stop isn't work

    # launch new installation-manager
    sudo /etc/init.d/${SERVICE_NAME} start
}

if [ -f /etc/debian_version ]; then
    os="Debian"
elif [ -f /etc/redhat-release ]; then
    os="Redhat"
elif [ -f /etc/os-release ]; then
    os="Opensuse"
else
    echo "Operation system isn't supported."
    exit
fi

echo "System is run on ${os} based distributive."
echo ""
echo "Wellcome to Codenvy. This programm will install Codenvy onto this node."
echo "When the installation is complete, the Codenvy URL will be displayed."
echo ""
echo "The installer will:"
echo "1. Install java"
echo "2. Install the Codenvy Installation Manager, which runs as a CLI and daemon"
echo "3. Download Codenvy"
echo "4. Install Codenvy by installing Puppte and configuring system parameters"
echo "5. Boot Codenvy"
echo ""
read -p "Press any key to continue" -n1 -s
echo ""
echo ""

cd ~

createCodenvyUserAndGroup

installIfNeedCommand curl
installIfNeedCommand tar
installIfNeedCommand wget
installJava
installIMCli

registerIMServiceOn${os}
launchingIMService

echo ""
echo "> Loging into Codenvy"
~/codenvy-cli/bin/codenvy login --remote "update-server"

echo ""
echo "> Downloading CDEC binaries"
~/codenvy-cli/bin/codenvy im-download cdec

echo ""
echo "> Checking list of downloaded binaries"
~/codenvy-cli/bin/codenvy im-download --list-local cdec

echo ""
echo "> Installing the latest CDEC version"
~/codenvy-cli/bin/codenvy im-install cdec



