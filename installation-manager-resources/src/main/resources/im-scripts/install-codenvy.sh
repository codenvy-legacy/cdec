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

trap cleanUp EXIT

unset HOST_NAME
unset SYSTEM_ADMIN_NAME
unset SYSTEM_ADMIN_PASSWORD

JDK_URL=http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jdk-8u45-linux-x64.tar.gz
JRE_URL=http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jre-8u45-linux-x64.tar.gz

PUPPET_AGENT_PACKAGE=puppet-3.5.1-1.el7.noarch
PUPPET_SERVER_PACKAGE=puppet-server-3.5.1-1.el7.noarch

EXTERNAL_DEPENDENCIES=("https://codenvy.com||0"
                       "https://install.codenvycorp.com||0"
                       "http://archive.apache.org/dist/ant/binaries||0"
                       "http://dl.fedoraproject.org/pub/epel/||1"
                       "https://storage.googleapis.com/appengine-sdks/||0"
                       "http://www.us.apache.org/dist/maven/||0"
                       "https://repo.mongodb.org/yum/redhat/||0"
                       "http://repo.mysql.com/||0"
                       "http://nginx.org/packages/centos/||1"
                       "http://yum.postgresql.org/||0"
                       "http://yum.puppetlabs.com/||1"
                       "http://repo.zabbix.com/zabbix/||0"
                       "${JDK_URL}|Cookie:oraclelicense=accept-securebackup-cookie|0"
                       "http://mirror.centos.org/centos||0");

CURRENT_STEP=0
INSTALLATION_STEPS=("Configuring system..."
                    "Installing required packages... [java]"
                    "Install the Codenvy installation manager..."
                    "Downloading Codenvy binaries... "
                    "Installing Codenvy... ~20 mins"
                    "Installing Codenvy... ~20 mins"
                    "Installing Codenvy... ~20 mins"
                    "Installing Codenvy... ~20 mins"
                    "Installing Codenvy... ~20 mins"
                    "Installing Codenvy... ~20 mins"
                    "Installing Codenvy... ~20 mins"
                    "Installing Codenvy... ~20 mins"
                    "Booting Codenvy... "
                    "");                      

PUPPET_MASTER_PORTS=("tcp:8140");
SITE_PORTS=("tcp:80" "tcp:443" "tcp:10050" "tcp:32001" "tcp:32101");
API_PORTS=("tcp:8080" "tcp:8180" "tcp:10050" "tcp:32001" "tcp:32101" "tcp:32201" "tcp:32301");
DATA_PORTS=("tcp:389" "tcp:5432" "tcp:10050" "tcp:27017" "tcp:28017");
DATASOURCE_PORTS=("tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
RUNNER_PORTS=("tcp:80" "tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
BUILDER_PORTS=("tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
ANALYTICS_PORTS=("tcp:7777" "tcp:8080" "udp:5140" "tcp:9763" "tcp:10050" "tcp:32001" "tcp:32101");

INTERNET_CHECKER_PID=
TIMER_PID=
PRINTER_PID=
LINE_UPDATER_PID=
DOWNLOAD_PROGRESS_UPDATER_PID=

STEP_LINE=
PUPPET_LINE=
PROGRESS_LINE=
TIMER_LINE=

DEPENDENCIES_STATUS_OFFSET=85  # fit screen width = 100 cols
PROGRESS_FACTOR=2

cleanUp() {
    setterm -cursor on
    killTimer
    killPuppetInfoPrinter
    killInternetAccessChecker
    killFooterUpdater
    killDownloadProgressUpdater
}

validateExitCode() {
    local exitCode=$1
    if [[ -n "${exitCode}" ]] && [[ ! ${exitCode} == "0" ]]; then
        pauseTimer
        pausePuppetInfoPrinter
        pauseInternetAccessChecker
        pauseFooterUpdater
        pauseDownloadProgressUpdater
        println
        println $(printError "Unexpected error occurred. See install.log for more details")
        exit ${exitCode}
    fi
}

setRunOptions() {
    DIR="${HOME}/codenvy-im"
    ARTIFACT="codenvy"
    CODENVY_TYPE="single"
    SILENT=false
    for var in "$@"; do
        if [[ "$var" == "--multi" ]]; then
            CODENVY_TYPE="multi"
        elif [[ "$var" == "--silent" ]]; then
            SILENT=true
        elif [[ "$var" == "--im-cli" ]]; then
            ARTIFACT="installation-manager-cli"
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

    if [[ ${ARTIFACT} == "codenvy" ]]; then
        LAST_INSTALLATION_STEP=13
        ARTIFACT_DISPLAY="Codenvy"
        if [[ -z ${VERSION} ]]; then
            VERSION=$(fetchProperty "https://codenvy.com/update/repository/properties/${ARTIFACT}?label=stable" "version")
        fi
    else
        LAST_INSTALLATION_STEP=3
        ARTIFACT_DISPLAY="Installation Manager CLI"
        CODENVY_TYPE="single"
        INSTALLATION_STEPS=("Configuring system..."
                            "Installing required packages... [java]"
                            "Install the Codenvy installation manager..."
                            "");
        if [[ -z ${VERSION} ]]; then
            VERSION=$(fetchProperty "https://codenvy.com/update/repository/properties/${ARTIFACT}" "version")
        fi
    fi

    CONFIG="codenvy.properties"
    EXTERNAL_DEPENDENCIES[0]="https://codenvy.com/update/repository/public/download/${ARTIFACT}/${VERSION}||0"

    if [[ ${CODENVY_TYPE} == "single" ]] && [[ ! -z ${HOST_NAME} ]] && [[ ! -z ${SYSTEM_ADMIN_PASSWORD} ]] && [[ ! -z ${SYSTEM_ADMIN_NAME} ]]; then
        SILENT=true
    fi
}


fetchProperty() {
    local url=$1
    local property=$2
    local seq="s/.*\"${property}\":\"\([^\"]*\)\".*/\1/"
    echo `curl -s ${url} | sed ${seq}`
}

# run specific function and don't break installation if connection lost
doEvalWaitReconnection() {
    local func=$1
    shift

    for ((;;)); do
        eval ${func} $@
        local exitCode=$?

        if [[ ${exitCode} == 0 ]]; then
            break
        else
            doUpdateInternetAccessChecker
            local checkFailed=$?

            if [[ ${checkFailed} == 0 ]]; then
                return ${exitCode} # Internet connection is OK, probably another error
            else
                sleep 1m # wait reconnection
            fi
        fi
    done
}

doConfigureSystem() {
    setStepIndicator 0

    if [ -d ${DIR} ]; then rm -rf ${DIR}; fi
    mkdir ${DIR}

    doEvalWaitReconnection installPackageIfNeed tar
    validateExitCode $?

    doEvalWaitReconnection installPackageIfNeed unzip
    validateExitCode $?

    doEvalWaitReconnection installPackageIfNeed net-tools
    validateExitCode $?
}

doInstallJava() {
    setStepIndicator 1
    doEvalWaitReconnection installJava
    validateExitCode $?
}

doInstallImCli() {
    setStepIndicator 2
    doEvalWaitReconnection installIm
    validateExitCode $?
}

# Download binaries. If file is corrupted due to unexpected errors then it will be redownloaded
doDownloadBinaries() {
    setStepIndicator 3

    for ((;;)); do
        OUTPUT=$(doEvalWaitReconnection executeIMCommand im-download ${ARTIFACT} ${VERSION})
        local exitCode=$?
        echo ${OUTPUT} | sed 's/\[[=> ]*\]//g'  >> install.log

        if [[ ${exitCode} == 0 ]]; then
            break
        fi

        if [[ ${OUTPUT} =~ .*File.corrupted.* ]]; then
            echo "Codenvy binaries will be redownloaded" >> install.log
            continue
        else
            validateExitCode ${exitCode}
        fi
    done
    doUpdateDownloadProgress 100

    executeIMCommand im-download --list-local >> install.log
    validateExitCode $?
}

runDownloadProgressUpdater() {
    updateDownloadProgress &
    DOWNLOAD_PROGRESS_UPDATER_PID=$!
}

killDownloadProgressUpdater() {
    if [ -n "${DOWNLOAD_PROGRESS_UPDATER_PID}" ]; then
        kill -KILL ${DOWNLOAD_PROGRESS_UPDATER_PID}
    fi
}

continueDownloadProgressUpdater() {
    if [ -n "${DOWNLOAD_PROGRESS_UPDATER_PID}" ]; then
        kill -SIGCONT ${DOWNLOAD_PROGRESS_UPDATER_PID}
    fi
}

pauseDownloadProgressUpdater() {
    if [ -n "${DOWNLOAD_PROGRESS_UPDATER_PID}" ]; then
        kill -SIGSTOP ${DOWNLOAD_PROGRESS_UPDATER_PID}
    fi
}

updateDownloadProgress() {
    local totalSize=$(fetchProperty "https://codenvy.com/update/repository/properties/${ARTIFACT}/${VERSION}" "size")
    local file=$(fetchProperty "https://codenvy.com/update/repository/properties/${ARTIFACT}/${VERSION}" "file")

    for ((;;)); do
        local size
        local localFile="${HOME}/codenvy-im-data/updates/${ARTIFACT}/${VERSION}/${file}"
        if [[ -f ${localFile} ]]; then
            size=`du -b ${localFile} | cut -f1`
        else
            size=0
        fi
        local percent=$(( ${size}*100/${totalSize} ))

        doUpdateDownloadProgress ${percent}
        sleep 1 2>/dev/null
    done
}

doUpdateDownloadProgress() {
    local percent=$1
    local bars=$(( ${LAST_INSTALLATION_STEP}*${PROGRESS_FACTOR} ))
    local progress_field=
    for ((i=1; i<=$(( ${bars}*${percent}/100 )); i++));  do
       progress_field="${progress_field}="
    done
    progress_field=$(printf "[%-${bars}s]" ${progress_field})
    local message="Downloading  ${progress_field} ${percent}%"

    updateLine ${PUPPET_LINE} "${message}"
}

doInstallCodenvy() {
    for ((STEP=1; STEP<=9; STEP++));  do
        if [ ${STEP} == 9 ]; then
            setStepIndicator $(( $STEP+3 ))
        else
            setStepIndicator $(( $STEP+3 ))
        fi

        for ((;;)); do
            local exitCode
            if [ ${CODENVY_TYPE} == "multi" ]; then
                doEvalWaitReconnection executeIMCommand im-install --step ${STEP} --forceInstall --multi --config ${CONFIG} ${ARTIFACT} ${VERSION} >> install.log
                exitCode=$?
            else
                doEvalWaitReconnection executeIMCommand im-install --step ${STEP} --forceInstall --config ${CONFIG} ${ARTIFACT} ${VERSION} >> install.log
                exitCode=$?
            fi

            if [[ ${exitCode} == 0 ]]; then
                break;
            fi

            # if error occurred not because of internet access lost then break installation
            # it prevents breaking installation due to a lot of puppet errors
            local checkFailed=`cat /tmp/im_internet_access_lost 2>/dev/null`
            if [[ ! ${STEP} == 9 ]] && [[ ${checkFailed} == 1 ]]; then
                echo "Repeating installation step "${STEP} >> install.log
                continue;
            else
                validateExitCode ${exitCode}
            fi

            break;
        done
    done
}

downloadConfig() {
    local url="https://codenvy.com/update/repository/public/download/codenvy-${CODENVY_TYPE}-server-properties/${VERSION}"

    # check url to config on http error
    http_code=$(curl --silent --write-out '%{http_code}' --output /dev/null ${url})
    if [[ ! ${http_code} -eq 200 ]]; then    # if response code != "200 OK"
        local updates=`curl --silent "https://codenvy.com/update/repository/updates/${ARTIFACT}"`
        println $(printError "ERROR: Version '${VERSION}' is not available")
        println
        if [[ -n ${VERSION} ]] && [[ ! ${updates} =~ .*\"${VERSION}\".* ]]; then
            println $(printWarning "NOTE: You've used '--version' flag to install a specific version.")
            println $(printWarning "NOTE: We could not find this version in the repository. Versions found:")
            println $(printWarning "NOTE: ${updates}")
            println $(printWarning "NOTE: Installing without '--version' will use latest version.")
        else
            println $(printWarning "NOTE: codenvy.properties not found or downloadable.")
        fi

        exit 1
    fi

    # load config into the ${CONFIG} file
    curl --silent --output ${CONFIG} ${url}
}

# $1 - command name
installPackageIfNeed() {
    local exitCode
    rpm -qa | grep "^$1-" &> /dev/null || { # check if required package already has been installed earlier
        echo -n "Install package '$1'... " >> install.log

        exitCode=$(sudo yum install $1 -y -q --errorlevel=0 >> install.log 2>&1; echo $?)
        if [[ ! ${exitCode} == 0 ]]; then
            echo " [FAILED]" >> install.log
            return ${exitCode}
        else
            echo " [OK]" >> install.log
        fi
    }
}

preConfigureSystem() {
    # valid sudo rights
    sudo -n true 2> /dev/null
    if [[ ! $? == 0 ]]; then
        println $(printError "ERROR: User '${USER}' doesn't have sudo rights")
        println
        println $(printWarning "NOTE: Grant sudo privileges to '${USER}' user and restart installation")
        exit 1
    fi

    doCheckInstalledPuppet

    sudo yum clean all &> /dev/null

    installPackageIfNeed curl
    validateExitCode $?

    installPackageIfNeed wget
    validateExitCode $?
    
    # back up file to prevent installation with wrong configuration
    if [[ -f ${CONFIG} ]] && [[ ! `cat ${CONFIG}` =~ .*${VERSION}.* ]]; then
        mv ${CONFIG} ${CONFIG}.back
    fi

    if [[ ! -f ${CONFIG} ]] && [[ ${ARTIFACT} == "codenvy" ]]; then
        downloadConfig
    fi
}

installJava() {
    echo -n "Install java package from '${JRE_URL}' into the directory '${DIR}/jre' ... " >> install.log

    wget -q --no-cookies --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" "${JRE_URL}" --output-document=jre.tar.gz >> install.log 2>&1 || return 1
    tar -xf jre.tar.gz -C ${DIR} >> install.log 2>&1

    rm -fr ${DIR}/jre >> install.log 2>&1
    mv -f ${DIR}/jre1.8.0_45 ${DIR}/jre >> install.log 2>&1
    rm jre.tar.gz >> install.log 2>&1

    echo " [OK]" >> install.log
}

installIm() {
    IM_URL="https://codenvy.com/update/repository/public/download/installation-manager-cli"
    if [[ "${ARTIFACT}" == "installation-manager-cli" ]]; then
        IM_URL=${IM_URL}"/"${VERSION}
    fi
    echo ${IM_URL} >> install.log

    IM_FILE=$(curl -sI  ${IM_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')
    if [[ ! $? == 0 ]]; then
        return 1
    fi

    curl -s -o ${IM_FILE} -L ${IM_URL} || return 1

    mkdir ${DIR}/codenvy-cli
    tar -xf ${IM_FILE} -C ${DIR}/codenvy-cli

    sed -i "2iJAVA_HOME=${HOME}/codenvy-im/jre" ${DIR}/codenvy-cli/bin/codenvy
    echo "export PATH=\$PATH:\$HOME/codenvy-im/codenvy-cli/bin" >> ${HOME}/.bashrc
}

clearLine() {
    echo -en "\033[2K"
}

cursorUp() {
    echo -en "\e[1A"
}

cursorDown() {
    echo -en "\e[1B"
}

cursorSave() {
    echo -en "\e[s"
}

cursorRestore() {
    echo -en "\e[u"
}

# $1 - line number which is starting backward from 1 - the last bottom row.
# $2, $3,.. - messages to display in line number $1
# replace spaces with "_" character not to loose them
updateLine() {
    local lineNumber=$1
    shift
    echo "$@" > /tmp/im_line_${lineNumber} 2>/dev/null
}

updateFooter() {
    for ((;;)); do
        cursorUp
        cursorUp
        cursorUp
        cursorUp
        
        for ((line=4; line>=1; line--));  do
            local prev_text=`cat /tmp/im_prev_line_${line} 2>/dev/null`
            local text=`cat /tmp/im_line_${line} 2>/dev/null | tail -1`
            
            if [[ ! ${prev_text} == ${text} ]]; then
                clearLine
                println "${text}"
                echo "${text}" > /tmp/im_prev_line_${line} 2>/dev/null
            else
                cursorDown
            fi
        done

        sleep 1 2>/dev/null
    done
}

# https://wiki.archlinux.org/index.php/Color_Bash_Prompt
printError() {
    echo -en "\e[91m$1\e[0m" # with High Intensity RED color
}

printSuccess() {
    echo -en "\e[32m$1\e[0m" # with Underline GREEN color
}

printWarning() {
    echo -en "\e[93m$1\e[0m" # with High Intensity YELLOW color
}

printImportantInfo() {
    echo -en "\e[92m$1\e[0m" # with Underline GREEN color
}

printImportantLink() {
    echo -en "\e[94m$1\e[0m" # with High Intensity blue color
}

printPrompt() {
    clearLine
    echo -en "\e[34m[CODENVY] \e[0m" # with Underline blue color
}

print() {
    printPrompt; echo -n "$@"
}

println() {
    printPrompt; echo "$@"
}

askProperty() {
    read VALUE
    echo ${VALUE}
}

insertProperty() {
    sed -i s/$1=.*/$1=$2/g ${CONFIG}
}

validateHostname() {
    DNS=$1
    OUTPUT=$(ping -c 1 ${DNS} &> /dev/null && echo success || echo fail)
    echo ${OUTPUT}
}

askHostnameAndInsertProperty() {
    PROMPT=$1
    VARIABLE=$2

    FIRST_ATTEMPT=true

    while :
    do
        if [[ ${FIRST_ATTEMPT} == false ]]; then
            cursorUp
            cursorUp
            clearLine
        else
            FIRST_ATTEMPT=false
        fi

        print "$(printf "%-35s" "${PROMPT}:") "

        read VALUE

        OUTPUT=$(validateHostname ${VALUE})
        if [ "${OUTPUT}" == "success" ]; then
           break
        else
            println $(printError "ERROR: The hostname '${VALUE}' isn't available or wrong. Please try again...")
        fi
    done

    insertProperty "${VARIABLE}" ${VALUE}
}

executeIMCommand() {
    ${DIR}/codenvy-cli/bin/codenvy $@
}

pressAnyKeyToContinueAndClearConsole() {
    if [[ ${SILENT} == false ]]; then
        println  "Press any key to continue"
        read -n1 -s
        clear
    fi
}

pressAnyKeyToContinue() {
    if [[ ${SILENT} == false ]]; then
        println  "Press any key to continue"
        read -n1 -s
    fi
}

pressYKeyToContinue() {
    if [[ ${SILENT} == false ]]; then
        if [[ ! -z $1 ]]; then
            print $@
        else
            print "Continue installation"
        fi
        echo -n " [y/N]: "

        read ANSWER
        if [[ ! "${ANSWER}" == "y" ]]; then
            exit 1
        fi
    fi
}

doCheckPortRemote() {
    local protocol=$1
    local port=$2
    local host=$3
    OUTPUT=$(ssh -o LogLevel=quiet -o StrictHostKeyChecking=no -t ${host} "netstat -ano | egrep LISTEN | egrep ${protocol} | egrep ':${host}\s'")
    echo ${OUTPUT}
}

doCheckPortLocal() {
    local protocol=$1
    local port=$2
    OUTPUT=$(netstat -ano | egrep LISTEN | egrep ${protocol} | egrep ":${port}\s")
    echo ${OUTPUT}
}

validatePortLocal() {
    local protocol=$1
    local port=$2
    local host="localhost"
    doValidatePort doCheckPortLocal ${protocol} ${port} ${host}
}

validatePortRemote() {
    local protocol=$1
    local port=$2
    local host=$3
    doValidatePort doCheckPortRemote ${protocol} ${port} ${host}
}

doValidatePort() {
    local func=$1
    local protocol=$2
    local port=$3
    local host=$4
    local output=$(eval ${func} ${protocol} ${port} ${host})

    if [ "${output}" != "" ]; then
        installPackageIfNeed lsof
        println $(printError "ERROR: The port ${protocol}:${port} on '${host}' is busy.")
        println $(printError "ERROR: The installation cannot proceed.")
        println
        println $(printWarning "NOTE: Codenvy uses this port internally. All required ports are listed in docs.")
        println $(printWarning "NOTE: The problem might occur if some services required by Codenvy are")
        println $(printWarning "NOTE: already running. Run 'sudo lsof -i ${protocol}:${port} | grep LISTEN' on '${host}' to identify")
        println $(printWarning "NOTE: the running process. We recommend restarting installation on a bare system.")
        exit 1
    fi  
}

doGetHostsVariables() {
    HOST_NAME=$(grep host_url\\s*=\\s*.* ${CONFIG} | sed 's/host_url\s*=\s*\(.*\)/\1/')
    PUPPET_MASTER_HOST_NAME=`grep puppet_master_host_name=.* ${CONFIG} | cut -f2 -d '='`
    DATA_HOST_NAME=`grep data_host_name=.* ${CONFIG} | cut -f2 -d '='`
    API_HOST_NAME=`grep api_host_name=.* ${CONFIG} | cut -f2 -d '='`
    BUILDER_HOST_NAME=`grep builder_host_name=.* ${CONFIG} | cut -f2 -d '='`
    RUNNER_HOST_NAME=`grep runner_host_name=.* ${CONFIG} | cut -f2 -d '='`
    DATASOURCE_HOST_NAME=`grep datasource_host_name=.* ${CONFIG} | cut -f2 -d '='`
    ANALYTICS_HOST_NAME=`grep analytics_host_name=.* ${CONFIG} | cut -f2 -d '='`
    SITE_HOST_NAME=`grep site_host_name=.* ${CONFIG} | cut -f2 -d '='`
}

doCheckAvailablePorts_single() {
    for PORT in ${PUPPET_MASTER_PORTS[@]} ${SITE_PORTS[@]} ${API_PORTS[@]} ${DATA_PORTS[@]} ${DATASOURCE_PORTS[@]} ${RUNNER_PORTS[@]} ${BUILDER_PORTS[@]}; do
        PROTOCOL=`echo ${PORT}|awk -F':' '{print $1}'`;
        PORT_ONLY=`echo ${PORT}|awk -F':' '{print $2}'`;

        validatePortLocal "${PROTOCOL}" "${PORT_ONLY}"
    done
}

doCheckInstalledPuppet() {
    # check puppet agent
    rpm -qa | grep "^puppet-[0-9]" &> /dev/null; 
    if [ $? -eq 0 ]; then
        rpm -qa | grep "$PUPPET_AGENT_PACKAGE" &> /dev/null;
        if [ $? -ne 0 ]; then
            println $(printError "ERROR: Your system has the wrong puppet agent version!")
            println $(printWarning "NOTE: Please, uninstall it or update to package '$PUPPET_AGENT_PACKAGE', and then start installation again.")
            exit 1;
        fi 
    fi

    # check puppet server
    rpm -qa | grep "^puppet-server-[0-9]" &> /dev/null; 
    if [ $? -eq 0 ]; then
        rpm -qa | grep "$PUPPET_SERVER_PACKAGE" &> /dev/null;
        if [ $? -ne 0 ]; then
            println $(printError "ERROR: Your system has the wrong puppet server version!")
            println $(printWarning "NOTE: Please, uninstall it or update to package '$PUPPET_SERVER_PACKAGE', and then start installation again.")
            exit 1;
        fi 
    fi
}

doCheckAvailablePorts_multi() {
    doGetHostsVariables

    for HOST in ${PUPPET_MASTER_HOST_NAME} ${DATA_HOST_NAME} ${API_HOST_NAME} ${BUILDER_HOST_NAME} ${DATASOURCE_HOST_NAME} ${ANALYTICS_HOST_NAME} ${SITE_HOST_NAME} ${RUNNER_HOST_NAME}; do
        if [[ ${HOST} == ${PUPPET_MASTER_HOST_NAME} ]]; then
            PORTS=${PUPPET_MASTER_PORTS[@]}
        elif [[ ${HOST} == ${DATA_HOST_NAME} ]]; then
            PORTS=${DATA_PORTS[@]}
        elif [[ ${HOST} == ${API_HOST_NAME} ]]; then
            PORTS=${API_PORTS[@]}
        elif [[ ${HOST} == ${BUILDER_HOST_NAME} ]]; then
            PORTS=${BUILDER_PORTS[@]}
        elif [[ ${HOST} == ${DATASOURCE_HOST_NAME} ]]; then
            PORTS=${DATASOURCE_PORTS[@]}
        elif [[ ${HOST} == ${ANALYTICS_HOST_NAME} ]]; then
            PORTS=${ANALYTICS_PORTS[@]}
        elif [[ ${HOST} == ${SITE_HOST_NAME} ]]; then
            PORTS=${SITE_PORTS[@]}
        elif [[ ${HOST} == ${RUNNER_HOST_NAME} ]]; then
            PORTS=${RUNNER_PORTS[@]}
        fi

        for PORT in ${PORTS[@]}; do
            PROTOCOL=`echo ${PORT}|awk -F':' '{print $1}'`;
            PORT_ONLY=`echo ${PORT}|awk -F':' '{print $2}'`;

            if [[ ${HOST} == ${PUPPET_MASTER_HOST_NAME} ]]; then
                validatePortLocal "${PROTOCOL}" "${PORT_ONLY}"
            else
                validatePortRemote "${PROTOCOL}" "${PORT_ONLY}" ${HOST}
            fi
        done
    done
}

printPreInstallInfo_single() {
    clear

    println "Welcome. This program installs ${ARTIFACT_DISPLAY} ${VERSION}."
    println
    println "Checking system pre-requisites..."
    println

    preConfigureSystem
    doCheckAvailableResourcesLocally 2500000 1 40000000 8000000 4 300000000

    println "Checking access to external dependencies..."
    println

    checkResourceAccess

    if [[ ${ARTIFACT} == "codenvy" ]]; then
        println "Configuring system properties with file://${CONFIG}..."
        println
    fi

    if [ -n "${SYSTEM_ADMIN_NAME}" ]; then
        insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
    fi

    if [ -n "${SYSTEM_ADMIN_PASSWORD}" ]; then
        if [[ "${VERSION}" =~ ^(4).* ]]; then
            insertProperty "admin_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
        else
            insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
        fi
    fi

    if [ -n "${HOST_NAME}" ]; then
        insertProperty "host_url" ${HOST_NAME}
    fi

    doCheckAvailablePorts_single
}

# parameter 1 - MIN_RAM_KB
# parameter 2 - MIN_CORES
# parameter 3 - MIN_DISK_SPACE_KB
doCheckAvailableResourcesLocally() {
    local MIN_RAM_KB=$1
    local MIN_CORES=$2
    local MIN_DISK_SPACE_KB=$3
    local REC_RAM_KB=$4
    local REC_CORES=$5
    local REC_DISK_SPACE_KB=$6


    local osIssueFound=false
    local osType=""
    local osVersion=""
    local osInfo=""

    case `uname` in
        Linux )
            # CentOS
            if [ -f /etc/redhat-release ] ; then
                osType="CentOS"
                osVersion=`cat /etc/redhat-release | sed 's/.* \([0-9.]*\) .*/\1/'`
                osInfo=`cat /etc/redhat-release | sed 's/Linux release //'`

            # SuSE
            elif [ -f /etc/SuSE-release ] ; then
                osInfo="SuSE"

            # debian
            elif [ -f /etc/debian_version ]; then
                osInfo=`cat /etc/issue.net`

            # other linux OS
            elif [ -f /etc/lsb-release ]; then
                osInfo=$(cat /etc/lsb-release | grep '^DISTRIB_ID' | awk -F=  '{ print $2 }')
            fi
            ;;

        * )
            osInfo=`uname`;
            ;;
    esac

    # check on OS CentOS 7
    if [[ ${osType} != "CentOS" ||  "7.1" > "${osVersion}" ]]; then
        osIssueFound=true
    fi

    local osInfoToDisplay=$(printf "%-30s" "${osInfo}")
    local osStateToDisplay=$([ ${osIssueFound} == false ] && echo "$(printSuccess "[OK]")" || echo "$(printError "[NOT OK]")")
    println "DETECTED OS: ${osInfoToDisplay} ${osStateToDisplay}"

    local resourceIssueFound="none"

    local availableRAM=`cat /proc/meminfo | grep MemTotal | awk '{print $2}'`
    local availableRAMIssue=false
    local RAMStateToDisplay=$(echo "$(printSuccess "[OK]")")

    local availableDiskSpace=`sudo df ${HOME} | tail -1 | awk '{print $4}'`  # available
    local availableDiskSpaceIssue=false
    local diskStateToDisplay=$(echo "$(printSuccess "[OK]")")

    local availableCores=`grep -c ^processor /proc/cpuinfo`
    local availableCoresIssue=false
    local coresStateToDisplay=$(echo "$(printSuccess "[OK]")")

    if (( ${availableRAM} < ${MIN_RAM_KB} )); then
        resourceIssueFound="blocker"
        RAMStateToDisplay=$(echo "$(printError "[NOT OK]")")
    elif (( ${availableRAM} < ${REC_RAM_KB} )); then
        [ ${resourceIssueFound} == "none" ] && resourceIssueFound="warning"
        RAMStateToDisplay=$(echo "$(printWarning "[WARNING]")")
    fi

    if (( ${availableCores} < ${MIN_CORES})); then
        resourceIssueFound="blocker"
        coresStateToDisplay=$(echo "$(printError "[NOT OK]")")
    elif (( ${availableCores} < ${REC_CORES} )); then
        [ ${resourceIssueFound} == "none" ] && resourceIssueFound="warning"
        coresStateToDisplay=$(echo "$(printWarning "[WARNING]")")
    fi

    if (( ${availableDiskSpace} < ${MIN_DISK_SPACE_KB})); then
        resourceIssueFound="blocker"
        diskStateToDisplay=$(echo "$(printError "[NOT OK]")")
    elif (( ${availableDiskSpace} < ${REC_DISK_SPACE_KB} )); then
        [ ${resourceIssueFound} == "none" ] && resourceIssueFound="warning"
        diskStateToDisplay=$(echo "$(printWarning "[WARNING]")")
    fi

    local minRAMToDisplay=$(printf "%-15s" "$(echo ${MIN_RAM_KB} | awk '{tmp = $1/1000/1000; printf"%0.2f",tmp}') GB")
    local recRAMToDisplay=$(printf "%-15s" "$(echo ${REC_RAM_KB} | awk '{tmp = $1/1000/1000; printf"%0.2f",tmp}') GB")
    local availableRAMToDisplay=`cat /proc/meminfo | grep MemTotal | awk '{tmp = $2/1000/1000; printf"%0.2f",tmp}'`
    local availableRAMToDisplay=$(printf "%-11s" "${availableRAMToDisplay} GB")

    local minCoresToDisplay=$(printf "%-15s" "${MIN_CORES} cores")
    local recCoresToDisplay=$(printf "%-15s" "${REC_CORES} cores")
    local availableCoresToDisplay=$(printf "%-11s" "${availableCores} cores")

    local minDiskSpaceToDisplay=$(printf "%-15s" "$(( ${MIN_DISK_SPACE_KB} /1000/1000 )) GB")
    local recDiskSpaceToDisplay=$(printf "%-15s" "$(( ${REC_DISK_SPACE_KB} /1000/1000 )) GB")
    local availableDiskSpaceToDisplay=$(( availableDiskSpace /1000/1000 ))
    local availableDiskSpaceToDisplay=$(printf "%-11s" "${availableDiskSpaceToDisplay} GB")

    println
    println "                MINIMUM        RECOMMENDED     AVAILABLE"
    println "RAM             $minRAMToDisplay $recRAMToDisplay $availableRAMToDisplay $RAMStateToDisplay"
    println "CPU             $minCoresToDisplay $recCoresToDisplay $availableCoresToDisplay $coresStateToDisplay"
    println "Disk Space      $minDiskSpaceToDisplay $recDiskSpaceToDisplay $availableDiskSpaceToDisplay $diskStateToDisplay"
    println

    if [[ ${osIssueFound} == true ]]; then
        println $(printError "ERROR: The OS version doesn't match requirements.")
        println
        println $(printWarning "NOTE: You need a CentOS 7.1 node.")
        exit 1;
    fi

    if [[ ! ${resourceIssueFound} == "none" ]]; then
        if [[ ${resourceIssueFound} == "blocker" ]]; then
            println $(printError "ERROR: The resources available are lower than minimum.")
            exit 1
        else
            println $(printWarning "!!! The resources available are lower than recommended.")
            println
            if [[ ${SILENT} == false ]]; then
                pressYKeyToContinue "Proceed?"
                println
            fi
        fi
    fi
}

checkResourceAccess() {
    local resourceIssueFound=false
    local printStatus=true

    for resource in ${EXTERNAL_DEPENDENCIES[@]}; do
        doCheckResourceAccess ${resource} ${printStatus} || resourceIssueFound=true
    done

    println

    if [[ ${resourceIssueFound} == true ]]; then
        println $(printError "ERROR: Some external repositories are not accessible.")
        println
        println $(printWarning "NOTE: This is probably a temporary issue.")
        println $(printWarning "NOTE: Run 'wget --spider <url>' to check for access.")
        println $(printWarning "NOTE: Restart installation once access is restored.")
        println $(printWarning "NOTE: You may consider setting up a proxy server if access is blocked.")
        exit 1
    fi
}

doCheckResourceAccess() {
    local resource=$1
    local printStatus=$2
    local url=`echo ${resource} | awk -F'|' '{print $1}'`;
    local cookie=`echo ${resource} | awk -F'|' '{print $2}'`;
    local checkFailed=0

    if [[ ${cookie} == "" ]]; then
        wget --timeout=10 --tries=5 --quiet --spider ${url} || checkFailed=1
    else
        wget --timeout=10 --tries=5 --quiet --spider --no-cookies --no-check-certificate --header "${cookie}" ${url} || checkFailed=1
    fi

    if [[ ${printStatus} == true ]]; then
        local checkStatus=$([ ${checkFailed} == 0 ] && echo "$(printSuccess "[OK]")" || echo "$(printError "[NOT OK]")")
        println "$(printf "%-${DEPENDENCIES_STATUS_OFFSET}s" ${url}) ${checkStatus}"
    fi

    return ${checkFailed}
}

printPreInstallInfo_multi() {
    clear

    println "Welcome. This program installs ${ARTIFACT_DISPLAY} ${VERSION}."
    println
    println "Checking system pre-requisites..."
    println

    preConfigureSystem
    doCheckAvailableResourcesLocally 1000000 1 20000000 2000000 2 50000000


    if [[ ${ARTIFACT} == "codenvy" ]]; then
        println "Configuring system properties with file://${CONFIG}..."
        println
    fi

    if [ -n "${SYSTEM_ADMIN_NAME}" ]; then
        insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
    fi

    if [ -n "${SYSTEM_ADMIN_PASSWORD}" ]; then
        if [[ "${VERSION}" =~ ^(4).* ]]; then
            insertProperty "admin_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
        else
            insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
        fi
    fi

    if [[ ${SILENT} == true ]]; then
        if [ -n "${HOST_NAME}" ]; then
            insertProperty "host_url" ${HOST_NAME}
        fi

        doGetHostsVariables

        println "Hostname of Codenvy              : "${HOST_NAME}
        println "Hostname of Puppet master node   : "${PUPPET_MASTER_HOST_NAME}
        println "Hostname of data node            : "${DATA_HOST_NAME}
        println "Hostname of API node             : "${API_HOST_NAME}
        println "Hostname of builder node         : "${BUILDER_HOST_NAME}
        println "Hostname of runner node          : "${RUNNER_HOST_NAME}
        println "Hostname of datasource node      : "${DATASOURCE_HOST_NAME}
        println "Hostname of analytics node       : "${ANALYTICS_HOST_NAME}
        println "Hostname of site node            : "${SITE_HOST_NAME}
        println
    else
        println "Codenvy hostnames:       will prompt for entry"
        println

        askHostnameAndInsertProperty "Set hostname of Codenvy" "host_url"
        askHostnameAndInsertProperty "Set hostname of Puppet master node" "puppet_master_host_name"
        askHostnameAndInsertProperty "Set hostname of data node" "data_host_name"
        askHostnameAndInsertProperty "Set hostname of API node" "api_host_name"
        askHostnameAndInsertProperty "Set hostname of builder node" "builder_host_name"
        askHostnameAndInsertProperty "Set hostname of runner node" "runner_host_name"
        askHostnameAndInsertProperty "Set hostname of datasource node" "datasource_host_name"
        askHostnameAndInsertProperty "Set hostname of analytics node" "analytics_host_name"
        askHostnameAndInsertProperty "Set hostname of site node" "site_host_name"

        clearLine

        println
        pressYKeyToContinue "Proceed?"
        println
    fi

    println "Checking access to Codenvy nodes..."
    println

    doCheckAvailableResourcesOnNodes

    doCheckAvailablePorts_multi

    println "Checking access to external dependencies..."
    println

    checkResourceAccess
}

doCheckAvailableResourcesOnNodes() {
    local globalNodeIssueFound=false
    local globalOsIssueFound=false

    doGetHostsVariables

    local output=$(validateHostname ${PUPPET_MASTER_HOST_NAME})
    if [ "${output}" != "success" ]; then
        println $(printError "ERROR: The hostname '${PUPPET_MASTER_HOST_NAME}' is not available.")
        println
        println $(printWarning "NOTE: This might happen when the node is down or not accessible")
        println $(printWarning "NOTE: by a pre-configured DNS host name. Make sure you have")
        println $(printWarning "NOTE: an appropriate entry in '/etc/hosts' file.")
        exit 1
    fi
    println "$(printf "%-43s" "${PUPPET_MASTER_HOST_NAME}" && printSuccess "[OK]")"

    for HOST in ${DATA_HOST_NAME} ${API_HOST_NAME} ${BUILDER_HOST_NAME} ${DATASOURCE_HOST_NAME} ${ANALYTICS_HOST_NAME} ${SITE_HOST_NAME} ${RUNNER_HOST_NAME}; do
        # check if host available
        local output=$(validateHostname ${HOST})
        if [ "${output}" != "success" ]; then
            println $(printError "ERROR: The hostname '${HOST}' is not available.")
            println
            println $(printWarning "NOTE: This might happen when the node is down or not accessible")
            println $(printWarning "NOTE: by a pre-configured DNS host name. Make sure you have")
            println $(printWarning "NOTE: an appropriate entry in '/etc/hosts' file.")
            exit 1
        fi

        local sshPrefix="ssh -o BatchMode=yes -o LogLevel=quiet -o StrictHostKeyChecking=no -t ${HOST}"

        # validate ssh access
        ${sshPrefix} "exit"
        if [[ ! $? == 0 ]]; then
            println $(printError "ERROR: There is no ssh access to '${HOST}'")
            println
            println $(printWarning "NOTE: Put public part of ssh key (id_rsa.pub) onto '${HOST}' node")
            println $(printWarning "NOTE: into ~/.ssh folder of '${USER}' user and restart installation")
            exit 1
        fi

        # valid sudo rights
        ${sshPrefix} "sudo -n true 2> /dev/null"
        if [[ ! $? == 0 ]]; then
            println $(printError "ERROR: User '${USER}' doesn't have sudo rights on '${HOST}'")
            println
            println $(printWarning "NOTE: Grant sudo privileges to '${USER}' user and restart installation")
            exit 1
        fi

        if [[ ${HOST} == ${RUNNER_HOST_NAME} ]]; then
            MIN_RAM_KB=1500000
            MIN_DISK_SPACE_KB=40000000
        else
            MIN_RAM_KB=1000000
            MIN_DISK_SPACE_KB=20000000
        fi

        local osIssueFound=false

        local osType=""
        local osVersion=""
        local osInfo=""

        case `${sshPrefix} "uname" | sed 's/\r//'` in
            Linux )
                if [[ `${sshPrefix} "if [[ -f /etc/redhat-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osType="CentOS";
                    osVersion=`${sshPrefix} "cat /etc/redhat-release" | sed 's/.* \([0-9.]*\) .*/\1/'`
                    osInfo=`${sshPrefix} "cat /etc/redhat-release" | sed 's/Linux release //' | sed 's/\r//'`

                # SuSE
                elif [[ `${sshPrefix} "if [[ -f /etc/SuSE-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo="SuSE"

                # debian
                elif [[ `${sshPrefix} "if [[ -f /etc/debian_version ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo=`${sshPrefix} "cat /etc/issue.net" | sed 's/\r//'`

                # other linux OS
                elif [[ `${sshPrefix} "if [[ -f /etc/lsb-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo=`${sshPrefix} "$(cat /etc/lsb-release | grep '^DISTRIB_ID' | awk -F=  '{ print $2 }')" | sed 's/\r//'`
                fi
                ;;

            * )
                osInfo=`${sshPrefix} "uname" | sed 's/\r//'`;
                ;;
        esac

        # check on OS CentOS 7
        if [[ ${osType} != "CentOS" || "7.1" > "${osVersion}" ]]; then
            osIssueFound=true
            globalOsIssueFound=true
        fi

        local availableRAM=`${sshPrefix} "cat /proc/meminfo | grep MemTotal" | awk '{print $2}'`
        local availableRAMIssue=false

        local availableDiskSpace=`${sshPrefix} "sudo df ${HOME} | tail -1" | awk '{print $4}'` # available
        local availableDiskSpaceIssue=false

        if [[ -z ${availableRAM} || ${availableRAM} < ${MIN_RAM_KB} ]]; then
            availableRAMIssue=true
        fi

        if [[ -z ${availableDiskSpace} || ${availableDiskSpace} < ${MIN_DISK_SPACE_KB} ]]; then
            availableDiskSpaceIssue=true
        fi

        if [[ ${osIssueFound} == true || ${availableRAMIssue} == true || ${availableDiskSpaceIssue} == true ]]; then
            globalNodeIssueFound=true

            local nodeStateToDisplay=$([ ${osIssueFound} == true ] && echo $(printError "[NOT OK]") || echo $(printWarning "[WARNING]"))
            println "$(printf "%-43s" "${HOST}" ${nodeStateToDisplay})"

            osInfoToDisplay=$(printf "%-30s" "${osInfo}")
            if [[ ${osIssueFound} == true ]]; then
                println "> DETECTED OS: ${osInfoToDisplay} $(printError "[NOT OK]")"
                println
            fi

            if [[ ${availableRAMIssue} == true || ${availableDiskSpaceIssue} == true ]]; then
                println ">                 RECOMMENDED     AVAILABLE"

                if [[ ${availableRAMIssue} == true ]]; then
                    local minRAMToDisplay=$(printf "%-15s" "$(printf "%0.2f" "$( m=34; awk -v m=${MIN_RAM_KB} 'BEGIN { print m/1000/1000 }' )") GB")
                    local availableRAMToDisplay=`${sshPrefix} "cat /proc/meminfo | grep MemTotal" | awk '{tmp = $2/1000/1000; printf"%0.2f",tmp}'`
                    local availableRAMToDisplay=$(printf "%-11s" "${availableRAMToDisplay} GB")

                    println "> RAM             $minRAMToDisplay $availableRAMToDisplay $(printWarning "[WARNING]")"
                fi

                if [[ ${availableDiskSpaceIssue} == true ]]; then
                    local minDiskSpaceToDisplay=$(printf "%-15s" "$(( ${MIN_DISK_SPACE_KB}/1000/1000 )) GB")
                    local availableDiskSpaceToDisplay=$(( availableDiskSpace /1000/1000 ))
                    local availableDiskSpaceToDisplay=$(printf "%-11s" "${availableDiskSpaceToDisplay} GB")

                    println "> Disk Space      $minDiskSpaceToDisplay $availableDiskSpaceToDisplay $(printWarning "[WARNING]")"
                fi

                println
            fi
        else
            println "$(printf "%-43s" "${HOST}" && printSuccess "[OK]")"
        fi
    done

    println

    if [[ ${globalNodeIssueFound} == true ]]; then
        if [[ ${globalOsIssueFound} == true ]]; then
            println $(printError "ERROR: The OS version doesn't match requirements.")
            println
            println $(printWarning "NOTE: You need a CentOS 7.1 node")
            exit 1;
        fi

        println $(printWarning "!!! Some nodes do not match recommended.")
        println
        if [[ ${SILENT} != true ]]; then
            pressYKeyToContinue "Proceed?"
            println
        fi
    fi
}

setStepIndicator() {
    CURRENT_STEP=$1
    echo ${CURRENT_STEP} > /tmp/im_current_step 2>/dev/null
    shift

    updateLine ${STEP_LINE} ${INSTALLATION_STEPS[${CURRENT_STEP}]}
    updateProgress ${CURRENT_STEP}
}

initTimer() {
    START_TIME=`date +%s`
}

runTimer() {
    updateTimer &
    TIMER_PID=$!
}

killTimer() {
    if [ -n "${TIMER_PID}" ]; then
        kill -KILL ${TIMER_PID}
    fi
}

continueTimer() {
    if [ -n "${TIMER_PID}" ]; then
        kill -SIGCONT ${TIMER_PID}
    fi
}

pauseTimer() {
    if [ -n "${TIMER_PID}" ]; then
        kill -SIGSTOP ${TIMER_PID}
    fi
}

updateTimer() {
    for ((;;)); do
        END_TIME=`date +%s`
        DURATION=$(( $END_TIME-$START_TIME))
        M=$(( $DURATION/60 ))
        S=$(( $DURATION%60 ))

        updateLine ${TIMER_LINE} "Elapsed time: "${M}"m "${S}"s"

        sleep 1 2>/dev/null
    done
}

updateProgress() {
    local current_step=$1
    local last_step=${LAST_INSTALLATION_STEP}

    local progress_number=$(( ${current_step}*100/${last_step} ))

    local progress_field=
    for ((i=1; i<=${current_step}*${PROGRESS_FACTOR}; i++));  do
       progress_field="${progress_field}="
    done

    progress_field=$(printf "[%-$(( ${last_step}*${PROGRESS_FACTOR} ))s]" ${progress_field})

    local message="Full install ${progress_field} ${progress_number}%"

    updateLine ${PROGRESS_LINE} "${message}"
}

runPuppetInfoPrinter() {
    updatePuppetInfo &
    PRINTER_PID=$!    
}

killPuppetInfoPrinter() {
    if [ -n "${PRINTER_PID}" ]; then
        kill -KILL ${PRINTER_PID}
    fi
}

continuePuppetInfoPrinter() {
    if [ -n "${PRINTER_PID}" ]; then
        kill -SIGCONT ${PRINTER_PID}
    fi
}

pausePuppetInfoPrinter() {
    if [ -n "${PRINTER_PID}" ]; then
        kill -SIGSTOP ${PRINTER_PID}
    fi
}

runFooterUpdater() {
    updateFooter &
    LINE_UPDATER_PID=$!    
}

killFooterUpdater() {
    if [ -n "${LINE_UPDATER_PID}" ]; then
        kill -KILL ${LINE_UPDATER_PID}
    fi
}

continueFooterUpdater() {
    if [ -n "${LINE_UPDATER_PID}" ]; then
        kill -SIGCONT ${LINE_UPDATER_PID}
    fi
}

pauseFooterUpdater() {
    if [ -n "${LINE_UPDATER_PID}" ]; then
        kill -SIGSTOP ${LINE_UPDATER_PID}
    fi
}

# footer lines count descendently
initFooterPosition() {
    println
    println
    println
    println

    STEP_LINE=4
    PUPPET_LINE=3
    PROGRESS_LINE=2
    TIMER_LINE=1
    
    echo "" > /tmp/im_line_1 2>/dev/null
    echo "" > /tmp/im_line_2 2>/dev/null
    echo "" > /tmp/im_line_3 2>/dev/null
    echo "" > /tmp/im_line_4 2>/dev/null
    echo "" > /tmp/im_prev_line_1 2>/dev/null
    echo "" > /tmp/im_prev_line_2 2>/dev/null
    echo "" > /tmp/im_prev_line_3 2>/dev/null
    echo "" > /tmp/im_prev_line_4 2>/dev/null
}

updatePuppetInfo() {
    for ((;;)); do
        local line=$(sudo tail -n 1 /var/log/puppet/puppet-agent.log 2>/dev/null)
        if [[ -n "$line" ]]; then
            updateLine ${PUPPET_LINE} "[PUPPET: ${line:0:$(( ${DEPENDENCIES_STATUS_OFFSET}-8 ))}...]"     # print first N symbols of line
        else
            updateLine ${PUPPET_LINE} ""
        fi
        sleep 1 2>/dev/null
    done
}

runInternetAccessChecker() {
    echo "0" > /tmp/im_internet_access_lost 2>/dev/null
    updateInternetAccessChecker &
    INTERNET_CHECKER_PID=$!
}

killInternetAccessChecker() {
    if [ -n "${INTERNET_CHECKER_PID}" ]; then
        kill -KILL ${INTERNET_CHECKER_PID}
    fi
}

continueInternetAccessChecker() {
    if [ -n "${INTERNET_CHECKER_PID}" ]; then
        kill -SIGCONT ${INTERNET_CHECKER_PID}
    fi
}

pauseInternetAccessChecker() {
    if [ -n "${INTERNET_CHECKER_PID}" ]; then
        kill -SIGSTOP ${INTERNET_CHECKER_PID}
    fi
}

updateInternetAccessChecker() {
    for ((;;)); do
        doUpdateInternetAccessChecker
        local checkFailed=$?
        local tmp=`cat /tmp/im_current_step 2>/dev/null`

        if [[ "${tmp}" =~ ^[0-9]*$ ]]; then
            CURRENT_STEP=${tmp}
        fi

        if [[ ${checkFailed} == 1 ]]; then
            updateLine ${STEP_LINE} "${INSTALLATION_STEPS[${CURRENT_STEP}]} $(printError " Internet connection lost... reconnecting...")"
        else
            updateLine ${STEP_LINE} "${INSTALLATION_STEPS[${CURRENT_STEP}]}"
        fi
       
        sleep 1m 2>/dev/null
    done
}

doUpdateInternetAccessChecker() {
    local printStatus=false
    local checkFailed=0

    for resource in ${EXTERNAL_DEPENDENCIES[@]}; do
        local isRequiredToCheck=`echo ${resource} | awk -F'|' '{print $3}'`;

        if [[ ${isRequiredToCheck} == 1 ]]; then
            doCheckResourceAccess ${resource} ${printStatus} || checkFailed=1
        fi

        if [[ ${checkFailed} == 1 ]]; then
            echo ${checkFailed} > /tmp/im_internet_access_lost 2>/dev/null
        fi
    done

    return ${checkFailed}
}

printPostInstallInfo_codenvy() {
    if [ -z ${SYSTEM_ADMIN_NAME} ]; then
        SYSTEM_ADMIN_NAME=`grep admin_ldap_user_name= ${CONFIG} | cut -d '=' -f2`
    fi

    if [ -z ${SYSTEM_ADMIN_PASSWORD} ]; then
        if [[ "${VERSION}" =~ ^(4).* ]]; then
            SYSTEM_ADMIN_PASSWORD=`grep admin_ldap_password= ${CONFIG} | cut -d '=' -f2`
        else
            SYSTEM_ADMIN_PASSWORD=`grep system_ldap_password= ${CONFIG} | cut -d '=' -f2`
        fi
    fi

    if [ -z ${HOST_NAME} ]; then
        HOST_NAME=$(grep host_url\\s*=\\s*.* ${CONFIG} | sed 's/host_url\s*=\s*\(.*\)/\1/')
    fi

    println
    println "Codenvy is ready: $(printImportantLink "http://$HOST_NAME")"
    println "Admin user name:  $(printImportantInfo "$SYSTEM_ADMIN_NAME")"
    println "Admin password:   $(printImportantInfo "$SYSTEM_ADMIN_PASSWORD")"
    println
    println "$(printWarning "!!! Set up DNS or add a hosts rule on your clients to reach this hostname.")"
}

printPostInstallInfo_installation-manager-cli() {
    println
    println "Codenvy Installation Manager is installed into ${DIR}/codenvy-cli directory"
}

setRunOptions "$@"
printPreInstallInfo_${CODENVY_TYPE}

setterm -cursor off

initFooterPosition
initTimer

runFooterUpdater
runTimer
runInternetAccessChecker

doConfigureSystem
doInstallJava
doInstallImCli

if [[ ${ARTIFACT} == "codenvy" ]]; then
    runDownloadProgressUpdater
    doDownloadBinaries
    pauseDownloadProgressUpdater

    runPuppetInfoPrinter
    doInstallCodenvy
    pausePuppetInfoPrinter
fi

setStepIndicator ${LAST_INSTALLATION_STEP}
updateLine ${PUPPET_LINE} " "

sleep 2

pauseTimer
pauseInternetAccessChecker
pauseFooterUpdater

printPostInstallInfo_${ARTIFACT}


