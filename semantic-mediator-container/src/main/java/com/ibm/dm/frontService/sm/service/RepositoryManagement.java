
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

public class RepositoryManagement { // extends SmServiceBase {
	static RepositoryManagement me = null;

	private RepositoryManagement(Database owner) throws Exception {
		super();
		if (null == owner) {
			owner = null;
			throw new Exception("Must have a database!");
		}
		mOwner = owner;
	}

	public Database getDatabase() {
		return mOwner;
	}
	private final Database mOwner;
	public static RepositoryManagement create(Database owner) throws Exception {
		return new RepositoryManagement(owner);
	}
	
//	private
//	public static RepositoryManagement create(Database db) {
//		if (null == me) synchronized (RepositoryManagement.class) {
//			if (null == me)
//				me = new RepositoryManagement();
//		}
//		return me;
//	}

	public void doGet(HttpServletRequest request, HttpServletResponse response,
			ISmService smService) throws Exception {
		boolean failed = false;
		String msg = "";
//		onEntry(request, response);
		String action = smService.getParam(request, "action");
		String id = smService.getParam(request, "id");
		String cls = smService.getParam(request, "class");
		String subject = smService.getParam(request, "subject");
		String property = smService.getParam(request, "property");
		String object = smService.getParam(request, "object");
		String value = smService.getParam(request, "value");
		if (Strings.isNullOrEmpty(id)) {
			Utils.respondWithText(response, "<html><body>OSLC Query can only be used from an application. No browser interface suppoerted</body></html>");
			return;
		}
		Database db = getDatabase();
		Port p = db.getPort(id);
				
		Repository r = (Repository) ((null != p)?p.getModelRepository():null);
		Model m = (null != r)?r.getModel():null;

		if ("saveEdit".equals(action)) { // save this repository to its original item
			Port orig_port = null;
			if (id.endsWith("-Copy")) 
				orig_port = db.getPort(id.substring(0, id.lastIndexOf("-Copy")));
			if (null == orig_port)
				throw new Exception("Cannot save [" + id + "] to an original ID.");
			
			Map<String, String> map = SmContainer.getContainer(getDatabase()).saveTempToPort(
					m, db, p, orig_port, response);
			String report = "Successfully ended, but no mapping info returned!";
			StringBuffer sb = new StringBuffer();
			if (null != map) {
				for (String m1: map.keySet()) {
					sb.append(m1).append(",").append(map.get(m1)).append("\n"); 
				}
				report = sb.toString();
			}
			JSONObject contents = new JSONObject();
			contents.put("title", "Saving edit results in draft");
			contents.put("rlation", "into");
			contents.put("report", report);
			contents.put("from_port", p.getId());
			contents.put("to_port", orig_port.getId());
			Utils.respondGetWithTemplateAndJson(contents, "saveEdit", response, "text/html");
			return;
		}
		
		Map<String, String> map = m.getNsPrefixMap();
		if (false == Strings.isNullOrEmpty(subject))
			subject = Utils.uriFromPrefix(subject, map);
		if (false == Strings.isNullOrEmpty(property))
			property = Utils.uriFromPrefix(property, map);
		if (false == Strings.isNullOrEmpty(object))
			object = Utils.uriFromPrefix(object, map);
		Resource rs = m.getResource(subject);

		if (null == p || false == p.isRepository()) {
			String s = "Failed to find repository port with id [" + id + "].";
			Utils.respondWithText(response, s);
			msg += s;
			failed = true;
		} else if ("updateResource".equals(action)) {
			String newValue = smService.getParam(request, "newText");
			if (Strings.isNullOrEmpty(subject) || 
					Strings.isNullOrEmpty(id) || 
					Strings.isNullOrEmpty(property) ) {
				msg = "Error in request, parameter is missing [" + subject + "|" + id + "|" + property + ".";
				failed = true;
			}  else if (null == p || false == p.isTemporary() || null == r || null == m) {
					msg = "Error - port with id [" + id + "] is missing or not temporary!";
					failed = true;
			} else {
				Resource res = m.getResource(subject);
				Property prop = m.getProperty(property);
				
				Ontology ont = p.getOntology();
				Model ontModel = ont.getModelRepository().getModel();
				boolean isObjectProperty = Utils.resourceHasPropertyValue(ontModel.createResource(prop.toString()), 
						RDF.type, OWL.ObjectProperty);
				if (res.hasProperty(prop)) 
					res.removeAll(prop);
				if (null != newValue) { // we add a predicate. If it is missing, nothing to add and predicate has been removed already above.
					if (isObjectProperty) {
						String uri = Utils.uriFromPrefix(newValue, map);
						res.addProperty(prop, m.createResource(uri));
					} else
						res.addProperty(prop, newValue);
				}
				msg = "Successfuly updated predicate [" + property + "] in resource [" + subject + "]:" + 
						((null != newValue)?("replaced with new value [" + newValue + "]."):"removed.");
			}
			Utils.respondWithText(response, msg);
			if (!failed) {
				r.setDirty();
				r.save();
			} else {
				throw new Exception(msg);
			}
		} else if (null == subject || false == "E".equals(action)) { // This is editing in the model level
			if ("D".equals(action)) {
				// Need only to mark that resource with a tag that it is to be deleted.
				rs.addProperty(m.createProperty(IConstants.SM_PROPERTY_SM_TAG_FULL), 
						m.getResource(IConstants.SM_PROPERTY_RESOURCE_DELETED_FULL));
//				StmtIterator stmts = m.listStatements(rs, (Property)null, (RDFNode)null);
//				m.remove(stmts);
//				stmts = m.listStatements((Resource)null, (Property)null, rs);
//				m.remove(stmts);
				//r.setDirty();
			} else if ("UD".equals(action)) { // undelete - remove the tag set above.
				// Need only to unmark that resource from the tag that it is to be deleted.
				m.remove(m.createStatement(rs, 
						m.createProperty(IConstants.SM_PROPERTY_SM_TAG_FULL), 
						m.getResource(IConstants.SM_PROPERTY_RESOURCE_DELETED_FULL)));
			} else if ("save".equalsIgnoreCase(action)) {
//				r.save();
			} else if ("NewIndividual".equalsIgnoreCase(action)) {
				//				String ontId = p.getOntologyId();
				Ontology ont = p.getOntology();
				if (null == ont) {
					String s = "Failed to find repository port with id [" + id + "].";
					Utils.respondWithText(response, s);
					failed = true;
					msg += s;
				} else {
					ModelRepository mr = (ModelRepository) ont.getModelRepository();
					Model ontm = mr.getModel();
					//					String base = ont.getBaseURL();
					Resource newIndividual = r.getResource();
					newIndividual.addProperty(RDF.type, ontm.getResource(ont.getNameSpaceUri() + cls));
					//r.setDirty(); 
				}
			}
			if (!failed) {
				r.setDirty();
				r.save();
				String title = "Repository [" + id + "]: " + p.getName() + ", Domain [" + r.getDomain() + "]";
				new SmManager(getDatabase()).processModel(response, r, "ShowList", title, id, null, db.getVar(Database.Vars.contentType.toString()), (String)null, (String)null, (String)null, (String)null);				    
			}
		} else {
			if ("newObjectProperty".equalsIgnoreCase(action)) {
				rs.addProperty(m.getProperty(property), object);
				r.setDirty();
			} else if ("newDataProperty".equalsIgnoreCase(action)) {
				rs.addProperty(m.getProperty(property), value);
				r.setDirty();
			}
			r.save();
			JSONObject contents = jsonForSubjecteditor(r, rs);
			Utils.respondGetWithTemplateAndJson(contents, "/templates/edit/editIndividual.html", response);
		}
		return;
//		onExit(request, response, failed);
	}

	private JSONObject jsonForSubjecteditor(Repository r, Resource rs)
	throws FileNotFoundException, IOException, ClassNotFoundException, JSONException {
		Map<String, String> map = r.getModel().getNsPrefixMap();
		JSONObject contents = new JSONObject();
		Port port = (Port)r.getMyItem();
		contents.put("id", port.getId());
		contents.put("name", port.getName());
		contents.put("subject", Utils.applyPrefixesOnResources(map, rs.toString()));
		contents.put("enableSave", r.isDirty());
		Statement s = rs.getProperty(RDF.type);
		if (null != s) {
			RDFNode type = s.getObject();
			contents.put("type", Utils.applyPrefixesOnResources(map, type.toString()));
		} else
			contents.put("type", "untyped");
		//		String ontId = port.getOntologyId();
		Ontology ontology = port.getOntology();
		if (null != ontology) {
			//			ontology = db.getOntology(ontId);
			contents.put("ontology", (null == ontology)?"none":(ontology.getId() + ":" + ontology.getName()));
			String prefixes = Utils.makePrefix4Query(map);
			contents.put("prefixes", Utils.forHtml(prefixes));
			JSONArray objectProperties = new JSONArray();
			contents.put("instObjectProperties", objectProperties);
			JSONArray dataProperties = new JSONArray();
			contents.put("instDataProperties", dataProperties);
			JSONArray objects = new JSONArray();
			contents.put("objects", objects);
			JSONObject jOnt = ontology.jsonFromOntology();
			if (null != jOnt) {
				contents.put("objectProperties", ((JSONObject)Utils.safeGet(jOnt, "objectProperties")).toString());
				contents.put("dataProperties", ((JSONObject)Utils.safeGet(jOnt, "dataProperties")).toString());

				StmtIterator stmts = rs.listProperties();
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					Property p = stmt.getPredicate();
					if (p.equals(RDF.type))
						continue;
					RDFNode o = stmt.getObject();
					JSONObject inst = new JSONObject();
					inst.put("property", Utils.applyPrefixesOnResources(map, p.toString()));
					if (o.isResource()) {
						inst.put("object", Utils.applyPrefixesOnResources(map, o.toString()));
						objectProperties.put(inst);
					} else {
						inst.put("value", o.toString());
						dataProperties.put(inst);
					}
				}
			}

		}
		return contents;
	}

	
	public void doPost(HttpServletRequest request, HttpServletResponse response,
			ISmService smService) throws Exception {
//		boolean failed = false;
//		onEntry(request, response);
//		String action = smService.getParam(request, "action");
		String id = smService.getParam(request, "id");
//		String cls = smService.getParam(request, "class");
//		String subject = smService.getParam(request, "subject");
//		String property = smService.getParam(request, "property");
//		String object = smService.getParam(request, "object");
//		String value = smService.getParam(request, "value");
		if (Strings.isNullOrEmpty(id)) {
			Utils.respondWithText(response, "<html><body>OSLC Factory can only be used from an application. No browser interface suppoerted</body></html>");
			return;
		}
	}

	/**
	 * Performs a post or a get of the model contents of a tool to its associated repository.
	 * @param request
	 * @param row tool item
	 * @param repositoryPort repository item
	 * @param action String G for get, or P for post.
	 * @throws JSONException 
	 */
	public void postOrGetToolToRepository(ResponseWrapper response,
			Port tool, Port repositoryPort, String action) throws JSONException {
		Map<String, String> map = null;
		
		switch (action) {
		case "P": map = SmContainer.getContainer(getDatabase()).postToolToPort(tool, repositoryPort, response); break;
		case "G": map = SmContainer.getContainer(getDatabase()).getToolFromPort(tool, repositoryPort, response);
		}
		// Need to save this map 
		String report = "Successfully ended, but no mapping info returned!";
		if (null != map) 
			report = Utils.mapS2csv(map, ",");
		JSONObject contents = new JSONObject();
		String title = "Posting tool contents of ";
		String relation = "into associated repositroy";
		if (action.equals("G")) {
			title = "Getting tool contents from associated repository";
			relation = "into tool";
		}
		contents.put("title", title);
		contents.put("rlation", relation);
		contents.put("report", report);
		contents.put("from_port", action.equals("P")?tool.getId():repositoryPort.getId());
		contents.put("to_port", action.equals("P")?repositoryPort.getId():tool.getId());
		Utils.respondGetWithTemplateAndJson(contents, "saveEdit", response.getResponse(), "text/html");
		return;
	}

}
