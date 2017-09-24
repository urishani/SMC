
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

/**
 * The work leading to these results have received funding from the Seventh Framework Programme
 * SPRINT ICT-2009.1.3 Project Number: 257909
 */

package com.ibm.dm.frontService.sm.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

public class OSLCHelper {
	protected final Database mDatabase;
	
	public OSLCHelper(Database db) {
		mDatabase = db;
	}

	private Database getDatabase() {
		return mDatabase;
	}
	
	public void respondForServiceProvider(String domain,
			HttpServletRequest request, HttpServletResponse response, ISmService smService) throws Exception {
		String url = request.getRequestURL().toString();
		Database db = Database.create(request);
		if (url.indexOf('?') >= 0)
			url = url.substring(0, url.indexOf('?'));
		Port port = db.getPort4Domain(domain);
		boolean html = Utils.willAccept(IConstants.HTML, request);

		JSONObject contents = new JSONObject();
		contents.put("accessName", port.getAccessName());
		contents.put("host", getDatabase().getHost(true, true)); //Utils.getHost(null, true));
		String type = IConstants.TURTLE;
		String contentTypeParam = smService.getParam(request, "contentType");
		if (! html)
			type = request.getHeader("Accept"); //.getValue();
		else if (false == Strings.isNullOrEmpty(contentTypeParam))
			type = contentTypeParam;
		String t = Utils.loadFromClassPath("/rdf/oslc.provider.ttl");	
		JSONArray prefixDefinitions = new JSONArray();
		contents.put("prefixDefinition", prefixDefinitions);
		Map<String, String> m = port.getModelRepository().getPreferredPrefixes();
		JSONObject p = null;
		for (String prefix: m.keySet()){
			p = new JSONObject();
			p.put("prefix", prefix);
			p.put("nameSpaceUri", m.get(prefix));
			p.put("semicolon",";");
			prefixDefinitions.put(p);
		}
		p.put("semicolon", ".");  // Last one is terminated with a fullstop.
		t = Utils.mergeJsonWithHtml(t, contents);
		if (false == IConstants.TURTLE.equals(type)) {
			Model model = Utils.modelFromString(t, IConstants.TURTLE, null);
			t = Utils.modelToText(model, type);
		}
		if (html) {
			contents.put("id", port.getId());
			contents.put("contentType", type);
			contents.put("name", port.getName());
			contents.put("description", port.getDescription()); 
			contents.put("url", url.toString());
			contents.put("header", "OSLC AM Service Catgalog");
			if (t.startsWith("@")) t = "\n"+t; // @ is a special char when at start of an embedded field value.
			contents.put("rdf", t);
			contents.put("canmodify", "false");
			// Only current version for OSLC service provider and queries.
			contents.put("version", "");
			contents.put("versionInfo", "");
			contents.put("noBackLinks", "true");
    			contents.put("host", getDatabase().getHost(true, true)); //Utils.getHost(null, true));
			contents.put("hideGraphics", "hidden");
			contents.put("comments", "Generated automatically on the fly.");


			Utils.respondGetWithTemplateAndJson(contents, "rdf4oslc", response);
		}  else
			Utils.respondWithText(response, t, type);
		return;
	}

	/**
	 * Processes the request for an OSLC response.
	 * If the response param is null, the result is returned as a String.
	 * @param path
	 * @param segments
	 * @param request
	 * @param response
	 * @param smService
	 * @return
	 * @throws Exception 
	 */
	public String responseForServiceCatalog(String path, String[] segments, 
			HttpServletRequest request, HttpServletResponse response, ISmService smService) throws Exception {
		String url = request.getRequestURL().toString();
		Database db = Database.create(request);
		if (url.indexOf('?') >= 0)
			url = url.substring(0, url.indexOf('?'));
		boolean html = Utils.willAccept(IConstants.HTML, request);

		JSONObject contents = new JSONObject();
		contents.put("host", getDatabase().getHost(true, true)); //oslcService.getDatabase().getHost(true)); //Utils.getHost(null, true));
		contents.put("oauth-authorize-uri", "TBD") ; //RmpsConfig.getInstance().getOAuthProviderProperties().userAuthorizationURL);
		contents.put("oauth-access-token-uri", "TBD") ; // RmpsConfig.getInstance().getOAuthProviderProperties().accessTokenURL);
		contents.put("oauth-request-token-uri", "TBD") ; // RmpsConfig.getInstance().getOAuthProviderProperties().requestTokenURL);
		contents.put("comments", "Generated automatically on the fly.");
		JSONArray sps = new JSONArray();
		contents.put("serviceProviderUrl", sps);
		List<Port> reps = db.getRepositories();
		JSONObject last = null;
		for (Port port : reps) {
			last = new JSONObject();
			sps.put(last);
			last.put("id", port.getId());
			last.put("name", port.getName());
			last.put("description", port.getDescription()); 
			last.put("accessName", port.getAccessName());
			last.put("ontologyNS", port.getOntology().getNameSpaceUri().toString());
			last.put("comma", ",");
		}
		if (last != null)
			last.put("comma", "."); // last of the list is terminated with semicolon to make it correct turtle.
		contents.put("serviceProvider", sps);  // used here too, with all params. First use needs only the accessName.
		String type = IConstants.TURTLE;
		String contentTypeParam = smService.getParam(request, "contentType");
		if (! html)
			type = request.getHeader("Accept");
		else if (false == Strings.isNullOrEmpty(contentTypeParam))
			type = contentTypeParam;
		String t = Utils.loadFromClassPath("rdf/oslc.provider.catalog.ttl");	
		t = Utils.mergeJsonWithHtml(t, contents);
		if (false == IConstants.TURTLE.equals(type)) {
			Model model = Utils.modelFromString(t, IConstants.TURTLE, null);
			t = Utils.modelToText(model, type);
		}
		if (html) {
			contents.put("id", "");contents.put("contentType", type);contents.put("url", url.toString());
			contents.put("header", "OSLC AM Service Catgalog");
			if (t.startsWith("@")) t = "\n"+t; // @ is a special char when at start of an embedded field value.
			contents.put("rdf", t);
			contents.put("canmodify", "false");
			// Only current version for OSLC service provider catalog and queries.
			contents.put("version", "");
			contents.put("versionInfo", "");
			contents.put("noBackLinks", "true");
    		contents.put("host", getDatabase().getHost(true, true)); //Utils.getHost(null, true));
			contents.put("hideGraphics", "hidden");

			return Utils.respondGetWithTemplateAndJson(contents, "rdf4oslc", response);
		}  else {
			Utils.respondWithText(response, t, type);
			return t;
		}
	}

	public void repsondForOslcQuery(List<Resource> R,
			HttpServletRequest request, HttpServletResponse response, ISmService smService, String id) throws FileNotFoundException, IOException, ClassNotFoundException, JSONException {
		String url = request.getRequestURL().toString(); //Line().getUri();
		//Database db = Database.create();
		if (url.indexOf('?') >= 0)
			url = url.substring(0, url.indexOf('?'));
		boolean html = Utils.willAccept(IConstants.HTML, request);
		String t = Utils.loadFromClassPath("/rdf/oslc.query.ttl");
		JSONObject contents = new JSONObject();
		contents.put("url", url);
		String uriType = "rdf:type <http://open-services.net/ns/am#Resource>" +
		((R.size() == 0)?".":";");
		contents.put("uriType", "@"+uriType);
		JSONArray members = new JSONArray(), resources = new JSONArray();
		contents.put("members", members);
		JSONArray nextPage = new JSONArray();
		contents.put("nextPage", nextPage);
		if (R.size() > 0) {
			JSONObject member = new JSONObject();
			member.put("resources", resources);
			members.put(member);
			JSONObject r = null;
			for (Resource rs: R) {
				r = new JSONObject();
				r.put("resource", rs.toString());
				r.put("comma", ",");
				resources.put(r);
			}
			if (null != r)
				r.put("comma", ".");
		}

		t = Utils.mergeJsonWithHtml(t, contents);

		String type = IConstants.TURTLE;
		String contentTypeParam = smService.getParam(request, "contentType");
		if (html) { 
			if (false == Strings.isNullOrEmpty(contentTypeParam))
				type = contentTypeParam;
		} else
			type = request.getHeader("Accept");
		if (false == IConstants.TURTLE.equals(type)) {
			Model model = Utils.modelFromString(t, IConstants.TURTLE, null);
			t = Utils.modelToText(model, type);
		}
		if (html) {
			contents.put("id", id);
			contents.put("contentType", type);
			contents.put("header", "OSLC AM Query Service");
			if (t.startsWith("@")) t = "\n"+t; // @ is a special char when at start of an embedded field value.
			contents.put("rdf", t);
			contents.put("canmodify", "false");
			// Only current version for OSLC service provider and queries.
			contents.put("version", "");
			contents.put("versionInfo", "");
    		contents.put("host", getDatabase().getHost(true, true)); //Utils.getHost(null, true));
			contents.put("hideGraphics", "hidden");

			Utils.respondGetWithTemplateAndJson(contents, "rdf4oslc", response);
		} else { 
			Utils.respondWithText(response, t, type);
		}
	}
}

