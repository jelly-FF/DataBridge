#!/bin/bash
# pick a java
JAVA=/usr/bin/java
NAME=databridge

# Gets the directory of the script
SCRIPT_HOME="${BASH_SOURCE[0]}";
if ([ -h "${SCRIPT_HOME}" ]) then
  while([ -h "${SCRIPT_HOME}" ]) do SCRIPT_HOME=`readlink "${SCRIPT_HOME}"`; done
fi
pushd . > /dev/null
cd `dirname ${SCRIPT_HOME}` > /dev/null
SCRIPT_HOME=`pwd`;
popd  > /dev/null

# DATABRIDGE_HOME is the next directory above this script
DATABRIDGE_HOME="${SCRIPT_HOME%/*}"

# The Databridge bin directory
DATABRIDGE_BIN=${DATABRIDGE_HOME}/bin/

# System jars have to be here
DATABRIDGE_SYSTEM_JARS=${DATABRIDGE_HOME}/bin/system

# Contributed jars have to be here
DATABRIDGE_CONTRIBUTED_JARS=${DATABRIDGE_HOME}/bin/contrib

# Get the DATABRIDGE config dir
DATABRIDGE_CONFIG_DIR=${DATABRIDGE_HOME}/config

# Get the DATABRIDGE run dir
DATABRIDGE_RUN_DIR=${DATABRIDGE_HOME}/run

# Get the DATABRIDGE log dir
DATABRIDGE_LOG_DIR=${DATABRIDGE_HOME}/log

ingest_pid_file=${DATABRIDGE_RUN_DIR}/ingest.pid
relevance_pid_file=${DATABRIDGE_RUN_DIR}/relevance.pid
network_pid_file=${DATABRIDGE_RUN_DIR}/network.pid

ingest_stdout_file=${DATABRIDGE_LOG_DIR}/ingest.log
relevance_stdout_file=${DATABRIDGE_LOG_DIR}/relevance.log
network_stdout_file=${DATABRIDGE_LOG_DIR}/network.log

ingest_stderr_file=${DATABRIDGE_LOG_DIR}/ingest.log
relevance_stderr_file=${DATABRIDGE_LOG_DIR}/relevance.log
network_stderr_file=${DATABRIDGE_LOG_DIR}/network.log

get_ingest_pid() {
   cat ${ingest_pid_file}
}

get_relevance_pid() {
   cat ${relevance_pid_file}
}

get_network_pid() {
   cat ${network_pid_file}
}

is_ingest_running() {
    [ -f "$ingest_pid_file" ] && ps `get_ingest_pid` > /dev/null 2>&1
}

is_relevance_running() {
    [ -f "$relevance_pid_file" ] && ps `get_relevance_pid` > /dev/null 2>&1
}

is_network_running() {
    [ -f "$network_pid_file" ] && ps `get_network_pid` > /dev/null 2>&1
}

is_running() {
    is_ingest_running && is_relevance_running && is_network_running
}
# Find the config file
DATABRIDGE_CONFIG_FILE=${DATABRIDGE_CONFIG_DIR}/DataBridge.conf

# include the classpath files for the various servers.
if [ -f ${DATABRIDGE_CONFIG_DIR}/networkEngineClasspath.txt ]
then
   rawNetworkFile=${DATABRIDGE_CONFIG_DIR}/networkEngineClasspath.txt
   rawNetworkPath=$(head -1 $rawNetworkFile)
   NETWORK_ENGINE_CLASS_PATH=${rawNetworkPath}:$DATABRIDGE_SYSTEM_JARS/*:/$DATABRIDGE_CONTRIBUTED_JARS/*
else
   echo "Can't find networkEngineClasspath.txt" 
   echo "Try mvn dependency:build-classpath -Dmdep.outputFile=networkEngineClasspath.txt"
   echo "in engines/network"
fi

if [ -f ${DATABRIDGE_CONFIG_DIR}/relevanceEngineClasspath.txt ]
then
   rawRelevanceFile=${DATABRIDGE_CONFIG_DIR}/relevanceEngineClasspath.txt
   rawRelevancePath=$(head -1 $rawRelevanceFile)
   #RELEVANCE_ENGINE_CLASS_PATH=\"${rawRelevancePath}:$DATABRIDGE_SYSTEM_JARS/*:$DATABRIDGE_CONTRIBUTED_JARS/*\"
   RELEVANCE_ENGINE_CLASS_PATH=${rawRelevancePath}:$DATABRIDGE_SYSTEM_JARS/*:$DATABRIDGE_CONTRIBUTED_JARS/*
else
   echo "Can't find relevanceEngineClasspath.txt" 
   echo "Try mvn dependency:build-classpath -Dmdep.outputFile=relevanceEngineClasspath.txt"
   echo "in engines/relevance"
fi

if [ -f ${DATABRIDGE_CONFIG_DIR}/ingestEngineClasspath.txt ]
then
   rawIngestFile=${DATABRIDGE_CONFIG_DIR}/ingestEngineClasspath.txt
   rawIngestPath=$(head -1 $rawIngestFile)
   INGEST_ENGINE_CLASS_PATH=${rawIngestPath}:$DATABRIDGE_SYSTEM_JARS/*:$DATABRIDGE_CONTRIBUTED_JARS/*
else
   echo "Can't find ingestEngineClasspath.txt" 
   echo "Try mvn dependency:build-classpath -Dmdep.outputFile=ingestEngineClasspath.txt"
   echo "in engines/ingest"
fi

start_ingest() {
    if is_ingest_running; then
        echo "Ingest engine already running"
    else
        echo "Starting ingest engine"
        cd "$DATABRIDGE_BIN"
        echo "Ingest engine startup at "`date -u` >> ${ingest_stdout_file}
        $JAVA -cp ${INGEST_ENGINE_CLASS_PATH} org.renci.databridge.engines.ingest.IngestEngine ${DATABRIDGE_CONFIG_DIR}/DataBridge.conf >> "$ingest_stdout_file" 2>> "$ingest_stderr_file" &
        echo $! > "$ingest_pid_file"
        if ! is_ingest_running; then
            echo "Unable to start ingest engine, see $ingest_stdout_file and $ingest_stderr_file"
            exit 1
        fi
    fi
}

stop_ingest() {
    # Stop the ingest engine
    if is_ingest_running; then
        echo -n "Stopping ingest engine.."
        kill `get_ingest_pid`
        for i in {1..10}
        do
            if ! is_ingest_running; then
                break
            fi

            echo -n "."
            sleep 1
        done
        echo

        if is_ingest_running; then
            echo "Not stopped; may still be shutting down or shutdown may have failed"
            exit 1
        else
            echo "Stopped"
            if [ -f "$ingest_pid_file" ]; then
                rm -f "$ingest_pid_file"
            fi
        fi
    else
        echo "Ingest engine not running"
    fi
}

start_relevance() {
    if is_relevance_running; then
        echo "Relevance engine already running"
    else
        echo "Starting relevance engine"
        cd "$DATABRIDGE_BIN"
        echo "Relevance engine startup at "`date -u` >> ${relevance_stdout_file}
        $JAVA -cp ${RELEVANCE_ENGINE_CLASS_PATH} org.renci.databridge.engines.relevance.RelevanceEngine ${DATABRIDGE_CONFIG_DIR}/DataBridge.conf >> "$relevance_stdout_file" 2>> "$relevance_stderr_file" &
        echo $! > "$relevance_pid_file"
        if ! is_relevance_running; then
            echo "Unable to start relevance engine, see $relevance_stdout_file and $relevance_stderr_file"
            exit 1
        fi
    fi
}

stop_relevance() {
    if is_relevance_running; then
        echo -n "Stopping relevance engine.."
        kill `get_relevance_pid`
        for i in {1..10}
        do
            if ! is_relevance_running; then
                break
            fi

            echo -n "."
            sleep 1
        done
        echo

        if is_relevance_running; then
            echo "Not stopped; may still be shutting down or shutdown may have failed"
            exit 1
        else
            echo "Stopped"
            if [ -f "$relevance_pid_file" ]; then
                rm -f "$relevance_pid_file"
            fi
        fi
    else
        echo "Relevance engine not running"
    fi
}

start_network() {
    if is_network_running; then
        echo "Network engine already running"
    else
        echo "Starting network engine"
        cd "$DATABRIDGE_BIN"
        echo "Network engine startup at "`date -u` >> ${network_stdout_file}
        $JAVA -cp ${NETWORK_ENGINE_CLASS_PATH} org.renci.databridge.engines.network.NetworkEngine ${DATABRIDGE_CONFIG_DIR}/DataBridge.conf >> "$network_stdout_file" 2>> "$network_stderr_file" &
        echo $! > "$network_pid_file"
        if ! is_network_running; then
            echo "Unable to start network engine, see $network_stdout_file and $network_stderr_file"
            exit 1
        fi
    fi
}

stop_network() {
    # Stop the network engine
    if is_network_running; then
        echo -n "Stopping network engine.."
        kill `get_network_pid`
        for i in {1..10}
        do
            if ! is_network_running; then
                break
            fi

            echo -n "."
            sleep 1
        done
        echo

        if is_network_running; then
            echo "Not stopped; may still be shutting down or shutdown may have failed"
            exit 1
        else
            echo "Stopped"
            if [ -f "$network_pid_file" ]; then
                rm -f "$network_pid_file"
            fi
        fi
    else
        echo "Network engine not running"
    fi
}

if [ "$#" -ge 2 ]; then
    target=$2
else
    target=all
fi

case "$1" in
    start)
       if [ "$target" == "all" ] || [ $target == "ingest" ]; then
          start_ingest
       fi

       if [ "$target" == "all" ] || [ $target == "relevance" ]; then
          start_relevance
       fi

       if [ "$target" == "all" ] || [ $target == "network" ]; then
          start_network
       fi
    ;;

    stop)
       if [ "$target" == "all" ] || [ $target == "ingest" ]; then
          stop_ingest
       fi
       if [ "$target" == "all" ] || [ $target == "relevance" ]; then
          stop_relevance
       fi
       if [ "$target" == "all" ] || [ $target == "network" ]; then
          stop_network
       fi
    ;;

    restart)
       $0 stop $target
       if is_running; then
           echo "Unable to stop, will not attempt to start"
           exit 1
       fi
       $0 start $target
    ;;

    status)
       if [ "$target" == "all" ] || [ $target == "ingest" ]; then
          if is_ingest_running; then
              echo "Ingest engine running"
          else
              echo "Ingest engine stopped"
          fi
       fi
       if [ "$target" == "all" ] || [ $target == "relevance" ]; then
          if is_relevance_running; then
              echo "Relevance engine running"
          else
              echo "Relevance engine stopped"
          fi
       fi
       if [ "$target" == "all" ] || [ $target == "network" ]; then
          if is_network_running; then
              echo "Network engine running"
          else
              echo "Network engine stopped"
          fi
       fi
    ;;
    *)
    echo "Usage: $0 {start|stop|restart|status} {all(default)|ingest|relevance|network}"
    exit 1
    ;;
esac

exit 0
