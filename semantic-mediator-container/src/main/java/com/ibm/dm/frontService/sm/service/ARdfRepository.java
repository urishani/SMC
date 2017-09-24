
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
 * Copyright IBM  2013 All Rights Reserved
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * Represents a repository and maintains an access to it in memory cache.
 * @author shani
 *
 */
public abstract class ARdfRepository {
	private static final String DOMAIN_EXT = ".owl";

	protected Model repository;
	protected final String fileName;
	protected final String folder;
	protected String base="";
	protected long currentDate = Long.MAX_VALUE;
	protected long loadDate = Long.MAX_VALUE;

	protected boolean isDirty;
	
	protected final AModelRow myItem;
	
	public final boolean isDirty() {
		return isDirty;
	}
	public final void setDirty() {
		isDirty = true;
	}
	public final String getFolder() {
		return folder;
	}

	/**
	 * Sets up a recovery point that can be used to recover status of the repository in case of an incomplete
	 * modification to be backtracked.
	 */
	public void setRecovery() {
		// Nothing to do... 
	}
	
	public JSONObject getSavedQueries() {
		File f = new File(this.fileName);
		File fSav = new File(f.getParent(), "queries.json");
		if (fSav.canRead())	 {
			JSONObject queries = new JSONObject();
			try {
				queries = new JSONObject(Utils.stringFromStream(new FileInputStream(fSav)));
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}
			return queries;
		}
		return new JSONObject();		
	}

	/**
	 * Saves or deletes an entry in an archive storage of queries for reuse.
	 * @param aQuery JSONObject which also contains the query name under property
	 * "name", and query arguments in other attributes. If the attribure "delete"
	 * exists, the query is removed from the storage.
	 * <br>
	 * The archive is in a file and is updated each time.
	 * @throws JSONException 
	 */
	public synchronized void saveQuery(JSONObject aQuery) throws JSONException {
		JSONObject queries = getSavedQueries();
		String name = (String) Utils.safeGet(aQuery, "name");
		if (null != Utils.safeGet(aQuery, "delete"))
			queries.remove(name);
		else
			queries.put(name, aQuery);
		File f = new File(this.fileName);
		File fSav = new File(f.getParent(), "queries.json");
		fSav.delete();
		try {
			fSav.createNewFile();
			OutputStream os = new FileOutputStream(fSav);
			os.write(queries.toString().getBytes());
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}


	/**
	 * Corrects the prefixes of the RDF model to match the preferred prefixes for the owning item.
	 */
	public void fixPrefixes() {
		if (myItem.getId().equals("Rul-115"))
			System.out.println("at Rule-115");
		Model model = getModel();
		Map<String, String> m = model.getNsPrefixMap();
		Map<String, String> im = new HashMap<String, String>(); // to maintain inverse for selecting unique name spaces.
		for (String k: m.keySet())
			im.put(m.get(k), k);
		Map<String, String> pm = getPreferredPrefixes();  // Implemented by subclasses.
		for (String k: pm.keySet()) {
			if (k.equals(IConstants.SM_PROPERTY_NS_PREFIX) && m.containsKey(IConstants.SM_PROPERTY_NS_PREFIX)) // do not add "sm" standard if one already exists in the model.
				continue; 
			im.put(pm.get(k), k);  // this overrides existing name spaces with preferred prefix.
		}
		// now ensure you have the base prefix defined:
		String base = getBase();
		if (false == Strings.isNullOrEmpty(base)) {
			if (false == (im.containsKey(base) || im.containsKey(base + '#')))
				im.put(base, "base");
		}
		// Now make a new map to be used:
		Map<String, String> nm = new HashMap<String, String>();
		for (String k: im.keySet())
			nm.put(im.get(k), k);
		// Now feed into the model the new namespace - if changed.
		if (false == nm.equals(m)) {
			System.out.println("Prefixes are not same for [" + myItem.getId() + "]. New:\n\t" + 
					Joiner.on("\n\t").withKeyValueSeparator(" = ").join(nm) + 
					"\nCompared with old one:\n\t" + 
					Joiner.on("\n\t").withKeyValueSeparator(" = ").join(m));
			for (String k: nm.keySet()) {
				if (null == m.get(k) || false == nm.get(k).equals(m.get(k)))
					model.setNsPrefix(k, nm.get(k));
			}
			for (String k: m.keySet()) {
				if (null == nm.get(k)) 
					model.removeNsPrefix(k);
			}
			isDirty = true;
			save();
			System.out.println("Repository modified namespace prefixes and saved. For item [" + myItem.getDisplayId() + "]");
		}
	}

	/**
	 * Answers with a preferred set of prefixes. Implementation in subclasses may return data based on ontologies descriptions, and
	 * oslc considerations.
	 * @return Map of prefixes to namespaces. Default is empty - no preferences.
	 */
	public Map<String, String> getPreferredPrefixes() {
		return new HashMap<String, String>();
	}
	/**
	 * Backtracks the status of the repository to the recovery point being set earlier.
	 */
	public void recover() {
		loadRepository(); // simple act in which the repository is reloaded.
	}
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
			File f = new File(new File(getFolder()), fn);
			if (f.exists()) {
				boolean b = f.delete();
				b = !b;
			}
		}
		return "Cleared repository [" + myItem.getId() + "] history. Now at depth [" + depth + "]";
	}
	
	
	protected ARdfRepository(AModelRow item) {
		this.folder = item.getDatabase().getFolder(item); // gives: /<project>/SM.model.rdf/ports/Prt-101
		this.myItem = item;
		File newFile = new File(folder, getDomain() +  getDomainExt()); // /.../Prt-101/rhp.owl
		this.fileName = newFile.getAbsolutePath();

		loadRepository();
//		currentDate = getDate();
//		myItem.markModified(currentDate);
	}

	protected void loadRepository() {
		File f = new File(this.fileName); 
		if (f.canRead())
			try {
				repository = Utils.modelFromFile(this.fileName);
				currentDate = getDate();
				loadDate = System.currentTimeMillis();
				isDirty = false;
				System.out.println("[" + new Date() + "]: loading an RDF-Repository for [" + myItem.getDisplayId() + "]. Last modified [" + new Date(getDate()) + "]");
				this.base = getBase();
			} catch (Exception e) {
				e.printStackTrace();
				init();  // The file is illegal. Initializing this altogether.
			}
		else {
			init();
		}
		save(); // will be done if the repository was initialized - in which case the dirty flag is on.
	}
	
	/**
	 * Answers with the time the repository has been loaded into memory.
	 * @return long for the date.
	 */
	public long getLoadDate() {
		return loadDate;
	}
	/**
	 * Needs to resolve the base NS of the model depending on the specific implementation.<br>
	 * Used during repository construction.
	 * @return String for the base NS. 
	 */
	public abstract String getBase();

	/**
	 * Initializes a new repository.
	 */
	protected void init() {
		repository = ModelFactory.createDefaultModel();
		currentDate = getDate();
		setDirty();
//		save();
		this.base = getBase();
		System.out.println("[" + new Date() + "]: initializing an RDF-Repository for [" + myItem.getDisplayId() + "]. Last modified [" + new Date(getDate()) + "]");
	}

	/**
	 * Clears the repository present contents. Does not touch its history.
	 * @return Message about what was done.
	 */
	public String clearRepository() {
		init();
		save();
		return ("RDF Repository [" + getDomain() + "] cleared.");
	}

	/**
	 * Initializes a new repository with a model
	 * @param initialModel - a model to use for initial value of this repository, replacing entire content with new content.
	 * @param isVolatile - optional boolean to specify if to make the repository dirty after this initialization. 
	 */
	public void init(Model initialModel, boolean...isVolatile) {
		repository = initialModel;
		if (isVolatile.length == 0 || false == isVolatile[0])
			setDirty();
	}

	
	/**
	 * Answers with the extension for files containing the model.
	 * @return
	 */
	protected static String getDomainExt() {
		return DOMAIN_EXT;
	}

	/**
	 * Answers with a base name for the model file.
	 * @return String for the name. should be something like "ontology", "model", "repository", or anything else
	 */
	protected abstract String getDomain();

	/**
	 * Answers with the list of files having domain as their prefix to mean all the files
	 * serving as history of that repository.
	 * @return List<String> of file names.
	 */
	public List<String> getHistory() {
		String files[] = new File(this.folder).list();
		List<String> list = new ArrayList<String>(files.length);
		for (String file : files) {
			if (file.startsWith(getDomain()) && file.substring(getDomain().length()).startsWith(getDomainExt() + ".v-")) {
				String dateStr = file.substring(getDomain().length() + getDomainExt().length() + 3);
				if (dateStr.matches("[0-9]*"))
					list.add(file);

			}
		}
		String[] l = list.toArray(new String[0]);
		Arrays.sort(l, new Comparator<String>() { // sort inversely
			public int compare(String o1, String o2) {
				return -o1.compareTo(o2);
			}
		});
		return Arrays.asList(l);
	}

	public JSONObject compareHistory(String version, String version2) throws JSONException {
		Model model1 = getModelForVersion(version, null);
		String b1 = getBaseForVersion(version);
		Model model2 = getModelForVersion(version2, null);
		String b2 = getBaseForVersion(version2);
		if (null == model1 || null == model2) {
			JSONObject result = new JSONObject();
			result.put("report", "Comparison is illegal, not all models exist!");
			return result;
		}
		if (null == b1 || null == b2) {
			JSONObject result = new JSONObject();
			result.put("report", "Comparison is illegal, not all models have a base URL!");
			return result;
		}
		int whichCurrent = version.equals("current")? 1: // first one
			version2.equals("current")? 2 : // second one
				0; // none of them.

		String v1 = version, v2 = version2;
		if (false == v1.equals("current"))
			v1 = dateOfVersion(v1).toString();
		if (false == v2.equals("current"))
			v2 = dateOfVersion(v2).toString();
		return ARdfRepository.compareHistory(model1, model2, v1, v2, whichCurrent, b1, b2);
	}
	
	/**
	 * Answers with the base NS for a given version of the repository.
	 * @param version String version identification of the repository to use.
	 * @return String NS for the specific version of the repository.
	 */
	abstract String getBaseForVersion(String version); 

	/**
     * @param version - String which may be null and if not it is a version of the model to be displayed, rather than the 
     * current version.
     * @param changeSet - String which may be null or '' to indicate no Change-set, or any of the following:<ol>
     * <li>'changeSet' to indicate that the ChangeSet of the current revision is to be shown
     * <li>anything else - to indicate there is no change set to show.
     * </ol>
	 * @return
	 */
	public Model getModelForVersion(String version, String changeSet) {
		if ("current".equals(version))
			return getModel();
		else if (false == Strings.isNullOrEmpty(version)) {
			File oldVersion = new File(getFolder(), version);
			if ("changeSet".equals(changeSet))
				oldVersion = new File(getFolder(), "ChangeSet." + version);
			try {
				return Utils.modelFromFile(oldVersion.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else
			return null;
	}
	
	/**
	 * Answers whether there is a revision or a changeSet for a given revision of this repository.
     * @param version - String which may be null and if not it is a version of the model to be displayed, rather than the 
     * current version.
     * @param changeSet - String which may be null or '' to indicate no Change-set, or any of the following:<ol>
     * <li>'changeSet' to indicate that the ChangeSet of the current revision is to be shown
     * <li>anything else - to indicate there is no change set to show.
     * </ol>
	 * @return
	 */
	public boolean hasModelForVersion(String version, String changeSet) {
		if (version.equals("current"))
			return true;
		else {
			File oldVersion = new File(getFolder(), version);
			if ("changeSet".equals(changeSet))
				oldVersion = new File(getFolder(), "ChangeSet." + version);
			return oldVersion.canRead();
		}
	}
	
	/**
	 * Answers with a JSON object of contents of comparison report.
	 * of two model files.<br>
	 * The comparison uses a triples format which is simpler and easier to do.
	 * Future versions may do it more resource-based for more readable comparisons.
	 * @param file1 name indicating the "side 1" of the comparison May be "current" for the present 
	 * content of the repository.
	 * @param file2 name indicating the "side 2" of the comparison. May be "current" for the present 
	 * content of the repository.
	 * @param whichCurrent - int to show which of the two is current: 1 - first, 2 - second, 0 - none.
	 * @return String report of the comparison.
	 * @throws JSONException 
	 */
	public static JSONObject compareHistory(Model model1, Model model2, String v1, String v2, int whichCurrent, String b1, String b2) throws JSONException {
		JSONObject result = new JSONObject();
		StringBuffer report = new StringBuffer();
//		if (file1.equals(file2)) {
//			report.append("Need to specify two different versions to compare!");
//		}
//		Model model1 = null, model2 = null;
		Map<String, String> map1 = null, map2 = null;
//		try {
//			model1 = Utils.modelFromFile(file1);
			map1 = model1.getNsPrefixMap();
//			model2 = Utils.modelFromFile(file2);
			map2 = model2.getNsPrefixMap();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//			return null;
//		}

		try {
			boolean isLocalNS = true;
			String baseNs = map1.get("base");
			if (Strings.isNullOrEmpty(baseNs)) {
				baseNs = b1;
			}
			String baseNs2 = map2.get("base");
			if (Strings.isNullOrEmpty(baseNs2)) {
				baseNs2 = b2;
			}
			String nsHost = new URL(baseNs).getHost();
			String myHost =  Utils.getHost(null);
			isLocalNS = nsHost.equalsIgnoreCase(myHost);
			result.put("base", baseNs);
			map1.put("base1", baseNs); map1.remove("base");
			map2.put("base2", baseNs2); map2.remove("base");
//			String v1 = new File(file1).getName();
//			if (1 != whichCurrent)
//				v1 = dateOfVersion(v1).toString();
//			else if (1 == whichCurrent)
//				v1 = "current";
//			String v2 = new File(file2).getName();
//			if (2 != whichCurrent)
//				v2 = dateOfVersion(v2).toString();
//			else if (2 == whichCurrent)
//				v2 = "current";
			String triples1[], triples2[];
			StmtIterator stmtI1 = model1.listStatements(), stmtI2 = model2.listStatements();
			int null1 = 0, null2=2;
			List<String> stmt1 = new ArrayList<String>(), stmt2 = new ArrayList<String>();
			while (stmtI1.hasNext()) {
				Statement s = stmtI1.next();
				if (s.getSubject().isAnon() || s.getObject().isAnon()) {
					null1++;
//					Resource r = s.getSubject();
//					RDFNode o = s.getObject();
//					boolean rb = r.isResource();
//					boolean ob = o.isResource();
//					String ri = r.getURI();
//					String oi = null;
//					if (ob)
//						oi = o.asResource().getURI();
//					System.out.println(s);
				} else {
					stmt1.add(s.asTriple().toString());
//					if (s.asTriple().toString().contains(":-")) {
//						Resource r = s.getSubject();
//						RDFNode o = s.getObject();
//						boolean ob = o.isResource();
//						boolean or = r.isResource();
//						System.out.println(s);
//					}
				}
			}
			while (stmtI2.hasNext()) {
				Statement s = stmtI2.next();
				if (s.getSubject().isAnon() || s.getObject().isAnon())
					null2++;
				else
					stmt2.add(s.asTriple().toString());
			}
//			if (null1+null2 > 0) 
//				report.append("Note: " + null1 + " triples in first version, and " + null2 + " triples in second version ignored due to BLANK nodes\n");
			//if (file1.equals("current")) {
			triples1 = stmt1.toArray(new String[0]);
			triples2 = stmt2.toArray(new String[0]);
			//triples1 = Utils.modelToTriples(model1);
			//triples2 = Utils.modelToTriples(model2);

			if (isLocalNS)
				result.put("basePref", (1 == whichCurrent) ? "base1": ((2 == whichCurrent)? "base2" : "nobase"));
			else
				result.put("basePref", "nobase");
			result.put("first", v1);
			result.put("second", v2);
			result.put("firstG", triples1.length);
			result.put("firstN", null1);
			result.put("firstT", triples1.length + null1);
			
			result.put("secondG", triples2.length);
			result.put("secondN", null2);
			result.put("secondT", triples2.length + null2);

			Arrays.sort(triples1);
			Arrays.sort(triples2);
			int i2 = 0, i1 = 0;
			int same = 0;
			ResourceGroup resourceGroup = new ResourceGroup();
			while (i1 < triples1.length && i2 < triples2.length) {
				String l1 = triples1[i1], l2 = triples2[i2];
				int c = l1.compareTo(l2);
				if (c == 0) {
					i1++; i2++; same++; }
				else {
					if (same > 0)
						report.append("----\n"); //   skipped [" + same + "] matching triples.");
					same = 0;
					String triple;
					if (c > 0) {
						triple = ">: " + Utils.applyPrefixesOnResources(map2, l2);
//						result.append("\n>: ").append(l2);
						i2++;
					} else {
						triple = "<: " + Utils.applyPrefixesOnResources(map1, l1);
//						result.append("\n<: ").append(l1);
						i1++;
					}
					if (false == resourceGroup.add(triple)) {
						report.append(resourceGroup.write(map1, map2));
						resourceGroup.add(triple);
					}
				}
			}
			report.append(resourceGroup.write(map1, map2));
			result.put("match", same); 
//				report.append("\n   " + ((same == triples1.length && same == triples2.length)?"All":"") + "[" + same + "] triples match.");
//			else
			report.append("\n----\n"); //   skipped [" + same + "] matching triples.");
			String triples[] = null;
			int i= 0;
			String prefix = "";
			if (i1 >= triples1.length) { // triples1 is exhausted, now list all remaining in triples2 as extra
				triples = triples2;
				i = i2;
				prefix = ">: ";
			} else if (i2 >= triples2.length) { // triples2 is exhausted, now list all remaining in triples1 as removed
				triples = triples1;
				i = i1;
				prefix = "<: ";
			}
			if (null != triples) {
				for (; i < triples.length; i++) { 
					//				result.append("\n).append(prefix).append(triples2[i]);					
					if (false == resourceGroup.add(prefix + triples[i])) {
						report.append(resourceGroup.write(map1, map2));
						resourceGroup.add(prefix + triples[i]);
					}
				}
				report.append(resourceGroup.write(map1, map2)); // flush it.
			}
		} catch (Exception e) {
			e.printStackTrace();
				report.append("Failed to compare [" + v1 + "] with [" + v2 + "]");
		}
		result.put("report", Utils.forHtml(report.toString()));
		//map1.put("base2", map2.get("base2"));
		map1.putAll(map2);
		String prefixes = Utils.makePrefix4Query(map1);
	    result.put("prefixes", Utils.forHtml(prefixes));
		return result;
	}

//	public static void main(String args[]) throws Exception {
//		System.out.println(new File(".").getAbsolutePath());
//		Repository r = Repository.create("localhost", Database.SM_MODEL_FOLDER, "ra");
//		System.out.println(r.getDomain());
//		List<String> h = r.getHistory();
//		JSONObject c = r.compareHistory("ra.1.owl", "current");
//		System.out.println(c);
//	}

	/**
	 * Answers with the model internally in this repository for direct working with it and all of its
	 * methods.
	 * @return a Model.
	 */
	public Model getModel() {
		if (getLoadDate() < getDate())
			loadRepository();
		return repository;
	}
	/**
	 * Answers with the model internally in this repository for direct working with it and all of its
	 * methods.
     * @param version - String which may be null and if not it is a version of the model to be displayed, rather than the 
     * current version.
     * @param changeSet - String which may be null or '' to indicate no Change-set, or any of the following:<ol>
     * <li>'changeSet' to indicate that the ChangeSet of the current revision is to be shown
     * <li>anything else - to indicate there is no change set to show.
     * </ol>
	 * @return a Model.
	 */
	public Model getModel(String version, String changeSet) {
		Model model = getModelForVersion(version, changeSet);
		if (null == model)
			model = getModel();
		return model;
	}

	public void setNsPrefixes(Map<String, String> nsPrefixMap) {
		Map<String, String> prefixes = repository.getNsPrefixMap();
		for (String p : nsPrefixMap.keySet())
			if (!prefixes.containsKey(p)) {
				repository.setNsPrefix(p, nsPrefixMap.get(p));
				setDirty();
			}
	}

	/**
	 * Returns a resource related to another resource via a given property.
	 * If none is found - null is returned. If more than one is picked randomly.
	 * @param resource Resource from a tool.
	 * @param prop A String property.
	 * @return null or one of possibly several resources. If the object is not a resource, null is returned.
	 */
	public Resource getResoruceProperty(Resource resource, String prop) {
		Statement stmt = repository.getProperty(resource, repository.getProperty(prop));
		if (null != stmt) {
			try {
				return stmt.getObject().asResource();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Returns a property as a String, which is the property of the given resource.
	 * If none is found - null is returned. If more than one is picked randomly.
	 * @param resource Resource from a tool.
	 * @param prop A String property.
	 * @return null or one of possibly several objects.
	 */
	public String getProperty(Resource resource, String prop) {
		Statement stmt = repository.getProperty(resource, repository.getProperty(prop));
		if (null != stmt) {
				return stmt.getObject().toString();
		}
		return null;
	}

	public void add(Resource resource, Property predicate, RDFNode object) {
		repository.add(resource, predicate, object);
	}

	/**
	 * Saves the in memory model into persistent store; 
	 * Must be implemented by subclasses
	 */
	public void save() {
		if (false == isDirty())
			return;
		isDirty = false;
		doSave();
		loadDate = currentDate = getDate();
		myItem.markModified(currentDate);
	}
	
	/**
	 * Performs the actual save of the content, per concrete subclasses.
	 */
	public abstract void doSave();
	
	/**
	 * Saves the model into a file.<br>
	 * It keeps the current file as a backed version.
	 */
	void saveToFile() {
		OutputStream os = null;
		// Firstly, move file to save
		File f = new File(this.fileName);
		File fSav = new File(this.fileName + ".sav");
		if (fSav.canRead())
			fSav.delete();
		f.renameTo(fSav);
		boolean done = false;
		// Now save new content to that file
		try {
			os = new FileOutputStream(f);
			repository.write(os);
			done = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != os) {
					os.flush();
					os.close();
					os = null;
				}
				if (! done) { // failure. Need to revert situation
					f.delete();
					fSav.renameTo(f);
				}
			} catch (Exception e) {}
		}
		// Now arrange history:
		long t = fSav.lastModified();
		fSav.renameTo(new File(this.fileName + ".v-" + t));
	}
	/**
	 * Clears all the back revisions of models in the repository folder, and the folder itself.
	 * @return String message on the results of this operation.
	 */
	public String deleteRepository() {
		String result = "Repository for [" + this.myItem.getId() + "]";
		if (null == folder)
			return "Error: " + result + ": Cannot delete - no folder";
		String files[] = new File(folder).list();
		if (null != files) 
			for (String file : files) {
				new File(folder, file).delete();
			}
//		Database database = getMyItem().getDatabase();
//		File root = database.getFolder(getMyItem());
//		File f = folder;
//		while (false == root.equals(f)) {
//			File p = f.getParentFile();
//			if (false == f.delete())
//				break;
//			f = p;
//		}
		if (new File(folder).exists()) {
			boolean done = new File(folder).delete();
			return  (done?"":"Error: ") + result + ":" + (done? "":" Not") + " Deleted.";
		} 
		return result + " was never persisted.";
	}

	public AModelRow getMyItem() {
		return myItem;
	}
	
	/**
	 * Utility answering with the date as coded in a back version of a file.
	 * @param version String file name which is a back version of a repository.
	 * @return Date as coded in that file name.
	 */
	public static Date dateOfVersion(String version) {
		int p = version.lastIndexOf(".v-");
		if (p < 0)
			return new Date();
		String timeStr = version.substring(p).substring(3);
		Date date = new Date(Long.parseLong(timeStr));

		return date;
	}


	/**
	 * Helper class to manage a list of triples that may belong to a single resource, so it is
	 * pretty printed more legibly.<br>
	 * Used for model comparison.
	 */
	private static class ResourceGroup {
		List<String> triples = new ArrayList<String>();
		String resource = null;
		int cnt1 = 0;
		int cnt2 = 0; 
		public String write(Map<String, String> map1, Map<String, String> map2) {
			if (null == resource)
				return "";
			StringBuffer sb = new StringBuffer();
			Map<String, String> map = map1;
			if (cnt1 > 0 && cnt2 > 0 )  // mixed adds and misses
				sb.append("Resource modified [");
			else if (cnt1 > 0) {
				sb.append("Resource removed [");
			} else if (cnt2 > 0) {
				sb.append("Resource added [");
				map = map2;
			} else
				return "";
			sb.append(Utils.applyPrefixesOnResources(map, resource)).append(" ]:\n");
			String prevPredicate = "";
			String prevValue = "";
			String prevPref = "";
			for (String triple : triples) {
				//triple = Utils.applyPrefixesOnResources(map, triple);
				String pref = triple.substring(0,3); // E.g., "<: "; 
				//sb.append(
				String parts[] = triple.split(" ");
				if (prevPredicate.equals(parts[2]) && false == prevPref.equals(pref)) {
					String value1 = Utils.concat(parts, " ", 3);
					String value2 = prevValue;
					if (prevPref.startsWith("<")) {
						value1 = prevValue;
						value2 = Utils.concat(parts, " ", 3);
					}
					String v1 = value1;
					if (Utils.isURL(v1))
						v1 = Utils.applyPrefixesOnResources(map1, v1);
					String v2 = value2;
					if (Utils.isURL(v2))
						v2 = Utils.applyPrefixesOnResources(map2, v2);
					sb.append("<>        ").append(parts[2]).append("    ").
						append(v1).
						append("  -->  ").
						append(v2).
						append(" \n");
					prevPredicate = "";
					continue;
				} else if (false == "".equals(prevPredicate)) {
					map = map1;
					if (prevPref.contains(">"))
						map = map2;
					sb.append(prevPref + "        ").
						append(Utils.applyPrefixesOnResources(map, prevPredicate)).append("  ").
						append(Utils.applyPrefixesOnResources(map, prevValue)).append(" \n");
					prevPref = pref;
					prevPredicate = parts[2];
					prevValue = Utils.concat(parts, " ", 3);//parts[3];
				} else {
					prevPref = pref;
					prevPredicate = parts[2];
					prevValue = Utils.concat(parts, " ", 3);//parts[3];
				}
//				sb.append(pref + "        ").append(parts[2]).append("  ").append(parts[3]).append("\n");
			}
			if (false == "".equals(prevPredicate)) {
				map = map1;
				if (prevPref.contains(">"))
					map = map2;
				sb.append(prevPref + "        ").
					append(Utils.applyPrefixesOnResources(map, prevPredicate)).append("  ").
					append(Utils.applyPrefixesOnResources(map, prevValue)).append(" \n");
			}
			reset();
			return sb.toString();
		}
		/**
		 * Utility to help with model comparison.
		 * Assumes triple has this format "<: resource predicate subject";
		 * @param triple
		 * @return
		 */
		public boolean add (String triple) {
			String parts[] = triple.split(" "); // 4 parts: "<:", resoruce, predicate, object.
			String c[] = parts[1].split(":");
			if (null != resource && c.length > 1 && false == resource.equals(c[1]))
				return false;
			resource = c[c.length-1];
			if (parts[0].startsWith("<")) cnt1++;
			else cnt2++;
			triples.add(triple);
			return true;
		}
		public void reset() {
			cnt1 = 0;
			cnt2 = 0;
			resource = null;
			triples.clear();
		}
	}

//	/**
//	 * Answers with a report that migrates all ontologies to new organization, and that renames all
//	 * Ports to use "group" folder which matches their item's getCollection() method resuls.
//	 * @param host - host name to be used in repositories
//	 * @return String message repost on progress.
//	 */
//	public static String migrateToV2_4(Database db, File oldFolder) {
//		String msg = "";
//		// Go over all the ontologies and rules:
//		List<Ontology> ontologies = db.getOntologies();
//		List<RuleSet> ruleSets = db.getRules();
//		List<AModelRow> models = new ArrayList<AModelRow>();
//		models.addAll(ontologies);
//		models.addAll(ruleSets);
//
//		for (AModelRow model : models) {
//			msg += model.migrateToV2_4(db, oldFolder);
//		}
//		// Now clean other files in Ontologgy/ and RuleSet/
//		msg += "Cleaning illegal entries in main item folders for Ontology and RuleSet:\n";
//		File ontologyFolder = new File(oldFolder, "Ontology");
//		ontologyFolder.mkdirs();
//		File rulSetFolder = new File(oldFolder, "RuleSet");
//		rulSetFolder.mkdirs();
//		String itemFolders[] = ontologyFolder.list();
//		for (String folder : itemFolders) {
//			ADatabaseRow item = db.getItem(folder);
//			if (null == item) { // illegal folderO
//				File d = new File(ontologyFolder, folder);
//				msg += "\tCleaning leftover folder and files [" + d.getAbsolutePath() + "].\n";
//				Utils.deleteFolder(d, true);
//			}s
//		}
//		itemFolders = rulSetFolder.list();
//		for (String folder : itemFolders) {
//			ADatabaseRow item = db.getItem(folder);
//			if (null == item) { // illegal folder
//				File d = new File(rulSetFolder, folder);
//				msg += "\tCleaning leftover folder and files [" + d.getAbsolutePath() + "].\n";
//				Utils.deleteFolder(d, true);
//			}
//		}
//
//		return msg;
//	}

	/**
	 * Used only for migration.
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	public String getEtag(){
		return Long.toString(new File(getFileName()).lastModified());
	}

	public Object getDateString() {
//		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ITALY);
//		String d = df.format(new Date(getDate()));
		String od = new Date(getDate()).toString();
		return od;
	}
	public long getDate() {
		return new File(getFileName()).lastModified();
	}
}

