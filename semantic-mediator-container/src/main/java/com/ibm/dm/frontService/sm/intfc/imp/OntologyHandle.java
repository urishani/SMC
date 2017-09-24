
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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.jena.rdf.model.*;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;

public class OntologyHandle implements IOntologyHandle {

	private final OntologyDescription od;
	private Collection<IOntologyHandle> imports = null;
	private final Database mOwner;
	
	/**
	 * Constructor of an OntologyHandle from an InputStream of the ontology content.
	 * @param is InputStream of a legal ontolgoy.
	 */
	public OntologyHandle(Database owner, InputStream is, String base, String contentType) {
		mOwner = owner;
		od = OntologyDescription.fromStream(mOwner, is, base, contentType);
	}

	protected Database getDatabase() {
		return mOwner;
	}
	/**
	 * Constructs a new OntologyHandle through an existing model.
	 * @param owner Database owner of this object.
	 * @param model Model for the ontology.
	 */
	public OntologyHandle(Database owner, Model model) {
		mOwner = owner;
		od = OntologyDescription.fromModel(mOwner, model, getBaseNS());
	}
	
	/**
	 * Constructs an OntologyHandler from a proper OntologyDescription. This 
	 * should be a legal and contain a model.
	 * @param od OntologyDescription with a legal ontology model.
	 */
	public OntologyHandle(OntologyDescription od) {
		this.mOwner = od.mOwner;
		this.od = od;
	}

	public String getBaseNS() {
		if (null != od)
			return od.getBase();
		return null;
	}

	public Collection<IOntologyHandle> getDependencies() {
		if (null != imports)
			return imports;

		if (null == od) 
			return null;

		String importNames[] = od.getImports();
		Collection<IOntologyHandle> list = new ArrayList<IOntologyHandle>();
		Database database = null;
		try {
			database = getDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (null == database)
			return null;

		for (String importName : importNames) {
			URI importUri = null;
			try {
				importUri = new URI(importName);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			if (null == importUri)
				continue;
			Ontology ontology = database.getOntologyByClassUri(importUri);
			if (null == ontology || false == ontology.isReady())
				continue;
			try {
				IOntologyHandle oh = new OntologyHandle(ontology.getOntologyDescription());
				list.add(oh);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		imports = list;
		return imports;
	}

	public String getRdf() {
		if (null == od)
			return null;
		return od.getRdf();
	}
	

	public InputStream getStream() {
		if (null == od)
			return null;
		return new ByteArrayInputStream(od.getRdf().getBytes());
	}

	public String getVersionIRI() {
		if (null == od)
			return null;
		return od.getVersionIRI();
	}

}

