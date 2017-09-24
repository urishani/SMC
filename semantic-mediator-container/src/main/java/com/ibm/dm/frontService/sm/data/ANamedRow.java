
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




/**
 * Class to define named rows - such with a "name" field.
 * 
 * @author shani
 */
public abstract class ANamedRow extends ADatabaseRow
{
    private static final long serialVersionUID = 1L;

    static public interface IFields extends ADatabaseRow.IFields
    {
        public static final String NAME = "name";
    }

    protected String name = "";

//    @Override
//    public JSONObject toJasonObject(Database database)
//    {
//        JSONObject result = super.toJasonObject(database);
//        result.put(IFields.NAME, name);
//        return result;
//    }

    public ANamedRow(boolean... isTemp)
    {
        super(isTemp);
//        mIsTemp = Utils.isOptional(false, isTemp);
    }

//    public ANamedRow(JSONObject jsonObject)
//    {
//        super(jsonObject);
//        name = Utils.stringify(jsonObject.get(IFields.NAME));
//    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isLegal() {
    	return (super.isLegal()); // && false == Strings.isNullOrEmpty(getName()));  The name is not so important here.
    }
    
    public void setName(String name)
    {
        if (false == name.equals(this.name)) {
        	this.name = name;
        	markModified();
        }
    }

    public void setField(String field, String value)
    {
        super.setField(field, value);
        if (field.equalsIgnoreCase(IFields.NAME)) 
        	this.name = value;
    }

    public String getField(String field)
    {
        if (field.equalsIgnoreCase(IFields.NAME))
            return this.name;
        else
            return super.getField(field);
    }

}

