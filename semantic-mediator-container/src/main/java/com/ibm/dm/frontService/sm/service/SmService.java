
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

//import net.openservices.rio.store.RioServerException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.http.HttpStatus;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.data.SmBlobs;
import com.ibm.dm.frontService.sm.utils.GraphicsTools;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

public class SmService extends SmServiceBase {

	//private final Map<String, String> params;

	public SmService(Database db) {
		super(db);
		//this.params = params;
		
//		this.registerRmpsService(Method.GET, "sm");
//		this.registerRmpsService(Method.GET, "sm", "oslc_am");
//		this.registerRmpsService(Method.GET, "sm", "tool", "*"); //$NON-NLS-1$
//		this.registerRmpsService(Method.GET, "sm", "tool"); //$NON-NLS-1$
//		this.registerRmpsService(Method.GET, "sm", "ontology"); //$NON-NLS-1$
//		this.registerRmpsService(Method.GET, "sm", "ruleset"); //$NON-NLS-1$
//		this.registerRmpsService(Method.GET, "sm", "*"); //$NON-NLS-1$
//
//		this.registerRmpsService(Method.POST, "sm", "repository", "*");
//		this.registerRmpsService(Method.POST, "sm", "repository", "*", "*");
//		this.registerRmpsService(Method.POST, "sm", "repository", "*", "*", "*");
//		this.registerRmpsService(Method.POST, "sm", "blobs", "*");
//		this.registerRmpsService(Method.POST, "sm", "blobs", "*", "*");
//		this.registerRmpsService(Method.POST, "sm", "blobs", "*", "*", "*");
//		this.registerRmpsService(Method.POST, "sm", "tool", "*"); //$NON-NLS-1$
//		this.registerRmpsService(Method.POST, "sm", "ruleset"); //$NON-NLS-1$
//		//this.registerRmpsService(Method.POST, "sm", "config"); //$NON-NLS-1$
//		this.registerRmpsService(Method.POST, "sm", "ontology"); //$NON-NLS-1$
//		this.registerRmpsService(Method.DELETE, "sm");
//		this.registerRmpsService(Method.DELETE, "sm", "ruleset"); //$NON-NLS-1$
//		this.registerRmpsService(Method.DELETE, "sm", "config"); //$NON-NLS-1$
//		this.registerRmpsService(Method.DELETE, "sm", "ontology"); //$NON-NLS-1$
//
//		// Repository management
//		this.registerRmpsService(Method.GET, "sm", "repository");
//		//OSLC
//		this.registerRmpsService(Method.GET, "sm", "repository", "*");
//		this.registerRmpsService(Method.GET, "sm", "repository", "*", "*");
//		this.registerRmpsService(Method.GET, "sm", "repository", "*", "*", "*");
//		this.registerRmpsService(Method.PUT, "sm", "repository", "*", "*", "*");
//		// Blobs
//		this.registerRmpsService(Method.GET, "sm", "blobs", "*");
//		this.registerRmpsService(Method.GET, "sm", "blobs", "*", "*");
//		this.registerRmpsService(Method.GET, "sm", "blobs", "*", "*", "*");
//		this.registerRmpsService(Method.PUT, "sm", "blobs", "*", "*", "*");
	}

	
	public void get(HttpServletRequest request, HttpServletResponse response, String segments[]) throws Exception
	{
		onEntry(request, response);
		boolean failed = false;
		String path = Utils.concat(segments, "/");
		String msg = "";
		try {
			if (path.equals("sm/oslc_am")) {
				String answer = "oslc_am is moved to another controller!!";
				System.err.println(answer);
				Utils.respondWithText(response, answer);
				return;
			}
			if (path.equals("sm/oslc_am")) {
				new OSLCHelper(getDatabase()).responseForServiceCatalog(path, segments, request, response, this);
				return;
			}
			if (1 == segments.length) { // && Utils.willAccept(ContentTypes.HTML, request)) {
				new SmManager(getDatabase()).doGet(request, response, this);
			} else if (segments.length > 1 && "tool".equalsIgnoreCase(segments[1])) {
				List<String> toolCommand = new ArrayList<String>();
				for (int i=2; i< segments.length; i++)
					toolCommand.add(segments[i]);
				SmContainer.getContainer(getDatabase()).doGet(request, response, toolCommand, this);
			} else if (2 == segments.length && "repository".equalsIgnoreCase(segments[1])) {
				RepositoryManagement.create(getDatabase()).doGet(request, response, this);
			} else if (segments.length > 1 && "repository".equalsIgnoreCase(segments[1])) {
				List<String> oslcCommand = new ArrayList<String>();
				for (int i=2; i< segments.length; i++)
					oslcCommand.add(segments[i]);
				SmContainer.getContainer(getDatabase()).doOslcGet(request, response, oslcCommand, this);
			} else if (segments.length > 1 && "blobs".equalsIgnoreCase(segments[1])) {
				SmBlobs.getBlobs(getDatabase()).doBlobGet(request, response, segments, this);
			} else if (segments.length > 1 && "ontology".equalsIgnoreCase(segments[1]) && "def".equalsIgnoreCase(segments[2])) {
				doOntologyGet(request, response, segments, this);
			} else
				throw new Exception("No service defined for this request");
			if (response.getStatus() != HttpStatus.SC_OK) {
				throw new Exception("Request failed."); //response.sendError(arg0);getStatusLine().getReasonPhrase());
			}

		} catch (Exception e) {
			e.printStackTrace();
			failed = true;
			msg = e.getMessage();
			throw e;
		} finally {
			onExit(request, response, failed, msg);
		}
	}
	/**
	 * Answer a click on the url of an ontology (or a rule) sccording to the accept type of the client.
	 * @param request
	 * @param response
	 * @param segments
	 * @param smService
	 * @throws Exception
	 */
	private void doOntologyGet(HttpServletRequest request, HttpServletResponse response, String[] segments,
			SmService smService) throws Exception {
		AModelRow ont = this.getDatabase().getOntologyByClassUri(request.getRequestURL().toString());
		if (null == ont)
			ont = this.getDatabase().getRulesByClassUri(request.getRequestURL().toString());
		if (null == ont)
			throw new Exception("No such ontology defined on this server. Request failed.");
			if (false == ont.isReady()) {
				throw new Exception("Ontology is in illegal state and cannot be served yet!");
			}
			byte contents[] = ont.getContentsAsBytes(IConstants.RDF_XML);
			if (null == contents) {
				throw new Exception("Failed to get Ontology content.");
			}
			String acceptType = Utils.willAcceptAny(IConstants.CONTENT_TYPES, request);
			if (null == acceptType) {
				if (Utils.willAccept("text/html", request)) {
					// answer with facade template for this ontology. It will than allow to do more views.
					JSONObject parms = new JSONObject();
					parms.put("id", ont.getId());
					parms.put("itemType", ont.getCollectionName().toUpperCase());
					parms.put("displayId", ont.getDisplayId());
					parms.put("namespace", ont.getModelInstanceNamespace());
					parms.put("rdf", Utils.forHtml(new String(contents)));
					parms.put("contentType", IConstants.RDF_XML);
					
					Utils.respondGetWithTemplateAndJson(parms, "getOntology", response, IConstants.HTML);
					return;
//					
//					String url = "http://ontorule-project.eu/parrot/parrot";
//					HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
//					con.setRequestMethod("POST");
//					con.setRequestProperty("User-Agent", "Mozilla/5.0");
//					con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
//					
//					String rdfContent = Utils.forHtml(new String(contents)).replaceAll(" ", "+");
//					String urlParameters = 
//							"mimetypeText=application/rdf&#43;xml&" +
//							"profile=technical&" +
//							"language=en&" +
//							"customizeCssUrl=&"+
//							"documentText=" + rdfContent;
//					con.setDoOutput(true);
//					DataOutputStream wr = new DataOutputStream(con.getOutputStream());
//					wr.writeBytes(urlParameters);
//					wr.flush();
//					wr.close();
//					int responseCode = con.getResponseCode();
//					System.out.println("\nSending 'POST' request to URL : " + url);
//					System.out.println("Post parameters : " + urlParameters);
//					System.out.println("Response Code : " + responseCode);
//					//responseCode=0; // for testing
//					if (responseCode != HttpStatus.SC_OK) {
//						String parot = "http://ontorule-project.eu/parrot/parrot";
//						Utils.respondWithText(response, "<html><body>Ontology RDF content of Ontology [" + ont.getDisplayId() + 
//								"] could not be converted to document with the <a href='" + parot + "'>" + parot + 
//								"</a> tool, and is displayed here in application/rdf+xml format.<br>" +
//								"<pre>" + Utils.forHtml(new String(contents)) + "</pre></body></html>");
//						return;
//					}
//					InputStream is = con.getInputStream();
//					String inputHtml = Utils.stringFromStream(is);
//					Utils.respondWithText(response, inputHtml);
//					return;
//					
					// Answer with the rdf.html template, than return 
				} 
				acceptType = Utils.willAcceptAny(new String[] {"image/jpeg", "image/png"}, request);
				if (null != acceptType) {
					GraphicsTools gt = new GraphicsTools(getDatabase());
					String dot = gt.createRepositoryGraph(ont.getId(), null, null, null);
					String type = acceptType.split("/")[1];
					String dotExe = Database.getDotExePath();
					byte[] answer = SmGraphService.get_img(dotExe, dot, type);
					if (null == answer) {
						type = "png";
						acceptType = "image/png";
						answer = Utils.bytesFromClassPath("templates/images/DOT.is.missing.png"); 
					}
					Utils.respondWithBytes(response, answer, acceptType);
					// Answer with the graphics image of the ontology, and return.
					return;
				}
				acceptType = IConstants.RDF_XML;
			}
			Utils.respondWithBytes(response, contents, acceptType);
			return;

		
	}


	/**
	 * Servicing a post of a file
	 * @param request
	 * @param response
	 * @param segments - of the path
	 * @param file - file content in multipart format
	 * @param params - other parameters of the post in a JSON structure.
	 * For ontologies, rulesets, etc, it will include [id];
	 * @throws Exception
	 */
	public void post(HttpServletRequest request, HttpServletResponse response, String segments[], MultipartFile file, JSONObject params) throws Exception
	{
		onEntry(request, response);
		boolean failed = false;
		String msg = "";

		InputStream is = null;
		if (!file.isEmpty())
			is = file.getInputStream();

		// look for segments[1] to be one of: "ontology", "rules", and "config", "tool/<tool>".
		System.out.println("segmens [" + segments.length + "]: " + Arrays.toString(segments));

		//HttpEntity entity = request.      //((BasicHttpEntityEnclosingRequest) request).getEntity();
		String contentType = request.getContentType(); //entity.getContentType().getValue();

		if (segments.length > 1 && "repository".equalsIgnoreCase(segments[1])) {
			if (segments.length > 2 && "attachments".equals(segments[2])) {
				try {
					SmBlobs.getBlobs(getDatabase()).createResource(request, response, contentType, file, params);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}


		if (segments.length > 1 &&
				("ontology".equalsIgnoreCase(segments[1]) ||
						"ruleset".equalsIgnoreCase(segments[1]))) {
			try {
				new SmManager(getDatabase()).loadModelFromFile(request, response, is, this, contentType);
			} catch (FileNotFoundException e) {
				failed = true;
				msg = e.getMessage();
				e.printStackTrace();
			} catch (IOException e) {
				failed = true;
				msg = e.getMessage();
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				failed = true;
				msg = e.getMessage();
				e.printStackTrace();
			} catch (Exception e) {
				failed = true;
				msg = e.getMessage();
				e.printStackTrace();
			} finally {
				onExit(request, response, failed, msg);
			}
			return;
		}

		
		if (segments.length > 3 && "tool".equalsIgnoreCase(segments[1]) && "sparql".equalsIgnoreCase(segments[3])) {
			Port port = getDatabase().getPort(WordUtils.capitalizeFully(segments[2]));
			if (null == port || ! port.isRepository())
				throw new Exception("Port [" + segments[2] + "] is either not existing, or not a repository. Cannot perform SPARQL on it.");
			String sparql = Utils.stringFromStream(request.getInputStream());
			Model model = port.getModel();
			Map<String, String> pxMap = Utils.fixNsPrefixes(model.getNsPrefixMap());
			String prefix = Utils.makePrefix4Query(pxMap);
			sparql = prefix + " " + sparql;
			System.out.println("Query: \n" + sparql);
			try {
				Query query = QueryFactory.create( sparql);
				if (false == Utils.willAccept("application/json", request))
					throw new Exception("Can answer sparql with JSON data only. Accept = [" + request.getHeader("Accept") + "].");
				QueryExecution qexec = QueryExecutionFactory.create ( query, model ) ;
				ResultSet results = qexec.execSelect();
				JSONArray rows = new JSONArray();
				List<String> vars = results.getResultVars();
				while (results.hasNext()) {
					JSONObject row = new JSONObject();
					QuerySolution soltn = results.next();
					for (String title : vars) {
						RDFNode node = (RDFNode) soltn.get(title);
						row.put(title, Utils.prefixForResource(node.toString(), pxMap));
					}
						rows.put(row);
				}
				JSONObject answer = new JSONObject();
				answer.put("query", sparql );
				answer.put("rows",  rows);
				Utils.respondWithJson(response, answer);
			} catch (Exception e) {
				System.err.println("Exception [" + e.getClass().getName() + "]: " + e.getMessage());
				failed = true;
				msg = e.getMessage();
				JSONObject answer = new JSONObject();
				answer.put("query", sparql );
				answer.put("error", msg);
				Utils.respondWithJson(response, answer);
			} finally {
				onExit(request, response, failed, msg);
			}
			return;
		}
	
		if (segments.length > 1 && "tool".equalsIgnoreCase(segments[1])) {
			List<String> toolCommand = new ArrayList<String>();
			for (int i=2; i< segments.length; i++)
				toolCommand.add(segments[i]);
			try {
				SmContainer.getContainer(getDatabase()).doPost(request, response, toolCommand, is, contentType, this);
			} catch (Exception e) {
				System.err.println("Exception [" + e.getClass().getName() + "]: " + e.getMessage());
				failed = true;
				msg = e.getMessage();
			} finally {
				onExit(request, response, failed, msg);
			}
			return;
		}
	}


	public void put(String rdf, HttpServletRequest request, HttpServletResponse response, String segments[]) throws Exception
	{
//		onEntry(request, response);
//		boolean failed = false;
//		String msg = "";
//		try {

			System.out.println("segments [" + segments.length + "]: " + Arrays.toString(segments));


//			HttpEntity entity = ((BasicHttpEntityEnclosingRequest) request).getEntity();
			String contentType = request.getContentType(); //entity.getContentType().getValue();
			//InputStream is = request.getInputStream();
//			try {
//				is = entity.getContent();
//			} catch (IllegalStateException e) {
//				failed = true;
//				e.printStackTrace();
//			} catch (IOException e) {
//				failed = true;
//				e.printStackTrace();
//			} finally {
//					if (failed) {
//						onExit(request, response, failed);
//						return;
//					}
//			}
			if (segments.length > 1 && "repository".equalsIgnoreCase(segments[1])) {
				List<String> oslcCommand = new ArrayList<String>();
				for (int i=2; i< segments.length; i++)
					oslcCommand.add(segments[i]);
//				try {
					SmContainer.getContainer(getDatabase()).doPut(request, response, oslcCommand, rdf, Utils.normalizeContentType(contentType), this);
//				} catch (Exception e) {
//					failed = true;
//				} finally {
//					onExit(request, response, failed);
//				}
				return;
			}
//		} catch (Exception e) {
//			throw
//		} finally {
//			onExit(request, response, failed);
//		}

	}

	protected void delete(HttpServletRequest request, HttpServletResponse response, String segments[]) throws Exception {
		// TODO Auto-generated method stub
		//super.rmpsDelete(request, response);
		// Handle deletion of a ontology into the /sm/ontology API
		// Handle deletion of a transformation rules into the /sm/rules API
		// Handle deletion of a configuration into the /sm/config API
	}


/*	public String getParam(HttpRequest request, String param) {
		return this.params.get(param);
				
	}
*/
}


