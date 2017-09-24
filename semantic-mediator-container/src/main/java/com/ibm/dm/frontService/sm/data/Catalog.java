
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

import java.util.Collection;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.service.ARdfRepository;
import com.ibm.dm.frontService.sm.service.Repository;

import thewebsemantic.Bean2RDF;
import thewebsemantic.RDF2Bean;


public class Catalog extends Port {

	private static final long serialVersionUID = 1L;
	private static String[]    catalogEditableFields   = new String[] { 
		IFields.VERSION, 
		IFields.NAME, 
		IFields.ACCESS_NAME, 
		IFields.ARCHIVED, 
		IFields.TAGS,
		IFields.VIEW_CONFIG,
	};
	
	public Catalog() {
		ontologyId = "SmOnt";
		type = PortType.catalog;
		
	}
	@Override
	public void clear(ARdfRepository repository) {
		new Exception("Cannot clear repository of a catalog directly. Use clear() instead.").printStackTrace();
	}

	@Override
	public String[] getEditableFieldNames() {
		return catalogEditableFields;
	}

	@Override
	public ARdfRepository initRdfRepository() {
		// TODO Auto-generated method stub
		return super.initRdfRepository();
	}

	@Override
	public boolean isRepository() {
		return super.isRepository();
	}

	@Override
	public void setOntologyId(String ontologyId) {
		new Exception("Cannot set ontology Id in a catalog").printStackTrace();
	}

	@Override
	public void setType(PortType type) {
		// TODO Auto-generated method stub
		super.setType(type);
	}

	@Override
	public void setType(String type) {
		new Exception("Cannot set port type for a catalog").printStackTrace();
	}

	@Override
	String update(String field, String v, JSONObject jUpdate, Map<String, String> params) throws JSONException {
		if (IFields.ONTOLOGY_ID.equals(field))
			new Exception("Cannot update ontology ID field in a catalog").printStackTrace();
		return super.update(field, v, jUpdate, params);
	}

	public String clear() {
		StringBuffer sb = new StringBuffer();
		ARdfRepository r = getModelRepository(); 
		Model model = r.getModel();
		RDF2Bean reader = new RDF2Bean(model);
		Collection<CatMember> members = reader.load(CatMember.class);
		boolean fail = false;
		for (CatMember member: members) {
			String m = member.deleteContents();
			if (m.startsWith("Error"))
				fail = true;
			sb.append(m).append("\n");
		}
		if (fail)
			return "Error: Failed to delete all. Details:\n" + sb.toString();
		return r.clearRepository() + "\nDetails:" + sb.toString();
	}
	
	public String post(Model m) {
		StmtIterator stmts = m.listStatements(null, RDF.type, (RDFNode)SmOntology.vocabulary.catalogMember);
		if (! stmts.hasNext()) 
			return "Error: No catalogMember in the model";
		Resource member = stmts.next().getResource();
		String description = null;
		NodeIterator nodes = m.listObjectsOfProperty(member, DCTerms.description);
		if (nodes.hasNext())
			description = nodes.next().toString();
		String title = null;
		nodes = m.listObjectsOfProperty(member, DCTerms.title);
		if (nodes.hasNext())
			title = nodes.next().toString();
		if (Strings.isNullOrEmpty(title)) 
			return "Error: Missing mandatory title for the member";
		Resource root = null;
		nodes = m.listObjectsOfProperty(member,  SmOntology.vocabulary.memberRoot);
		if (nodes.hasNext())
			root = m.getResource(nodes.next().toString());
		if (null == root)
			return "Error: Missing reference to the rool element of the member";
		ARdfRepository r = getModelRepository();
		Model myModel = r.getModel();
		stmts = myModel.listStatements(null, DCTerms.title, m.createLiteral(title));
		CatMember catMember = null;
		RDF2Bean reader = new RDF2Bean(myModel);
		Property pId = myModel.getProperty(getNameSpaceUri() + "id");
		if (stmts.hasNext()) { // Member exists, replace its contents
			Statement stmt = stmts.next();
			String id = myModel.getProperty(stmt.getResource(), pId).getObject().toString();
			catMember = reader.load(CatMember.class, id);
		} else { // Make a new member, and fill it up with contents
			Resource res = ((Repository)r).getResource();
			String id = res.toString();
			id = id.substring(id.lastIndexOf('/') +1);
			catMember = CatMember.create(id, title, description, root.toString());
		}
		Bean2RDF writer = new Bean2RDF(myModel);
		writer.save(catMember);
		return null;
	}
	
}
