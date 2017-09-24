
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.jena.rdf.model.*;
import com.ibm.dm.frontService.sm.intfc.IInputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.utils.Utils;

public class InputModelHandle implements IInputModelHandle {

	Model model = null;
	String rdfXML = null;

	private final InputStream is;
	public InputModelHandle(InputStream is, String contentType) throws IOException {
		this.is = is;
		model = Utils.modelFromStream(is, contentType, null);
	}
	public InputModelHandle(String rdf, String contentType) throws IOException {
		this.is = null;
		model = Utils.modelFromString(rdf, contentType, null);
	}
	
	public InputModelHandle(Model model) {
		is = null;
		this.model = model;
	}
	
	public Date getEtag() {
		return new Date();
	}

	public IOntologyHandle getOntology() {
		return null;
	}

	public String getRdfXML() {
		if (null != rdfXML)
			return rdfXML;
		getModel();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		model.write(os);
		rdfXML = os.toString();
		return rdfXML;
	}

	public InputStream getStream() {
		getRdfXML();
		return new ByteArrayInputStream(rdfXML.getBytes());
	}

	public Model getModel() {
		if (null == is && null == model)
			return null;
		if (null != is && null == model) {
			try {
				model = Utils.modelFromStream(is, null, getBase());
			} catch (IOException e) {
				return null;		
			}
		}	
		return model;
	}
	
	public String getBase() {
		getModel();
		if (null == model) return null;
		String base = model.getNsPrefixURI("");
		if (null == base)
			base = model.getNsPrefixURI("base");
		return base;
	}

}

