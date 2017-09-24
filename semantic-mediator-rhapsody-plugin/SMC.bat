set JavaClassPath1=RHAPSODY/commons-lang3-3.1.jar;RHAPSODY/commons-logging-1.1.1.jar;RHAPSODY/guava-14.0.1.jar;RHAPSODY/httpclient-4.2.2.jar;RHAPSODY/httpcore-4.2.2.jar;RHAPSODY/httpmime-4.2.1.jar;RHAPSODY/jena-core-2.10.1.jar;RHAPSODY/jena-iri-0.9.6.jar;RHAPSODY/log4j-1.2.16.jar;RHAPSODY/slf4j-api-1.7.5.jar;RHAPSODY/slf4j-log4j12-1.7.5.jar;RHAPSODY-ibm/com.ibm.haifa.smc.client-1.0.0.jar;RHAPSODY-ibm/Sm-1.0.0.jar;
set RhpJarPath1=RHAPSODY.8.1/rhapsody.jar
set RhpLibPath1=RHAPSODY.8.1
set JavaMainClass1=com.ibm.rhapsody.sm.plugin.RhpPlugin
C:\Java\jdk\bin\java -cp %JavaClassPath1%%RhpJarPath1% -Djava.library.path=%RhpLibPath1% %JavaMainClass1%

