
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.data.Mediator.SERVICE;
import com.ibm.dm.frontService.sm.service.ARdfRepository;
import com.ibm.dm.frontService.sm.service.Repository;
import com.ibm.dm.frontService.sm.utils.Utils;

import thewebsemantic.Namespace;
import thewebsemantic.Transient;

/**
 * Class to define ports - four type of ports: post, get, pipe and repository.
 * A port is associated with an ontolgoy.
 *
 * @author shani
 */
@Namespace(Port.NAME_SPACE)
public class Port extends AModelRow
{

	@Override
	public boolean canBuild() {
		return isReady() && ! SmBlobs.ATTACHMENTS_PORT_ID.equals(getId()) && myTools().size() < 1 && (canPost() || canGet());
	}
	
	

	@Override
	public boolean canLoad() {
		// TODO Auto-generated method stub
		return isTool();
	}



	/**
	 * Answers with the number of tools which are associated with this repository.
	 * @return int count of those. In fact, only one can be associated with a port.
	 */
	public List<Port> myTools() {
		List<Port> tools = new ArrayList<Port>();
		if (!isRepository() || isTool()) // a repository may also be a tool.
			return tools;
		List<Port> ports = getDatabase().getPorts();
		for (Port p: ports) {
			if (p.isTool() && this.getId().equals(p.getField(Port.IFields.ASSOCIATED_REPOSITORY_ID)))
				tools.add(p);
		}
		return tools;
	}

	// Initialize a default columns specification for repositories.
	private static final String defaultColumn = 
		"[" + 	// "{'tag':'<i>%</i>','forText':'true', 'forView':'true','title':'Label','content':['" + RDFS.label + "']},\n" +
			" {'tag':'<i>%</i>','forText':'true', 'forView':'true','title':'Title','content':['" + DCTerms.title + "']},\n" +
				// " {'tag':'%','forText':'true', 'forView':'true','title':'Comment','content':['" + RDFS.comment + "']}," +
		" {'tag':'%','forText':'true', 'forView':'true','title':'Description','content':['" + DCTerms.description + "']}]";
	private static transient final JSONArray defaultViewConfig;
	static {
		JSONArray v = new JSONArray();
		try {
			v = new JSONArray(defaultColumn);
		} catch (Exception e) {
			e.printStackTrace();		
		}
		defaultViewConfig = v;
    }


    private Mediator.SERVICE accessibility = Mediator.SERVICE.NaN;
    private boolean isDirect = false;

	@Override
	public String getDependencyLinks(Database db) {
		return "";
	}


	@Override
    /**
     * Answers false if there is an ontology for this port and that ontology is missing.
     */
	public boolean isLegal() {
    	boolean legal = true;

//    	if (Strings.isNullOrEmpty(ontologyId) || ADatabaseRow.NaN.equals(getOntologyId()))
//   			legal = false;
    	Ontology ont = getOntology();
    	if ( null == ont || false == ont.isReady() )
       			legal = false;
    	if (Strings.isNullOrEmpty(accessName) || ADatabaseRow.NaN.equals(getAccessName()))
   			legal = false;
    	Database database = getDatabase();
    	if (PortType.export.equals(getType())) {
        	if (ADatabaseRow.NaN.equals(getFriendId()))
        		legal = false;
        	if ( null == database.getItem(getFriendId()) ||
           		false == database.getItem(getFriendId()).isReady() )
       			legal = false;
    	}
		return legal && (false == isRepository() || super.isLegal());
	}


	/**
	 * Answers with the direct service of the port (if it is a repository), and also attempts to 
	 * port to a new version where isDirect has been ported to directService.
	 * @return SERVICE type of this port.
	 */
    public SERVICE getAccessibility() {
		if (isTool()) {
			Port associated = getDatabase().getPort(associatedRepositoryId);
			if (null != associated)
				return associated.getAccessibility().inverse();
			else
				return accessibility;
		}
    	if (accessibility==SERVICE.NaN) {
    		String v = SERVICE.NaN.toString();
    		if (isRepository()) {
    				v = (isDirect?SERVICE.BOTH:SERVICE.NO_SERVICE).toString();
    		} else if (getType().equals(PortType.get))
    			v = SERVICE.IMPORT.toString();
    		else if (getType().equals(PortType.post))
    			v = SERVICE.EXPORT.toString();
   			setField(IFields.ACCESSIBILITY, v, this.accessibility.toString());
    	}
    	return accessibility ;
    }

	@Override
	public boolean canTest(Database owner) {
//		if (id.equals("Prt-276"))
//			System.out.println(id);
		return super.canTest(owner) && isTool() && canGet(); // canGet means that its content can be pulled out. When testing a tool port, 
		// the model in this repository is posted to its mediator.
//		boolean cantest = canPost() || canGet();
//		return cantest;
	}


	@Override
    public ARdfRepository initRdfRepository() {
    	if (isRepository()) {
        	Repository r = Repository.create(this);
        	return r;
    	}
    	return null;
    }

	
	@Override
	public void validateRepository(ARdfRepository r) {
		if (r != null && r.getDate() < getOntology().lastModified)
			synchronized(r.getClass()) {
				r.fixPrefixes();
			}
	}


	static public interface IFields extends AModelRow.IFields
    {
        public static final String TYPE        = "type";
        public static final String ACCESS_NAME = "accessName";
        public static final String ONTOLOGY_ID = "ontologyId";
        public static final String ASSOCIATED_REPOSITORY_ID = "associatedRepositoryId";
        public static final String FRIEND_ID   = "friendId";
		public static final String PORT_SYNC = "portSync";
		public static final String ACCESSIBILITY = "accessibility";

    }

    private static final long  serialVersionUID     = 1L;

    private static String[]    portEditableFields   = new String[] {
    	IFields.ARCHIVED,
    	IFields.VERSION,
    	IFields.NAME,
    	IFields.ACCESS_NAME,
    	IFields.FRIEND_ID,
    	IFields.TAGS,
    	IFields.ACCESSIBILITY,
    	IFields.VIEW_CONFIG,
    };
    private static String[]    toolEditableFields   = new String[] {
    	IFields.ARCHIVED,
    	IFields.VERSION,
    	IFields.NAME,
    	IFields.TAGS,
    	IFields.VIEW_CONFIG,
    };


    public static enum PortType {
    	post, //("post"),
    	get, //("get"),
    	pipe, //("pipe"),
    	repository, //("repository"),
    	export,
    	catalog,
    	tool,
    	undefined //("undefined");
    }

    protected PortType           type               = PortType.undefined;
    protected String           accessName           = "";

    protected String           ontologyId           = "";
    protected String           friendId             = "";
	protected String  		   associatedRepositoryId = "";

//    /**
//     * Answers with the possible tpyes of ports supported
//     *
//     * @return
//     */
//    public final String[] getTypes()
//    {
//        return new String[] { PortType.PORT_TYPE_GET.toString(), PORT_TYPE_POST, PORT_TYPE_PIPE, PORT_TYPE_REPOSITORY };
//    }

    // Ports will take the top properties from the associated ontology.
    @Override
	public Set<String> getTopProperties() {
    	try {
			Ontology o = getOntology();
			if (null != o)
				return o.getTopProperties();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new HashSet<String>();
	}


    /**
     * Looks for the edited view config of the owner ontology.
     * If there is no such modified version, take the default one for Ports rather 
     * than the default one for ontologies.
     */
	@Override
	public JSONArray getDefaultViewConfig() {
		JSONArray result = null;
		Ontology ont = getOntology();
		if (null != ont)
			result = ont.getViewConfig();
		if (null == result)
			return defaultViewConfig;
		return result;
	}


	public Ontology getOntology() {
		try {
			return getDatabase().getOntology(getOntologyId());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public Port(PortType type, boolean... isTemp)
    {
        super(isTemp);
    	this.type = type;
    }

    public Port() {
    }

    /**
     * Answers with a new Port which is a temporary copy of the given Id
     */
    public Port(PortType type, String id) {
    	super(true); // is temporary
    	this.type = type;
    	this.id = id;
	}

	public Port(boolean[] isTemp) {
		super(isTemp);
	}


	@Override
    public String[] getEditableFieldNames()
    {
		if (isTool())
			return toolEditableFields;
        return portEditableFields;
    }

    @Override
    public boolean canShow()
    {
        return isRepository() && false == Strings.isNullOrEmpty(getAccessName()); // do not add isLegal() here,
        // Showing the content is only if there is some content to show. Right?
    }
    
    @Override
    public boolean canClear()
    {
    	return super.canClear() && canShow() && GRAPHIC_YES == portSync;
    }

	public String getAccessName()
    {
        return accessName;
    }

    public void setAccessName(String accessName)
    {
        this.accessName = accessName;
        markModified();
    }

    public final String getOntologyId()
    {
        return ontologyId;
    }


    public void setOntologyId(String ontologyId)
    {
        this.ontologyId = ontologyId;
    }

    public final String getFriendId()
    {
        return friendId;
    }


    public void setFriendId(String friendId)
    {
        this.friendId = friendId;
    }

    public void setField(String field, String value)
    {
        super.setField(field, value);
        if (field.equalsIgnoreCase(IFields.TYPE))
            this.type = PortType.valueOf(value);
        else
            if (field.equalsIgnoreCase(IFields.NAME))
                this.name = value;
            else
                if (field.equalsIgnoreCase(IFields.ACCESS_NAME))
                    this.accessName = value;
                else
                    if (field.equalsIgnoreCase(IFields.ONTOLOGY_ID))
                    	this.ontologyId = value;
                    else
                    	if (field.equalsIgnoreCase(IFields.FRIEND_ID))
                    		this.friendId = value;
                        else
                        	if (field.equalsIgnoreCase(IFields.ACCESSIBILITY))
                        		this.accessibility = SERVICE.get(value);
                        	else
                        		if (field.equalsIgnoreCase(IFields.ASSOCIATED_REPOSITORY_ID))
                        			this.associatedRepositoryId = value;
    }

    public String getField(String field)
    {
        if (field.equalsIgnoreCase(IFields.TYPE.toString()))
            return this.getType().toString();
        else
            if (field.equalsIgnoreCase(IFields.NAME.toString()))
                return this.name;
            else
                if (field.equalsIgnoreCase(IFields.ACCESS_NAME.toString()))
                    return this.accessName;
                else
                    if (field.equalsIgnoreCase(IFields.ONTOLOGY_ID.toString()))
                        return this.ontologyId;
                    else
                    	if (field.equalsIgnoreCase(IFields.FRIEND_ID.toString()))
                    		return this.friendId;
                    	else
                    		if (field.equalsIgnoreCase(IFields.PORT_SYNC.toString()))
                    			return canSync() ? this.portSync : SYNC_IRRELEVANT;
                            else
                                if (field.equalsIgnoreCase(IFields.ACCESSIBILITY))
                                    return this.accessibility.toString();
                            	else
                            		if (field.equalsIgnoreCase(IFields.ASSOCIATED_REPOSITORY_ID))
                            			return this.associatedRepositoryId;

        return super.getField(field);
    }

    private boolean canSync() {
		// TODO Auto-generated method stub
		return isReady() && canShow() && ! isTool() && ! SmBlobs.ATTACHMENTS_PORT_ID.equals(getId());
	}


	@Override
	public String getModelInstanceNamespace() {
    	Repository r = (Repository) getModelRepository();
    	return (null != r)?r.getBase():null;
	}


	@Override
    public String getReferenceName(Database db, String field)
    {
    	if (field.equalsIgnoreCase("ontologyId")) {
    		Ontology ont = getOntology();
    		if (null != ont && ont.isReady()) return ont.getName();
    		return (null == ont)?"!!missing!!" : "!!noReady!!";
    	}
    	else if (field.equalsIgnoreCase("friendId")) {
    		Friend frn = db.getFriend(getFriendId());
    		if (null != frn && frn.isReady()) return frn.getName();
    		return (null == frn)?"!!missing!!" : "!!noReady!!";
    	}
    	else if (field.equalsIgnoreCase(Port.IFields.ASSOCIATED_REPOSITORY_ID)) {
    		Port associated = db.getPort(getField(field));
    		if (null != associated && associated.isReady()) return associated.getName();
    		return (null == associated)?"!!missing!!" : "!!noReady!!";
    	}

    	return super.getReferenceName(db, field);
    }

	/**
	 * Answers with the remote ontology of the exporter port based on its access name and the friend
	 * is is associated with.
	 * @param port Port of type Export.
	 * @param database
	 * @return name space of the remote ontology associated with the access name on the remote friend.
	 * @throws JSONException 
	 */
    private String resolveRemoteOntology(JSONObject port, Database database) throws JSONException {
    	Friend friend = database.getFriend((String)Utils.safeGet(port, Port.IFields.FRIEND_ID));
    	String accessName = (String)Utils.safeGet(port, Port.IFields.ACCESS_NAME);

    	if (null != friend && friend.isReady()) {
    		try {
//        		com.ibm.haifa.smc.client.oauth.OAuthCommunicator comm = friend.getCommunicator();
        		HttpGet query = new HttpGet("https://" + friend.getIpAddress() + "/dm/server/management/query?post=" + accessName);
        		query.setHeader(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
        		HttpResponse resp = friend.execute(query);
        		int code = resp.getStatusLine().getStatusCode();
        		if (HttpStatus.SC_OK == code) {
        			JSONObject data = Utils.jsonFromString(Utils.stringFromStream(resp.getEntity().getContent()));
        			if (data.has("namespace")) {
        				String namespace = Utils.safeGet(data, "namespace").toString();
        				String version = Utils.safeGet(data, "version").toString();
        				List<Ontology> ontologies = database.getOntologies();
        				for (Ontology ont : ontologies) {
        					if (namespace.equals(ont.getModelInstanceNamespace()))
        						if (version.equals("") || version.equals(ont.getVersion())) {
        							String ontologyId = ont.getId();
        							if (null == ontologyId) ontologyId = "";
        							return ontologyId;
        						}
        				}
        			} else
        				return "Update [" + getDisplayId() + "] failed to find remote access name [" + accessName + "].";  
        		}
    		} catch (Exception e) { // String must start with "Update" to indicate it is not a good response with an ontology ID.
    			return "Update [" + getDisplayId() + "] failed to obtain remote ontology for Friend [" + friend.getDisplayId() + "] and access name [" + accessName + "], with error [" + e.getMessage() + "]";
    		}
    	}
		return "";
    }

	
    @Override
    String update(String field, String v, JSONObject jUpdate, Map<String, String> params) throws JSONException
    {
        if (false == IFields.ACCESS_NAME.equalsIgnoreCase(field) && 
        		false == IFields.VERSION.equalsIgnoreCase(field))
            return super.update(field, v, jUpdate, params);
        if (IFields.VERSION.equalsIgnoreCase(field)) {
        	if (Strings.isNullOrEmpty(version) || false == version.equals(v)) {
        		version = v;
        		markModified();
        	}
        	return null;
        }
        for (Port otherPort : getDatabase().getPorts()) {
            if (false == Strings.isNullOrEmpty(v) &&
            		v.equals(otherPort.getAccessName()) && 
            		false == otherPort.equals(this) &&
                    ( getType().equals(otherPort.getType()) || 
                    		(canGet() && otherPort.canGet()) ||
                    		(canPost() && otherPort.canPost())
                    ))
                return "Error: [" + getDisplayId() + "] Access name [" + v + "] overlaps another accessible port [" + otherPort.getDisplayId() + "]";
        	if (false == otherPort.isReady())
        		continue;
        }
        // Check that the access name is not changed where the port is repository and it is not empty.
        if (isRepository() && false == v.equals(getField(IFields.ACCESS_NAME))) {
//        	if (isReady() && false == isEmpty())
//        		return "Error: Active repository Port [" + getDisplayId() + "] cannot change access name.";
        	Repository r = (Repository)getModelRepository();
        	if (isReady() && null != r && false == r.isEmpty())
        		return "Error: Repository Port [" + getDisplayId() + "] must be empty before access name can change!";
        	else if (null != r) { // need to reset the repository and reinitialize it.
        		if (false == v.equals(accessName)) {
        			accessName = v;
        			markModified();
        		} else
        			return null;
        		// now update the access name in the repository
        		r.reset();
        		return null;
        	}
        }
        if (getType().equals(PortType.export)) {
        	String ontologyId = resolveRemoteOntology(jUpdate, getDatabase());
        	if (ontologyId.startsWith("Update"))
        		return ontologyId;
        	if (Strings.isNullOrEmpty(ontologyId.trim()))
        		return "Update [" + getDisplayId() +"] failed to locate a local ontology for remote access name [" + v + "]"; 
        	setField("ontologyId", ontologyId, this.ontologyId);
//            jUpdate.put("ontologyId", ontologyId);
        }

        String report = super.update(field, v, jUpdate, params);
        return report;
    }

	/**
	 * Answers whether the repository port is empty or has some resources in it.
	 * @return
	 */
    public boolean isEmpty() {
    	if (false == isRepository())
    		return true;
    	ARdfRepository r = getModelRepository();
    	if (null == r)
    		return true;
    	if (false == r instanceof Repository)
    		return false;
    	return ((Repository)r).isEmpty();
	}





	@Override
    public void integrityValidation(ADatabaseRow changed, Collection<? extends ADatabaseRow> all) throws IntegrityValidationException
    {
        Port changedPort = (Port) changed;
        String v = ((Port) changed).getAccessName();

        for (ADatabaseRow other : all)
        {
            Port otherPort = (Port) other;
            if (v.equals(otherPort.getAccessName()) && false == otherPort.equals(this) && changedPort.type.equals(otherPort.type))
                throw new IntegrityValidationException("Error: Access name [" + v + "] overlaps another similar port [" + otherPort.getId() + "]");
        }

    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((accessName == null) ? 0 : accessName.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((ontologyId == null) ? 0 : ontologyId.hashCode());
        result = prime * result + ((friendId == null) ? 0 : friendId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        Port other = (Port) obj;
        if (accessName == null)
        {
            if (other.accessName != null) return false;
        }
        else
            if (!accessName.equals(other.accessName)) return false;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else
            if (!name.equals(other.name)) return false;
        if (ontologyId == null)
        {
            if (other.ontologyId != null) return false;
        }
        else
            if (!ontologyId.equals(other.ontologyId)) return false;
        if (friendId == null)
        {
            if (other.friendId != null) return false;
        }
        else
            if (!friendId.equals(other.friendId)) return false;
        if (type == null)
        {
            if (other.type != null) return false;
        }
        else
            if (!type.equals(other.type)) return false;
        return true;
    }

    public static Port generateRandom()
    {
        Port o = new Port(Port.PortType.pipe);
        o = o.generateRandomBase(o);
        o.accessName = "accessName_value";
        o.ontologyId = "ontologyId_Value";
        o.friendId = "friendId_Value";
        o.type = PortType.pipe;
        o.archived = true;
        //o.collectionName = "COLLECTION_NAME_Port";
        o.dateCreated = U.roundDateToSecond(null);
        o.lastModified = U.roundDateToSecond(null);
        o.dirty = false;
        o.lastModified = U.roundDateToSecond(null);
        o.name = "NAME_VALUE_Port";
        o.status = "STATUS VALUE_Port";
        o.version = "VERSION VALUE_Port";
        return o;
    }

    //    @Override
    public String _toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" {\n\tname: ");
        builder.append(name);
        builder.append("\n\ttype: ");
        builder.append(type);
        builder.append("\n\taccessName: ");
        builder.append(accessName);
        builder.append("\n\tontologyId: ");
        builder.append(ontologyId);
        builder.append("\n\tfriendId: ");
        builder.append(friendId);
        builder.append("\n\tversion: ");
        builder.append(version);
        builder.append("\n\tstatus: ");
        builder.append(status);
        builder.append("\n\tarchived: ");
        builder.append(archived);
        builder.append("\n\tcollectionName: ");
        builder.append(collectionName);
        builder.append("\n\tdateCreated: ");
        builder.append(new Date(dateCreated).toString());
        builder.append("\n\tdirty: ");
        builder.append(dirty);
        builder.append("\n\tid: ");
        builder.append(id);
        builder.append("\n\tidURI: ");
        builder.append(idURI);
        builder.append("\n\tbaseURL: ");
        builder.append(baseURL);
        builder.append("\n\tlastModified: ");
        builder.append(new Date(lastModified).toString());
        builder.append("\n\tetag: ");
        builder.append(etag);
        builder.append("\n\ttags: ");
        builder.append(tags);
        builder.append("\n}");
        return builder.toString();
    }

    public PortType getType()
    {
        return (null == type)?PortType.undefined : type;
    }

    public void setType(String type)
    {
        this.type = PortType.valueOf(type);
    }
    public void setType(PortType type)
    {
    	this.type = type;
    }
    /**
     * Anwsers positively if the port is ready and its type is either a POST or a GET.
     * @return boolean to say the port is a usable RESTful port.
     */
	public final boolean isRestful() {
		PortType t = getType();
		if (false == isReady())
			return false;
		boolean result = (PortType.get == t) || (PortType.post == t);
		return result;
	}

    @Transient
    protected transient int syncCounter = 0;
    @Transient
    protected transient String portSync = GRAPHIC_YES;

    public synchronized void sync() {
    	if (--syncCounter <= 0)
    		syncReset();
    }

    public synchronized void unsync() {
    	++syncCounter;
    	this.portSync = GRAPHIC_NO;
    }

    public synchronized void syncReset() {
    	syncCounter = 0;
    	this.portSync = GRAPHIC_YES;
    }

    public synchronized boolean isSync() {
    	return GRAPHIC_YES.equals(this.portSync);
    }

	public JSONObject markReachablePorts(Database db, Set<String> reachablePorts) throws JSONException {
		unsync();
		List<Mediator> mediators = db.getMediators();
		JSONObject flow = new JSONObject();
		JSONArray list = new JSONArray();
		flow.put(getId(), list);

		for (Mediator mdtr : mediators) {
			if ( ! mdtr.isLegal())
				continue;
			// Other side of mediator should be either a pipe or a repository
			if (Mediator.SERVICE.BOTH.equals(mdtr.checkService(db)) &&
					mdtr.getInputPortId().equals(this.getId()) &&
					!reachablePorts.contains(mdtr.getOutputPortId()))
			{
				list.put(mdtr.getId());
				JSONObject jo = mdtr.markReachablePorts(db, reachablePorts);
				Iterator<?> keys = jo.keys();
				while (keys.hasNext()) {
				    String key = (String)keys.next(); // JSONObjectgetNames(jo)) {
					flow.put(key, Utils.safeGet(jo, key));
//					flow.putAll(mdtr.markReachablePorts(db, reachablePorts));
				}
			}
		}

		return flow;
	}

	public boolean canPost() {
		return getType().equals(PortType.post) ||
			getAccessibility().equals(Mediator.SERVICE.EXPORT) ||
			getAccessibility().equals(Mediator.SERVICE.BOTH);
	}
	public boolean canGet() {
		return getType().equals(PortType.get) ||
			getAccessibility().equals(Mediator.SERVICE.IMPORT) ||
			getAccessibility().equals(Mediator.SERVICE.BOTH);
	}
	/**
	 * Answers true if the port is also a repository of some kind.
	 * @return
	 */
	public boolean isRepository() {
		switch(type) {
 			case repository: return true;
 			case tool: return true;
 			default: return false;
		}
	}

	public boolean isTool() {
		return type == PortType.tool;
	}
	public boolean isCatalog() {
		return type == PortType.catalog;
	}

	/**
	 * Limited usage to clear the repository of attachments - so attachments are deleted.
	 */
	public void clear(ARdfRepository repository) {
		Model model = repository.getModel();
		StmtIterator stmts = model.listStatements();
		while (stmts.hasNext()) {
			String object = stmts.next().getObject().toString();
			if (SmBlobs.deleteResource(object)) {
				System.out.println("Object [" + object + "] identified as a blob and cleared.");
			}
		}
	}


	@Override
	public String getDescription() {
		String ar = "";
		if (getId().equals(SmBlobs.ATTACHMENTS_PORT_ID))
			ar = ". Attachments Repository.";
		return super.getDescription() + (isCatalog()?" Catalog.":" Type: " + getType().toString()) + ar;		
	}

	/**
	 * Answers whether a tool Port can post to an associated repository.
	 * @return boolean
	 */
	public boolean canPostAsTool() {
		Port myRepository = getDatabase().getPort(associatedRepositoryId);
		return isTool() && null != myRepository && myRepository.isRepository() && ! myRepository.isTool() &&
				myRepository.canPost();
	}

	/**
	 * Answers whether a tool Port can get from an associated repository.
	 * @return boolean
	 */
	public boolean canGetAsTool() {
		Port myRepository = getDatabase().getPort(associatedRepositoryId);
		return isTool() && null != myRepository && myRepository.isRepository() && ! myRepository.isTool() &&
				myRepository.canGet();
	}

	/**
	 * Answers whether the tool needs to post new content since it has been updated since the last
	 * post to the associated repository, and thus initiate a mediation.
	 * @return true if contents needs to be posted to the associated repository.
	 */
	public boolean needPostAsTool() {
		Port myRepository = getDatabase().getPort(associatedRepositoryId);
		return isTool() && null != myRepository && myRepository.isRepository() && ! myRepository.isTool() &&
				myRepository.canPost() && getDateModifiedAsLong() > myRepository.getDateModifiedAsLong();
	}

	/**
	 * Answers whether the tool needs to get new content since the associated repository has been changed (perhaps
	 * via mediation).
	 * @return true if contents needs to be updated.
	 */
	public boolean needGetAsTool() {
		Port myRepository = getDatabase().getPort(associatedRepositoryId);
		return isTool() && null != myRepository && myRepository.isRepository() && ! myRepository.isTool() &&
				myRepository.canGet() && getDateModifiedAsLong() < myRepository.getDateModifiedAsLong();
	}

	/**
	 * Answers with the model of the port, or null if there is not model for it.
	 * @return Model or null.
	 */
	public Model getModel() {
		ARdfRepository r = getModelRepository();
		if (null != r)
			return r.getModel();
		return null;
	}
}

