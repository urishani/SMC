
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
import java.io.OutputStream;

import org.apache.jena.rdf.model.*;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IOutputModelHandle;

public class OutputModelHandle implements IOutputModelHandle {

	Model model = null;
	ByteArrayOutputStream os = null; 
	String rdfXML;
	boolean rdfReady = false;
	public IOntologyHandle getOntology() {
		return null;
	}

	/**
	 * Returns a new OutputStream in which content of the model can be written.
	 * It assumes that the stream writing will follow by a getModel() call which will
	 * return a model consisting of the content of that stream writing.
	 * Any other activity, will rest the stream and ignore its contents.
	 * @return OutputStream to accept content of the model description.
	 */
	public OutputStream getStream() {
		os = new ByteArrayOutputStream();
		rdfReady = false;
		return os;
	}

	/**
	 * A quick way to set a model content using a Sring of its RDF content in rdf+xml format.
	 * When the model is obtained, its content will come from this RDF description, unless
	 * other means of providing models will be used, such as a stream.<br>
	 * This class uses lazy computation and will not create the model internally unless requested 
	 * by a client of this class.
	 * @param rdf a String of a model in rdf+xml format.
	 */
	public void setRdf(String rdf) {
		reset();
		this.rdfXML = rdf;
		rdfReady = true;
		model = null; // ensure the model is empty;
	}
	
	/**
	 * When the model is requested, it comes from an existing model in this handle, or from other
	 * sources within it, such as an RDF string, or an OutputStream. In the later, the client calling
	 * this method must ensure to flush all its output to that stream before calling this method.
	 * @return a Model.
	 */
	public Model getModel() {
		if (null != model)
			return model;
		if (null != os && false == rdfReady) {
			rdfXML = os.toString();
			rdfReady = true;
		}

		model = ModelFactory.createDefaultModel();

		if (rdfReady) {
			//model.read(rdfXML);
			model.read(new ByteArrayInputStream(rdfXML.getBytes()), null);
		}
		return model;
	}
	
	public void reset() {
		rdfReady = false;
		rdfXML = null;
		os = null;
		model = null;
	}

	public void setModel(Model model) {
		reset();
		this.model = model;
	}

	public String getBase() {
		getModel();
		if (null == model) return null;
		String base = model.getNsPrefixURI("");
		if (null == base)
			base = model.getNsPrefixURI("base");
		return base;
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
	
	

}

