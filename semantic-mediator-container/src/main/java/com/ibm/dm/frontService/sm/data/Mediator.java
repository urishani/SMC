
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import thewebsemantic.Namespace;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.data.Port.PortType;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.utils.Utils;

// ============================= Mediators ==============================
// Customizing for rows in the mediators table.
@Namespace(Mediator.NAME_SPACE)
public class Mediator extends ADatabaseRow
{
    @Override
	public boolean canTest(Database owner) {
		if (false == isReady())
			return super.canTest(owner);
		Port inPort = null, outPort = null;
		inPort = (Port) owner.getItem(getInputPortId());
		outPort = (Port) owner.getItem(getOutputPortId());
		if (inPort == null || outPort == null)
			return false;
		return inPort.isRestful() || outPort.isRestful();
	}

	static public interface IFields extends ADatabaseRow.IFields
    {
        public static final String NAME              = "name";
        public static final String INPUT_PORT_ID     = "inputPortId";
        public static final String OUTPUT_PORT_ID    = "outputPortId";
        public static final String RULE_SET_ID       = "ruleSetId";
	    public static final String SERVICE            = "service";
	    public static final String TOOL               = "tool";
	    public static final String API_NAME           = "apiName";
	    public static final String INPUT_ONTOLOGY_ID  = "inputOntologyId";
	    public static final String OUTPUT_ONTOLOGY_ID = "outputOntologyId";
	}

	public enum SERVICE 
	{
	    EXPORT("Export"), IMPORT("Import"), BOTH("Both"), NaN("NaN"), NO_SERVICE("No-Service");
	    private String name;
	    private SERVICE(String name) {
	    	this.name = name;
	    }
	    public String toString() {
	    	return name;
	    }
	    public static SERVICE get(String name) {
	    	for (SERVICE s: values())
	    		if (name.equals(s.toString()))
	    			return s;
	    	return NaN;
	    }
	    public SERVICE inverse() {
    		switch (this) {
			case EXPORT: return IMPORT;
			case IMPORT: return EXPORT; 
			case BOTH: return BOTH;
			case NO_SERVICE: return NO_SERVICE; 
			default: return NaN;
		}

	    }
	}

	private static final long serialVersionUID       = 1L;

	public static final String MEDIATION_TRACE_KEY = "##MediationTraceKey##";

    private static String[]   mediatorEditableFields = new String[] { IFields.VERSION,
    	IFields.NAME,
    	IFields.INPUT_PORT_ID,
    	IFields.OUTPUT_PORT_ID,
    	IFields.RULE_SET_ID,
    	IFields.ARCHIVED,
    	IFields.TAGS };

    protected String          name                   = "";
    protected String          inputPortId            = "";
    protected String          outputPortId           = "";
    protected String          ruleSetId              = "";
//    protected String          interceptorClass       = "";
//    protected String		  interceptorName		 = "";
//	protected String 		  interceptorDescription = "";

    @Override
    public String[] getEditableFieldNames()
    {
        return mediatorEditableFields;
    }

//    @Override
//    public JSONObject toJasonObject(Database database)
//    {
//        JSONObject result = super.toJasonObject(database);
//        result.put(IFields.NAME, name);
//        result.put(IFields.INPUT_PORT_ID, inputPortId);
//        result.put(IFields.OUTPUT_PORT_ID, outputPortId);
//        result.put(IFields.RULE_SET_ID, ruleSetId);
//        result.put(IFields.INTERCEPTOR_CLASS, interceptorClass);
//        result.put(IFields.INTERCEPTOR_NAME, interceptorName);
//        result.put("canTest", canTest(database));
//        return result;
//    }

//    public Mediator(JSONObject jsonObject)
//    {
//        super(jsonObject);
//        name = Utils.stringify(jsonObject.get(IFields.NAME));
//        inputPortId = Utils.stringify(jsonObject.get(IFields.INPUT_PORT_ID));
//        outputPortId = Utils.stringify(jsonObject.get(IFields.OUTPUT_PORT_ID));
//        ruleSetId = Utils.stringify(jsonObject.get(IFields.RULE_SET_ID));
//        interceptorClass = Utils.stringify(jsonObject.get(IFields.INTERCEPTOR_CLASS));
//        interceptorName = Utils.stringify(jsonObject.get(IFields.INTERCEPTOR_NAME));
//    }

    public Mediator() {
    	super();
    }
    public Mediator(boolean... isTemp)
    {
        super(isTemp);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
        markModified();
    }

    public String getInputPortId()
    {
        return inputPortId;
    }

    public void setInputPortId(String inputPortId)
    {
        this.inputPortId = inputPortId;
        markModified();
    }

    public String getOutputPortId()
    {
        return outputPortId;
    }

    public void setOutputPortId(String outputPortId)
    {
        this.outputPortId = outputPortId;
        markModified();
    }

    public String getRuleSetId()
    {
        return ruleSetId;
    }

    public void setRuleSetId(String ruleSetId)
    {
        this.ruleSetId = ruleSetId;
        markModified();
    }

    public void setField(String field, String value)
    {
        super.setField(field, value);
        if (field.equalsIgnoreCase(IFields.NAME))
            this.name = value;
        else
            if (field.equalsIgnoreCase(IFields.INPUT_PORT_ID))
                this.inputPortId = value;
            else
                if (field.equalsIgnoreCase(IFields.OUTPUT_PORT_ID))
                    this.outputPortId = value;
                else
                    if (field.equalsIgnoreCase(IFields.RULE_SET_ID))
                        this.ruleSetId = value;
    }

    public String getField(String field)
    {
        if (field.equalsIgnoreCase(IFields.NAME))
            return this.name;
        else
            if (field.equalsIgnoreCase(IFields.INPUT_PORT_ID))
                return this.inputPortId;
            else
                if (field.equalsIgnoreCase(IFields.OUTPUT_PORT_ID))
                    return this.outputPortId;
                else
                    if (field.equalsIgnoreCase(IFields.RULE_SET_ID))
                        return this.ruleSetId;
                    else if (field.equalsIgnoreCase(RuleSet.IFields.INTERCEPTOR_NAME) ||
                    		field.equalsIgnoreCase(RuleSet.IFields.INTERCEPTOR_CLASS) ||
                    		field.equalsIgnoreCase(RuleSet.IFields.INTERCEPTOR_DESCRIPTION) ||
                    		field.equalsIgnoreCase(RuleSet.IFields.INTERCEPTOR_IS_LEGAL)
                    		) {
                    	RuleSet rs = getDatabase().getRuleSet(getRuleSetId());
                    	if (null == rs)
                    		return null;
                    	else
                    		return rs.getField(field);
                    }
                    else
                        return super.getField(field);
    }

	public String getReferenceName(Database db, String field) {
    	if (field.equalsIgnoreCase(IFields.INPUT_PORT_ID)) {
    		Port port = db.getPort(getInputPortId());
    		if (null != port && port.isReady()) return port.getName();// + "/" + port.getAccessName();
    		return (null == port)?"!!missing!!" : "!!noReady!!";
    	} else if (field.equalsIgnoreCase(IFields.OUTPUT_PORT_ID)) {
    		Port port = db.getPort(getOutputPortId());
    		if (null != port && port.isReady()) return port.getName();// + "/" + port.getAccessName();
    		return (null == port)?"!!missing!!" : "!!noReady!!";
    	} else if (field.equalsIgnoreCase(IFields.RULE_SET_ID)) {
    		RuleSet rules = db.getRuleSet(getRuleSetId());
    		if (null != rules && rules.isReady()) return rules.getName();
    		return (null == rules)?"!!missing!!" : "!!noReady!!";
    	} else
    		return super.getReferenceName(db, field);
    }


	public ISmModuleIntercept getInterceptor() {
    	RuleSet rs = getDatabase().getRuleSet(getRuleSetId());
    	if (null == rs)
    		return null;
    	return rs.getInterceptor();
	}

    public String getInterceptorClass() {
    	RuleSet rs = getDatabase().getRuleSet(getRuleSetId());
    	if (null == rs)
    		return null;
    	return rs.getInterceptorClass();
    }
    
    public String getInterceptorName() {
    	RuleSet rs = getDatabase().getRuleSet(getRuleSetId());
    	if (null == rs)
    		return null;
    	return rs.getInterceptorName();
    }

    @Override
    /**
     * Answers false if input and output ports are defined and missing, if any of them missing
     * and if the interceptor is missing.
     */
	public boolean isLegal() {
    	String msg = validate();
    	if (false == Strings.isNullOrEmpty(msg))
    		return false;
    	if (null == getDatabase().getItem(getInputPortId()) ||
    			false == getDatabase().getItem(getInputPortId()).isReady() )
    		return false;

    	if (null == getDatabase().getItem(getOutputPortId()) ||
    			false == getDatabase().getItem(getOutputPortId()).isReady() )
    		return false;
    	
		 RuleSet rs = getDatabase().getRuleSet(getRuleSetId());
		 if (false == rs.isReady())
			 return false;

		 return true;
   }
    
    public String validate() {
    	
    	Database db = getDatabase();
    	Port p1 = db.getPort(getInputPortId()), p2 = db.getPort(getOutputPortId());
    	RuleSet rs = db.getRuleSet(getRuleSetId());
    	if (null == p1 || null == p2 )
    		return "Some required fields are not specified";
    	// Rules are mandatory, and when they are defined, they should be ready:
    	if (null == rs)
    		return "Missing rule set which is mandatory";
    	
    	// Now check the compatibility of ports and ontologies of the rules set.
    	Ontology o1 = db.getOntology(p1.getOntologyId()), 
    			 o2 = db.getOntology(p2.getOntologyId());
    	Ontology ro1 = rs.getEndPointOntology(RuleSet.END_POINT.FIRST),
    			 ro2 = rs.getEndPointOntology(RuleSet.END_POINT.SECOND);
    	boolean rev = rs.isEndPointReversible();
    	boolean ok = (null == ro1 || ro1 == o1);
    	boolean inRev = false;
    	if (!ok) {
    		ok = rev && (null == ro2 || ro2 == o1);
    		inRev = true;
    	}
    	if (!ok)
    		return "Input port has incompatible ontology to the one specified in the rule-set [" + rs.getDisplayId() + "]."; 
    	
    	ok = (!inRev && (null == ro2 || ro2 == o2) ) ||
    		 ( inRev && (null == ro1 || ro1 == o2) );
    	if (!ok)
    		return "Output port has incompatible ontology to the one specified in the rule-set [" + rs.getDisplayId() + "]."; 

    	if (p1.getType() == Port.PortType.get)
    		return "Input port cannot provide model data for mediation.";
    	if (p2.getType() == Port.PortType.post)
    		return "Output port cannot accept model data for mediation.";

    	return super.validate();
	}

//	@Override
//    String update(String field, String v, JSONObject jUpdate, Map<String, String> params)
//    {
//        return super.update(field, v, jUpdate, params);
//    }


//	public static String validateConfigurationElement(String configurationElement) {
//        try // Now try to validate the className as a correct class.
//        {        	
//            Class<?> c = Class.forName(className);
//            c.asSubclass(ISmModuleIntercept.class);
//        }
//        catch (ClassNotFoundException e)
//        {
//            return "Error: Class not found [" + e.getMessage() + "]";
//        }
//        catch (ClassCastException e)
//        {
//            return "Error: Class [" + className + "] is not subclass of [" + ISmModuleIntercept.class.getCanonicalName() + "]";
//        }
//        return null;
//	}

    /**
     * To be used for testing via REST API this method answers as for
     * the Config class by evaluating the ports and identifying whether
     * a POST (export) or GET (import) test can be performed.
     *
     * @return String describing service.
     */
    public SERVICE checkService(Database database)
    {

        SERVICE service = SERVICE.NO_SERVICE;
        if (false == isReady()) return service;
        Port input = database.getPort(getInputPortId()), output = database.getPort(getOutputPortId());
        if (input.getType() == PortType.post)
        {
            if (output.getType() == PortType.get)
            	// Can't have a mediator passes data right away from a POST port into a GET port
            	// This makes no sense
                return SERVICE.NO_SERVICE;
            else
                return SERVICE.EXPORT;
        }
        else
            if (output.getType() == PortType.get) return SERVICE.IMPORT;

        // The mediator is ready and it's connected to pipes and repositories
        // (since neither port is GET or POST)
        return SERVICE.BOTH;
    }

    @Override
    public void integrityValidation(ADatabaseRow changed, Collection<? extends ADatabaseRow> all) throws IntegrityValidationException
    {
        // TODO Auto-generated method stub

    }

    public static Mediator generateRandom()
    {
        Mediator o = new Mediator();
        o = o.generateRandomBase(o);
        o.archived = true;
        //o.collectionName = "COLLECTION_NAME_RuleSet";
        o.dateCreated = U.roundDateToSecond(null);
        o.lastModified = U.roundDateToSecond(null);
        o.dirty = false;
        o.lastModified = U.roundDateToSecond(null);
        o.name = "NAME_VALUE_RuleSet";
        o.status = "STATUS VALUE_RuleSet";
        o.version = "VERSION VALUE_RuleSet";
        return o;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((inputPortId == null) ? 0 : inputPortId.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((outputPortId == null) ? 0 : outputPortId.hashCode());
        result = prime * result + ((ruleSetId == null) ? 0 : ruleSetId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        Mediator other = (Mediator) obj;
        if (inputPortId == null)
        {
            if (other.inputPortId != null) return false;
        }
        else
            if (!inputPortId.equals(other.inputPortId)) return false;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else
            if (!name.equals(other.name)) return false;
        if (outputPortId == null)
        {
            if (other.outputPortId != null) return false;
        }
        else
            if (!outputPortId.equals(other.outputPortId)) return false;
        if (ruleSetId == null)
        {
            if (other.ruleSetId != null) return false;
        }
        else
            if (!ruleSetId.equals(other.ruleSetId)) return false;
        return true;
    }

//    @Override
    public String _toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" {\n\tname: ");
        builder.append(name);
        builder.append("\n\tinputPortId: ");
        builder.append(inputPortId);
        builder.append("\n\toutputPortId: ");
        builder.append(outputPortId);
        builder.append("\n\truleSetId: ");
        builder.append(ruleSetId);
        builder.append("\n\tinterceptorClass: ");
        builder.append("\n\tversion: ");
        builder.append(version);
        builder.append("\n\tstatus: ");
        builder.append(status);
        builder.append("\n\tarchived: ");
        builder.append(archived);
        builder.append("\n\tcollectionName: ");
        builder.append(collectionName);
        builder.append("\n\tdateCreated: ");
        builder.append(dateCreated);
        builder.append("\n\tdirty: ");
        builder.append(dirty);
        builder.append("\n\tid: ");
        builder.append(id);
        builder.append("\n\tidURI: ");
        builder.append(idURI);
        builder.append("\n\tbaseURL: ");
        builder.append(baseURL);
        builder.append("\n\tlastModified: ");
        builder.append(lastModified);
        builder.append("\n\tetag: ");
        builder.append(etag);
        builder.append("\n}");
        return builder.toString();
    }

	public JSONObject markReachablePorts(Database db, Set<String> reachablePorts) throws JSONException {
		Port outputPort = db.getPort(getOutputPortId());
		JSONObject flow = new JSONObject();
		if (reachablePorts.add(outputPort.getId())) {
			flow.put(getId(), outputPort.getId());
			JSONObject jo = outputPort.markReachablePorts(db, reachablePorts);
			Iterator<?> keys = jo.keys();
			while (keys.hasNext()) {
			    String key = (String)keys.next(); // JSONObject.getNames(jo)) {
				flow.put(key, Utils.safeGet(jo, key));
//				flow.putAll(outputPort.markReachablePorts(db, reachablePorts));
			}
		}
		return flow;
	}

	@Override
    public boolean canShow()
    {
		try {
			Database db = getDatabase(); 
			return (PortType.repository == db.getPort(getInputPortId()).getType())
					&& (PortType.repository == db.getPort(getOutputPortId()).getType());
		} catch (Throwable t) {
			return false;
		}
    }

	/**
	 * A utility method answering with an info string telling about what the mediator intends
	 * to do for a log entry.
	 * @param test boolean to say if this is a test on the mediator.
	 * @param doApply boolean to say if the test is applied to the target repository.
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public String getLogInfo(boolean test, boolean doApply) throws FileNotFoundException, IOException, ClassNotFoundException {
		Database db = getDatabase();
		String result = db.getPort(getInputPortId()).getDisplayId() + " -> " +
			getDisplayId() + " ->" + (doApply?" ":"<font color='red'>X</font> ") +
			db.getPort(getOutputPortId()).getDisplayId();
		if (test)
			result = "[test]" + result;
		return result;
	}
}
// ============================= End of  Mediators ==============================

