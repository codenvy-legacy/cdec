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

PUPPET_MASTER_PORTS=("tcp:8140");
SITE_PORTS=("tcp:80" "tcp:443" "tcp:10050" "tcp:32001" "tcp:32101");
API_PORTS=("tcp:8080" "tcp:8180" "tcp:10050" "tcp:32001" "tcp:32101" "tcp:32201" "tcp:32301");
DATA_PORTS=("tcp:389" "tcp:5432" "tcp:10050" "tcp:27017" "tcp:28017");
DATASOURCE_PORTS=("tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
RUNNER_PORTS=("tcp:80" "tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
BUILDER_PORTS=("tcp:8080" "tcp:10050" "tcp:32001" "tcp:32101");
ANALYTICS_PORTS=("tcp:7777" "tcp:8080" "udp:5140" "tcp:9763" "tcp:10050" "tcp:32001" "tcp:32101");

TIMER_PID=
PRINTER_PID=

STEP_LINE=
PUPPET_LINE=
PROGRESS_LINE=
TIMER_LINE=

DEPENDENCIES_STATUS_OFFSET=85  # fit screen width = 100 cols

function cleanUp() {
    killTimer
    killPuppetInfoPrinter
}

validateExitCode() {
    EXIT_CODE=$1
    if [[ -n "${EXIT_CODE}" ]] && [[ ! ${EXIT_CODE} == "0" ]]; then
        pauseTimer
        pausePuppetInfoPrinter
        println
        println "Unexpected error occurred. See install.log for more details"
        exit ${EXIT_CODE}
    fi
}

setRunOptions() {
    DIR="${HOME}/codenvy-im"
    ARTIFACT="codenvy"
    CODENVY_TYPE="single"
    SILENT=false
    VERSION=`curl -s https://codenvy.com/update/repository/properties/${ARTIFACT}?label=stable | sed 's/.*"version":"\([^"]*\)".*/\1/'`
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
    CONFIG="codenvy.properties"

    if [[ ${CODENVY_TYPE} == "single" ]] && [[ ! -z ${HOST_NAME} ]] && [[ ! -z ${SYSTEM_ADMIN_PASSWORD} ]] && [[ ! -z ${SYSTEM_ADMIN_NAME} ]]; then
        SILENT=true
    fi
}

downloadConfig() {
    url="https://codenvy.com/update/repository/public/download/codenvy-${CODENVY_TYPE}-server-properties/${VERSION}"

    # check url to config on http error
    http_code=$(curl --silent --write-out '%{http_code}' --output /dev/null ${url})
    if [[ ! ${http_code} -eq 200 ]]; then    # if response code != "200 OK"
        # print response from update server
        println $(curl --silent ${url})
        exit 1
    fi

    # load config into the ${CONFIG} file
    curl --silent --output ${CONFIG} ${url}
}

validateOS() {
    if [ -f /etc/redhat-release ]; then
        osToDisplay="Red Hat"
    else
        println  "Operation system isn't supported."
        exit 1
    fi

    OS=`cat /etc/redhat-release`
    osVersion=`${osToDisplay} | sed 's/.* \([0-9.]*\) .*/\1/' | cut -f1 -d '.'`

    if [ "${VERSION}" == "3.1.0" ] && [ "${osVersion}" != "6" ]; then
        println "Codenvy 3.1.0 can be installed onto CentOS 6.x only"
        exit 1
    fi

    if [ "${CODENVY_TYPE}" == "multi" ] && [ "${osVersion}" != "7" ]; then
        println "Codenvy multi-node can be installed onto CentOS 7.x only"
        exit 1
    fi
}

# $1 - command name
installPackageIfNeed() {
    local exitCode
    rpm -qa | grep "^$1-" &> /dev/null || { # check if required package already has been installed earlier
        echo -n "Install package '$1'... " >> install.log

        exitCode=$(sudo yum install $1 -y -q --errorlevel=0 >> install.log 2>&1; echo $?)

        validateExitCode ${exitCode}

        echo " [OK]" >> install.log
    }
}

preConfigureSystem() {
    sudo yum clean all &> /dev/null
    installPackageIfNeed curl
    installPackageIfNeed net-tools

    if [[ ! -f ${CONFIG} ]]; then
        downloadConfig
    fi
}

installJava() {
    local exitCode
    echo -n "Install java package from '${JRE_URL}' into the directory '${DIR}/jre' ... " >> install.log

    exitCode=$(wget -q --no-cookies --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" "${JRE_URL}" --output-document=jre.tar.gz >> install.log 2>&1; echo $?)
    validateExitCode ${exitCode}

    exitCode=$(tar -xf jre.tar.gz -C ${DIR} >> install.log 2>&1; echo $?)
    validateExitCode ${exitCode}

    rm -fr ${DIR}/jre >> install.log 2>&1
    mv -f ${DIR}/jre1.8.0_45 ${DIR}/jre >> install.log 2>&1
    rm jre.tar.gz >> install.log 2>&1

    echo " [OK]" >> install.log
}

installIm() {
    IM_URL="https://codenvy.com/update/repository/public/download/installation-manager-cli"
    IM_FILE=$(curl -sI  ${IM_URL} | grep -o -E 'filename=(.*)[.]tar.gz' | sed -e 's/filename=//')

    curl -s -o ${IM_FILE} -L ${IM_URL}

    mkdir ${DIR}/codenvy-cli
    tar -xf ${IM_FILE} -C ${DIR}/codenvy-cli

    sed -i "2iJAVA_HOME=${HOME}/codenvy-im/jre" ${DIR}/codenvy-cli/bin/codenvy
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
updateLine() {
    local lineNumber=$1

    if [[ -n ${lineNumber} ]]; then
        for ((i=1; i<lineNumber+1; i++)); do
            cursorUp
        done

        clearLine
        shift
        println "$@"

        for ((i=1; i<lineNumber; i++)); do
            cursorDown
        done
    fi
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
            println $(printError "ERROR: The hostname '${VALUE}' isn't availabe or wrong. Please try again...")
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
    PROTOCOL=$1
    PORT=$2
    HOST=$3
    OUTPUT=$(ssh -o LogLevel=quiet -o StrictHostKeyChecking=no -t $HOST "netstat -ano | egrep LISTEN | egrep ${PROTOCOL} | egrep ':${PORT}\s'")
    echo ${OUTPUT}
}

doCheckPortLocal() {
    PROTOCOL=$1
    PORT=$2
    OUTPUT=$(netstat -ano | egrep LISTEN | egrep ${PROTOCOL} | egrep ":${PORT}\s")
    echo ${OUTPUT}
}

validatePortLocal() {
    PROTOCOL=$1
    PORT=$2
    OUTPUT=$(doCheckPortLocal ${PROTOCOL} ${PORT})

    if [ "${OUTPUT}" != "" ]; then
        println $(printError "ERROR: The port ${PROTOCOL}:${PORT} is busy.")
        println $(printError "ERROR: The installation can't be proceeded.")
        exit 1
    fi
}

validatePortRemote() {
    PROTOCOL=$1
    PORT=$2
    HOST=$3
    OUTPUT=$(doCheckPortRemote ${PROTOCOL} ${PORT} ${HOST})

    if [ "${OUTPUT}" != "" ]; then
        println $(printError "ERROR: The port ${PROTOCOL}:${PORT} on host ${HOST} is busy.")
        println $(printError "ERROR: The installation can't be proceeded.")
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
    for PORT in ${PUPPET_MASTER_PORTS[@]} ${SITE_PORTS[@]} ${API_PORTS[@]} ${DATA_PORTS[@]} ${DATASOURCE_PORTS[@]} ${RUNNER_PORTS[@]} ${BUILDER_PORTS[@]} ${ANALYTICS_PORTS[@]}; do
        PROTOCOL=`echo ${PORT}|awk -F':' '{print $1}'`;
        PORT_ONLY=`echo ${PORT}|awk -F':' '{print $2}'`;

        validatePortLocal "${PROTOCOL}" "${PORT_ONLY}"
    done
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

    println "Welcome. This program installs Codenvy ${VERSION}."
    println
    println "Checking system pre-requisites..."
    println

    doCheckAvailableResourcesLocally 8000000 4 300000000

    preConfigureSystem

    println "Checking access to external dependencies..."
    println

    checkAccessToExternalDependencies

    println "Configuring system properties with file://${CONFIG}..."
    println

    if [ -n "${SYSTEM_ADMIN_NAME}" ]; then
        insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
    fi

    if [ -n "${SYSTEM_ADMIN_PASSWORD}" ]; then
        insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
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

    local osIssueFound=false
    local osType=""
    local osVersion=""
    local osInfo=""

    case `uname` in
        Linux )
            # CentOS
            if [ -f /etc/redhat-release ] ; then
                osType="CentOS"
                osVersion=`cat /etc/redhat-release | sed 's/.* \([0-9.]*\) .*/\1/' | cut -f1 -d '.'`
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
    if [[ ${osType} != "CentOS" || ${osVersion} != "7" ]]; then
        osIssueFound=true
    fi

    local osInfoToDisplay=$(printf "%-30s" "${osInfo}")
    local osStateToDisplay=$([ ${osIssueFound} == false ] && echo "$(printSuccess "[OK]")" || echo "$(printError "[NOT OK]")")
    println "DETECTED OS: ${osInfoToDisplay} ${osStateToDisplay}"

    resourceIssueFound=false

    local availableRAM=`cat /proc/meminfo | grep MemTotal | awk '{print $2}'`
    local availableRAMIssue=false

    local availableDiskSpace=`sudo df ${HOME} | tail -1 | awk '{print $2}'`
    local availableDiskSpaceIssue=false

    local availableCores=`grep -c ^processor /proc/cpuinfo`
    local availableCoresIssue=false

    if (( ${availableRAM} < ${MIN_RAM_KB} )); then
        resourceIssueFound=true
        availableRAMIssue=true
    fi

    if (( ${availableCores} < ${MIN_CORES})); then
        resourceIssueFound=true
        availableCoresIssue=true
    fi

    if (( ${availableDiskSpace} < ${MIN_DISK_SPACE_KB})); then
        resourceIssueFound=true
        availableDiskSpaceIssue=true
    fi

    local minRAMToDisplay=$(printf "%-15s" "$(printf "%0.2f" "$( m=34; awk -v m=${MIN_RAM_KB} 'BEGIN { print m/1000/1000 }' )") GB")
    local availableRAMToDisplay=`cat /proc/meminfo | grep MemTotal | awk '{tmp = $2/1000/1000; printf"%0.2f",tmp}'`
    local availableRAMToDisplay=$(printf "%-11s" "${availableRAMToDisplay} GB")
    local RAMStateToDisplay=$([ ${availableRAMIssue} == false ] && echo "$(printSuccess "[OK]")" || echo "$(printWarning "[WARNING]")")

    local minCoresToDisplay=$(printf "%-15s" "${MIN_CORES} cores")
    local availableCoresToDisplay=$(printf "%-11s" "${availableCores} cores")
    local coresStateToDisplay=$([ ${availableCoresIssue} == false ] && echo "$(printSuccess "[OK]")" || echo "$(printWarning "[WARNING]")")

    local minDiskSpaceToDisplay=$(printf "%-15s" "$(( ${MIN_DISK_SPACE_KB} /1000/1000 )) GB")
    local availableDiskSpaceToDisplay=$(( availableDiskSpace /1000/1000 ))
    local availableDiskSpaceToDisplay=$(printf "%-11s" "${availableDiskSpaceToDisplay} GB")
    local diskStateToDisplay=$([ ${availableDiskSpaceIssue} == false ] && echo "$(printSuccess "[OK]")" || echo "$(printWarning "[WARNING]")")

    println
    println "                RECOMMENDED     AVAILABLE"
    println "RAM             $minRAMToDisplay $availableRAMToDisplay $RAMStateToDisplay"
    println "CPU             $minCoresToDisplay $availableCoresToDisplay $coresStateToDisplay"
    println "Disk Space      $minDiskSpaceToDisplay $availableDiskSpaceToDisplay $diskStateToDisplay"
    println

    if [[ ${osIssueFound} == true || ${resourceIssueFound} == true ]]; then
        if [[ ${osIssueFound} == true ]]; then
            println $(printError "!!! The OS version or config do not match requirements.")
            exit 1;
        fi

        if [[ ${resourceIssueFound} == true ]]; then
            println $(printWarning "!!! The resources available are lower than recommended.")
        fi

        println

        if [[ ${SILENT} == false && ${resourceIssueFound} == true ]]; then
            pressYKeyToContinue "Proceed?"
            println
        fi
    fi
}

checkAccessToExternalDependencies() {
    local resourceIssueFound=false

    checkUrl https://install.codenvycorp.com || resourceIssueFound=true
    checkUrl http://archive.apache.org/dist/ant/binaries || resourceIssueFound=true
    checkUrl ${JDK_URL} "Cookie: oraclelicense=accept-securebackup-cookie" || resourceIssueFound=true
    checkUrl http://dl.fedoraproject.org/pub/epel/ || resourceIssueFound=true
    checkUrl https://storage.googleapis.com/appengine-sdks/ || resourceIssueFound=true
    checkUrl http://www.us.apache.org/dist/maven/ || resourceIssueFound=true
    checkUrl https://repo.mongodb.org/yum/redhat/ || resourceIssueFound=true
    checkUrl http://repo.mysql.com/ || resourceIssueFound=true
    checkUrl http://nginx.org/packages/centos/ || resourceIssueFound=true
    checkUrl http://yum.postgresql.org/ || resourceIssueFound=true
    checkUrl http://yum.puppetlabs.com/ || resourceIssueFound=true
    checkUrl http://repo.zabbix.com/zabbix/ || resourceIssueFound=true
    checkUrl http://mirror.centos.org/centos/ || resourceIssueFound=true
    checkUrl https://codenvy.com/update/repository/public/download/${ARTIFACT}/${VERSION} || resourceIssueFound=true  # check Codenvy binary accessibility

    println

    if [[ ${resourceIssueFound} == true ]]; then
        println $(printError "!!! Some repositories are not accessible. The installation will fail.")
        println $(printError "!!! Consider setting up a proxy server.")
        println

        if [[ ${SILENT} == true ]]; then
            exit 1;
        fi

        pressYKeyToContinue "Proceed?"
        println
    fi
}

# parameter 1 - url
# parameter 2 - cookie
checkUrl() {
    local checkFailed=0
    local url=$1
    local cookie=$2

    if [[ ${cookie} == "" ]]; then
        wget --timeout=10 --tries=5 --quiet --spider ${url} || checkFailed=1
    else
        wget --timeout=10 --tries=5 --quiet --spider --no-cookies --no-check-certificate --header "${cookie}" ${url} || checkFailed=1
    fi

    local checkStatus=$([ ${checkFailed} == 0 ] && echo "$(printSuccess "[OK]")" || echo "$(printError "[NOT OK]")")
    println "$(printf "%-${DEPENDENCIES_STATUS_OFFSET}s" ${url}) ${checkStatus}"

    return ${checkFailed}
}

printPreInstallInfo_multi() {
    clear

    println "Welcome. This program installs Codenvy ${VERSION}."
    println
    println "Checking system pre-requisites..."
    println

    doCheckAvailableResourcesLocally 1000000 1 14000000
    preConfigureSystem

    println "Configuring system properties with file://${CONFIG}..."
    println

    if [ -n "${SYSTEM_ADMIN_NAME}" ]; then
        insertProperty "admin_ldap_user_name" ${SYSTEM_ADMIN_NAME}
    fi

    if [ -n "${SYSTEM_ADMIN_PASSWORD}" ]; then
        insertProperty "system_ldap_password" ${SYSTEM_ADMIN_PASSWORD}
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

    checkAccessToExternalDependencies
}

doCheckAvailableResourcesOnNodes() {
    local globalNodeIssueFound=false
    local globalOsIssueFound=false

    doGetHostsVariables

    for HOST in ${PUPPET_MASTER_HOST_NAME} ${DATA_HOST_NAME} ${API_HOST_NAME} ${BUILDER_HOST_NAME} ${DATASOURCE_HOST_NAME} ${ANALYTICS_HOST_NAME} ${SITE_HOST_NAME} ${RUNNER_HOST_NAME}; do
        # check if host available
        local OUTPUT=$(validateHostname ${HOST})
        if [ "${OUTPUT}" != "success" ]; then
            println $(printError "ERROR: The hostname '${HOST}' isn't availabe or wrong.")
            exit 1
        fi

        local SSH_PREFIX="ssh -o LogLevel=quiet -o StrictHostKeyChecking=no -t ${HOST}"

        if [[ ${HOST} == ${RUNNER_HOST_NAME} ]]; then
            MIN_RAM_KB=1500000
            MIN_DISK_SPACE_KB=50000000
        else
            MIN_RAM_KB=1000000
            MIN_DISK_SPACE_KB=14000000
        fi

        local osIssueFound=false

        local osType=""
        local osVersion=""
        local osInfo=""

        case `${SSH_PREFIX} "uname" | sed 's/\r//'` in
            Linux )
                if [[ `${SSH_PREFIX} "if [[ -f /etc/redhat-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osType="CentOS";
                    osVersion=`${SSH_PREFIX} "cat /etc/redhat-release" | sed 's/.* \([0-9.]*\) .*/\1/' | cut -f1 -d '.'`
                    osInfo=`${SSH_PREFIX} "cat /etc/redhat-release" | sed 's/Linux release //' | sed 's/\r//'`

                # SuSE
                elif [[ `${SSH_PREFIX} "if [[ -f /etc/SuSE-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo="SuSE"

                # debian
                elif [[ `${SSH_PREFIX} "if [[ -f /etc/debian_version ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo=`${SSH_PREFIX} "cat /etc/issue.net" | sed 's/\r//'`

                # other linux OS
                elif [[ `${SSH_PREFIX} "if [[ -f /etc/lsb-release ]]; then echo 1; fi" | sed 's/\r//'` == 1 ]]; then
                    osInfo=`${SSH_PREFIX} "$(cat /etc/lsb-release | grep '^DISTRIB_ID' | awk -F=  '{ print $2 }')" | sed 's/\r//'`
                fi
                ;;

            * )
                osInfo=`${SSH_PREFIX} "uname" | sed 's/\r//'`;
                ;;
        esac

        # check on OS CentOS 7
        if [[ ${osType} != "CentOS" || ${osVersion} != "7" ]]; then
            osIssueFound=true
        fi

        local availableRAM=`${SSH_PREFIX} "cat /proc/meminfo | grep MemTotal" | awk '{print $2}'`
        local availableRAMIssue=false

        local availableDiskSpace=`${SSH_PREFIX} "sudo df ${HOME} | tail -1" | awk '{print $2}'`
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
                    local availableRAMToDisplay=`${SSH_PREFIX} "cat /proc/meminfo | grep MemTotal" | awk '{tmp = $2/1000/1000; printf"%0.2f",tmp}'`
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
        println $(printWarning "!!! Some nodes do not match recommended.")
        println

        if [[ ${SILENT} == true && ${globalOsIssueFound} == true ]]; then
            exit 1;
        fi

        if [[ ${SILENT} != true ]]; then
            pressYKeyToContinue "Proceed?"
            println
        fi
    fi
}

doConfigureSystem() {
    nextStep 0 "Configuring system..."

    if [ -d ${DIR} ]; then rm -rf ${DIR}; fi
    mkdir ${DIR}
}

doInstallPackages() {
    nextStep 1 "Installing required packages... [tar ]"
    installPackageIfNeed tar

    nextStep 1 "Installing required packages... [wget]"
    installPackageIfNeed wget

    nextStep 1 "Installing required packages... [unzip]"
    installPackageIfNeed unzip

    nextStep 1 "Installing required packages... [java]"
    installJava
}

doInstallImCli() {
    nextStep 2 "Install the Codenvy installation manager..."
    installIm
}

doDownloadBinaries() {
    nextStep 3 "Downloading Codenvy binaries... "
    OUTPUT=$(executeIMCommand im-download ${ARTIFACT} ${VERSION})
    EXIT_CODE=$?
    echo ${OUTPUT} | sed 's/\[[=> ]*\]//g'  >> install.log
    validateExitCode ${EXIT_CODE}

    executeIMCommand im-download --list-local >> install.log
    validateExitCode $?
}

doInstallCodenvy() {
    for ((STEP=1; STEP<=9; STEP++));  do
        if [ ${STEP} == 9 ]; then
            nextStep $(( $STEP+3 )) "Booting Codenvy... "
        else
            nextStep $(( $STEP+3 )) "Installing Codenvy... ~20 mins"
        fi

        if [ ${CODENVY_TYPE} == "multi" ]; then
            executeIMCommand im-install --step ${STEP} --forceInstall --multi --config ${CONFIG} ${ARTIFACT} ${VERSION} >> install.log
            validateExitCode $?
        else
            executeIMCommand im-install --step ${STEP} --forceInstall --config ${CONFIG} ${ARTIFACT} ${VERSION} >> install.log
            validateExitCode $?
        fi
    done

    nextStep 14 ""

    sleep 2
    pauseTimer
    pausePuppetInfoPrinter
}

nextStep() {
    pauseTimer
    pausePuppetInfoPrinter

    CURRENT_STEP=$1
    shift

    updateLine ${STEP_LINE} "$@"
    updateProgress ${CURRENT_STEP}

    continueTimer
    continuePuppetInfoPrinter
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
    pausePuppetInfoPrinter

    for ((;;)); do
        END_TIME=`date +%s`
        DURATION=$(( $END_TIME-$START_TIME))
        M=$(( $DURATION/60 ))
        S=$(( $DURATION%60 ))

        updateLine ${TIMER_LINE} "Elapsed time: "${M}"m "${S}"s"

        sleep 1
    done

    continuePuppetInfoPrinter
}

updateProgress() {
    CURRENT_STEP=$1
    LAST_STEP=14
    FACTOR=2

    local progress_number=$(( CURRENT_STEP*100/LAST_STEP ))

    local progress_field=
    for ((i=1; i<=CURRENT_STEP*FACTOR; i++));  do
       progress_field="${progress_field}="
    done

    progress_field=$(printf "[%-$(( LAST_STEP*FACTOR ))s]" ${progress_field})

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
}

updatePuppetInfo() {
    pauseTimer

    for ((;;)); do
        local line=$(sudo tail -n 1 /var/log/puppet/puppet-agent.log 2>/dev/null)
        if [[ -n "$line" ]]; then
            updateLine ${PUPPET_LINE} "[PUPPET: ${line:0:$(( ${DEPENDENCIES_STATUS_OFFSET}-8 ))}...]"     # print first (${DEPENDENCIES_STATUS_OFFSET}-7) symbols of line
        else
            updateLine ${PUPPET_LINE} ""
        fi
        sleep 1
    done

    continueTimer
}

printPostInstallInfo() {
    if [ -z ${SYSTEM_ADMIN_NAME} ]; then
        SYSTEM_ADMIN_NAME=`grep admin_ldap_user_name= ${CONFIG} | cut -d '=' -f2`
    fi

    if [ -z ${SYSTEM_ADMIN_PASSWORD} ]; then
        SYSTEM_ADMIN_PASSWORD=`grep system_ldap_password= ${CONFIG} | cut -d '=' -f2`
    fi

    if [ -z ${HOST_NAME} ]; then
        HOST_NAME=$(grep host_url\\s*=\\s*.* ${CONFIG} | sed 's/host_url\s*=\s*\(.*\)/\1/')
    fi

    println
    println "Codenvy is ready: $(printImportantLink "http://$HOST_NAME")."
    println "Admin user name:  $(printImportantInfo "$SYSTEM_ADMIN_NAME")"
    println "Admin password:   $(printImportantInfo "$SYSTEM_ADMIN_PASSWORD")"
    println
    println "!!! Set up DNS or add a hosts rule on your clients to reach this hostname."
}

postInstallationConfigure() {
    echo "export PATH=\$PATH:\$HOME/codenvy-im/codenvy-cli/bin" >> ${HOME}/.bashrc
    source ${HOME}/.bashrc
}

set -e
setRunOptions "$@"
printPreInstallInfo_${CODENVY_TYPE}

initFooterPosition

initTimer
runTimer

runPuppetInfoPrinter

doConfigureSystem
doInstallPackages
doInstallImCli

set +e

doDownloadBinaries
doInstallCodenvy

printPostInstallInfo

postInstallationConfigure