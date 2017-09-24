
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

package com.ibm.dm.frontService.sm.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.ADatabaseRow;
import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.data.DataBaseData.EClassification;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Mediator;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.data.RuleSet;
import com.ibm.dm.frontService.sm.data.RuleSet.END_POINT;
import com.ibm.dm.frontService.sm.intfc.imp.OntologyDescription;
import com.ibm.dm.frontService.sm.service.ARdfRepository;
import com.ibm.dm.frontService.sm.service.SmContainer;

public class GraphicsTools {

	static Property defines;
	static Property exports;
	static Property imports;
	static Property depends;
	static Property direct;
	static Property input;
	static Property output;
	static Resource top;

	private final Database mOwner;
	
	public GraphicsTools(Database owner) {
//		if (null == owner)
//			owner = Database.create();
		mOwner = owner;
	}
	
	public Database getDatabase() {
		return mOwner;
	}
	
	static {
		Model model = ModelFactory.createDefaultModel();
		defines = model.createProperty("http://L/defines");
		exports = model.createProperty("http://L/exports");
		imports = model.createProperty("http://L/imports");
		depends = model.createProperty("http://L/depends");
		direct = model.createProperty("http://L/both");
		input = model.createProperty("http://L/inputTo");
		output = model.createProperty("http://L/outputTo");
		top = model.createResource("http://L/Configuration");
	}
	
	/**
	 * Generates an ontological dependency tree graph model for the platform, Choosing only elements
	 * according to the filter.
	 * @param filter csv of tags to filter items by
	 * @return Model which models the graph of mediations.
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws ClassNotFoundException
	 */
	public Model createOntologiesGraph(String filter) throws IOException, ClassNotFoundException {
		if (null == filter)
			filter = "";
		Model model = ModelFactory.createDefaultModel();
		Database db = getDatabase();
		List<Ontology> os = db.getOntologies();
		List<RuleSet> rs = db.getRules();
		//List<AModelRow> ms = new ArrayList<AModelRow>();
		// ms.addAll(os); ms.addAll(rs);
		Set<String> marked = new HashSet<String>();
		Set<String> used = new HashSet<String>();	
		for (Ontology m : os) {
			if (false == m.isReady()) 
				continue;
			OntologyDescription od = m.getOntologyDescription();
			if (null == od)
				continue;
			String b = od.getBase();
			marked.add(b);
			String imnports[] = od.getImports();
			for (String anImport : imnports) {
				model.add(model.createResource(b), depends, model.createResource(anImport));
				used.add(anImport);
			}
		}
		for (RuleSet m : rs) {
			if (false == m.isReady()) 
				continue;
			OntologyDescription od = m.getOntologyDescription();
			if (null == od)
				continue;
			String b = od.getBase();
			Ontology o1 = m.getEndPointOntology(END_POINT.FIRST);
			Ontology o2 = m.getEndPointOntology(END_POINT.SECOND);
			String b1 = null, b2 = null;
			if (o1.isReady() && o2.isReady()) {
				OntologyDescription od1 = o1.getOntologyDescription(),
									od2 = o2.getOntologyDescription();
				if (null != od1 && null != od2) {
					b1 = od1.getBase();
					b2 = od2.getBase();
				}
			}
			boolean reversible = m.isEndPointReversible();
			if (null != b1 && null != b2) {
				model.add(model.createResource(b1), model.createProperty(Utils.trimOptional(b, "#")), model.createResource(b2));
				if (reversible)
					model.add(model.createResource(b2), model.createProperty(Utils.trimOptional(b, "#")), model.createResource(b1));
			}
	
		}
		for (String string : marked) {
			if (used.contains(string))
				continue;
			model.add(top, defines, model.createResource(string));
		}
		
		return model;
//		StringWriter sw = new StringWriter();
//		model.write(sw);
//		return sw.toString();
	}
	
	public String createDotFromRepository(Model model, String base, String resource) throws IOException, ClassNotFoundException {
		Property URLprop = model.createProperty("http://URL");
		Property TITLEprop = model.createProperty("http://TITLE");
		//		StringBuffer sb = new StringBuffer("digraph G {\nrankdir=LR;\n");
		StringBuffer sb = new StringBuffer("digraph G {\nrankdir=LR;\n node [style=\"rounded,filled\" color=\"dodgerblue\" fillcolor=\"lightyellow\"  gradientangle=45]\n edge [color=blue]\n");
		if (null != resource && false == resource.contains(",")) {
			String num = resource;
			if (num.contains(":"))
				num = num.substring(num.indexOf(":")+1);
			sb.append("id=\"" + num + "\";\n");
		}
		ResIterator resources = model.listSubjects();
		Map<String, String> map = model.getNsPrefixMap();
		Property titleP = DCTerms.title;
		Property labelP = RDFS.label;
		Property typeP = RDF.type;
		if (null == base)
			base = map.get("base");
		String basePrefix = "base:";
		String prfx = Utils.prefixForResource(base, map);
		if (false == Strings.isNullOrEmpty(prfx))
			basePrefix = prfx;
		if (Strings.isNullOrEmpty(base))
			return sb.append("Unrecognized Base }").toString();
		Set<String> subjects = new HashSet<String>();
		while (resources.hasNext())
			subjects.add(resources.next().toString());
		resources = model.listSubjects();
		Map<String, String> temps = new HashMap<String, String>(); // holds an alternative prefixes of temp resources.
		Property hasTag = model.createProperty(IConstants.SM_PROPERTY_SM_TAG_FULL);
		Resource isDeleted = model.createResource(IConstants.SM_PROPERTY_RESOURCE_DELETED_FULL);
		while (resources.hasNext()) {
			String shape = "";
			String links = "";
			Resource r = resources.next();
			boolean r_isDeleted = Utils.resourceHasPropertyValue(r, hasTag, isDeleted);
			Statement stmt = r.getProperty(URLprop);
			String URL = "";
			if (null != stmt) {
				URL = stmt.getObject().toString();
			}
			stmt = r.getProperty(TITLEprop);
			String TITLE = "";
			if (null != stmt) {
				TITLE = stmt.getObject().toString();
			}
			StmtIterator si = r.listProperties(typeP);
			String type = "";
			while (si.hasNext()) {
				Statement s = si.next();
				type = "|"+Utils.applyPrefixesOnResources(map, s.getObject().toString());
			}
			
			// special treatment to the commonly used display names of a resource: dcterms:title, and rdfs:label.
			Statement s = r.getProperty(titleP);
			if (null == s)
				s = r.getProperty(labelP);
			String title = "";
			if (null != s) 
				title = "|"+s.getObject().toString();
			String id = Utils.applyPrefixesOnResources(map, r.toString()).trim();
			StmtIterator props = r.listProperties();
//			if (id.endsWith(":")) // ensure that a case like 'base:' is still split to at least two cells.
//				id += "
			int pos = -1;
			if (false == id.equals(r.toString())) {
				pos = id.lastIndexOf(":");
			}
			String name = id;
			if (pos >= 0) {
				String pref = id.substring(0, pos);
				name = id.substring(pos+1);
				if (null == map.get(pref)) {
					if (temps.get(pref) == null)
						temps.put(pref, "other" + temps.size());
					pref = temps.get(pref);
				}
				pref += ":";
				if (false == basePrefix.equals(pref) || name.isEmpty())
					name = pref + name;
				if (false == Strings.isNullOrEmpty(URL))
					URL = "URL=\""+ URL + "\"";
				if (false == Strings.isNullOrEmpty(TITLE))
					TITLE = "TITLE=\""+ TITLE + "\"";
			}
			//String ids[] = id.split(":"); // prepare to pick the last part of the url for the id.
			String style = "";
			if (r_isDeleted)
				style = "fontcolor=gray,style=dashed,";
			
			shape += "\"" + id + "\" [" + style + URL + TITLE + "shape=record,label=\"{{{" + name + title + type + "}";
			while (props.hasNext()) {
				s = props.next();
				Property p = s.getPredicate();
				if (p.equals(URLprop) || p.equals(TITLEprop) || p.equals(hasTag))
					continue;

				String p_s = Utils.applyPrefixesOnResources(map, p.toString());
				if (p.equals(typeP) || p.equals(titleP) || p.equals(labelP)) // all these properties are treated already.
					continue;
				RDFNode v = s.getObject();
				String v_s = v.toString();
				// Fix the object in case it is a literal so { and } are escaped.
				if (v.isLiteral())
					v_s = Utils.replaceAll(Utils.replaceAll(Utils.replaceAll(Utils.replaceAll(Utils.replaceAll(Utils.replaceAll(v_s, "{", "\\{"), 
							"}", "\\}"), 
							">", "\\>"),
							"<", "\\<"),
							"|", "\\|"),
							"\"", "\\\"");
				if (v instanceof Resource) {
					Resource v_r = (Resource)v;
					boolean v_r_isDeleted = Utils.resourceHasPropertyValue(v_r, hasTag, isDeleted);
					String v_id =  Utils.applyPrefixesOnResources(map, v_r.toString()).trim();
					style = "";
					if (v_r_isDeleted)
						style = "fontcolor=gray,style=dashed,";
					if (subjects.contains(v_r.toString())) {
						// this will be a link
						links += "\"" + id + "\" -> \"" + v_id + "\" [" + style + "label=\"" +
						   p_s + "\"];\n";
						continue;
					} else {
						v_s = v_id;
					}
				} else {
					if (v_s.length() > 20)
						v_s = breakUp2(v_s, 20, "\\l") + "\\l";
				}
				shape += "|{" + p_s + "|" + v_s + "}";
				
			}
			sb.append(shape).append("}}\"];\n").append(links);
		}
		return sb.append("\n}").toString();
	}
	/**
	 * Answers with a String of the DOT format of a graph visualization for Graphvis, 
	 * based on the Model input
	 * @param model A Model of the graph to be converted to DOT format.
	 * @return String of the DOT specification of the graph to be visualized.
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	public String createDotFromModel(Model model) throws IOException, ClassNotFoundException {
		StringBuffer sb = new StringBuffer("digraph G {\nrankdir=LR;\n node [style=\"rounded,filled\" color=\"dodgerblue\" fillcolor=\"yellow;0.01:lightblue\"  gradientangle=45]\n edge [color=blue]\n");
		ResIterator resources = model.listSubjects();
		Map<Resource, String> nodes = new HashMap<Resource, String>();
		Set<Resource> restPorts = new HashSet<Resource>();
		Set<Resource> tools = new HashSet<Resource>();
		if (false == resources.hasNext())
			sb.append("\"No Resources to Draw\"\n");
		Resource isDeletedTag = model.getResource(IConstants.SM_PROPERTY_RESOURCE_DELETED_FULL);
		Property hasTag = model.getProperty(IConstants.SM_PROPERTY_SM_TAG_FULL);
		while (resources.hasNext()) {
			Resource r = resources.next();
			StmtIterator stmts = model.listStatements(r, null, (RDFNode)null);
			boolean resourceIsDeleted = Utils.resourceHasPropertyValue(r, hasTag, isDeletedTag);
			while (stmts.hasNext()) {
				Statement stmt = stmts.next();
				Property p = stmt.getPredicate();
				RDFNode o = stmt.getObject();
				Resource s = stmt.getSubject();
				boolean objectIsDeleted = o.isResource()?
						Utils.resourceHasPropertyValue(o.asResource(), hasTag, isDeletedTag): false;
			    String lineStyle = "";
			    if (resourceIsDeleted || objectIsDeleted) 
			    	lineStyle = "[style=dashed]";
				sb.append("\"" + id(s)).append("\"")
//				.append(nodeStyle)
				.append(" -> \"")
				.append(lineStyle)
				.append(id(o)).append("\" ")
				.append(label(p, p));
				String sType = s.toString().split(":")[0];
				String oType = o.toString().split(":")[0];
				if (sType.equals("get") || sType.equals("post"))
					restPorts.add(s);
				if (oType.equals("get") || oType.equals("post"))
					restPorts.add((Resource)o);
				if (oType.equals("repository") && (p.equals(direct) || p.equals(imports) | p.equals(exports)))
					restPorts.add((Resource)o);
				if (o instanceof Resource)
					nodes.put((Resource)o, label(o, p, restPorts.contains(o)));
			}
			if (nodes.keySet().contains(r))
				continue;
			nodes.put(r, label(r, null));
			if (r.toString().startsWith("tool:"))
				   tools.add(r);
		}
		if (nodes.size() > 0) {
			for (Resource r: nodes.keySet()) {
				String label = nodes.get(r);
				
				if (Utils.resourceHasPropertyValue(r, hasTag, isDeletedTag)) {
					// insert more properties into the beginning of this label, which has the format '[...]'
					label = "[fontcolor=gray, style=dashed, " + label.substring(1);
				}
				sb.append("\"").append(id(r)).append("\" ").append(label);
			}
		}
		if (restPorts.size() > 0) {
			sb.append("  { rank=same; ");
			for (Resource r: restPorts) {
				sb.append("\"").append(id(r)).append("\" ");
			}
			sb.append(" }\n");
		}
		if (tools.size() > 0) {
			sb.append("  { rank=source; ");
			for (Resource r: tools) {
				sb.append("\"").append(id(r)).append("\" ");
			}
			sb.append(" }\n");
		}
		
		return sb.append("}").toString();
	}
	
	private static final String prefix = "http://L/";
	private static final String id(RDFNode n) {
		String ns = n.toString();
		if (ns.indexOf(':') < 0 || ns.startsWith("http"))
			return ns;
		return ns.split("/")[2];
	}
	static Map<String, String> labels = new HashMap<String, String>();
	static {
		labels.put("mediator", "MEDIATOR");
		labels.put("mediator2", "MEDIATOR");
		labels.put("get", "GET");
		labels.put("post", "POST");
		labels.put("tool", "TOOL");
		labels.put("repository", "REP");
		labels.put("pipe", "PIPE");		
	}
	private static String fixLabel(String s) {
		int pnt = s.indexOf(":");
		if (pnt < 0)
			return s;
		String l = s.substring(0,pnt);
		if (Strings.isNullOrEmpty(labels.get(l)))
			return s;
		return labels.get(l) + s.substring(pnt);
	}
	private String label(RDFNode n,  Property p, boolean...isRest) throws FileNotFoundException, IOException, ClassNotFoundException {
		String s = n.toString();
		String result = "";
		if (s.startsWith("http://L/")) {
			result = "label=\"" + s.substring(prefix.length()) + "\"";
			if (imports.equals(p))
				result +=",dir=back";
			else if (direct.equals(p))
				result +=",dir=both,style=bold";
			else if (defines.equals(p))
				result +=",dir=none,style=dashed";
			else if (input.equals(p))
				result +=",dir=back";
			else if (n.equals(top)) 
				result += ",shape=star";
			else if (output.equals(p))
				;//result +=",dir=back"; nothing to do, forward arrow.
			else if (exports.equals(p)) 
				; // result += ",shape=star"; nothing to do, forward arrow.
			else if (imports.equals(p)) 
				result += ",dir=back";
			else
				result += ",dir=none";

		} else if (s.startsWith("get:") || s.startsWith("post:") || s.startsWith("repository:") || s.startsWith("tool:")) {
			int pnt = s.lastIndexOf("/");
			String accessName = s.substring(pnt + 1);
			result += s.substring(0, pnt);
			String URL = null;
			if (Utils.isOptional(false, isRest)) {
				URL = "/dm/sm/tool/" + accessName;
				result += "\\n\\[REST=" + URL + "\\]";
			}
			String id = s.split("/")[2];
			Port m = getDatabase().getPort(id);
			Ontology ont = null;
			if (null != m)
				ont = m.getOntology();
			if (null != ont) {
				result += "\\n\\[ontology=" + ont.getDisplayId() + "\\]";
			}

			result += "\"";
			result = "label=\"" + fixLabel(result);
			if (s.startsWith("repository:")) {
				result += ",peripheries=2";
				if (null != URL) 
					result += ",URL=\"" + URL + "\",TITLE=\"Click to see RDF content of the repository.\\nRight click to get link to export or import with tools.\"";
				
			} else if (s.startsWith("tool:")) {
				result += ",shape=box";
				if (null != ont)
					result += ",peripheries=2";
			}
		} else if (s.startsWith("mediator:") || s.startsWith("mediator2:")) {
			String id = s.split("/")[2];
			Mediator m = getDatabase().getMediator(id);
			String intName = "";
			if (null != m) {
				intName = "\\n\\[" + m.getInterceptorName();
				RuleSet r = getDatabase().getRuleSet(m.getRuleSetId());
				if (null != r)
					intName += "; rule=" + r.getDisplayId();
				intName += "\\]";
			}
			result = "label=\"" + fixLabel(s) + intName + "\"";
		} else {
			result = "label=\"" + fixLabel(s) + "\"";
			if (s.startsWith("tool:")) {
				result += ",shape=box";
			}
		}
		if (s.startsWith("mediator2:"))
			result += ",dir=both";
		return "[ " + result + "]\n";			
	}

//	private static void defineGraphNode(Resource r, StringBuffer sb) {
//		if (r.toString().startsWith("tool:")) {
//			sb.append("\"").append(r.toString()).append("\" [shape=box];\n");
//		} else if (r.toString().startsWith("repository:")) {
//			sb.append("\"").append(r.toString()).append("\" [peripheries=2];\n");
//		} else if (r.equals(top)) {
//			sb.append("\"").append(r.toString()).append("\" [shape=star];\n");
//		}
//	}

	/**
	 * Generates an opetational graph model for the mediation flows in the platform.
	 * @param filter csv of tags to filter items by
	 * @return Model which models the graph of flows.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public Model createOperationalGraph(String filter) throws IOException, ClassNotFoundException {
		String tags[] = null;
		if (false == Strings.isNullOrEmpty(filter)) 
				tags = Utils.csv2array(filter, ",", true);
		Model model = ModelFactory.createDefaultModel();
		Database db = getDatabase();
		List<Port> ports = (List<Port>)(List<?>)Utils.filter((List<ADatabaseRow>)(List<?>)db.getPorts(), tags);
		List<Mediator> mediators = (List<Mediator>)(List<?>)Utils.filter((List<ADatabaseRow>)(List<?>)db.getMediators(), tags);
		Set<String> posters = new HashSet<String>();
		Set<String> getters = new HashSet<String>();
//		Set<String> used = new HashSet<String>();
		for (Port p : ports) {
			if (false == p.isReady())
				continue;
			Ontology toolOntology = p.getOntology();
			if ((p.canGet() || p.canPost()) && !p.isTool()) {
				Property relation = direct;
				Resource toolR2 = null;
				Port tool = null;
				if (p.isRepository() && ! p.isTool()) {
					List<Port> tools = p.myTools();
					if (tools.size() > 0) {
						tool = tools.get(0);
						toolR2 = model.createResource("tool://" + tool.getId() + makeName(tool.getName()) + "/" + tool.getAccessName());
					}
				}
				if (p.canGet()) { 
					getters.add(p.getId());
					if (null != tool) 
						getters.add(tool.getId());
					relation = imports;
				}
				if (p.canPost()) { 
					posters.add(p.getId()); 
					if (null != tool) 
						posters.add(tool.getId());
					relation = exports;
				}
				if (p.canGet() && p.canPost())
					relation = direct;
				Resource toolR = model.createResource("tool://" + p.getOntologyId().substring(4) + makeName(toolOntology.getName()));
				Resource port = model.createResource(p.getType() + "://" + p.getId() + makeName(p.getName()) + "/" + p.getAccessName());
				// Create default tool relation representing the ontology
				model.add(toolR, relation, port);
				// Create actual existing relation with a real tool item.
				if (null != toolR2)
					model.add(toolR2, relation, port);					
			}
		}
		for (Mediator m : mediators) {
			if (false == m.isReady())
					continue;
				RuleSet rs = this.getDatabase().getRuleSet(m.getRuleSetId());
				boolean reversible = null!= rs && rs.isEndPointReversible();
				Property mr = model.createProperty("mediator" + (reversible?"2":"") + "://" + m.getId() + makeName(m.getName()));
				Port p = db.getPort(m.getInputPortId());
				Resource ir = model.createResource(p.getType() + "://" + p.getId() + makeName(p.getName()) + "/" + p.getAccessName());
				p = db.getPort(m.getOutputPortId());
				Resource or = model.createResource(p.getType() + "://" + p.getId() + makeName(p.getName()) + "/" + p.getAccessName());
				;
				model.add(ir, mr, or); 
			}
			return model;
		}



		/**
		 * Answers with a List of items for a specific classification (ontologies, rules, etc.), and a filter.
	 * @param filter csv of tags to filter items by
	 * @param classification {@link EClassification} to specify which type of items are needed.
	 * @return List of {@link ADatabaseRow} that match the parameters.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<ADatabaseRow> getItems(String filter, EClassification classification) throws FileNotFoundException, IOException, ClassNotFoundException {
		if (null == filter)
			filter = "";
//		String tags[] = Utils.csv2array(filter, ",", true);
		Database db = getDatabase();
		List<ADatabaseRow> items = db.getItems(classification);
//		List<Port> ports = (List<Port>)(List<?>)Utils.filter((List<ADatabaseRow>)(List<?>)db.getPorts(), tags);
		return items;

	}
	
	/**
	 * Answers with a list of all the ontologies for which tools operate with the SMC
	 * @param filter csv of tags to filter items by
	 * @return List of ids of ontologies.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public List<String> getTools(String filter) throws FileNotFoundException, IOException, ClassNotFoundException {
		List<Port> ports = (List<Port>)(List<?>)getItems(filter, EClassification.ports);
		List<String> result = new ArrayList<String>();
		for (Port port : ports) {
			if (false == port.isReady()) 
				continue;
			if (port.canGet() || port.canPost()) { //getType() == Port.PortType.post || port.getType() == Port.PortType.get ||
//					(port.isDirect())) {
				result.add(port.getOntologyId());
			}
		}
		return result;
	}
	
	/**
	 * Answers with a string of a filled template for the tools part of a tree visualization
	 * of the operational network, for a given tags filter
	 * @param filter csv of tags to filter by.
	 * @return String for the first level of a tree view - the tools.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws JSONException 
	 */
	public String getToolsUL(String filter) throws FileNotFoundException, IOException, ClassNotFoundException, JSONException {
		List<String> ontIds = getTools(filter);
		JSONArray tools = new JSONArray();
		JSONObject contents = new JSONObject();
		contents.put("tools", tools);
		for (String id : ontIds) {
			Ontology o = getDatabase().getOntology(id);
			JSONObject tool = new JSONObject();
			tools.put(tool);
			tool.put("name", o.getName());
			tool.put("ontologyId", id);
		}
		String result = Utils.mergeJsonWithHtml(Utils.getHtmlTemplate("tree#tools"), contents);
		return result;
	}
	
	/**
	 * Answers with a string of a filled template for the rest api-s part of a tree visualization
	 * of the operational network, for a given tags filter
	 * @param ns - namespace of an ontology of a tool and its rest ports.
	 * @param filter csv of tags to filter by.
	 * @return String for the first level of a tree view - the tools.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws JSONException 
	 */
	public String getRestPortsUL(String ns, String filter) throws FileNotFoundException, IOException, ClassNotFoundException, JSONException {
		@SuppressWarnings("unchecked")
		List<Port> ports = (List<Port>)(List<?>)getItems(filter, EClassification.ports);
		JSONArray restPorts = new JSONArray();
		JSONObject contents = new JSONObject();
		contents.put("ports", restPorts);
		for (Port port : ports) {
			JSONObject p = new JSONObject();
			restPorts.put(p);
			p.put("name", port.getName());
			p.put("accessName", port.getAccessName());
			p.put("type", port.getType());
		}
		String result = Utils.mergeJsonWithHtml(Utils.getHtmlTemplate("tree#rest"), contents);
		return result;
	}
	
	/**
	 * Answers with a string of a filled template for the rest api-s part of a tree visualization
	 * of the operational network, for a given tags filter
	 * @param ns - namespace of an ontology of a tool and its rest ports.
	 * @param filter csv of tags to filter by.
	 * @param fromPortId String id of a port from which mediators are pursued.
	 * @return String for the first level of a tree view - the tools.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws JSONException 
	 */
	@SuppressWarnings("unchecked")
	public  String getMeditorsUL(String ns, String filter, String fromPortId) throws FileNotFoundException, IOException, ClassNotFoundException, JSONException {
//		List<Port> ports = (List<Port>)(List<?>)getItems(filter, EClassification.ports);
		List<Mediator> mediators = (List<Mediator>)(List<?>)getItems(filter, EClassification.mediators);
//		Port fromPort = getDatabase().getPort(fromPortId);
		JSONArray throughPorts = new JSONArray();
		JSONObject contents = new JSONObject();
		contents.put("ports", throughPorts);
		for (Mediator mediator: mediators) {
			if (false == mediator.isReady() ||
					false == mediator.getInputPortId().equals(fromPortId))
				continue;
			JSONObject p = new JSONObject();
			throughPorts.put(p);
// <li>_toPortId_  _name_  _rulesId_ _portType_ _portName_ _accessName_

			p.put("toPortId", mediator.getOutputPortId());
			p.put("name", mediator.getName());
			p.put("rulesId", mediator.getRuleSetId());
			Port toPort = getDatabase().getPort(mediator.getOutputPortId());
			p.put("portType", toPort.getType());
			p.put("portName", toPort.getName());
			p.put("accessName", toPort.getAccessName());
		}
		String result = Utils.mergeJsonWithHtml(Utils.getHtmlTemplate("tree#port"), contents);
		return result;
	}

	public static void main(String args[]) throws IOException, ClassNotFoundException {
		String graph = Utils.modelToText(new GraphicsTools(null).createOntologiesGraph(null), null);
		System.out.println(graph);
	}

	/**
	 * Convert the input name String to a string which can be part of a URL.
	 * @param name String to be fixed.
	 * @return String of the fixed name prepended with a /.
	 */
	public static String makeName(String name) {
		if (Strings.isNullOrEmpty(name))
			return "";
//		String parts[] = name.split(" ");
//		name = parts[0];
//		if (parts.length > 1) // only two parts.
//			name += "_" + parts[1];
		// now clear the name
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
//			if (ch=='>') {
//				int k= 4;
//				int j= k++;
//			}
			if (Character.isJavaIdentifierPart(ch))
				continue;
			name = name.replace(ch, '_');
		}
		return "/" + name;
	}

	static public String breakUp(String longText, int limit, String nl) {
		String words[] = longText.split(" ");
		StringBuffer sb  = new StringBuffer();
		String line = "";
		for (String word: words) {
			if (line.length() > limit) {
				if (sb.length() > 0) 
					sb.append(nl);
				sb.append(line);
				line = "";
			}
			if (line.length() > 0)
				line += " ";
			line += word.trim();
		}
		if (line.length() > 0) {
			if (sb.length() > 0)
				sb.append(nl);
			sb.append(line);
		}
		return sb.toString();
	}
	
	static public String breakUp2(String longText, int limit, String nl) {
		String lines[] = longText.split("\n");
		StringBuffer sb = new StringBuffer();
		for (String line : lines) {
			if (sb.length() > 0)
				sb.append(nl);
			sb.append(breakUp(line, limit, nl));
		}
		String s = sb.toString();
		s = s.replaceAll("\"", "\\\"");
		s = s.replaceAll("\\<", "\\<");
		s = s.replaceAll("\\>", "\\>");
		s = s.replaceAll("\\|", "\\|");
		return s;
	}

	public String createRepositoryGraph(String id, String resource, String version, String query) throws JSONException {
		AModelRow port = (AModelRow) getDatabase().getItem(id);
		Model rmodel = null, model = null;
		String base = port.getModelInstanceNamespace();
		if (null != port && port.isReady()) {
			ARdfRepository r = port.getModelRepository();
			if (null != r) {
				if (Strings.isNullOrEmpty(version)) {
					rmodel = r.getModel();
					version = "";
				} else {
					rmodel = r.getModelForVersion(version, null);
					version = "&version=" + version ; // make it ready to be included in a URL.
				}
				if (null == rmodel)
					rmodel = r.getModel();
			}
		}
		Property hasTag = rmodel.createProperty(IConstants.SM_PROPERTY_SM_TAG_FULL);
		Resource isDeleted = rmodel.createResource(IConstants.SM_PROPERTY_RESOURCE_DELETED_FULL);
		String otherParams = "&amp;id=" + id + "&amp;action=showGraph" + version;
		Set<Resource> resources_set = new HashSet<Resource>();
		Property URLprop = rmodel.createProperty("http://URL");
		Property TITLEprop = rmodel.createProperty("http://TITLE");
		if (Strings.isNullOrEmpty(resource) && null == query) {
			model = rmodel;
		} else {// prepare a subgraph)
			model = ModelFactory.createDefaultModel();
			model.setNsPrefixes(rmodel.getNsPrefixMap());
			if (false == Strings.isNullOrEmpty(resource)) {
				String resources[] = resource.split(",");
				for (String res: resources) {
					if (res.indexOf(":") < 0)
						continue;
					Resource r = rmodel.getResource(base + res.split(":")[1]);
					resources_set.add(r);
					StmtIterator stmts = rmodel.listStatements(r, (Property)null, (RDFNode)null);
					model.add(stmts);
					stmts = rmodel.listStatements((Resource)null, (Property)null, r);
					model.add(stmts);
					NodeIterator ni = rmodel.listObjectsOfProperty(r, (Property)null);
					while (ni.hasNext()) {
						RDFNode an = ni.next();
						if (an.isResource()) {
							model.add(rmodel.listStatements((Resource)an, RDF.type, (RDFNode)null));
							model.add(rmodel.listStatements((Resource)an, hasTag, isDeleted));
						}
					}
				}
				ResIterator ri = model.listSubjects(); //ResourcesWithProperty((Property)null);
				while (ri.hasNext()) {
					Resource ar = ri.next();
					model.add(rmodel.listStatements(ar, RDF.type, (RDFNode)null));
					if (false == resources_set.contains(ar)) {
						String ars = ar.toString();
						String num = ars.substring(ars.lastIndexOf("/") + 1);
						model.add(ar, URLprop, "/dm/smGraph?src&amp;resource=base:" + num + "&amp;from=" + resource + otherParams);
						Statement stmt = rmodel.getResource(ar.toString()).getProperty(DCTerms.title);
						if (null != stmt)
							model.add(ar, TITLEprop, stmt.getObject().toString());
					}
				}
			} else { // model a query
				Port p = getDatabase().getPort(id);
				JSONObject aQuery = null;
				if (null != p && p.isRepository()) { 
					JSONObject savedQueries = p.getModelRepository().getSavedQueries();
					if (null != savedQueries)
						aQuery = (JSONObject) Utils.safeGet(savedQueries, query);
					if (null != aQuery) {
						Map<String, String> pxMap = Utils.fixNsPrefixes(rmodel.getNsPrefixMap()); 
						ResultSet results = SmContainer.getContainer(getDatabase()).
						executeQuery(rmodel, aQuery, pxMap);
						List<String> vars = results.getResultVars();
						Set<RDFNode> nodes = new HashSet<RDFNode>();
						while (results.hasNext()) {
							QuerySolution qs = results.next();
							Set<RDFNode> lits = new HashSet<RDFNode>();
							Set<Resource> rs = new HashSet<Resource>();
							for (String var : vars) {
								RDFNode n = qs.get(var);
								if (null == n)
									continue;
								if (n.isResource()) {
									nodes.add(n);
									rs.add(n.asResource());
									String num = n.toString();
									num = num.substring(num.lastIndexOf("/") + 1);
									Resource r = n.asResource();
									model.add(r, model.createProperty("http://resource"), num);
									model.add(r, URLprop, "/dm/smGraph?src&amp;resource=base:" + num + "&amp;from=" + resource + otherParams);
								} else if (n.isLiteral()) {
									lits.add(n);
//								} else {											
//									StmtIterator iter = rmodel.listStatements(null, model.getProperty(n.toString()), (RDFNode)null);
//									model.add(iter);
								}
							}
							for (Resource r : rs) {
								StmtIterator siter = r.listProperties();
								while (siter.hasNext()) {
									Statement s = siter.next();
									RDFNode o = s.getObject(); 
									if (o.isLiteral() && 
										lits.contains(o))
										model.add(s);
								}
							}


						}
						for (RDFNode n : nodes) {
							StmtIterator siter = n.asResource().listProperties();
							while (siter.hasNext()) {
								Statement s = siter.next();
								if (s.getObject().isResource() && nodes.contains(s.getObject().asResource()))
									model.add(s);
							}
						}
					}
				}
			}
		}

		try {
			return createDotFromRepository(model, base, resource);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

}

