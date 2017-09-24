
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
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 *
 */

package com.ibm.dm.frontService.sm.service;

//import net.openservices.rio.store.RioServerException;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.jena.rdf.model.Model;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.utils.GraphicsTools;
import com.ibm.dm.frontService.sm.utils.Utils;

public class SmGraphService  extends SmService {


	public SmGraphService(Database db) {
		super(db);
	}

	static private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	static private Transformer transformer;
	static { 
		try {
			factory.setNamespaceAware(false);
			factory.setValidating(false);
			factory.setFeature("http://xml.org/sax/features/namespaces", false);
			factory.setFeature("http://xml.org/sax/features/validation", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			transformer = TransformerFactory.newInstance().newTransformer();
		    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		    transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
		    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Serves lots of possibilities, including operational, ontologies, and repositories. Cannot provice
	 * url mapping, but for repositories having an id in the query line.
	 * <br>
	 * TODO: Add also same service for operational - so we can find urls of repositories to see their 
	 * contents, and to copy their links directly from the graph. 
	 * Presently, the graph already contains the URL= and TITLE= Graphviz DOT tags to make this possible.
	 * Only need to handle that also from the request button, and complement it here, or somewhere else 
	 * on the the stack above.
	 */
	public void get(HttpServletRequest request, HttpServletResponse response, String segments[]) throws Exception
	{
		
		onEntry(request, response);
		boolean failed = false;
		String msg = "";
		boolean ontologyGraph = false;
		boolean operationalGraph = false;
		boolean repositoryGraph = false;
		String id = getParam(request, "id");
		String purpose = "none";
		String query = getParam(request, "query");
		String resource = getParam(request, "resource");
		String version = getParam(request, "version");
		String doimg = getParam(request, "img"); // provide in return an svg image text.
		String domap = getParam(request, "map"); // provide in reeturn a map text.
		String dosrc = getParam(request, "src"); // provide in return an html that will ask for svg and map.
		String doFrom = getParam(request, "from"); // provide in return an html that will ask for svg and map.
		if (null != getParam(request, "OntologiesGraph")) {
			ontologyGraph = true;
			purpose = "Ontology";
		} else if (null != getParam(request, "OperationGraph")) {
			operationalGraph = true;
			purpose = "Operational";
		} else if (null != id) {
			repositoryGraph = true;
			purpose = "Model graph for [" + id + "]";
			if (false == Strings.isNullOrEmpty(version))
				purpose += ", for Version [" + version + "].";
		}

		File pngFile = null;
		try {
			GraphicsTools gt = new GraphicsTools(getDatabase());
			JSONObject contents = new JSONObject();
			contents.put("purpose", purpose);
			String graph = "No graph available";
			String dot = ""; //, dot2 = null;
			Model model = null;
			if (ontologyGraph) {
				model = gt.createOntologiesGraph(getDatabase().getFilter());
				graph = Utils.modelToText(model, null);
				contents.put("graph", graph);
				dot = gt.createDotFromModel(model);
			} else if (operationalGraph) {
				model = gt.createOperationalGraph(getDatabase().getFilter());
				graph = Utils.modelToText(model, null);
				contents.put("graph", graph);
				dot = gt.createDotFromModel(model);
			} else if (repositoryGraph) {
				dot = gt.createRepositoryGraph(id, resource, version, query);
			}
			if (null != dot)
				System.out.println(dot);
			if (false == Strings.isNullOrEmpty(dot)) {
				String dotExe = Database.getDotExePath(); //System.getenv("DOT");
				if (Strings.isNullOrEmpty(dotExe)){
					String m = "Server should install Graphviz, having the 'dot' command executable,\n"+
				"or to include in the application WAR also the executable under the Graphviz/ folder.\n" + 
				"So Graphviz/dot or Graphviz/dot.sh or Graphviz/dot.exe or Graphviz/dot.bat exist and are executable.\n" +
				"Graphviz can be obtain from http://www.graphviz.org.\n";
					System.err.println(m);
					msg += "NOTE THIS: Graphviz not properly installed, using RDF validator instead:\n" + m + "\n";
					dotExe = "dot";
				}
				if (false == Strings.isNullOrEmpty(dotExe)) {
					String type = "png", mediaType = "image/png";
					if (null != domap || null != dosrc) {
						type = "cmapx";
						mediaType = "text/plain";
					} else if (null != doimg) {
						type = "svg";
						mediaType = "image/svg+xml";
					}
					byte answer[] = get_img(dotExe, dot, type);
					if (null != doFrom && type.equals("svg")) { // work out animation
					}
					if (null == answer && type.equals("png")) {
						//Send image of an error message
						byte image[] = Utils.bytesFromClassPath("templates/images/DOT.is.missing.png"); 
						Utils.respondWithBytes(response, image, mediaType);
						return;
					} else if (null != answer) {
						if (type.equals("png"))
							Utils.respondWithBytes(response, answer, mediaType);
						else {
							String content = new String(answer);
							if (null != domap) {
								// remove top and bottom lines of the map
								if (content.indexOf("\n") >= 0)
									content = content.substring(content.indexOf("\n"));
								if (content.lastIndexOf("\n") >= 0)
									content = content.substring(0, content.lastIndexOf("\n"));
								if (content.lastIndexOf("\n") >= 0)
									content = content.substring(0, content.lastIndexOf("\n"));
							} else if (null != dosrc) {
								String from = "";
								if (null != doFrom)
									from = "&from=" + doFrom;
								content = "<html>\n" +
									"<body>\n" +
										"<h1>Graphical berowsing of items in repository [" + id + "]</h1>" +
										"<font size='+2'><table><tr><td><form action='/dm/sm'>\n" +
										"<button type='submit'>Done</button>\n" +
										"</form>" +
										"<td><form action='/dm/sm'>\n" +
										"<button type='submit'>List View</button>\n" +
										"<input hidden name='id' value='" + id + "'>\n" + 
										"<input hidden name='action' value='showList'>\n" +
										"</form><td>" +
										"</table></font>\n" +
										" Click on selectable nodes to move through the graph.<br>" +
									"<IMG SRC='/dm/smGraph?img&amp;id=" + id + "&amp;resource=" + resource + from + "&amp;action=showGraph' USEMAP='#G' />" + 
								content + "</body></html>";
								mediaType = "text/html";
							}
							Utils.respondWithText(response, content, mediaType);
						}
						System.err.println("[" + dotExe + "] seems to work for the graphviz tool here! Great!");
						return;
					}
					String m = "Executing graphviz with dot [" + dotExe + "] failed.";
					System.err.println(m);
					msg += m;
				}
			}
			contents.put("dot-message", msg); //Utils.concat(msg.split("\n"), "<br>"));
			String template = "templates/graph.html";
			if (repositoryGraph)
				template = "templates/graph_dot.html";
//			if (Utils.willAccept(ContentTypes.HTML, request)
					Utils.respondGetWithTemplateAndJson(contents, template, response);
//			if (Utils.willAccept(ContentTypes.IMAGE_ANY, request))
//				Utils.respondWithBytes(response, contents, type)
		} catch (Exception e) {
			failed = true;
			e.printStackTrace();
		} finally {
			onExit(request, response, failed, msg);
			if (null != pngFile && pngFile.canRead())
				try {
					pngFile.delete();
				} catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	/**
	 * Answers with the html page in which an image with a map
	 * are embedded, where the name of the image file is embedded also in that page.
	 * @param DOT
	 * @param dot
	 * @param type
	 * @return
	 */
	public static byte[] get_img(String DOT, String dot, String type) {
		File outFile = null, dotFile = null;
		
		try {
			outFile = File.createTempFile(type, "." + type);
			dotFile = File.createTempFile("dot", ".dot");
			Utils.fileFromBytes(dotFile, dot.getBytes());

			Runtime rt = Runtime.getRuntime();
			String[] args = {DOT, "-T"+type, "-o"+outFile.getAbsolutePath(), 
					dotFile.getAbsolutePath()};
			System.err.println("Executing dot: " + Utils.concat(args,  " "));
			Process p = rt.exec(args);

			p.waitFor();

			if (outFile.canRead() && outFile.length() > 10) {
//				String result = "<html><img src='/dm/smGraph?imageName=" + outFile.getName() + "' USEMAP='#G'>" +
				return Utils.bytesFromFile(outFile);
			}
		}
		catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		}
		catch (java.lang.InterruptedException ie) {
			ie.printStackTrace();
		}
		finally {
			if (null != dotFile && dotFile.canRead()) {
				try {
					dotFile.delete();
				} catch (Exception e) { e.printStackTrace(); }
			}
			if (null != outFile && outFile.canRead()) {
				try {
					outFile.delete();
				} catch (Exception e) { e.printStackTrace(); }
			}
		}
		return new byte[0];
	}

}


