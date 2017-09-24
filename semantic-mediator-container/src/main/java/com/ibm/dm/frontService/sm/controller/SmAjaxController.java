
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
package com.ibm.dm.frontService.sm.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.service.Repository;
import com.ibm.dm.frontService.sm.utils.Utils;

@RestController
public class SmAjaxController extends SmcBaseController {

	@RequestMapping(value="/smAjax", method=RequestMethod.GET, produces="text/html" )
	public String smAjaxRequest(HttpServletRequest request) {
		String text =  Utils.getHtmlTemplate("/templates/frames.html");
		System.out.println( "called [/smAjax] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
    	return text;
	}
	
	/**
	 * Replaces a predicate in a model or a resource in a repository<br>
	 * Example:<br>
	 * dm/smAjax/updateResource?id=Prt-102-Copy&resource=base:0002&predicate=http://www.w3.org/2000/01/rdf-schema#comment&newValue=Oh%20my%20GOD!
	 *
	 * @param params
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/updateResource", method=RequestMethod.GET, produces="text/plain" )
	public String smAjaxRequestUpdateResource(
			@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println( "called [/smAjax/updateResource] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
		onEntry(request);
		String msg = null;
		boolean failed = false;
		String resource = params.get("resource");
		String id = params.get("id");
		String predicate = params.get("predicate");
		String newValue = params.get("newText");
		Port p = null;
		if (Strings.isNullOrEmpty(resource) || 
				Strings.isNullOrEmpty(id) || 
				Strings.isNullOrEmpty(predicate)) {
			msg = "Error in request, parameter is missing [" + resource + "|" + id + "|" + predicate + ".";
			failed = true;
		}  else {
			p = getDatabase(request).getPort(id);
			if (null == p || false == p.isTemporary()) {
				msg = "Error - port with id [" + id + "] is missing or not temporary!";
				failed = true;
			}
		}
		if (null != msg) {
			onExit(request, failed, msg, !failed);
			return msg;
		}
		Repository r = (Repository) p.getModelRepository();
		Model m = r.getModel();
		Map<String, String> map =m.getNsPrefixMap();
		String[] splitR = resource.split(":"); 
		resource = map.get(splitR[0]) + splitR[1];
		Resource res = m.getResource(resource);
		Property pred = m.getProperty(predicate);
		Ontology ont = p.getOntology();
		Model ontModel = ont.getModelRepository().getModel();
		boolean isObjectProperty = Utils.resourceHasPropertyValue(ontModel.createResource(pred.toString()), 
				RDF.type, OWL.ObjectProperty);
		if (res.hasProperty(pred)) 
			res.removeAll(pred);
		if (null != newValue) { // we add a predicate. If it is missing, nothing to add and predicate has been removed already above.
			if (isObjectProperty) {
				String uri = Utils.uriFromPrefix(newValue, map);
				res.addProperty(pred, m.createResource(uri));
			} else
				res.addProperty(pred, newValue);
		}
		msg = "Successfuly updated predicate [" + predicate + "] in resource [" + resource + "]:" + 
				((null != newValue)?("replaced with new value [" + newValue + "]."):"removed.");
		r.setDirty();
		r.save();
    	onExit(request, failed, msg, !failed);
    	return msg;
	}

}
