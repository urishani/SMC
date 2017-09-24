
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.lyo.core.query.QueryUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.ADatabaseRow;
import com.ibm.dm.frontService.sm.data.ADatabaseRow.STATUS;
import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Friend;
import com.ibm.dm.frontService.sm.data.Mediator;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.data.Port.PortType;
import com.ibm.dm.frontService.sm.intfc.IInputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IModelHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IRulesHandle;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.intfc.imp.InputModelHandle;
import com.ibm.dm.frontService.sm.intfc.imp.OntologyDescription;
import com.ibm.dm.frontService.sm.intfc.imp.OntologyHandle;
import com.ibm.dm.frontService.sm.intfc.imp.OutputModelHandle;
import com.ibm.dm.frontService.sm.intfc.imp.RulesHandle;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.SMCLogger;
import com.ibm.dm.frontService.sm.utils.SMCLogger.SMCJobLogger;
import com.ibm.dm.frontService.sm.utils.SMCLogger.SMCTaskLogger;
import com.ibm.dm.frontService.sm.utils.Utils;
import com.ibm.haifa.sm.mediator.MediatorContext;

public class SmContainer
{
	private static class Job {
		final SmContainer container;
		final Database db;
		final Port port;
		final InputModelHandle inputModel;
		final Set<String> reachedPorts;
		final SMCJobLogger logger;
		final Map<String, SMCTaskLogger> propagationTasks = new HashMap<String, SMCTaskLogger>();

		public Job(SmContainer container, Database db, SMCJobLogger logger, SMCTaskLogger completedTask, Port port, InputModelHandle inputModel, Set<String> reachedPorts) {
			this.container = container;
			this.db = db;
			this.port = port;
			this.inputModel = inputModel;
			this.reachedPorts = reachedPorts;
			this.logger = logger;

			// Create a propagation task for each relevant mediator
			for (Mediator mdtr : db.getMediators()) {
				if (Mediator.SERVICE.BOTH.equals(mdtr.checkService(db)) &&
						mdtr.getInputPortId().equals(port.getId()) &&
							false == reachedPorts.contains(mdtr.getOutputPortId()))
				{
					propagationTasks.put(mdtr.getId(), logger.newTask(db.crateTaskId(mdtr.getId()), logInfo(mdtr)));
				}
			}
			complete(completedTask);
		}

		private void onEnd(long t, boolean failed, Exception e) {
			System.out.format("[%1$tF %1$tR] %2$s. Execution in [%3$.3f seconds]. %4$s%n",
					new Date(), (failed? "Failed" : "Succeeded"), (System.currentTimeMillis() - t)/1000.0,
					(failed ? ("Exception [" + e.getMessage() +"].") : ""));
			if (failed)
				this.logger.Error("Exception [" + e.getMessage() +"].");
			//this.logger.ended(failed?"Failed":"Succeeded");
		}

		private long onStart() {
			System.out.format("[%1$tF %1$tR] Starting propagation job from repository [%2$s/%3$s]%n",
					new Date(), port.getName(), port.getAccessName());
			System.out.format("[%1$tF %1$tR] Max memory is [%2$.3f GB]%n",
					new Date(), Runtime.getRuntime().maxMemory()/1024.0/1024.0/1024.0); //, mUser, mUserUri);
			//this.logger.start();
			return System.currentTimeMillis();
		}
	}

	private final Database mOwner;

	private SmContainer(Database owner) throws JSONException {
		mOwner = owner;
    	readRepositoryMap();
	}

	private Database getDatabase() {
		return mOwner;
	}
	private static String logInfo(Mediator mdtr) {
		try {
			return mdtr.getLogInfo(false, true);
		} catch (Throwable t) {
			return "WARNING: Could not get info for " + mdtr.getId();
		}
	}

	private static BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();

	private static class SyncThread extends Thread {
		public SyncThread() {
			super();
			setPriority(MIN_PRIORITY);
			start();
			System.err.println ("INFO: Sync thread started!");
		}

		public void run() {
				for (;;) {
					Job job = null; long t = -1;
					try {
						job = jobs.take(); // Pick a job from the top of the queue; blocks if queue is empty (till it's not)
						Thread.sleep(5000);
						t = job.onStart();
						propagate(job.container, job.db, job.logger, null, job.propagationTasks, job.port, job.inputModel, null, job.reachedPorts);
						job.onEnd(t, false, null);
					} catch (Exception e) {
						e.printStackTrace();
						job.onEnd(t, true, e);
					}
				}
		}
	};

	static {
		new SyncThread();
	}


	/////////////////////////////////////////////////////////////////////////

    public static final String SM_ROOT_ELEMENT_PARAM = "ROOT_RESOURCE";
    public static String       SM_MODEL_REPOSITORY_MAP = "RepositoryMap.json";

    public void doGet(HttpServletRequest request, HttpServletResponse response, List<String> toolCommand, ISmService smService) throws Exception
    {

        if (toolCommand.size() < 1)
        {
            response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
            Utils.respondWithText(response, "No API found for unspecific tool");
            return;
        }

        // Figure out that if expected result is html, than this is a test
        boolean test = false;

        Enumeration<String> headers = request.getHeaders(HttpHeaders.ACCEPT);
        if (null != headers && headers.hasMoreElements())
        {
            if (Utils.willAccept(IConstants.HTML, request)) test = true;
        }

        String toolApi = toolCommand.get(0);
        try
        {
            Database db = getDatabase();
        	// ARIEL - No config found, look for a GET port
        	String accessName = toolApi;
			List<Port> ports = db.getPorts();
			Port port = null;
			for (Port prt : ports) {
				if (prt.getAccessName().equals(accessName) &&
						prt.getStatus().equals(STATUS.READY) &&
						(
								prt.canGet()
//								prt.getType() == PortType.get ||
//								(
//										prt.getType() == PortType.repository &&
//										prt.canGet()
//								)
						)) {
					port = prt;
					break;
				}
			}

			if (null != port) {
				if (port.getType() == PortType.get)
					doGetFromPort(request, response, db, port, smService, test);
				else {
					SMCJobLogger job = SMCLogger.createJobLogger(db.createJobId());
					try {
						job.start();
						SMCTaskLogger task = job.newTask(db.crateTaskId("client"), "<-" + port.getDisplayId());
//						InputModelHandle inputModel = new InputModelHandle(port.getModelRepository().getModel());
						task.start();
						String acceptType = Utils.formatFromAcceptType4Jena(request);
						if (test) { // overwrite the accept type to the one passed in the parameter, or take a default from the database
							String contentType = smService.getParam(request, "acceptType");
							String dbContentType = port.getDatabase().getVar(Database.Vars.contentType.toString());
							if (Strings.isNullOrEmpty(contentType)) {
								contentType = dbContentType;
							}
							if (false == contentType.equals(dbContentType)) 
								port.getDatabase().setVar(Database.Vars.contentType.toString(), contentType);
							acceptType = contentType;
						}
							
						response.setStatus(HttpStatus.SC_OK);
						String text = Utils.modelToText(port.getModelRepository().getModel(), acceptType);
						Utils.setEntity(response, text);
					} catch (Exception e) {
						e.printStackTrace();
						job.failed("ERROR:" + e.getClass().getName() + "[" + e.getMessage() + "]");
					}
				}
				return;
			}

            String msg = "No READY API service found for import on API [" + toolApi + "]";
            if (test)
            {
                SmManager.respondForTest(response, msg);
            }
            else
            {
            	throw new Exception("ERROR: " + msg);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new Exception("Extraction from port failed [" + e.getMessage() + "].");
        }

    }

    /**
     * Posting an RDF model using the SMC protocol.
     * @param request
     * @param response
     * @param toolCommand
     * @param is
     * @param contentType
     * @param smService
     * @throws Exception 
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response, List<String> toolCommand, InputStream is, String contentType, SmService smService) throws Exception
    {
        if (toolCommand.size() < 1)
        {
            response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
            Utils.respondWithText(response, "No API found for unspecific tool");
            return;
        }

        // Figure out that if expected result is html, than this is a test
        boolean test = false;

        Enumeration<String> headers = request.getHeaders(HttpHeaders.ACCEPT);
        if (headers.hasMoreElements())
        {
            if (Utils.willAccept(IConstants.HTML, request)) test = true;
        }

        String toolApi = toolCommand.get(0);
        try // This looks for a RESTful post port to be used, which can be a (post)Port or an direct Repository Port.
        {
            Database db = getDatabase();
        	String accessName = toolApi;
			List<Port> ports = db.getPorts();
			Port port = null;
			for (Port prt : ports) {
				if (prt.getAccessName().equals(accessName) &&
						prt.getStatus().equals(STATUS.READY) &&
						(
								prt.canPost()
//								prt.getType() == PortType.post ||
//								(
//										prt.getType() == PortType.repository &&
//										prt.isDirect()
//								)
						)
					){
					port = prt;
					break;
				}
			}

			if (null != port) {
				if (port.getType() == PortType.post)
					doPostToPort(request, response, db, port, is, contentType, smService, test);
				else if (test && port.isReady() && port.canPost() && port.isRepository()) { // we are doing export directly to a repository from a test window.
					doPostTestToPort(request, response, db, port, is, contentType, smService);
				} else {
					SMCJobLogger job = SMCLogger.createJobLogger(db.createJobId());
					job.start();
					try {
						SMCTaskLogger task = job.newTask(db.crateTaskId("client"), "client->" + port.getDisplayId());
						InputModelHandle inputModel = new InputModelHandle(is, contentType);
						task.start();
						Map<String, String> smResources = getSmResources(inputModel.getModel());
						JSONObject flow = port.markReachablePorts(db, syncSet(port.getId()));
						flow.put("start", port.getId());
						//System.err.println(flow);
						doServiceAtPort(db, job, task, port, inputModel, smResources, new ResponseWrapper(response), syncSet(), false);
					} catch (Exception e) {
						System.err.println("job " + job.getId() + " - failed.");
						job.failed("ERROR:" + e.getClass().getName() + "[" + e.getMessage() + "]");
						job.ended("failed");
						throw e;
					}
				}
				return;
			}

            String msg = "Failed. Reason:<br>" + "No READY API service found for export on API [" + toolApi + "]";
            if (test)
            {
                SmManager.respondForTest(response, msg);
            }
            else
            {
                throw new Exception(msg);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            if (test)
            {
                SmManager.respondForTest(response, "Error [" + e.getClass().getName() + "]: [" + e.getMessage() + "]");
            }
            else
                throw new Exception(e);
        }
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response,List<String> oslcCommand, String rdf, String contentType, SmService smService) throws Exception
    {
        if (oslcCommand.size() < 2)
        {
            response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
            Utils.respondWithText(response, "No API found for unspecific repository/resource");
            return;
        }

        String domain = oslcCommand.get(0);
        try
        {
            Port row = getDatabase().getPort4Domain(domain);
            if (null == row)
            	throw new Exception("OSLC fails: Cannot find repository for access name [" + domain + "]");
            Repository repository = (Repository) row.getModelRepository();
            Model M1 = new InputModelHandle(rdf, contentType).getModel();
            Model M2 = repository.getModel();
            String base = M2.getNsPrefixURI("base");

        	String N = oslcCommand.get(2);
        	Resource R1 = M1.getResource(base + N);
        	Resource R2 = M2.getResource(base + N);
        	if (R1 != null) {
            	if (R2 != null) // clean it
            		R2.removeProperties();
            	StmtIterator it = R1.listProperties();
            	while (it.hasNext()) {
            		Statement stmt = it.next();
            		M2.add(stmt);
            	}
            }

        	String text = Utils.modelToText(M1, contentType);
        	Utils.respondWithText(response, text, contentType);
            repository.setDirty();
            repository.save();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new Exception("OSLC failed [" + e.getMessage() + "].");
        }

    }

	// We're at a GET port - Find a mediator and extract the model
    private void doGetFromPort(HttpServletRequest request, HttpServletResponse response,
    	Database db, Port port, ISmService smService, boolean test)
    throws Exception
    {
		List<Mediator> mediators = db.getMediators();
		Mediator mediator = null;
		for (Mediator mdtr : mediators) {
			if (Mediator.SERVICE.IMPORT.equals(mdtr.checkService(db)) &&
					mdtr.getOutputPortId().equals(port.getId())) {
				mediator = mdtr;
				break;
			}
		}

		if (null != mediator) {
			OutputModelHandle targetModel = getOutputModel(db, mediator, smService.getParam(request, SM_ROOT_ELEMENT_PARAM));
            if (null == targetModel.getModel())
            	throw new Exception("Failed to extract a model for Mediator id [" + mediator.getId() + "] and Api [" + port.getAccessName() + "].");

            response.setStatus(HttpStatus.SC_OK);
            String acceptType = Utils.formatFromAcceptType4Jena(request);
            if (test)
            {
                acceptType = smService.getParam(request, "acceptType");
                SmManager.respondForTest(response, Utils.forHtml(Utils.modelToText(targetModel.getModel(), acceptType)));
            }
            else
            {
                response.setStatus(HttpStatus.SC_OK);
                String text = Utils.modelToText(targetModel.getModel(), acceptType);
                Utils.setEntity(response, text);
            }
            return;
		}
    }

    /**
     * Process OSLC request for a resource, or all resources
     * Request URL is of the form "https://host:9444/dm/sm/repository/domain/resource/101".<br>
     * The oslcCommand list is: [ "domain", "resource", "101" ].<br>
     * Potential params are "id" to identify the details of a repository of ontology, and "query" for a query
     * to be initialized in SPARQL.
     * @param request
     * @param response
     * @param oslcCommand
     * @param smService
     * @throws Exception 
     */
	public void doOslcGet(HttpServletRequest request, HttpServletResponse response,
			List<String> oslcCommand, ISmService smService) throws Exception {

		Database db = getDatabase();
		String query = smService.getParam(request, "query");
		if (oslcCommand.size() == 1 && Strings.isNullOrEmpty(query)) {  // This is a repository/<accessName> case (but not query) - it is an OSLC repository, and we respond with
			// a description of this service provider.
			new OSLCHelper(getDatabase()).respondForServiceProvider(oslcCommand.get(0), request, response, smService);
			return;
		}
		String id = "";
		String version = null;
		String versionInfo = "";
		if (null != smService) {
			id = smService.getParam(request, "id");
			version = smService.getParam(request, "version");
			if (false == Strings.isNullOrEmpty(version) && false == "current".equals(version))
				versionInfo = "&nbsp;&nbsp;[ For version " + Repository.dateOfVersion(version).toString() + "]";
		}
        if (oslcCommand.size() < 1)
        {
            response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
            Utils.respondWithText(response, "No API found for unspecific repository");
            return;
        }

        String contentType = smService.getParam(request, Database.Vars.contentType.toString());
        if (Strings.isNullOrEmpty(contentType)) {
        	contentType = db.getVar(Database.Vars.contentType.toString());
        } else
        	db.setVar(Database.Vars.contentType.toString(), contentType);

        if ((false == Utils.willAccept(IConstants.HTML, request) && false == Utils.willAccept(IConstants.PLAIN_TEXT_TYPE, request)) || 
        		Strings.isNullOrEmpty(contentType)) {
        	contentType = Utils.formatFromAcceptType4Jena(request);
        }

		ADatabaseRow row = db.getItem(id);
        String domain = oslcCommand.get(0);
        if (null == row && null != domain)
        	row = db.getPort4Domain(domain);
        try
        {
            Repository repository = (Repository) row.getModelRepository(); // This is a model repository.// Repository.create(row);
            Model model = repository.getModelForVersion(version, null);
            if (null == model)
            	model = repository.getModel();

        	String select = smService.getParam(request, "select");
        	String where = smService.getParam(request, "where");
        	String orderBy = smService.getParam(request, "orderBy");
        	String restore = smService.getParam(request, "restore");
        	String name = smService.getParam(request, "name");
        	String save = smService.getParam(request, "save");
        	String deleteSaved = smService.getParam(request, "deleteSaved");


            if (false == Strings.isNullOrEmpty(query)) {
            	JSONObject contents = makeInitialJsonForSparql(query, repository, id, version);
            	JSONObject jo = getQueryParams(smService, request);
            	Iterator<?> keys = jo.keys();
            	while (keys.hasNext()) {
            		String key = (String)keys.next(); // JSONObject.getNames(jo)) {
            		contents.put(key, Utils.safeGet(jo, key));
//            		contents.putAll(getQueryParams(smService, request));
            	}
                Utils.respondGetWithTemplateAndJson(contents, "/templates/sparql.html", response);
            	return;
            } else if (oslcCommand.size() == 2 && "sparql".equals(oslcCommand.get(1))) {
            	                
            	if (Strings.isNullOrEmpty(select) || Strings.isNullOrEmpty(where)) {
            		Utils.respondWithText(response, "Illegal query, no SELECT or WHERE");
            		return;
            	}
            	select = select.trim();
            	where = where.trim();
            	if (Strings.isNullOrEmpty(orderBy))
            		orderBy = "";
            	orderBy = orderBy.trim();
            	JSONObject contents = getSparqlContents(request, repository, select, where, orderBy, id, smService,
            			name, false == Strings.isNullOrEmpty(restore),
            			false == Strings.isNullOrEmpty(save),
            			false == Strings.isNullOrEmpty(deleteSaved));
            	contents.put("moreOrLess", smService.getParam(request, "moreOrLess"));
//        		System.out.println("Contents for the SPARQL query page:\n[" + contents.toString() + "]");
            	Utils.respondGetWithTemplateAndJson(contents, "/templates/sparql.html", response);
            	return;
            }

            String base = repository.getBase(); //getModel().getNsPrefixURI("base");
            List<Resource> R = new ArrayList<Resource>();
           	boolean isSingleResource = false;
            
            String title = "";
            if (oslcCommand.size() == 2 && null != smService.getParam(request, "sparql")) {
            	// do sparql instead of standard oslc query
            	if (Strings.isNullOrEmpty(select) || Strings.isNullOrEmpty(where)) {
            		Utils.respondWithText(response, "Illegal query, no SELECT or WHERE");
            		return;
            	}
            	select = select.trim();
            	where = "{ " + where.trim() + " } ";
            	if (Strings.isNullOrEmpty(orderBy))
            		orderBy = "";
            	orderBy = orderBy.trim();
            	JSONObject contents = getSparqlContents(request, repository, select, where, orderBy, id, smService,
            			name, false == Strings.isNullOrEmpty(restore),
            			false == Strings.isNullOrEmpty(save),
            			false == Strings.isNullOrEmpty(deleteSaved));
            	if (Utils.willAccept(IConstants.PLAIN_TEXT_TYPE, request)) { // This is an AJAX query to get list of resources,
            														// yet the list needs to be formatted according to the contentType
            		String resOpen = "\"", resClose = "\"";
            		if (contentType.equals(IConstants.N_TRIPLE) || 
//            			contentType.equals(IConstants.NTRIPLE) ||
            			contentType.equals(IConstants.TURTLE) ||
            			contentType.equals(IConstants.N3)
            			) {
            				resOpen = "<";
            				resClose = ">";
            		}

            		StringBuffer sb = new StringBuffer();
            		JSONArray rows = (JSONArray) Utils.safeGet(contents, "rows");
            		if (rows.length() < 1)
            			sb.append("No resources reference url for " + where.split(" ")[3].trim());
            		else { 
            			sb.append("Following resources reference " + where.split(" ")[3].trim());
            			for (int i=0; i < rows.length(); i++) {
            				JSONObject theRow = (JSONObject) rows.get(i); 
            				JSONArray values = (JSONArray) Utils.safeGet(theRow, "values");
            				for (int j=0; j < values.length(); j++) {
            					JSONObject val = (JSONObject) values.get(j);
            					sb.append("\n").append(resOpen).append(base).append(Utils.safeGet(val, "td").toString().split(":")[1]).append(resClose);
            				}
            			}
            		}
            		Utils.respondWithText(response, sb.toString());
            	} else {
//            		System.out.println("Contents for the SPARQL query page:\n[" + contents + "]");
                	contents.put("moreOrLess", smService.getParam(request, "moreOrLess"));
                	Utils.respondGetWithTemplateAndJson(contents, "/templates/sparql.html", response);
            	}
            	return;
            } else if (oslcCommand.size() > 2 && "resource".equals(oslcCommand.get(1))) { // single resource
//                boolean isSingleResource = (oslcCommand.size() > 2 && "resource".equals(oslcCommand.get(1)));
//                if (isSingleResource) {
                	Resource r = model.getResource(base + oslcCommand.get(2));
                	title = "Resource [" + r.getURI() + "] in Domain [" + domain + "]";
                	R.add(r);
                	isSingleResource = true;
                	//getOslcresourcdeNew(base, repository, oslcCommand.get(2));
           } else {
            
            	title = "Resources in " + (("".equals(id))?"":"[" + id + "], ") + "Domain [" + domain + "]";
            	if (oslcCommand.size() <= 2) {
            		String oslcWhere = smService.getParam(request, "oslc.where");
            		String oslcPrefix = smService.getParam(request, "oslc.prefix");
//            		String oslcProperties = smService.getParam(request, "oslc.properties");
//            		String oslcOrderBy = smService.getParam(request, "oslc.orderBy");

            		// Handle the oslc prefixes:
            		Map<String, String> prefixMap = new HashMap<String, String>();

            		if (false == Strings.isNullOrEmpty(oslcPrefix))
            			try {
            				prefixMap = QueryUtils.parsePrefixes(oslcPrefix);
            			} catch (org.eclipse.lyo.core.query.ParseException e) {
            				throw new IOException(e.getMessage());
            			}


            			// Handle the oslc properties part:
//            			Properties properties = null;
//
//            			try {
//            				if (Strings.isNullOrEmpty(oslcProperties)) {
//            					properties = QueryUtils.WILDCARD_PROPERTY_LIST;
//            				} else
//            					properties = QueryUtils.parseSelect(oslcProperties, prefixMap);
//            			} catch (org.eclipse.lyo.core.query.ParseException e) {
//            				throw new IOException(e.getMessage());
//            			} catch (RuntimeException e) {
//            				e.printStackTrace();
//            			}

//            			// Handle the oslc where clause:
//            			WhereClause whereClause = null;
//
//            			if (false == Strings.isNullOrEmpty(oslcWhere)) {
//            				try {
//            					whereClause = QueryUtils.parseWhere(oslcWhere, prefixMap);
//            				} catch (org.eclipse.lyo.core.query.ParseException e) {
//            					throw new IOException(e.getMessage());
//            				}
//            			}

//            			// Handle the oslc order by clause:
//            			OrderByClause orderByClause = null;
//
//            			if (false == Strings.isNullOrEmpty(oslcOrderBy)) {
//            				try {
//            					orderByClause = QueryUtils.parseOrderBy(oslcOrderBy, prefixMap);
//            				} catch (org.eclipse.lyo.core.query.ParseException e) {
//            					throw new IOException(e.getMessage());
//            				}
//            			}

//            			Model model = repository.getModel();
            			if (Strings.isNullOrEmpty(oslcWhere)) {
            				ResIterator it = model.listSubjects();
            				while (it.hasNext()) {
            					Resource res = it.next();
            					if (res.getURI().startsWith(base))
            						R.add(res);
            				}
            			} else {
            				prefixMap.putAll(model.getNsPrefixMap());
            				prefixMap = Utils.fixNsPrefixes(prefixMap);
            				String prefix = Utils.makePrefix4Query(prefixMap);
            				String sparql = "SELECT ?r WHERE + { ?r ?p ?v }";
            				String t[] = oslcWhere.split("=");
            				if (t.length > 1) {
            					String pred = t[0].trim();
            					String value = t[1].trim();
            					sparql = prefix + "SELECT ?r WHERE { ?r " + pred + " " + value + ". }";
            					QueryExecution qexec = QueryExecutionFactory.create(sparql, model);
            					ResultSet rslt = qexec.execSelect();
            					while (rslt.hasNext()) {
            						QuerySolution soltn = rslt.next();
            						RDFNode r = (RDFNode) soltn.get("r");
            						if (r.toString().startsWith(base))
            							R.add((Resource)r);
            					}
            				}
            			}
            			new OSLCHelper(getDatabase()).repsondForOslcQuery(R, request, response, smService, id);
            			return;
            	} else {
            		String resource = oslcCommand.get(1), N = "";
            		if (resource.startsWith("resource#"))
            			N = resource.substring("resource#".length());
            		else if (resource.startsWith("resource/"))
            			N = resource.substring("resource/".length());
            		R.add(model.getResource(base + N));
            	}
            }
            Model tmpModel = ModelFactory.createDefaultModel();
            //        	model.setNsPrefixes(Utils.fixNsPrefixes(repository.getModel().getNsPrefixMap()));
            for (Resource res : R) {
            	StmtIterator it = res.listProperties();
            	while (it.hasNext()) {
            		Statement stmt = it.next();
            		tmpModel.add(stmt);
            	}
            }

//        	String type = Utils.formatFromAcceptType4Jena(request);
//        	if (Utils.willAccept(ContentTypes.HTML, request)) {
//        		type = db.getVar(Database.Vars.contentType.toString());
//        	}
        	String output = Utils.modelToText(tmpModel, contentType);

            response.setHeader(HttpHeaders.ETAG, repository.getEtag());

            JSONObject contents = new JSONObject();
            contents.put("rdf", output);
            contents.put("id", id);
            String url = request.getRequestURL().toString(); //.getUri();
            if (url.indexOf('?') >=0)
            	url = url.substring(0, url.indexOf('?'));
            contents.put("url", url);
            contents.put(Database.Vars.contentType.toString(), db.getVar(Database.Vars.contentType.toString()));
            contents.put("header", title);
    		contents.put("canDownload", isSingleResource?"false":"true");
    		contents.put("host", db.getHost(true, true));// db.getBaseURL()); //Utils.getHost(null, true));
    		contents.put("canmodify", "true");
    		contents.put("version", Strings.isNullOrEmpty(version)?"":version);
            contents.put("versionInfo", versionInfo);
            //.processModelForJson(domain, "ShowRDF", repository, title, id, R);

//    		String acceptType = Utils.formatFromAcceptType4Jena(request);

        	if (Utils.willAccept(IConstants.HTML, request)) {
//        		JSONObject contents = SmManager.processModelForJson(domain, "ShowRDF", model, title, id, true);
//        		contents.put("canDownload", isSingleResource?"false":"true");
        		contents.put("hideGraphics", "");
        		Utils.respondGetWithTemplateAndJson(contents, "/templates/rdf4oslc.html", response);
        	} else if (Utils.willAccept(IConstants.JSON, request)){
//        		JSONObject contents = SmManager.processModelForJson(domain, "ShowRDF", model, title, id, true);
//        		contents.put("canDownload", isSingleResource?"false":"true");
        		Utils.respondWithJson(response, contents);
        	} else {
        		Utils.respondWithText(response, output, contentType);
        	}
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new Exception("OSLC failed [" + e.getMessage() + "].");
        }
	}

	/**
	 * Answers with a processing of the additional query params: distinct, reduced, limit and offset,
	 * and return them in the result as a hash map.
	 * @param smService provider of the HTTP query parameters 
	 * @param request HTTP handler to get parameters from .
	 * @return Map of coded values of the parameters to be used 
	 * @throws JSONException 
	 */
	private JSONObject getQueryParams(ISmService smService, HttpServletRequest request) throws JSONException {
		String distinct = smService.getParam(request, "distinct");
		String reduced = smService.getParam(request, "reduced");
		String offset = smService.getParam(request, "offset");
		String limit = smService.getParam(request, "limit");
		String offsetVal_ = smService.getParam(request, "offsetVal");
		String limitVal_ = smService.getParam(request, "limitVal");
		int offsetVal = 0;
		if (null != offsetVal_)  try {
			offsetVal = Integer.parseInt(offsetVal_);
		} catch (Exception e){}
		int limitVal = 0;
		if (null != limitVal_)  try {
			limitVal = Integer.parseInt(limitVal_);
		} catch (Exception e){}
		JSONObject result = new JSONObject();
		result.put("distinctChecked", (null != distinct && distinct.equals("on"))?"checked":"");
		result.put("reducedChecked", (null != reduced && reduced.equals("on"))?"checked":"");
		result.put("offsetChecked", (null != offset && offset.equals("on") && offsetVal > 0)?"checked":"");
		result.put("limitChecked", (null != limit && limit.equals("on") && limitVal > 0)?"checked":"");
		result.put("offsetVal", Integer.toString(offsetVal));
		result.put("limitVal", Integer.toString(limitVal));
		return result;
	}

	/**
	 * Answer with the JSON object to fill up a response from a sparql query.
	 * @param request
	 * @param repository
	 * @param select
	 * @param where
	 * @param orderBy
	 * @param id
	 * @param smService
	 * @param name String name of a saved query to possibly use, or the one being executed and which 
	 * needs to be stored under that name - if the name is not empty string.
	 * @param restore boolean to indicate that the name needs to be restored from saved storage. That 
	 * will happen if the name has been picked by the user from a list of such names.
	 * @return
	 * @throws JSONException 
	 */
	public JSONObject getSparqlContents(HttpServletRequest request,
			Repository repository, String select, String where, String orderBy, String id, ISmService smService,
			String name, boolean restore, boolean save, boolean delete) throws JSONException {
		if (null != name)
			name = name.trim();
		String version = (null != smService)?smService.getParam(request, "version"):"current";
		Model model = repository.getModelForVersion(version, null);
    	if (null == model) {
    		version = "";
    		model = repository.getModel();
    	}
    	JSONObject queryParams = null;
		JSONObject savedQueries = repository.getSavedQueries();
   		queryParams =  (null != smService)?getQueryParams(smService, request):new JSONObject();
		JSONObject aQuery = new JSONObject();
		aQuery.put("select", select);
		aQuery.put("where", where);
		aQuery.put("orderBy", orderBy);
		aQuery.put("name", name);
		aQuery.put("queryParams", queryParams);
    	if (restore) {
    		if (null != savedQueries && null != Utils.safeGet(savedQueries, name)) {
    			aQuery = (JSONObject) Utils.safeGet(savedQueries, name);
    			select = (String) Utils.safeGet(aQuery, "select");
    			where = (String) Utils.safeGet(aQuery, "where");
    			orderBy = (String) Utils.safeGet(aQuery, "orderBy");
    			Object qp = Utils.safeGet(aQuery, "queryParams");
    			if (null != qp)
    				queryParams = (JSONObject)qp;
    		}
    	} else if (false == Strings.isNullOrEmpty(name) && save){ // save it
    		repository.saveQuery(aQuery);
    	} else if (false == Strings.isNullOrEmpty(name) && delete){ // delete it
    		JSONObject tQuery = new JSONObject();
    		tQuery.put("delete", "true");
    		tQuery.put("name", name);
    		repository.saveQuery(tQuery);
    		name="";
    	} 
    	// save it also under the empty name to mark the "default" last query on this item.
   		aQuery.put("name", "");
   		repository.saveQuery(aQuery);

   		JSONObject contents = makeJsonForSparql(select, where, "", repository,
				smService.getParam(request, "resourceType"),
				orderBy,
				smService.getParam(request, "sortresource"),
				smService.getParam(request, "descresource"),
				smService.getParam(request, "usedByChecked"),
				version,
				model
				);
		contents.put("name", name);
		Iterator<?> keys = queryParams.keys();
		while (keys.hasNext()) {
			String key = (String)keys.next(); //: JSONObject.getNames(queryParams)) {
			contents.put(key, Utils.safeGet(queryParams, key));
//			contents.putAll(queryParams);
		}
		
		JSONArray rows = new JSONArray();
		JSONArray row;
		JSONObject theRow;
		JSONArray header = new JSONArray();
		JSONObject th;
		JSONObject td;
//		boolean hasResults = false;
		ResultSet rslt = null;
		try {
			Map<String, String> pxMap = Utils.fixNsPrefixes(model.getNsPrefixMap());
			rslt = executeQuery(model, aQuery, pxMap); 
			List<String> vars = rslt.getResultVars();
			for (String string : vars) {
				th = new JSONObject();
				th.put("th", string);
//				hasResults = true;
				header.put(th);
			}
			Set<String> properties = new HashSet<String>();
			contents.put("headers", header);
			int num = 0;
			while (rslt.hasNext()) {
				num++;
				row = new JSONArray();
				QuerySolution soltn = rslt.next();
				for (String title : vars) {
					RDFNode node = (RDFNode) soltn.get(title);
					td = new JSONObject();
					td.put("td", (null==node)?"-":Utils.prefixForResource(node.toString(), pxMap));
					row.put(td);
					if ("resource".equals(title) && repository.getModel().containsResource(node)) {
						StmtIterator stmt= repository.getModel().listStatements(node.asResource(), (Property)null, (String)null);
						while (stmt.hasNext()) {
							properties.add(stmt.next().getPredicate().toString());
						}
					}
				}
				theRow = new JSONObject();
				theRow.put("values", row);
				theRow.put("num", num);
				theRow.put("evenRow", num%2);
				rows.put(theRow);
			}
			contents.put("rows", rows);
			if (properties.size() > 0) {
				JSONArray props = new JSONArray();
				for (String prop : properties) {
					String prefixedProp = Utils.prefixForResource(prop, pxMap);
					// remove the rdf:type from this list
					if ("rdf:type".equals(prefixedProp))
						continue;;

					JSONObject p = new JSONObject();
					p.put("predicate", prefixedProp);
					p.put("checked", "");
					p.put("sortChecked", "");
					p.put("sortDisabled", "disabled");
					p.put("descDisabled", "disabled");
					if (null != smService.getParam(request, prefixedProp)) {
						p.put("checked", "checked");
						if (null != smService.getParam(request, "sort" + prefixedProp))
							p.put("sortChecked", "checked");
						if (null != smService.getParam(request, "desc" + prefixedProp))
							p.put("descChecked", "checked");
						p.put("sortDisabled", "");
						p.put("descDisabled", "");
					}
					props.put(p);
				}
				contents.put("predicates", props);
			}

		} catch (Exception e) {
			StringBuffer result = new StringBuffer();
			result.append("Error: ").append(e.getClass().getName()).append(": ")
			.append(e.getMessage());
//			.append("</pre></font><br>");
			contents.put("error", result.toString());
			contents.put("headers", new JSONArray());
			contents.put("rows", new JSONArray());
//			contents.put("showResults", "0");
		} finally {
			// Can we close the query from here?
		}

//		if (hasResults)
//			contents.put("showResults", "1");
		contents.put("id", id);
		contents.put("version", version);
		return contents;
	}

    public ResultSet executeQuery(Model model, JSONObject aQuery, Map<String, String> pxMap) throws JSONException {
		String prefix = Utils.makePrefix4Query(pxMap);
		String dist_redu = "";
		JSONObject queryParams = (JSONObject) Utils.safeGet(aQuery, "queryParams");
		if ("checked".equals(Utils.safeGet(queryParams, "distinctChecked")))
			dist_redu = "DISTINCT ";
		if ("checked".equals(Utils.safeGet(queryParams, "reducedChecked")))
			dist_redu += "REDUCED ";
		String select = (String) Utils.safeGet(aQuery, "select");
		String where = (String) Utils.safeGet(aQuery, "where");
		String orderBy = (String) Utils.safeGet(aQuery, "orderBy");
		String sparql = prefix + "SELECT " + dist_redu + select + "\nWHERE\n" + where;
		if (false == "".equals(orderBy))
			sparql += "\nORDER BY " + orderBy;
		
		if ("checked".equals(Utils.safeGet(queryParams, "limitChecked")))
			sparql += " LIMIT " + Utils.safeGet(queryParams, "limitVal");
		if ("checked".equals(Utils.safeGet(queryParams, "offsetChecked")))
			sparql += " OFFSET " + Utils.safeGet(queryParams, "offsetVal");

		System.out.println("Will execute SPARQL [" + sparql  +"]");
		
		QueryExecution qexec= null;
		ResultSet rslt = null;
//		try {
			Query query =  QueryFactory.create(sparql);
			qexec = QueryExecutionFactory.create(query.toString(), model);
			rslt = qexec.execSelect();
//		} finally {
//			if (null != qexec)
//				qexec.close();
//		}
		return rslt;
	}

	private static JSONObject makeJsonForSparql(String select, String where, String report, Repository repository, String resourceType,
    		String orderBy, String resourceSortChecked, String resourceDescChecked, String usedByChecked, 
    		String version, Model model) throws JSONException {
    	JSONObject result = new JSONObject();
    	Map<String, String> map = Utils.fixNsPrefixes(model.getNsPrefixMap());
    	String prefixes = Utils.makePrefix4Query(map);
    	result.put("base", map.get("base").toString());
    	result.put("resourceType", resourceType);
    	result.put("domain", repository.getDomain());
    	result.put("prefixes", prefixes);
    	result.put("select", select);
    	result.put("where", where);
    	result.put("orderBy", orderBy);
    	result.put("report", report);
    	result.put("error", "");
    	result.put("name", "");
    	result.put("saved", savedQueriesNames(repository.getSavedQueries()));
//    	result.put("showResults", "0");
		result.put("predicates", new JSONArray());
		result.put("usedByChecked",  (null==usedByChecked)?"":"checked");
		result.put("resourceSortChecked", (null==resourceSortChecked)?"":"checked");
		result.put("resourceDescChecked", (null==resourceDescChecked || null==resourceSortChecked)?"":"checked");
		result.put("resourceDescDisabled", (null==resourceSortChecked)?"disabled":"");
		result.put("version", version);
		result.put("versionInfo", Strings.isNullOrEmpty(version)?"":"&nbsp;&nbsp;[ For version " + Repository.dateOfVersion(version).toString() + "]");
    	return result;
	}

    /**
     * Answers with a JSONArray with the names of the saved queries as the "item" of each entry in the
     * array of JSONObject(s).
     * @param savedQueries
     * @return
     * @throws JSONException 
     */
	private static JSONArray savedQueriesNames(JSONObject savedQueries) throws JSONException {
		JSONArray saved = new JSONArray();
    	if (null != savedQueries) {
    		Iterator<?> keys = savedQueries.keys();
    		while (keys.hasNext()) {
    			Object key= keys.next();//: savedQueries.keySet()) {
    			JSONObject item = new JSONObject();
    			item.put("item", key);
    			saved.put(item);
    		}
    	}
    	return saved;
	}

	public static JSONObject makeInitialJsonForSparql(String query, 
			Repository repository, String id, String version) throws JSONException {
		Model model = repository.getModelForVersion(version, null);
    	if (null == model) {
    		version = "";
    		model = repository.getModel();
    	}
		JSONObject result = makeJsonForSparql("?resource", "{\n   ?resource a " + query + " .\n}",
    			"Not activated yet", repository, query, "", null, null, null, version, model);
		result.put("error", "");
		result.put("headers", new JSONArray());
		result.put("rows", new JSONArray());
		result.put("id", id);
		result.put("version", version);

		return result;
	}

	private OutputModelHandle getOutputModel(Database db, Mediator mediator, String root)
    throws Exception
    {
        String interceptorClassName = mediator.getInterceptorClass();
//        Class<ISmModuleIntercept> interceptorClass = (Class<ISmModuleIntercept>) Class.forName(interceptorClassName);
        ISmModuleIntercept interceptor = mediator.getInterceptor();
    	if ("".equals(root)) root = null;
    	if (null != root) {
    		Method mInit = null;
    		try { mInit = interceptor.getClass().getMethod("init", String[].class);} catch (Exception e) {};
    		if (null == mInit) {
    			System.err.println("warning: Mediator [" + interceptorClassName + "] is invoked with init parameter, but has no init() method to call in interceptor [" + interceptorClassName + "]!");
    		} else {
    			mInit.invoke(interceptor, new Object[]{new String[]{root}});
    		}
    	}
    	
//        try
//        {
//
//            Constructor<ISmModuleIntercept> constructor = interceptorClass.getConstructor(String.class);
//            interceptor = constructor.newInstance(root);
//        }
//        catch (NoSuchMethodException e)
//        {
//            //e.printStackTrace();
//        }
//
//        if (null == interceptor) interceptor = (ISmModuleIntercept) interceptorClass.newInstance();

        IInputModelHandle inputModel = getInputModel(db, db.getPort(mediator.getInputPortId()));
        OutputModelHandle targetModel = new OutputModelHandle();
		IOntologyHandle inputOntology = getOntologyHandle((AModelRow)db.getItem(db.getPort(mediator.getInputPortId()).getOntologyId()));
		IOntologyHandle outputOntology = getOntologyHandle((AModelRow)db.getItem(db.getPort(mediator.getOutputPortId()).getOntologyId()));
		IRulesHandle ruleSet = (IRulesHandle) getOntologyHandle((AModelRow)db.getItem(mediator.getRuleSetId()));

        // --------------    INVOKATION   --------------------------
        // Invoking the interceptor sm module for this tool
        // TODO need to generalize to any situation.
        interceptor.invoke(inputOntology, inputModel, outputOntology, ruleSet, targetModel);
        // -----------------------------------------------------

        return targetModel;
    }

    private IInputModelHandle getInputModel(Database db, Port port)
    throws Exception
    {
    	PortType type = port.getType();
    	if (PortType.repository == type) {
    		Repository repository = (Repository) port.getModelRepository(); // Repository.create(port);
            IInputModelHandle inputModel = new InputModelHandle(repository.getModel());
            return inputModel;
    	}
    	else if (PortType.pipe == type) {
			List<Mediator> mediators = db.getMediators();
			Mediator mediator = null;
			for (Mediator mdtr : mediators) {
				// Other side of mediator should be either a pipe or a repository
				if (Mediator.SERVICE.BOTH.equals(mdtr.checkService(db)) &&
						mdtr.getOutputPortId().equals(port.getId())) {
					mediator = mdtr;
					break;
				}
			}

			if (null != mediator) {
				OutputModelHandle targetModel = getOutputModel(db, mediator, null);
				IInputModelHandle inputModel = new InputModelHandle(targetModel.getModel());
				return inputModel;
			}
			else
            	throw new Exception("Failed to find a mediator that outputs into pipe port [" + port.getAccessName() + "].");
    	}
    	else {
    		throw new RuntimeException("Invalid port type [" + type + "] for port [" + port.getId() + "], " +
    			"expected " + PortType.repository + " or " + PortType.pipe);
    	}
    }

    @SuppressWarnings("unchecked")
	private void doPostTestToPort(HttpServletRequest request, HttpServletResponse response, Database db,
    		Port port, InputStream is, String contentType, SmService smService) {
    	try {
    		ByteArrayOutputStream bos = new ByteArrayOutputStream();
    		byte [] tmp = new byte[65536]; int c;
    		while ((c = is.read(tmp)) != -1)
    			bos.write(tmp, 0, c);
    		InputStream bis = new ByteArrayInputStream(bos.toByteArray());
    		is.close();
    		Map<String, Object> params = Utils.loadModelFromFileHelper(request, bis, smService, contentType);

    		List<Mediator> mediators = db.getMediators();
    		Mediator mediator = null;
    		for (Mediator mdtr : mediators) {
    			if (Mediator.SERVICE.EXPORT.equals(mdtr.checkService(db)) &&
    					mdtr.getInputPortId().equals(port.getId())) {
    				mediator = mdtr;
    				break;
    			}
    		}

    		Set<String> reachablePorts = syncSet();
    		if (null != mediator) {
    			JSONObject flow = mediator.markReachablePorts(db, reachablePorts);
    			flow.put("start", port.getId());
        		JSONArray list = new JSONArray();
        		list.put(mediator.getId());
        		flow.put(port.getId(), list);
				//System.err.println(flow);
    		}
    		SMCJobLogger job = SMCLogger.createJobLogger(db.createJobId());
    		job.start();

    		InputModelHandle inputModel = null;
    		String modelData = Utils.buffList2String((List<byte[]>)params.get("fileName.value"));
    		if (null == modelData || "".equals(modelData.trim()))
    			modelData = Utils.buffList2String((List<byte[]>)params.get("model.value"));
    		String modelContentType = Utils.buffList2String((List<byte[]>)params.get("contentType.value"));
    		if (null != modelData && modelData.trim().length() > 0) {
    			try {
    				inputModel = new InputModelHandle(new ByteArrayInputStream(modelData.getBytes()), modelContentType);
    			} catch (Exception e) {
    				SmManager.respondForTest(response, "Error: Input is illegal [" + e.getClass().getCanonicalName() + "]: [" + e.getMessage() + "].");
    				return;
    			}

    			SMCTaskLogger task = job.newTask(db.crateTaskId("browser"), "-D->" + port.getDisplayId());
    			task.start();
    			doServiceAtPort(db, job, task, port, inputModel, new HashMap<String, String>(), 
    					new ResponseWrapper(response), reachablePorts, false);
    			SmManager.respondForTest(response, "Successfully exported a model directly to repository port [" + port.getDisplayId() + "]");
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    		SmManager.respondForTest(response, "Error [" + e.getClass().getName() + "]: [" + e.getMessage() + "]");
    	}
    }

    // We're at a POST port - Find a mediator and export the model
	@SuppressWarnings("unchecked")
	private void doPostToPort(HttpServletRequest request, HttpServletResponse response, Database db,
			Port port, InputStream is, String contentType, SmService smService, boolean test) throws Exception {

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte [] tmp = new byte[65536]; int c;
			while ((c = is.read(tmp)) != -1)
				bos.write(tmp, 0, c);
			InputStream bis = new ByteArrayInputStream(bos.toByteArray());
			is.close();

			// DEBUG - Print the posted payload
			//System.err.println("!!! PAYLOAD BEGIN !!!");
			//System.err.println(bos);
			//System.err.println("!!! PAYLOAD END !!!");

			List<Mediator> mediators = db.getMediators();
			Mediator mediator = null;
			for (Mediator mdtr : mediators) {
				if (Mediator.SERVICE.EXPORT.equals(mdtr.checkService(db)) &&
						mdtr.getInputPortId().equals(port.getId())) {
					mediator = mdtr;
					break;
				}
			}


			if (null != mediator) {
				boolean doApply;
				Map<String, Object> params = null;
				if (test) {
					params = Utils.loadModelFromFileHelper(request, bis, smService, contentType);
					String apply = Utils.buffList2String((List<byte[]>)params.get("apply.value"));
					doApply = null != apply && "on".equals(apply);
				} else
					doApply = true;
				if (doApply) {
					Set<String> reachablePorts = syncSet();
					JSONObject flow = mediator.markReachablePorts(db, reachablePorts);
					flow.put("start", port.getId());
					JSONArray list = new JSONArray();
					list.put(mediator.getId());
					flow.put(port.getId(), list);
					//System.err.println(flow);
				}

				SMCJobLogger job = SMCLogger.createJobLogger(db.createJobId());
				job.start();
//				IInputModelHandle inputModel = null;

				String msg = (String)params.get("msg");
				if (msg.length() > 0) {
					SmManager.respondForTest(response, msg);
					return;
				}
				String modelData = Utils.buffList2String((List<byte[]>)params.get("fileName.value"));
				if (null == modelData || "".equals(modelData.trim()))
					modelData = Utils.buffList2String((List<byte[]>)params.get("model.value"));
//				String modelContentType = Utils.buffList2String((List<byte[]>)params.get("contentType.value"));
//				if (null != modelData && modelData.trim().length() > 0) {
//					try {
//						inputModel = new InputModelHandle(new ByteArrayInputStream(modelData.getBytes()), modelContentType);
//					} catch (Exception e) {
//						SmManager.respondForTest(response, "Error: Input is illegal [" + e.getClass().getCanonicalName() + "]: [" + e.getMessage() + "].");
//						return;
//					}
//				}
				String mediationTrace = doServiceAtMediator(request, response, db, job, mediator, bis, contentType, params, test);
				String pair = makePair(mediator); //.getInputPortId(), mediator.getOutputPortId());
				Map<String, String> smResources = repositoryMap.get(pair);
				if (null == smResources) {
					smResources = new HashMap<String, String>();
					repositoryMap.put(pair, smResources);
				}
				smResources.put(Mediator.MEDIATION_TRACE_KEY, mediationTrace);
				saveRepositoryMap();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (test) {
				SmManager.respondForTest(response, "Error [" + e.getClass().getName() + "]: [" + e.getMessage() + "]");
			} else
				throw e;
		}
	}

	private static void complete(SMCTaskLogger task) {
		if (null != task)
			task.ended("Succeeded");
	}

	private static void cancel(SMCTaskLogger task) {
		if (null != task)
			//task.cancel();
			task.cancelled("Redundant situation");
	}

	// We're at an export mediator: our first mediator, between a POST port, and whatever is behind it (a PIPE or a REPOSITORY)
	// transform the model and pass it on to the output port
	@SuppressWarnings("unchecked")
	private String doServiceAtMediator(HttpServletRequest request, HttpServletResponse response, Database db,
			SMCJobLogger job, Mediator mediator, InputStream is, String contentType, Map<String, Object> params, boolean test)
	throws Exception
	{
		SMCJobLogger.SMCTaskLogger task = null;
		String taskID = db.crateTaskId(mediator.getId());

//		String interceptorClassName = mediator.getInterceptorClass();
//		Class<ISmModuleIntercept> interceptorClass = (Class<ISmModuleIntercept>)Class.forName(interceptorClassName);
		ISmModuleIntercept interceptor = mediator.getInterceptor(); //interceptorClass.newInstance();

		IInputModelHandle inputModel = null;
		if (test) {
			String msg = (String)params.get("msg");
			if (msg.length() > 0) {
				SmManager.respondForTest(response, msg);
				return msg;
			}
			String modelData = Utils.buffList2String((List<byte[]>)params.get("fileName.value"));
			if (null == modelData || "".equals(modelData.trim()))
				modelData = Utils.buffList2String((List<byte[]>)params.get("model.value"));
			String modelContentType = Utils.buffList2String((List<byte[]>)params.get("contentType.value"));
			if (null != modelData && modelData.trim().length() > 0) {
				try {
				inputModel = new InputModelHandle(new ByteArrayInputStream(modelData.getBytes()), modelContentType);
				} catch (Exception e) {
					String traceMsg = "Error: Input is illegal [" + e.getClass().getCanonicalName() + "]: [" + e.getMessage() + "].";
					SmManager.respondForTest(response, traceMsg);
					return traceMsg;
				}
			}
		} else
			inputModel = new InputModelHandle(is, contentType);
		OutputModelHandle targetModel = new OutputModelHandle();
		IOntologyHandle inputOntology = getOntologyHandle((AModelRow)db.getItem(db.getPort(mediator.getInputPortId()).getOntologyId()));
		IOntologyHandle outputOntology = getOntologyHandle((AModelRow)db.getItem(db.getPort(mediator.getOutputPortId()).getOntologyId()));
		IRulesHandle ruleSet = (IRulesHandle) getOntologyHandle((AModelRow)db.getItem(mediator.getRuleSetId()));

		Port outputPort = db.getPort(mediator.getOutputPortId());
		PortType outputPortType = outputPort.getType();
//		Repository repository = null;
//		if (PortType.repository == outputPortType)
//			repository = (Repository) outputPort.getModelRepository(); // Repository.create(outputPort);

		Map<String, String> smResources = getSmResources(inputModel.getModel());
//		if (test) {
//			String internalContentType = Utils.buffList2String((List<byte[]>)params.get("contentType.value"));
//			String acceptType = Utils.buffList2String((List<byte[]>)params.get("acceptType.value"));
//			String apply = Utils.buffList2String((List<byte[]>)params.get("apply.value"));
//
//			String accessName = db.getPort(mediator.getInputPortId()).getAccessName();
//			boolean doApply = null != apply && "on".equals(apply);
//			new SmManager(getDatabase()).respond2ExportTestConfig(response, accessName, inputModel, inputOntology, targetModel, outputOntology, ruleSet, interceptor, internalContentType, acceptType,
//					repository, doApply, job, taskID, false, mediator.getLogInfo(test, doApply));
//			// SmManager has just written the HTTP response for the client; let's mark it.
//			markResponse(response);
//			if (!doApply) {
//				outputPort.sync();
//				return;
//			}
//		}
//		else {
			task = job.newTask(taskID, mediator.getLogInfo(false, true));
			task.start();
			SMCJobLogger.SMCPhaseLogger mediationPhase = task.newPhase("Mediation");
			mediationPhase.start();

			MediatorContext context = new MediatorContext();
			interceptor.initialized(context);
			try {
				interceptor.invoke(inputOntology, inputModel, outputOntology, ruleSet, targetModel);
			} catch (Exception e) {
				mediationPhase.ended("Failed [" + e.getClass() + ": " + e.getMessage() + "]");
				task.ended("Failed");
				throw e;
			}
			

			mediationPhase.ended("Succeeded");

			if ((PortType.pipe == outputPortType) || (PortType.repository == outputPortType) || (PortType.export == outputPortType))
				doServiceAtPort(db, job, task, outputPort, 
						new InputModelHandle(targetModel.getModel()), 
						smResources, new ResponseWrapper(response), syncSet(), false);
			else
				complete(task);

			return context.getMediationTrace();
	}

	// The following method mediates a model between two internal ports (PIPEs and REPOSITORIEs)
	private String doServiceAtMediator(Database db, SMCJobLogger job, SMCTaskLogger completedTask, SMCTaskLogger currentTask, Mediator mediator, InputModelHandle inputModel, ResponseWrapper response,
		Set<String> reachedPorts)
	throws Exception
	{
		// Avoid useless work...
        Port outputPort = db.getPort(mediator.getOutputPortId());
        Port inputPort = db.getPort(mediator.getInputPortId());
		if (reachedPorts.contains(outputPort.getId())) {
			complete(completedTask);
			cancel(currentTask);
			return "Mediation has been cancelled";
		}

		if (null == currentTask)
			currentTask = job.newTask(db.crateTaskId(mediator.getId()), logInfo(mediator));
		complete(completedTask);
		currentTask.start();

		SMCJobLogger.SMCPhaseLogger mediationPhase = currentTask.newPhase("Mediation");
		mediationPhase.start();

		Map<String, String> smResources = getSmResources(inputModel.getModel());
		String pair = makePair(inputPort, outputPort);
		Map<String, String> storedSmResources = repositoryMap.get(pair);
		if (null != storedSmResources)
			smResources.putAll(storedSmResources);
		
		OutputModelHandle targetModel = new OutputModelHandle();

		MediatorContext context = new MediatorContext();

		try {
			String interceptorClassName = mediator.getInterceptorClass();
			System.out.println("interceptorClassName [" + interceptorClassName + "]");
//			Class<ISmModuleIntercept> interceptorClass = (Class<ISmModuleIntercept>)Class.forName(interceptorClassName);
			ISmModuleIntercept interceptor = mediator.getInterceptor(); //interceptorClass.newInstance();
			interceptor.initialized(context);

			IOntologyHandle inputOntology = getOntologyHandle((AModelRow)db.getItem(db.getPort(mediator.getInputPortId()).getOntologyId()));
			IOntologyHandle outputOntology = getOntologyHandle((AModelRow)db.getItem(db.getPort(mediator.getOutputPortId()).getOntologyId()));
			IRulesHandle ruleSet = (IRulesHandle) getOntologyHandle((AModelRow)db.getItem(mediator.getRuleSetId()));
			interceptor.invoke(inputOntology, inputModel, outputOntology, ruleSet, targetModel);

			mediationPhase.ended("Succeeded");

		} catch(Exception e) {
			mediationPhase.ended("Failed [" + e.getClass() + ": " + e.getMessage() + "]");
			currentTask.ended("Failed");
			throw e;
		}

		PortType outputPortType = outputPort.getType();
        if ((PortType.pipe == outputPortType) || (PortType.repository == outputPortType) || PortType.export == outputPortType)
            doServiceAtPort(db, job, currentTask, outputPort, new InputModelHandle(targetModel.getModel()), smResources, response, reachedPorts, false);
        else
        	complete(currentTask);
        return context.getMediationTrace();
	}

	/**
	 * Performs a post of model data at a port, which may be on the initial port of a chain of mediation, 
	 * or at the following up ports. The difference is marked in the <i>response</i> parameter by using the 
	 * method <i><b>markResponse()</b></i>. When marked, than the mapping of resources are NOT saved in the 
	 * response. That has been done already when the response was not marked when the chain of tasks
	 * in the mediation job have started. At that starting point, the mapping of resource IS returned to 
	 * the client posting that model content.
	 * <p>
	 * The <i>internal</i> parameter is used to mark the case that the post is done from within the same
	 * server, and the mapping is managed internally through the Java APIs, and not the REST APIs. In this case,
	 * the map is also not saved in the response, but left to the caller of this method, via the return value,
	 * to be embedded properly in the response to the HTML dashboard of this environment.
	 * 
	 * @param db - Database
	 * @param job - Job of the mediation chain.
	 * @param task - Present task being pursued in that job.
	 * @param port - Port on which the mediated model needs to be merged with.
	 * @param inputModel InputModel wrapper of the model being merged into that port.
	 * @param smResources Map of resources to be used from previous application of merging of mediated or original models
	 * and this port.
	 * @param response Http Response object for the present HTTP request.
	 * @param reachedPorts List of ports that have already been processed as part of the present job, so we are 
	 * not repeating any port and not running into infinitte loops.
	 * @param internal boolean flag (used to be used for testing in the past, and that is now obsolete), and used to indicate 
	 * we are working in an internal initiation of mediation and not external, so we run our logic a bit different.
	 * @return Map of resources as being processed in this merge of model and a repository.
	 * @throws Exception
	 */
	private Map<String, String> doServiceAtPort(Database db, SMCJobLogger job, SMCJobLogger.SMCTaskLogger task, Port port, InputModelHandle inputModel, Map<String, String> smResources,
		ResponseWrapper response, Set<String> reachedPorts, boolean internal)
	throws Exception
	{
		// Avoid infinite loops!
		if (!reachedPorts.add(port.getId())) {
			cancel(task);
			return null;
		}

		boolean isFirstRepository = isPending(response);

		if (port.isRepository()) {
			SMCJobLogger.SMCPhaseLogger mergePhase = task.newPhase("Merge");
			mergePhase.start();

			try {
				Repository repository = (Repository) port.getModelRepository(); //Repository.create(port);
				Map<String, String> map = mergeMediatedModel(inputModel, smResources, repository);
				// not convert all resources in the input model according to the map:
				Model mergedModel = ModelFactory.createDefaultModel();
				StmtIterator stmt = inputModel.getModel().listStatements();
				while (stmt.hasNext()) {
					Statement s = stmt.next();
					Resource r = s.getSubject();
					String nrS = map.get(r.toString());
					if (null == nrS)
						continue;
					Resource nr = mergedModel.createResource(nrS);
					RDFNode o = s.getObject();
					if (o.isResource()) {
						String noS = map.get(o.toString());
						if (null != noS) {
							o = mergedModel.createResource(noS);
						}
					}
					Statement ns = mergedModel.createStatement(nr, s.getPredicate(), o);
					mergedModel.add(ns);
				}
				inputModel = new InputModelHandle(mergedModel);
				
		        if (! isFirstRepository || internal)
		            writeToRepositoryMap(map);
				if (map == null) {
			          if (response != null) // Response will be null unless this is the first repository to be reached
			        	  response.getResponse().setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			          mergePhase.ended("Failed - internal server error");
			          task.ended("Failed");
			          return null;
				} else {
			        if (isFirstRepository) {
			        	if (!internal) {
			        		setResponse(map, response);
			        	}
			        	markResponse(response);
			        }
			        repository.setDirty();
	                repository.save();

	                mergePhase.ended("Succeeded");
	                smResources = map;
				}
			} catch (Exception e) {
				mergePhase.ended("Failed");
				task.ended("Failed:"+ e.getClass().getName());
				return null;
			}
		}

		if (PortType.export == port.getType()) {
			setSmResources(inputModel, port);
			Friend friend = db.getFriend(port.getFriendId());
			HttpPost post = new HttpPost("https://" + friend.getIpAddress() + "/dm/sm/tool/" + port.getAccessName());
			post.setHeader(HttpHeaders.CONTENT_TYPE, IConstants.RDF_XML);
			post.setEntity(new StringEntity(inputModel.getRdfXML()));

			HttpResponse resp = friend.execute(post);

			Map<String, String> map = new HashMap<String, String>();
			Scanner data = new Scanner(resp.getEntity().getContent());
			while(data.hasNext()) {
				String local = data.next(), remote = data.next();
				map.put(local, remote);
			}
			data.close();
			writeToRepositoryMap(map, port);
			setResponse(map, response);
		}

		port.sync();

		if (isPending(response))
			propagate(this, db, job, task, new HashMap<String, SMCTaskLogger>(), port, inputModel, response, reachedPorts);
		else
			jobs.add(new Job(this, db, job, task, port, inputModel, reachedPorts));
		return smResources;
	}

	private static void propagate(SmContainer container, Database db, SMCJobLogger job, 
			SMCTaskLogger completedTask, Map<String, SMCTaskLogger> propagationTasks, 
			Port port, InputModelHandle inputModel, ResponseWrapper response, 
			Set<String> reachedPorts)
	throws Exception {
		try {
			List<Mediator> mediators = db.getMediators();
			Mediator mediator = null;
			for (Mediator mdtr : mediators) {
				// Other side of mediator should be either a pipe or a repository
				if (Mediator.SERVICE.BOTH.equals(mdtr.checkService(db)) &&
						mdtr.getInputPortId().equals(port.getId())) {
					mediator = mdtr;
					Model copyOfModel = ModelFactory.createDefaultModel().add(inputModel.getModel());
					SMCTaskLogger currentTask = propagationTasks.remove(mdtr.getId());
					String mediationTrace = container.doServiceAtMediator(db, job, completedTask, currentTask, mediator, new InputModelHandle(copyOfModel), response, reachedPorts);
					String pair = makePair(mediator);
					Map<String, String> smResources = repositoryMap.get(pair);
					if (null == smResources) {
						smResources = new HashMap<String, String>();
						repositoryMap.put(pair, smResources);
					}
					smResources.put(Mediator.MEDIATION_TRACE_KEY, mediationTrace);
					container.saveRepositoryMap();
					completedTask = null;
				}
			}
		} finally {
			complete(completedTask);
			for (SMCTaskLogger task : propagationTasks.values())
				cancel(task);
		}
	}

	private void setResponse(Map<String, String> map, ResponseWrapper response)
	throws Exception {
		if (isPending(response)) {
			response.getResponse().setStatus(HttpStatus.SC_OK);
			String text = Utils.mapS2csv(map, " ");
			Utils.setEntity(response.getResponse(), text);
			markResponse(response);
		}
	}

	private void markResponse(ResponseWrapper response) {
		response.mark(); //setHeader("sm-ok", "yes");
	}

	private boolean isPending(ResponseWrapper response) {
		boolean ispending = response != null && 
				!response.isMarked(); // containsHeader("sm-ok");
		return ispending;
	}

    /**
     * Helper method to establish an ontology handle from a database row given its
     * id.
     *
     * @param id
     *            String for the row id of an ontology or a rule-set.
     * @param db
     *            Database of configuration data.
     * @return {@link IOntologyHandle} or null if not defined or row is not ready, or if
     *         file of the ontology is not found.
     */
    public static IOntologyHandle getOntologyHandle(AModelRow row)
    {
        if (null == row || false == row.getStatus().equals(STATUS.READY)) return null;
        OntologyDescription od = row.getOntologyDescription();
//        Model m = row.getModelRepository().getModel();
//        od.load(m);
        if (row instanceof Ontology)
            return new OntologyHandle(od);
        else
            return new RulesHandle(od);
    }

//    public Map<String, String> mergeMediatedModel(IModelHandle mediatedModel, Map<String, String> smResources, Repository repository) {
//    	return mergeMediatedModel(mediatedModel, smResources, repository, true);
//    }

    /**
     * 2nd version (July 2015) - allow to delete resources in target model where resources in the mediated model are maked
     * as for delete. That is when a resource as a predicate smc:hasTag to a resource marker smc:isDeleted. In these
     * case - the associated target resource should be removed entirely from the model. If no associated
     * resoruce - than the deleted resource is simply ignored.
     * @param mediatedModel
     * @param smResources
     * @param repository
     * @param isFirstRepository
     * @return a Map of the mapped resources in the mediated model to the target model.
     */
    public Map<String, String> mergeMediatedModel(IModelHandle mediatedModel, Map<String, String> smResources, Repository repository) //, boolean isFirstRepository)
    {
    	Model model = mediatedModel.getModel();
        ResIterator iter = model.listSubjects();
        repository.setNsPrefixes(model.getNsPrefixMap());
        Map<String, String> map = new HashMap<String, String>();
        Property hasTag = model.createProperty(IConstants.SM_PROPERTY_SM_TAG_FULL);
        Resource isDeleted = model.createResource(IConstants.SM_PROPERTY_RESOURCE_DELETED_FULL);

        while (iter.hasNext())
        {
            Resource mediatedResource = iter.nextResource();
            if (Repository.isConfigResource(mediatedResource))
            	continue;
            Resource smResource = null;

        	boolean resourceDeleted = Utils.resourceHasPropertyValue(mediatedResource, hasTag, isDeleted);

            String mediatedResourceURI = mediatedResource.getURI();
            String domain = getDomain(mediatedResourceURI);
            if (null != domain) {
                String repositoryPair = makePair(domain, repository.getDomain());
                if (repositoryMap.containsKey(repositoryPair) && repositoryMap.get(repositoryPair).containsKey(mediatedResourceURI))
                	smResource = repository.getResource(repositoryMap.get(repositoryPair).get(mediatedResourceURI));
            }
            if (null == smResource && null != smResources && smResources.containsKey(mediatedResource.getURI()))
            	smResource = repository.getResource((String)smResources.get(mediatedResource.getURI()));

        	if (null == smResource)
        		smResource = repository.getResource();
        	
        	if (resourceDeleted && null != smResource) { //delete the sm resource in the target repository: meaning all the triples that reference it (as subject or object).
        		Model rmodel = repository.getModel();
        		rmodel.remove(rmodel.listStatements(smResource, (Property)null, (RDFNode)null));
        		rmodel.remove(rmodel.listStatements((Resource)null, (Property)null, smResource));
        		
        		// Now also delete that from the map
        		map.remove(mediatedResource.getURI());
        		map.remove(smResource.getURI());
        	}
        	if (! resourceDeleted)
        		map.put(mediatedResource.getURI(), smResource.getURI());
        }
        // Loop over the new resources coming in from the mediation, and insert their properties
        // as new or updated resources in the repository.
        Property smResourceProperty = model.getProperty(IConstants.SM_PROPERTY_SM_RESOURCE_FULL);
        for (String resourceUri : map.keySet())
        {
            String mappedResourceUri = map.get(resourceUri);
            Resource repositoryResource = repository.getModel().getResource(mappedResourceUri);
            // As per Defect #5612, we must only replace the properties
            // which appear in the mediated resource, and not remove the existing ones beforehand
            // repositoryResource.removeProperties(); // delete this one from the repository.
            StmtIterator statements = model.getResource(resourceUri).listProperties();
            while(statements.hasNext())
            {
                Statement statement = statements.next();
                Property p = statement.getPredicate();
                if (p.equals(smResourceProperty))
                	continue;
                repositoryResource.removeAll(p);
            }
            // Now create all these relations in the repository
            statements = model.getResource(resourceUri).listProperties();
            while (statements.hasNext())
            {
                Statement statement = statements.next();
                RDFNode object = statement.getObject();
                Property p = statement.getPredicate();
                if (p.equals(smResourceProperty))
                	continue;
                if (object instanceof Resource)
                {
                    String mappedObjectUri = map.get(object.toString());
                    if (null != mappedObjectUri) object = repository.getModel().getResource(mappedObjectUri);
                }
                repository.add(repositoryResource, p, object);
            }
        }

// the next seems to be doing illegal thing and it has no effect on the results.        
//        // Rename resources in mediated model to their new URI
//        iter = model.listSubjects();
//        while (iter.hasNext()) {
//        	Resource res = iter.next();
//        	String resUri = res.getURI();
//        	if (map.containsKey(resUri))
//            	ResourceUtils.renameResource(res, map.get(resUri));
//        }

        return map;
    }

    public static SmContainer getContainer(Database owner) throws JSONException
    {
         return new SmContainer(owner);
    }

    public static String getFolder()
    {
        return Database.SM_MODEL_FOLDER;
    }

    static Map<String, Map<String, String>> repositoryMap = null;

    public  synchronized void readRepositoryMap() throws JSONException {
    	if (repositoryMap == null) {
    		repositoryMap = new HashMap<String, Map<String, String>>();
    		try {
    			Database db = getDatabase();
    			FileInputStream is = new FileInputStream(new File(db.getFolder(), SM_MODEL_REPOSITORY_MAP));
    			String mrm = Utils.stringFromStream(is);
    			JSONObject map = new JSONObject(mrm);
    			is.close();
    			Iterator<?> keys = map.keys();
    			while (keys.hasNext()) {
    				Object obj = keys.next();// : map.keySet()) {
    				String repositoryPair = (String)obj;
    				Map<String, String> resourceMap = new HashMap<String, String>();
    				JSONObject jsonResourceMap = (JSONObject)Utils.safeGet(map, repositoryPair);
    				Iterator<?> keys2 = jsonResourceMap.keys();
    				while (keys2.hasNext()) {
    					Object key2 = keys2.next();// : jsonResourceMap.keySet())
    					resourceMap.put(key2.toString(), Utils.safeGet(jsonResourceMap, key2.toString()).toString());
    					repositoryMap.put(repositoryPair, resourceMap);
    				}
    			}
    		} catch (IOException e) {}
    	}
    }

    public synchronized void writeToRepositoryMap(Map<String, String> map) throws JSONException {
    		if (null != map && null != repositoryMap) {
    		for (Map.Entry<String, String> entry : map.entrySet()) {
    			addPairToRepository(entry.getKey(), entry.getValue());
    			addPairToRepository(entry.getValue(), entry.getKey());
    		}
    		saveRepositoryMap();
    	}
    }

    public synchronized void writeToRepositoryMap(Map<String, String> map, Port exportPort)  throws Exception {
    	assert Port.PortType.export.equals(exportPort.getType());
    	if (null != map && null != repositoryMap) {
    		for (Map.Entry<String, String> entry : map.entrySet())
    			addPairToRepository(entry.getKey(), entry.getValue(), exportPort);
    		saveRepositoryMap();
    	}
    }

    public void resetDomain(String domain) throws JSONException {
    	readRepositoryMap();
    	String[] keys = repositoryMap.keySet().toArray(new String[0]);
    	for (String key : keys) {
    		if (key.startsWith(domain + "->"))
				repositoryMap.remove(key);
    		if (key.endsWith("->" + domain))
    			repositoryMap.remove(key);
    	}
    	saveRepositoryMap();
    }

    private void saveRepositoryMap() throws JSONException {
        try {
        	FileWriter writer = new FileWriter(new File(getDatabase().getFolder(), SM_MODEL_REPOSITORY_MAP));
        	JSONObject jsonMap = new JSONObject();
        	for (String pair : repositoryMap.keySet()) {
        		Map<String, String> resourceMap = repositoryMap.get(pair);
        		JSONObject jsonResourceMap = new JSONObject();
        		for (String key : resourceMap.keySet())
        			jsonResourceMap.put(key, resourceMap.get(key));
        		jsonMap.put(pair, jsonResourceMap);
        	}
        	writer.write(jsonMap.toString());
        	writer.close();
        } catch (IOException e) {
        	System.err.println(e.getMessage());
        }
    }

    private static void addPairToRepository(String key, String value) {
    	String D1 = getDomain(key), D2 = getDomain(value);
    	if (null != D1 && null != D2) {
        	String repositoryPair = makePair(D1, D2);
        	if (!repositoryMap.containsKey(repositoryPair))
        		repositoryMap.put(repositoryPair, new HashMap<String, String>());
        	repositoryMap.get(repositoryPair).put(key, value);
    	}
    }

    private void addPairToRepository(String key, String value, Port exportPort) throws Exception
    {
    	assert Port.PortType.export.equals(exportPort.getType());
    	Database db = exportPort.getDatabase();
    	Friend friend = db.getFriend(exportPort.getFriendId());
    	if (null != friend) {
        	String D1 = getDomain(key), D2 = getDomain(exportPort);
	    	if (null != D1 && null != D2) {
	        	String repositoryPair = makePair(D1, D2);
	        	if (!repositoryMap.containsKey(repositoryPair))
	        		repositoryMap.put(repositoryPair, new HashMap<String, String>());
	        	repositoryMap.get(repositoryPair).put(key, value);
	    	}
    	}
    }

    /**
     * Utility to make the pair reference of ports listed in a mediator.
     * @param mdt
     * @return
     */
    public static String makePair(Mediator mdt) {
    	return makePair(mdt.getInputPortId(), mdt.getOutputPortId(), mdt.getDatabase());
    }
    /**
     * Create a reference to a pair of ports for mediation mapping, based on their id.
     * @param id1
     * @param id2
     * @param db
     * @return
     */
    public static String makePair(String id1, String id2, Database db) {
    	Port p1 = db.getPort(id1), p2 = db.getPort(id2);
  		return makePair(p1, p2);
    }
    public static String makePair(Port p1, Port p2) {
    	if (null != p1 && null != p2)
    		return makePair(p1.getAccessName(), p2.getAccessName());
    	return "";
    }
    
    public static String makePair(String d1, String d2) {
    	return d1 + "->" + d2;
    }

    private static String getDomain(String resourceUri) {
    	String REPOSITORY_PATH = "/dm/sm/repository/";
    	if (null != resourceUri && resourceUri.contains(REPOSITORY_PATH)) {
        	int x = resourceUri.indexOf(REPOSITORY_PATH) + REPOSITORY_PATH.length();
        	int y = resourceUri.indexOf('/', x);
        	return resourceUri.substring(x, y);
    	} else
    		return null;
    }

    String getDomain(Port exportPort) throws Exception {
    	assert Port.PortType.export.equals(exportPort.getType());
    	Friend friend = exportPort.getDatabase().getFriend(exportPort.getFriendId());
    	if (null != friend)
    		return friend.getIpAddress() + "/" + exportPort.getAccessName();
    	else
    		return null;
    }

    public static Map<String, String> getSmResources(Model model) {
    	Map<String, String> map = new HashMap<String, String>();
        Property smResourceProperty = model.getProperty(IConstants.SM_PROPERTY_SM_RESOURCE_FULL);
    	ResIterator it = model.listResourcesWithProperty(smResourceProperty);
    	while (it.hasNext()) {
    		Resource res = it.next();
    		map.put(res.getURI(), res.getProperty(smResourceProperty).getObject().asResource().getURI());
    	}
    	return map;
    }

    public void setSmResources(InputModelHandle inputModel, Port exportPort) throws Exception {
    	assert Port.PortType.export.equals(exportPort.getType());
    	Model model = inputModel.getModel();
        Property smResourceProperty = model.getProperty(IConstants.SM_PROPERTY_SM_RESOURCE_FULL);
		ResIterator iterator = model.listSubjects();
		while (iterator.hasNext()) {
			Resource local = iterator.next();
			String localUri = local.getURI();
			String D1 = getDomain(localUri), D2 = getDomain(exportPort);
			if (null != D1 && null != D2) {
				String repositoryPair = makePair(D1, D2);
	    		if (repositoryMap.containsKey(repositoryPair)) {
	    			Map<String, String> associations = repositoryMap.get(repositoryPair);
	    			if (associations.containsKey(localUri)) {
	    				String remoteUri = associations.get(localUri);
	    				local.addProperty(smResourceProperty, remoteUri);
    				}
    			}
    		}
    	}
    }

    private static Set<String> syncSet(String ... S) {
    	Set<String> set = Collections.synchronizedSet(new HashSet<String>());
    	for (String s : S)
    		set.add(s);
    	return set;
    }

    /**
	 * Performs a post of the model contents of a tool to its associated repository.
     * This copy needs to use a resource mapping, so 
     * it has not been populated already with all the resource mappings as appropriate.
     * @param tool Port source Port to post from
     * @param toPort target Port to post to
     * @param response to track the mediation flow and report the final pairing at the end.
     * @return Map of resources as copied to the target repository.
     */
	public Map<String, String> postToolToPort(Port tool, Port toPort,
			ResponseWrapper response) {
		Repository r = (Repository) ((null != tool)?tool.getModelRepository():null);
		Model m = (null != r)?r.getModel():null;
		if (null == m)
			return null;
		Database db = getDatabase();
		SMCJobLogger job = SMCLogger.createJobLogger(db.createJobId());
		job.start();
		SMCTaskLogger task = job.newTask("post@" + toPort.getId(), tool.getId() + " --> " + toPort.getId());
		task.start();
		
		try {
			String pair = makePair(tool, toPort);
			Map<String, String> smResources = repositoryMap.get(pair);
			if (null == smResources)
				System.out.println("No resource map for this pair [" + pair + "]");
//			markResponse(response); // prevent next method from setting the response content.
			JSONObject flow = toPort.markReachablePorts(db, syncSet(toPort.getId()));
			flow.put("start", toPort.getId());
			//System.err.println(flow);
//			doServiceAtPort(db, job, task, port, inputModel, smResources, response, syncSet(), false);

			smResources = doServiceAtPort(db, job, task, toPort, new InputModelHandle(m), smResources, response, syncSet(), true);
//			if (response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR)
//				return null;
			// need to save the mapping now:
	//		repositoryMap.
			return smResources;
		} catch (Exception e) {
			task.ended("Failed");
			return null;
		}
	}

    /**
	 * Performs a get of the model contents of a tool from its associated repository.
     * This copy needs to use a resource mapping, so 
     * it has not been populated already with all the resource mappings as appropriate.
     * @param tool Port target Port to get into
     * @param fromPort source Port to get from
     * @param response to track the mediation flow and report the final pairing at the end.
     * @return Map of resources as copied to the target repository.
     */
	public Map<String, String>  getToolFromPort(Port tool, Port fromPort,
			ResponseWrapper response) {
		Repository r = (Repository) ((null != fromPort)?fromPort.getModelRepository():null);
		Model m = (null != r)?r.getModel():null;
		if (null == m)
			return null;
		Database db = getDatabase();
		SMCJobLogger job = SMCLogger.createJobLogger(db.createJobId());
		job.start();
		SMCTaskLogger task = job.newTask("get@" + fromPort.getId(), tool.getId() + " <-- " + fromPort.getId());
		task.start();
		SMCJobLogger.SMCPhaseLogger mergePhase = task.newPhase("Merge");
		mergePhase.start();
		try {
			String pair = makePair(fromPort, tool);
			Map<String, String> smResources = repositoryMap.get(pair);
			if (null == smResources)
				System.out.println("No resource map for this pair [" + pair + "]");
			Repository repository = (Repository) tool.getModelRepository();
			Map<String, String> map = mergeMediatedModel(new InputModelHandle(m), smResources, repository);
			if (map == null) {
				if (response != null) // Response will be null unless this is the first repository to be reached
					response.getResponse().setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				mergePhase.ended("Failed - internal server error");
				task.ended("Failed");
			} else {
				repository.setDirty();
				repository.save();
				mergePhase.ended("Succeeded");
				task.ended("Succeeded");
				writeToRepositoryMap(map);
			}	
			return map;
		} catch (Exception e) {
			mergePhase.ended("Failed");
			task.ended("Failed");
			return null;
		}
	}

    /**
     * Perform a post request to itself, from the given model that needs to use a resource mapping, so 
     * it has not been populated already with all the resource mappings as appropriate.
     * an internal "copy" repository to its original repository. That is obtained through the 
     * id of the original repository. 
     * @param m Model to be posted
     * @param db reference to Database
     * @param tmp_port source (copy) Port to post from. If that is not null, it is a save action and no further mediation should happen.
     * Further mediation is a separate activity, initiated by the user from the dash-board.
     * @param to_port target Port to post to
     * @param response to track the mediation flow and report the final pairing at the end.
     * @return Map of resources as copied to the target repository.
     */
	public Map<String, String> saveTempToPort(Model m, Database db,
			Port tmp_port, Port to_port, HttpServletResponse response) {

		if (null == tmp_port || null == to_port)
			return null;
		SMCJobLogger job = SMCLogger.createJobLogger(db.createJobId());
		job.start();
		SMCTaskLogger task = job.newTask("save@" + to_port.getId(), to_port.getId() + "-Copy->" + to_port.getId());
		task.start();
		//				IInputModelHandle inputModel = null;

		SMCJobLogger.SMCPhaseLogger mergePhase = task.newPhase("Merge");
		mergePhase.start();
		try {
			String pair = makePair(tmp_port,to_port);
			Map<String, String> smResources = repositoryMap.get(pair);
			if (null == smResources)
				System.out.println("No resource map for this pair [" + pair + "]");
			Repository repository = (Repository) to_port.getModelRepository(); //Repository.create(port);
			Map<String, String> map = mergeMediatedModel(new InputModelHandle(m), smResources, repository);
			if (map == null) {
				if (response != null) // Response will be null unless this is the first repository to be reached
					response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				mergePhase.ended("Failed - internal server error");
				task.ended("Failed");
//				job.ended("Failed");
			} else {
				repository.setDirty();
				repository.save();
				mergePhase.ended("Succeeded");
				resetDomain(tmp_port.getAccessName()); // removed the temporary mapping.
				db.delete(tmp_port.getId(), true);
				task.ended("Succeeded");
//				job.ended("Succeeded");
			}	
			return map;
		} catch (Exception e) {
			mergePhase.ended("Failed");
			task.ended("Failed");
//			job.ended("Failed");
			return null;
		}
	}

    
}
