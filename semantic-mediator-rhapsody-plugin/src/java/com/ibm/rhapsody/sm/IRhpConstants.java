
/*
  Copyright 2011-2016 IBM
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

*/
/**
 * Licensed Material - Property of IBM
 * Copyright IBM  2011-2013 All Rights Reserved
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 *
 */
package com.ibm.rhapsody.sm;


public interface IRhpConstants {
	public static final String VERSION_LEVEL = "2.20- Feb 21 2016 15:52 PM";
	public static final String RELEASE_MSG = " Feb 21 2016 - Work with or w/out OAuth\n" +
								"March 8, 2015 - UPDM support, stereotypes, and tags\n" + 
								"Sept 30, using Jena library on RDF export.\n" +
								"Aug 18 2014 - For UPDM using updm ontology + Using relative plugin path + UPDM and SysML export to SMC or file.\n";
//	public static final String VERSION_LEVEL = "2.12";
//	public static final String RELEASE_MSG = "] -- Aug 18 2013 - Improved UI, new aAuth implementation.\n";
	public static final String RHP_ONTOLOGY_NS = "http://com.ibm.ns/rhapsody/haifa/sm#"; //"http://com.ibm.rhapsody/sm/";
	public static final String RHP_INSTANCE_NS = "http://com.ibm.ns/rhapsody/instance/ns#"; //"http://com.ibm.rhapsody/sm/";
	public static final String RHP_INSTANCE_PREFIX = "instance"; //"http://com.ibm.rhapsody/sm/";
	
	public static final String RHP_MODEL_PREFIX = "rhp_model";
	public static final String RHP_UPDM_ONTOLOGY_NS = "http://com.ibm.ns/rhapsody.updm/haifa/sm#"; //"http://com.ibm.rhapsody/sm/";
	public static final String RHP_UPDM_MODEL_PREFIX = "updm";
	public static final String VERSION = "version";
	public static final String RHP_PROPERTY_VERSION = RHP_ONTOLOGY_NS + VERSION;
//	public static final String RHP_ARG_NS = "http://ibm.com/rhapsody/RhpArg/";
//	public static final String RHP_ARG_PREFIX = "rhp_arg";
//	public static final String RHP_RAW_CONTEXT = "Rhp:rawContext";
//	public static final String RHP_MODIFIED_CONTEXT = "Rhp:modifiedContext";

	public static final String DESCRIPTION = "description";
	public static final String RHP_PROPERTY_DESCRIPTION = RHP_ONTOLOGY_NS + DESCRIPTION;
	public static final String NAME = "name";
	public static final String RHP_PROPERTY_NAME =  RHP_ONTOLOGY_NS + NAME;
	public static final String MODIFIED = "modifiedTimeStrong";
	public static final String RHP_PROPERTY_LAST_MODIFIED =  RHP_ONTOLOGY_NS + MODIFIED;
	public static final String USR_CLASS_NAME = "UsrClassName";
	public static final String RHP_PROPERTY_USR_CLASS_NAME = RHP_ONTOLOGY_NS + USR_CLASS_NAME;
	public static final String PART = "Part";
	public static final String RHP_PROPERTY_PART = RHP_ONTOLOGY_NS + PART;
	public static final String TYPE = "Type";
	public static final String RHP_PROPERTY_TYPE = RHP_ONTOLOGY_NS + TYPE;
	public static final String PORT = "Port";
	public static final String RHP_PROPERTY_PORT = RHP_ONTOLOGY_NS + PORT;
	public static final String FLOW_DIRECTION = "FlowDirection";
	public static final String ActivityPerformedByPerformer = "ActivityPerformedByPerformer"; // A UPDM relation and element type, which is a dependency in sysml.
	public static final String RHP_PROPERTY_TITLE = "http://purl.org/dc/terms/title";

//	public static final String SII_URI = "SII.Relations.URI";
//	public static final String RHP_PROPERTY_SII_TAG = RHP_MODEL_NS + SII_URI;
//	public static final String SII_MODIFIED = "SII.Relations.MODIFIED";
//	public static final String RHP_PROPERTY_SII_MODIFIED = RHP_MODEL_NS + SII_MODIFIED;
	public static final String RHP_PROPERTY_FLOW_DIRECTION = RHP_ONTOLOGY_NS + FLOW_DIRECTION;
	public final static String HAS_TYPE= RHP_ONTOLOGY_NS + "hasType";
	public final static String IMPLICIT_TYPE= RHP_ONTOLOGY_NS + "ImplicitType";
	public final static String STEREOTYPE = RHP_ONTOLOGY_NS + "Stereotype";
	public final static String HAS_QUALIFIED_NAME = RHP_ONTOLOGY_NS + "hasQualifiedName";
	public final static String GUID = RHP_ONTOLOGY_NS + "GUID";
	public final static String HAS_GUID = RHP_ONTOLOGY_NS + "hasGUID";
	public final static String APPLICABLE_TO = RHP_ONTOLOGY_NS + "isApplicableTo";
		
	public static final String SM_PROPERTY_NS = "http://com.ibm.ns/haifa/sm#";
	public static final String SM_PROPERTY_NS_PREFIX = "sm";
	public static final String SM_PROPERTY_SM_RESOURCE = SM_PROPERTY_NS_PREFIX + ":hasSMResource";
	public static final String SM_PROPERTY_SM_RESOURCE_FULL = SM_PROPERTY_NS + "hasSMResource";

	// Post parameters:
	public static final String PARAMETER_ACTION = "ACTION";
	public static final String PARAMETER_TARGET = "TARGET";
	public static final String PARAMETER_ROOT_RESOURCE = "ROOT_RESOURCE";

	// Structural properties
//	public static final String RHP_ATTRIBUTES = RHP_ARG_NS + "attributes";
//	public static final String RHP_ASSOCIATIONS = RHP_ARG_NS + "associations";
//	public static final String RHP_AGGREGATES = RHP_ARG_NS + "aggregates";

	public static enum RHP_USR_CLASS_NAME {
		/* not lega: Realization, Object*/ Class, Flow, Port, Project, Package, Interface, SysML, Object, block, StandardPort
	};
	public static enum  RHP_FLOW_DIRECTION {
		toEnd1, toEnd2
	}

	public static String datePattern = 	"yyyy-MM-dd:HH:mm:ss.SSSZ"; //"2011-08-22T23:34:45.978+03:00" --> "2011-08-22 23:34:45.978+0300"

	public static String QUERY_HEADER = "PREFIX rhp: <" + IRhpConstants.RHP_ONTOLOGY_NS + ">\n" +
	"PREFIX bso: <" + "http://www.sprint-iot.eu/bso/" + ">\n" +
	"PREFIX bso_System: <" + "http://www.sprint-iot.eu/bso/" + "System#>\n" +
	"PREFIX bso_Component: <" + "http://www.sprint-iot.eu/bso/" + "Component#>\n" +
	"PREFIX bso_ComponentProperty: <" + "http://www.sprint-iot.eu/bso/" + "ComponentProperty#>\n" +
	"PREFIX bso: <" + "http://www.sprint-iot.eu/bso/" + ">\n" +
	"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
	"PREFIX dc_terms: <http://purl.org/dc/terms/>\n" +
	"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" ;

	public  static String GET_REQUEST_PARAM = "ROOT_RESOURCE";

	// JSON stuff
	static final String JSON_NAME = "name";
	static final String JSON_PARTS = "parts";
	static final String JSON_URI = "uri";
	static final String JSON_METATYPE = "metatype";
	static final String JSON_TYPES = "types";
	static final String JSON_GUID = "guid";
	static final String JSON_DESCRIPTION = "description";
	static final String JSON_MODIFIED = "modified";
	
	public static final String SysML = RHP_ONTOLOGY_NS + "SysML";
	public static final String Project = RHP_ONTOLOGY_NS + "Project";
	public static final String SOS = RHP_UPDM_ONTOLOGY_NS + "SOS";
}
