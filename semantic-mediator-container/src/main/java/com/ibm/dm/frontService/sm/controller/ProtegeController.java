
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

package com.ibm.dm.frontService.sm.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import org.apache.jena.rdf.model.*;
import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.RuleSet;
import com.ibm.dm.frontService.sm.data.RuleSet.END_POINT;
import com.ibm.dm.frontService.sm.intfc.imp.OntologyDescription;
import com.ibm.dm.frontService.sm.service.ARdfRepository;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

@RestController
//@RequestMapping("/smProtege")
public class ProtegeController  extends SmcBaseController {
	@RequestMapping(value="/smProtege/big", method=RequestMethod.GET, produces="application/json")
	public String getDatabase4Protege(
			HttpServletRequest request,  HttpServletResponse response) throws Exception
	{
		onEntry(request);
		String result = serviceRequest(request, false);
		onExit(request, false, "", false);
		return result;
	}


	@RequestMapping(value="/smProtege", method=RequestMethod.GET)
	public String getDatabase4ProtegeBig(
//			@PathVariable Map<String, String> path,
			@RequestParam(value="id", required=false, defaultValue="") String id,
			@RequestParam(value="prefix", required=false, defaultValue="") String prefix,
			@RequestParam(value="ns", required=false, defaultValue="") String ns,
			HttpServletRequest request,  HttpServletResponse response) throws Exception
	{
		onEntry(request);
		Database db = getDatabase(request);
		String result="{'error': 'No resutls'}";
		String msg = "";
		boolean failed = false;
		if (Strings.isNullOrEmpty(id) && Strings.isNullOrEmpty(prefix) && Strings.isNullOrEmpty(ns)) {
			result =  serviceRequest(request, true);
		} else {
			try {
				result = "";
				AModelRow item = (AModelRow) db.getItem(id);
				if (null == item)
					item = db.getModelItemByPrefix(prefix);
				if (null == item)
					item = db.getModelItemByNS(ns);
				ARdfRepository r = item.getModelRepository();
				response.addHeader("ETag", r.getEtag());
				//	        	ACConstants.
				result = Utils.modelToText(r.getModel(), null); // default is rdf+xml
			} catch (Exception e) {
				e.printStackTrace();
				failed = true;
				msg =  "Error: " + e.getClass().getName() + ": [" + e.getMessage() + "]";
				return msg;
			} finally {
				if (null != db)
					db.save();
			}

		} 
		onExit(request, failed, msg, false);
		return result;
	}


	private String serviceRequest(HttpServletRequest request, boolean small) {
		Database db = null;
		try {
			db = getDatabase(request); //TODO apply projects
			JSONObject result = new JSONObject();
			List<Ontology> lo = db.getOntologies();
			List<RuleSet> lr = db.getRules();
			JSONArray jo = new JSONArray();
			for (Ontology o : lo) {
				if (o.isReady())
					jo.put(small?smallJson4Model(o):bigJson4Model(o));
			}
			result.put("ontologies", jo);
			JSONArray jr = new JSONArray();
			for (RuleSet r : lr) {
				if (r.isReady())
					jr.put(small?smallJson4Model(r):bigJson4Model(r));
			}
			result.put("rules", jr);
			result.put("filter", db.getFilter());
			System.out.println(result);
			return result.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "{\"Error\": \"" + e.getClass().getName() + ": [" + e.getMessage() + "]\"}";  
		} finally {
			if (null != db)
				db.save();
		}
	}

	private JSONObject smallJson4Model(AModelRow mr) throws JSONException {
		JSONObject o = new JSONObject();
		o.put(AModelRow.IFields.ID, mr.getId());
		o.put(AModelRow.IFields.NAME, mr.getName());
		OntologyDescription od = mr.getOntologyDescription();
		o.put(AModelRow.IFields.MODEL_INSTANCE_NAMESPACE, mr.getModelInstanceNamespace()); 
		o.put(AModelRow.IFields.VERSION, mr.getVersion());
		String d[] = (null == od)? new String[0] : od.getImports();
		o.put("dependencies", Arrays.asList(d));
		//	    	o.put(AModelRow.IFields.PREFIX, mr.getPrefix());
		o.put(AModelRow.IFields.LAST_MODIFIED, mr.getDateModified());
		o.put(AModelRow.IFields.VERSION, mr.getVersion()); // This is now redundant, since the NS is the version.
		o.put(AModelRow.IFields.TAGS, mr.getTags());
		o.put(AModelRow.IFields.ETAG, mr.getEtag());
		o.put(AModelRow.IFields.IS_IMPORTED, mr.isImported());
		return o;
	}
	private JSONObject bigJson4Model(AModelRow mr) throws JSONException {
		JSONObject o = smallJson4Model(mr);
		o.put(AModelRow.IFields.PREFIX, mr.getPrefix());
		o.put(AModelRow.IFields.VIEW_CONFIG, mr.getSavedViewConfig());
		o.put(AModelRow.IFields.IMPORT_URL, mr.getImportUrl());
		if (mr instanceof RuleSet) {
			RuleSet rs = (RuleSet)mr;
			Ontology on = rs.getEndPointOntology(END_POINT.FIRST);
			String onns = "";
			if (null != on)
				onns = on.getVersion();
			o.put(RuleSet.IFields.END1, onns);
			on = rs.getEndPointOntology(END_POINT.SECOND);
			onns = "";
			if (null != on)
				onns = on.getVersion();
			o.put(RuleSet.IFields.END2, onns);
			o.put(RuleSet.IFields.INTERCEPTOR_NAME, rs.getInterceptorName());
			o.put(RuleSet.IFields.REVERSIBLE, rs.isEndPointReversible());
			o.put(RuleSet.IFields.PREFIX, rs.getPrefix());
		}
		return o;
	}

//	@RequestMapping(value="/smProtege", method=RequestMethod.GET, produces="application/rdf+xml")
//	public String getItem4Protege(
//			@RequestParam(value="id", required=false, defaultValue="") String id,
//			@RequestParam(value="prefix", required=false, defaultValue="") String prefix,
//			@RequestParam(value="ns", required=false, defaultValue="") String ns,
//			HttpServletRequest request,  HttpServletResponse response) throws Exception
//	{
//		onEntry(request);
//		boolean failed = false;
//		String msg = "";
//		try {
//			db = getDatabase(request);
//			AModelRow item = (AModelRow) db.getItem(id);
//			if (null == item)
//				item = db.getModelItemByPrefix(prefix);
//			if (null == item)
//				item = db.getModelItemByNS(ns);
//			ARdfRepository r = item.getModelRepository();
//			response.addHeader("ETag", r.getEtag());
//			//	        	ACConstants.
//			return Utils.modelToText(r.getModel(), null); // default is rdf+xml
//		} catch (Exception e) {
//			e.printStackTrace();
//			failed = true;
//			msg =  "Error: " + e.getClass().getName() + ": [" + e.getMessage() + "]";
//			return msg;
//		} finally {
//			if (null != db)
//				db.save();
//			onExit(request, failed, msg, false);
//		}
//	}

	@RequestMapping(value="/smProtege", method=RequestMethod.POST, produces="text/plain")
	@ResponseBody
	public String postItemFromProtege(
			@RequestParam(value="id", required=false, defaultValue="") String id,
			@RequestParam(value="prefix", required=false, defaultValue="") String prefix,
			@RequestParam(value="ns", required=false, defaultValue="") String ns,
			@RequestHeader(value=IConstants.IF_MATCH, required=true) String eTag,
			HttpServletResponse response, HttpServletRequest request) throws Exception
	{
		Database db = null;
		boolean failed = false;
		String msg = "";
		try {
			db = getDatabase(request);
//			String eTag = ifMatch.hasMoreElements())?ifMatch.nextElement():null;
			AModelRow item = (AModelRow) db.getItem(id);
			if (null == item && false == Strings.isNullOrEmpty(ns))
				item = db.getModelItemByNS(ns);
			if (null == item && false == Strings.isNullOrEmpty(prefix))
				item = db.getModelItemByPrefix(prefix);
			if (null == item)	        		
				throw new Exception("Error: item associated with this update is not recognized.");
			if (item.isImported())
				throw new Exception("Error: Imported ontologies cannot by posted as updates.");
			ARdfRepository r = item.getModelRepository();
			if (null != eTag && null != r.getEtag() && false == eTag.equals(r.getEtag())) {
				msg = "Error: Precondition failed with code 412";
				failed = true;
				throw new Exception(msg);
			}
//			HttpEntity entity = ((BasicHttpEntityEnclosingRequest) request).getEntity();
			String contentType = request.getContentType(); //entity.getContentType().getValue();
			InputStream is= request.getInputStream(); //entity.getContent();
			FileItemIterator it = Utils.getMultipartIterator(request, is, contentType);
			if (! it.hasNext()) {
				failed = true;
				msg = "Error: No content in this post";
				return msg;
			}
			FileItemStream in = it.next();
			String name = in.getFieldName();
			String ct = in.getContentType();
			if (false == ct.equals("application/rdf+xml")) {
				failed = true;
				msg = "Error: Content is not [application/rdf+xml] content type";
				return msg;
			} if (false == name.equals("ontology")) {
				failed = true;
				msg = "Error: must post an [ontology] as name of content part.";
				return msg;
			}
			Model m = Utils.modelFromStream(in.openStream(), IConstants.RDF_XML, null);
			if (null == m) {
				failed = true;
				msg = "Error: Failed to create a model from the input.";
				return msg;
			}
			r.init(m);
			OntologyDescription od = OntologyDescription.fromModel(db, m, null); //Stream(new FileInputStream(new File(getFileName())), base);
			item.setOntologyDescription(od);
			r.save();
			response.setHeader(IConstants.ETAG, r.getEtag());
			msg = "Success";
			return msg;
		} catch (IOException e) {
			e.printStackTrace();
			failed = true;
			msg = "Error: " + e.getClass().getName() + ": [" + e.getMessage() + "]";
			return msg;
		} catch (FileUploadException e) {
			e.printStackTrace();
			failed = true;
			msg =  "Error: " + e.getClass().getName() + ": [" + e.getMessage() + "]";  
			return msg;
		} finally {
			if (null != db)
				db.save();
			onExit(request, failed, msg, true);
		}
	}

}
