
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

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.data.SmBlobs;
import com.ibm.dm.frontService.sm.service.ISmInfo;
import com.ibm.dm.frontService.sm.utils.IConstants;

@RestController
public class ServerManagementController extends SmcBaseController {
	@RequestMapping(value="/server/management/{project}/sync", method=RequestMethod.GET, produces=IConstants.JSON)
	public String projectSync(
			@PathVariable("project") String project,
			HttpServletRequest request,  HttpServletResponse response) throws Exception
	{
		return doSync(request);
	}

	@RequestMapping(value="/management/sync", method=RequestMethod.GET, produces=IConstants.JSON)
	public String sync(
            HttpServletRequest request) throws Exception
	{
		return doSync(request);
	}
	
	@RequestMapping(value="/server/management/sync", method=RequestMethod.GET, produces=IConstants.JSON)
	public String serverSync(
            HttpServletRequest request) throws Exception
	{
		return doSync(request);
	}

	static int cnt = 1;
	private String doSync(HttpServletRequest request) throws Exception {
		Database db = null;
		boolean failed = false;
		String msg = "";
		try {
			cnt = (cnt++)%100;
			if (0 == cnt) System.out.println(".");
			else
				System.out.print(".");
			//onEntry(request);
			db = getDatabase(request);
			JSONObject status = new JSONObject();
			JSONObject repositories = new JSONObject();
			JSONObject buttons = new JSONObject();
			JSONObject sync = new JSONObject();

			for (Port prt : db.getPorts()) {
				if (prt.isTemporary() || prt.isTool() || SmBlobs.ATTACHMENTS_PORT_ID.equals(prt.getId()))
					continue;
				repositories.put(prt.getId() + "-port-sync", prt.getField("portSync").replace(Port.SYNC_IRRELEVANT, ""));
				buttons.put(prt.getId() + "-port-clear", prt.canClear());
				if (prt.isRepository() && ! prt.isTool())
					sync.put(prt.getAccessName(), prt.canClear());
			}

			status.put("synchronized", sync);
			status.put("repositories", repositories);
			status.put("buttons", buttons);

			return status.toString();
		} catch (Exception e) {
			throw new Exception(e);
		} finally {
			if (failed)
				onExit(request, failed, msg, false);
		}
    }

	@RequestMapping(value="/server/management/login", method={RequestMethod.GET, RequestMethod.POST}, produces=IConstants.PLAIN_TEXT_TYPE)
	public String login() {
		return "OK";
	}

	@RequestMapping(value="/server/management/query", method=RequestMethod.GET, produces=IConstants.JSON)
	public String doQuery(
			@RequestParam(value="notes", required=false, defaultValue="") String notes,
			@RequestParam(value="version", required=false, defaultValue="") String ver,
			@RequestParam(value="var", required=false, defaultValue="") String var,
			@RequestParam(value="post", required=false, defaultValue="") String accessName,
            HttpServletRequest request) throws Exception {

		Database db = getDatabase(request);
		JSONObject json = doQuery(notes, ver, var, accessName, request, db);
		System.out.println("[" + new Date() + "]: sm query response [" + json.toString() + "]");
		return json.toString();
	}
	
	@RequestMapping(value="/server/management/notesHistory", method=RequestMethod.GET, produces=IConstants.HTML)
	public String getNotesHistory () {
		return ISmInfo.NOTES_HISTORY;
	}

	private JSONObject doQuery(String notes, String ver, String var, String accessName,
			HttpServletRequest request, Database db) throws Exception {
		try	{
			JSONObject json = new JSONObject();
			if (null != accessName) {
				json = getOntology(accessName, request, db);
			} else if (null != notes) {
				json.put("@notes", ISmInfo.NOTES);
			} else if (null != ver) {
				json.put("version", ISmInfo.VERSION);
			} else if (false == Strings.isNullOrEmpty(var)) {
				json.put("var", var);
				json.put("value", db.getVar(var, "0"));
			}
			return json;
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	private JSONObject getOntology(String accessName, HttpServletRequest request, Database db) throws JSONException {
		JSONObject json = new JSONObject();
//		try	{
			//			String accept = request.getFirstHeader("accept").getValue();
			for (Port prt : db.getPorts() /*PortType.post)*/) {
				if (false == prt.canPost())
					continue;
				if (null != accessName && accessName.equals(prt.getAccessName())) {
					Ontology ontology = prt.getOntology();
					if (null != ontology) {
						String namespace = ontology.getModelInstanceNamespace();
						String version = ontology.getVersion();
						json.put("namespace", null != namespace ? namespace : "");
						json.put("version", null != version ? version : "");
						return json;
					}
				}
			}
			return json;
//		} catch (Exception e) {
//			throw new Exception(e);
//		}
//
	}
}
