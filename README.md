# SMC
***Semantic Mediation Container*** is a web service tool to transform ontological (RDF) data sets based on their OWL ontologies and a bridging (OWL) ontology.
This tool is originated from IBM, develolped during the SPRINT, DANSE and PSYMBIOSYS EU projects and used mainly to provide interoperability among system engineering modeling tools. During the PSYMBIOSYS project, this tool is being migrated to the open source community to be hosted on this Github repository.

# Resources
The ***bin*** folder holds all binary resources that are described and used in the installation guide.

# Installation
There are several installation Options

The service works as a web service. A web-server is required to run it and that can be achieved in several ways.

1.  Install the war file on a web server such as Apache Tomcat.
2.  Do the same, but through a Docker container
3.  Execute it from a command line (cli) where an internal web server will be launched and execute the service.

The next sections describe each option in more details.

In addition, we now have a simple database of ontologies and models that are described in the product videos and presentations. It is nicer to start with that one, rather than the empty default one. To do that we use the MySMC.zip file resource, which simply needs to be unzip into the working directory of the server. That is easier if the service operates on your own workstation. Than define the system environment SMC_root=MySMC, and start the server.

## Install WAR file on a Web Server

The Apache Tomcat server (https://tomcat.apache.org/tomcat-8.0-doc/setup.html) can host and execute the SMC WAR file. Once the Web Server is installed, all you need is to copy the WAR file (dm.war) into the webapp/ folder at the Tomcat installation root.

Alternatively, the Tomcat manager application can be configured on the server and used to upload and install that dm.war file.

## Install (and run) SMC with Docker

The IBM Semantic Mediation Contaioner (SMC) is a war (web application resource) file that can be deployed on Tomcat. The following is an installation procedure using Docker virtual machine that can run on any platform, windows, or unix:

### Build (and run) your docker image

You can install SMC very easily using docker.

Follow these steps:

-    Update /etc/hosts to add the current machine as a hostname. Note: on mac or windows, no need for "sudo" in the lined below.

-    Install docker.

Start the docker daeomon - either during the docker installation, or with the command

     sudo docker daemon & # THIS NEEDS TO BE RUN ONLY ONCE, IN THE BACKGROUND 

Or you can use the "Docker Quickstart Terminal" application.

Navigate to a folder where you’d like the docker files to be placed.

Create a Dockerfile file with the following content (simply cut/paste that into the file and save it):

    # Start with tomcat based on Ubuntu
    FROM tomcat
    RUN apt-get update && apt-get install -y wget bash curl
    RUN apt-get install -y wget bash curl graphviz
    # we need graphviz for the dot command to be used by the SMC graphing diagrams

    # If the tomcat-users.xml exists, you can customise that before making this image
    # ADD tomcat-users.xml /usr/local/tomcat/conf

    # Installing the SMC war file from the PSYMBIOSYS wiki pages
    # Download the dm.war file from:
    # http://demos.txt.it:8096/intranet/wp3/wp3-platform/semantic-mediation-container/resources/smc-war-file/at_download/file
    ADD dm.war /usr/local/tomcat/webapps

    EXPOSE 8080 
    # instruct docker to start the tomcat when launched:
    ENTRYPOINT ["/usr/local/tomcat/bin/catalina.sh", "run"]

As commented in the quote above, download the dm.war file to the local directory from the PSYMBIOSYS wiki using this link: dm.war

Build the Docker Image from the Dockerfile:

     sudo docker build –t smc . 

You can view your images with the command

    docker images

Run the docker image you just generated:

     sudo docker run --privileged=true --name=smc --cap-add SYS_PTRACE -p 8080:8080 -it -d -v="/Users/shani/MySMC:/usr/local/tomcat/MySMC" -e="SMC_name=MySMC" smc

The option -e="SMC_name=MySMC" sets an environment variable which indicates the name in the dashboard title of the project being managed in this execution. Here, it is set to "MySMC". The default is "Database". In a new version (v2.03) this will also indicate a folder where the project is saved during the session and which can continue to be used in follow up image resumption or re-runs.

Another option you can use is -e="SMC_root=MySMCfolder" will distinguish the working folder from the project name. Default folder will be same as project name, as long as that name is a proper string for a file name. on the SMC dashboard, hovering over the project name will display folder path being used.
        
The -v parameter maps a folder in the host machine to the project folder so that its contents are saved also after the image executions stops. Assume you keep the project in a folder MySMC/ on your current file system, you can set this option: -v="/Users/shani/MySMC:/usr/local/tomcat/MySMC". The image will continue to use the same project data and configuration over multiple restarts.

Note: On Mac, path should be within /Users, on Windows, it should be in C:\Users (which on -v option should be specified as /c/Users. See more here: https://docs.docker.com/engine/userguide/containers/dockervolumes/

View the running images with this command, and possibly run a bash terminal inside it:

     sudo docker ps # That will provide <container_id> for the proton image. 
     sudo docker exec -it <container_id> bash

Try it.
Find the correct <ip> of your docker container on your network and try http://<ip>:8080. That will display the tomcat welcome screen.
To see the SMC dashboard go with your browser to this URL: http://<ip>:8080/dm/sm

You can simply install Tomcat and install dm.war using it management web interface, or simply save the dm.war file in the /webapps folder under its main installation folder. When you run tomcat, the SMC application will be instantiated.

### Platform considerations

#### Windows: 
Do not use the "sudo" prefix to the above commands.

#### Mac:

No need to use the "sudo" prefix to the commands above when user had administrator priviledges.
Use boot2docker - Set it up, than initialize environment shell variables:

     boot2docker up
     eval "$(boot2docker shellinit)";

Than, you can use the docker command as above.
The IP of the docker container VM can be obtained with

    boot2docker ip

After starting (running) the image on the docker vm, you can check the tomcat with:

    open "http://$(boot2docker ip):8080"

Exporting 8080 port on the host machine: To use the server over the network required to forward the 8080 port from your host. To expose this port on your mac and forward it to the docker VM do this:

    VBoxManage controlvm "boot2docker-vm" natpf1 "tcp-port8080,tcp,,8080,,8080";

You can try it with the open command:

    open "http://localhost:8080"

Which will open up the tomcat opening page.
    Note:Best way to address the service is with a real IP name so that SMC record a proper host name for its managed RDF resources.

## Run from a command line (CLI)

To execute from a command line, unzip the WAR file, add the dm-classes.jar file resource, and execute as a java application.

Create a new folder and copy the dm.war and dm-classes.jar file into it. Change directory to that working folder.

    unzip dm.war file

    jar -xvf dm.war

Optionally, you can also unzip the MySMC.zip file here so that you have a simple database to start play with

    jar -xvf MySMC.zip

Execute the java application

    java -jar dm-classes.jar

# Tutorials

## SMC Demonstration and Tutorial Videos:
### Brief Overview

A mediation demonstration (from the PSYMBIOSYS Youtube channel): https://www.youtube.com/watch?v=4ZkwSUSsxEg
Detailed tutorials in 7 classes

**Demo 1:** Installation of SMC on Docker: https://youtu.be/X0v-JcMncN4
**Demo 2:** Working with the SMC - An Overview: https://youtu.be/zBCJJ_UcO2k
**Demo 3:** Working with SMC - Creating Ontologies and edithing with Protege: https://youtu.be/2URWxyqgjXs
**Demo 4:** Working with SMC - Creating Rules ontology and editing it with Protege: https://youtu.be/OcZQLtcFMPU
**Demo 5:** Working with SMC - Creating Repositories, Tools and Mediators: https://youtu.be/hNoYUgqxWkc
**Demo 6:** Working with SMC - Editing an RDF model in a Tool over the web: https://youtu.be/6j_45twP9Tw
**Demo 7:** Working with SMC - Performing a mediation from a Tool and Tracing it: https://youtu.be/KvITTl-wJ6E
**Demo 7:** Working with SMC - Performing a mediation from a Tool and Tracing it: https://youtu.be/KvITTl-wJ6E
**Demo 7:** Working with SMC - Performing a mediation from a Tool and Tracing it: https://youtu.be/KvITTl-wJ6E
**Demo 7:** Working with SMC - Performing a mediation from a Tool and Tracing it: https://youtu.be/KvITTl-wJ6E

### Tutorials from DANSE - FP7 EU project on System of Systems (SoS):

    1. A demonstration of semantic mediation among system design tools using the ibm semantic mediation container: Mediating UPDM-SysML-Modelica
    2. DANSE tools education webinars: PART 1 of the DANSE tool-net, explaining the role of semantic mediation to the collaborative eco system of design tools for SoS. The semantic mediator here is an earlier version based on the IBM Jazz tools platform: DANSE Tools G - Tool-Net 1.mp4 - 22 MB.ds
    3. Second part of the tools education webinar. Relevant to IBM SMC is the first part only: Tool-Net 2 DANSE Tools H - Tool-Net 2.mp4 - 33 MB
