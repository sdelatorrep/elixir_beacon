#!/bin/sh
# Copyright Abandoned 1996 TCX DataKonsult AB & Monty Program KB & Detron HB
# This file is public domain and comes with NO WARRANTY of any kind

# Comments to support LSB init script conventions
### BEGIN INIT INFO
# Provides: wfhandler
# Required-Start: $local_fs $network $remote_fs
# Should-Start: ypbind nscd ldap ntpd xntpd
# Required-Stop: $local_fs $network $remote_fs
# Default-Start:  3 5
# Default-Stop: 0 1 2 4 6
# Short-Description: start and stop wfhandler
# Description: wfhandler
### END INIT INFO

NAME="elixirbeacon"
SERVICE="${NAME}service"
DIR="/microservices/${SERVICE}/test/"
JARFile="${DIR}${NAME}-service.jar"
PIDFile="${DIR}application.pid"
SPRING_OPTS="--spring.profiles.active=test"
JMX_OPTS=" -XX:+UseParNewGC -agentpath:/home/omartinez/profiling/yjp-2015-build-15064/bin/linux-x86-64/libyjpagent.so=dir=/home/omartinez/yourkit/snapshots/,logdir=/home/omartinez/yourkit/logs/,port=10980 "
EXEC_USER="${SERVICE}"

function check_if_pid_file_exists {
    if [ ! -f $PIDFile ]
    then
 echo "PID file not found: $PIDFile"
        exit 1
    fi
}

function check_if_process_is_running {
 if ps -p $(print_process) > /dev/null
 then
     return 0
 else
     return 1
 fi
}

function print_process {
    echo $(<"$PIDFile")
}

cd $DIR

case "$1" in
  status)
    check_if_pid_file_exists
    if check_if_process_is_running
    then
      echo $(print_process)" is running"
    else
      echo "Process not running: $(print_process)"
    fi
    ;;
  stop)
    check_if_pid_file_exists
    if ! check_if_process_is_running
    then
      echo "Process $(print_process) already stopped"
      exit 0
    fi
    kill -TERM $(print_process)
    echo -ne "Waiting for process to stop"
    NOT_KILLED=1
    for i in {1..20}; do
      if check_if_process_is_running
      then
        echo -ne "."
        sleep 1
      else
        NOT_KILLED=0
      fi
    done
    echo
    if [ $NOT_KILLED = 1 ]
    then
      echo "Cannot kill process $(print_process)"
      exit 1
    fi
    echo "Process stopped"
    ;;
  start)
    if [ -f $PIDFile ] && check_if_process_is_running
    then
      echo "Process $(print_process) already running"
      exit 1
    fi
    su - $EXEC_USER -s "/bin/bash" -c "cd $DIR && nohup /software/java/latest/bin/java $JMX_OPTS -jar $JARFile $SPRING_OPTS &"
    echo "Process started"
    ;;
  restart)
    $0 stop
    if [ $? = 1 ]
    then
      exit 1
    fi
    $0 start
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
esac

exit 0
