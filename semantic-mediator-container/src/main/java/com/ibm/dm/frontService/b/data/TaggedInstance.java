
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

package com.ibm.dm.frontService.b.data;

import java.io.Serializable;

/**
 * this class is does not serialize to RDF and does not go over the Wire to the browser!
 * it needed for convenience of returning results from DAO service
 */
public class TaggedInstance<T> implements Serializable
{
    private static final long serialVersionUID = 1L;
    protected T               object           = null;
    protected String          tag              = null;

    public TaggedInstance()
    {};

    public TaggedInstance(String tag, T object)
    {
        this.tag = tag;
        this.object = object;
    }

    public void setObject(T object)
    {
        this.object = object;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public T getObject()
    {
        return object;
    }

    public String getTag()
    {
        return tag;
    }

    @Override
    public String toString()
    {
        return "TaggedInstance [object=" + object + ", tag=" + tag + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((object == null) ? 0 : object.hashCode());
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        @SuppressWarnings("unchecked") TaggedInstance<T> other = (TaggedInstance<T>) obj;
        if (object == null)
        {
            if (other.object != null) { return false; }
        }
        else
            if (!object.equals(other.object)) { return false; }
        if (tag == null)
        {
            if (other.tag != null) { return false; }
        }
        else
            if (!tag.equals(other.tag)) { return false; }
        return true;
    }

}

