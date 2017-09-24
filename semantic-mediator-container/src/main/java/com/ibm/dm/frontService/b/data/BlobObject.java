
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

package com.ibm.dm.frontService.b.data;

import java.util.Date;

import org.apache.commons.codec.binary.Base64;

import thewebsemantic.Namespace;

import com.ibm.dm.frontService.b.utils.RandomStrings;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.data.AbstractRDFReadyClass;

@SuppressWarnings("serial")
@Namespace(BlobObject.NAME_SPACE)
public class BlobObject extends AbstractRDFReadyClass
{
    /**
     * Pay Attention! This is data in BASE64 Encoding!
     */
    protected String data        = "";
    protected Date   dateCreated = new Date(U.roundDateToSecond(null));
    protected String name        = null;
    protected String mimeType    = "application/octet-stream";
//	Base64 b64 = new Base64();

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public Date getDateCreated()
    {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated)
    {
        this.dateCreated = dateCreated;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return "BlobObject [data=" + data + ", dateCreated=" + dateCreated + ", name=" + name + ", mimeType=" + mimeType + ", id=" + id + ", idURI=" + idURI + ", baseURL=" + baseURL + ", etag="
                + etag + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((dateCreated == null) ? 0 : dateCreated.hashCode());
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        BlobObject other = (BlobObject) obj;
        if (data == null)
        {
            if (other.data != null) return false;
        }
        else
            if (!data.equals(other.data)) return false;
        if (dateCreated == null)
        {
            if (other.dateCreated != null) return false;
        }
        else
            if (!dateCreated.equals(other.dateCreated)) return false;
        if (mimeType == null)
        {
            if (other.mimeType != null) return false;
        }
        else
            if (!mimeType.equals(other.mimeType)) return false;
        if (name == null)
        {
            if (other.name != null) return false;
        }
        else
            if (!name.equals(other.name)) return false;
        return true;
    }

    public static BlobObject generateRandom()
    {
        BlobObject x = new BlobObject();
        x = x.generateRandomBase(x);
        byte[] t = RandomStrings.alphanumeric(50).getBytes();
        x.data = new String(Base64.encodeBase64(t));
        x.name = RandomStrings.alphabetic(6);
        x.dateCreated = new Date(U.roundDateToSecond(null));
        return x;
    }

    public static void main(String[] s)
    {
        final String string = "1234567890qwertyui9op[]asdfghjkl;zxcvbnm";
        System.out.println("Input  String " + string);
        String r = new String(Base64.encodeBase64(string.getBytes()));
        System.out.println("Output String " + r);
        r = new String(Base64.decodeBase64(r.getBytes()));
        System.out.println("Output String " + r);
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

}

