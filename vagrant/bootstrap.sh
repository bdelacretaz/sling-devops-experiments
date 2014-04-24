#!/bin/bash

# Install Ubuntu packages
apt-get update
apt-get install -y openjdk-7-jdk curl
apt-get remove -y openjdk-6-jre-headless

# Download and unpack Maven (because Ubuntu's package has too many unnecessary dependencies)
if ! (ls apache-maven-3.2.1/bin/mvn &> /dev/null)
then
	rm -rf apache-maven*
	wget http://mirror.switch.ch/mirror/apache/dist/maven/binaries/apache-maven-3.2.1-bin.tar.gz
	tar -xzf apache-maven-3.2.1-bin.tar.gz
fi

# Build DevOps bundles
apache-maven-3.2.1/bin/mvn clean install

# Sling: kill running Sling, if any
if [ -f sling.pid ]
then
	kill -9 $(cat sling.pid)
	rm -f sling.pid
fi
rm -rf sling
