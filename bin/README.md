# SMC - Semantic Mediation Container Binaries

The following binaries included in this folder:

1. **dm.war** - SMC webapp WAR file that can be deployed on a web server such as Tomcat. See installation guide for doing that with a Docker container.
2. **dm-classes.jar** - Implementation jar that allows to run the service from a commang line (cli). See installation guide for how to do that.
3. **com.ibm.haifa.smc.repository.jar** - Protege plugin jar. Copy into the **plugin/** folder of a Protege installation. See [README.md](../semantic-mediator-protege-plugin/README.md) for detains.
4. **rhapsody.plugin.zip** - Plugin for the Rhapsody SysML design tool. See [Rhapsody plugin nstallation instructions](../semantic-mediator-rhapsody-plugin/README.md)
5. **MySMC.zip** - An example SMC workspace. See the installation instructions for how to include it in your server space.
6. **Dockerfile** - Can be used to create a docker image. See instructions in main README file.