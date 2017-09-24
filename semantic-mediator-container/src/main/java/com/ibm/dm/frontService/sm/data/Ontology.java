
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

import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.b.utils.U;

import thewebsemantic.Namespace;

// Customizing for items in teh ontologies table.
@Namespace(Ontology.NAME_SPACE)
public class Ontology extends AModelRow
{

	@Override
	public boolean isAnOntology() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canBuild() {
		return isLegal();
	}

	@Override
	public boolean isLegal() {
		return super.isLegal() && false == Strings.isNullOrEmpty(getModelInstanceNamespace()) && canShow(); //null != getModelRepository(); //Strings.isNullOrEmpty(getFileName()); // && false == "".equals(getFileName().trim()) && new File(getFileName()).canRead();
	}
	
	static public interface IFields extends AModelRow.IFields
    {
        public static final String ONTOLOGY_DESCRIPTION     = "ontologyDescription";
    }
	
	private static transient final long serialVersionUID       = 1L;
	public static JSONArray ontologyViewconfig = new JSONArray();
	static {
		String config = 
			"[{'tag':'<i>%</i>','forText':'true', 'forView':'true','title':'Label','content':['" + RDFS.label + "']},\n" +
			" {'tag':'%','forText':'true', 'forView':'true','title':'Comment','content':['" + RDFS.comment + "']}]";
		try {
			ontologyViewconfig = new JSONArray(config);
		} catch (Exception e) {
			e.printStackTrace();		
		}
	}
	
//	/**
//	 * Specializes the general method in the parent class to work on Ontologies only.
//	 * @param rows List<Ontology> to search for an item matching the namespace 
//	 * @param importNS String of the name space to search.
//	 * @return Ontology if found, or null
//	 */
//	public static Ontology findModelForNameSpace(List<Ontology> rows, String importNS) {
//		return (Ontology)AModelRow.findModelForNameSpace(rows, importNS);
//	}
//	
	@Override
	public JSONArray getDefaultViewConfig() {
		return ontologyViewconfig;
	}

	
	@Override
	public JSONArray getShowViewConfig() {
		return getDefaultViewConfig();
	}


	private static String[]   ontologyEditableFields = new String[] { 
		IFields.ARCHIVED, 
		IFields.VERSION, 
		IFields.NAME, 
		IFields.TAGS,
		IFields.PREFIX,
		IFields.VIEW_CONFIG,
		IFields.MODEL_INSTANCE_NAMESPACE, 
	};

    @Override
    public String[] getEditableFieldNames()
    {
        return ontologyEditableFields;
    }

//    @Override
//    public JSONObject toJasonObject(Database database)
//    {
//        JSONObject result = super.toJasonObject(database);
//        return result;
//    }

    public Ontology(boolean... isTemp)
    {
        super();
        setTemporary(isTemp.length > 0 ? isTemp[0] : false); 
    }

    public Ontology()
    {
        super();
    }

    @Override
    public void integrityValidation(ADatabaseRow changed, Collection<? extends ADatabaseRow> all) throws IntegrityValidationException
    {}

    public static Ontology generateRandom()
    {
        Ontology o = new Ontology();
        o = o.generateRandomBase(o);
        o.archived = true;
        //o.collectionName = "COLLECTION_NAME_Ontology";
        o.dateCreated = U.roundDateToSecond(null);
        o.lastModified = U.roundDateToSecond(null);
        o.dirty = false;
        o.lastModified = U.roundDateToSecond(null);
        o.name = "NAME_VALUE_Ontology";
        o.modelInstanceNamespace = "ModelInstanceNamespace VALUE_Ontology";
        o.status = "STATUS VALUE_Ontology";
        o.version = "VERSION VALUE_Ontology";
        return o;
    }


//    @Override
    public String _toString()
    {
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
        builder.append("\n\tviewConfig: ");
        builder.append(viewConfig);
        builder.append("\n}");
        return builder.toString();
    }

	@Override
	public String getFieldHelp(String field) {
		if (field.equalsIgnoreCase(IFields.VIEW_CONFIG.toString()))
			return this.viewConfigHelp;
//		else 
		//			if (field.equalsIgnoreCase(IFields.SHOWLIST_CONFIG.toString()))
		//				return this.showListConfigHelp;


		return super.getFieldHelp(field);
	}


	@Override
	public String getField(String field) {
		if (IFields.PREFIX.equals(field))
			return prefix;
		return super.getField(field);
	}

	@Override
	public void setField(String field, String value) {
		if (IFields.PREFIX.equals(field)) {
			prefix = value;
		} else
			super.setField(field, value);
	}

	public String getDefaultNamespace() {
		return getDatabase().getHost(true, true);
	}

}

