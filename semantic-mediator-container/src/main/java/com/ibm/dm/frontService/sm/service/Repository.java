
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Ontology;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.intfc.imp.OntologyDescription;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * Represents a repository and maintains an access to it in memory cache.
 * <br>Used for model RDF repositories. Not for ontologies.
 * @author shani
 *
 */
public class Repository extends ARdfRepository {
	private static final String BASE_NS = "http://com.ibm.semantic.mediation";
	private static final String CONFIG_RESOURCE = BASE_NS + "/config";
	private static final String BASE_PROPERTY = CONFIG_RESOURCE + "/property#";
	private static final String BASE_PROPERTY_COUNTER = BASE_PROPERTY + "counter";
	private static final String BASE_PROPERTY_DOMAIN = BASE_PROPERTY + "domain";
//	private static final String DOMAIN_EXT = ".owl";

//	private Model repository;
	int resourceNum = 0;
//	private final String fileName;
//	private final String folder;
//	private final String domain;
//	private final String base;

	/**
	 * Answers whether the repository is empty or not.
	 */
	public boolean isEmpty() {
		return resourceNum < 2;
	}
	
	public String getDomain() {
		return ((Port)myItem).getAccessName();
	}

	/**
	 * Validates whether the resource parameter is a configuration resource which should be ignored during mediation merge.
	 * @param r Resource to be validated.
	 * @return boolean.
	 */
	public static boolean isConfigResource(Resource r) {
		return null != r && r.toString().equals(CONFIG_RESOURCE);
	}
	/**
	 * Answers with the String for the URI used as a configuration resource of repositories.
	 * @return String.
	 */
	public static String getConfigResource() {
		return CONFIG_RESOURCE;
	}
	public static final String REPOSITORY = "repository";
//	private static final String REPOSITORIES_FOLDER = "repositories";

//	/**
//	 * used only for migration.
//	 * @return
//	 */
//	private static final File repositoriesFolder() {
//		File folder = new File(Database.SM_MODEL_FOLDER, REPOSITORIES_FOLDER);
//		if (!folder.exists())
//			folder.mkdirs();
//		return folder;
//	}

//	/**
//	 * Modified old code to be deprecated in the future. Listing all repositories, new and old
//	 * to view.
//	 * @param folderName folder of all configuration material.
//	 * @return
//	 * @throws Exception 
//	 */
//	public static JSONArray getDomains(String folderName, Database db) throws Exception {
//		JSONArray domains = new JSONArray();
//		List<Port> ports = db.getPorts();
//		for (Port port : ports) {
//			if (port.isRepository()) {
//				JSONObject d = new JSONObject();
//				d.put("id", port.getId());
//				d.put("domain", port.getAccessName());
//				domains.add(d);
//			}
//		}
//		return domains; 
//	}



	/**
	 * Answers with a message for the completion of a clearing of 
	 * a repository history at a certain depth.
	 * @param depth int for how "deep" to clear history. If -1 - clears all history, otherwise, 
	 * leaves only so many history versions.
	 * @return Message about what was done.
	 */
	public String clearHistory(int depth) {
		List<String> vers = getHistory();
		if (depth < 0)
			depth = 0;
		else if (depth > vers.size())
			depth = vers.size();
		for (int i=depth; i < vers.size(); i ++) {
			String fn = vers.get(i);
			File f = new File(folder, fn);
			if (f.exists()) {
				boolean b = f.delete();
				b = !b;
			}
		}
		return "Cleared repository [" + getDomain() + "] history. Now at depth [" + depth + "]";
	}
	
	@Override
	protected void init() {
		super.init();
//		repository = ModelFactory.createDefaultModel();
		String ns = makeBase();
//		repository.setNsPrefix("", ns);
		repository.setNsPrefix("base", ns);
		this.base = ns; // getBase(); // need to ensure this is the base. 
		System.out.println("NS map: " + repository.getNsPrefixMap());
		Resource resource = repository.createResource(CONFIG_RESOURCE);
		Property counter = repository.createProperty(BASE_PROPERTY_COUNTER);
		resource.removeAll(counter);
		resource.addProperty(counter, "1")
			.addProperty(repository.createProperty(BASE_PROPERTY_DOMAIN), getDomain());
		resourceNum = 1;
		System.out.println(resource.toString());
		StmtIterator iter = repository.listStatements();
		while (iter.hasNext())
			System.out.println(iter.next().asTriple().toString());
//		save();
	}
	
	/**
	 * Resets the repository doing a full initialization. This happens when base of the repository may be changed
	 * due to change of the access of its related item, or the "domain" of that item.
	 */
	public synchronized void reset() {
		this.base = null;
		init();
	}

	@Override
	public void doSave() {
		Resource r = repository.createResource(CONFIG_RESOURCE);
		Property p = repository.createProperty(BASE_PROPERTY_COUNTER);
		r.removeAll(p);
		r.addProperty(p, Integer.toString(resourceNum));
		saveToFile();
	}

	@Override
	/**
	 * This cannot happen in a repository... it must be initialized to empty
	 * and filled up through merging.
	 */
	public void init(Model initialModel, boolean...isVolatile) {
		System.err.println("Repository.init(model) is not legal. Repository is reset to empty.\nTrace:\n");
		new Exception("Illegal call to Repository.init(model)").printStackTrace();
		super.init();
	}

	private String makeBase() {
		String baseName = getMyItem().getDatabase().getHost(true, true); //Utils.getHost(null, true);
		String project = getMyItem().getDatabase().getProject();
		if (false == Strings.isNullOrEmpty(project))
			project += "/";
		else
			project = "";
		return baseName + "/dm/sm/repository/" + project + getDomain() + "/resource/";
	}

	
	/** 
	 * Initialize the repository with an existing model<br>
	 * If this is a model repository and not ontology, we need to not make the meta element
	 * in it a resource. That is indicated in the <i>isPort</i> boolean parameter.
	 * @param model
	 * @param isPort
	 * @return Map<String, String> of resources in the original model, to the resources in the
	 * cloned model.
	 */
	public Map<String, String> cloneModel(Model model, boolean isPort) {
		ResIterator ri = model.listSubjects();
		Map<String, Resource> m = new HashMap<String, Resource>();
		init();
		while (ri.hasNext()) {
			String rs = ri.next().toString();
			if (isPort && rs.equals(CONFIG_RESOURCE)) 
				continue;
			Resource nr = getResource();
			m.put(rs, nr);
		}
		ri = model.listSubjects();
//		Map<String, String> mp = getModel().getNsPrefixMap();
		getModel().setNsPrefixes(model.getNsPrefixMap());
		getModel().setNsPrefix("base", base);
//		mp = getModel().getNsPrefixMap();
		while (ri.hasNext()) {
			Resource r = ri.next();
			Resource nr = m.get(r.toString());
			if (null == nr)
				continue;
			StmtIterator si = r.listProperties();
			while (si.hasNext()) {
				Statement s = si.next();
				Property p = s.getPredicate();
				RDFNode o = s.getObject();
				Resource ro = m.get(o.toString());
				if (null != ro)
					o = ro;
				add(nr, p, o);
			}
		}
		save();
		Map<String, String> map = new HashMap<String, String>();
		Set<String> keys = m.keySet(); 
		for (String url : keys) {
			Resource r = m.get(url);
			map.put(url, r.toString());
		}
		return map;
	}

public Repository(AModelRow item) {
		super(item);
		resourceNum = repository.getProperty(repository.createResource(CONFIG_RESOURCE),
				repository.createProperty(BASE_PROPERTY_COUNTER)).getInt();
		if (false == item instanceof Port || false == ((Port)item).isRepository())
			System.err.println("Repository is created for an item which is not a repository of models. Item [" + item.getDisplayId() + "]");
		fixPrefixes();

	}

	/**
	 * Answers with a resource mapping for the fixed name space, in case there is a fix to be done.
	 * Mappings is from the old resource names to the new resource URLs.
	 * Otherwise, a null is returned.
	 * The model is rebuilt from the ground up based on the present model contents of the repository.
	 * Yet, the associations made to resources need to be updated too, based on this mapping.
	 */
	public Map<String, String> fixNameSpace() {
		String cns = getBase(); // current name space
		String gns = makeBase(); // good name space
		if (cns.equals(gns))
			return null;
		
		return cloneModel(getModel(), false);
	}

	/**
	 * A factory of resources calling this will allocate a number for a resource even if it is not
	 * populated later.
	 * @return Resource for this repository base SM name space.
	 */
	public Resource getResource() {
		resourceNum ++;
		setDirty();
		return repository.createResource(this.base + new DecimalFormat("0000").format(resourceNum));
	}

	@Override
	protected void loadRepository() {
		super.loadRepository();
		try {
			Property p = repository.getProperty(BASE_PROPERTY_COUNTER);
			Resource r = repository.getResource(CONFIG_RESOURCE);
			Statement s = r.getProperty(p);
			RDFNode v = s.getObject();
			resourceNum = Integer.parseInt(v.toString());
		} catch (Exception e) {
			e.printStackTrace();
			init();
			save();
		}
	}

	/**
	 * Obtains an existing resource, or null if it is not in the graph.
	 * @param uri String URI of the resource to be located
	 * @return Resource or null if resource does not exist.
	 */
	public Resource getResource(String uri) {
		Resource resource = repository.createResource(uri);
		if (false == containsResource(uri))
			return null;
		return resource;
	}

	/**
	 * verify that a resource is indeed in the graph of the model.
	 * @param uri
	 * @return
	 */
	public boolean containsResource(String uri) {
		if (null == uri || "".equals(uri))
			return false;
		return repository.containsResource(repository.createResource(uri));
	}
	/**
	 * Answers whether the resource parameter is not null, and the repository contains it.
	 * @param resource Resource or null
	 * @return true is there is such a resource in the repository.
	 */
	public boolean containsResource(Resource resource) {
		if (null == resource)
			return false;
		return repository.containsResource(resource);
	}

//	/**
//	 * Answers with a report on cleaning all leftover repositories in wrong place
//	 * and all repositories astray in repository port places.
//	 * @return String message repost on progress.
//	 */
//	public static String migrateToV2_1(Database db) {
//		String msg = "";
//		// first look for repositories in wrong place: directly in the folder of all data:
//		File folder = db.getFolder(); //new File(SmContainer.getFolder());
//		folder.mkdirs();
//		String files[] = folder.list(new FilenameFilter() {
//			public boolean accept(File dir, String name) {
//				boolean accept = name.endsWith(getDomainExt());
//				return accept;
//			}
//		});
//		for (String file : files) {
//			String domain = file.substring(0, file.indexOf(getDomainExt()));
//			Port rep_port = db.getPort4Domain(domain);
//			File oldFile = new File(folder, file);
//			if (null != rep_port) {
//				if (oldFile.canRead()) {
//					msg += "\tMigrating old repository for [" + file + "].\n";
//					File newFolder = repositoriesFolder();
//					newFolder = new File(newFolder, rep_port.getId());
//					newFolder.mkdirs();
//					File newFile = new File(newFolder, file);
//					if ((false == newFile.canRead()) && oldFile.canRead()) {
//						oldFile.renameTo(newFile);
//						msg += "Migrated repository file [" + oldFile + "] --> [" + newFile + "].\n";
//					}
//				}
//			}
//			
//			if (oldFile.canRead()) { // need to remove
//				msg += "\t\tNot migrated - deleting file [" + oldFile + "]: " + (oldFile.delete()?" done." : "failed.") + "\n";
//			}
//		}
//	    // now clean all port folders that are not a real port, and all repositories
//		// in these port folders that are not based on the port domain name
//		List<Port> ports = db.getPorts();
//		List<String> repositories = new ArrayList<String>();
//		for (Port port : ports) {
//			if (port.isRepository())
//				repositories.add(port.getId());
//		}
//		folder = repositoriesFolder();
//		String portDirs[] = folder.list(new FilenameFilter() {
//			public boolean accept(File dir, String name) {
//				return name.startsWith("Prt");
//			}
//		});
//		for (String dir : portDirs) {
//			if (false == repositories.contains(dir)) {
//				msg += "\tDeleting illegal repository port directory [" + dir + "]: " + (Utils.deleteFolder(new File(folder, dir), true)?"Done.\n" : "Failed.\n");
//			} else {
//				String domain = db.getPort(dir).getAccessName();
//				File portFolder = new File(folder, dir);
//				String members[] = portFolder.list();
//				for (String member : members) {
//					if (false == member.startsWith(domain))
//						msg += "\tRemove repository member [" + member + "] in port folder [" + portFolder.toString() + "], domain [" + domain + "]: " +
//						(new File(portFolder, member).delete()? "Done.\n" : "Failed.\n");
//				}
//			}
//		}
//		return msg;
//	}

//	/**
//	 * Migrating repositories from folder ${home}/repositories/ to ${home}/Repository so it matches the
//	 * the common naming convention for items with repositories.
//	 * @param database
//	 * @return String msg report on the progress of the migration.
//	 */
//	public static String migrateToV2_5(Database database) {
//		String msg = "";
//		// first look for repositories in wrong place: directly in the folder of all data:
//		File folder = database.getFolder(); //new File(SmContainer.getFolder());
//		File origFolder = new File(folder, "repositories");
//		File toFolder = new File(folder, "Port");
//		origFolder.mkdirs();
//		toFolder.mkdirs(); toFolder.delete();
//		msg = "Migrating port repositories to Version 2.5.\n" + 
//		"\tMoving all Prt folders from [" + origFolder.getAbsolutePath() + "] --> [" + 
//			toFolder.getAbsolutePath() + "]: ";
//		boolean done = origFolder.renameTo(toFolder);
//		msg += done? "successfully.\n":"failed.\n";
//		return msg;
//	}


//	public String getFileName() {
//		return fileName;
//	}

	@Override
	public String getBase() {
		if (Strings.isNullOrEmpty(base)) {
		   synchronized (Repository.class) {
			   if (Strings.isNullOrEmpty(base))
				   this.base = makeBase();
		   }
		}
		return base;
	}

	public static Repository create(AModelRow item) {
		return new Repository(item);
	}

	@Override
	String getBaseForVersion(String version) {
		if (version.equals("current"))
			return getModel().getNsPrefixURI("base");
		else {
			File oldVersion = new File(getFolder(), version);
			try {
				return Utils.modelFromFile(oldVersion.getAbsolutePath()).getNsPrefixURI("base");
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	private final static Map<String, String> preferredPrefixes = new HashMap<String, String>();
	static {
		preferredPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		preferredPrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		preferredPrefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		preferredPrefixes.put("dcterms", "http://purl.org/dc/terms/");
		preferredPrefixes.put("oslc", "http://open-services.net/ns/core#");
		preferredPrefixes.put("smc", "http://com.ibm.ns/haifa/sm#");
	}

	@Override
	public Map<String, String> getPreferredPrefixes() {
		if (false == myItem instanceof Port)
			return preferredPrefixes;
		Ontology o = ((Port)myItem).getOntology();
		if (null == o)
			return preferredPrefixes;
		Database db;
		try {
			db = getMyItem().getDatabase();
		} catch (Exception e) {
			e.printStackTrace();
			return preferredPrefixes;
		}
		Map<String, String> m = new HashMap<String, String>(preferredPrefixes);
		int pCnt = 0;
		LinkedList<Ontology> s = new LinkedList<Ontology>();
		if (null != o) {
			s.add(o);
		}
		while (s.size() > 0) {
			o = s.removeFirst();
			OntologyDescription od = o.getOntologyDescription();
			String imports[] = od.getImports();
			for (String imp: imports) {
				Ontology io = db.getOntologyByClassUri(imp);
				if (null == io)
					continue;
				s.add(io);
			}
			URI nameSpace = o.getNameSpaceUri();
			String prefix = o.getPrefix();
			if (Strings.isNullOrEmpty(prefix))
				prefix = "p_" + (++pCnt);
			m.put(prefix, nameSpace.toString());
		}
		return m;
	}


}

