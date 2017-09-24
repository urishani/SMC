
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

package com.ibm.dm.frontService.sm.data;

import java.io.ByteArrayInputStream;
//import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.intfc.imp.OntologyDescription;
import com.ibm.dm.frontService.sm.service.ARdfRepository;
import com.ibm.dm.frontService.sm.service.ModelRepository;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * A common super class for configuraions and ruls sets which have same structure.
 *
 * @author shani
 */
public abstract class AModelRow extends ANamedRow
{
	@Override
	public String[] getEditableFieldNames() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void validateRepository(ARdfRepository r) {
		if (null == r)  return;
		super.validateRepository(r);
		System.err.println("Validating repository [" + r.getMyItem().getId() + ":" + r.getMyItem().getName() + "] of [" + new Date(r.getDate()) + "] compared with [" + new Date(r.getLoadDate()) + "]: " + ((r.getDate() > r.getLoadDate())? "reload":"noreload"));
		if (r.getDate() > r.getLoadDate())
			synchronized(r.getClass()) {
				r.getModel();
			}
	}

	private transient JSONObject ontologyJson = null;
	private transient String ontologyJsonEtag = null;
	protected String importUrl = "";
	protected String prefix = "";
	protected boolean isImported = false;
	
	protected String   		   viewConfig 		= "";
	//  protected String   		   showListConfig 		= "";
	protected transient String   		   viewConfigHelp 		= "List of predicates for fields containing indexing contents for free text search, separated with commas.";
	//  protected transient String   		   showListConfigHelp 		= "Configuration for the List display of a repository. A JSON structure as this: \n" + 
	//  				"[{'title':'<a title>','content':['<uri of predicate 1>', ... '<uri of predicate 2>'],'tag':'<tag>'}, ... ]. \n<tag> can be <img> with legal attributes as well.";

	private static final String SMC_PROTOCOL_PREFIX = "smc://";

	/**
	 * Answers if this model ontolgy is imported, and therefore cannot be uploaded from a client.
	 * @return true of it is imported, false otherwise.
	 */
	public boolean isImported() {
		return isImported;
	}

	
	/**
	 * Codes a special URL as the importUrl composed of the "smc" protocol, and the coding of the ids
	 * of the local friend item, and the remote model row item where this friend points.
	 * @param friendId local id of the friend
	 * @param remoteId remote id of the model row.
	 */
	public void setSmcImportUrl(String friendId, String remoteId) {
		Friend friend = getDatabase().getFriend(friendId);
		if (null == friend)
			return;
		String url = SMC_PROTOCOL_PREFIX + remoteId + "@" + friend.getIpAddress() + "/" ;
		setImportUrl(url);
	}
	/**
	 * Answers with a prefix that use sets up for this ontology's namespace.<br>
	 * 
	 * If the prefix is not set, it is taken from the name space, and if that is missing,
	 * it is taken from the name field.
	 * @return String prefix.
	 */
	public String getPrefix() {
		if (Strings.isNullOrEmpty(prefix)) {
			String p = Utils.makePrefix(getModelInstanceNamespace());
			if (Strings.isNullOrEmpty(p)) {
				p = Utils.makeIdentifier(getName().toLowerCase());
				if (Utils.IDENT.equalsIgnoreCase(p) )
					return "";
				return p;
			} 
			return "";
		}
		String  p2 = Utils.makeIdentifier(prefix);
		if (Utils.IDENT.equalsIgnoreCase(p2))
			return "";
		return p2;
	}

	/**
	 * Imports a model from the internet based on the namespace. But since namespace may not match
	 * the URL to import the model, there is a mechanism to keep the URL source of the ontology so that
	 * it can be re=imported from the same location.
	 * <br>
	 * With this indication, uploading an ontology or modifying it in any other way will be prevented. 
	 * @return
	 */
	public String importModel() {
        String nameSpace = getModelInstanceNamespace();
        if (false == Utils.isURL(nameSpace)) //null == nameSpace || "".equals(nameSpace.trim())) 
        	return "Error: Importing item [" + getId() + "] from internet must have a legal URI for a name space";
        if (false == Strings.isNullOrEmpty(getImportUrl()))
        	nameSpace = getImportUrl();
        InputStream is;
        boolean smcImport = false;
		try {
			if (nameSpace.startsWith(SMC_PROTOCOL_PREFIX)) { // create a url that will work with https
				smcImport = true;
				String tmp = nameSpace.split("/")[2]; // we need 3rd part
				String parts[] = tmp.split("@");
				String rid = parts[0], fIp = parts[1];
				Friend friend = getDatabase().getFriendFromIp(fIp);
				if (null == friend)
					return "No (ready) Friend foung for IP [" + fIp + "] of model item [" + getDisplayId() + "].";
				String url = "https://" + fIp + "/dm/smProtege?id=" + rid;
//				com.ibm.haifa.smc.client.oauth.OAuthCommunicator comm = friend.getCommunicator();
				if (! friend.isLoggedIn()) 
					return "Cannot communcate with friend [" + friend.getDisplayId() + "] does not exist for the url of model item [" + getDisplayId() + "].";
				HttpGet get = new HttpGet(url);
				System.out.println("Importing from URL [" + url + "] for [" + getDisplayId() + "].");
				get.setHeader(HttpHeaders.ACCEPT, IConstants.RDF_XML);
				HttpResponse resp = friend.execute(get);
				if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					return "Failed to communcate with friend [" + friend.getDisplayId() + "] does not exist for the url of model item [" + getDisplayId() + 
					"]. Status [" + resp.getStatusLine().getStatusCode() + "]";
				}
				is = resp.getEntity().getContent();
			} else
				is = Utils.streamFromNetwork(nameSpace, IConstants.RDF_XML);
	        if (null == is) 
	        	return "Could not obtain any data for item [" + getId() + "] from internet for URL [" + nameSpace + "].";
		} catch (Exception e) {
			e.printStackTrace();
			return "Failed for item [" + getId() + "] with exception [" + e.getMessage() + "]";
		}
        OntologyDescription od = OntologyDescription.fromStream(getDatabase(), is, nameSpace, IConstants.RDF_XML);
        String answer =  saveOntology(od);
        nameSpace = getModelInstanceNamespace(); // get it as defined for the model at the end of the import process.

        // Need to check if actual namespace was different. In the general case, the URL used to get it is recorded so 
        // Reimporting will work. The real name space is recorded already.
        if (Strings.isNullOrEmpty(answer) && false == getImportUrl().equals(nameSpace)) { 
        	if (!smcImport)
        		setImportUrl(nameSpace);
        	markModified();
        }
        return answer;

	}

	/**
	 * Asnwers with a "default" which has to be defined by the subclasses of this class.
	 * @return
	 */
    public abstract JSONArray getDefaultViewConfig();

    public JSONArray getActualViewConfig() {
    	JSONArray result = getViewConfig();
    	if (null == result)
    		result = getDefaultViewConfig();
    	return result;
    }

    public JSONArray getEditViewConfig() {
    	return getActualViewConfig();
    }
    public JSONArray getShowViewConfig() {
    	return getActualViewConfig();
    }
    
    /**
	 * Parses the saved json view config expression into a json object. If not defined, will define it as an
	 * empty object.
	 * @return JsonObject of the viewConfig expression or null if not defined.
	 */
	public JSONArray getViewConfig() {
		if (Strings.isNullOrEmpty(getSavedViewConfig()))
			return null;
		try {
			JSONArray result = new JSONArray(getSavedViewConfig());
			return result;
		} catch (Exception e) {
			setField(IFields.VIEW_CONFIG, "", viewConfig);
//			markModified();
			return null;
		}
	}
	
	
	@Override
	String update(String field, String v, JSONObject jUpdate, Map<String, String> params) throws JSONException {
		if (false == IFields.MODEL_INSTANCE_NAMESPACE.equals(field) &&
				(false == IFields.VERSION.equals(field))) {
			return super.update(field, v, jUpdate, params);
		}
		if (IFields.VERSION.equals(field)) {
			String check = null;
			if (false == Strings.isNullOrEmpty(v))
				check = Utils.isLegalURI(v); 
			if (null != check)
				return "Error: Update of [" + getId() + "] - version [" + v + "] is not legal URI [" + check + "].";
			if (false == Strings.isNullOrEmpty(getModelInstanceNamespace()) &&
					false == getVersion().equals(v))
				return "Error: Update of [" + getId() + "] - cannot modify version in an existing model instance.";
			if (false == getVersion().equals(v)) {
				version = v;
				markModified();
			}
			return null;
		}
		String ns = v; //(String)jUpdate.get(field);
		String check = Utils.isLegalURI(ns); 
		if (null != check)
			return "Error: Update of [" + getId() + "] - namespace [" + ns + "] is not legal URI [" + check + "].";
		if (false == Strings.isNullOrEmpty(ns) && false == Strings.isNullOrEmpty(getModelInstanceNamespace()) &&
				false == getModelInstanceNamespace().equals(ns))
			return "Error: Update of [" + getId() + "] - cannot modify an existing model instance namespace.";
		if (false == Strings.isNullOrEmpty(ns) && false == ns.equals(modelInstanceNamespace)) {
			if (false == Strings.isNullOrEmpty(modelInstanceNamespace) || hasModelRepository())
				return "Error updating [" + getDisplayId() +"]: Cannot change name-space on a 'live' ontology. Recreate a new one, load a complete new one, or use Protege to edit.";
			// Assume it is a new namespace and the ontology content needs to be initialize
			Model ontology = Utils.createEmptyOntology(ns, version, params);
			OntologyDescription od = OntologyDescription.fromModel(getDatabase(), ontology, ns);
            setModelInstanceNamespace(od.getBase());
            createRdfRepository();
	        ARdfRepository /*ModelRepository*/ mr = getModelRepository(); //ModelRepository.create(this);
//	        if (null != mr) {
	        	mr.init(od.getModel());
		        mr.save();
	            setFileName("loaded");
//	        }
            String version = od.getVersionIRI();
            if (null != version) 
            	setVersion(version);
		}
		return null;
	}

	@Override
	public String getDependencyLinks(Database db) {
        List<? extends AModelRow> ontologys = db.getOntologies();
        String result = findDependencies(ontologys);
        return result;
	}

	@Override
	public boolean canImport() {
		 return (false == isArchived()) && 
		 	( hasNameSpace() || false == Strings.isNullOrEmpty(getImportUrl()));
	}
	@Override
	public boolean canLoad() {
		return false == isArchived() && false == isImported();
	}

//	@Override
//	public String[] getEditableFieldNames() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public boolean isLegal() {
		// TODO Auto-generated method stub
		return super.isLegal() && canShow();
	}


	/**
	 * Initializes an ontological reposotory
	 */
	@Override
	public ARdfRepository initRdfRepository() {
		ARdfRepository r = ModelRepository.create(this);
		return r;
	}
	

	@Override
	public void integrityValidation(ADatabaseRow changed,
			Collection<? extends ADatabaseRow> all)
			throws IntegrityValidationException {
		// TODO Auto-generated method stub

	}

//	@Override
//	public boolean isLegal() {
//		return super.isLegal() && false == Strings.isNullOrEmpty(getModelInstanceNamespace()); //null != getModelRepository(); //Strings.isNullOrEmpty(getFileName()); // && false == "".equals(getFileName().trim()) && new File(getFileName()).canRead();
//	}

	static public interface IFields extends ANamedRow.IFields
    {
        public static final String FILE_NAME                = "fileName";
        public static final String MODEL_INSTANCE_NAMESPACE = "modelInstanceNamespace";
		public static final String VIEW_CONFIG 				= "viewConfig";
        public static final String PREFIX = "prefix";
		public static final String IS_IMPORTED = "isImported";
		public static final String IMPORT_URL = "importURI";
    }

    private static final long     serialVersionUID       = 1L;
    /**
     * this is name space of the model that instance this class describes
     */
    protected String              modelInstanceNamespace = "";
    /// TODO: this is should become ID of the OWL in the JAZZ
    protected volatile String              fileName               = "";
    /**
     * Ontology description structure
     */
    protected transient OntologyDescription ontologyDescription    = null;
	private long validationTime = 0;

    /**
     * Converts the nameSpace into a URI and return it if successful.
     *
     * @return URI of the ontology namespace.
     */
    public URI getNameSpaceUri()
    {
        if (Strings.isNullOrEmpty(modelInstanceNamespace))
        {
            setStatus(STATUS.CREATED);
            return null;
        }
        try
        {
            return new URI(getModelInstanceNamespace());
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
        setStatus(STATUS.CREATED);
        return null;
    }

    public AModelRow() {
    	super();
    }
    public AModelRow(boolean... isTemp)
    {
        super(isTemp);
    }

    @Override
    public boolean canShow()
    {
       	return false == Strings.isNullOrEmpty(getFileName());
    }

//    @Override
//    public JSONObject toJasonObject(Database database)
//    {
//        JSONObject result = super.toJasonObject(database);
//        result.put(IFields.NAME, name);
//        result.put(IFields.MODEL_INSTANCE_NAMESPACE, modelInstanceNamespace);
//        result.put(IFields.FILE_NAME, fileName);
//        if (null != ontologyDescription) result.put(IFields.ONTOLOGY_DESCRIPTION, ontologyDescription.toJson());
//        result.put("canShow", canShow());
//        result.put("canLoad", canLoad());
//        result.put("canImport", canImport());
//        result.put("dependencies", getDependencyLinks(database));
//        return result;
//    }

    @Override
    public boolean hasNameSpace()
    {
        boolean result = false == Strings.isNullOrEmpty(modelInstanceNamespace);
        return result;
    }

    public String getModelInstanceNamespace()
    {
        return modelInstanceNamespace;
    }

    public void setModelInstanceNamespace(String nameSpace)
    {
    	if (false == this.modelInstanceNamespace.equals(nameSpace)) {
//    		System.out.println("[" + this.modelInstanceNamespace + "](" + this.modelInstanceNamespace.length() + ")=![" + nameSpace + "](" + nameSpace.length() + ")");
//    		int i=0;
//    		for (char a: this.modelInstanceNamespace.toCharArray()) {
//    			if (i >= nameSpace.length()) 
//    				System.out.println("nameSpace is too small");
//    			else
//    				System.out.println(i + ":[" + a + ":" + (int)a + "]==[" + nameSpace.charAt(i) + ":" + (int)nameSpace.charAt(i) + "] -> " + (a == nameSpace.charAt(i)));
//    		}
    		this.modelInstanceNamespace = nameSpace;
    		markModified();
    	}
    }

    public String getFileName()
    {
        
    	return fileName;
    }
    
    // Not saved to database anymore, so don't mark as modified.
    public void setFileName(String fileName)
    {
    	if (this.fileName != fileName) {
    		this.fileName = fileName;
    		markModified();
        }
    }

    public void setField(String field, String value)
    {
        super.setField(field, value);
    	if (field.equalsIgnoreCase(IFields.VIEW_CONFIG))
    		this.viewConfig = value;
        else if (field.equalsIgnoreCase(IFields.MODEL_INSTANCE_NAMESPACE)) {
            this.modelInstanceNamespace = value;
        } else if (field.equalsIgnoreCase(IFields.FILE_NAME)) 
        	this.fileName = value;
        else if (field.equalsIgnoreCase(IFields.PREFIX)) 
        	this.prefix = value;
        else if (field.equalsIgnoreCase(IFields.IMPORT_URL)) 
        	this.importUrl = value;
    }

    public String getField(String field)
    {
    	if (field.equalsIgnoreCase(IFields.VIEW_CONFIG))
    		return this.viewConfig;
    	else
    		if (field.equalsIgnoreCase(IFields.MODEL_INSTANCE_NAMESPACE))
    			return this.modelInstanceNamespace;
    		else
    			if (field.equalsIgnoreCase(IFields.FILE_NAME))
    				return this.fileName;
    			else
    				return super.getField(field);
    }

    /**
     * Load an ontology description if not loaded already for this item in the database.
     *
     * @return OntologyDescription.
     */
    public OntologyDescription getOntologyDescription()
    {
       	ARdfRepository r = getModelRepository();
    	if (null != this.ontologyDescription && this.validationTime  != 0) {
//    		String vDate = new Date(this.validationTime).toString();
//    		String rDate = new Date(getModelRepository().getDate()).toString();
    		if (this.validationTime < r.getDate())
    			System.err.println("Reloading repository for item [" + this.getId() + "]");
    			this.ontologyDescription = null; // invalidate so it is reloaded.
    	}
    	
    	//        if () return null; //getStatus().equals(STATUS.READY)) return null;
    	OntologyDescription od = this.ontologyDescription;
        if (null == od && isLegal())
        {
            od = OntologyDescription.fromModel(getDatabase(), r.getModel(), r.getBase()); //Stream(new FileInputStream(new File(getFileName())), base);
            setOntologyDescription(od);
        }
        return this.ontologyDescription;
    }

    public void setOntologyDescription(OntologyDescription od)
    {
    	this.validationTime = System.currentTimeMillis();
        if (null == od)
        	return;
        boolean modified = false;
//        System.out.println("Updating from Protege OD [" + this.ontologyDescription + "]\n" +
//        		"against coming OD [" + ontologyDescription + "]");
        if (null == this.ontologyDescription || false == od.toString().equals(this.ontologyDescription.toString())) {
        	this.ontologyDescription = od;
//        	markModified(); this is a transient field now - so no updates are needed.
           	setModelInstanceNamespace(ontologyDescription.getBase());
           	setVersion(ontologyDescription.getVersionIRI());
//           	modified = true;  // This flag should be set only in case any of the prev. two setters triggered it. OD is never set in the item
           			// and is always loaded from the model. However, only the instance and versiopn URLs must be recorded in the item
           			// meta data, and must comply with the model
        }
        if (Strings.isNullOrEmpty(version) || 
        		false == version.equals(ontologyDescription.getVersionIRI())) {
        	setVersion(ontologyDescription.getVersionIRI());
        	modified = true;
        }
        if (modified)
        	markModified();
    }

    /**
     * Create a string referencing ontologies that the row's ontology depends on.
     *
     * @param row
     *            Current row to use
     * @param rows
     *            Other rows containing candidate ontologies.
     * @return String that can be used to print or show on an HTML page, etc.
     */
    private String findDependencies(List<? extends AModelRow> rows)
    {
        OntologyDescription od = getOntologyDescription();
        String imports[] = new String[0];
        if (null != od)
        {
            imports = od.getImports();
        }
        StringBuffer sb = new StringBuffer();
        for (String importModel : imports)
        {
            if (sb.length() > 0) sb.append(", ");
            AModelRow dRow = findModelForNameSpaceByVersion(rows, importModel);
            if (null == dRow)
                sb.append(importModel);
            else
                sb.append(dRow.getId() + ": " + dRow.getName());
        }
        return sb.toString();
    }
    /**
     * Utility answering with a row in which has a certain namespace by their version
     * NOTE: As we learn that versions are not an identification IRI for the model, we now only use model instance name space and not versions. (18/5/16).
     *
     * @param rows
     *            Array of AModelRow-s defining some namespaces of ontologies.
     * @param importNS
     *            Name-space version of a model
     * @return
     */
    public static AModelRow findModelForNameSpaceByVersion(List<? extends ADatabaseRow> rows, String importNS)
    {
    	
    	if (Strings.isNullOrEmpty(importNS))
    		return null;
        //AModelRow row = null;
        for (ADatabaseRow aDatabaseRow : rows)
        {
            if (aDatabaseRow instanceof AModelRow)
            {
                if (importNS.equals(((AModelRow) aDatabaseRow)./*getVersion*/getModelInstanceNamespace())) 
                	return (AModelRow) aDatabaseRow;
            }
        }
        return null;
    }

    /**
     * Utility answering with a row in which has a certain namespace (not  by their version!)
     *
     * @param rows
     *            Array of AModelRow-s defining some namespaces of ontologies.
     * @param importNS
     *            Name-space of a model
     * @return
     */
    public static AModelRow findModelForNameSpace(List<? extends ADatabaseRow> rows, String importNS)
    {
    	
    	if (Strings.isNullOrEmpty(importNS))
    		return null;
        //AModelRow row = null;
        for (ADatabaseRow aDatabaseRow : rows)
        {
            if (aDatabaseRow instanceof AModelRow)
            {
                if (importNS.equals(((AModelRow) aDatabaseRow).getModelInstanceNamespace())) 
                	return (AModelRow) aDatabaseRow;
            }
        }
        return null;
    }

	/**
	 * Answers with the list of files having domain as their prefix to mean all the files
	 * serving as history of that repository.
	 * @return List<String> of file names.
	 */
	public List<String> getHistory() {
		String files[] = getDatabase().listMembersForItem(this);
		List<String> list = new ArrayList<String>(files.length);
//		String baseName = new File(getFileName()).getName();
		for (String file : files) {
			if (/*file.startsWith(baseName) && */file./*substring(baseName.length()).startsWith*/contains(".v-")) {
				String dateStr = file.substring(file.indexOf(".v-")/*baseName.length() */+ 3); 
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

//	/**
//	 * Migrating, assuming we are working with a file system.
//	 * @param db
//	 * @param oldFolder
//	 * @return
//	 */
//	public String migrateToV2_4(Database db, File oldFolder) {
//		String msg = "";
//		ARdfRepository repository = getModelRepository(); //ModelRepository.create(this);
//		msg += migrateFilesForV2_4(repository, oldFolder);
//		// Now clear the folder from foreign things
//		File folder = new File(repository.getFolder());
//		String baseName = new File(getFileName()).getName();
//		String members[] = folder.list();
//		for (String member : members) {
//			if (false == member.startsWith(baseName)) { // clean the file
//				msg += "\tCleaning foreign thing [" + member + "]: ";
//				boolean done = new File(folder, member).delete();
//				msg += (done? "Successfully":"Failed to be ") + " deleted.\n";
//			}			
//		}
//		return msg;
//	}

//	/**
//	 * Migrating, assuming we are working with a file system.
//	 * @param db
//	 * @param oldFolder
//	 * @return
//	 */
//	protected String migrateFilesForV2_4(ARdfRepository repository, File oldFolder) {
//		String msg = "Migrating file for [" + getId() + "]:\n";
//		String fn = this.fileName; //getFileName();
//		if (Strings.isNullOrEmpty(fn) || false == new File(fn).canRead()) {
//			msg += " --> no file or file missing.\n";
//			if (Strings.isNullOrEmpty(fn))
//				return msg;
//		}
//		File f = new File(fn);
////		File folder = repository.getFolder();
//		File root = oldFolder;
//		File newf = new File(repository.getFileName());
//		boolean done = false;
//		if (f.exists() && f.canRead()) {
//			done = f.renameTo(newf);
//			msg += " --> moving [" + fn + "]: " + (done?"":"NOT") + " succeeded to move to [" + newf.getAbsolutePath() + "].\n";
//		} else {
//			done = f.delete();
//			msg += " --> just deleting [" + fn + "]: " + (done?"Successfull": "Failed") + ".\n";
//		}
//		// Now clean the path as possible:
//		if (done) { 
//			f = f.getParentFile();
//			while (f.compareTo(root) != 0 && f.delete()) {
//				f = f.getParentFile();
//			}
//			msg += "\t --> Cleared old file up to [" + f.getAbsolutePath() + "].\n";
//		}
//		fileName = repository.getFileName();
//		if (Strings.isNullOrEmpty(fileName) || false == new File(fileName).canRead())
//			status = ADatabaseRow.STATUS.UPDATED;
//		markModified();
//		return msg;
//	}

	/**
	 * converts to Json an Ontology idem.
	 * @return
	 * @throws JSONException 
	 */
	public JSONObject jsonFromOntology() throws JSONException {
		if (null == ontologyJson || (null != getEtag() && false == (getEtag().equals(ontologyJsonEtag))))
			synchronized (this.getClass()) {
				if (null == ontologyJson || false == (getEtag().equals(ontologyJsonEtag))) {
					ARdfRepository r = getModelRepository();
					ontologyJson = createJsonFromOntology(r.getModel(), getNameSpaceUri());
					System.out.println("jsonOnt for [" + r.getMyItem().getId() + "]: Regenerated.\n"); 
					ontologyJsonEtag = getEtag();
				}
			}
		// TODO Auto-generated method stub
		return ontologyJson;
	}

	private JSONObject createJsonFromOntology(Model model, URI uri) throws JSONException {
    	OntModel ontModel = ModelFactory.createOntologyModel();
    	Map<String, String> map = model.getNsPrefixMap();
    	ontModel.add(model);
    	JSONObject result = new JSONObject();
    	JSONArray classes = new JSONArray(); result.put("classes", classes);
    	JSONObject objectProperties = new JSONObject(); result.put("objectProperties", objectProperties);
    	JSONObject dataProperties = new JSONObject(); result.put("dataProperties", dataProperties);
    	ExtendedIterator<OntClass> classesIt = ontModel.listClasses();
		String uris = uri.toString();
//    	StmtIterator classesIt = model.listStatements(null, RDF.type, OWL.Class);
    	while (classesIt.hasNext()) {
    		OntClass c = classesIt.next();
//    		Resource modelClass = stmt.getSubject();
    		if (false == c.isAnon()) {
    			if (c.getNameSpace().equals(uris))
    				classes.put(c.toString().substring(uris.length()));
    		}
    	}
    	ExtendedIterator<ObjectProperty> oPropertiesIt = ontModel.listObjectProperties();
//    	StmtIterator oPropertiesStmts = model.listStatements(null, RDF.type, OWL.ObjectProperty);
    	 
    	while (oPropertiesIt.hasNext()) {
    		ObjectProperty p = oPropertiesIt.next();
    		if (false == p.toString().startsWith(uris))
    			continue;
//    		Resource property = stmt.getSubject();
    		JSONObject jo = new JSONObject();
    		objectProperties.put(Utils.prefixForResource(p.toString(), map), jo);
    		OntResource r = p.getRange();
    		JSONArray range = new JSONArray();
    		jo.put("range", range);
//    		StmtIterator it = model.listStatements(property, RDFS.range, (RDFNode)null);
//    		while (it.hasNext()) {
    		if (null != r && r.toString().startsWith(uris))
    			range.put(Utils.prefixForResource(r.toString(), map));
//    		}
    		JSONArray domain = new JSONArray();
    		jo.put("domain", domain);
    		r = p.getDomain(); //model.listStatements(property, RDFS.range, (RDFNode)null);
//    		while (it.hasNext()) {
    		if (null != r && r.toString().startsWith(uris)) {
    			domain.put(Utils.prefixForResource(r.toString(), map));		
    		}
    	}
    	
    	ExtendedIterator<DatatypeProperty> dPropertiesIt = ontModel.listDatatypeProperties();
//    	StmtIterator dPropertiesStmts = model.listStatements(null, RDF.type, OWL.DatatypeProperty);
    	while (dPropertiesIt.hasNext()) {
    		DatatypeProperty p = dPropertiesIt.next();
//    		Resource property = stmt.getSubject();
    		JSONObject jo = new JSONObject();
    		if (false == p.toString().startsWith(uris))
    			continue;
    		dataProperties.put(Utils.prefixForResource(p.toString(), map), jo);
    		OntResource r = p.getDomain();
    		JSONArray domain = new JSONArray();
    		jo.put("domain", domain);
//    		StmtIterator it = model.listStatements(property, RDFS.range, (RDFNode)null);
//    		while (it.hasNext()) {
    		if (null != r && r.toString().startsWith(uris))
    			domain.put(Utils.prefixForResource(r.toString(), map));
    		r = p.getRange();
    		if (null != r && r.toString().startsWith(XSD.getURI()))
        		jo.put("value", Utils.prefixForResource(r.toString(), map));
//    		}
    	}
		return result;
	}

	// Maintain a local state analysis of the ontology to save on recomputations.
//	private Set<String> topTextProperties = null;
	private Set<String> topProperties = null;
	protected String root = null;
	private long topPropertiesDate = 0;

//	/**
//	 * Answers with a set of resources serving as the data properties of the top element of the ontology
//	 * which means that it is relevant to all elements of a valid model of this ontology and can be used
//	 * to search for text content such as can be used for text searching with a Lucene type of indexer.
//	 * @return Set of strings of data properties in the ontology.
//	 */
//	public Set<String> getTopTextProperties() {
//		ARdfRepository r =getModelRepository(); 
//		if (topPropertiesDate >= r.getDate() && null != topTextProperties )
//			return topTextProperties;
//		synchronized (this.getClass()) {
//			if (topPropertiesDate >= r.getDate() && null != topTextProperties )
//				return topTextProperties;
//			topPropertiesDate = r.getDate();
//			Set<String> properties = new HashSet<String>();
//			Model m = r.getModel();
//			getRootElement(m);
//			QueryExecution qexec = null;
//			if (null != root) {
//				String sparql = "Select ?p \n" +
//				"Where {\n" +
//				" { ?p <" + RDF.type + "> <" + OWL.DatatypeProperty + "> } \n" +
//				" UNION\n" + 
//				" { ?p <" + RDF.type + "> <" + OWL.AnnotationProperty + "> } \n" + 
//				" ?p <" + RDFS.domain + "> <" + root + "> .\n" + 
//				"}\n"; 
//				try {
//					Query query =  QueryFactory.create(sparql);
//					qexec = QueryExecutionFactory.create(query, m);
//					ResultSet rslt = qexec.execSelect();
//					while (rslt.hasNext()) {
//						QuerySolution soltn = rslt.next();
//						properties.add(soltn.get("p").toString());
//					}
//					topTextProperties = properties;
//				} catch (Exception e) {
//					topTextProperties = null;
//					e.printStackTrace();
//				}
//			}
//		}
//		return topTextProperties;
//	}

	/**
	 * Answers with a set of resources serving as all the properties of the top element of the ontology
	 * which means that it is relevant to all elements of a valid model of this ontology and can be used
	 * to display list of resources in associated models with their relevant properties, customized based on their ontology.
	 * @return Set of strings of properties of the root element in the ontology.
	 */
	public Set<String> getTopProperties() {
		ARdfRepository r = getModelRepository();
		if (null == r)
			return new HashSet<String>();
		if (topPropertiesDate >= r.getDate() && null != topProperties )
			return topProperties;
		synchronized (this.getClass()) {
			if (topPropertiesDate >= r.getDate() && null != topProperties )
				return topProperties;
			topPropertiesDate = r.getDate();
			Set<String> properties = new HashSet<String>();
			Model m = r.getModel();
			getRootElement();
			QueryExecution qexec = null;
			if (null != root) {
				String sparql = "Select ?p \n" +
				"Where {\n" +
				" { ?p <" + RDF.type + "> <" + OWL.DatatypeProperty + "> } \n" +
				" UNION\n" + 
				" { ?p <" + RDF.type + "> <" + OWL.AnnotationProperty + "> } \n" + 
				" UNION\n" + 
				" { ?p <" + RDF.type + "> <" + OWL.ObjectProperty + "> } \n" + 
				" ?p <" + RDFS.domain + "> <" + root + "> .\n" + 
				"}\n"; 
				try {
					Query query =  QueryFactory.create(sparql);
					qexec = QueryExecutionFactory.create(query, m);
					ResultSet rslt = qexec.execSelect();
					while (rslt.hasNext()) {
						QuerySolution soltn = rslt.next();
						properties.add(soltn.get("p").toString());
					}
					topProperties = properties;
				} catch (Exception e) {
					topProperties = null;
					e.printStackTrace();
				}
			}
		}
		return (null == topProperties)?new HashSet<String>():topProperties;
	}

	protected void getRootElement() {
		ARdfRepository r = getModelRepository(); 
		Model m = r.getModel();
		String sparql = "Select ?sc ?c \n" +
		"Where {\n" +
		" ?c <" + RDF.type + "> <" + OWL.Class + "> .\n" + 
		" ?sc <" + RDF.type + "> <" + OWL.Class + "> .\n" + 
		" ?sc <" + RDFS.subClassOf + "> ?c .\n" + 
		"}\n";

		QueryExecution qexec = null;
		try {
			Query query =  QueryFactory.create(sparql);
			qexec = QueryExecutionFactory.create(query, m);
			ResultSet rslt = qexec.execSelect();
			Set<RDFNode> c_s = new HashSet<RDFNode>();
			Set<RDFNode> sc_s = new HashSet<RDFNode>();
			Set<RDFNode> r_s = new HashSet<RDFNode>();
			while (rslt.hasNext()) {
				QuerySolution soltn = rslt.next();
				c_s.add(soltn.get("c"));
				sc_s.add(soltn.get("sc"));
			}
			for (RDFNode ac : c_s) {
				if (false == sc_s.contains(ac))
					r_s.add(ac);
			}
			if (r_s.size() > 0) 
				root = ((Resource) r_s.toArray(new RDFNode[0])[0]).toString();
			else
				root = null;
		} catch (Exception e) {
			root = null;
			e.printStackTrace();
		}
	}
	public final String getSavedViewConfig() {
		return viewConfig;
	}
	
	/**
	 * Given an ontology description, save it as the ontology of this model element.
	 * @param od Ontology Description of an ontology.
	 * @return String indicating success (null or ""), or failure with a failure message.
	 */
	public String saveOntology(OntologyDescription od) {
        if (null == od) return "Failed to load/import ontology for item [" + id + "]";
        String version = "";
        version = od.getVersionIRI(); // will default to the base now, if missing.
        AModelRow other = getDatabase().getOntologyByClassUri(version);
        if (null != other && false == other.equals(this)) 
        	return "Failed to Import into [" + getId() + "] using same version IRI [" + version + "] as in item [" + other.getDisplayId() + "].";
        ModelRepository mr = ModelRepository.create(this);
        mr.init(od.getModel());
        mr.save();
//        try
//        {
            setOntologyDescription(od);
//            if (null != base) 
//            	setModelInstanceNamespace(base);
//            if (null != version)
//            	setVersion(version);
            setFileName("loaded"); //serverFileName);
            if (isLegal())
             	setStatus(STATUS.READY);
            else
              	setStatus(STATUS.UPDATED);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//            return "Error: failed to get and persist model with name space uri [" + base + "]. [" + e.getClass() + "]: [" + e.getMessage() + "].";
//        }
        return ""; //"Successful model loading to item [" + getId() + "].";
	}

	public final String getImportUrl() {
		return importUrl;
	}


	public void setImportUrl(String importUrl) {
		if (false == this.importUrl.equals(importUrl)) {
			this.importUrl = importUrl;
			if (false == Strings.isNullOrEmpty(this.importUrl))
					isImported = true;
			markModified();
		}
	}


	public String loadModel(String contents, String rdfContentType) {
        String base = getModelInstanceNamespace();
        if (null != base && "".equals(base.trim()))
        	base = null;
        if (false == Strings.isNullOrEmpty(getImportUrl())) 
        	return "Loading [" + getId() + "] failed. Content can only be imported from the internet.";
        OntologyDescription od = OntologyDescription.fromStream(getDatabase(), new ByteArrayInputStream(contents.getBytes()), base, rdfContentType);
        if (null == od) 
        	return "Loading [" + getId() + "] failed. Errors in the content. Check file for proper format and syntax.";
        else {
        	String ns = od.getBase();
        	Ontology other = getDatabase().getOntologyByClassUri(ns);
        	if (null != other && false == other.getId().equals(id)) 
        		return "Loading [" + getId() + "] failed. Namespace [" + ns + "] of new ontology is already in use in Ontoloy [" + other.getDisplayId() + "]";
        	else
        		return saveOntology(od);
        }
	}
}

