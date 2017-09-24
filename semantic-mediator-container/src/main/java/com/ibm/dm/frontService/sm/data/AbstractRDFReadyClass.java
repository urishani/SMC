
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

/**
 *
 */
package com.ibm.dm.frontService.sm.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import thewebsemantic.Id;
import thewebsemantic.Namespace;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.b.data.interfaces.IRDFReady;
import com.ibm.dm.frontService.b.utils.U;

/**
 * @author borisd Contract:
 */
@Namespace(AbstractRDFReadyClass.NAME_SPACE)
public abstract class AbstractRDFReadyClass implements IRDFReady
{
    static public interface IFields
    {
        public static final String DISPLAY_ID    = "displayId";
        public static final String MODEL_DATA_ID = "modelDataId";
        public static final String ID            = "id";
        public static final String ID_URI        = "idURI";
        public static final String BASE_URL      = "baseURL";
        public static final String LAST_MODIFIED = "dateModified";
        public static final String ETAG          = "etag";
    }

    protected transient AbstractRDFReadyClass owner = null;
    protected void setOwner(AbstractRDFReadyClass owner) { this.owner = owner;}

    private static final long            serialVersionUID = 1L;
    public static final transient String NAME_SPACE       = "https://ibm.com/rational/SM/";
    protected String                     id               = null;
    /**
     * consist of the [baseURL]/[Class.getSimpleName()]/[id]
     */
    protected URI                        idURI            = null;
    /**
     * Must be a URL safe! all chars that are not allowed in URL path part must be escaped
     * Does not include the name of the Class
     */
    protected static String                     baseURL          = null;
    /**
     * this is needed for the JavaScript - on client JS keeps in it the etag of the instance and making it transient ensures that it will not go in to RDF and not to JSON but its
     * presence will not crash automatic serialization from JSON
     */
    protected transient String           etag             = null;

    protected String                     displayId        = null;   //id's like "Ont-xx" that are used, by the json db as id, and the jazz app for display
    protected String                     modelDataId      = null;   //id of the model data (i.e. the blobobject in jazz)

    protected long         			     lastModified  	  = U.roundDateToSecond(null);

    @Id
    public URI getIdURI()
    {
        if (idURI == null) rebuildIdURI();
        return idURI;
    }

    protected void rebuildIdURI()
    {
        String baseURL = getBaseURL();
        if (!baseURL.endsWith("/")) baseURL += "/";
        String newURI = baseURL + getClass().getSimpleName() + "/" +  id;
        try
        {
            idURI = new URI(newURI);
            validateIdURI();
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Contract: 1. should not end with "/"
     * <p/>
     * segments separated by "/" 2. last segment is ID 3. segment before last is name of the class -> getClass().getSimpleName() 4. should not contain spaces in the beginning and
     * end of the URI
     */
    protected void validateIdURI()
    {
        if (idURI == null) throw new RuntimeException("idURI should not be null");
        if (!idURI.isAbsolute()) throw new RuntimeException("Base URI must be absolute URI");
        String uri = idURI.toASCIIString().trim();

        if (uri.length() != uri.trim().length()) throw new RuntimeException("white space not allowed in the beginning or end of URI");
        if (uri.endsWith("/")) throw new RuntimeException("URI should not end with '/'");
        String path = idURI.getPath();
        String[] parts = path.split("/");
        if (parts.length < 2) throw new RuntimeException("URI path portion should have at least 2 segments class name and ID");
        if (!getClass().getSimpleName().equals(parts[parts.length - 2])) throw new RuntimeException("URI path portion should have class name as one before last segment");
    }

//    public void setIdURI(URI idURI)
//    {
//        String uri = idURI.toASCIIString();
//        int i = uri.lastIndexOf("/");
//        this.id = uri.substring(i + 1);
//        i = uri.substring(0, i).lastIndexOf("/");
//        baseURL = uri.substring(0, i + 1);
//        this.idURI = idURI;
//        validateIdURI();
//    }

    /**
     * returns id it may be no URL safe use EncodingUtil.encodeURIComponent() to make it safe to use as part of URL path
     */
    public String getId()
    {
        return id;
    }

    // public String getUrlSafeId()
    // {
    // if (Strings.isNullOrEmpty(id)) return id;
    // return EncodingUtil.encodeURIComponent(id);
    // }

    // public void setUrlSafeId(String id)
    // {
    // if (Strings.isNullOrEmpty(id))
    // this.id = id;
    // else
    // this.id = EncodingUtil.decodeURIComponent(id);
    // rebuildIdURI();
    // }

    /**
     * returns id as is may be url unsafe
     */
    public void setId(String id)
    {
        this.id = id;
//        rebuildIdURI();
    }

    public void setDisplayId(String displayId)
    {
        this.displayId = displayId;
    }

    public void setModelDataId(String modelDataId)
    {
        this.modelDataId = modelDataId;
    }

    public String getBaseURL()
    {
        if (baseURL == null) baseURL = getNameSpace() /*+ EncodingUtil.encodeURIComponent(getClass().getSimpleName()) + "/"*/;
        return baseURL;
    }

    /**
     * Must be a URL safe! all chars that are not allowed in URL path part must be escaped
     */
    public void setBaseURL(String baseURL)
    {
        AbstractRDFReadyClass.baseURL = baseURL;
        rebuildIdURI();
    }

    // public String getResourceURI()
    // {
    // return NAME_SPACE + "/" + EncodingUtil.encodeURIComponent(this.getClass().getSimpleName()) + "/" + EncodingUtil.encodeURIComponent(getId());
    // }

    public String getNameSpace()
    {
        return NAME_SPACE;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseURL == null) ? 0 : baseURL.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((idURI == null) ? 0 : idURI.hashCode());
        Long lm = lastModified;
        result = prime * result + ((lm == null) ? 0 : lm.hashCode());
        return result;
    }

    //    @Override
    //    public boolean equals(Object obj)
    //    {
    //        return EqualsBuilder.reflectionEquals(this, obj, true, null);
    //    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" {\n\tid: ");
        builder.append(id);
        builder.append("\n\tidURI: ");
        builder.append(idURI);
        builder.append("\n\tbaseURL: ");
        builder.append(baseURL);
        builder.append("\n\tlastModified: ");
        builder.append(lastModified);
        builder.append("\n}");
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AbstractRDFReadyClass other = (AbstractRDFReadyClass) obj;
//        if (baseURL == null)
//        {
//            if (other.baseURL != null) return false;
//        }
//        else
//            if (!baseURL.equals(other.baseURL)) return false;
        if (id == null)
        {
            if (other.id != null) return false;
        }
        else
            if (!id.equals(other.id)) return false;
        if (idURI == null)
        {
            if (other.idURI != null) return false;
        }
        else
            if (!idURI.equals(other.idURI)) return false;
        Long lm = lastModified, olm = other.lastModified;
//        if (lm == null)
//        {
//            if (olm != null) return false;
//        }
//        else
            if (!lm.equals(olm)) return false;
        return true;
    }

    protected <T extends AbstractRDFReadyClass> T generateRandomBase(T x)
    {
        id = UUID.randomUUID().toString();
        if (Strings.isNullOrEmpty(baseURL))
        {
            baseURL = getNameSpace() /*+ EncodingUtil.encodeURIComponent(x.getClass().getSimpleName()) + "/"*/;
        }
        //        try
        //        {
        //            String str = baseURL + EncodingUtil.encodeURIComponent(x.getClass().getSimpleName()) + "/" + EncodingUtil.encodeURIComponent(id);
        //            idURI = URI.create(str);
        //        }
        //        catch (Exception e)
        //        {
        //            e.printStackTrace();
        //        }
        rebuildIdURI();
        return x;
    }

    public Date getLastModified()
    {
        return new Date(lastModified);
    }

    public boolean setLastModified(Date lastModified)
    {
    	if (this.lastModified < lastModified.getTime()) {
    		this.lastModified = lastModified.getTime();
    		System.err.println("Modified and made dirty [" + getId() + "]");
    		return true;
    	}
    	return false;
    }
}

