
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

import java.io.InputStream;

import org.apache.jena.rdf.model.*;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.intfc.IRulesHandle;
import com.ibm.dm.frontService.sm.intfc.MediationException;
import com.ibm.dm.frontService.sm.utils.IConstants;

public class RulesHandle extends OntologyHandle implements IRulesHandle {

	public RulesHandle(Database owner, InputStream is, String base) {
		super(owner, is, base, IConstants.RDF_XML);
	}

	public RulesHandle(Database owner, Model model) {
		super(owner, model);
	}

	public RulesHandle(OntologyDescription od) {
		super(od);
	}
	
	public String getRules() throws MediationException {
		return getRdf();
	}
	

}

