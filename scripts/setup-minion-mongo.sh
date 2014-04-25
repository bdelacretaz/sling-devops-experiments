#!/bin/bash -e

. $(dirname $0)/setup-common.sh

pid_document_nodestore_service="org.apache.jackrabbit.oak.plugins.document.DocumentNodeStoreService"

### Functions

show_usage_and_exit() {
	echo "Usage:"
	echo "$0 mongohost:port slinghost:port"
	echo "Example:"
	echo "$0 localhost:27017 localhost:8080"
	exit 1
}

### Main

if [ "$#" != 2 -o "$1" = '-?' -o "$1" = '--help' ]
then
	show_usage_and_exit
fi

mongo_uri=mongodb://$1
sling=http://$2

echo "Configuring ${pid_document_nodestore_service}..."
curl -sSu ${login} -X POST -d "mongouri=${mongo_uri}&propertylist=mongouri&apply=true&action=ajaxConfigManager" "${sling}/system/console/configMgr/${pid_document_nodestore_service}"
echo "Sleeping 10s..."
sleep 10s
