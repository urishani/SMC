
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

import org.json.JSONArray;

import thewebsemantic.Namespace;

import com.ibm.dm.frontService.b.utils.U;

// Customizing for items in the rules table.
@Namespace(Shape.NAME_SPACE)
public class Shape extends AModelRow
{
    static public interface IFields extends AModelRow.IFields
    { 
        public static final String ONTOLOGY_ID = "ontologyId";
    }

    private static final long serialVersionUID      = 1L;
    private static String[]   shapeEditableFields = new String[] {
    		IFields.VERSION, IFields.NAME, IFields.ONTOLOGY_ID, IFields.ARCHIVED, IFields.TAGS };


    @Override
	public JSONArray getDefaultViewConfig() {
		return Ontology.ontologyViewconfig;
	}

	@Override
    public String[] getEditableFieldNames()
    {
        return shapeEditableFields;
    }
//
//    public RuleSet(JSONObject jsonObject)
//    {
//        super(jsonObject);
//    }

    public Shape() {
    	super();
    }
    public Shape(boolean... isTemp) {
		super(isTemp);
	}

	@Override
    public void integrityValidation(ADatabaseRow changed, Collection<? extends ADatabaseRow> all) throws IntegrityValidationException
    {}

    public static Shape generateRandom()
    {
        Shape o = new Shape();
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
        builder.append("\n}");
        return builder.toString();
    }

}

