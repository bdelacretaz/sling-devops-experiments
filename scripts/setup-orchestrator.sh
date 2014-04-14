#!/bin/bash -e

### Settings

# Common
source `dirname $0`/setup-common.sh

# Repository
dir_apps="apps"
dir_install="apps/install"

# Bundles (as maven dependencies)
bundles_orchestrator=(
	"org.apache.sling:org.apache.sling.devops.orchestrator:0.0.1-SNAPSHOT"
	)

### Functions

show_usage_and_exit() {
	echo "Usage:"
	echo "$0 host:port"
	echo "Example:"
	echo "$0 localhost:8080"
	exit 1
}

### Main

if [ "$#" != 1 -o "$1" = '-?' -o "$1" = '--help' ]
then
	show_usage_and_exit
fi

sling=http://$1

echo "-- Setting up Orchestrator on Sling instance at ${sling} --"

# check if jars exist
echo "Checking existence of jars..."
jars=$(parse_and_check_maven_artifacts ${bundles_common[@]} ${bundles_orchestrator[@]})

# create paths
echo "Creating repository paths..."
echo "  /${dir_apps} (if doesn't exist)"
curl -u ${login} -X MKCOL "${sling}/${dir_apps}" &> /dev/null
echo "  /${dir_install} (deleting and recreating)"
curl -u ${login} -X DELETE "${sling}/${dir_install}" &> /dev/null
curl -u ${login} -X MKCOL "${sling}/${dir_install}"

# copy jars
echo "Copying jars to /${dir_install}..."
copy_jars "${sling}/${dir_install}/" ${jars[@]}

echo "Done!"
