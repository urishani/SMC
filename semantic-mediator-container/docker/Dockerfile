# Start with tomcat based on Ubuntu
FROM tomcat
RUN apt-get update && apt-get install -y wget bash curl
RUN apt-get install -y wget bash curl graphviz
# we need graphviz for the dot command to be used by the SMC graphing diagrams

# If the tomcat-users.xml exists, you can customise that before making this image
# uncomment the next line if you have such a file.
#ADD tomcat-users.xml /usr/local/tomcat/conf

# Installing the SMC war file from the PSYMBIOSYS wiki pages. Download it first using your user/pw for this url:
# http://demos.txt.it:8096/intranet/wp3/wp3-platform/semantic-mediation-container/resources/smc-war-file/at_download/file
# Than, this command will work:
COPY dm.war /usr/local/tomcat/webapps

EXPOSE 8080

# smoketest bash script

# instruct docker to start the tomcat when launched:
ENTRYPOINT ["/usr/local/tomcat/bin/catalina.sh", "run"]

