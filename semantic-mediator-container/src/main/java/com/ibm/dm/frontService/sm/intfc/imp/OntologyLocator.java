
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

/**
 * Licensed Material - Property of IBM
 * Copyright IBM  2011 All Rights Reserved
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 *
 */
package com.ibm.dm.frontService.sm.intfc.imp;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyLocator;
import com.ibm.dm.frontService.sm.utils.IConstants;

public class OntologyLocator implements IOntologyLocator {

	private final Database mOwner;
	public OntologyLocator(Database owner) {
		mOwner = owner;
	}
	public IOntologyHandle getOntologyForClass(URI classUri) {
		try {
			Ontology ontology = mOwner.getOntologyByClassUri(classUri);
			if (null == ontology)
				return null;
			File file = new File(ontology.getFileName());
			if (false == file.canRead())
				return null;
			return new OntologyHandle(mOwner, new FileInputStream(file), ontology.getModelInstanceNamespace(), IConstants.RDF_XML);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}

