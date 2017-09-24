
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

package com.ibm.dm.frontService.sm.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.data.ADatabaseRow.STATUS;
import com.ibm.dm.frontService.sm.data.Port.PortType;
import com.ibm.dm.frontService.sm.interceptor.HaifaSemanticMediator;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.service.SmContainer;
import com.ibm.dm.frontService.sm.service.SmManager;
import com.ibm.dm.frontService.sm.service.SmPlainMediationModule;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * "Database" of the configuration data.
 * There are 3 sets of item types, all in internal classes: ADatabaseRow is an abstract super
 * class of all, Config, Ontology and RuleSet are 3 types of items. <br>
 * To extend with a new field, the following need to be updated:
 * <ul>
 * <li>Database.getFields()
 * <li>Database.${itemType}editebleFields - if the field is editable.
 * <li>Add field variable in ${itemType}
 * <li>${itemType}.get${field-name}()
 * <li>${itemType}.set${field-name}( new value)
 * <li>${itemType}.getField(String fieldName)
 * <li>${itemType}.setField (String fieldName, String value)
 * <li>${itemType}.toJasonObject()
 * <li>${itemType}.setKey(String fieldName)
 * <li>constructor: ${itemType}(JsonObject data)
 * </ul>
 * Than, go and update the GUI templates accordingly: both display and editing
 * of a row for an item.
 *
 * @author shani
 */
public class Database extends DataBaseData
{
	String version = "2.6";

	public static final String SM_NS = IConstants.SM_PROPERTY_NS; //"http://com.ibm.ns/haifa/sm#";
    private static final long                  serialVersionUID   = 1L;
    private final static String                DATABASE_FILE_NAME = "Database.json";
    protected static String mProject = "";
    /*
     * need only at runtime must be recreated on load and discarded before save
     * */
    public final transient Map<String, ADatabaseRow> map;
    /**
     * transient field that need to decide if need to save - consider replace by the changeListener from JDK
     */
    protected transient boolean                  dirty              = false;

    /**
     * Flag to control how much info in database
     * is viewed. When true, only active items are shown. Otherwise, all are shown.<br>
     * i.e., active items are such whose status is NOT "archived".
     * this is relevant for the DTO to JSON - need for the GUI
     * check with Eyal if it can become a Cookie with expiration date?
     * OR this is part of visualization augmenting data
     */
    // We are using variables for this purpose. Next variable is deprecated.
//    private String showActive = null; // meaning false;

    public static enum Vars {
    	contentType,
    	filterVar,
    }
	private JSONObject mVars = new JSONObject();
    private final transient Map<String, Set<String>> tags;

	public static String       SM_MODEL_FOLDER       = "SM.model.rdf";

	private static String mHost = "localhost";
	private static boolean mHostSet = false;
	private static int mPort = 8080;
	private static String mScheme = "http";
	protected static String mDbName = null;
	private static String mBaseName = null;
	private static String mName = null;
	private static String mDotExePath = null;
	
	static {
/*		InputStream is = null;
		try {
			is = Runtime.getRuntime().exec("hostname").getInputStream();
			mHost = Utils.stringFromStream(is).trim();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != is)
				try { is.close(); } catch (IOException e) {}
		}
*/		
//		Map<String, String> ev = System.getenv();
//		System.out.println("Accessing environment variables: [" + ev.keySet().size() + "]:\n");
//		for (String k: ev.keySet()) System.out.println(k + ": " + ev.get(k));
		String dbNameParam = System.getenv("semantic_mediation_container_dbName");
		mBaseName = System.getenv("semantic_mediation_container_baseName");
		mName = System.getenv("semantic_mediation_container_name");
		
		if (Strings.isNullOrEmpty(dbNameParam))
			dbNameParam = "database";
		mDbName = (dbNameParam + mProject).toLowerCase();
		if (Strings.isNullOrEmpty(mName))
			mName = mDbName.toUpperCase();
		if (false == Strings.isNullOrEmpty(mBaseName)) {
			String msg = "";
			if (null != (msg = Utils.isLegalURI(mBaseName, true))) {  // do a simple URL verification - so path is not needed.
				System.err.println("baseName [" + mBaseName + "] is illegal [" + msg + "].");
				mBaseName = null;
			} else {
				System.out.println("Will use this for base URL of RDF model repositories [" + mBaseName + "]");
			}
		}
		System.out.println("----- static initialization -----\n" + //mBaseName=" + mBaseName +
				"\nmName=" + mName + "\nmDbName=" + mDbName + "\n----------------");

	}
	
	/**
	 * Answers with true if the DOT tool is install and operative.
	 * @return
	 */
	public boolean canDoGraphics() {
		return null != mDotExePath;
	}
	
	/**
	 * Sets variouse state static variables to serve this server throughout the execution,
	 * based on the first Http call to the server.
	 * @param request
	 * @throws Exception 
	 */
	public static void setHost(HttpServletRequest request, Database db) throws Exception {
		String name = request.getServletContext().getInitParameter("semantic_mediation_container_name");
		String dbName = request.getServletContext().getInitParameter("semantic_mediation_container_dbName");
		String baseName = request.getServletContext().getInitParameter("semantic_mediation_container_baseName");
		if (! Strings.isNullOrEmpty(dbName)) { 
			System.out.println("Changing mDbName from [" + mDbName + "] to [" + dbName + "]");
			mDbName = dbName; 
		}

		if (Strings.isNullOrEmpty(name) && false == Strings.isNullOrEmpty(dbName))
			name = dbName;

		if (! Strings.isNullOrEmpty(name)) { 
			System.out.println("Changing mName from [" + mName + "] to [" + name + "]");
			mName = name.toUpperCase(); 
		}

		if (! Strings.isNullOrEmpty(baseName)) { 
			System.out.println("Changing mBaseName from [" + mBaseName + "] to [" + baseName + "]");
			mBaseName = baseName; 
		}

		mScheme = request.getScheme();
		if (mHost.equals("localhost") && false == mHostSet) {
			mHost = request.getServerName();
			mHostSet = true;
		}
		int sp = request.getServerPort();
		if (mPort == 8080 && sp > 0)
			mPort = sp;
		if (Strings.isNullOrEmpty(mBaseName)) {
			mBaseName = request.getRequestURL().toString();
			String path = request.getRequestURI();
			System.out.println("path = " + path);
			if (mBaseName.endsWith(path))
				mBaseName = mBaseName.replace(path, "");
			System.out.println("BaseName for resources is set based on request URL [" + mBaseName + "]");
		}
		System.out.println("----- Session initialization -----\nmBaseName=" + mBaseName +
				"\nmName=" + mName + "\nmDbName=" + mDbName + "\nmHost=" + mHost + "\nmPort=" + mPort + "\n----------------");

		/*
		String baseName = request.getRequestURL().toString();
		String bpath = request.getRequestURI();
		System.out.println("path = " + bpath);
		if (baseName.endsWith(bpath))
			baseName = baseName.replace(bpath, "");
		if (baseName.endsWith("/"))
			baseName = baseName.substring(0, baseName.length()-1);
		if (Strings.isNullOrEmpty(AbstractRDFReadyClass.baseURL)) {
			// This has not been set yet. It will be used to define the URL name space for
			// resources generated in this container.
			db.setBaseURL(baseName);
			db.dirty = true;
		} else {
			if (! baseName.equals(AbstractRDFReadyClass.baseURL)) {
				System.err.println("Error: Base URL [" + AbstractRDFReadyClass.baseURL + "] differs from request URL [" + baseName + "] and is not changed.\n !! Aborting Client session !!");
				throw new Exception("Client called url [" + baseName + "]. Must use host name as in the following base URL [" + AbstractRDFReadyClass.baseURL + "]");
			}
		}
		*/
//		if (! Strings.isNullOrEmpty(baseName)) { 
//			System.out.println("Changing mBaseName from [" + mBaseName + "] to [" + baseName + "]");
//			mBaseName = baseName; 
//		}

//		if (Strings.isNullOrEmpty(mBaseName)) {
//			mBaseName = request.getRequestURL().toString();
//			String path = request.getRequestURI();
//			System.out.println("path = " + path);
//			if (mBaseName.endsWith(path))
//				mBaseName = mBaseName.replace(path, "");
//			System.out.println("BaseName for resources is set based on request URL [" + mBaseName + "]");
//		}
		
//		String explainDot = null;
		String dotP = null;
		for (String aDot: new String[] {"dot", "/usr/local/bin/dot", "/usr/bin/dot"}) {
			if (Utils.checkExec(aDot)) { 
				dotP = aDot;
				break;
			}
		}
		if ( null != dotP) {
			System.err.println("Success: Graphviz dot command is installed on the platform as [" + dotP + "].");
			mDotExePath = dotP;
		} else {
			System.err.println("Warning: dot command is not executable. looking for servlet context.");
			String path = request.getServletContext().getRealPath("Graphviz");
			if (Strings.isNullOrEmpty(path)) {
				System.err.println("Error: No servlet context for Graphviz. Assuming the tool is missing.");
				mDotExePath = null;
			} else {
				File dotPath = null;
				File pathF = new File(path);
				for (String aDot: new String[] {"dot", "dot.sh", "dot.exe", "dot.bat"}) {
					if (new File(pathF, aDot).canExecute()) { 
						dotPath = new File(pathF, aDot);
						break;
					}
				}

				if (null != dotPath && !Utils.checkExec(dotPath.getAbsolutePath()) || null == dotPath) {
					System.err.println("Error: dot command in the servlet path /Graphviz is not executable. Assuming tool is missing.");
					// one more check for OSX
						mDotExePath = null;
				} else {
					System.err.println("Success: Graphviz dot tool is installed and executable.");
					mDotExePath = dotPath.getAbsolutePath();
				}
			}
		}
		System.out.println("----- Session initialization -----\nbaseURL=" + db.getBaseURL() +
				"\nmName=" + mName + "\nmDbName=" + mDbName + 
				"\nDOT path="+ mDotExePath + 
				"\nname: " + Database.getName() + "\nroot: " + Database.getRootName() + 
				"\n----------------");
	}
	
//	/**
//	 * Answers with the URL of base resources served by this service provider.
//	 * @return String that comes from environment or the present request.
//	 */
//	public static String getBaseName() {
//		return ""; //getBaseURL();
//	}
	
	/**
	 * Constructs a host name: <ul>
	 * <li> alone (getHost()), or 
	 * <li>the host with port only (getHost(true)) or
	 * <li>the scheme, host and port combined (getHost(true, true)), or 
	 * <li>the scheme and host, but not port (getHost(false, true)).
	 * </ul>
	 * @param none, one or two booleans.
	 * @return String or null.
	 */
	public static String getHost(boolean...full) {
		if (null == mHost)
			return null;
		boolean withPort = full.length > 0 ? full[0] : false;
		boolean withScheme = full.length > 1 ? full[1] : false;
		if (!withPort) 
			return mHost;
		if (withPort && !withScheme)
			return mHost + ":" + mPort;
		if (withPort && withScheme)
			return mScheme + "://" + mHost + ":" + mPort;
		return null; // never happens
		
		//return  (full2 ? (mScheme + "://") : "") +
		//		mHost + (full1 ? (":" + mPort) : "") +  (full2 ? "/dm/sm" : "");
	}

	/**
	 * Method to inquire what kind of a Database we use: FS for file system, or DB for database.
	 * @param db Database to be inquired.
	 * @return String: FS or DB.
	 */
	public static String databaseType(Database db) {
		if (db instanceof CloudantDatabase)
			return "DB";
		else return "FS";
	}
	
	Database() {
		super();
		tags = new HashMap<String, Set<String>>();
		map  = new HashMap<String, ADatabaseRow>();
		mProject = "";
	}


	public String getProject() {
		return mProject;
	}
    /**
     * Scans all items for their tags and adds them to the set of tags of the database, trimmed
     * and lower-cased. Resets that collection beforehand.
     */
    public void setTags() {
    	tags.clear();
        Collection<ADatabaseRow> items = map.values();
        for (ADatabaseRow item : items) {
			String tagsValues = item.getField("tags");
	    	if (null == tagsValues || "".equals(tagsValues))
	    		continue;
	    	String tagVals[] = tagsValues.split(",");
	    	for (String tag : tagVals) {
	    		tag = tag.trim().toLowerCase();
	    		if (false == tags.containsKey(tag))
	    			tags.put(tag, new HashSet<String>());
	    		tags.get(tag).add(item.getId());
			}
		}
	}

    /**
     * Answers with the set of tags for all the items.
     * @return
     */
    public Map<String, Set<String>> getTags() {
    	return tags;
    }
    /**
     * Answers with the set of selected tags for the database.
     * @return
     * @throws JSONException 
     */
    public String getFilter() throws JSONException {
    	return Utils.stringify(Utils.safeGet(mVars, "filterVar"));
    }
	/**
     * Answers true if the database is marked dirty or any of its items are.
     * @return boolean
     */
    public boolean isDirty()
    {
    	StringBuffer report = new StringBuffer();
        if (dirty) report.append("Database;");
        for (ADatabaseRow item : map.values())
        {
            if (item.isDirty()) {
            	report.append(item.getId());
            	if (item instanceof AModelRow) {
            		AModelRow mItem = (AModelRow)item;
                    if (mItem.hasModelRepository() && mItem.getModelRepository().isDirty()) {
                    	mItem.getModelRepository().save();
                      	report.append("[R-saved]");
                    }
                }
                report.append(";");
            }
        }
        if (report.length() > 0) {
        	System.err.println("Database.isDirty()=[" + report.toString() + "]");
        }
        return report.length() > 0;
    }

    /**
     * Ensures all dirty flags in the database and its items are reset to false.
     */
    public synchronized void clearDirty()
    {
        dirty = false;
        for (ADatabaseRow item : map.values())
        {
            item.dirty = false;
        }
    }

    /**
     * Saves a copy of the database, making a backup copy first to protect against failures.
     * <br>
     * Dirty flags are reset after that.
     */
    public synchronized void save()
    {
    	try {
//    		System.out.println("Not doing dump of JSON database now."); //"map:\n" + map);
    		System.out.print("Saving database [project:" + mProject + "]...");
    		if (false == isDirty()) {
        		System.out.println("Database NOT saved!");
    			return;
    		}
    		boolean saved = this.store();
    		System.out.println("Database " + (saved?"":"Not ")+ "saved!");
    	} catch (Exception e) {
    		System.out.println("Database failed to save!. " + e.getClass().getName() + " [ " + e.getMessage() + "]");
    		System.out.println(toJson());
    	}
    	// now make sure the dirty flags are all reset after this has been saved.
    	clearDirty();
    }

 boolean store() throws IOException {
	//		new ObjectOutputStream(new FileOutputStream(new File(DATABASE_FILE_NAME))).writeObject(data);
	// Doing "double buffer" to ensure file data is not corrupted due to disk space problems.
	// 1. save in a new file that is first cleared.
	String data = toJson(); 
	File tmp = new File(getDatabaseRoot(), DATABASE_FILE_NAME + ".new");
	tmp.mkdirs();
	if (tmp.canWrite())
		tmp.delete();
	if (false == tmp.createNewFile()) {
		System.out.println("Database failed to save!. Could not create new file [" + tmp.getAbsolutePath() + "]");
		return false;
	}
	FileWriter fw = new FileWriter(tmp);
	fw.append(data);
	fw.close();
	File old = new File(getDatabaseRoot(), DATABASE_FILE_NAME);
	File sav = new File(getDatabaseRoot(), DATABASE_FILE_NAME + ".sav");
	if (sav.exists() && false == sav.delete())
		System.out.println("Failed to delete " + sav.getAbsolutePath());
	if (sav.canRead())
		System.out.println("Failed to delete " + sav.getAbsolutePath());
	if (old.exists() && false == old.renameTo(sav))
		System.out.println("Failed to rename " + old.getAbsoluteFile() + " -> " + sav.getAbsolutePath());
	if (false == tmp.renameTo(old)) {
		System.out.println("Failed to rename " + tmp.getAbsoluteFile() + " -> " + old.getAbsolutePath());
		return false;
	}
	return true;
}


//    private Database()
//    {
//        counter = 100;
//        ontologies = new ArrayList<Ontology>();
//        rules = new ArrayList<RuleSet>();
//        ports = new ArrayList<Port>();
////        configs = new ArrayList<Config>();
//        mediators = new ArrayList<Mediator>();
//        // dirty = true; // NOT TRUE: for a new database - it is dirty to it will be saved at least in its initial state
//    }

    /**
     * Answers the Ontology for a given String URI name space.
     * uses getOntologyByClassUri(URI).
     * @param classUri - URI of the name space URI
     * @return Ontology for the URI.
     */
    public Ontology getOntologyByClassUri(URI classUri) {
//    	if (Strings.isNullOrEmpty(classUri))
//    		return null;
//    	try {
			return getOntologyByClassUri(classUri.toString());
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
//		return null;
    }
    /**
     * Answers the Ontology for a given URI name space.
     * @param classUri - String of the URI of the name space URI
     * @return Ontology for the URI.
     */
    public Ontology getOntologyByClassUri(String classUri)
    {
        return (Ontology)getItemByClassUri(classUri, getOntologies());
//        for (Ontology ontology : ontologys)
//        {
//            if (false == ontology.getStatus().equals(STATUS.READY))
//            	continue;
//            if (Utils.compareOntologyNamespace(ontology.getNameSpaceUri().toString(), classUri.toString()))
//            	return ontology;
//        }
//        return null;
    }
    
    /**
     * answers with an item (ontology or rules) based on the URI of the item.
     * @param classUri
     * @param models candidates to be scanned.
     * @return an item if a "ready" item is found, or null is not.
     */
    private AModelRow getItemByClassUri(String classUri, List<? extends AModelRow> models) {
        if (null != classUri) for (Object o : models)
        {
        	AModelRow model = (AModelRow)o;
            if (false == model.isReady())
            	continue;
            if (Utils.compareOntologyNamespace(model.getNameSpaceUri().toString(), classUri.toString()))
            	return model;
        }
        return null;
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception
    {
        Database db = Database.create(null);
        System.out.println(db.createId("Exm"));
        String ontologyId = db.createOntology().getId();
        String ruleSetId = db.createRuleSet().getId();
//        String configId = db.createConfig().getId();
        String htmlPage = Utils.getHtmlTemplate(null);
        System.out.println("Result:\n" + db.toJson().toString() + "------------\n");
        htmlPage = SmManager.embedData(db, db.getOntologies(), htmlPage, "_ontologies_", "_/ontologies_", db.isShowActive(), db.getOntologies());
        htmlPage = SmManager.embedData(db, db.getRules(), htmlPage, "_rules_", "_/rules_", db.isShowActive(), db.getOntologies());
//        htmlPage = SmManager.embedData(db, db.getConfigs(), htmlPage, "_configs_", "_/configs_", db.isShowActive(), null);
        System.out.println(htmlPage);
        JSONObject page = new SmManager(db).edit(ontologyId, db, null, false);
        System.out.println("Ontology:\n" + page);
        page = new SmManager(db).edit(ruleSetId, db, null, false);
        System.out.println("RuleSet:\n" + page);
//        page = SmManager.edit(configId, db);
//        System.out.println("Config:\n" + page);

        db.save();
    }

//    private static void saveVersion(JSONObject data, String version)
//    throws IOException
//    {
//    	File pwd = new File(".");
//    	File [] files = pwd.listFiles();
//
//    	boolean found = false;
//		for (File file : files) {
//			if (file.getName().startsWith("Database." + version + "."))
//				found = true;
//		}
//
//		if (found)
//			return;
//
//		File saved = new File("Database." + version + "." + new Date().getTime() + ".json");
//		FileWriter writer = new FileWriter(saved);
//		writer.write(data.toString());
//		writer.close();
//    }

    /**
     * Migration is now disabled. Setting to version 2.6 and incompatibility with earlier versions.
     * @throws Exception
     */
    private void migrate() 
    {
    	String msg = "";
//    	if (version.equals("2.0"))
//    		msg += "In version 2.0, migrating to 2.1:\n " + migrateToV2_1();
//    	if (version.equals("2.1"))
//    		msg += "In version 2.1, migrating to 2.2:\n " + migrateToV2_2();
//    	if (version.equals("2.2"))
//    		msg += "In version 2.2, migrating to 2.3:\n " + migrateToV2_3();
//    	if (version.equals("2.3"))
//    		msg += "In version 2.3, migrating to 2.4:\n " + migrateToV2_4();
//    	if (version.equals("2.4"))
//    		msg += "In version 2.4, migrating to 2.5:\n " + migrateToV2_5();
//    	if (version.equals("2.5")) 
//    		msg += "In version 2.5, migrating to 2.6:\n " + migrateToV2_6();
    	if (false == version.equals("2.6")) {
//        	if (false == Strings.isNullOrEmpty(msg))
//        		SmManagerLog.create(this).reportMigration(msg);
    		msg += "Not In version 2.6. Cannot work with this database."; //, no further migration to 2.7 needed."; // + "Not implemented yet!\n"; //migrateToV2_2(db);
    	}
    	System.out.println("Migration report:\n" + msg);
    	save();
    }

//	private void updateRowViaRepositoryHelper(StringBuffer sb, ADatabaseRow p) {
//		long pm = p.getDateModifiedAsLong();
//		ARdfRepository r = p.getModelRepository();
//		long lm = r.getDate();
//		if (lm > pm) {
//			p.markModified(lm);
//			sb.append(p.getCollectionName() + p.getDisplayId() + 
//					" last modified updated " + new Date(pm) + " --> " + new Date(lm) + ".\n");
//		}
//	}
//    private String migrateToV2_6() {
//		StringBuffer sb = new StringBuffer();
//		for (Port p : getPorts(PortType.repository)) 
//			updateRowViaRepositoryHelper(sb, p);
//		for (AModelRow p: getOntologies())
//			updateRowViaRepositoryHelper(sb, p);
//		for (AModelRow p: getRules())
//			updateRowViaRepositoryHelper(sb, p);
//			
//    	dirty = true;
//    	version = "2.6";
//		return sb.toString();
//	}

//	private String migrateToV2_5() {
//		String msg = Repository.migrateToV2_5(this);
//    	dirty = true;
//    	version = "2.5";
//		return msg;
//	}

//	private String migrateToV2_4() {
//		String msg = ModelRepository.migrateToV2_4(this, new File(getFolder()));
//    	dirty = true;
//    	version = "2.4";
//		return msg;
//	}

//	/**
//     * Migrates the system from V2.0 to V2.1
//     * @return String msg report on the migration.
//     * @throws Exception
//     */
//    private String migrateToV2_1() throws Exception {
//    	// Add PrtAR if not included alraeady
//    	String msg = "Migrating from version " + version + " to version 2.1.\n";
//   		String host = Utils.getHost(null, false);
//
//		msg += "\tUsing host [" + host + "]\n";
//		msg += makeArPort();
//
//    	// Now clean all repositories in the wrong place:
//    	msg += Repository.migrateToV2_1(this);
// //   	db.version = "2.1";
//    	dirty = true;
//    	version = "2.1";
//		return msg;
//	}

    /**
     * Answers with a report on the ensuring of the existance of the built in AR port
     * @return String.
     */
    private String makeArPort() {
    	String msg = "";
    	Port prtAR = getPort(SmBlobs.ATTACHMENTS_PORT_ID);
    	if (null == prtAR) {
    		msg += "\tAdding PrtAR repository\n";
    		prtAR = createPort(Port.PortType.repository.toString(), SmBlobs.ATTACHMENTS_PORT_ID);
    		prtAR.setAccessName("attachments");
    		Date now = new Date();
    		prtAR.setLastModified(now);
    		prtAR.setDateCreated(now);
    		prtAR.setName("Attachments Repsitory");
    		prtAR.markModified();
    		prtAR.setPermanent(true);
    		prtAR.setVersion("1.02");
    		prtAR.setStatus(STATUS.READY);
//     		Repository repository = Repository.create(/*host, SmContainer.getFolder(),*/ prtAR);
//           	msg += "\t\t" + repository.clearRepository() + "\n";
    	} else
    		msg += "\tPrtAR repository already exists.\n";
    	return msg;
	}

//	/**
//     * Migrate the system from V2.1 to V2.2.<ol>
//     * <li> Move PrtAR port to the top of the Prt list.
//     * </ol>
//     * @return String msg report on the migration.
//     */
//	private String migrateToV2_2() {
//        String msg = "Attempting migrating to 2.2\n";
//		Port prtAR = getPort(SmBlobs.ATTACHMENTS_PORT_ID);
//		if (null == prtAR){
//			msg+= "\tPrtAR is missing - something is very bad!\nRedoing it:\n";
//			msg += makeArPort();
////			return msg;
//		}
//		int idx = ports.indexOf(prtAR);
//		if (idx > 0) {
//			msg += "\tMoving " + SmBlobs.ATTACHMENTS_PORT_ID + " from [" + idx + "] to top of list.\n";
//			ports.remove(prtAR);
//			ports.add(0, prtAR);
////			List<Port> temp = new ArrayList<Port>();
////			temp.add(prtAR);
////			temp.addAll(ports);
////			ports = temp;
//		}
//        version = "2.2";
//        dirty = true;
//        save();
//        return msg + "--> Now in version [" + version + "]\n";
//	}
//
//    /**
//     * Migrate the system from V2.2 to V2.3.<ol>
//     * <li> Remove old ontologies representing the built in and permanent ontology OntSM.
//     * </ol>
//     * @return String msg report on the migration.
//     */
//    private String migrateToV2_3() {
//        String msg = "Attempting migrating to 2.3\n";
//		Ontology ontSM;
//		ontSM = SmOntology.create(this, null);
//		if (null == ontSM){
//			msg+= "\tOntSM is missing - something is very bad!\n";
//			return msg;
//		}
//		URI ns = ontSM.getNameSpaceUri();
//		List<Ontology> clone = new ArrayList<Ontology>(ontologies.size());
//		clone.addAll(ontologies);
//		for (Ontology ontology : clone) {
//			if (ontology == ontSM)
//				continue;
//			URI ons = ontology.getNameSpaceUri();
//			if (null != ons && 0 == ns.compareTo(ons)) {
//				msg += "Removing [" + ontology.getId() + "] having same SN as OntSM [" + ns + "].\n";
//				String m = delete(ontology.getId(), true);
//				if (Strings.isNullOrEmpty(m) || false == m.startsWith("Error"))
//					msg += "\t --> removed. " + m + "\n";
//				else
//					msg += m + "\n";
//			}
//		}
//        version = "2.3";
//        dirty = true;
//        save();
//        return msg + "--> Now in version [" + version + "]\n";
//	}

	// Singleton for this object.
    protected static Map<String, Database> mDatabase = new HashMap<String, Database>();

    public synchronized static Database create(HttpServletRequest request) throws Exception  
    {
//    	if (pProject.length ==0) {
//    		String msg = "Database.create() was called";
//    		System.err.println(msg);
//    		Exception e = new Exception(msg);
//    		e.printStackTrace();
//    	}
    	Database db = mDatabase.get(mProject); 
    	if (null != db) {
    		return db;
    	}

//    	Properties env = System.getProperties();
//    	System.out.println("Environment:\n");
//    	for (Object v: env.keySet().toArray()) {
//    		System.out.println(v.toString() + ": " + env.get(v));
//    	}

    	// Notes
    	// 1. Need to attempt to read file as object (backward compatibility)
    	// 2. Need to attempt to patch older JSON format (backward compatibility)
    	// 3. Cannot fall-through into "new Database()" on any exception - Any error will erase the existing data!
    	// 4. GSON seems not to enforce field's existence:
    	//    If some important field is absent, or misspelled, the code won't notice...
   		db = new Database();
//    	File f = db.getDatabaseFile(); //Root(project)); //DATABASE_FILE_NAME);
//    	FileReader in = null;
//    	if (false == f.canRead()) {
//        	System.out.println("Creating a whole new Database file [" + f.getAbsolutePath() + "]");
//    		db.dirty = true;
//    		db.save();
//    	}
    	Reader data = null;
    	try {
    		
//        	System.out.println("Reading Database file [" + f.getAbsolutePath() + "]");
//    		in = new FileReader(f);
    		data = db.load();
//    		//    			data = (JSONObject) new ObjectInputStream(in).readObject();
//    		data = JSONObject.parse(in);
//    		in.close();
    		db = U.getGson().fromJson(data, Database.class);
    		data.close(); data=null;
    		db.finalizeDb();
    		setHost(request, db);
    	} finally {
    		if (null != data)
				try {
					data.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    	}
    	if (null != db)
    		mDatabase.put(mProject, db);
    	return db;
    }
    
    protected void finalizeDb() {
		recreateTheMap();
		postCreate();
		postCreate2();
		setTags();
		migrate();
	}


	Reader load() throws IOException {
    	File f = this.getDatabaseFile(); //Root(project)); //DATABASE_FILE_NAME);
    	if (false == f.canRead()) {
        	System.out.println("Creating a whole new Database file [" + f.getAbsolutePath() + "]");
    		this.dirty = true;
    		this.save();
    	}
    	try {
        	System.out.println("Reading Database file [" + f.getAbsolutePath() + "]");
    		return new FileReader(f);
//    		return new 
//    		JSONObject data = null;
//    		//    			data = (JSONObject) new ObjectInputStream(in).readObject();
//    		data = JSONObject.parse(in);
//    		in.close();
//    		return data;
    	} finally {
//    		if (null != in)
//    			try { in.close();} catch (Exception e2) {};

    	}
    	
	}


	private File getDatabaseFile() {
		File root = getDatabaseRoot();
		return new File(root,DATABASE_FILE_NAME);
	}

	static private String projectName = null;
	public final static String getProjectName() {
		if (null == projectName) 
			projectName = Utils.getenv("SMC_name", "Database");
		return projectName;
	}
	
	static private String rootName = null;
	public final static String getRootName() {
		if (null != rootName) 
			return rootName;
		
		synchronized (Database.class) { if (null == rootName) {
			String name = getProjectName();
			File f = new File(name);
			if (null == f || (false == f.isDirectory() && ! f.mkdir()))
				name = "";
			rootName = Utils.getenv("SMC_root", name);
		} }
		return rootName;
    }
    private static File root = null;
	private synchronized File getDatabaseRoot() {
		if (null != root)
			return root;
		synchronized (Database.class) {
			String rootName = getRootName();
//			File r = new File("X");
//			String rs = r.getAbsolutePath();
//			root = new File("X").getParentFile(); // the current installation folder.
			if (false == Strings.isNullOrEmpty(rootName)) {
				root = new File(rootName);
				root.mkdirs();
			}
			if (false == Strings.isNullOrEmpty(mProject)) {
				root = new File(root, mProject);
				root.mkdirs();
			}
			System.out.println("root=[" + root.getAbsolutePath() + "]");
			return root;
		}
	}

	/**
     * perform variouse post creation fixing and mending:
     * <ul>
     * <li>Add the permanent Ontology SM (SmOntology, id: Ont-SM) which is also a singleton class that cannot come from the file system.
     * </ul>
     * @throws IOException
     */
    private void postCreate()  {
		SmOntology smOnt = SmOntology.create(this, getOntology(SmOntology.ID));
		// Reorganize the list of ontologies so SmOnt is the first:
		for (Ontology ont: ontologies) {
			if (ont.getId().equals(smOnt.getId())) {
				ontologies.remove(ont);
				break;
			}
		}
		ontologies.add(0, smOnt);
		map.put(smOnt.getId(), smOnt); // replace id mapping to the proper one.
    }
    private void postCreate2() {
		// ensure PrtAR is ready:
		Port prtAR = getPort(SmBlobs.ATTACHMENTS_PORT_ID);
		if (null == prtAR) {
			System.out.println("PrtAR is missing. Recreating it:");
			System.out.println(makeArPort());
			prtAR = getPort(SmBlobs.ATTACHMENTS_PORT_ID);
			if (null == prtAR) {
				System.err.println("PrtAR cannot be created. Call the marines!");
				return;
			}
		}
		if (false == prtAR.isReady()) {
			prtAR.setStatus(ADatabaseRow.STATUS.READY);
			System.out.println("Setting PrtAR to READY!");
		}
		if (Strings.isNullOrEmpty(prtAR.getOntologyId())) {
			prtAR.setOntologyId(SmOntology.ID);
			prtAR.markModified();
		}
		String viewConfig = 
			"[{'tag':'<i>%</i>','forText':'true', 'forView':'true','title':'Title','content':['" + DCTerms.title + "']},\n" +
			" {'tag':'%','forText':'true', 'forView':'true','title':'Description','content':['" + DCTerms.description + "']}," +
			" {'tag':'%','forText':'false', 'forView':'true','title':'Attachment Type','content':['" + SmOntology.attachmentType + "']}," +
			" {'tag':'%','forText':'false', 'forView':'true','title':'Mime Type','content':['" + SmOntology.mimeType + "']}," +
			" {'tag':'<a href=\"%\">%</a>','forText':'false', 'forView':'true','title':'Attachment','content':['" + SmOntology.attachment + "']}]";

		prtAR.setField(Port.IFields.VIEW_CONFIG, viewConfig);

	}

    /**
     * Prepares a hash map of the items for a loaded database, and sets up the owner of 
     * these items to the database creating them.
     */
	private void recreateTheMap()
    {
    	for (ADatabaseRow x : getItems()) {
    		map.put(x.getId(), x);
    		x.owner = this;
    	}
    }

	/**
	 * Answers with a unique job id, so it is associated with the server host name and port in this format:
	 * Job-<num>@<host>:<port>
	 * @return String.
	 */
	public String createJobId() {
		String id = createId("Job");
		return id + "@" + "server"; // on jazz - used to return host w/port to identify the server
	}
	/**
	 * Creates a task id, in the format Tsk-<num>@<mediator id>
	 * @param mediator id of a mediator.
	 * @return String
	 */
	public String crateTaskId(String mediator) {
		return createId("Tsk") + "@" + mediator;
	}
	
    /**
     * Answers with a unique id that may be fixed, or temporary
     * @param kind String stem from which the id is constructed.
     * @return String of the id
     */
    public synchronized String createId(String kind) {
    	return createId(kind, false);
    }
    static long lastTimeMillis = 0;
    /**
     * Answers with a unique id that may be fixed, or temporary
     * @param kind String stem from which the id is constructed.
     * @param isTemp boolean indicating this is an id to be used for a temporary item.
     * @return String of the id
     */
    public synchronized String createId(String kind, boolean isTemp)
    {
    	String idTrail = "";
    	if (isTemp) {
    		long t = System.currentTimeMillis();
    		if (t <= lastTimeMillis)
    			t = lastTimeMillis + 1;
    		lastTimeMillis = t;
    		idTrail = Long.toString(t);
    	} else {
    		counter++;
    		idTrail = Integer.toString(counter);
    		dirty = true;
    	}
        return kind + "-" + idTrail; //.counter;
    }

//    // Utilities methods
//    private ADatabaseRow createItem(String stem, ADatabaseRow item, String collection)
//    {
//    	return createItem(stem, item, collection, false);
//    }

    private  ADatabaseRow createItem(String stem, ADatabaseRow item, String collection, boolean hasId)
    {
    	return createItem(stem, item, collection, hasId, false);
    }

    private synchronized ADatabaseRow createItem (String stem, ADatabaseRow item, String collection, boolean hasId, boolean isTemp)
    {
        item.setId(hasId? stem : createId(stem, isTemp)); // hasId true means - the ID is fixed on call.
        item.setStatus(STATUS.CREATED);
        item.markModified();
//        item.setCollection(collection);
        item.setTemporary(isTemp);
        //list.add(item);
        map.put(item.getId(), item);
        dirty = true;
        item.owner = this;
        return item;
    }

    public synchronized Port createPort(String type) {
    	return createPort(type, null);

    }
    public synchronized Port createPort(String type, String prtId)
    {
    	String stem = "Prt";
    	if (null != prtId)
    		stem = prtId;
    	PortType pt = PortType.valueOf(PortType.class, type);
        Port port = (Port) createItem(stem, new Port(pt), "Port", null != prtId);
        ports.add(port);
        return port;
    }

    public synchronized Port createPort(PortType type, boolean... isTemp)
    {
        Port port;
    	if (type == PortType.catalog)
        	port = (Port) createItem("Cat", new Catalog(), "Port",  false, Utils.isOptional(false, isTemp));
        else
        	port = (Port) createItem("Prt", new Port(isTemp), "Port",  false, Utils.isOptional(false, isTemp));
        ports.add(port);
        return port;
    }

    /**
     * Creates a tmp port form another port, taking from it all properties, including edit, view and text configurations.
     * @param toDuplicate Port to be duplicated.
     * @return a new Port that is a duplicate, but with its own separate id.
     */
    public synchronized Port createPort(Port toDuplicate)
    {
    	String id = toDuplicate.getId() + "-Copy";
    	Port p = getPort(id);
    	if (null != p) {
    		if (false == p.getType().equals(toDuplicate.getType())) {
    			System.err.println("Incompatible existing copy port of type [" + p.getType() + "]. Cannot create port of type [" + toDuplicate.getType() + "] and id [" + id + "].");
    			return null;
    		}
    		// It is possible that the edit configuration has changed - so we copy it again.
    		p.viewConfig = toDuplicate.viewConfig;
    		return p;
    	}
        p = (Port) createItem(id, new Port(toDuplicate.getType(), id), "Port",  true, true);
		p.viewConfig = toDuplicate.viewConfig;
		p.setComments("Auto generated editor repository for " + toDuplicate.getType() + " " + toDuplicate.getDisplayId());
		ports.add(p);
        return p;
    }

    public synchronized Ontology createOntology(boolean... isTemp)
    {
        Ontology ontology = (Ontology) createItem("Ont", new Ontology(isTemp), "Ontology",  false, Utils.isOptional(false, isTemp));
        ontologies.add(ontology);
        return ontology;
    }

    public synchronized RuleSet createRuleSet(boolean... isTemp)
    {
        RuleSet ruleSet = (RuleSet) createItem("Rul", new RuleSet(isTemp), "RuleSet", false, Utils.isOptional(false, isTemp));
        rules.add(ruleSet);
        return ruleSet;
    }

    /**
     * Answers with a Mediator registered in the database
     * @param isTemp boolean which indicate that this is a temporary item.
     * @return Mediator.
     */
    public synchronized Mediator createMediator(boolean... isTemp) {
        Mediator mediator = (Mediator) createItem("Mdt", new Mediator(isTemp), "Mediator", false, Utils.isOptional(false, isTemp));
        mediators.add(mediator);
        return mediator;
    }

    public synchronized Friend createFriend(boolean... isTemp) {
    	Friend friend = (Friend) createItem("Frn", new Friend(isTemp), "Friend", false, Utils.isOptional(false, isTemp));
    	friends.add(friend);
    	return friend;
    }

    // Get an element based on its id and according to its type.
    public ADatabaseRow getItem(String id)
    {
        ADatabaseRow object = map.get(id);
//        if (null == object) return null;
        return object;
    }

    public Ontology getOntology(String id)
    {
        ADatabaseRow object = getItem(id);
        if (object instanceof Ontology) return (Ontology) object;
        return null;
    }

    public RuleSet getRuleSet(String id)
    {
        ADatabaseRow object = getItem(id);
        if (object instanceof RuleSet) return (RuleSet) object;
        return null;
    }

//    public Config getConfig(String id)
//    {
//        ADatabaseRow object = getItem(id);
//        if (object instanceof Config) return (Config) object;
//        return null;
//    }

    public Mediator getMediator(String id)
    {
        ADatabaseRow object = getItem(id);
        if (object instanceof Mediator) return (Mediator) object;
        return null;
    }

    public Port getPort(String id)
    {
        ADatabaseRow object = getItem(id);
        if (object instanceof Port) return (Port) object;
        return null;
    }

    public Friend getFriend(String id)
    {
    	ADatabaseRow object = getItem(id);
    	if (object instanceof Friend) return (Friend) object;
    	return null;
    }

    // Get rows as an array of elements
    public ADatabaseRow[] getItems()
    {
        List<ADatabaseRow> all = new ArrayList<ADatabaseRow>();
        all.addAll(ontologies);
        all.addAll(rules);
        all.addAll(ports);
        all.addAll(mediators);
        all.addAll(friends);
        return (ADatabaseRow[]) all.toArray(new ADatabaseRow[0]);
    }

    /**
     * Sorts an array of abstract database rows according to a specific column name
     *
     * @param rows
     *            array of abstract database rows.
     * @param column
     *            String name of the column to sort by
     * @return array sorted.
     */
    public static ADatabaseRow[] sort(ADatabaseRow[] rows, String column, boolean ascending)
    {
        Sorter s = new Sorter(column, ascending);
        Arrays.sort(rows, s);
        return rows;
    }

    public static String[] getFields()
    {
        return new String[] {//
        AbstractRDFReadyClass.IFields.ID, //
                ANamedRow.IFields.NAME, //
                Ontology.IFields.PREFIX, //
                ADatabaseRow.IFields.VERSION, //s
                AModelRow.IFields.MODEL_INSTANCE_NAMESPACE,//
                AModelRow.IFields.DATE_CREATED,//
                AModelRow.IFields.LAST_MODIFIED,//
                Mediator.IFields.INPUT_PORT_ID,//
                Mediator.IFields.OUTPUT_PORT_ID,//
                RuleSet.IFields.INTERCEPTOR_NAME,//
                RuleSet.IFields.INTERCEPTOR_CLASS,
                RuleSet.IFields.INTERCEPTOR_DESCRIPTION,
                RuleSet.IFields.INTERCEPTOR_IS_LEGAL,
                RuleSet.IFields.END1,//
                RuleSet.IFields.END2,//
                RuleSet.IFields.REVERSIBLE, //
                RuleSet.IFields.API_ACCESS_NAME, //
                Friend.IFields.USER, //
                Friend.IFields.PASSWORD, //
                Friend.IFields.IP_ADDRESS, //
                Port.IFields.ASSOCIATED_REPOSITORY_ID, //
                "ruleSetId", //
                "status", //
                "tool", //
                "apiName", //
                "archived",//
                "service", //
                "accessName", //
                "type",//
                "ontologyId", //
                "friendId",
                "portSync",
                "tags",
        };
    }

    /**
     * Deleting a row with the option that deletion is forced also if the row is not archived. <br>
     * Should be used in caution. So this method is private for internal management of the database.
     * @param id String id of an existing row.
     * @param force boolean to force deletion.
     * @return
     */
    public synchronized String delete(String id, boolean force) {
        ADatabaseRow row = getItem(id);
        if (null == row) { return "Row [" + id + "] not found"; }
        if (false == row.isArchived() && false == force)
        	return "Cannot delete " + id + " - it must be archived first.";
        if (false == row.isArchived()) // we must unarchive it to prevent further protections from disrupting the deletion.
        	row.setArchived(true);
        map.remove(id);
        ontologies.remove(row);
        rules.remove(row);
        ports.remove(row);
        mediators.remove(row);
        friends.remove(row);
        String result = row.deleteContents();
        dirty = true;
        return result;
    }
    /**
     * Deletes a row by the id parameter, only if it is marked as archived beforehand.
     * @param id legal id of an existing row in the database.
     * @return
     */
    public synchronized String delete(String id)
    {
    	return delete(id, false);
    }


    public boolean isShowActive() throws JSONException
    {
        return "checked".equals(getVar("showActive"));
    }


    static final public class Sorter implements Comparator<ADatabaseRow>
    {

        final String propertyToCompareBy;
        final int    accessing;

        public Sorter(String propertyName, boolean acs)
        {
            propertyToCompareBy = propertyName;
            accessing = acs ? 1 : -1;
        }

        public int compare(ADatabaseRow a, ADatabaseRow b)
        {
            Object o = null;
            try
            {
                o = PropertyUtils.getProperty(a, propertyToCompareBy);
            }
            catch (Throwable t)
            {}
            String x = (o == null) ? "" : o.toString();

            try
            {
                o = PropertyUtils.getProperty(b, propertyToCompareBy);
            }
            catch (Throwable t)
            {}
            String y = (o == null) ? "" : o.toString();

            return accessing * x.compareTo(y);
        }
    }

//    public static void sSetVar(String var, String val) throws FileNotFoundException, IOException, ClassNotFoundException {
//		Database db = create();
//		db.setVar(var, val);    	
//    }
	public synchronized void setVar(String var, String val) throws JSONException {
		String ov = null;
		if (mVars.has(var))
			ov = (String) mVars.get(var);
		mVars.put(var, val);
		if (false == val.equals(ov)) {
		    dirty = true;
    		System.out.println("Saving var [" + var + "]: [" + ov + "] --> [" + val + "].");	    
		}
	}

//	public static String sGetVar(String var) throws FileNotFoundException, IOException, ClassNotFoundException {
//		Database db = create();
//		return db.getVar(var);
//	}
//

	public String getVar(String var, String... def) throws JSONException {
		if ( ! mVars.has(var))
			return (null != def && def.length > 0)?def[0]:"";
		String val = (String) mVars.get(var);
//		if (null == val)
//			val = "";
		return val;
	}
	/**
	 * Activate status updates on all items.<br>
	 * Answers a list of all the items which have been
	 * modified their status.
	 */
	public Set<ADatabaseRow> updateStatuses() {
		ADatabaseRow items[] = getItems();
		return updateStatuses(items);
	}
	private Set<ADatabaseRow> updateStatuses(ADatabaseRow items[]) {
		Set<ADatabaseRow> results = new HashSet<ADatabaseRow>();
		for (ADatabaseRow item : items) {
			if (item.setStatus(this))
				results.add(item);
		}
		if (results.size() > 0)
			results.addAll(updateStatuses(items));
		return results;
	}

	/**
	 * Find the port corresponding to a given access name, or null if none was found.
	 * @param domain String which is a domain of a certain port.
	 * @return Port or null if none was found.
	 */
	public Port getPort4Domain(String domain) {
		List<Port> ports = getPorts();
		for (Port port : ports)
			if (domain.equals(port.getAccessName()) && (port.isRepository() || port.isCatalog()))
				return port;
		return null;
	}

	/**
	 * performs update of an item. All call to update of items shoud go through this method
	 * so that they can be synchronized.
	 * @param item ADatabaseRow to be updated.
	 * @param jsonObject update contents.
	 * @return answers with a String message about the results of this update to be
	 * reported back to the GUI.
	 * @throws JSONException 
	 */
	public synchronized String update(ADatabaseRow item, JSONObject jsonObject, Map<String, String> params) throws JSONException {
		String result = item.update(jsonObject, params);
		setTags(); // they might have changed;
		if (Strings.isNullOrEmpty(result))
			result = item.validate();
		item.setStatus(this);
		if (false == Strings.isNullOrEmpty(result)) {
			return "Update for [" + item.getId() + "]: " + result;
		}
		return result;
	}

	private static Map<String, Object> mInterceptors = null;
	private static List<String> mInterceptorNames = null;
	/**
	 * Gets interceptors registered in plugin extensions, and fill in all information
	 * in Mediators so that version where the plugin extension mechanism is not working - will
	 * also be registered. Also, checks validity of classes and marks them as such.
	 * @return
	 */
	public Map<String, Object> getInterceptors() {
		if (null == mInterceptors) synchronized (Database.class) {
			if (null != mInterceptors)
				return mInterceptors;
			System.err.println("Discovering interceptors:");
			mInterceptors = new HashMap<String, Object>();
			mInterceptorNames = new ArrayList<String>();
			// Collect all the interceptor classes from extension points and from mediators
//			Map<String, String> classes = new HashMap<String, String>();
//			Map<String, String> desc = new HashMap<String, String>();
//			Set<String> licenses = new HashSet<String>();
//			Map<String, String> texts = new HashMap<String, String>();
			Map<String, ISmModuleIntercept> mediators = new HashMap<String,ISmModuleIntercept>();
			mediators.put("IBM Mediator", new HaifaSemanticMediator());
			mediators.put("Null Mediator", new SmPlainMediationModule());
					//Utils.getExtentions("com.ibm.dm.sm.container.api.SmMediator");
	    	for (String item : mediators.keySet()) {
//				JSONObject item = (JSONObject)object;
	    		ISmModuleIntercept mediator = mediators.get(item);
				String className = mediator.getClass().getCanonicalName();
				String name = item;
				boolean requiresLicense = false; 
//				if (Boolean.parseBoolean(requiresLicense))
//					licenses.add(className);
				String text = ""; //Utils.stringify(item.get("licenseText"));
				String desc = ""; //Utils.stringify((String)item.get("description"));
				Method[] methods = mediator.getClass().getMethods();
				for (Method m: methods) {
					try {
						if (m.getName().equals("requiresLicense"))
							requiresLicense = (Boolean)m.invoke(mediator);
						if (m.getName().equals("licenseText"))
							text = (String) m.invoke(mediator);
						if (m.getName().equals("description"))
							desc = (String) m.invoke(mediator);
						if (m.getName().equals("name"))
							name = (String) m.invoke(mediator);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				//				IConfigurationElement configurationElement =  null;
				//Object ce = item.get("configurationElement");
//				if (ce instanceof String) { // illegal interceptor. This is the error message.
//						mInterceptors.put(name + ".legal", ""); //(String)ce);
//				}
				//mInterceptors.put(name + ".configurationElement", ce);
//	    	}
//
//	    	for (String className : classes.keySet()) {
//				String name = classes.get(className);
				mInterceptors.put(name + ".class", className);
				mInterceptors.put(name + ".description", desc);
//				boolean valid = Mediator.validateClass(className);
				
				mInterceptors.put(name + ".requiresLicense", requiresLicense);
				mInterceptors.put(name + ".licenseText", text);
				mInterceptors.put(name + ".configuration", mediator);
				mInterceptorNames.add(name);
			}
			for (String k: mInterceptors.keySet()) {
				System.err.println(k + "= " + mInterceptors.get(k).toString());
			}
			System.err.println("===========================");
		}
		return mInterceptors;
	}

    public boolean  isInterceptorRequiresLicense(String name) {
    	boolean answer = (boolean) getInterceptors().get(name + ".requiresLicense");
    	return answer; //.parseBoolean(answer);
    }

    public String getInterceptorLicenseText(String name) {
    	String answer = Utils.stringify(getInterceptors().get(name + ".licenseText"));
    	return (Strings.isNullOrEmpty(answer))?"No License info":answer;
    }

    public String getInterceptorDescription(String name) {
    	String answer = Utils.stringify(getInterceptors().get(name + ".description"));
    	return (Strings.isNullOrEmpty(answer))?"No description":answer;
    }

    /**
	 * Answers with the list of registered interceptors in the plugin SmMediator extension point.
	 * @return
	 */
	public List<String> getInterceptorNames() {
		if (null == mInterceptorNames) {
			getInterceptors();
		}
		return mInterceptorNames;
	}

	/**
	 * Ansewrs with a folder path to use to store objects of the item parameter
	 * @param item ADatabaseRow
	 * @return String
	 */
	public String getFolder(ADatabaseRow item) {
		File folder = new File(getFolder());
		folder = new File(folder, item.getCollectionName());
		folder = new File(folder, item.getId());
		if (false == (folder.isDirectory() && folder.exists())) synchronized (Database.class) {
			if (false == (folder.isDirectory() && folder.exists()))
				if (folder.exists() && false == folder.isDirectory())
					folder.delete();
			folder.mkdirs();
		}

		return folder.toString();
	}
	
	public String getLogFolder() {
		return new File(new File(getFolder()), "logs").toString();
	}

	public String getFolder() {
		File root = getDatabaseRoot();
		return new File(root, SmContainer.getFolder()).toString();
	}

	/**
	 * Ansswers with a mediator to which the repository parameter is an input or output port according to the
	 * isInput boolean parameter
	 * @param repository the repository being input or output to this mediator.
	 * @param isInput
	 * @return
	 */
	public Mediator findMediator(Port repository, boolean isInput) {
		String id = repository.getId();
		for (Mediator mediator : mediators) {
			if (! mediator.isReady())
				continue;
			if ((isInput && mediator.getInputPortId().equals(id)) ||
				(!isInput && mediator.getOutputPortId().equals(id)))
				return mediator;
		}
		return null;
	}

	/**
	 * Ansswers with a list of mediators to which the repository parameter is an input or output port according to the
	 * isInput boolean parameter
	 * @param repository the repository being input or output to this mediator.
	 * @param isInput
	 * @return
	 */
	public Mediator[] findMediators(Port repository, boolean isInput) {
		String id = repository.getId();
		ArrayList<Mediator> ms = new ArrayList<Mediator>();
		for (Mediator mediator : mediators) {
			if (! mediator.isReady())
				continue;
			if ((isInput && mediator.getInputPortId().equals(id)) ||
				(!isInput && mediator.getOutputPortId().equals(id)))
				ms.add(mediator);
		}
		return ms.toArray(new Mediator[0]);
	}

	/**
	 * Answers with a List of {@link ADatabaseRow} items for a given classification.
	 * @param classification {@link EClassification} identifier for desired items.
	 * @return List of {@link ADatabaseRow} items of the given classification.
	 */
	@SuppressWarnings("unchecked")
	public List<ADatabaseRow> getItems(EClassification classification) {
		if (EClassification.ontologies.equals(classification))
			return (List<ADatabaseRow>)(List<?>)getOntologies();
		if (EClassification.ontologies.equals(classification))
			return (List<ADatabaseRow>)(List<?>)getPorts();
		if (EClassification.ontologies.equals(classification))
			return (List<ADatabaseRow>)(List<?>)getRules();
		if (EClassification.ontologies.equals(classification))
			return (List<ADatabaseRow>)(List<?>)getFriends();
		if (EClassification.ontologies.equals(classification))
			return (List<ADatabaseRow>)(List<?>)getMediators();
		return null;
	}

	/**
	 * Return all ports which are repositories, catalogs and catalog members.
	 * @return List of {@link Port}s.
	 */
	public List<Port> getRepositories() {
		List<Port> p = new ArrayList<Port>(), p1 = getPorts();
		for (Port port : p1) {
			if (port.isReady() && (port.isRepository() || port.isCatalog()) && false == port.isTemporary())
					p.add(port);
		}
		return p;
	}


	/**
	 * Answers with an ontology for the first occurance of the specified prefix.
	 * @param prefix String prefix of the name space of an ontology.
	 * @return Ontology or RulesSet for that prefix.
	 */
	public AModelRow getModelItemByPrefix(String prefix) {
        if (Strings.isNullOrEmpty(prefix)) return null;
        List<AModelRow> items = new ArrayList<AModelRow>();
        items.addAll(getOntologies());
        items.addAll(getRules());
        for (AModelRow item : items)
        {
            if (false == item.getStatus().equals(STATUS.READY))
            	continue;
            if (item.getPrefix().equals(prefix))
            	return item;
        }
        return null;
	}


	public AModelRow getModelItemByNS(String ns) {
	       if (Strings.isNullOrEmpty(ns)) return null;
	        List<AModelRow> items = new ArrayList<AModelRow>();
	        items.addAll(getOntologies());
	        items.addAll(getRules());
	        return AModelRow.findModelForNameSpaceByVersion(items, ns);
//	        for (AModelRow item : items)
//	        {
//	            if (false == item.getStatus().equals(STATUS.READY))
//	            	continue;
//	            if (item.getVersion()/*ModelInstanceNamespace()*/.equals(ns))
//	            	return item;
//	        }
//	        return null;
	}


	/**
	 * Answers with the friend whose IP is the parameter. Look only over legal and ready friends.
	 * @param fIp IP of a friend, composed of a machine ip and port.
	 * @return Friend of null is none matches the category.
	 */
	public Friend getFriendFromIp(String fIp) {
		if (Strings.isNullOrEmpty(fIp))
			return null;
		for (Friend friend: getFriends()) {
			if (false == friend.isReady())
				continue;
			if (fIp.equals(friend.getIpAddress()))
				return friend;
		}
		return null;
	}


	/**
	 * Lists all the identifiers of members of an item that may habe been files in a folder in the file-based
	 * implementation - as in this implementation.
	 * @param aModelRow
	 * @return
	 */
	public String[] listMembersForItem(AModelRow aModelRow) {
		return new File(getFolder(aModelRow)).list();
	}

	public static String getName() {
		return mName;
	}

	public static String getDotExePath() {
		return mDotExePath;
	}

	/**
	 * Answers with a RuleSet item based on the URI string parameter.
	 * @param classUri
	 * @return a RuleSet if a ready one is found, or null.
	 */
	public RuleSet getRulesByClassUri(String classUri) {
        return (RuleSet)getItemByClassUri(classUri, getRules());
	}



//	/**
//	 * finds the item owning a repository and sets its last update information based on the date of the repository.
//	 * @param aRdfRepository Repository of an item.
//	 */
//	public void setRepositoryUpdated(ARdfRepository aRdfRepository) {
//		long lastModified = aRdfRepository.getDate();
//		aRdfRepository.getMyItem().setDateModified(new Date(lastModified));
//	}


}

