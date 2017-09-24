
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.service.ARdfRepository;
import com.ibm.dm.frontService.sm.service.ModelRepository;
import com.ibm.dm.frontService.sm.service.RulesRepository;
import com.ibm.dm.frontService.sm.utils.Utils;

import thewebsemantic.Namespace;

// Customizing for items in the rules table.
@Namespace(RuleSet.NAME_SPACE)
public class RuleSet extends AModelRow
{
	// Specific fields
	protected String apiAccessName = null;
	protected boolean hasApi = false;
	
	@Override
	public ARdfRepository initRdfRepository() {
		ARdfRepository r = RulesRepository.create(this);
		return r;
	}

	static public interface IFields extends AModelRow.IFields
    {
		String HAS_API = "hasAPI";
		String API_ACCESS_NAME = "apiAccessName";
		String END1 = "EndPointA";
		String END2 = "EndPointB";
		String REVERSIBLE = "Reversible";
        String INTERCEPTOR_NAME  = "interceptorName";
		String INTERCEPTOR_CLASS = "interceptorClass";
		String INTERCEPTOR_DESCRIPTION = "interceptorDescription";
		String INTERCEPTOR_IS_LEGAL = "interceptorIsLegal";
	}

    private static final long serialVersionUID      = 1L;
    private static String[]   ruleSetEditableFields = new String[] { 
    	IFields.ARCHIVED, 
    	IFields.VERSION, 
    	IFields.NAME, 
    	IFields.TAGS,
    	IFields.REVERSIBLE,
		IFields.PREFIX,
    	IFields.VIEW_CONFIG,
    	IFields.MODEL_INSTANCE_NAMESPACE, 
    	IFields.END1,
    	IFields.END2,
    	IFields.INTERCEPTOR_NAME,
    	IFields.API_ACCESS_NAME,
    	IFields.HAS_API,
    	};
    
    public static String SM_RULES_NS = "http://ww.sprint-iot.eu/owl/ontologies/sm.owl#";
	public static JSONArray ruleSetViewconfig = new JSONArray();
	static {
		String config = 
			"[{'tag':'<i>%</i>','forText':'true', 'forView':'true','title':'Label','content':['" + RDFS.label + "']},\n" +
			" {'tag':'%','forText':'true', 'forView':'true','title':'Comment','content':['" + RDFS.comment + "']},\n" +
			" {'tag':'%','forText':'false', 'forView':'true','title':'subClass','content':['" + RDFS.subClassOf + "']},\n" +
			" {'tag':'%','forText':'false', 'forView':'true','title':'subProperty','content':['" + RDFS.subPropertyOf + "']},\n" +
			" {'tag':'%','forText':'false', 'forView':'true','title':'equiv.Class','content':['" + OWL.equivalentClass + "']},\n" +
			" {'tag':'%','forText':'false', 'forView':'true','title':'equiv.Prop','content':['" + OWL.equivalentProperty + "']},\n" +
			" {'tag':'%','forText':'false', 'forView':'true','title':'sm:domain','content':['" + SM_RULES_NS + "domainProperty']},\n" +
			" {'tag':'%','forText':'false', 'forView':'true','title':'sm:class','content':['" + SM_RULES_NS + "propertyClass']},\n" +
		    " {'tag':'%','forText':'false', 'forView':'true','title':'sm:range','content':['" + SM_RULES_NS + "rangeProperty']}]";
		try {
			ruleSetViewconfig = new JSONArray(config);
		} catch (Exception e) {
			e.printStackTrace();		
		}
	}
	
	@Override
	public boolean isAnOntology() {
		// TODO Auto-generated method stub
		return true;
	}

    @Override
	String update(String field, String v, JSONObject jUpdate, Map<String, String> params) throws JSONException {
        if (IFields.END1.equalsIgnoreCase(field) || IFields.END2.equalsIgnoreCase(field)) {
        	Ontology o = null;
        	if (false == "ANY".equals(v) && false == Strings.isNullOrEmpty(v)) {
        		v = v.split(":")[0];
        		o = getDatabase().getOntology(v);
        	}
        	setEndPoint(field.equals(IFields.END1)?END_POINT.FIRST : END_POINT.SECOND, o);
        	return null;
        } else if (field.equals(IFields.REVERSIBLE)) {
        	setEndPointReversible(Boolean.parseBoolean(v));
        	return null;
        } 
        if (IFields.INTERCEPTOR_NAME.equalsIgnoreCase(field)) {
           	setInterceptorName(v);
            if (!isValidInterceptor())
           		return "Error: Inrterceptor is not valid [" + validInterceptorStatus() + "]";
        } else
        	return super.update(field, v, jUpdate, params);
        return null;
	}

	public static enum END_POINT {FIRST, SECOND};
    private String[] endpoints = new String[] {null, null};
    private String interceptorName = null;
    private boolean reversible = false;

	public ISmModuleIntercept getInterceptor() {
		if (isLegal()) {
//			IConfigurationElement ce = (IConfigurationElement) getDatabase().getInterceptors().get(getInterceptorName() + ".configurationElement");
//			try {
//				ISmModuleIntercept mi = (ISmModuleIntercept) ce.createExecutableExtension("class");
//				return mi;
//			} catch (CoreException e) {
//				e.printStackTrace();
//			}
			return (ISmModuleIntercept)getDatabase().getInterceptors().get(getInterceptorName() + ".configuration");
		}
		return null;
	}


    public String getInterceptorClass() {
    	return Utils.stringify(getDatabase().getInterceptors().get(getInterceptorName() + ".class"));
    }
    
//	/**
//	 * Specializes the general method in the parent class to work on RuleSets only.
//	 * @param rows List<Ontology> to search for an item matching the namespace 
//	 * @param importNS String of the name space to search.
//	 * @return RuleSet if found, or null
//	 */
//	public static RuleSet findModelForNameSpace(List<RuleSet> rows, String importNS) {
//		return (RuleSet)AModelRow.findModelForNameSpace(rows, importNS);
//	}

    @Override
	public String getField(String field) {
		if (IFields.INTERCEPTOR_NAME.equalsIgnoreCase(field))
			return getInterceptorName();
		else if (IFields.INTERCEPTOR_CLASS.equalsIgnoreCase(field))
			return getInterceptorClass();
		else if (IFields.INTERCEPTOR_DESCRIPTION.equalsIgnoreCase(field))
			return getInteceptorDescription();
		else if (IFields.INTERCEPTOR_IS_LEGAL.equalsIgnoreCase(field))
			return validInterceptorStatus();
		else if (IFields.END1.equalsIgnoreCase(field))
			return getEndPointOntologyDisplayId(END_POINT.FIRST);
		else if (IFields.END2.equalsIgnoreCase(field))
			return getEndPointOntologyDisplayId(END_POINT.SECOND);
		else if (IFields.REVERSIBLE.equalsIgnoreCase(field))
			return isEndPointReversible()?"Yes":"No";
		else if (IFields.API_ACCESS_NAME.equalsIgnoreCase(field))
			return (Strings.isNullOrEmpty(apiAccessName)? getId() : apiAccessName).toLowerCase();
		else if (IFields.HAS_API.equalsIgnoreCase(field))
			return Boolean.toString(hasApi);
//		else if (IFields.LAST_MODIFIED.equalsIgnoreCase(field))
//			return "";
		return super.getField(field);
	}

    
	@Override
	public void setField(String field, String value) {
		if (IFields.API_ACCESS_NAME.equalsIgnoreCase(field)) {
			if (value.trim().equalsIgnoreCase(getId()))
					value = "";
			apiAccessName = value;
		}
		else if (IFields.HAS_API.equalsIgnoreCase(field))
			hasApi = Boolean.parseBoolean(value);
		else 
			super.setField(field, value);
	}

	private String getEndPointOntologyDisplayId(END_POINT endPoint) {
		Ontology o = getEndPointOntology(endPoint);
		if (null == o)
			return "ANY";
		return o.getDisplayId();
	}

	public String getInterceptorName() {
        return interceptorName;
    }

    private String getInteceptorDescription() {
    	String name = getInterceptorName();
    	return Utils.stringify(getDatabase().getInterceptors().get(name + ".description"));
    }

    public void setInterceptorName(String newInterceptorName) {
    	if (Strings.isNullOrEmpty(newInterceptorName) && Strings.isNullOrEmpty(this.interceptorName))
    		return;
    	boolean newNull = Strings.isNullOrEmpty(newInterceptorName),
    			curNull = Strings.isNullOrEmpty(this.interceptorName);
    	if (newNull && !curNull || !newNull && curNull || (
    			!curNull && !newNull && false == newInterceptorName.equals(this.interceptorName))) {
    		this.interceptorName = newInterceptorName;
    		markModified();
    	}
    }

    // It is valid when the status is empty.
    public boolean isValidInterceptor() {
    	String valid = validInterceptorStatus();
    	return Strings.isNullOrEmpty(valid);
    }
    
    // Status is empty when it is legal, otherwise, this is the problem text of the interceptor.
    public String validInterceptorStatus() {
    	String valid =  Utils.stringify(getDatabase().getInterceptors().get(getInterceptorName() + ".legal"));
//    	if (Strings.isNullOrEmpty(valid))
//    		valid = "No status";
    	return valid;
    }

    /**
     * Sets the reversible feature of end points for this rule set
     * @param reversible boolean. True means rules can be applied in both directions over its end points.
     */
    public void setEndPointReversible(boolean reversible) {
    	if (this.reversible != reversible) {
    		this.reversible = reversible;
    		markModified();
    	}
    }
    
    /**
     * Answers with whether the endpoints of this rule set are reversible
     * @return boolean reflecting this status
     */
    public boolean isEndPointReversible() {
    	return reversible;
    }
    
	@Override
	public boolean canBuild() {
		return isLegal();
	}

    private final void fixEndpoints() {
    	if (null == endpoints || endpoints.length < 2) {
    		String[] neps = new String[] {null, null};
    		if (null != endpoints) {
    			if (endpoints.length > 0)
    				neps[0] = endpoints[0];
    			if (endpoints.length > 1)
    				neps[1] = endpoints[1];
    		}
    		endpoints = neps;
    	}
    }
    /**
     * sets an endpoint ontology for the rules set
     * @param endPoint END_POINT - first or second
     * @param endType {@link END_TYPES} any or specific
     * @param endOntology Ontology of that endpoint, or null for "any".
     */
    public void setEndPoint(END_POINT endPoint, Ontology endOntology) {
    	fixEndpoints();
    	boolean changed = false;
    		String ns = getModelInstanceNamespace();
    		String cepons = endpoints[endPoint.ordinal()]; //(endPoint == END_POINT.FIRST) ? endpoint1 : endpoint2;
    		if ((null == cepons && null != endOntology) || (null != cepons && null == endOntology) || (
    				null != cepons && null != endOntology && false == endOntology.getModelInstanceNamespace().equals(cepons))) {
    			changed = true;
  				if (null == endOntology)
   					endpoints[endPoint.ordinal()] = null;
       			else 
       				endpoints[endPoint.ordinal()] = endOntology.getModelInstanceNamespace();
    			ModelRepository r = (ModelRepository) getModelRepository();
    			if (null != r) {
    				boolean mChanged = false;
    				OntModel m = ModelFactory.createOntologyModel();
    				Model bm = r.getModel();
    				m.add(r.getModel());
    				Set<String> imports = m.listImportedOntologyURIs();
    				if (null != cepons) { //remove the current import
    					Resource cepor = bm.createResource(cepons);
    					bm.remove(bm.createResource(ns), OWL.imports, cepor);
    					changed = mChanged = true;
    				}
    				if (null != endOntology && false == imports.contains(endOntology.getModelInstanceNamespace())) { // add the new one.
    					Resource epor = bm.createResource(endOntology.getModelInstanceNamespace());
    					bm.add(m.createResource(ns), OWL.imports, epor);
    					bm.setNsPrefix(endOntology.getPrefix(), endOntology.getModelInstanceNamespace());
    					changed = mChanged = true;
    				}
    				if (mChanged) {
    					this.ontologyDescription = null; // so it is invalidated.
    					r.setDirty(); r.save();
    				}
    			}
    		}
    	if (changed)
    		markModified();
    }
    
    /**
     * Answers with the ontology ID for the end point as per the argument endPoint.
     * @param endPoint {@link END_POINT} to be examined.
     * @return {@link Ontology} as per that end point, if type is {@link END_TYPE}.SPECIFIC, or null.
     */
    public Ontology getEndPointOntology(END_POINT endPoint) {
    	fixEndpoints();
    	String epns = endpoints[endPoint.ordinal()];
    	if (null == epns)
    		return null;
    	return getDatabase().getOntologyByClassUri(epns);
    }
        
    @Override
	public JSONArray getDefaultViewConfig() {
		return RuleSet.ruleSetViewconfig;
	}

	@Override
	public JSONArray getShowViewConfig() {
		return getDefaultViewConfig();
	}

	@Override
	public boolean isLegal() {

    	if (false == Strings.isNullOrEmpty(getInterceptorClass()) &&
    			false == isValidInterceptor())
    		return false;
		return super.isLegal() && false == Strings.isNullOrEmpty(getModelInstanceNamespace()); //null != getModelRepository(); //Strings.isNullOrEmpty(getFileName()); // && false == "".equals(getFileName().trim()) && new File(getFileName()).canRead();
	}

	@Override
    public String[] getEditableFieldNames()
    {
        return ruleSetEditableFields;
    }
//
//    public RuleSet(JSONObject jsonObject)
//    {
//        super(jsonObject);
//    }

    public RuleSet() {
    	super();
    }
    public RuleSet(boolean... isTemp) {
		super(isTemp);
	}

	@Override
    public void integrityValidation(ADatabaseRow changed, Collection<? extends ADatabaseRow> all) throws IntegrityValidationException
    {}

    public static RuleSet generateRandom()
    {
        RuleSet o = new RuleSet();
        o = o.generateRandomBase(o);
        o.archived = true;
        //o.collectionName = "COLLECTION_NAME_RuleSet";
        o.dateCreated = U.roundDateToSecond(null);
        o.lastModified = U.roundDateToSecond(null);
        o.dirty = false;
        o.lastModified = U.roundDateToSecond(null);
        o.name = "NAME_VALUE_RuleSet";
        o.modelInstanceNamespace = "ModelInstanceNamespace VALUE_RuleSet";
        o.status = "STATUS VALUE_RuleSet";
        o.version = "VERSION VALUE_RuleSet";
        return o;
    }

//    @Override
    public String _toString()
    {
    	fixEndpoints();

        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" {\n\tname: ");
        builder.append(name);
        builder.append("\n\tmodelInstanceNamespace: ");
        builder.append(modelInstanceNamespace);
        builder.append("\n\tfileName: ");
        builder.append(fileName);
        builder.append("\n\tontologyDescription: ");
        builder.append(ontologyDescription);
        builder.append("\n\tendPoint1: ");
        builder.append(endpoints[0]);
        builder.append("\n\tendPoint2: ");
        builder.append(endpoints[1]);
        builder.append("\n\treversible: ");
        builder.append(reversible);
        builder.append("\n\tinterceptorName: ");
        builder.append(interceptorName);
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

	public boolean hasApi() {
		return hasApi;
	}

}

