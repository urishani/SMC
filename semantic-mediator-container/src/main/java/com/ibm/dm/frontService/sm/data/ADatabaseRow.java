
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.service.ARdfRepository;
import com.ibm.dm.frontService.sm.utils.Utils;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.data.AbstractRDFReadyClass;

import thewebsemantic.Transient;

// Generic class for common properties and methods for an item in any collection of items.
public abstract class ADatabaseRow extends AbstractRDFReadyClass
{
    static public interface IFields extends AbstractRDFReadyClass.IFields
    {
        public static final String VERSION         = "version";
        public static final String STATUS          = "status";
        public static final String ARCHIVED        = "archived";
        public static final String DATE_CREATED    = "dateCreated";
        public static final String COLLECTION_NAME = "collectionName";
        public static final String TAGS			   = "tags";
//        public static final String DIRTY           = "dirty";
		public static final String DATE_MODIFIED   = "dateModified";
		public static final String COMMENTS   	   = "comments";
    }

    public final boolean equals(ADatabaseRow other) {
    	return (other.getClass().equals(getClass())) &&
    	   other.getId().equals(getId());
    }
    
    private String msg = "";
    
    protected static final String GRAPHIC_YES = "<img src='/dm/resources/graphics/icons/tick.gif' width='25' height='20' id='Yes'>";
    protected static final String GRAPHIC_NO  = "<img src='/dm/resources/graphics/icons/cross.gif'  width='25' height='20' id='No'>";
    public static final String SYNC_IRRELEVANT = "&nbsp;";

    private static final long             serialVersionUID = 1L;
    public static final String            NaN              = "NaN";
    /***
     * interesting only for DTO and serialization (needed for JSON format )
     */
    protected transient static DateFormat dateFormatD      = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, Locale.ITALY);
    protected transient static DateFormat dateFormatT      = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.ITALY);
    protected boolean isPermanent = false;

    /**
     * To be used to set special properties of this item which means it is not user-defined, but
     * system defined.
     * @param isPermanent
     */
    void setPermanent(boolean isPermanent) {
    	this.isPermanent = isPermanent;
    }
    
    
    void setMessage(String msg) {
    	this.msg = msg;
    	markModified();
    }
    
    public String getMessage() {
    	return msg;
    }
    
    /**
     * Returns a description that is automaticly made.
     * @return
     */
    public String getDescription() {
    	return getId() + ": SMC " + getCollectionName() + " item.";
    }
    //    /**
    //     * user visible ID column -- build from class name and counter
    //     */
    //    String                                id          = "";
    protected String                      version          = "";
    protected String                      status           = "";

    @Transient
    protected transient boolean           dirty            = false;

    /**
     * TODO: Boris track this down and replace
     */
    public interface STATUS
    {
        public static final String READY   = "ready";
        public static final String CREATED = "created";
        public static final String UPDATED = "updated";
        public static final String ERROR   = "error";

    }

    protected boolean          archived       = false;
    protected String 			tags = "";
    public final String getTags() {
    	return tags;
    }
    /**
     * TODO: computed value - should be simpleClassName
     */
    protected final transient String collectionName = this.getClass().getSimpleName();
    protected long  dateCreated = U.roundDateToSecond(null);
	protected boolean mIsTemp = false;
	private transient ARdfRepository mRdfRepository = null;
	protected String comments = "";
	
	public boolean hasModelRepository() {
		return null != mRdfRepository;
	}
	
    public void markModified(long t)
    {
    	if (t < 0)
    		lastModified = U.roundDateToSecond(null);
//    	else lastModified = t;
    	boolean modified = setLastModified(new Date(t));
        dirty = dirty || modified;
    }
    public void markModified() {
    	markModified(U.roundDateToSecond(null));
    }

//    @Deprecated
//    public void setCollection(String collection)
//    {
//       //  collectionName = collection; // this is set automatically from the class name.
//    }

    @Transient
    public boolean isDirty()
    {
        return dirty;
    }

    /**
     * Answers with a Json in a String
     * @return
     */
    public String toJson() {
        return U.getGson().toJson(this);
    }

    protected ADatabaseRow(boolean... isTemp) {
    	super();
    	setTemporary(Utils.isOptional(false, isTemp));
	}

    public Database getDatabase() {
    	if (null != owner && owner instanceof Database)
    		return (Database)owner;
    	else return null; 
//    		return Database.create();
    }
	// Getters
    public String getId()
    {
        return id;
    }

    public String getDisplayId()
    {
        if (null == displayId)
        	return id + ":" + getName();
        return displayId;
    }

    public String getModelDataId()
    {
        return this.modelDataId;
    }

    public String getVersion()
    {
        return version;
    }
    public String getVersionSuffix()
    {
    	String v = version;
    	if (false == Strings.isNullOrEmpty(v) && v.lastIndexOf('/') >= 0)
    		v = v.substring(v.lastIndexOf('/'));
    	return v;
    }

    public void setVersion(String version)
    {
    	if (false == this.version.equals(version)) {
    		this.version = version;
    		markModified();
    	}
    }

    public String getStatus()
    {
        return status;
    }

    public boolean isReady()
    {
        return isTemporary() || (false == isArchived() && getStatus().equals(STATUS.READY));
    }

    /**
     * Answers true if item is legal and ready to be used.
     *
     * @return boolean
     */
    public boolean isLegal()
    {
        return true;
    }

    /**
     * Sets the status according to readiness ofthe item and whether it has been modified.
     */

    public void setStatus(String status)
    {
        if (this.status.equals(status))
        	return;
        this.status = status;
        markModified();
    }

    /**
     * Answers whether status has been changed
     * @param database
     * @return
     */
    public boolean setStatus(Database database)
    {
    	String theStatus = getStatus();
        if (isDirty())
        	setStatus(STATUS.UPDATED);
//        else if (ADatabaseRow.STATUS.CREATED.equals(getStatus())) // avoid changing the "created" status
//        	return false;
        if (isLegal()) {
        	if (isValid()) setStatus(STATUS.READY);
        	else setStatus(STATUS.ERROR);
        } else setStatus(STATUS.UPDATED);
        return false == getStatus().equals(theStatus);
    }

    /**
     * Validator of the item. To be implemented by the different items to reflect
     * whether the item is valid. That will validate compatibility of things in the item
     * such as references to other items, compatibility of ontologies and rules, etc.
     * @return true by default - item is valid (if it is legal since isLegal() is called
     * before this method is used.
     */
    private boolean isValid() {
		return true;
	}

    /**
     * Getter for the date created field.
     *
     * @return String of the date, which is itself a number.
     */
    public String getDateCreated()
    {
        return dateFormatD.format(dateCreated) + " " + dateFormatT.format(dateCreated);
    }
    
    public String getComments() { return this.comments; }
    
    /**
     * Appends new comments to this item if new.
     * @param comments
     */
    public void setComments(String comments) {
    	if (false == Strings.isNullOrEmpty(comments) && this.comments.indexOf(comments) < 0) {
    		if (Strings.isNullOrEmpty(this.comments))
    			this.comments = "";
    		this.comments += ((this.comments.length() > 0)? " / ":"") + comments;
    		markModified();
    	}
    }

    public long getDateCreatedAsLong() {
    	return dateCreated;
    }
    public long getDateModifiedAsLong() {
    	return lastModified;
    }
    /**
     * Getter for the date modified field.
     *
     * @return String of the date, which is itself a number.
     */
    public String getDateModified()
    {
        return dateFormatD.format(lastModified) + " " + dateFormatT.format(lastModified);
    }

    /**
     * Sets up a value for a field, looking it up by its name.
     *
     * @param field
     *            String name of the field to look up.
     * @param value
     *            New String value to assign this field - if found.
     */
    public void setField(String field, String value)
    {
        if (field.equalsIgnoreCase(IFields.ID))
            this.id = value;
        else
            if (field.equalsIgnoreCase(IFields.VERSION))
                this.version = value;
            else
                if (field.equalsIgnoreCase(IFields.STATUS))
                    this.status = value;
                else
                    if (field.equalsIgnoreCase(IFields.ARCHIVED))
                    	this.archived = Boolean.parseBoolean(value);
                    else
                        if (field.equalsIgnoreCase(IFields.TAGS))
                            this.tags = value;
   }

   /**
    * Sets a new value for the field, and marks it as modified if the value parameter differs from 
    * the present value.  
    * @param fieldName String for the field name
    * @param newValue String for the new value
    * @param oldValue String for the old value
    */
   protected final void setField(String fieldName, String newValue, String oldValue) {
	   if (newValue != null && false == newValue.equals(oldValue)) {
		   setField(fieldName, newValue);
		   markModified();
	   }
	}
   
   public final String toString() {
       return U.getGson().toJson(this).toString();
   }


    /**
     * Scans the fields to match the field parameter and return its stored value.
     * <br>
     * Always returns a string, which may be empty, but never null.
     * @param field
     *            String field name.
     * @return String value of that field.
     */
    public String getField(String field)
    {
    	String result = "";
        if (field.equalsIgnoreCase(IFields.ID))
        	result = this.id;
        else
            if (field.equalsIgnoreCase(IFields.VERSION)) {
                String v = this.version;
//                if (false == Strings.isNullOrEmpty(v) && v.lastIndexOf('/') >= 0)
//                	return v.substring(v.lastIndexOf('/'));
//                else
                result =  v;
            } else
                if (field.equalsIgnoreCase(IFields.STATUS))
                	result =  this.status;
                else
                    if (field.equalsIgnoreCase(IFields.DATE_CREATED))
                    	result =  this.getDateCreated();
                    else
                        if (field.equalsIgnoreCase(IFields.DATE_MODIFIED))
                        	result =  this.getDateModified();
                        else
                            if (field.equalsIgnoreCase(IFields.ARCHIVED))
                            	result =  String.valueOf(this.archived);
                            else
                                if (field.equalsIgnoreCase(IFields.TAGS))
                                	result =  this.tags;
                                else if (field.equalsIgnoreCase(IFields.COMMENTS))
                                	result =  this.comments;
                                else
                                	result =  "";
        return Strings.isNullOrEmpty(result) ? "" : result;
    }

    /**
     * Return the value of a field, and if that one is null or empty, the
     * value "NaN".
     *
     * @param field
     *            String name of the field.
     * @return A String. Never returns a null.
     */
    public String getFieldOrNaN(String field)
    {
        String result = getField(field);
        if (Strings.isNullOrEmpty(result))
            return NaN;
        else
            return result;
    }

    public String getDispalyName(Database db, String field)
    {
    	String result = getFieldOrNaN(field);
    	if (!NaN.equals(result)) {
    		String refName = getReferenceName(db, field).trim();
    		if (false == "".equals(refName))
    				result += ":" + refName;
    	}
    	return result;
    }

    public String getReferenceName(Database db, String field)
    {
    	return "";
    	//throw new NoSuchMethodError("for: " + this.getClass() + "(" + field + ")");
    }

    public String getCollectionName()
    {
        return collectionName;
    }

    public abstract String[] getEditableFieldNames();

    public boolean isArchived()
    {
        return archived;
    }

    public boolean hasNameSpace()
    {
        return false;
    }

//    public boolean hasFileName()
//    {
//        return false;
//    }

    public boolean canDelete()
    {
        return false == this.isPermanent && isArchived();
    }

    public boolean canEdit()
    {
    	return false == this.isPermanent;
    }

    public boolean canShow()
    {
        return false;
    }

    public boolean canClear()
    {
        return true; // TODO restore back with this condition. false == this.isPermanent;
    }

    public boolean canTest(Database owner)
    {
        return isReady();
    }

	public boolean isPermanent() {
		return isPermanent;
	}

    /**
     * Updates a specific row with a map from a Json object.
     *
     * @param jUpdate
     *            JSONObject with a map of relevant fields to update
     * @throws JSONException 
     */
    @SuppressWarnings("unchecked")
    public String update(JSONObject jUpdate, Map<String, String> params) throws JSONException
    {
    	boolean wasArchived = isArchived();
    	boolean wasReady = isReady();
        String msg = null;
       // Set<String> keys = jUpdate.keySet();
        String[] orderedKeys = getEditableFieldNames();
        for (String field: orderedKeys) 
        {
        	if (false == jUpdate.has(field) || jUpdate.isNull(field)) continue;
            String v = Utils.stringify(Utils.safeGet(jUpdate, field));
            String oldV = getField(field);
            if (Strings.isNullOrEmpty(oldV) || false == oldV.equals(v))
            	msg = update(field, v, jUpdate, params);
            if (null != msg && false == "".equals(msg.trim())) break;
        } 
        boolean changed = setStatus(getDatabase()); 
        dirty = dirty || changed;
        if (isDirty() && (
        		wasArchived != isArchived() ||
        		wasReady != isReady())) { // Status changed in a way that can affect other items
        	getDatabase().updateStatuses();
        }
//        if (jUpdate.get("archived"))
        return msg;
    }

    /**
     * Update a field according to items subclassing.
     *
     * @param field
     *            String for the field name
     * @param v
     *            String for the value. Cannot be null
     * @param jUpdate
     *            An JSONObject with all updated fields and values.
     * @return String that is not empty or null to indicate an error to be reported.
     */
    String update(String field, String v, JSONObject jUpdate, Map<String, String> params)  throws JSONException
    {
        String oldValue = getField(field);
    	if (oldValue.equals(v)) // nothing to change.
    		return null; 
//    	if (IFields.ARCHIVED.equals(field)) {
//    		boolean oldValue = Boolean.parseBoolean(getField(field));
//    		boolean newValue = Boolean.parseBoolean(v);
//    		if (oldValue == newValue) return null;
//    	}
        setField(field, v, oldValue); // This now does all the verification and marking as modified.
//        markModified();
        return null;
    }

    public abstract void integrityValidation(ADatabaseRow changed, Collection<? extends ADatabaseRow> all) throws IntegrityValidationException;

    public void setArchived(boolean archived)
    {
    	if (this.archived != archived) {
    		markModified();
    		this.archived = archived;
    	}
    }

//    @Deprecated
//    public void setCollectionName(String collectionName)
//    {
//       // this.collectionName = collectionName; // this is set automatically.
//    }

    public void setDateCreated(Date dateCreated)
    {
        this.dateCreated = dateCreated.getTime();
    }

//    /**
//     * Sets the modified date if later then the present modified date.
//     * Than will mark this item as modified to it is persisted.
//     * @param dateModified Date of modification to be updated.
//     */
//    public void setDateModified(Date dateModified)
//    {
//       long t = dateModified.getTime();
//       if ( this.lastModified < t) {
//    	   markModified(t);
//       }
//    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (archived ? 1231 : 1237);
        Long dc = dateCreated;
        result = prime * result + ((dc == null) ? 0 : dc.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        ADatabaseRow other = (ADatabaseRow) obj;
        if (archived != other.archived) return false;
        Long dc = dateCreated, odc = other.dateCreated;
        if (!dc.equals(odc)) return false;
        if (status == null)
        {
            if (other.status != null) return false;
        }
        else
            if (!status.equals(other.status)) return false;
        if (version == null)
        {
            if (other.version != null) return false;
        }
        else
            if (!version.equals(other.version)) return false;
        return true;
    }

    /**
     * Utility to be used by relevant subclasses to connect items to repositories holding the
     * relevant RDF models of the items
     */
    protected final void createRdfRepository() {
    	if (null == mRdfRepository) synchronized (ADatabaseRow.class) {
    		if (null == mRdfRepository)
    			mRdfRepository = initRdfRepository();
		}
    	validateRepository(mRdfRepository); 
    }
    
    public void validateRepository(ARdfRepository r) {
    	// Check the prefixes of internal ontologies (and rules)
    	Model m = r.getModel();
    	Map<String, String> pmap = m.getNsPrefixMap();
    	AModelRow item = r.getMyItem();
    	Database db =item.getDatabase();
        List<AModelRow> items = new ArrayList<AModelRow>();
        items.addAll(db.getOntologies());
        items.addAll(db.getRules());
        Map<String, String> pchange = new HashMap<String, String>();
        for (String pref: pmap.keySet()) {
        	String ns = pmap.get(pref);
        	AModelRow oItem = AModelRow.findModelForNameSpace(items, ns);
        	if (null != oItem && false == oItem.getPrefix().equals(pref)) {
        		pchange.put(pref, oItem.getPrefix());
        	}
        }
        if (pchange.size() > 0) {
        	System.err.println("Prefix fixing: " + Utils.mapToString(pchange) );
        	for (String pref: pchange.keySet()) {
        		String ns = pmap.get(pref);
        		pmap.remove(pref);
        		pmap.put(pchange.get(pref), ns);
        	}
        	m.setNsPrefixes(pmap);
        	r.setDirty();
        	item.markModified();
        }
    }; // do nothing here. 
    
    protected ARdfRepository initRdfRepository() {
    	return null;
    }

	public final String getContents(String contentType) {
		ARdfRepository r = getModelRepository();
		if (null != r) {
			Model m = r.getModel();
			if (null != m)
				return Utils.modelToText(m, contentType);
		}
		return null;
	}

	public final ARdfRepository getModelRepository() {
		if (isLegal())
				createRdfRepository();
		return mRdfRepository;
	}

	public final byte[] getContentsAsBytes(String contentType) {
		String c = getContents(contentType);
		if (null == c)
			return null;
		return c.getBytes();
	}

	public boolean canLoad() {
		return false;
	}

	public boolean canImport() {
		return false;
	}

	public String getDependencyLinks(Database db) {
		return "";
	}

	public String getName() {
		return "";
	}

	/**
	 * Clears contents of the row item after it is being deleted from the database
	 */
	public String deleteContents() {
		if (! canDelete())
			return "Error: cannot delete [" + getId() + "]";
//		String fileName = getFileName();
//		if (Strings.isNullOrEmpty(fileName))
//			return "Error: cannot delete [" + getId() + "]";
		ARdfRepository r = getModelRepository();
		if (null != r)
			return r.deleteRepository();
		return "[" + getId() + "] deleted.";
//		File file = new File(fileName);
//		String parent = file.getParentFile().getName();
//		if (parent.equals(getId())) { // it is the new style - remove all files in the parent (id) folder:
//			File parentf = file.getParentFile();
//			String files[] = parentf.list();
//			for (String f : files) {
//				boolean done = new File(parentf, f).delete();
//				if (! done)
//					return false;
//			}
//			return parentf.delete();
//		}
//		return file.delete();
//
	}

	public String getFileName() {
		return null;
	}

	/**
	 * Used only at generation time.
	 * @param isTemp boolean indicating this is a temporary item and it should be
	 * used accordingly.
	 */
	protected void setTemporary(boolean isTemp) {
		mIsTemp = isTemp;
	}

	/**
	 * Answers with an indication if this item is a temporary one.
	 * @return boolean
	 */
	public boolean isTemporary() {
		return this.mIsTemp;
	}

    public String getEtag() {
    	ARdfRepository r = getModelRepository();
    	if (null == r)
    		return null;
    	return r.getEtag();
	}
    
	public String getFieldHelp(String string) {
		return "";
	}

	public boolean canBuild() {
		return false;
	}

	public String validate() {
		return null;
	}


	/**
	 * Answers whether this item contains an ontology or rules. That will be an Ontology, or a RuleSet.
	 * @return
	 */
	public boolean isAnOntology() {
		// TODO Auto-generated method stub
		return false;
	}


	public String getDefaultNamespace() {
		return getDatabase().getHost(true, true);
	}



}

