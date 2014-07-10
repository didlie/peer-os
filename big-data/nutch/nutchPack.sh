#!/bin/bash
set -e
. /var/lib/jenkins/jobs/master.get_branch_repo/workspace/big-data/pack-funcs

productName=nutch
downloadFileAndMakeChanges() {
	initializeVariables $1

	tempDirectory=$BASE/$fileName/opt
	confDirectory=$BASE/$fileName/etc/nutch

	nutchVersion=1.8

	# Create directories that are required for the debian package
    mkdir -p $tempDirectory
    mkdir -p $confDirectory

	# download hbase which is compatible with hadoop1 version. 
	wget http://archive.apache.org/dist/nutch/$nutchVersion/apache-nutch-$nutchVersion-bin.tar.gz -P $tempDirectory
	pushd $tempDirectory
	tar -xzpf apache-nutch-*.tar.gz

	# remove tar file
	rm apache-nutch-*.tar.gz

	# rename folder --remove hadoop1 from file name --
	mv apache-nutch-* nutch-$nutchVersion
	
	# move configuration files 
	mv nutch-$nutchVersion/conf/* $confDirectory
	popd
}
# 1) Get the sources which are downloaded from version control system
#    to local machine to relevant directories to generate the debian package
getSourcesToRelevantDirectories $productName
# 2) Download tar file and make necessary changes
downloadFileAndMakeChanges $productName
# 3) Create the Debian package
generateDebianPackage $productName