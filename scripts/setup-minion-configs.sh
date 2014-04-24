#!/bin/bash -e

### Settings

# Common
. $(dirname $0)/setup-common.sh

# Components
# OS X comes with bash 3.2, so no associative arrays - mimicking by
# variables like array_mykey instead of array[mykey]
components=("jcr_installer" "jcr_resource_resolver_factory") # order matters for some reason
pid_jcr_resource_resolver_factory="org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl"
pid_jcr_installer="org.apache.sling.installer.provider.jcr.impl.JcrInstaller"
prop_jcr_resource_resolver_factory="resource.resolver.searchpath"
prop_jcr_installer="sling.jcrinstall.search.path"
extra_apps_jcr_installer=":200" # JCR Installer requires path weights...
extra_libs_jcr_installer=":100"

# Requests
req_path_components="system/console/components"
req_path_configMgr="system/console/configMgr"
req_configMgr_common="apply=true&action=ajaxConfigManager"
req_sleep="2s"

### Functions

show_usage_and_exit() {
	echo "Usage:"
	echo "$0 config host1:port1 [host2:port2 [host3:port3 [...]]]"
	echo "Example:"
	echo "$0 C1 localhost:8080 localhost:8082"
	exit 1
}

### Main

if [ "$#" -lt 2 -o "$1" = '-?' -o "$1" = '--help' ]
then
	show_usage_and_exit
fi

config=$1
shift
slings=()
for sling in $@
do
	slings+=("http://${sling}")
done

# configure each instance
for sling in ${slings[@]}
do
	echo "Configuring ${sling}..."
	
	# configure each component on instance
	for component in ${components[@]}
	do
		pidvar=pid_${component}
		propvar=prop_${component}
		echo "  ${!pidvar}"
		
		# stop component first
		curl -silent -u ${login} -X POST -d "action=disable" "${sling}/${req_path_components}/${!pidvar}/${!pidvar}" > /dev/null
		sleep ${req_sleep}
		
		# build request string and make request
		req="${req_configMgr_common}&propertylist=${!propvar}"
		for dir in ${dirs[@]}
		do
			extravar=extra_${dir}_${component}
			req="${req}&${!propvar}=/${dir_cfg}/${config}/${dir}${!extravar}"
		done
		curl -u ${login} -X POST -d "${req}" "${sling}/${req_path_configMgr}/${!pidvar}"
		sleep ${req_sleep}
		
		# start component
		curl -silent -u ${login} -X POST -d "action=enable" "${sling}/${req_path_components}/${!pidvar}/${!pidvar}" > /dev/null
		sleep ${req_sleep}
	done

	# set servlet resolver cacheSize to 0 (otherwise esp scripts can only be run once)
	pid_servlet_resolver="org.apache.sling.servlets.resolver.SlingServletResolver"
	prop_servlet_resolver="servletresolver.cacheSize"
	echo "  ${pid_servlet_resolver}"
	curl -u ${login} -X POST -d "${req_configMgr_common}&propertylist=${prop_servlet_resolver}&${prop_servlet_resolver}=0" "${sling}/${req_path_configMgr}/${pid_servlet_resolver}"
	sleep ${req_sleep}
done

echo "Done!"
