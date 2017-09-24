
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

package com.ibm.dm.frontService.b.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Model;
import org.xml.sax.InputSource;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class U
{
    public static final String CONTENT_TYPE_JSON  = "text/json";
    public static final String CONTENT_TYPE_PLAIN = "text/plain";

    static Gson                gson               = getGson();

    static public Gson getGson()
    {
        if (gson == null)
        {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }
        return gson;
    }

    public static void prepareForHeadLessRhapsody()
    {
        String addedValue = CurrentConfiguration.get().getProperty("rhapsody.dll.path");
        if (addedValue != null)
        {
            String currentValue = System.getProperty("java.library.path");
            if (Strings.isNullOrEmpty(currentValue))
            {
                System.setProperty("java.library.path", addedValue);
                return;
            }

            if (!currentValue.contains(addedValue))
            {
                String codeBase = Joiner.on(";").skipNulls().join(currentValue, addedValue);
                System.setProperty("java.library.path", codeBase);
            }
        }
    }

    public static long roundDateToSecond(Date d)
    {
        if (d == null) d = new Date();
        long l = ((d.getTime() + 500) / 1000) * 1000;
        return new Date(l).getTime();
    }

    public static <T> List<T> takeCollectionFromResponce(HttpResponse response, Class<T> clazz)
    {
        List<T> collection = null;
        // get the request entity
        HttpEntity e = response.getEntity();
        try
        {
            String objAsString = EntityUtils.toString(e);
            Type collectionType = new TypeToken<List<T>>() {}.getType();

            collection = gson.fromJson(objAsString, collectionType);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return collection;
    }

    public static <T> List<T> takeListFromResponce(HttpResponse response, Class<T> clazz)
    {
        List<T> l = new ArrayList<T>();
        try
        {
            String json = EntityUtils.toString(response.getEntity());
            System.out.println("Using Gson.toJson() on a raw collection: " + json);

            JsonParser parser = new JsonParser();
            JsonArray array = parser.parse(json).getAsJsonArray();
            Iterator<JsonElement> it = array.iterator();
            while (it.hasNext())
            {
                T t = gson.fromJson(it.next(), clazz);
                if (t != null)
                    l.add(t);
                else
                    System.out.println("Failed to extract object from JSON array");
            }

            // for (int i = 0; i < array.size(); i++)
            // {
            // T t = gson.fromJson(array.get(2), clazz);
            // if (t != null)
            // l.add(t);
            // else
            // System.out.println("Failed to extract object from JSON array");
            // }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return l;
    }

    public static <T> T takeObjFromResponce(HttpResponse response, Class<T> clazz)
    {
        T o = null;
        // get the response entity
        HttpEntity e = response.getEntity();
        try
        {
            String objAsString = EntityUtils.toString(e);
            o = gson.fromJson(objAsString, clazz);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return o;
    }

    public static <T> Collection<T> takeCollectionFromRequest(HttpRequest request, Class<T> clazz) throws ParseException, IOException
    {
        Collection<T> collection = null;
        // get the response entity
        try
        {
            HttpEntityEnclosingRequest requestWithEntity = ((HttpEntityEnclosingRequest) request);
            HttpEntity e = requestWithEntity.getEntity();
            String objAsString = EntityUtils.toString(e);
            Type collectionType = new TypeToken<Collection<T>>() {}.getType();
            collection = gson.fromJson(objAsString, collectionType);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return collection;

    }

    public static <T> T takeObjFromRequest(HttpRequest request, Class<T> clazz) throws ParseException, IOException
    {
        T o = null;
        // get the request entity
        HttpEntityEnclosingRequest requestWithEntity = ((HttpEntityEnclosingRequest) request);
        HttpEntity e = requestWithEntity.getEntity();
        String objAsString = EntityUtils.toString(e);
        o = gson.fromJson(objAsString, clazz);
        return o;
    }

    public static HttpResponse putObjToResponce(HttpResponse response, Object o) throws UnsupportedEncodingException
    {
        StringEntity entity = new StringEntity(gson.toJson(o));
        entity.setContentType(CONTENT_TYPE_JSON);
        response.setEntity(entity);
        return response;
    }

    public static URI addQueryParamToURI(URI uri, NameValuePair nameValuePair) throws URISyntaxException
    {
        List<NameValuePair> p = new ArrayList<NameValuePair>(URLEncodedUtils.parse(uri, HTTP.UTF_8));
        p.add(nameValuePair);
        String newQuery = URLEncodedUtils.format(p, HTTP.UTF_8);
        URI newURI = URIUtils.createURI(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), newQuery, uri.getFragment());
        return newURI;
    }

    public static String debugModel2RDFXML_ABBREV(Model model)
    {
        String result = null;
        // serialize as rdf/xml
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Using seems RDF/XML-ABBREV to have a huge performance cost on big
        // models
        // in case of problem use N-TRIPLE by uncommenting the line below
        // model.write(baos, IRmpsConstants.JENA_NTRIPLE_OUTPUT_LANGUAGE);
        model.write(baos, "RDF/XML-ABBREV");//$NON-NLS-1$
        result = new String(baos.toByteArray());
        // System.out.println(x2);
        return result;
    }

    public static String debugModel2N3(Model model)
    {
        String result = null;
        // serialize as rdf/xml
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Using seems RDF/XML-ABBREV to have a huge performance cost on big
        // models
        // in case of problem use N-TRIPLE by uncommenting the line below
        // model.write(baos, IRmpsConstants.JENA_NTRIPLE_OUTPUT_LANGUAGE);
        model.write(baos, "N3");//$NON-NLS-1$
        result = new String(baos.toByteArray());
        // System.out.println(x2);
        return result;
    }

    // public static String getBaseURL()
    // {
    // String hostName = "localhost";
    // if (thisHostName == null)
    // {
    // try
    // {
    // hostName = InetAddress.getLocalHost().getCanonicalHostName();
    // thisHostName = hostName;
    // }
    // catch (UnknownHostException e)
    // {
    // System.err.println("FAILED TO GET current host name because: " +
    // e.getMessage() + " going to use \"localhost\"");
    // e.printStackTrace();
    // }
    // }
    // else
    // hostName = thisHostName;
    // return ("https://" + hostName.toLowerCase() + ":9444/dm/");
    // }

    static protected String thisHostName = null;

    public static int randomFromArray(int... options)
    {
        int x = random.nextInt(options.length);
        return options[x];
    }

    public static <T> T randomFromArray(T... options)
    {
        int x = random.nextInt(options.length);
        return options[x];
    }

    public static String prettyPrintXml(String xml)
    {
        try
        {
            Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            // serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
            // "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            // serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
            // "yes");
            // serializer.setOutputProperty("{http://xml.customer.org/xslt}indent-amount",
            // "2");
            Source xmlSource = new SAXSource(new InputSource(new ByteArrayInputStream(xml.getBytes())));
            StreamResult res = new StreamResult(new ByteArrayOutputStream());
            serializer.transform(xmlSource, res);
            return new String(((ByteArrayOutputStream) res.getOutputStream()).toByteArray());
        }
        catch (Exception e)
        {
            // TODO log error
            return xml;
        }
    }

    static public final Random random = new Random();

    public static final byte[] genRandomBytes(int len)
    {
        len = (len > 255) ? 250 : len;
        byte[] b = new byte[len];
        for (int i = 0; i < len; b[i] = (byte) i++);
        return b;
    }

}

