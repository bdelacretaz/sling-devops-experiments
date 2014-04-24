#!/bin/bash -e

### Settings

# Common
. $(dirname $0)/setup-common.sh

# Bundles (as maven dependencies)
bundles_minion=(
	"org.apache.sling:org.apache.sling.hc.core:1.1.0"
	"org.apache.sling:org.apache.sling.junit.core:1.0.8"
	"org.apache.sling:org.apache.sling.junit.healthcheck:1.0.6"
	"org.apache.sling:org.apache.sling.hc.webconsole:1.1.0" # useful
	"org.apache.sling:org.apache.sling.devops.minion:0.0.1-SNAPSHOT"
	)

# Resources
bundle_minion_C1="$(dirname $0)/../resources/org.apache.sling.samples.test-0.0.1.jar"
bundle_minion_C2="$(dirname $0)/../resources/org.apache.sling.samples.test-0.0.2.jar"
script_minion_C1="$(dirname $0)/../resources/test1.esp"
script_minion_C2="$(dirname $0)/../resources/test2.esp"
script_extension="test"
testresource_type="foo/bar"
testresource_path="content/mynode"

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

# check if artifacts exist
echo "Checking existence of artifacts..."
jars=$(parse_and_check_maven_artifacts ${bundles_common[@]} ${bundles_minion[@]})

# add config-determined bundle
config_bundle_var=bundle_minion_${config}
jars+=("${!config_bundle_var}")

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

# create test content
curl -u ${login} -F"sling:resourceType=${testresource_type}" "${sling}/${testresource_path}" &> /dev/null

# copy test script
path+="/${dirs[0]}"
config_script_var=script_minion_${config}
for pathpiece in $(echo ${testresource_type} | tr '/' ' ')
do
	path+="/${pathpiece}"
	curl -u ${login} -X MKCOL "${sling}/${path}" &> /dev/null
done
echo "Copying ${!config_script_var} to /${path}/${script_extension}.esp..."
curl -u ${login} -X DELETE "${sling}/${path}/${script_extension}.esp" &> /dev/null
curl -u ${login} -T ${!config_script_var} "${sling}/${path}/${script_extension}.esp"

echo "Done!"
