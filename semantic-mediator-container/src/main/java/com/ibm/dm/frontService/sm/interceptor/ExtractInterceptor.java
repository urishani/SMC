
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
 */
package com.ibm.dm.frontService.sm.interceptor;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

//import com.ibm.dm.frontService.sm.intfc.ASmModuleIntercept;
import com.ibm.dm.frontService.sm.intfc.IInputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyLocator;
import com.ibm.dm.frontService.sm.intfc.IOutputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IRulesHandle;
import com.ibm.dm.frontService.sm.intfc.ISmModuleContext;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.intfc.MediationException;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * Interceptor class which asusmes a rules language which defines two rules: which are the containment relations in the
 * repository ontology and which are the cross relations among resources in the model of the ontology.<br>
 * The ruls: prefix is "http://rules#".<br>
 * Elements of this language are:<ul>
 * <li> rules:containmentRelation with property rules:includes - lists the containment relations of the ontology.
 * <li> rules:relations with property rules:includes - list the cross relations in the ontology.
 * <li> rules.containers with property rules.includes - lists all the discovered parents for a certain root, according to the first containment rule.
 * <li> rules.related with property rules.includes - is used to list all the discovered related elements.
 * </ul>
 * The interceptor iteratively find the closure of the containment relation for a given <b>root</b>. After that, the closure of related elements to the root
 * are followed and stored in the rules:related resource.
 * @author shani
 */
public class ExtractInterceptor implements ISmModuleIntercept { //extends ASmModuleIntercept {
	public boolean invoke(IOntologyLocator ontologyLocator,
			IInputModelHandle sourceModel, IOutputModelHandle targetModel)
	throws MediationException {
		throw new MediationException("invoke with ontologyLocator for " + this.getClass().getName() + " Is not implemented.");
	}

	private String rootString;
	public ExtractInterceptor(String root) {
		super();
		this.rootString = root;
	}

	public ExtractInterceptor() {
		super();
		this.rootString = null;
	}

	/**
	 * Signals to the invoker to initialize this intercepter first with this root element.
	 * @param root
	 * @return
	 */
	public boolean init(String[] root) {
		if (null != root && root.length > 0)
			this.rootString = root[0];
		return false;
	}
	/**
	 * Assumes that the model has embedded the rules.<br>
	 * The parent(s) of the root according to the rules:containmentRelations rule are returned as a Set.
	 * @param model a Model with the rules in it.
	 * @param rootString a Resource of the root element to follow on.
	 * @return
	 */
	public boolean invoke(IOntologyHandle sourceOntology,
			IInputModelHandle sourceModel, IOntologyHandle targetOntology,
			IRulesHandle mediationRules, IOutputModelHandle targetModel)
	throws MediationException {

		Model fromModel =  sourceModel.getModel();
		Model rulesModel;
		try {
			rulesModel = Utils.modelFromStream(mediationRules.getStream(), null, null);
		} catch (IOException e) {
			throw new MediationException(e.getMessage());		
		}
//		System.out.println("=====\n" + Utils.modelToText(rulesModel, null) + "\n============\n");
		String rulesNS = getNS();
		Collection<IOntologyHandle> dependencies = mediationRules.getDependencies();
		boolean ok = false;
		for (IOntologyHandle iOntologyHandle : dependencies) {
			String ns = iOntologyHandle.getBaseNS();
			if (ns.equals(rulesNS))
				ok = true;
			try {
				rulesModel.add(Utils.modelFromStream(iOntologyHandle.getStream(), null, null));
			} catch (IOException e) {
				throw new MediationException(e.getMessage());		
			}
//			System.out.println("=====\n" + Utils.modelToText(rulesModel, null) + "\n============\n");
		}
		if (false == ok)
			throw new MediationException("Mediation rules are not based on the rules ontology [" + rulesNS + "]");

		Map<String,String> prefixes = rulesModel.getNsPrefixMap();
		String prefix = Utils.makePrefix4Query(prefixes);

		// Figure out the properties for containers and for relaters
		setContainers(rulesModel, fromModel, prefix);
		setRelaters(rulesModel, fromModel, prefix);

		// Set the root of the extraction
		Resource root = findRoot(fromModel, rulesModel, rootString);

		// Get list of containers resources and a list of the related resources.
		Set<Resource> containers = collectParents(fromModel, root, prefix);
		Set<Resource> related = collectRelated(fromModel, root, prefix);

		fromModel.setNsPrefix("rules", rulesNS);
		fromModel.setNsPrefix("rdfs", RDFS.getURI()); //IConstants.RDFS_NAMESPACE);

		related.addAll(containers);

		// Put all the related element into the model as properties of a special node r
		Resource r = fromModel.getResource(getRelevant(true));
		Property p = fromModel.getProperty(getIncludes(true));
		for (Resource relatedResource : related) {
			fromModel.add(r, p, relatedResource);
		}
		fromModel.add(r, p, root);

		// Now extract a new model of only those resources included as properties of this special node, with all of their own properties.
		Model toModel = extractModel(fromModel);
		StmtIterator stmts = fromModel.listStatements(r, null, (RDFNode)null);
		while (stmts.hasNext()) {
			Statement stmt = stmts.next();
			/*Resource r1 = */stmt.getSubject();
		}
		stmts = fromModel.listStatements(root, null, (RDFNode)null);
		while (stmts.hasNext()) {
			Statement stmt = stmts.next();
			/*Resource r1 = */stmt.getSubject();
		}
		// Make the new model ready with proper NS prefixes and set it in the target result model of this mediation.
		toModel.setNsPrefixes(fromModel.getNsPrefixMap());
		targetModel.setModel(toModel);
		return false;
	}

	/**
	 * Answers with a resource that can serve as a root for the model extraction.
	 * In case this.rootString is null, it will use a rule to find an element which
	 * is a root of a model.<br>
	 * If this fails, it will generate a root element and return it to indicate that
	 * no root was specified and nothing was extracted therefore.
	 * @param model Model in which root needs to be located.
	 * @param root String of the root URI, or null if missing.
	 * @return a Resource of an element that can server as the root of the model structure.
	 */
	private Resource findRoot(Model model, Model rules, String root) {
		if (null != root)
			return model.getResource(root);
		Resource rootElementRule = rules.getResource(getRootElement(true));
		StmtIterator stmts = rules.listStatements(null, RDFS.subClassOf /*rules.getProperty(IConstants.RDFS_NAMESPACE + "subClassOf") */, rootElementRule);
		Resource rootType = null;
		if (stmts.hasNext())
			rootType = stmts.next().getSubject();
		if (null != rootType) { // this is the type of a root resource in the model. Now look it up.
			stmts = model.listStatements(null, RDF.type, rootType);
			Resource rootR = null;
			if (stmts.hasNext()) {
				rootR = stmts.next().getSubject();
				this.rootString = rootR.toString();
				return rootR;
			}
		}
		if (null == rootString) {
			Resource badRoot = model.createResource(getNS() + "unknownRoot");
			Statement s = model.createLiteralStatement(badRoot, DC.description /*model.createProperty(IConstants.DC_NAMESPACE + "description")*/, "Error: No root specified in extraction request");
			model.add(s);
			this.rootString = badRoot.toString();
			stmts = model.listStatements(badRoot, null, (RDFNode)null);
//			while (stmts.hasNext()) {
//				Statement stmt = stmts.next();
//				Resource r1 = stmt.getSubject();
//			}
			return badRoot;

		}
		return model.createResource(this.rootString); // will never execute anyway...
	}


	/**
	 * Answers with a new model of only relavant resources from the fromModel.
	 * @param fromModel Model in which rules:relavant is a bag (via rules:includes) of resources which are
	 * relevant to a certain root node (which is also included in this bag).
	 * @return a new Model of relevant reosurces and all their properties.
	 */
	private Model extractModel(Model fromModel) {
		QueryExecution qexec = null;
		String prefix = Utils.makePrefix4Query(fromModel.getNsPrefixMap());
		String constructQueryString = prefix +
		"CONSTRUCT { ?resource ?predicate ?object .}\n" +
		"WHERE {\n" +
			getRelevant(false) + " " + getIncludes(false) + " ?resource .\n" +
			"?resource ?predicate ?object .\n" +
		"}";
		Query query =  QueryFactory.create(constructQueryString);
		try {
			qexec = QueryExecutionFactory.create(query.toString(), fromModel);
			return qexec.execConstruct();
		} finally {
			if (null != qexec)
				qexec.close();
		}
	}

	/**
	 * Answers with the closure of related resources to the given root resource.
	 * <br>Assumes that the model has rules:related which is a bag of containment properties.
	 * <br>The "bag" is implemented via the rules:includes relation.<br>
	 * The related resources are discovered and added to the collection, using the <i>getRelated()</i> method.
	 * When the resulted set from this call does not add more new elements - it is time to stop calling this method.
	 * @param model a Model with the bag of relations in it.
	 * @param root a Resource of the root element to follow on.
	 * @param prefix String to be used as prefix for SPARQL queries.
	 * @return
	 */

	public Set<Resource> collectRelated(Model model, Resource root, String prefix) {
		Set<Resource> related = new HashSet<Resource>(),
			newRelated = new HashSet<Resource>();
		newRelated = getRelated(model, root, related, prefix);
		related.addAll(newRelated);
		Set<Resource> temp = null;
		while (newRelated.size() > 0) {
			Set<Resource> collator = new HashSet<Resource>();
			for (Resource resource : newRelated) {
				temp = getRelated(model, resource, related, prefix);
				collator.addAll(temp);
				related.addAll(temp);
			}
			newRelated= collator;
		}
		return related;
	}

	/**
	 * Utility to find all resources which are related of the root.<br>
	 * This is called repeatedly, passing in what was discovered already so that
	 * only new resources are returned.<br>
	 * When there are no new resources - it is time to stop calling this method.
	 * @param model Model with the rules of the root resource.
	 * @param root Resource to look for its direct containers.
	 * @param related Set of resources already followed and should not be included in the returned result.
	 * @param prefix String of prefixes to be used in queries.
	 * @return Set of Resources which are related of the original root and which should not be followed on.
	 */
	private Set<Resource> getRelated(Model model, Resource root, Set<Resource> related, String prefix) {
//		System.out.println("==========================\n" + Utils.modelToText(model, null));

		String query_r = prefix +
			"SELECT ?related \n" + //?related1 \n" +
			"WHERE {\n" +
				"<" + root + "> ?relation ?related .\n" +
				getRelated(false) + " " + getIncludes(false) + " ?relation . \n" +
//				"OPTIONAL {\n" +
//					"<" + root + "> ?relation1 ?related1 .\n" +
//					getContainers(false) + " " + getIncludes(false) + " ?relation1 . \n" +
//				"}\n" +
			"}\n";

		String query_c = prefix +
		"SELECT ?related  \n" +
		"WHERE {\n" +
			"<" + root + "> ?relation ?related .\n" +
			getContainers(false) + " " + getIncludes(false) + " ?relation . \n" +
		"}\n";

		Set<Resource> newRelated = getResultsOfQuery(query_r, model, "related"); //, "related1");
		newRelated.addAll( getResultsOfQuery(query_c, model, "related"));
		newRelated.removeAll(related);
		return newRelated;
	}

	/**
	 * Executes a query for resources in a single var select statement<br>
	 * E.g.., SELECT ?var WHERE { a b c. }, where the var name is provided as a param as
	 * well as the model.
	 * @param queryString String of the query as described above.
	 * @param model Model to apply the query on
	 * @param var String for the var name.
	 * @return Set<Resource> of answers to the query.
	 */
	private Set<Resource> getResultsOfQuery(String queryString, Model model,
			String... var) {
		Query query =  QueryFactory.create(queryString);
		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query.toString(), model);
			ResultSet results = qexec.execSelect();
			Set<Resource> answer = new HashSet<Resource>();
			while (results.hasNext()) {
				QuerySolution soltn = results.next();
				for (int i= 0; i < var.length; i++) {
					Resource resource = (Resource) soltn.get(var[i]);
					answer.add(resource);
				}
			}
			return answer;
		} finally {
			if (null != qexec)
				qexec.close();
		}
	}

	/**
	 * Answers with the chain of parents to the given root resource.
	 * <br>Assumes that the model has rules:containers which is a bag of containment properties.
	 * <br>The "bag" is implemented via the rules:includes relation.<br>
	 * The parents are discovered and added to the collection, using the <i>getParents()</i> method.
	 * When the resulted set from this call does not add more elements - it is time to stop calling this method.
	 * @param model a Model with the bag of relations in it.
	 * @param root a Resource of the root element to follow on.
	 * @param prefix String to be used as prefix for SPARQL queries.
	 * @return
	 */
	private Set<Resource> collectParents(Model model, Resource root, String prefix) {
		Set<Resource> containers = new HashSet<Resource>();
//		System.out.println("==========================\n" + Utils.modelToText(model, null));
		Set<Resource> parents = getParents(model, root, prefix);
//		containers.addAll(parents);
		while(parents.size() > 0) {
			containers.addAll(parents);
			Set<Resource> granspaps = new HashSet<Resource>();
			for (Resource resource : parents) {
				granspaps.addAll(getParents(model, resource, prefix));
			}
			parents = granspaps;
		}
		return containers;
	}



	/**
	 * Utility to find all resources which are containers of the root.
	 * @param model Model with the rules of the root resource.
	 * @param root Resource to look for its direct containers.
	 * @param prefix String for query prefix
	 * @return Set of Resources which are containers of the root.
	 */
	private Set<Resource> getParents(Model model, Resource root, String prefix) {
		String queryString = prefix +
			"SELECT ?parent\n" +
			"WHERE {\n" +
			"?parent ?contains <" + root + "> .\n" +
			getContainers(false) + " " + getIncludes(false) + " ?contains . \n" +
		" }" ;
		Query query =  QueryFactory.create(queryString);
		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query.toString(), model);
//			qexec = QueryExecutionFactory.create(prefix + " select ?parent where { ?parent sm:hasBlock sm:9 . }", model);
			ResultSet results = qexec.execSelect();
			Set<Resource> parents = new HashSet<Resource>();
			while (results.hasNext()) {
				QuerySolution soltn = results.next();
				Resource parent = (Resource) soltn.get("parent");
				if (null != parent) parents.add(parent);
			}
			return parents;
		} finally {
			if (null != qexec)
				qexec.close();
		}
	}


	/**
	 * Utility to find all resources which are properties for containers in the rules model, and
	 * set them in bag for rules:containers in the targetModel.
	 * @param ruleModel Model with the rules for finding the containment relations.
	 * @param targetModel Model in which to create the bag of containment relations.
	 * @param prefix String for Ns prefix for the query.
	 */
	private void setContainers(Model rulesModel, Model targetModel, String prefix) {
		String queryString = prefix +
			"SELECT ?contains ?contains1 ?contains2 \n" +
			"WHERE { \n" +
			"  ?contains rdfs:subPropertyOf rules:ContainmentProperty . \n" +
			"  OPTIONAL { ?contains1 rdfs:subPropertyOf ?contains . \n" +
			"             OPTIONAL { ?contains2 rdfs:subPropertyOf ?contains1 . } \n" +
			"  }\n" +
		    "}" ;
		Query query =  QueryFactory.create(queryString);
		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query.toString(), rulesModel);
			ResultSet results = qexec.execSelect();
			Set<Resource> containers = new HashSet<Resource>();
			while (results.hasNext()) {
				QuerySolution soltn = results.next();
				Resource contains = (Resource) soltn.get("contains");
				Resource contains1 = (Resource) soltn.get("contains1");
				Resource contains2 = (Resource) soltn.get("contains2");
				if (null != contains) containers.add(contains);
				if (null != contains1) containers.add(contains1);
				if (null != contains2) containers.add(contains2);
			}
			Resource r = targetModel.getResource(getContainers(true));
			Property p = targetModel.getProperty(getIncludes(true));
			for (Resource resource : containers) {
				targetModel.add(r, p, resource);
			}
		} finally {
			if (null != qexec)
				qexec.close();
		}
	}

	/**
	 * Utility to find all resources which are properties for relations and
	 * set them in the rules:related bag in the targetModel.
	 * @param ruleModel Model with the rules for finding the related relations.
	 * @param targetModel Model in which to create the bag of related relations.
	 * @param prefix String for Ns prefix for the query.
	 */
	private void setRelaters(Model rulesModel, Model targetModel, String prefix) {
		String queryString = prefix +
			"SELECT ?relates ?relates1 ?relates2 ?relates3\n" +
			"WHERE {\n" +
			"?relates rdfs:subPropertyOf rules:RelationProperty . \n" +
			"OPTIONAL { ?relates1 rdfs:subPropertyOf ?relates . }\n" +
//			"OPTIONAL { ?relates2 rdfs:subPropertyOf ?relates1 . }\n" +
//			"OPTIONAL { ?relates3 rdfs:subPropertyOf ?relates2 . }\n" +
		" }" ;
//		System.out.println("==========\n" + Utils.modelToText(rulesModel, null) + "\n============\n");
		Query query =  QueryFactory.create(queryString);
		QueryExecution qexec = null;
		try {
			qexec = QueryExecutionFactory.create(query.toString(), rulesModel);
//			qexec = QueryExecutionFactory.create(prefix + "select ?b ?c where { sm:operations ?b ?c . }", rulesModel);
			ResultSet results = qexec.execSelect();
			Set<Resource> relations = new HashSet<Resource>();
			while (results.hasNext()) {
				QuerySolution soltn = results.next();
				Resource relates = (Resource) soltn.get("relates");
				Resource relates1 = (Resource) soltn.get("relates1");
				Resource relates2 = (Resource) soltn.get("relates2");
				Resource relates3 = (Resource) soltn.get("relates3");
				if (null != relates) relations.add(relates);
				if (null != relates1) relations.add(relates1);
				if (null != relates2) relations.add(relates2);
				if (null != relates3) relations.add(relates3);
			}
			Resource r = targetModel.getResource(getRelated(true));
			Property p = targetModel.getProperty(getIncludes(true));
			for (Resource resource : relations) {
				targetModel.add(r, p, resource);
			}
		} finally {
			if (null != qexec)
				qexec.close();
		}
	}

	static public final String getNSPrefix() {
		return "rules";
	}
	static public final String getNS() {
		return "http://rules#";
	}
	static public final String getPrefix() {
		return "rules";
	}

	static public String getRootElement(boolean full) {
		return (full?getNS():getNSPrefix() + ":") + "RootElement";
	}
	static public String getRelatedRelation(boolean full) {
		return (full?getNS():getNSPrefix() + ":") + "relatedRelation";
	}
	static public String getContainmentRelation(boolean full) {
		return (full?getNS():getNSPrefix() + ":") + "containmentRelation";
	}
	static public String getContainers(boolean full) {
		return (full?getNS():getNSPrefix() + ":") + "containers";
	}
	static public String getRelated(boolean full) {
		return (full?getNS():getNSPrefix() + ":") + "related";
	}
	static public String getRelevant(boolean full) {
		return (full?getNS():getNSPrefix() + ":") + "relevant";
	}
	static public String getIncludes(boolean full) {
		return (full?getNS():getNSPrefix() + ":") + "includes";
	}



	static Model mRulesModel = null;
	public static Model getExtractionRules() {
		Model model = ModelFactory.createDefaultModel();
		//Map<String, String> nsMap = model.getNsPrefixMap();
		model.setNsPrefix(getNSPrefix(), getNS()).setNsPrefix("rhp_model", "http://com.ibm.ns/rhapsody/haifa/sm#");
		/*nsMap = */model.getNsPrefixMap();
		Resource r = model.getResource(getContainmentRelation(true));
		Property p = model.createProperty(getIncludes(true));
		model.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasPart"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasPackage"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasEvent"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasProvided"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasInterface"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasRealization"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasRequired"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasSystem"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasAttribute"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasConnector"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasContract"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasFlow"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasOperation"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasPort"));
		r = model.getResource(getRelatedRelation(true));
		model.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#flowsViaPort"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#actsOnEvent"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasFlowDirection"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasPortDirection"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasEndPoint"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasPort_1"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasPort_2"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasBaseClass"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasAttributeType"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasReturnType"))
		.add(r, p, model.getResource("http://com.ibm.ns/rhapsody/haifa/sm#hasType"));
		mRulesModel = model;
//		String rdf = Utils.modelToText(model, null);
		/*nsMap = */model.getNsPrefixMap();
		return model;
	}

	public boolean close() {
		return false;
	}

	public boolean created() {
		return false;
	}

	public boolean initialized(ISmModuleContext context) {
		return false;
	}


	public boolean reset() {
		return false;
	}

	public boolean tearDown() {
		return false;
	}
}

