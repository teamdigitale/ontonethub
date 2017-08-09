#!/bin/bash
if [ ! -f /usr/local/tomcat/webapps/stanbol.war ]; then
    cp ontonethub-war/target/stanbol.war /usr/local/tomcat/webapps/
fi
while :
do
	sleep 1
done