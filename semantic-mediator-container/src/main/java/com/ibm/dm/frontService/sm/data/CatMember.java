
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

package com.ibm.dm.frontService.sm.data;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.DCTerms;

import thewebsemantic.Id;
import thewebsemantic.RdfProperty;

@SuppressWarnings("serial")
//@Namespace(prefix =IConstants.SM_PROPERTY_NS_PREFIX, uri = SmOntology.vocabulary.uri) 
public class CatMember extends Port {
	private String description = "";

	private CatMember() {
	}
	
	public static CatMember create(String id, String title, String description, String root) {
		CatMember catMember = new CatMember();
		catMember.id = id;
		catMember.type = PortType.repository;
		catMember.status = ADatabaseRow.STATUS.READY;
		catMember.ontologyId = SmOntology.ID;
		catMember.name = title;
		catMember.description = description;
		catMember.root = root;
		return catMember;
	}
	
	@Id
	public String getId()  {
		return super.getId();
	}
	public void setId(String id) {
		this.id = id;
	}
	@RdfProperty("http://purl.org/dc/terms/title")
	public String getTitle() {
		return name;
	}
	public void setTitle(String name) {
		this.name = name;
	}
	@RdfProperty(value="http://purl.org/dc/terms/description")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	Property created = DCTerms.created;
	@RdfProperty("http://purl.org/dc/terms/created")
	public long getCreated() {
		return dateCreated;
	}
	public void setCreated(long created) {
		this.dateCreated = created;
	}
	Property modified = DCTerms.modified;
	@RdfProperty("http://purl.org/dc/terms/modified")
	public long getModified() {
		return lastModified;
	}
	public void setModified(long modified) {
		this.lastModified = modified;
	}
	Property rootP = SmOntology.vocabulary.root;
	@RdfProperty("http://com.ibm.ns/haifa/sm#root")
	public String getRoot() {
		if (null == root)
			getRootElement();
		return root;
	}
	public void setRoot(String root) {
		this.root = root;
	}
}
