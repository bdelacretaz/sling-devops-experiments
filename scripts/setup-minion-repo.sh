#!/bin/sh -e

### Settings

# Common
source `dirname $0`/setup-common.sh

# Bundles (as maven dependencies)
bundles_minion=(
	"org.apache.sling:org.apache.sling.hc.core:1.1.0"
	"org.apache.sling:org.apache.sling.junit.core:1.0.8"
	"org.apache.sling:org.apache.sling.junit.healthcheck:1.0.6"
	"org.apache.sling:org.apache.sling.hc.webconsole:1.1.0" # useful
	"org.apache.sling:org.apache.sling.devops.minion:0.0.1-SNAPSHOT"
	)

### Functions

show_usage_and_exit() {
	echo "Usage:"
	echo "$0 config host:port"
	echo "Example:"
	echo "$0 C1 localhost:8080"
	exit 1
}

### Main

if [ "$#" != 2 -o "$1" = '-?' -o "$1" = '--help' ]
then
	show_usage_and_exit
fi

config=$1
sling=http://$2

echo "-- Setting up repository for config ${config} via Sling instance at ${sling} --"

# check if jars exist
echo "Checking existence of jars..."
jars=$(parse_and_check_maven_artifacts ${bundles_common[@]} ${bundles_minion[@]})

# create paths
echo "Creating repository paths..."
echo "  /${dir_cfg} (if doesn't exist)"
curl -u ${login} -X MKCOL "${sling}/${dir_cfg}" &> /dev/null
path="${dir_cfg}/${config}"
echo "  /${path} (deleting and recreating)"
curl -u ${login} -X DELETE "${sling}/${path}" &> /dev/null
curl -u ${login} -X MKCOL "${sling}/${path}"

for dir in ${dirs[@]} "${dir_install}" # e.g. /sling-cfg/C1/apps, as well as the install dir
do
	echo "  /${path}/${dir}"
	curl -u ${login} -X MKCOL "${sling}/${path}/${dir}"
done

# copy jars
echo "Copying jars to /${path}/${dir_install}..."
copy_jars "${sling}/${path}/${dir_install}/" ${jars[@]}

echo "Done!"
