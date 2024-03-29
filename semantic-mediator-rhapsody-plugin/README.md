Version 2.20 (Feb 2016)
-----------------------

1. Assume the plugin is installed in <RhP>/Share/Profiles/SMC/

2. Restart Rhapsody
3. Select a layout that show output window (Alt-3), and click the "Log" tab.
4. Open a project that is a SysML project. The project should be associated with the
	SysML profile and have the SysML type.
5. Issue file->Add Profile to Model... -> Profiles/Rhp-SMC/Rhp-SMC.sbs
	--> Log will show:
		Initializing SMC interface plugin. Version [2.x].
		project [<project name>]: <project type - which may be 'SysML'>

		Version is x.nn where x is advanced as new ontologies are modified for Rhapsody
	Version is also a tag of the plugin profile and can be seen on the Rhapsody model explorer.
6. To export:
	7.1 Right click on a "block" element, a package or the project. You can
		do that via the model explorer window on left.
	7.2 Select the new entry: "SMC" -> "Export to SMC" on the drop down menu.
	7.3 You will be prompted for the host URL in a pop up window. Enter name, click "OK" - or "Cancel" to abort export.
		--> host: http://<host>:9444/dm/sm/tool/<access-name>
		(e.g., http://sm.haifa.il.ibm.com:9444/dm/sm/tool/rhapsody)
	7.4 You will see on log file progress report such as:
		Executing [Export to SMC]....
		Processes [15] elements: [GUID-12f8a3a1-854f-42c9-bf29-8b86e009ad3a, GUID-7aad6368-2a56-4681-917f-764063e9c00e, GUID-a60b9f0a-66ab-469c-a420-0db1a9815e53, GUID-e3f88ae5-b8a7-4764-adb7-3a3b3e1c7c42, GUID-d74fc50b-76de-4425-9714-61d284ef0c75, GUID-2c476289-e20a-40ad-ade3-b6d17a5dbc29, GUID-246f0a6e-9a2a-413d-a78d-d41b9b800021, GUID-bdded439-7c20-4da9-ae4d-d873dbec3f7d, GUID-421f7179-f82d-4d09-81cd-e2ba91fed5c4, GUID-4e7664c6-2ca3-4078-8d6d-8efc75899c84, GUID-4fb34d78-c353-441a-b206-3177f4b8bf7a, GUID-2dddc22a-d10d-4521-902c-fbbae273728c, GUID-5b7d6eaf-30dd-4450-8d15-3773b92d7a16, GUID-36d78c92-55e7-499c-91a2-9d365edef5fb, GUID-34dfe6bb-ca90-4230-a825-22bf82745ed2, ]
		Into tmp file [C:\temp\TowBotSimpleExample8704301047725671866.xml].
		Exporting...OK. Posting report:
		. . .
		Completed calling OnMenuItemSelect.
	7.5 The tmp file contains the rdf+xml file of the Rhapsody contents published
		into SII.
7. To import:
	8.1 Right click on a project that is either a new one (but with profiles and
		type as described in #5 above), or an existing project that was already
		exported to SMC.
	8.2 Select the new entry: "SMC" -> "Import from SMC" on the drop down menu.
	8.3 You will be prompted twice: for server URL and than for element URI to import from. You can cancel
		during either pop-up by clicking the "Cancel" button.
	8.4 You will see on log file progress report such as:
		Executing [Import from SMC]....
		http://<host>:9444/dm/sm/tool/<access-name>?ROOT_RESOURCE=http://<host>:9444/dm/sm/repository/<repos-name>/resource#<resource-number>
		(e.g., http://sm.haifa.il.ibm.com:9444/dm/sm/tool/rhapsody?ROOT_RESOURCE=http://sm.haifa.il.ibm.com:9444/dm/sm/repository/rhapsody/resource#2)
		OK.
		Reply content:
		. . .
		------ Successful update   --------
		Completed calling OnMenuItemSelect.
9. To reset state stored in a project:
	9.1 Right click on the project and select "SMC" -> "Reset state with SMC".
	9.2 You will be prompted to confirm in a pop-up window. Click "Yes, please" to do that, or "No, thanks" to abort.




To build the project and install it as a plugin on the Rhapsody 7.6 installed on this machine:
Run tools/build.xml ... as Ant Build.

The result is also the zip file in build/rhapsody.plugin.zip. This is the element to ship to customers to build
and install on their Rhapsody clients.

This zip contains all the instructions and components to do that. All OSS jars will be imported from the internet.

Similarly, to install the plugin on Rhapsody 8.0.2 on your machine, do this build.xml on the Sm.Rhapsocy.dev.8.0.2 project.


This ant file also updates the shared project  com.ibm.haifa.smc.rhapsody.plugin. This project is actually the source 
of the customer installation zip file created in the build/ folder here.

