# SMC - Semantic Mediation Container

[![License badge](https://img.shields.io/hexpm/l/plug.svg)](https://opensource.org/licenses/Apache-2.0)
[![Docker badge](https://img.shields.io/docker/pulls/urishani/semanticmediationcontainer.svg)](https://hub.docker.com/r/smc/semanticmediationcontainer/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](http://stackoverflow.com/questions/tagged/semantic-mediation-container)

* [Introduction](#introduction)
* [Overall description](#overall-description)
* [Build and Install](#build-and-install)
* [API Reference Documentation](#api-reference-documentation)
* [License](#license)
* [Support](#support)

## Introduction
IBM Semantic Mediation Container is an open source semantic mediation engine developed at IBM Research - Haifa. The short name is SMC, and it provides API services to transform RDF models having OWL ontologies from one ontology to another. As a platform, it will hold OWL ontologies and associated RDF models for them, as well as OWL "bridge" ontologies which define the rules by which the semantic mediation engine transforms the models. RDF models stored on the SMC platform are served as OSLC service providers for linking and accessing their contents, as well as performing interactive SPARQL queries over them. Engineering design tools with the proper adapters can export their managed models in RDF to the platform and these export can trigger an automatic model transformation to another RDF model. Tools can also import an RDF model from the platform into a managed model by the tool. An example of such mediation is impleented for a SysML tool (IBM Rhapsody) and a Modelica tool so they could exchange models, modify the models and share it again, continuously.

Web GUI interface allows to manage the contents of the SMC which can consist of OWL ontologies and RDF models from design tools. The GUI tools will also display interactively RDF graphs of the models, or table views of their elements and properties, create new resources or alter existing resources in these graphs, edit OWL ontologies with associated Protege plugin, perform SPARQL queries, and test mediation steps.

OSLC API allows to access according to the OSLC standard the resources within RDF graphs stored on the platform.

Semantic mediation API allows the simple submission of RDF models for a mediation transformation with immediate return of a resulting RDF transformed graph. The API supported by the platform is documented with SWAGGER style web GUI.

The binary resoruces of the sorftware are packages as a web WAR file, and can be launched by web servers such as Tomcat. A Docker container can be easily build with an Ubuntu and Tomcat images, and a ready made such image is also stored over the docker hub repository.
Plugins and adapters for the IBM Rhapsody design tool and for Protege are also included.

[Top](#smc-semantic-mediation-container)

## Overall description

This project was developed through 3 EU HP7 and H2020 projects, in particularly SPRINT ([http://www.sprint-iot.eu/] (http://www.sprint-iot.eu/)), DANSE ([http://www.danse-ip.eu/home] (http://www.danse-ip.eu/home)) and PSYMBIOSYS.

* [For more information about this project see] (http://catalogue.fiware.org/enablers/complex-event-processing-cep-proactive-technology-online) - The FIWARE catalogue. 
  * In the catalogue you can find several ways to use the Proactive Technology Online Generic Enabler instance (creating instance, using existing instances, downloads the code). 
* [To install the Proactive Technology Online] (http://proactive-technology-online.readthedocs.org/en/latest/Proton-InstallationAndAdminGuide/index.html) - An installation guide at ReadTheDocs. This guide includes running instructions and sanity checks procedures. 
* Docker installation for the Proactive Technology Online is provided in the docke/ folder.
* [For more advanced tests, follow] (https://forge.fiware.org/plugins/mediawiki/wiki/fiware/index.php/CEP_GE_-_IBM_Proactive_Technology_Online_Unit_Testing_Plan) - unit tests.
* [For general information on this technology] (http://forge.fiware.org/plugins/mediawiki/wiki/fiware/index.php/FIWARE.OpenSpecification.Data.CEP) - CEP open specification page.
* [A high level description of the technology and an api overview can be found included in this comprehensive documentation] (https://www.fiware.org/devguides/real-time-processing-of-context-events/) - FIWARE developers’ tour guide.
* [A complete set of the REST api] (http://forge.fiware.org/plugins/mediawiki/wiki/fiware/index.php/Complex_Event_Processing_Open_RESTful_API_Specification), or in [apiary format] (http://htmlpreview.github.io/?https://github.com/ishkin/Proton/blob/master/documentation/apiary/CEP-apiary-blueprint.html).
* [Online documents] (http://proactive-technology-online.readthedocs.org/en/latest/index.html) - Proactive Technology Online documents are published at [ReadTheDocs] (http://proactive-technology-online.readthedocs.org/en/latest/ProtonUserGuide_FI_WAREv4_4_1/index.html) - User Guide, and the [Programmer Guide] (http://proactive-technology-online.readthedocs.org/en/latest/ProtonProgrammerGuide_FI_WAREv4_4_1/index.html). 
* More documentation is provided under the documentation folder, as described below. 
  * [Education material] (https://edu.fiware.org/course/view.php?id=58) - FIWARE academy.

[Top](#smc-semantic-mediation-container)

## Build and Install
Installation Options

The service works as a web service. A web-server is required to run it and that can be achieved in several ways.

* Install the war file on a web server such as Apache Tomcat.
* Do the same, but through a Docker container
* Execute it from a command line (cli) where an internal web server will be launched and execute the service.

The next sections describe each option in more details.

In addition, we now have a simple database of ontologies and models that are described in the product videos and presentations. It is nicer to start with that, rather than the empty default one. To do that we use the MySMC.zip file resource, which simply needs to be unzip into the working directory of the server. That is easier if the service operates on your own workstation. Than define the system environment SMC_root=MySMC, and start the server.

### Install WAR file on a Web Server

The Apache Tomcat server (https://tomcat.apache.org/tomcat-8.0-doc/setup.html) can host and execute the SMC WAR file. Once the Web Server is installed, all you need is to copy the WAR file (dm.war) into the webapp/ folder at the Tomcat installation root.

Alternatively, the Tomcat manager application can be configured on the server and used to upload and install that dm.war file.

### Install (and run) SMC with Docker

The IBM Semantic Mediation Contaioner (SMC) is a war (web application resource) file that can be deployed on Tomcat. The following is an installation procedure using Docker virtual machine that can run on any platform, windows, or unix:

#### Build (and run) your docker image

You can install SMC very easily using docker.
Follow these steps:

* Update /etc/hosts to add the current machine as a hostname. Note: on mac or windows, no need for "sudo" in the lines below.

* Install docker.

* Start the docker daeomon - either during the docker installation, or with the command

	sudo docker daemon & # THIS NEEDS TO BE RUN ONLY ONCE, IN THE BACKGROUND 

Or you can use the "Docker Quickstart Terminal" application.

* Navigate to a folder where you’d like the docker files to be placed. 
* Create a Dockerfile file with the following content (simply cut/paste that into the file and save it):

	# Start with tomcat based on Ubuntu
    FROM tomcat
    RUN apt-get update && apt-get install -y wget bash curl
    RUN apt-get install -y wget bash curl graphviz
    # we need graphviz for the dot command to be used by the SMC graphing diagrams
	#If the tomcat-users.xml exists, you can customise that before making this image	 
    # ADD tomcat-users.xml /usr/local/tomcat/conf
    # Installing the SMC war file from the PSYMBIOSYS wiki pages
    # Download the dm.war file from:
    # http://demos.txt.it:8096/intranet/wp3/wp3-platform/semantic-mediation-container/resources/smc-war-file/at_download/file
    ADD dm.war /usr/local/tomcat/webapps
    EXPOSE 8080 
    # instruct docker to start the tomcat when launched:
    ENTRYPOINT ["/usr/local/tomcat/bin/catalina.sh", "run"]
As commented in the quote above, use the generated dm.war file, or take it from the /bin folder, to the local directory.

* Build the Docker Image from the Dockerfile:

  	sudo docker build –t smc . 
You can view your images with the command

    docker images

* Run the docker image you just generated:

     sudo docker run --privileged=true --name=smc --cap-add SYS_PTRACE -p 8080:8080 -it -d -v="/Users/shani/MySMC:/usr/local/tomcat/MySMC" -e="SMC_name=MySMC" smc

The option -e="SMC_name=MySMC" sets an environment variable which indicates the name in the dashboard title of the project being managed in this execution. Here, it is set to "MySMC". The default is "Database". In a new version (v2.03) this will also indicate a folder where the project is saved during the session and which can continue to be used in follow up image resumption or re-runs.

Another option you can use is -e="SMC_root=MySMCfolder" will distinguish the working folder from the project name. Default folder will be same as project name, as long as that name is a proper string for a file name. on the SMC dashboard, hovering over the project name will display folder path being used.

The -v parameter maps a folder in the host machine to the project folder so that its contents are saved also after the image executions stops. Assume you keep the project in a folder MySMC/ on your current file system, you can set this option: -v="/Users/shani/MySMC:/usr/local/tomcat/MySMC". The image will continue to use the same project data and configuration over multiple restarts.

Note: On Mac, path should be within /Users, on Windows, it should be in C:\Users (which on -v option should be specified as /c/Users. See more here: https://docs.docker.com/engine/userguide/containers/dockervolumes/

* View the running images with this command, and possibly run a bash terminal inside it:

     sudo docker ps # That will provide <container_id> for the proton image. 
     sudo docker exec -it <container_id> bash

 * Try it.
 Find the correct <ip> of your docker container on your network and try http://<ip>:8080. That will display the tomcat welcome screen.
 To see the SMC dashboard go with your browser to this URL: http://<ip>:8080/dm/sm

You can simply install Tomcat and install dm.war using it management web interface, or simply save the dm.war file in the /webapps folder under its main installation folder. When you run tomcat, the SMC application will be instantiated.

#### Platform considerations

**Windows:** Do not use the "sudo" prefix to the above commands.

**Mac:** No need to use the "sudo" prefix to the commands above when user had administrator priviledges.
If needed, use boot2docker - Set it up, than initialize environment shell variables:

After starting (running) the image on the docker vm, you can check the tomcat with:

	open "http://localhost:8080"

Which will open up the tomcat opening page.
 	open "http://localhost:8080/dm/sm"
 	
will take you to the SMC web dashboard.

#### Run from a command line (CLI)

To execute from a command line, unzip the WAR file, add the dm-classes.jar file resource, and execute as a java application.

Create a new folder and copy the dm.war and dm-classes.jar file into it. Change directory to that working folder.
	unzip dm.war file
	jar -xvf dm.war

 Optionally, you can also unzip the MySMC.zip file here so that you have a simple database to start play with
 
 	jar -xvf MySMC.zip

Execute the java application

   java -jar dm-classes.jar


[[Top](#smc-semantic-mediation-container)

## API Reference Documentation

[Top](#smc-semantic-mediation-container)

## License
The IBM Semantic Mediation Container is licenced under the Apache Licence Version 2.0. For more information see the LICENCE.md

[Top](#smc-semantic-mediation-container)

## Support 
Support can be obtained thorugh stackoverflow by tagging questions with semantic-mediation-container.

For working with source code, note that the projects are maven projects. 
They can be built using the "clean install" targets by running the mvn command on the parent pom (located in the "Proton”) directory. 
This command will build all the SMC projects.

In addition, by executing the "mvn deploy" 	command of the same parent pom after performing clean install, the target jars will be installed into a local repository named "mvn-repo" (/target/maven-repo).

[Top](#smc-semantic-mediation-container)
