
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
 * Copyright IBM 2011 All Rights Reserved
 * The work leading to these results have received funding from the Seventh Framework Programme
 * SPRINT ICT-2009.1.3 Project Number: 257909
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 */
package com.ibm.dm.frontService.sm.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.data.ADatabaseRow;
import com.ibm.dm.frontService.sm.data.ADatabaseRow.IFields;
import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.data.ANamedRow;
import com.ibm.dm.frontService.sm.data.Catalog;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Friend;
import com.ibm.dm.frontService.sm.data.Mediator;
import com.ibm.dm.frontService.sm.data.Mediator.SERVICE;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.data.Port.PortType;
import com.ibm.dm.frontService.sm.data.RuleSet;
import com.ibm.dm.frontService.sm.data.RuleSet.END_POINT;
import com.ibm.dm.frontService.sm.intfc.IInputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IModelHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IOutputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IRulesHandle;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.SMCLogger.SMCJobLogger;
import com.ibm.dm.frontService.sm.utils.Utils;

public class SmManager
{
	private final Database mOwner;
	public SmManager(Database owner) {
		mOwner = owner;
	}

	private Database getDatabase() {
		return mOwner;
	}
    private List<String> messages = new ArrayList<String>();
    public void doGet(HttpServletRequest request, HttpServletResponse response, ISmService service) throws Exception
    {
    	Database db = getDatabase();
    	synchronized(db) {
    		doGet_Base(request, response, service);
    	}
    }
    public synchronized void doGet_Base(HttpServletRequest request, HttpServletResponse response, ISmService service) throws Exception
    {
    	Database database = getDatabase();

    	// we now may have two "action"s in the same form, so we distinguish with two parameters.
    	String action = service.getParam(request, "action");
    	/* --------------
    	 * The next fields are used as follows: 
    	 * If a "build" command is issued on an ontology,
    	 * Than the autoCreate will be set to true, and the edit panel for the repository will 
    	 * show a checkbox initializeForOSLC to be used. The hidden field initialize is used
    	 * to carry the value of the autoCreate flag from the "build" action.
    	 * Now, that will be used to decide how to follow a update or cancel from the repository 
    	 * edit result. If cancel - the row will be removed. If save, it will be saved empty, or
    	 * initialized for OSLC depending on the checkBox result.
    	 */
    	boolean autoCreate = false;
    	String initializeForOSLC = service.getParam(request, "initializeForOSLC");
    	String initialize = service.getParam(request, "initialize"); 
    	//-----------------------
    	String port_type = service.getParam(request, "portType");
    	String subAction = service.getParam(request, "subAction");
    	String fromRow = service.getParam(request, "fromRow");
    	String id = service.getParam(request, "id");
    	String create = service.getParam(request, "create");
    	String _import = service.getParam(request, "import");
//       	String friendId = service.getParam(request, "friendId");
        String importFromFriend = service.getParam(request, "importFromFriend");
    	String createWithTags = service.getParam(request, "createWithTags");
    	String portType = service.getParam(request, "portType");
    	String update = service.getParam(request, "update");
//    	String showActive = service.getParam(request, "showActive");
    	String refresh = service.getParam(request, "refresh");
    	String setVar = service.getParam(request, "var");
    	String varVal = service.getParam(request, "val");
    	String columns = service.getParam(request, "columns");

    	ADatabaseRow row = database.getItem(id);

    	if (false == Strings.isNullOrEmpty(create) && null != _import) {
    		Friend friend = database.getFriend(importFromFriend);
    		if (null == friend) { 
    			setMessage("Failed: import from [" + importFromFriend + "]. Not found or not Ready");
    			showMainDialog(database, response, false /*Utils.isDm(request)*/);
    			return;
    		}

    		JSONObject items = friend.getItemsFromFriend();
    		if (null != Utils.safeGet(items,"Error")) {
    			setMessage("Failed: import from [" + importFromFriend + "]. " + (String)Utils.safeGet(items,"Error"));
    			showMainDialog(database, response, false /*Utils.isDm(request)*/);
    			return;
    		}
    		String itemsType = "ontologies";
    		if (create.equals("ruleSet"))
    			itemsType = "rules";
    		JSONArray _items = (JSONArray)Utils.safeGet(items,itemsType);

    		if 	(_import.equals("Import all selected")) { // create imported elements
    			String summary = "Importing from [" + friend.getDisplayId() + "]:";
    			String ids = service.getParam(request, "ids");
    			if (Strings.isNullOrEmpty(ids))
    				return;
    			// get the details from the friend.
    			String id_s[] = ids.split(",");
    			for (String aId: id_s) {
    				AModelRow newRow = null;
    				JSONObject rowInfo = null;
    				for (int i=0; i < _items.length(); i++) {
    					JSONObject item = (JSONObject)_items.get(i);
    					if (aId.equals(Utils.safeGet(item,"id"))) {
    						rowInfo = item;
    						break;
    					}
    				}
    				if (null == rowInfo)
    					continue;

    				if (create.equalsIgnoreCase("Ontology"))
    					newRow = database.createOntology();
    				else
    					newRow = database.createRuleSet();

    				String info = (String) Utils.safeGet(rowInfo, AModelRow.IFields.MODEL_INSTANCE_NAMESPACE);
    				if (false == Strings.isNullOrEmpty(info))
    					newRow.setField(AModelRow.IFields.MODEL_INSTANCE_NAMESPACE, info);

    				info = (String) Utils.safeGet(rowInfo, AModelRow.IFields.NAME);
    				newRow.setName(info);
    				info = (String) Utils.safeGet(rowInfo, AModelRow.IFields.PREFIX);
    				newRow.setField(AModelRow.IFields.PREFIX,info);
    				newRow.setField(AModelRow.IFields.TAGS, createWithTags);
    				info = (String) Utils.safeGet(rowInfo, AModelRow.IFields.VIEW_CONFIG);
    				newRow.setField(AModelRow.IFields.VIEW_CONFIG, info);

    				info = (String) Utils.safeGet(rowInfo, AModelRow.IFields.IMPORT_URL);
    				if (Strings.isNullOrEmpty(info))
    					newRow.setSmcImportUrl(friend.getId(), aId);
    				else newRow.setImportUrl(info);
    				if (newRow instanceof RuleSet) {
    					//    					RuleSet newRs = (RuleSet)newRow;
    					info = (String) Utils.safeGet(rowInfo, RuleSet.IFields.END1);
    					Ontology o = (Ontology) AModelRow.findModelForNameSpaceByVersion(database.getOntologies(), info);
    					((RuleSet) newRow).setEndPoint(END_POINT.FIRST, o);
    					info = (String) Utils.safeGet(rowInfo, RuleSet.IFields.END2);
    					o = (Ontology) AModelRow.findModelForNameSpaceByVersion(database.getOntologies(), info);
    					((RuleSet) newRow).setEndPoint(END_POINT.SECOND, o);
    					info = (String) Utils.safeGet(rowInfo, RuleSet.IFields.INTERCEPTOR_NAME);
    					((RuleSet) newRow).setInterceptorName(info);
    					Boolean binfo = (Boolean) Utils.safeGet(rowInfo, RuleSet.IFields.REVERSIBLE);
    					newRow.setField(RuleSet.IFields.REVERSIBLE, binfo.toString());
    				}
    				newRow.markModified();
    				System.out.println("Imported meta-data for new item [" + newRow.getDisplayId() + "]");
    				String msg = newRow.importModel();
    				System.out.println("Imported model for item [" + newRow.getDisplayId() + "]: " + (Strings.isNullOrEmpty(msg)?"OK": msg));
    				summary += (summary.endsWith(":")?"":", ") + newRow.getDisplayId() +
   						(Strings.isNullOrEmpty(msg)?"-OK":"-X");
    			}
    			setMessage(summary + "].");
    		} else {
    			// Content of the Json object are coded with different properties than the "create" parameter. So fix them.
    			JSONObject content = new JSONObject();
    			_items = (JSONArray) Utils.safeGet(items, itemsType);
    			content.put("what", create);
    			content.put("createWithTags", createWithTags);
    			content.put("friend", friend.getDisplayId());
    			content.put("friendId", friend.getId());
    			content.put("friendIp", friend.getIpAddress());
    			content.put("items", _items);  // what to create is the kind of items to use.
    			// Now go over them and fixs some additional attributes
    			List<AModelRow> localItems = new ArrayList<AModelRow>();
    			localItems.addAll(database.getOntologies());
    			localItems.addAll(database.getRules());
    			for (int i = 0; i < _items.length(); i++) {
    				JSONObject item = (JSONObject) _items.get(i);
    				String ns = (String) Utils.safeGet(item,AModelRow.IFields.VERSION);
    				if (Strings.isNullOrEmpty(ns))
    					ns = (String) Utils.safeGet(item,AModelRow.IFields.MODEL_INSTANCE_NAMESPACE);
    				AModelRow localItem = AModelRow.findModelForNameSpaceByVersion(localItems, ns);
    				item.put("hidden", (null != localItem)?"hidden":"");
    				item.put("comment", (null != localItem)?"Already Exists":"Can be imported");
    			}
    			content.put("data", _items.toString());
    			Utils.respondGetWithTemplateAndJson(content, "importFromFriends", response);
    			return;
    		}
    		showMainDialog(database, response, false); //Utils.isDm(request));
    		return;
    	}

    	if ("Build".equals(action)) { // comoing from an ontology, triggers creating a new repository for that ontology
    		create = "port";
    		if (Strings.isNullOrEmpty(port_type)) 
    			portType = Port.PortType.repository.toString();
    		if (row instanceof Port && ((Port) row).isRepository() && ! ((Port) row).isTool()) {
    			create = "tool";
    			portType = Port.PortType.tool.toString();
    		}
    		if (row instanceof RuleSet) {
    			create = "mediator";
    			portType = Port.PortType.undefined.toString();
    		}
    		
    		autoCreate= true;
    		createWithTags = database.getVar(Database.Vars.filterVar.toString());
    	}

    	if (null != create) {	
    		ADatabaseRow item = createRow(create, database, portType, createWithTags, id);
    		if (null == item) {
    			setMessage("Failed to create an item of type [" + portType + "].");
    			showMainDialog(database, response, false); //Utils.isDm(request));
    			return;
    		}
    		autoCreate = true;

    		// now - continue to edit the item rather than showing a created item that is not fully specified.
    		create = null;
    		action = "E";
    		id = item.getId();
    	}

    	// RDF list buttons
    	String clearAllHistory = service.getParam(request, "ClearAllHistory");
    	String clear5History = service.getParam(request, "Clear5History");

    	if (null != setVar) {
    		database.setVar(setVar, varVal);
    		response.setStatus(HttpStatus.SC_OK);
    		database.save();
    		if (null == refresh)
    		return;
    	}
    	ADatabaseRow item = null;
    	String domainName = null;
    	if (false == Strings.isNullOrEmpty(id)) { //null != id && false == "".equals(id)) {
    		item = database.getItem(id);
    		if (item instanceof Port) {
    			Port port = (Port)item;
    			if (port.isRepository())
    				domainName = port.getAccessName();
    		}
    	}

    	ARdfRepository repository = null;
    	if (item instanceof AModelRow)
    		repository = ((AModelRow)item).getModelRepository();

    	boolean clear = (item instanceof Port && ((Port)item).isRepository() && "C".equals(action));
    	boolean delete = (item instanceof Port && ((Port)item).isRepository() && "D".equals(action));

    	String title = null;
    	if (null != item) {
    		String portTypeTitle = "";
    		if (item instanceof Port) {
    			portTypeTitle = ": a " + ((Port)item).getType();
    		}
    		title = item.getCollectionName() + portTypeTitle + " [" + id + "]";
    	}

    	if (clear || delete || null != clear5History || null != clearAllHistory) {
    		String collection = item.getCollectionName();

    		title =
    			collection.substring(0,1).toUpperCase() + collection.substring(1) +  // capitalize collection name
    			" [" + id + "]: " + item.getName(); // + ", Domain [" + domainName + "]";
    	}

    	if (null != clearAllHistory) {
    		assert null != repository;
    		String msg = repository.clearHistory(-1);
    		processModel(response, repository, "ShowList", title, id, columns, database.getVar("contentType"), fromRow, subAction, null, null);
//    		processDomain(response, domain, "L", database, id);
    		setMessage(msg);
    		return;
    	}
    	if (null != clear5History) {
    		assert null != repository;
    		String msg = repository.clearHistory(4);
    		processModel(response, repository, "ShowList", title, id, columns, database.getVar("contentType"), fromRow, subAction, null, null);
//    		processModel(response, domainName, "ShowList", null, title, id, true);
//    		processDomain(response, domain, "L", database, id);
    		setMessage(msg);
    		return;
    	}
    	if (clear) {
    		assert null != repository;
    	    String msg =  "Port [" + id + "]: ";
    		Port port = (Port)item; // This works only with Ports, so should be safe to cast.
           	synchronized(port) {
        		if (port.isCatalog()) {
        			msg = ((Catalog)port).clear();
        		} else
           		if (port.isSync()) {
//           			if (port.getId().equals(SmBlobs.ATTACHMENTS_PORT_ID))
//           				port.clear(repository);
                   	msg += repository.clearRepository();
               		SmContainer.getContainer(getDatabase()).resetDomain(domainName);
               		port.syncReset();
//               		if (port instanceof AModelRow)
//               			((AModelRow)port).setIsImported(false);
           		} else
           			msg += "Clear failed. Domain of this port is out of sync.";
          	}
    		setMessage(msg);
    	} else if (delete) {
    		assert item instanceof Port;
    	    String msg =  "Port [" + id + "]: ";
    	    Port port = (Port)item;
           	synchronized(port) {
           		if (port.isSync()) {
           		    String bmsg = "";
           		    if (null == repository) 
           		    	bmsg = "Deleted";
           		    else 
           		    	bmsg = repository.deleteRepository();
            		if (bmsg.indexOf("Deleted") >= 0) {
            			msg = database.delete(id); // + ". " + bmsg;
                   		SmContainer.getContainer(getDatabase()).resetDomain(domainName);
                   		port.syncReset();
            		} else
            			msg += "Delete Failed. " + bmsg;
           		} else
           			msg += "Delete failed. Domain of this port is out of sy)nc.";
          	}
    		setMessage(msg);
    	}

    	//String action = (String) params.getParameter("action");
    	if (null != action)
    	{
    		String verb = action;
    		if (verb.equals("Edit Repository")) {
    			Port fromPort = database.getPort(id);
    			Port tmpRepository = database.createPort(fromPort); // temp reporisory
//    			Mediator tmpMediator = database.findMediator(tmpRepository, true); // look for a temporary mediator for which this temp repository is input.
//    			if (null == tmpMediator) {
//    			   tmpMediator = database.createMediator(true);
//    			   tmpMediator.setInputPortId(tmpRepository.getId());
//    			   tmpMediator.setOutputPortId(id);
//    			   tmpMediator.setRuleSetId(null);
//    			}
    			tmpRepository.setOntologyId(fromPort.getOntologyId());
    			domainName = "tmpRepository" + tmpRepository.getId();
    			tmpRepository.setAccessName(domainName);
    			Repository r = (Repository) tmpRepository.getModelRepository();
    			Map<String, String> map = r.cloneModel(fromPort.getModelRepository().getModel(), true);
    			BiMap<String, String> bmap = HashBiMap.create(map);
    			map = bmap.inverse();
    			SmContainer.getContainer(getDatabase()).writeToRepositoryMap(map);
    			
    			System.out.println("Cloning map:\n" + Utils.mapS2csv(map, ",\n"));
    	  		title = "Repository Edit Session [" + tmpRepository.getId() + "], Domain [" + domainName + "]";
    			processModel(response, r, "ShowList", title, tmpRepository.getId(), columns, database.getVar("contentType"), fromRow, subAction, null, null);
    			return;
    		}
    		else if (verb.equals("Download")) {
    			if (false == item.isReady()) {
    				Utils.respondWithText(response, "Contents of [" + id + "] are not ready yet!");
    				return;
    			}
    			byte contents[] = item.getContentsAsBytes(database.getVar(Database.Vars.contentType.toString()));
    			if (null == contents) {
    				Utils.respondWithText(response, "No contents to download from [" + id + "]");
    				return;
    			}
//    			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_OCTET);
    			//Utils.respondWithBytes(response, contents, ContentTypes.APPLICATION_OCTET);
    			Utils.respondWithAttachment(response, row.getCollectionName() + "-" + id + ARdfRepository.getDomainExt(), contents);
    			return;
    		}
    		else if (verb.equals("compare")) { // && (null != domainName || item instanceof AModelRow)) {
    			String version = service.getParam(request, "version");
    			String version2 = service.getParam(request, "version2");
    			if (null == version || null == version2) {
    				Utils.respondWithText(response, "<html><body><h1>Must select two versions to compare with</h1></body></html>");
    				return;
    			}
    			ARdfRepository r = item.getModelRepository(); //createRdfRepository();
    			JSONObject contents = r.compareHistory(version, version2);
    			contents.put("collection", item.getCollectionName());
    			contents.put("id", id);
    			contents.put("name", row.getName());
    			Utils.respondWithText(response,
    					Utils.mergeJsonWithHtml(Utils.getHtmlTemplate("templates/compare.html"), contents));
    			return;
    		}
    		else if (verb.equalsIgnoreCase("D") && !delete) { // for rows other than Port repositories.
    			setMessage(database.delete(id));
    		}
    		else if (verb.equalsIgnoreCase("E")){
    			JSONObject result = edit(id, database, portType, autoCreate);
    			String error = (String) Utils.safeGet(result, "error");
    			if (null != error)
    				setMessage(error);
    			else
    			{
    				if (Utils.willAccept(IConstants.HTML, request)) {
    					Utils.respondGetWithTemplateAndJson(
    							(JSONObject) Utils.safeGet(result, "contents"),
    							(String)Utils.safeGet(result, "templateName"), response);
    					return;
    				} else if (Utils.willAccept(IConstants.JSON, request)) {
    					Utils.respondWithJson(response, (JSONObject) Utils.safeGet(result, "contents"));
    					return;
    				} else
    					setMessage("Accept type for editing id [" + id + "] is illegal");
    			}
    		}
    		else if (verb.equalsIgnoreCase("L"))
    		{
    			loadModelFileFromFileDialog(id, response, database);
    			return;
    			// setMessage ("Loading from file not implemented yet");
    		}
    		else if (verb.equalsIgnoreCase("I"))
    		{
    			String msg = importModel(database, id);
    			if (Strings.isNullOrEmpty(msg))
    				msg = "Successful import of model into [" + id + "].";
    			setMessage(msg);
    		}
    		else if (verb.equalsIgnoreCase("ShowRDF") || verb.equalsIgnoreCase("ShowList") || verb.equalsIgnoreCase("ShowShort"))
    		{
    			ARdfRepository r = null;
    			String contentType =  service.getParam(request, Database.Vars.contentType.toString());
    			String version = service.getParam(request, "version");
    			String changeSet = service.getParam(request, "changeSet");
    			if (false == Strings.isNullOrEmpty(contentType)) {
					database.setVar(Database.Vars.contentType.toString(), contentType);
    			} else
    				contentType = database.getVar(Database.Vars.contentType.toString());
    			if (row instanceof Port) {
    				Port port = (Port) row;
    				title = "Port: a " + port.getType() + " [" + id + "]: " + port.getName() + ", Domain [" + domainName + "]";
    				r =  port.getModelRepository(); // Repository.create(port);
//    				processModel(response, port.getAccessName(), verb, null, title, id, true); 				
    			} else if (row instanceof AModelRow) {
    				AModelRow modelRow = (AModelRow)row;
    				title = "Model [" + id + "]: " + modelRow.getModelInstanceNamespace();
    				r = modelRow.getModelRepository(); //ModelRepository.create(modelRow);
//     				processModel(response, null, verb, modelRow.getFileName(),title , id, false);
    			} else {
    				Utils.respondWithText(response, "Illegal action=[" + verb + "] for item [" + id + "]");
    				return;
    			}
				processModel(response, r, verb, title, id, columns, contentType, fromRow, subAction, version, changeSet);
    			return;
    		} else if (verb.startsWith("ShowMediator")) { // can serve ShowMediator and ShowMediatorTrace (TBD).
    			SmContainer container = SmContainer.getContainer(database);
    			String msg = title + ": Failed.";
    			if (false == row instanceof Mediator) {
    				msg = title + ": " + verb + " failed as an illegal action";
    			} else {
    				// show repository associations
    				StringBuffer text = new StringBuffer();
    				Mediator mdt = (Mediator)row;
    				String inputPortId = mdt.getInputPortId(), outputPortId = mdt.getOutputPortId();
    				msg = title + ": Nothing to show";
    				JSONObject contents = new JSONObject();
    				if (null != inputPortId && null != outputPortId) {
    					Port inputPort = database.getPort(inputPortId), outputPort = database.getPort(outputPortId);
    					if (null != inputPort && null != outputPort) {
    						String d1 = inputPort.getAccessName(), d2 = null;
    						if (Port.PortType.export.equals(outputPort.getType()))
    							d2 = container.getDomain(outputPort);
    						else
    							d2 = outputPort.getAccessName();
    						contents.put("displayId", mdt.getDisplayId());
    						contents.put("fromDisplayId", inputPort.getDisplayId());
    						Model fromModel = inputPort.getModel();
    						contents.put("toDisplayId", outputPort.getDisplayId());
    						Model toModel = outputPort.getModel();
    						text.append("<h1>Resource links for mediator [" + mdt.getId() + "]: Between domain [" + d1 + "] and domain [" + d2 + "]</h1>\n");
    						if (null != d1 && null != d2) {
    							String repositoryPair = SmContainer.makePair(d1, d2);
    							container.readRepositoryMap();
    							Map<String, String> resourceMap = SmContainer.repositoryMap.get(repositoryPair);
    							if (null == resourceMap || resourceMap.size() == 0)
    								msg = title + ": No associations registered for this mediator.";
    							else {
        							String mediationTrace = resourceMap.get(Mediator.MEDIATION_TRACE_KEY);
        							contents.put("mediationTrace",  (null != mediationTrace)?mediationTrace:"No trace recorded");
        							text.append("<font size='+1' face='Courier New'>");
    								boolean firstRow = true;
    								String fromNS = "", toNS = "";
    								JSONArray rows = new JSONArray();
    								contents.put("rows", rows);
    								int num = 1;
    								for (String key : resourceMap.keySet()) {
    									if (Mediator.MEDIATION_TRACE_KEY.equals(key))
    										continue;
    									String from = key;
    									String to = resourceMap.get(from);
    									String fromName = "";
    									if (false == Strings.isNullOrEmpty(from)) {
    										Statement s = fromModel.getProperty(fromModel.getResource(from), RDFS.label);
    										if (null != s)
    											fromName = '"' + s.getObject().toString() + '"';
    									}
    									if (firstRow) {
    										fromNS = from.substring(0, from.lastIndexOf('/') + 1);
    										toNS =   to.substring(0, to.lastIndexOf('/') + 1);
    										firstRow = false;
    									}
    									JSONObject arow = new JSONObject();
    									rows.put(arow);
    									arow.put("num", num); 
    									arow.put("fullFResource", from);
    									arow.put("FResourceName", fromName);
    									arow.put("Fresource", from.substring(fromNS.length()));
    									arow.put("fullTResource", to);
    									arow.put("Tresource", to.substring(toNS.length()));
    									arow.put("evenRow", num%2);
    									num++;
    									text.append("<a href='" + key + "'>" + key + "</a> &nbsp;&nbsp;&nbsp; <a href='" + resourceMap.get(key) + "'>" + resourceMap.get(key) + "</a><br>\n");
    								}
    								contents.put("numRows", num);
    								contents.put("fromNS", fromNS);
    								contents.put("toNS", toNS);
    								
    								text.append("</font>");
    								Utils.respondGetWithTemplateAndJson(contents, "showMediator", response);
    								//Utils.respondWithText(response, text.toString());
    								return;
    							}
    						}
    					}
    				}
    			}
    			setMessage(msg); //    			Utils.respondWithText(response, text.toString());
    		} else if (verb.equalsIgnoreCase("T"))	{
    			if (row instanceof Mediator) {
        			String msg = testMediator(response, id, database);
        			if (null == msg || "".equals(msg.trim())) return;
        			setMessage(msg);
    			} else if (row instanceof Port) {
    				String msg = testRepository(response, (Port)row);
        			if (null == msg || "".equals(msg.trim())) return;
        			setMessage(msg);
    			} else if (row instanceof Friend)
    				setMessage(testFriend(id, database));
    			else {
    				Utils.respondWithText(response, "Illegal action=[" + verb + "] for item [" + id + "]");
    				return;
    			}
    		} else if (verb.equalsIgnoreCase("P") || verb.equals("G")) {
    			if (false == row instanceof Port || ! ((Port)row).isTool()) {
    				setMessage("Error [" + row.getId() + "]: Cannot post or get a non port or non tool such as [" + id + "].");
    				return;
    			}
    			String toPortId = Port.IFields.ASSOCIATED_REPOSITORY_ID;
    			Port toPort = database.getPort(row.getField(toPortId));
    			if (null == toPort || ! ((verb.equals("P") && toPort.canPost()) || (verb.equals("G") && toPort.canGet()))) {
    				setMessage("Error [" + row.getId() + "]: Cannot post or get with an improperly configured target associated repository [" + toPortId + "].");
    				return;
    			}
   				RepositoryManagement.create(database).postOrGetToolToRepository( new ResponseWrapper(response), (Port) row, toPort, verb);
    			return;
    		}

    	}
    	if (null != update) {
            Map<String, String> params = new HashMap<String, String>(); // to be used for extra params that 
            // are not an update field.
           	params.put("initializeForOSLC", initializeForOSLC);
           	params.put("initialize", initialize);
           	params.put("prefix", service.getParam(request, "prefix"));

            

    		if (update.equals("Update"))
    			setMessage(update(database, request, service, params)); 
    		else if (update.equals("Cancel")) {
    			if ("true".equals(initialize)) {// need to delete that row. even if not archived yet.
    				row.setArchived(true); // so the next action will not fail.
    				setMessage(database.delete(id));
    			} // else, we flow to the default flow which will leave that item and do nothing about it.
    		}
    	}

    	showMainDialog(database, response, false); //Utils.isDm(request));
    }

	/**
     * Generates a form to invoke test of a config.
     *
     * @param response
     * @param id
     *            a config id
     * @param database
     *            Database of configuration
     * @return String to indicate error, or an html page.
	 * @throws JSONException 
     */
    public String testMediator(HttpServletResponse response, String id, Database database) throws JSONException
    {
        Mediator row = (Mediator) database.getItem(id);
        if (null == row) { return "Error: Mediator [" + id + "] not found for testing."; }
        StringBuffer htmlPage = new StringBuffer();
        htmlPage.append(Utils.getHtmlTemplate("templates/testMediator/main.html"));
        if (row.checkService(database).equals(Mediator.SERVICE.IMPORT))
            htmlPage.append(Utils.getHtmlTemplate("templates/testMediator/import.html"));
        if (row.checkService(database).equals(Mediator.SERVICE.EXPORT))
            htmlPage.append(Utils.getHtmlTemplate("templates/testMediator/export.html"));
        htmlPage.append(Utils.getHtmlTemplate("templates/testMediator/post.html"));
        JSONObject content = new JSONObject();
        content.put("mdt", id + " - " + row.getName());
        content.put("type", "Mediator");
        String contentType = getDatabase().getVar(Database.Vars.contentType.toString());
		content.put("rdf+xml", contentType.equals("application/rdf+xml")?"selected":"");
		content.put("rdf+xmlAbr", contentType.equals("application/rdf+xml-ABBREV")?"selected":"");
		content.put("n3", contentType.equals("application/n3")?"selected":"");
		content.put("turtle", contentType.equals("text/turtle")?"selected":"");
		content.put("n-triples", contentType.equals("application/n-triples")?"selected":"");
        content.put("importAccess", database.getPort(row.getOutputPortId()).getAccessName());
        content.put("exportAccess", database.getPort(row.getInputPortId()).getAccessName());
        Utils.respondWithText(response, Utils.mergeJsonWithHtml(htmlPage.toString(), content));
        return null;
    }

	/**
     * Generates a form to invoke test of a config.
     *
     * @param response
     * @param id
     *            a config id
     * @param database
     *            Database of configuration
     * @return String to indicate error, or an html page.
	 * @throws JSONException 
     */
    private String testRepository(HttpServletResponse response, Port row) throws JSONException
    {
        StringBuffer htmlPage = new StringBuffer();
        htmlPage.append(Utils.getHtmlTemplate("templates/testMediator/main.html"));
        if (row.canGet())
            htmlPage.append(Utils.getHtmlTemplate("templates/testMediator/import.html"));
        if (row.canPost())
            htmlPage.append(Utils.getHtmlTemplate("templates/testMediator/export2repository.html"));
        htmlPage.append(Utils.getHtmlTemplate("templates/testMediator/post.html"));
        JSONObject content = new JSONObject();
        String contentType = getDatabase().getVar(Database.Vars.contentType.toString());
		content.put("rdf+xml", contentType.equals("application/rdf+xml")?"selected":"");
		content.put("rdf+xmlAbr", contentType.equals("application/rdf+xml-ABBREV")?"selected":"");
		content.put("n3", contentType.equals("application/n3")?"selected":"");
		content.put("turtle", contentType.equals("text/turtle")?"selected":"");
		content.put("n-triples", contentType.equals("application/n-triples")?"selected":"");
        content.put("mdt", row.getDisplayId());
        content.put("type", "Direct Repository");
        content.put("importAccess", row.getAccessName());
        content.put("exportAccess", row.getAccessName());
        Utils.respondWithText(response, Utils.mergeJsonWithHtml(htmlPage.toString(), content));
        return null;
    }

	/**
     * Tests of a friend server for connectivity and authentication
     *
     * @param id
     *            a friend id
     * @param database
     *            Database of configuration
     * @return String to indicate error, or null.
     */
    private String testFriend(String id, Database database)
    {
        final Friend row = (Friend) database.getItem(id);
        if (null == row) { return "Error: Friend [" + id + "] not found for testing."; }
       	row.testCommunicator();
       	String msg = row.getMessage();
       	if (Strings.isNullOrEmpty(msg))
       		msg = "successful";
  		return "Testing Friend[" + id + "]: " + msg;
     }

//    public static OAuthCommunicator getCommunicator(final Friend friend, Database database)
//    throws OAuthCommunicatorException
//    {
//        OAuthCommunicator comm = new OAuthCommunicator(new IUserCredentials() {
//			public String getUserId() { return friend.getUser(); }
//			public String getPassword() { return friend.getPassword(); }
//		});
//        HttpPost test = new HttpPost("https://" + friend.getIpAddress() + "/dm/server/management/login");
//        HttpResponse resp = comm.execute(test);
//        int code = resp.getStatusLine().getStatusCode();
//        if (HttpStatus.SC_OK == code) {
//        	friend.setStatus(true);
//        	return comm;
//        }
//        else {
//        	friend.setStatus(false);
//        	String msg = resp.getStatusLine().getReasonPhrase();
//        	throw new OAuthCommunicatorException(msg);
//        }
//    }

//    private static String resolveRemoteOntology(JSONObject port, Database database) {
//    	Friend friend = database.getFriend((String)port.get(Port.IFields.FRIEND_ID));
//    	String accessName = (String)port.get(Port.IFields.ACCESS_NAME);
//
//    	if (null != friend && friend.isReady()) {
//    		try {
//        		com.ibm.haifa.smc.client.oauth.OAuthCommunicator comm = friend.getCommunicator();
//        		HttpGet query = new HttpGet("https://" + friend.getIpAddress() + "/dm/server/management/query?post=" + accessName);
//        		query.setHeader(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
//        		HttpResponse resp = comm.execute(query);
//        		int code = resp.getStatusLine().getStatusCode();
//        		if (HttpStatus.SC_OK == code) {
//        			JSONObject data = new JSONObject(resp.getEntity().getContent());
//        			if (data.containsKey("namespace")) {
//        				String namespace = data.get("namespace").toString();
//        				String version = data.get("version").toString();
//        				List<Ontology> ontologies = database.getOntologies();
//        				for (Ontology ont : ontologies) {
//        					if (namespace.equals(ont.getModelInstanceNamespace()))
//        						if (version.equals("") || version.equals(ont.getVersion())) {
//        							String ontologyId = ont.getId();
//        							if (null == ontologyId) ontologyId = "";
//        							return ontologyId;
//        						}
//        				}
//        			}
//        		}
//    		} catch (Exception e) {
//    		}
//    	}
//		return "";
//    }

    private String importModel(Database database, String id)
    {
        AModelRow row = (AModelRow) database.getItem(id);
        try
        {
        	String answer = row.importModel();
            return answer;
        }
        catch (Exception e)
        {
            setMessage("Error in import for item [" + id + "] - [" + e.getClass().getName() + "]: [" + e.getMessage() + "].");
        }
        return "";
    }

    /**
     * Usful method to redisplay the present status of the database as the main management dialog
     * as a response to any http requst or post to this server /sm API.
     *
     * @param database
     *            Loaded database that may need to be saved (if indeed "dirty").
     * @param response
     *            HTTP response to be filled up with contents.
     * @throws Exception
     */
    private void showMainDialog(Database database, HttpServletResponse response, boolean isDm) throws Exception
    {
        try
        {
            String htmlPage;
            if (null == database)
            {
                htmlPage = "<html><body>INTERNAL ERROR in Semantic Mediation management dialog manager</body><html>";
            }
            else
            {
//                database.save();
                htmlPage = populate(Utils.getHtmlTemplate(null), database, messages, isDm);
                Utils.respondWithText(response, htmlPage);
//                response.setEntity(new StringEntity(htmlPage));
//                response.setHeader(HttpHeaders.CONTENT_TYPE, ContentTypes.HTML);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void loadModelFileFromFileDialog(String id, HttpServletResponse response, Database database) throws JSONException
    {
        ADatabaseRow row = database.getItem(id);
        String type = row.getCollectionName().toLowerCase();
        JSONObject contents = new JSONObject();
        contents.put("id", id);
        contents.put("type", type);
        String contentType = database.getVar("contentType", "text/turtle");
//        if (Strings.isNullOrEmpty(contentType))
//        	contentType = "text/turtle";
        contents.put("contentType", contentType);
        for (String ct: IConstants.CONTENT_TYPES) {
        	if (ct.equalsIgnoreCase(contentType)) {
        		contents.put(contentType.toLowerCase() + "Selected", "selected");
        		break;
        	}
        }
        Utils.respondGetWithTemplateAndJson(contents, "/templates/loadModel.html", response);
    }

    /**
     * New revision:<br>
     * answers with a JSON object reflecting the content of a model according to a
     * code in the <code>what</code> parameter. Content of this object can be
     * processed in DOJO, or they can be merged with an html template which can
     * be returned as a result of a GET. See calls to this method.<br>
     * Can work for both multiple resources in a model, or with a single resource.
     * @param domain String name of the domain being explored.
     * @param what String code. S - explore it as an RDF XML, L - explore it as a table
     * of resources with properties.
     * @param repository Content manager for the content of the model.
     * @param title String header for the information.
     * @param id String identifier of a repository or ontology element
     * @param contentType String for the content type of the model to be used for listing it.
     * @param fromRow indicate to show only from that row, for a step-wise display of model content. Numerical value in this String.
     * @param subAction can be showNext or showPrev to control which portion of the resources to show now compared with the fromRow value.
     * @param version - String which may be null and if not it is a version of the model to be displayed, rather than the 
     * current version.
     * @param changeSet - String which may be null or '' to indicate no Change-set, or any of the following:<ol>
     * <li>'changeSet' to indicate that the ChangeSet of the current revision is to be shown
     * <li>anything else - to indicate there is no change set to show.
     * </ol>
     * @throws Exception
     */
    @SuppressWarnings({ })
	public static JSONObject processModelForJson(String domain, String what,
    		ARdfRepository repository, String title, String id,
    		String columns, String contentType, String fromRow, String subAction,
    		String version, String changeSet)
    throws Exception {
    	if (Strings.isNullOrEmpty(contentType))
    		contentType = "text/turtle";
        JSONObject contents = new JSONObject();
        if (null == id)
        	id= "";
        boolean isRepositrory = repository instanceof Repository;
        if (false == Strings.isNullOrEmpty(version))
        	title += "&nbsp;&nbsp;[ version of " + Repository.dateOfVersion(version).toString() + "]";
        contents.put("id", id);
        contents.put("version", Strings.isNullOrEmpty(version)?"":"version="+ version);
        contents.put("header", title);
        contents.put("hasRefs", isRepositrory);
        contents.put("canDownload", true);
        contents.put("domain", domain);
        Model model = repository.getModel(version, changeSet);
        if (what.equalsIgnoreCase("ShowRDF")) {
            if (false == repository.getMyItem().isAnOntology())
            	contents.put("hiddenIfRepository", "hidden");
            contents.put("rdf", Utils.modelToText(model, contentType));
            return contents;
        }

        // This is now to show in a list format, long or short.
        JSONArray history = new JSONArray();
        contents.put("history", history);
        contents.put("currentDate", repository.getDateString());
    // Firstly, put the history:
        List<String> versions = null;
        if (what.equalsIgnoreCase("ShowList")) {
        	versions = repository.getHistory();
        }
        if (Strings.isNullOrEmpty(version)) { // "current" is not selectable.
        	contents.put("selectableCurrent", "hidden");
        	contents.put("fixedCurrent", "");
        } else {
        	contents.put("selectableCurrent", "");
        	contents.put("fixedCurrent", "hidden");
        }
    	if (null != versions)
    		for (String ver : versions) {
    			JSONObject el = new JSONObject();
    			history.put(el);
    			el.put("ver", ver);
    			el.put("versionDate", Repository.dateOfVersion(ver).toString());
    			if (ver.equals(version)) {
    	        	el.put("selectableItem", "hidden");
    	        	el.put("fixedItem", "");
    	        } else {
    	        	el.put("selectableItem", "");
    	        	el.put("fixedItem", "hidden");
    	        }
    		}

        Map<String, String> map = Utils.fixNsPrefixes(model.getNsPrefixMap());
        JSONArray prefixes = new JSONArray();
        contents.put("prefixes", prefixes);
        for (String prefix : map.keySet())
        {
        	JSONObject pref = new JSONObject();
        	prefixes.put(pref);
        	pref.put("prefix", prefix);
        	pref.put("ns", map.get(prefix));
        }
        contents.put("numNS", String.valueOf(map.size()));

        ResIterator iter = model.listSubjects();
        Collection<String> filter = new HashSet<String>();
        filter.add(Repository.getConfigResource());
        Collection<String> resources = Utils.sortResources(iter, filter);
        int num = 0;
        JSONArray rows = new JSONArray();
        contents.put("rows", rows);
        contents.put("query", "/dm/sm/repository/" + domain + "?query=");
    	int numRows = resources.size();
        contents.put("numRows", numRows);
        int row1 = 0;
        if (null != fromRow) {
        	row1 = Integer.parseInt(fromRow);
        	if ("showPrev".equals(subAction))
        		row1 = Math.max(0, row1-400);
        	else if ("<<".equals(subAction))
         		row1 = 0;
        	if (">>".equals(subAction))
        		row1 = Math.max(0, numRows-200);
        }
    	int row2 = Math.min(row1 + 200, numRows);
        contents.put("fromRow", row1 + 1);
        contents.put("toRow", row2);
        contents.put("showPrevHidden", (row1 > 0)?"":"hidden");
        contents.put("showNextHidden", (row2 < numRows)?"":"hidden");

		JSONArray columnTitles = new JSONArray();
		contents.put("titles", columnTitles);

		ADatabaseRow owner = repository.myItem;
    	boolean isRepository = owner instanceof Port && ((Port)owner).isRepository(); //repository instanceof Repository;
    	JSONArray columnSpecs = null;
//		if (isRepository)
    		columnSpecs = processViewConfigs(columns, (AModelRow)owner);
//    	else
//    		columnSpecs = Ontology.getOntologyViewConfig();
		JSONArray titles = new JSONArray();
		contents.put("titles", titles);
		JSONArray[] viewProperties = new JSONArray[columnSpecs.length()];
		String[] viewTags = new String[columnSpecs.length()];
		int cnt = 0;
		String[] predicates = new String[columnSpecs.length()];
		for (cnt = 0; cnt < columnSpecs.length(); cnt++) {
			JSONObject o = (JSONObject) columnSpecs.get(cnt);
			if (false == Boolean.valueOf(Utils.safeGet(o, "forView").toString()))
				continue;
			String t = Utils.safeGet(o, "title").toString();
			JSONArray p = (JSONArray)Utils.safeGet(o, "content");
			if (Strings.isNullOrEmpty(t))
				t = "no-title";
			JSONObject th = new JSONObject();
			th.put("title", t);
			th.put("predicate", p.toString());
			predicates[cnt] = p.getString(0);
			titles.put(th);

			JSONArray c = (JSONArray) Utils.safeGet(o, "content");
			viewProperties[cnt] = c;

			String tag = "%";
			if (owner.isTemporary()) {
				tag = (String) Utils.safeGet(o, "tag");
				if (Strings.isNullOrEmpty(tag))
					tag = "%";
				if (tag.indexOf("%") < 0)
					tag = "%";
			}
			viewTags[cnt] = "@" + tag;// that will prevent the tag to be html escaped.
		}


		for (String uri : resources)
        {
        	num++;
        	if (num <= row1)
        		continue;
        	if (num > row2) 
        		break;
        	
        	JSONObject row = new JSONObject();
        	rows.put(row);
        	Resource resource = model.getResource(uri);
        	Property tagProp = model.getProperty(IConstants.SM_PROPERTY_SM_TAG_FULL);
        	Resource tagIsDeleted = model.getResource(IConstants.SM_PROPERTY_RESOURCE_DELETED_FULL);
        	boolean isDeleted = Utils.resourceHasPropertyValue(resource, tagProp, tagIsDeleted);
        	row.put("shownIfDeleted", isDeleted?"":"hidden");
        	row.put("hiddenIfDeleted", isDeleted?"hidden":"");
        	row.put("disabledIfDeleted", isDeleted?"disabled":"");
        	row.put("num", num);
        	row.put("evenRow", num%2);
        	String prefixForResource = Utils.prefixForResource(uri, map);
        	if (isRepository && prefixForResource.startsWith("base:")) {
        		if (null == Utils.safeGet(contents, "base")) {
        			String base = prefixForResource.split(":")[0];
        			contents.put("base", base);
        			String baseUri = /*new URI(*/map.get(base).replace("#", "%23"); // ).getPath().replace("#", "%23");
        			contents.put("baseUri", baseUri);
        		}
        	}
        	row.put("resource", prefixForResource);
        	row.put("fullResource", uri);

        	JSONArray columnContents = new JSONArray();
        	row.put("contents", columnContents);

//        	if (isRepository){
        	String resourceType = Utils.object4PropertyAsString(model, resource, RDF.type);
        	String prefixedType = Utils.prefixForResource(resourceType, map);
        	row.put("type", prefixedType);
        	for (int col = 0; col < viewProperties.length; col++) {
        		if (null == viewProperties[col])
        			continue;
        		JSONObject c = new JSONObject();
        		columnContents.put(c);
        		String colContent = Utils.object4PropertyAsString(model, resource, viewProperties[col]);
        		String tag = viewTags[col];
        		colContent = Utils.prefixForResource(colContent, map);
    			boolean isPropertyDeleted = null == model.getProperty(resource, model.createProperty(predicates[col]));    			
        		if (! isPropertyDeleted) // colMissing  && false == "-".equals(colContent))
        			colContent = Utils.replaceAll(tag, "%", colContent);
        		else 
        			colContent = "";
        		c.put("content", colContent);
    			JSONObject o = (JSONObject) columnSpecs.get(col);
    			boolean canEdit = Boolean.valueOf(Utils.safeGet(o, "forText").toString());
        		c.put("hideEditContents", canEdit?"":"hidden");
        		c.put("predicate", predicates[col]);
            	c.put("hiddenIfMissing", isPropertyDeleted?"hidden":"");
            	c.put("shownIfMissing", isPropertyDeleted?"":"hidden");
        	}
        }
		return contents;
    }


    /**
     * Answers with a modified columnSpecs to support also view configuration if existing in an associated ontology where
     * users can configure how columns are shown in a list format.
     * @param columnSpecs Array of JSONObject-s specifying columns, titles and the predicates making them up.
     * The structure of a column specification is: {"title":"&lt;column title>", "contents":["&lt;predicate1>",..."&lt;predicateN>"], "tag":"&lt;tag...%...>"}<br>
     * The % in the tag is replaced with the content of a field taken from any of the predicates in the "contents" item. The default for "<tag...>" is %.
     * @param owner ADatrabaseRow
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws FileNotFoundException
     */
    private static JSONArray processViewConfigs(String columns,
    		AModelRow owner) throws FileNotFoundException, IOException, ClassNotFoundException {
    	JSONArray viewConfig = null;
    	if (false == Strings.isNullOrEmpty(columns)) {
    		try {
    			viewConfig = new JSONArray(columns);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	if (null == viewConfig) {
    		viewConfig = owner.getShowViewConfig();
    	}
    	if (null == viewConfig)
    		viewConfig = new JSONArray();
    	return viewConfig;
    }


    /**
     * New version which processes a model being ontology, rules or port for showing its content. Unified service based
     * on the common Repository object.
     * @param response
     *            HttpResponse to answer with the generated page.
     * @param repository
     *            RDF content management object for this model.
     * @param what
     *            command verb from the request.
     * @param title
     *            String to use as title on the HTML page.
     * @param id
     *            String id of the configuration row of this model. May be null to mean this is a repository domain and \
     *            not a file of an ontology or ruleset in the configuration.
     * @param contentType String for the content type of the model to be used for listing it.
     * @param fromRow indicate to show only from that row, for a step-wise display of model content. Numerical value in this String.
     * @param subAction can be showNext or showPrev to control which portion of the resources to show now compared with the fromRow value.
     * @param version - String which may be null and if not it is a version of the model to be displayed, rather than the 
     * current version.
     * @param changeSet - String which may be null or '' to indicate no Change-set, or any of the following:<ol>
     * <li>'changeSet' to indicate that the ChangeSet of the current revision is to be shown
     * <li>anything else - to indicate there is no change set to show.
     * </ol>
     * @throws Exception
     */
    public void processModel(HttpServletResponse response, ARdfRepository repository,
    		String what, String title, String id, String columnSpecs, String contentType, String fromRow, String subAction,
    		String version, String changeSet)
    throws Exception
    {
//        Model model = repository.getModel();

    	if (Strings.isNullOrEmpty(contentType))
    		contentType = "text/turtle";
        if (what.equalsIgnoreCase("ShowList") || what.equalsIgnoreCase("ShowShort") || what.equalsIgnoreCase("ShowRDF")) {
        	JSONObject contents = processModelForJson(repository.getDomain(), what, repository, title, id, columnSpecs, contentType, fromRow, subAction,
        			version, changeSet);
        	Database database = getDatabase();
        	ADatabaseRow row = database.getItem(id);
        	contents.put(Database.Vars.contentType.toString(), contentType);
        	contents.put("isTemp", Boolean.toString(row.isTemporary()));
        	contents.put("hiddenIfNotTemp", row.isTemporary()?"":"hidden");
        	contents.put("collectionName", row.getCollectionName());
        	contents.put("portType", (row instanceof Port)? ((Port)row).getType():"");
        	contents.put("enableSave", repository.isDirty());
        	contents.put("host", database.getHost(true, true)); //Utils.getHost(null, true));
            contents.put("comments", Strings.isNullOrEmpty(row.getComments())?"NaN":row.getComments());
        	if (row.isTemporary() && row instanceof Port && ((Port)row).isRepository()) {
        		Port port = (Port)row;
//    			String ontId = port.getOntologyId();
    			Ontology ont = port.getOntology();
    			if (null != ont) {
    				JSONObject jsonOnt = jsonFromOntology(ont); //ont.getModelRepository().getModel(), ont.getNameSpaceUri());
        			contents.put("classes", Utils.safeGet(jsonOnt, "classes").toString());
    			}

        	}
        	String template = "templates/rdfList.html";
        	if (what.equalsIgnoreCase("ShowShort")) {
        		template = "templates/rdfShortList.html";
        	} else if (what.equalsIgnoreCase("ShowRDF"))
        		template = "templates/rdf.html";
        	Utils.respondGetWithTemplateAndJson(contents, template, response);
        	return;
        }
    }
    /**
     * Generates an html page from a template and populating it with data from the
     * database, and messages from the list parameter.
     *
     * @param htmlPage
     *            String template of an html page.
     * @param database
     *            a Database with contents to be populated in the page.
     * @param messages
     *            List of messages. May be empty in which case the text "None" is used.
     * @return String of an html page ready to be displayed on a browser.
     * @throws Exception
     */
    private String populate(String htmlPage, Database database, List<String> messages, boolean isDm) throws Exception
    {
    	String projectName = Database.getProjectName(); //System.getProperty("com.ibm.dm.frontServices.sm.name", "EU_Project");
    	String folderPath = new File(Database.getRootName()).getAbsolutePath();
    	String ontologyVar = database.getVar("Ont", "0"); //if (false == "0".equals(ontologyVar)) ontologyVar = "1";
    	String portVar = database.getVar("Prt", "0");     //if (false == "0".equals(portVar)) portVar = "1";
    	String ruleSetVar = database.getVar("Rul", "0");  //if (false == "0".equals(ruleSetVar)) ruleSetVar = "1";
    	String mediatorVar = database.getVar("Mdt", "0"); //if (false == "0".equals(mediatorVar)) mediatorVar = "1";
    	String friendsVar = database.getVar("Frn", "0");  //if (false == "0".equals(friendsVar)) frNamendsr = "1";
    	String ontImgVar = database.getVar("ontImgVar", "Image URL here");  //if ("".equals(ontImgVar)) ontImgVar = "Image URL here";
    	String operImgVar = database.getVar("ontImgVar", "Image URL here"); //if ("".equals(operImgVar)) operImgVar = "Image URL here";
    	String tagsData = U.getGson().toJson(database.getTags());
    	String filterVar = database.getFilter(); //Var("filterVar");
//    	if ("".equals(tags)) tags = "No tags";
//        System.out.println("tags = " + tags);

    	JSONArray friends = new JSONArray();
    	for (Friend friend: database.getFriends()) {
    		if (friend.isReady()) {
    			JSONObject f = new JSONObject();
    			f.put("id", friend.getId());
    			f.put("name", friend.getDisplayId());
    			friends.put(f);
    		}
    	}
    	JSONObject fContents = new JSONObject();
    	fContents.put("friends4OntImport", friends);
    	fContents.put("friends4RulImport", friends);

    	htmlPage = Utils.mergeJsonWithHtml(htmlPage, fContents);
    	htmlPage = Utils.replaceAll(htmlPage, "_serverVersion_", ISmInfo.VERSION);
    	htmlPage = Utils.replaceAll(htmlPage, "_notes_", ISmInfo.NOTES);
    	htmlPage = Utils.replaceAll(htmlPage, "_ontologyVar_", ontologyVar);
    	htmlPage = Utils.replaceAll(htmlPage, "_portVar_", portVar);
    	htmlPage = Utils.replaceAll(htmlPage, "_ruleSetVar_", ruleSetVar);
    	htmlPage = Utils.replaceAll(htmlPage, "_mediatorVar_", mediatorVar);
    	htmlPage = Utils.replaceAll(htmlPage, "_friendsVar_", friendsVar);
    	htmlPage = Utils.replaceAll(htmlPage, "_ontImgVar_", ontImgVar);
    	htmlPage = Utils.replaceAll(htmlPage, "_operImgVar_", operImgVar);
        htmlPage = Utils.replaceAll(htmlPage, "_metaProject_", projectName);
        htmlPage = Utils.replaceAll(htmlPage, "_projectRootPath_", folderPath);
        htmlPage = Utils.replaceAll(htmlPage, "_databaseType_", Database.databaseType(database));
        htmlPage = Utils.replaceAll(htmlPage, "_project_", isDm?"dm":"");
        //htmlPage = Utils.replaceAll(htmlPage, "_allTags_", allTags);
        htmlPage = Utils.replaceAll(htmlPage, "_tagsData_", tagsData);
        htmlPage = Utils.replaceAll(htmlPage, "_filterVar_", filterVar);
        htmlPage = Utils.replaceAll(htmlPage, "_hideInDm_", isDm?"hidden":"");
        htmlPage = Utils.replaceAll(htmlPage, "_importDisabled_", database.getFriends().size() == 0?"hidden":"");

        Set<Port.PortType> portsNoTools = Sets.newHashSet(
        	Port.PortType.repository,
        	Port.PortType.post,
        	Port.PortType.get,
        	Port.PortType.pipe,
        	Port.PortType.catalog,
        	Port.PortType.export
        );
        Set<Port.PortType> tools = Sets.newHashSet(Port.PortType.tool);

        htmlPage = Utils.mergeJsonWithHtml(htmlPage, mergeTags2Json(database.getTags(), filterVar));
        htmlPage = embedData(database, database.getOntologies(), htmlPage, "_ontologies_", "_/ontologies_", database.isShowActive(), database.getOntologies());
        htmlPage = embedData(database, database.getRules(), htmlPage, "_rules_", "_/rules_", database.isShowActive(), database.getOntologies());
        htmlPage = embedData(database, database.getPorts(portsNoTools), htmlPage, "_ports_", "_/ports_", database.isShowActive(), null);
        htmlPage = embedData(database, database.getPorts(tools), htmlPage, "_tools_", "_/tools_", database.isShowActive(), null);
        htmlPage = embedData(database, database.getMediators(), htmlPage, "_mediators_", "_/mediators_", database.isShowActive(), null);
        htmlPage = embedData(database, database.getFriends(), htmlPage, "_friends_", "_/friends_", database.isShowActive(), null);
//        htmlPage = embedDomains(database, Repository.getDomains(SmContainer.getFolder()), htmlPage, "_domains_", "_/domains_");
        htmlPage = Utils.replaceAll(htmlPage, "_showActive_", database.getVar("showActive"));
        if (messages.size() > 0)
        {
            StringBuffer sb = new StringBuffer();
            for (String msg : messages)
            {
                sb.append(msg).append("<br>");
            }
            htmlPage = Utils.replaceAll(htmlPage, "_messages_", sb.toString());
            messages.clear();
        }
        else
            htmlPage = Utils.replaceAll(htmlPage, "_messages_", "None");
        return htmlPage;
    }

    /**
	 * Merges a map of tags to the magnitude (number associated with them).
	 * @param tags Map from tag String to Integer magnitude of the tag.
	 * @return
     * @throws JSONException 
	 */
    private  static JSONObject mergeTags2Json(Map<String, Set<String>> tags, String filter) throws JSONException {
    	List<String> filterItems = Arrays.asList(filter.split(","));
    	JSONObject results = new JSONObject();
    	JSONArray tagSpecs = new JSONArray();
    	results.put("tags", tagSpecs);
		if (tags.size() < 1)
			return results;
		Set<String> keys = tags.keySet();
		int mx = 0, mn = Integer.MAX_VALUE;
		for (String key : keys) {
			int v = tags.get(key).size();
			mx = Math.max(mx, v);
			mn = Math.min(mn, v);
		}

		double factor = 5.0/(mx-mn+1);
		String k[] = keys.toArray(new String[0]);
		Arrays.sort(k);
		for (String key : k) {
			int v = 1 + (int)Math.round((tags.get(key).size()-mn)*factor); //Math.round(Math.sqrt(tags.get(key)*factor)); // + r.nextInt(4);
			JSONObject t = new JSONObject();
			tagSpecs.put(t);
			t.put("tag", key);
			t.put("tagSize", Integer.toString(v));
			t.put("checked", filterItems.contains(key)?"checked":"");
		}
		return results;
	}


    private void setMessage(String msg)
    {
        if (null == msg) return;
        messages.add(msg);
    }

    /**
     * Serves an edit session on a certain row and updates the fields of that row. Than responding with the
     * updated database rendering.
     *
     * @param database
     *            Database to be updated.
     * @param request
     *            HttpRequest of the edit session
     * @param service
     *            SmService to obtain needed services.
     * @return String that will include an error msg to display in case of update info being invalid.
     * @throws JSONException 
     */
    private String update(Database database, HttpServletRequest request, ISmService service, Map<String, String> params) throws JSONException
    {
        String id = service.getParam(request, "id");
        if (null == id) return "Error: no id field in an upate request for sm manager.";
        ADatabaseRow row = database.getItem(id);
        if (null == row) return "Error: no row found for id [" + id + "].";

        String fields[] = row.getEditableFieldNames();
        JSONObject jUpdate = new JSONObject();
        for (String field : fields)
        {
        	String param = service.getParam(request, field);
//        	if (null == param) // do not process missing fields in the update message - dont do them at all. Ignore!
//        		continue; 
            String v = Utils.stringify(param);
            if (field.endsWith("Id") && false == ADatabaseRow.NaN.equals(v))
            {
                if (v.contains(":"))
                		v = v.split(":")[0];
            }
            if (ADatabaseRow.NaN.equals(v))
            	v="";
            jUpdate.put(field, v);
        }

        String msg = database.update(row, jUpdate, params);
//        if (null == msg)
//        	database.save();
        return msg;
    }

    // following say what is a proper port type to serve as input or output of a mediation.
    static final Collection<PortType> inputPortFilter = new HashSet<PortType>(Arrays.asList(new PortType[] {PortType.post, PortType.repository, PortType.pipe}));
    static final Collection<PortType> outputPortFilter = new HashSet<PortType>(Arrays.asList(new PortType[] {PortType.get, PortType.export, PortType.repository, PortType.pipe }));
    /**
     * Prepare an html form to edit fields of a row having a certain id.
     *
     * @param id
     *            String identifier id of the row to be edited.
     * @param database
     *            Database of the rows.
     * @return String with html form to be sent to browser, or null if row not found.
     * @throws JSONException 
     */
    public JSONObject edit(String id, Database database, String portType, boolean autoCreate) throws JSONException
    {
    	JSONObject result = new JSONObject();
        ADatabaseRow row = database.getItem(id);
        if (null == row) {
        	result.put("error", "Error: Row [" + id + "] not found for editing.");
        	return result;
        }
//        StringBuffer htmlPage = new StringBuffer();
        JSONObject contents = new JSONObject();
//        htmlPage.append("<html><body><h1>Edit " + row.getCollectionName() + " for id [" + id + "]\n" + "<form><table border='1'><tr><th>Field<th>Value</tr>\n");
        contents.put("id", id);
        if (Strings.isNullOrEmpty(portType))
        	portType = "repository";
        contents.put("portType", portType);
        String fields[] = row.getEditableFieldNames();
        contents.put("archived", (row.isArchived() ? "checked" : ""));
        contents.put("hideOSLCchoice", autoCreate?"":"hidden");
        contents.put("notHideOSLCchoice", autoCreate?"hidden":""); // inverse of the one above.
        contents.put("initialize", autoCreate);
        
        contents.put("OSLCDefaultChoice", autoCreate); // if it is autocreate the initialize field should be false to not hide the
        // initializeForOSLC checkbox in the GUI.
        contents.put("tags", row.getTags());
        contents.put("version", row.getVersion());
        contents.put("versionSuffix", row.getVersionSuffix());
        if (row.isAnOntology())
        	contents.put("defaultNamespace",row.getDefaultNamespace());
        
        contents.put("name", row.getName());
       	if (row.isAnOntology()) {
    		AModelRow amr = (AModelRow)row;
    		contents.put("prefix", amr.getPrefix());
    		String origin = amr.isImported()?("Imported from [" + amr.getImportUrl() + "]"):
    			"Original, owned and editable by this server";
    		amr.setComments(origin);
    	}
        contents.put("comments", Strings.isNullOrEmpty(row.getComments())?"NaN":row.getComments());
//        JSONArray optional = new JSONArray();
        if (row instanceof AModelRow) {
        	AModelRow modelRow = (AModelRow)row;
         	contents.put("modelInstanceNamespace", modelRow.getModelInstanceNamespace());
			Set<String> properties = modelRow.getTopProperties();
			JSONArray viewConfig = modelRow.getEditViewConfig();
			JSONArray viewElements = new JSONArray();
			contents.put("@savedViewConfig", modelRow.getSavedViewConfig());
			contents.put("@actualViewConfig", viewConfig.toString());
//   			contents.put("@defaultViewConfig", modelRow.getDefaultViewConfig().toString());
   		 	contents.put("viewPredicates", viewElements);
//			properties = ontology.getTopProperties();
			properties.addAll(getPredicates(viewConfig));

			Map<String, JSONObject> map = new HashMap<String, JSONObject>();
			//Iterator<JSONObject> i = viewConfig.iterator();
			for (int i= 0; i < viewConfig.length(); i++) { //while (i.hasNext()) {
				JSONObject e = (JSONObject) viewConfig.get(i);
				if (null == e) continue;
				JSONArray e_s = (JSONArray) Utils.safeGet(e, "content");
				if (e_s.length() < 1) continue;
				for (int k=0; k < e_s.length(); k++)
					map.put(e_s.get(k).toString(), e);
			}

			for (String property : properties) {
				JSONObject predicate = new JSONObject();
				viewElements.put(predicate);
				predicate.put("predicate", property);
				JSONObject configProps = null;
				if (null != map.get(property))
						configProps = map.get(property);
				predicate.put("hiddenRow", (null == configProps)?"hidden":"");
				String bv = "false";
				if (null != configProps)
					bv = Utils.safeGet(configProps, "forText").toString();
				predicate.put("tchecked", ("true".equals(bv))?"checked":"");
				bv = "false";
				if (null != configProps)
					bv = Utils.safeGet(configProps, "forView").toString();
				boolean lChecked = "true".equals(bv);
				predicate.put("lchecked", lChecked?"checked":"");
				String title= "";
				String tag = "%";
				if (null != configProps) {
						if (null != Utils.safeGet(configProps, "title"))
							title = Utils.safeGet(configProps, "title").toString();
						if (null != Utils.safeGet(configProps, "tag"))
							tag = Utils.safeGet(configProps, "tag").toString();
				}
				predicate.put("ptitle", title);
				predicate.put("ptag", tag);
				predicate.put("pdisabled", lChecked?"disabled":"");
			}
        }
        if (row instanceof Port) {
        	Port port = (Port)row;
        	contents.put("accessName", port.getAccessName());
        	contents.put("type", port.getType().toString());
        	contents.put("accessibility", port.getAccessibility().toString()); //(port.isDirect() ? "checked" : ""));
        	for (SERVICE s: SERVICE.values()) {
        		contents.put("is"+s.toString()+"Selected", (port.getAccessibility()==s)?"selected":"");
        	}
        	contents.put("showAccessibility", (port.isRepository() ? "" : "hidden"));
        	contents.put("disableAccesibility", (port.isTool() ? "disabled":""));
        	contents.put("hideIfNotRepository", (port.isRepository() ? "" : "hidden"));
    		contents.put("selectedOntologyId", row.getDispalyName(database, "ontologyId")); //row.getFieldOrNaN("ontologyId"));
    		contents.put("ontologyPrefix", port.getOntology().getPrefix()); //row.getFieldOrNaN("ontologyId"));
    		contents.put("ontologyNameSpace", port.getOntology().getModelInstanceNamespace()); //row.getFieldOrNaN("ontologyId"));
    		if (((Port) row).isTool()) {
    			contents.put("associatedRepositoryId",  row.getDispalyName(database, Port.IFields.ASSOCIATED_REPOSITORY_ID)); //row.getFieldOrNaN(Port.IFields.ASSOCIATED_REPOSITORY_ID));
    			Port associated = database.getPort(row.getField(Port.IFields.ASSOCIATED_REPOSITORY_ID));
    			String accessibility = "None";
    			if (null != associated)
    				accessibility = associated.getAccessibility().inverse().toString();
            	contents.put("accessibility", accessibility);
    		}
        }
        if (row instanceof Friend) {
        	Friend f = (Friend)row;
        	contents.put("user", f.getUserId());
        	contents.put("password", f.getPassword());
        	contents.put("authenticatedChecked", f.isAuthenticated()?"checked":"");
        	contents.put("authenticatedDdisabled", f.isAuthenticated()?"":"disabled");
        	contents.put("ipAddress", f.getIpAddress());
        }
        String filter = database.getFilter();

        if (row instanceof RuleSet) {
        	RuleSet rs = (RuleSet)row;
        	String ontId = "ANY";
        	Ontology ont = rs.getEndPointOntology(RuleSet.END_POINT.FIRST);
        	if (null != ont)
        		ontId = ont.getId();
        	processChoiceJson("ontologyId-1", row, contents,
        			(ADatabaseRow[])database.getOntologies().toArray(new ADatabaseRow[0]),
        			ontId, null, filter, "ANY");
        	ontId = "ANY";
        	ont = rs.getEndPointOntology(RuleSet.END_POINT.SECOND);
        	if (null != ont)
        		ontId = ont.getId();
        	processChoiceJson("ontologyId-2", row, contents,
        			(ADatabaseRow[])database.getOntologies().toArray(new ADatabaseRow[0]),
        			ontId, null, filter, "ANY");
        	if ("ANY".equals(ontId)) { // This is not processed well above since this is not NaN.
        		
        	}
        	
            contents.put("reversible", (rs.isEndPointReversible()? "checked" : ""));
            
            contents.put("hasApi", rs.hasApi()?"checked":"");
            contents.put("apiAccessName",  rs.getField(RuleSet.IFields.API_ACCESS_NAME));
        }
        
        Set<String> fieldset = new HashSet<String>();
        fieldset.addAll(Arrays.asList(fields));
        if (fieldset.contains("ontologyId")) {
//        	if (row instanceof Port) 
        		contents.put("selectedOntologyId", row.getFieldOrNaN("ontologyId"));
//        	else
//        		processChoiceJson("ontologyId", row, contents,
//        			(ADatabaseRow[])database.getOntologies().toArray(new ADatabaseRow[0]),
//        			row.getFieldOrNaN("ontologyId"), null, filter, null);
        }
        if (fieldset.contains("friendId")) {
        	processChoiceJson("friendId", row, contents,
        			(ADatabaseRow[])database.getFriends().toArray(new ADatabaseRow[0]),
        			row.getFieldOrNaN("friendId"), null, filter, null);
        }
        if (fieldset.contains("inputPortId")) {
        	processChoiceJson("inputPortId", row, contents,
        			(ADatabaseRow[])database.getPorts().toArray(new ADatabaseRow[0]),
        			row.getFieldOrNaN("inputPortId"), inputPortFilter, filter, null);
        }
        if (fieldset.contains("outputPortId")) {
        	processChoiceJson("outputPortId", row, contents,
        			(ADatabaseRow[])database.getPorts().toArray(new ADatabaseRow[0]),
        			row.getFieldOrNaN("outputPortId"), outputPortFilter, filter, null);
        }
        if (fieldset.contains("ruleSetId")) {
        	processChoiceJson("ruleSetId", row, contents,
        			(ADatabaseRow[])database.getRules().toArray(new ADatabaseRow[0]),
        			row.getFieldOrNaN("ruleSetId"), null, filter, null);
        }
        if (fieldset.contains("interceptorName")) {
        	JSONArray elements = new JSONArray();
        	contents.put("interceptorName", elements);
        	List<String> interceptors = getDatabase().getInterceptorNames();
			String value = row.getFieldOrNaN("interceptorName");
        	for (String interceptor : interceptors) {
				JSONObject entry = new JSONObject();
				entry.put("selected", value.equalsIgnoreCase(interceptor)?"selected":"");
				entry.put("idName", interceptor);
				entry.put("requiresLicense", Boolean.toString(getDatabase().isInterceptorRequiresLicense(interceptor)));
				entry.put("licenseText", Utils.escapeQuotesAndNL(/*Utils.forHtml(*/ getDatabase().getInterceptorLicenseText(interceptor))); //);
				entry.put("interceptorDescription", Utils.escapeQuotes(/*Utils.forHtml(*/ getDatabase().getInterceptorDescription(interceptor))); //);
				elements.put(entry);
			}
        	String capField = "InterceptorName";
        	contents.put("selectedNaN" + capField, value.equals(ADatabaseRow.NaN)?"selected":"");
        	contents.put("selected" + capField, value);
       }

        result.put("contents", contents);
        result.put("templateName", "templates/edit/" + row.getCollectionName().toLowerCase());

        if (row instanceof Port) {
        	if (PortType.export.equals(((Port)row).getType())) {
        		result.put("templateName", "templates/edit/port-export");
        		contents.put("selectedOntologyId", row.getDispalyName(database, "ontologyId"));
        	} else if (((Port) row).isTool()) 
        		result.put("templateName", "templates/edit/tool");
        }
        return result;
    }

    /**
     * Answers with the list of predicates in the vciew configuration. That is the set of all entries in the "content"
     * element of items in that JSON array.
     * @param viewConfig JSONArray of view configuration items.
     * @return Set of the predicate strings in this view configuration.
     * @throws JSONException 
     */
    private static Collection<? extends String> getPredicates(
    JSONArray viewConfig) throws JSONException {
    	Set<String> results = new HashSet<String>();
    	for (int i=0; i < viewConfig.length(); i++) {
    		JSONArray content = (JSONArray) ((JSONObject)viewConfig.get(i)).get("content");
    		for (int j= 0; j < content.length(); j++)
    			results.add(content.get(j).toString());
    	}
    	return results;
    }
    /**
     * Fills up a JSON object with choices for a selection of a certain type of items<br>
     * The choices are those of the provided elements, for a certain field in a certain row, which are
     * legal references for that field in that row.
     * @param field String name of the field of choices.
     * @param row item for which choices are made
     * @param contents JSON object to be filled up with the choices.
     * @param items Candidate items to be listed in that choices.
     * @param value The present checked value of the choices.
     * @param portType String for the port type if the row is a port. Otherwise, it is ignored.
     * @param filter String for the list of tags of the present filter. To be used instead of the tags of the row.
     * @param NaN - String representing the NaN value.
     * @throws JSONException 
     */
    private static void processChoiceJson(String field, ADatabaseRow row, JSONObject contents,
    		ADatabaseRow[] items, String value, Collection<PortType> portType, String filter, String NaN) throws JSONException {
    	JSONArray elements = new JSONArray();
    	contents.put(field, elements);
    	fillElements(items,	value, row, elements, portType, filter);
    	String capField = Character.toUpperCase(field.charAt(0)) + field.substring(1);
    	if (null == NaN)
    		NaN = ADatabaseRow.NaN;
    	contents.put("selectedNaN" + capField, value.equals(NaN)?"selected":"");
    	contents.put("selected" + capField, value);
	}
	/**
     * Utility which fills up the JSONArray with pair of values "selected" and "idName" from
     * the list of items, such which share tags with the row.
     * @param ontologies
     * @param row
     * @param elements
     * @param portType Collection of PortTypes allowed to be picked. If null - it is ignored.
     * @param filter String for the list of tags of the present filter. To be used instead of the tags of the row.
	 * @throws JSONException 
     */
    private static void fillElements(ADatabaseRow[] items, String selectedId,
			ADatabaseRow row, JSONArray elements, Collection<PortType>portType, String filter) throws JSONException {
    	Collection<String> tagSet = new HashSet<String>();
    	// instead of the tags of the row, use the tags of the database, if provided.
    	if (null == filter)
    		filter = row.getTags();
    	if (false == Strings.isNullOrEmpty(filter)) {
    		String[] tags = filter.split(",");
    		for (String tag : tags) {
    			tagSet.add(tag.trim().toLowerCase());
    		}
    	}
    	for (ADatabaseRow item : items) {
			if (item.isTemporary()  || item.isPermanent() || false == item.isReady())
				continue;
			if (null != portType && filterPorts(item, portType, false)) // The portType collection is what should NOT be filtered out, but included.
				continue;
			boolean ok = tagSet.size() < 1; // if no tags in this item - all rules apply w/out care on filtering.
//			if (item.getTags().indexOf('x') >= 0) {
//				System.out.println(item.getTags());
//			}
			if (false == ok && false == Strings.isNullOrEmpty(item.getTags())) {
				String[] itemTags = item.getTags().split(",");
				for (String tag : itemTags) {
					if (tagSet.contains(tag.trim().toLowerCase())) {
						ok = true;
						break;
					}
				}
			}
			if (!ok)
				continue;
			JSONObject entry = new JSONObject();
			entry.put("selected", (selectedId.equals(item.getId()) ? "selected" : ""));
			String name = "";
			if (item instanceof ANamedRow) {
				String n = ((ANamedRow)item).getName();
				if (null != n && false == "".equals(n))
					name = ":" + n;
			}
			entry.put("idName", item.getId() + name);
			elements.put(entry);
		}
	}

    /**
     * Answers true if aNamedRow should be filtered out and ignored.
     *
     * @param aNamedRow
     *            Port row to be examined.
     * @param subtype
     *            Collection of PortType-s that may be null to mean no filtering - e.g., answer false. But if Port row's port type is in this collection, it
     *            is filtered only if the next param is true.
     * @param filter
     *            boolean that if true, subtype-d row is filtered - ignored.
     * @return
     */
    private static boolean filterPorts(ADatabaseRow aNamedRow, Collection<Port.PortType> subtype, boolean filter)
    {
//    	if (aNamedRow.getName().toLowerCase().indexOf("bso") >= 0)
//    		System.out.println(aNamedRow);
        if (null == subtype) return false; 
        if (false == aNamedRow instanceof Port) return false;
        Port port = (Port) aNamedRow;
        PortType ptype = port.getType();
        if (PortType.export.equals(ptype)) ptype = PortType.get;
        if (subtype.contains(ptype)) return filter;

        return false;
    }

    /**
     * Answers with a new item of the class specified in the create parameter, the potentially needed
     * posrtType in case create is "port", and with the specified tags, and optionally using the associated ontology id 
     * for creating a repository for an ontology.
     * @param create String for the class of item to create
     * @param database
     * @param portType String for the port type in case create is "port".
     * @param tags String for list of tags to assign the new item.
     * @param associatedId String for the ontology Id to be associated with a new reporitory port type.
     * @return
     */
    private ADatabaseRow createRow(String create, Database database, String portType, String tags, String associatedId)
    {
    	ADatabaseRow item = null;
        if ("ontology".equalsIgnoreCase(create))
            item = database.createOntology();
        else
            if ("ruleSet".equalsIgnoreCase(create))
                item = database.createRuleSet();
            else if ("port".equalsIgnoreCase(create) || "tool".equalsIgnoreCase(create)) {
                item = database.createPort(portType);
//                boolean repositoryOrTool = portType.equals(Port.PortType.repository.toString()) || 
//                		portType.equals(Port.PortType.tool.toString());
                if (/*repositoryOrTool && */false == Strings.isNullOrEmpty(associatedId)) {
                	Port port = (Port)item;
                	if ("tool".equals(create)) {
                		Port associatedPort = database.getPort(associatedId);
                		if (null == associatedPort)
                			return null;
                		port.setField(Port.IFields.ASSOCIATED_REPOSITORY_ID, associatedId);
                		port.setOntologyId(associatedPort.getOntologyId());
                		port.setField(Port.IFields.ACCESSIBILITY, associatedPort.getAccessibility().inverse().toString());
                    	port.setOntologyId(associatedPort.getOntologyId());
                		port.setComments("Auto generated from repository [" + associatedPort.getDisplayId() + "].");
                    	port.setName(associatedPort.getName() + "-tool");
                	} else {
                		port.setOntologyId(associatedId);
                		Ontology ont = database.getOntology(associatedId);
                		port.setComments("Auto generated from ontology [" + ont.getDisplayId() + "].");
                    	port.setName(ont.getName() + " rep.");
                	}
                	port.setAccessName(port.getId().toLowerCase());
                	port.setVersion("1");
                }
            } else if ("mediator".equalsIgnoreCase(create)) {
            	item = database.createMediator();
            	if (false == Strings.isNullOrEmpty(associatedId)) {
            		Mediator mediator = (Mediator)item;
            		mediator.setRuleSetId(associatedId);
            		RuleSet rules = database.getRuleSet(associatedId);
            		mediator.setComments("Auto generated from rule-set [" + rules.getDisplayId() + "].");
            		mediator.setName(rules.getName() + " med.");
            		mediator.setVersion("1");
            	}
            } else if ("friend".equalsIgnoreCase(create))
            	item = database.createFriend();

        if (null != item && false == Strings.isNullOrEmpty(tags)) { //null != tags && false == "".equals(tags)) {
        	item.setField(ADatabaseRow.IFields.TAGS, tags);
        	database.setTags();
        }
        return item;
    }

    /**
     * Embeds the data form a certain row in the database into a pattern of HTML to show in the configuration management
     * html page.
     *
     * @param rows
     *            rows from the management configuration table to be filled up.
     * @param htmlPage
     *            Original HTML page pattern in which a segment is picked up (between 'from' and 'to'
     *            and repeatedly filled up for each row in the rows array.
     * @param from
     *            String to mark the start of the sub-pattern to use.
     * @param to
     *            String to mark the end of the sub-pattern to use.
     * @param showActive
     *            flag to decide based on the 'archive' field in the row whether to not show it.
     * @return String of the updated pattern with the rows in it.
     */
    public static String embedData(Database db, List<? extends ADatabaseRow> rows, String htmlPage, String from, String to, boolean showActive, List<Ontology> ontologys)
    {
        String pre = htmlPage.substring(0, htmlPage.indexOf(from));
        String post = htmlPage.substring(htmlPage.indexOf(to) + to.length());
        String pattern = htmlPage.substring(htmlPage.indexOf(from) + from.length(), htmlPage.indexOf(to));
        String fields[] = Database.getFields();
        StringBuffer sb = new StringBuffer(pre);
        int i = 0;
        for (ADatabaseRow row : rows)
        {
        	// Skip rows with are temporaries:
        	if (row.isTemporary())
        		continue;
//        	String tags = row.getField("tags");
            i++;
            if (showActive && row.isArchived()) continue;
            String rowHtml = new String(pattern);
        	Set<String> IDs = Sets.newHashSet(
        			Port.IFields.ONTOLOGY_ID, Port.IFields.FRIEND_ID, Port.IFields.ASSOCIATED_REPOSITORY_ID,
        			Mediator.IFields.INPUT_PORT_ID, Mediator.IFields.OUTPUT_PORT_ID, Mediator.IFields.RULE_SET_ID,
        			RuleSet.IFields.END1, RuleSet.IFields.END2);
            for (String field : fields)
            {
            	if (IDs.contains(field)) {
                    rowHtml = Utils.replaceAll(rowHtml, "_" + field + "_", row.getDispalyName(db, field));
                }
                else {
                    // Regular string processing.
                    String fieldV = row.getField(field);
                    // special case to shorthand the version only:
                    if (field.equalsIgnoreCase(IFields.VERSION)) {
                        fieldV = row.getVersionSuffix(); // if (false == Strings.isNullOrEmpty(fieldV) && fieldV.lastIndexOf('/') >= 0)
                        	// fieldV = fieldV.substring(fieldV.lastIndexOf('/'));
                    }
                    if (field.equalsIgnoreCase(IFields.STATUS)) {
                    	switch (fieldV) {
                    	case "error": fieldV = "notReady"; break;
                    	case "updated": fieldV = "ready"; 
                    	case "ready": if (row.isArchived()) fieldV = "notReady"; break;
                    	case "created": break;
                    	default: System.err.println("FieldV for STATUS in [" + row.getId() + "] is not ready, error, created or updated.");
                    		fieldV= "notReady";
                    	}
                    }
                    rowHtml = Utils.replaceAll(rowHtml, "_" + field + "_", fieldV);
                }
            }
            String dependencies = row.getDependencyLinks(db);
            rowHtml = Utils.replaceAll(rowHtml, "_i_", String.valueOf(i));
            rowHtml = Utils.replaceAll(rowHtml, "_dependencies_", "".equals(dependencies)?"none":dependencies);
            rowHtml = Utils.replaceAll(rowHtml, "_deleteDisabled_", row.canDelete() ? "":"hidden"); //"style='font-weight:bold; background-color:red; color:white;'" : "disabled");
            rowHtml = Utils.replaceAll(rowHtml, "_deleteDisabled4t_", row.canDelete() ? "enabled":"disabled"); //"enabledstyle='font-weight:bold; background-color:red; color:white;'" : "disabled");
            rowHtml = Utils.replaceAll(rowHtml, "_loadDisabled_", row.canLoad() ? "": "hidden"); //"disabled");
            rowHtml = Utils.replaceAll(rowHtml, "_importDisabled_", row.canImport() ? "" : "hidden"); //"disabled");
            rowHtml = Utils.replaceAll(rowHtml, "_editDisabled_", row.canEdit() ? "" : "hidden");
            rowHtml = Utils.replaceAll(rowHtml, "_showDisabled_", row.canShow() ? "" : "hidden"); //"disabled");
 //           rowHtml = Utils.replaceAll(rowHtml, "_editDisabled_", row.canEdit() ? "block" : "none");
            rowHtml = Utils.replaceAll(rowHtml, "_clearDisabled_", row.canClear() ? "" : "hidden"); //"style='background-color:#FFFFC0' disabled");
            boolean isNotRepository = !(row instanceof Port && ((Port)row).isRepository());
            rowHtml = Utils.replaceAll(rowHtml, "_clearHidden_", isNotRepository ? "hidden" : "");
            rowHtml = Utils.replaceAll(rowHtml, "_showHidden_", isNotRepository ? "hidden" : "");
            Mediator.SERVICE accessibility = Mediator.SERVICE.NO_SERVICE;
            Mediator.SERVICE activeAccessibility = accessibility.inverse();
            if (row instanceof Port) {
            	accessibility =((Port)row).getAccessibility();
                activeAccessibility = accessibility;
                if (((Port)row).isTool()) {
                	Port associated = db.getPort(row.getField(Port.IFields.ASSOCIATED_REPOSITORY_ID));
                	activeAccessibility = (null != associated)?associated.getAccessibility():Mediator.SERVICE.NO_SERVICE;
                }
            }
            if (row instanceof RuleSet) {
            	RuleSet rs = (RuleSet)row;
            	rowHtml = Utils.replaceAll(rowHtml, "_hasAPI_", rs.hasApi()?"":"not");
            	rowHtml = Utils.replaceAll(rowHtml, "_hasNoAPI_", rs.hasApi()?"not":"");
            }
//           rowHtml = Utils.replaceAll(rowHtml, "_isDirect_", isDirect ? "checked" : "");
            rowHtml = Utils.replaceAll(rowHtml, "_accessibilityIcon_", accessibility.toString());
            rowHtml = Utils.replaceAll(rowHtml, "_accessibility_", accessibility.toString());
            rowHtml = Utils.replaceAll(rowHtml, "_activeAccessibility_", activeAccessibility.toString());
            rowHtml = Utils.replaceAll(rowHtml, "_isArchived_", row.isArchived() ? "archived":"notArchived");
//            rowHtml = Utils.replaceAll(rowHtml, "_archived_", row.isArchived() ? "checked" : "");
            rowHtml = Utils.replaceAll(rowHtml, "_testDisabled_", row.canTest(db) ? "" : "hidden"); // "disabled");
            rowHtml = Utils.replaceAll(rowHtml, "_buildDisabled_", row.canBuild() ? "" : "hidden"); // "disabled");
            if (row instanceof Port && ((Port) row).isTool()) {
            	rowHtml = Utils.replaceAll(rowHtml, "_postHidden_", ((Port)row).canPostAsTool() ? "" : "hidden"); 
            	rowHtml = Utils.replaceAll(rowHtml, "_getHidden_", ((Port)row).canGetAsTool() ? "" : "hidden");
            	rowHtml = Utils.replaceAll(rowHtml, "_postDisabled_", ((Port)row).needPostAsTool() ? "" : "disabled"); 
            	rowHtml = Utils.replaceAll(rowHtml, "_getDisabled_", ((Port)row).needGetAsTool() ? "" : "disabled");
            }
            
//            if (row instanceof Mediator) {
//                rowHtml = Utils.replaceAll(rowHtml, "_repoIn_", )
            sb.append(rowHtml);
        }
        sb.append(post);
        return sb.toString();
    }



    @SuppressWarnings("unchecked")
	public void loadModelFromFile(HttpServletRequest request, HttpServletResponse response, 
			InputStream is, SmService service, String contentType) throws Exception
    {
        Database database = getDatabase();
        Map<String, Object> params = Utils.loadModelFromFileHelper(request, is, service, contentType);
        String msg = (String)params.get("msg");
        setMessage(msg);

        if (msg.length() <= 0)
        {
            String contents = Utils.buffList2String((List<byte[]>)params.get("fileName.value"));
            String id = Utils.buffList2String((List<byte[]>)params.get("id.value"));
            String rdfContentType = Utils.buffList2String((List<byte[]>)params.get("rdfContentType.value"));
            if (null == contents || "".equals(contents.trim()))
            	contents = Utils.buffList2String((List<byte[]>)params.get("model.value"));
            //String fileName = results.get("fileName.name");
            if (null == id)
            {
                setMessage("Error: Protocol error. Upload not issued for the proper item.\n");
                return;
            }
            if (null == contents || "".equals(contents.trim()))
            {
                setMessage("Error: Nothing uploaded for item [" + id + "].");
                return;
            }
            AModelRow row = (AModelRow) database.getItem(id);
            if (null == row)
            	msg = "No row found for id [" + id + "].";
            else
            	msg = row.loadModel(contents, rdfContentType);
            setMessage(msg);
        }
        showMainDialog(database, response, false); // Utils.isDm(request));
        return;
    }

    /**
     * Saves an ontology into a file in a certain item in the database.
     *
     * @param od
     * @param id
     * @param database
     * @return
     * @throws JSONException 
     */
//    @Deprecated
//    private String saveOntology(OntologyDescription od, String id, Database database)
//    {
//        AModelRow row = (AModelRow) database.getItem(id);    	
//        if (null == row) 
//        	return "Error in loading a file into missing item [" + id + "]";
//        return row.saveOntology(od);
//    }

    @Deprecated
    public Object respond2ExportTestConfig(HttpServletResponse response, String toolApi, IInputModelHandle inputModel, IOntologyHandle inputOntology, IOutputModelHandle targetModel,
            IOntologyHandle outputOntology, IRulesHandle ruleSet, ISmModuleIntercept interceptor, String contentType, String acceptType, Repository repository, boolean apply,
            SMCJobLogger job, String taskID, boolean isJson, String info) throws JSONException
    {
        String succeeded = "Succeeded";
        String applyMsg = "";
        Map<String, String> idMap = null;
        if (null != interceptor && null != inputModel)
        {
        	SMCJobLogger.SMCTaskLogger task = job.newTask(taskID, info);
        	task.start();

        	SMCJobLogger.SMCPhaseLogger mediationPhase = null, mergePhase = null;

            try
            {
            	mediationPhase = task.newPhase("Mediation");
            	mediationPhase.start();

            	interceptor.invoke(inputOntology, inputModel, outputOntology, ruleSet, targetModel);

            	mediationPhase.ended("Succeeded");
                mediationPhase = null;

                if (null != repository)
                {
                	mergePhase = task.newPhase("Merge");
                	mergePhase.start();
                	repository.setRecovery();
                    idMap = SmContainer.getContainer(getDatabase()).mergeMediatedModel(targetModel, SmContainer.getSmResources(inputModel.getModel()), repository);
                    if (apply)
                    {
                        repository.save();
                        applyMsg = "Repository [" + repository.getDomain() + "] successfully updated.";
                    } else
                    	repository.recover();

                    mergePhase.ended("Succeeded " + (apply?"[saved]":"[not saved]"));
                    mergePhase = null;
                }

                task.ended("Succeeded");
            }
            catch (Exception e)
            {
            	e.printStackTrace();
                succeeded = "<font color='red'>Failed with [" + e.getClass().getCanonicalName() + "]" + " message: [" + e.getMessage() + "]</font>";

                if (mediationPhase != null)
                	mediationPhase.ended("Failed");
                if (mergePhase != null)
                	mergePhase.ended("Failed");
                task.ended("Failed");
            }
        }

        if (isJson)
        	return respond2ExportTest4Json(response, applyMsg, toolApi, inputModel, inputOntology, targetModel,
        			outputOntology, ruleSet, interceptor, succeeded, contentType, acceptType, idMap, repository, apply);
        else
        	return respond2ExportTest4Html(response, applyMsg, toolApi, inputModel, inputOntology, targetModel,
        			outputOntology, ruleSet, interceptor, succeeded, contentType, acceptType, idMap, repository, apply);
   }

    private static String respond2ExportTest4Html(HttpServletResponse response,
    		String applyMsg, String toolApi, IInputModelHandle inputModel,
			IOntologyHandle inputOntology, IOutputModelHandle targetModel,
			IOntologyHandle outputOntology, IRulesHandle ruleSet, ISmModuleIntercept interceptor,
			String succeeded, String contentType, String acceptType,
			Map<String, String> idMap, Repository repository,
			boolean apply) {
        String htmlPage = "";
        htmlPage = Utils.getHtmlTemplate("templates/test.html");
        htmlPage = Utils.replaceAll(htmlPage, "_apiName_", toolApi);
        htmlPage = Utils.replaceAll(htmlPage, "_interceptorClass_", interceptor.getClass().getCanonicalName());
        htmlPage = Utils.replaceAll(htmlPage, "_succeeded_", succeeded);
        htmlPage = Utils.replaceAll(htmlPage, "_applyMsg_", applyMsg);

        String from = "_ModelDiv_", to = "_/ModelDiv_";
        String pre = htmlPage.substring(0, htmlPage.indexOf(from));
        String post = htmlPage.substring(htmlPage.indexOf(to) + to.length());
        String pattern = htmlPage.substring(htmlPage.indexOf(from) + from.length(), htmlPage.indexOf(to));

        htmlPage = pre;

        // Input model
        String modelPattern = Utils.replaceAll(pattern, "_type_", "input");
        modelPattern = Utils.replaceAll(modelPattern, "_ModelNS_", (null == inputModel) ? "null" : inputModel.getBase());
        modelPattern = Utils.replaceAll(modelPattern, "_model_", null == inputModel ? null : Utils.forHtml(inputModel.getRdfXML()));
        htmlPage += modelPattern;

        // Inpt Ontology
        modelPattern = Utils.replaceAll(pattern, "_type_", "inputOntology");
        modelPattern = Utils.replaceAll(modelPattern, "_ModelNS_", (null == inputOntology) ? "null" : inputOntology.getBaseNS());
        modelPattern = Utils.replaceAll(modelPattern, "_model_", null == inputOntology ? null : Utils.forHtml(inputOntology.getRdf()));
        htmlPage += modelPattern;

        // Output Ontology
        modelPattern = Utils.replaceAll(pattern, "_type_", "outputOntology");
        modelPattern = Utils.replaceAll(modelPattern, "_ModelNS_", (null == outputOntology) ? "null" : outputOntology.getBaseNS());
        modelPattern = Utils.replaceAll(modelPattern, "_model_", null == outputOntology ? null : Utils.forHtml(outputOntology.getRdf()));
        htmlPage += modelPattern;

        // Rules
        modelPattern = Utils.replaceAll(pattern, "_type_", "ruleSet");
        modelPattern = Utils.replaceAll(modelPattern, "_ModelNS_", (null == ruleSet) ? "null" : ruleSet.getBaseNS());
        modelPattern = Utils.replaceAll(modelPattern, "_model_", null == ruleSet ? null : Utils.forHtml(ruleSet.getRdf()));
        htmlPage += modelPattern;

        // target Model
        String targetContent = "null";
        try {
            if (null == acceptType || acceptType.equals(IConstants.RDF_XML))
                targetContent = Utils.forHtml(targetModel.getRdfXML());
            else
                targetContent = Utils.forHtml(Utils.modelToText(targetModel.getModel(), acceptType));
        } catch (Exception e) {
        	System.err.println("WARNING: Target model is illegal - There might have been a mediation error");
        }

        modelPattern = Utils.replaceAll(pattern, "_type_", "target");
        modelPattern = Utils.replaceAll(modelPattern, "_ModelNS_", (null == targetModel) ? "null" : targetModel.getBase());
        modelPattern = Utils.replaceAll(modelPattern, "_model_", targetContent);
        htmlPage += modelPattern;

        // id mappings
        if (null != idMap)
            post = Utils.replaceAll(post, "_idMap_", Utils.forHtml(Utils.mapS2csv(idMap, " --> ")));
        else
            if (null == repository)
                post = Utils.replaceAll(post, "_idMap_", "Not applied.");
            else
                post = Utils.replaceAll(post, "_idMap_", "Nothing to apply.");

        htmlPage += post;

        try {
			Utils.setEntity(response, htmlPage);
	        response.setHeader(HttpHeaders.CONTENT_TYPE, IConstants.HTML);
		} catch (IOException e) {
			e.printStackTrace();
		}
        return htmlPage;
	}

    private static JSONObject getModelAsJson(String prefix, IModelHandle model, String acceptType) throws JSONException {
    	JSONObject modelObj = new JSONObject();
    	modelObj.put("header", "null");
    	modelObj.put("content", "null");
    	//modelObj.put("list", new JSONObject());
    	if (null != model) {
        	modelObj.put("header", prefix + " Model base NS[" + model.getBase() + "]");
        	String content = "null";
            if (null == acceptType || acceptType.equals(IConstants.RDF_XML))
                content = model.getRdfXML();
            else
                content = Utils.modelToText(model.getModel(), acceptType);
        	modelObj.put("content", content);
    		//modelObj.put("list", processModelForJson("", "L", model.getModel(), "", "", false));
    	}
    	return modelObj;
    }

    private static JSONObject getModelAsJson(String prefix, IOntologyHandle modelH) throws JSONException {
    	JSONObject modelObj = new JSONObject();
    	modelObj.put("header", "null");
    	modelObj.put("content", "null");
    	//modelObj.put("list", new JSONObject());
    	if (null != modelH) {
//    		Model model = Utils.modelFromString(modelH.getRdf()); //model.getRdf());
        	modelObj.put("header", prefix + " Model base NS[" + modelH.getBaseNS() + "]");
    		modelObj.put("content", modelH.getRdf());
    		//modelObj.put("list", processModelForJson("", "L", model, "", "", false));
    	}
    	return modelObj;
    }

//    private static String idMap2String (Map<String, String> idMap, Repository repository) {
//        if (null != idMap)
//            return Utils.forHtml(Utils.mapS2csv(idMap, " --> "));
//        else
//            if (null == repository)
//                return "Not applied.";
//            else
//                return "Nothing to apply.";
//    }

    private static JSONArray respond2ExportTest4Json(HttpServletResponse response,
    		String applyMsg, String toolApi, IInputModelHandle inputModel,
			IOntologyHandle inputOntology, IOutputModelHandle targetModel,
			IOntologyHandle outputOntology, IRulesHandle ruleSet, ISmModuleIntercept interceptor,
			String succeeded, String contentType, String acceptType,
			Map<String, String> idMap, Repository repository,
			boolean apply) throws JSONException {

//    	results.put("apiName", toolApi);
//    	results.put("interceptorClass", interceptor.getClass().getCanonicalName());
//    	results.put("succeeded", succeeded);
//    	results.put("applyMsg", applyMsg);

    	JSONArray results = new JSONArray();
    	results.put(getModelAsJson("input", inputModel, null));
    	results.put(getModelAsJson("inputOntology", inputOntology));
    	results.put(getModelAsJson("outputOntology", outputOntology));
    	results.put(getModelAsJson("ruleSet", ruleSet));
        results.put(getModelAsJson("target", targetModel, acceptType));

//        results.put("idMap", idMap2String(idMap, repository));
//        try {
//			response.setEntity(new StringEntity(json.toString()));
//	        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}

        return results;
	}

	public static void respondForTest(HttpServletResponse response, String msg)
    {
        response.setHeader(HttpHeaders.CONTENT_TYPE, IConstants.HTML);
        response.setStatus(HttpStatus.SC_OK);
        String page = "<html><body><h1>GET Failed</h1>\n" + "<form action='/dm/sm'><button type='submit'>Return</button></form>\n" + "<pre align='left'>" + msg + "</pre>" + "</body></html>";
//        try
//        {
            Utils.respondWithText(response, page);
//        }
//        catch (UnsupportedEncodingException e)
//        {
//            e.printStackTrace();
//        }
    }

    private static JSONObject jsonFromOntology(AModelRow mRow) throws JSONException {
    	return mRow.jsonFromOntology();
    }
}

