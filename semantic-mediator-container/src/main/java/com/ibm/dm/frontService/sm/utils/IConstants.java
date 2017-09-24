
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
/*
 *+------------------------------------------------------------------------+
 
 
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

// Old file is rewritten to use different sources for the constants that are used in the project.
// Removing all OSLC things as well.
package com.ibm.dm.frontService.sm.utils;


@SuppressWarnings("nls")
public interface IConstants {

	// OWL namespace
	//public static final Property OWL_VERSIONIRI = OWL.versionInfo;

	public static final String N_TRIPLE = "application/n-triples";
	public static final String N3 = "text/n3";
	public static final String TURTLE = "text/turtle";
	public static final String RDF_XML = "application/rdf+xml";
	//public static final String NTRIPLE = "application/ntriples";

	// Copied here from IRhpConstants in the Rhapsody plugin project.
	public static final String SM_PROPERTY_NS = "http://com.ibm.ns/haifa/sm#";
	public static final String SM_PROPERTY_NS_PREFIX = "smc";
	public static final String SM_PROPERTY_SM_RESOURCE = SM_PROPERTY_NS_PREFIX + ":hasSMResource";
	public static final String SM_PROPERTY_SM_RESOURCE_FULL = SM_PROPERTY_NS + "hasSMResource";
	public static final String SM_PROPERTY_SM_TAG = SM_PROPERTY_NS_PREFIX + ":hasSMTag";
	public static final String SM_PROPERTY_SM_TAG_FULL = SM_PROPERTY_NS + "hasSMTag";
	public static final String SM_PROPERTY_SM_RESOURCE_DELETED = SM_PROPERTY_NS_PREFIX + ":resourceDeleted";
	public static final String SM_PROPERTY_RESOURCE_DELETED_FULL = SM_PROPERTY_NS + "resourceDeleted";

	public static final String PARAMETER_ACTION = "ACTION";
	public static final String PARAMETER_TARGET = "TARGET";
	public static final String PARAMETER_ROOT_RESOURCE = "ROOT_RESOURCE";
	public static final String JSON = "application/json";
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	public static final String UTF_8 = "UTF-8";
	public static final String HTML = "text/html";
	public static final String IF_MATCH = "If-Match";
	public static final String ETAG = "ETag";
	public static final String PLAIN_TEXT_TYPE = "text/plain";
	
	public static final String CONTENT_TYPES[] = {N_TRIPLE, N3, TURTLE, RDF_XML};


}

