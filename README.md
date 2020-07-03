# SMC
![GitHub release](https://img.shields.io/github/release/qubyte/rubidium.svg)
[![License badge](https://img.shields.io/hexpm/l/plug.svg)](https://opensource.org/licenses/Apache-2.0)
[![Docker badge](https://img.shields.io/docker/pulls/urishani/semanticmediationcontainer.svg)](https://hub.docker.com/r/urishani/semanticmediationcontainer/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](http://stackoverflow.com/questions/tagged/semantic-mediation-container)

the ***Semantic Mediation Container*** 
IBM Semantic Mediation Container is an open source semantic mediation engine developed at IBM Research - Haifa. The short name is SMC, and it provides API services to transform RDF models having OWL ontologies from one ontology to another. 

This project was developed through 3 EU FP7 and H2020 projects, in particularly [SPRINT](http://www.sprint-iot.eu/), [DANSE](http://www.danse-ip.eu/home) and [PSYMBIOSYS](http://www.psymbiosys.eu/)

As a platform, it will hold OWL ontologies and associated [RDF](https://www.w3.org/RDF/) models for them, as well as [OWL](https://www.w3.org/2007/OWL/wiki/OWL_Working_Group) "bridge" ontologies which define the rules by which the semantic mediation engine transforms the models. RDF models stored on the SMC platform are served as OSLC service providers for linking and accessing their contents, as well as performing interactive SPARQL queries over them. Engineering design tools with the proper adapters can export their managed models in RDF to the platform and these export can trigger an automatic model transformation to another RDF model. Tools can also import an RDF model from the platform into a managed model by the tool. An example of such mediation is implemented for a SysML tool (IBM Rhapsody) and a Modelica tool so they could exchange models, modify the models and share it again, continuously.

Web GUI interface allows to manage the contents of the SMC which can consist of OWL ontologies and RDF models from design tools. The GUI tools will also display interactively RDF graphs of the models, or table views of their elements and properties, create new resources or alter existing resources in these graphs, edit OWL ontologies with associated [Protege](https://protegewiki.stanford.edu/wiki/Main_Page) plugin, perform [SPARQL](https://www.w3.org/TR/sparql11-query/) queries, and test mediation steps.

[OSLC](https://open-services.net/specifications/) API allows to access according to the OSLC standard the resources within RDF graphs stored on the platform.

Semantic mediation API allows the simple submission of RDF models for a mediation transformation with immediate return of a resulting RDF transformed graph. The API supported by the platform is documented with [SWAGGER](https://swagger.io/specification/) style web GUI.

The binary resoruces of the sorftware are packages as a web WAR file, and can be launched by web servers such as Tomcat. A Docker container can be easily build with an Ubuntu and Tomcat images, and a ready made such image is also stored over the docker hub repository.
Plugins and adapters for the IBM Rhapsody design tool and for Protege are also included.


## Table of Contents
- [Resources](#resources)
- [Installation](#installation)
  * [Install WAR file on a Web Server](#install-war)
  * [Install (and run) SMC with Docker](#install-docker)
    * [Build (and run) your docker image](#build-run)
    * [Platform considerations](#platform-considerations)
      * [Windows](#windows)
      * [Mac](#mac)
- [Tutorials](#tutorials)
  * [SMC Demonstration and Tutorial Videos](#tutorial-video)
    * [Brief Overview](#brief-overview)
    * [Detailed tutorials in 7 classes](#7-classes)
  * [Tutorials from DANSE - FP7 EU project on System of Systems (SoS)](#danse)

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

## Install WAR file on a Web Server <a name="install-war"></a>

The Apache Tomcat server (https://tomcat.apache.org/tomcat-8.0-doc/setup.html) can host and execute the SMC WAR file. Once the Web Server is installed, all you need is to copy the WAR file (dm.war) into the webapp/ folder at the Tomcat installation root.

Alternatively, the Tomcat manager application can be configured on the server and used to upload and install that dm.war file.

## Install (and run) SMC with Docker <a name="install-docker"></a>

The IBM Semantic Mediation Contaioner (SMC) is a war (web application resource) file that can be deployed on Tomcat. The following is an installation procedure using Docker virtual machine that can run on any platform, windows, or unix:

### Build (and run) your docker image <a name="build-run"></a>

You can install SMC very easily using docker.

Follow these steps:

-    Update /etc/hosts to add the current machine as a hostname. Note: on mac or windows, no need for "sudo" in the lines below.

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

    # Installing the SMC war file from github:
    # https://github.com/urishani/SMC/blob/master/bin/dm.war
    
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

#### Windows 
Do not use the "sudo" prefix to the above commands.

#### Mac

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

## Run from a command line (CLI) <a name="run-cli"></a>

To execute from a command line, unzip the WAR file, add the dm-classes.jar file resource, and execute as a java application.

Create a new folder and copy the dm.war and dm-classes.jar file into it. Change directory to that working folder.

    unzip dm.war file

    jar -xvf dm.war

Optionally, you can also unzip the MySMC.zip file here so that you have a simple database to start play with

    jar -xvf MySMC.zip

Execute the java application

    java -jar dm-classes.jar

# Tutorials

## SMC Demonstration and Tutorial Videos  <a name="tutorial-video"></a>
### Brief Overview

A mediation demonstration (from the PSYMBIOSYS Youtube channel): https://www.youtube.com/watch?v=4ZkwSUSsxEg

### Detailed tutorials in 7 classes <a name="7-classes"></a>

   1. Installation of SMC on Docker: https://youtu.be/X0v-JcMncN4
   2. Working with the SMC - An Overview: https://youtu.be/zBCJJ_UcO2k
   3. Working with SMC - Creating Ontologies and edithing with Protege: https://youtu.be/2URWxyqgjXs
   4. Working with SMC - Creating Rules ontology and editing it with Protege: https://youtu.be/OcZQLtcFMPU
   5. Working with SMC - Creating Repositories, Tools and Mediators: https://youtu.be/hNoYUgqxWkc
   6. Working with SMC - Editing an RDF model in a Tool over the web: https://youtu.be/6j_45twP9Tw
   7. Working with SMC - Performing a mediation from a Tool and Tracing it: https://youtu.be/KvITTl-wJ6E
   8. Working with SMC - Performing a mediation from a Tool and Tracing it: https://youtu.be/KvITTl-wJ6E
   9. Working with SMC - Performing a mediation from a Tool and Tracing it: https://youtu.be/KvITTl-wJ6E
   10. Working with SMC - Performing a mediation from a Tool and Tracing it: https://youtu.be/KvITTl-wJ6E

## Tutorials from DANSE - FP7 EU project on System of Systems (SoS) <a name="danse"></a>

1. A demonstration of semantic mediation among system design tools using the ibm semantic mediation container: [Mediating UPDM-SysML-Modelica](https://www.youtube.com/watch?v=SAEzSqsARC4)
2. DANSE tools education webinars: PART 1 of the DANSE tool-net, explaining the role of semantic mediation to the collaborative eco system of design tools for SoS. The semantic mediator here is an earlier version based on the IBM Jazz tools platform: [DANSE Tools G - Tool-Net 1.mp4 - 22 MB.ds](http://danse-ip.eu/home/mp4/danse%20tools%20g%20-%20tool-net%201342b.mp4?download=157:danse-tool-training-webinar-g)
3. Second part of the tools education webinar. Relevant to IBM SMC is the first part only: [Tool-Net 2 DANSE Tools H - Tool-Net 2.mp4 - 33 MB](http://danse-ip.eu/home/mp4/danse%20tools%20h%20-%20tool-net%2021824.mp4?download=158:danse-tool-training-webinar-h)
