
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

package com.ibm.dm.frontService.sm.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.RuleSet;
import com.ibm.dm.frontService.sm.data.Shape;
import com.ibm.dm.frontService.sm.data.SmOntology;
import com.ibm.dm.frontService.sm.intfc.imp.OntologyDescription;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * Used to maintain ontological repositories.
 * 
 * @author shani
 *
 */
public class ModelRepository extends ARdfRepository {

//	@Override
//	protected String getDomainExt() {
//		return ".owl";
//	}

	protected ModelRepository(AModelRow item) {
		super(item);
		if (false == (item instanceof Ontology || item instanceof RuleSet || item instanceof Shape) )
			System.err.println("Model repository is created for an item which is not an ontology. Item [" + item.getDisplayId() + "]");
		if (false == item instanceof SmOntology) // a cludge to prevent redundant database updates.
			fixPrefixes();
	}

	protected final static Map<String, String> preferredPrefixes = new HashMap<String, String>();
	static {
		preferredPrefixes.put("owl", "http://www.w3.org/2002/07/owl#");
		preferredPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		preferredPrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		preferredPrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
	}
	@Override
	public Map<String, String> getPreferredPrefixes() {
		return preferredPrefixes;
	}

	@Override
	public String getBase() {
		if (null == repository)
			return ((AModelRow)this.myItem).getNameSpaceUri().toString();
		String base = repository.getNsPrefixMap().get("");
		if (Strings.isNullOrEmpty(base))
			base = repository.getNsPrefixMap().get("base");

		if (Strings.isNullOrEmpty(base)) // sometimes there is no base, and it must exist.
			return ((AModelRow)this.myItem).getNameSpaceUri().toString();
		else
			return base;
	}

	@Override
	protected String getDomain() {
		return myItem.getCollectionName().toLowerCase();
	}

	public static ModelRepository create(AModelRow model) {
		return new ModelRepository(model);
	}

	@Override
	public void doSave() {
		saveToFile();
	}

	@Override
	String getBaseForVersion(String version) {
		if (version.equals("current"))
			return getBase();
		else {
			File oldVersion = new File(getFolder(), version);
			try {
				OntologyDescription od = OntologyDescription.fromModel(getMyItem().getDatabase(),
						Utils.modelFromFile(oldVersion.getAbsolutePath()), null);
				return (null != od)?od.getBase():null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
}
