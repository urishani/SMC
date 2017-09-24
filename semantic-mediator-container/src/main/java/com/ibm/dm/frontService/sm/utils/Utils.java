
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

package com.ibm.dm.frontService.sm.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.*;
import org.apache.jena.vocabulary.*;
import com.ibm.dm.frontService.sm.data.ADatabaseRow;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.service.SmManager;
import com.ibm.dm.frontService.sm.service.SmService;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

public class Utils {

	public static final String DEFAULT_CONTENT_TYPE = "text/html"; // charset=UTF_8";
	
	/**
	 * Creates an input stream from a URL on the internet.
	 * @param nameSpace
	 * @return InputStream or null if failed.
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static InputStream streamFromNetwork(String nameSpace, String...contentType) throws ClientProtocolException, IOException  {
		HttpClient client = HttpClients.createDefault();
		HttpGet getter = new HttpGet(nameSpace);
		String type = IConstants.RDF_XML;
		if (contentType.length > 0)
			type = contentType[0];
		getter.setHeader("Accept", type);
		HttpResponse response;
			response = client.execute(getter);
			//int rc = response.getStatusLine().getStatusCode();
			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				InputStream in = response.getEntity().getContent();
				Header h[] = response.getHeaders(HTTP.CONTENT_TYPE);
				boolean match = false;
				String hv = "";
				for (Header header : h) {
					hv += ", " + header.getValue();
					if (header.getValue().equals(type)) {
						match = true;
						break;
					}
				}
				if (! match) {
					//String m = Utils.stringFromStream(in);
					//in = new ByteArrayInputStream(m.getBytes());
					//System.err.println(m);
					System.err.println("Input from Internet [" + nameSpace + "] Content-type [" + hv + "] does not match the accepted type [" + type + "]: ");
					System.err.println("Trying it anyway... just in case!");
//					return null;
				}
				return in;
			} else {
				System.err.println("Input from Internet  [" + nameSpace + "] Failed with status [" + response.getStatusLine().getStatusCode() + "]");
			}
		return null;
	}

	/**
	 * Produces an input stream from a readable file.
	 * @param fileName
	 * @return InputStream or null if failed.
	 * @throws FileNotFoundException
	 */
	public static InputStream streamFromFile(String fileName) throws FileNotFoundException {
		return new FileInputStream(new File(fileName));
	}

	/**
	 * Answers with a String containing content of the file.
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String stringFromStream(InputStream in) throws IOException {
		return new String(bytesFromStream(in));
	}
	/**
	 * Generates a byte array from an input stream.
	 * @param in
	 * @return String
	 * @throws IOException
	 */
	public static byte[] bytesFromStream(InputStream in) throws IOException {
        byte[] buf = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        while ((len = in.read(buf)) >= 0){
          baos.write(buf, 0, len);
        }
        in.close();
        return baos.toByteArray();
	}

	/**
	 * Generates a file from an input stream
	 * @param fileName
	 * @param in
	 * @throws IOException
	 */
	public static void fileFromStream(String fileName, InputStream in) throws IOException {
		OutputStream out = new FileOutputStream(new File(fileName));
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0){
          out.write(buf, 0, len);
        }
        out.flush();
        out.close();
	}

	/**
	 * Writes the buf into the file.
	 * @param file File to write content to
	 * @param buf data to be written
	 * @throws IOException
	 */
	public static void fileFromBytes(File file, byte[] buf) throws IOException {
		OutputStream out = new FileOutputStream(file);
		out.write(buf);
		out.flush();
		out.close();
	}

//	public static Model modelFromStream(InputStream in) throws IOException {
//		return modelFromStream(in, (String[])null);
//	}

	/**
	 * Reads a model from an input stream and converts is to a model.
	 * @param in Input text stream according to an HTML content type passed in.
	 * @param contentType optional content type. If null, default is application/rdf+xml.
	 * @param base base name space for the model, which can be null by default.
	 * @return a Jena model.
	 * @throws IOException
	 */
	public static Model modelFromStream(InputStream in, String contentType, String base) throws IOException {
		// create an empty model
		Model model = ModelFactory.createDefaultModel();
		// read the RDF/XML file
		String type = IConstants.RDF_XML;
		if ( ! Strings.isNullOrEmpty(contentType))
			type = contentType;
//		String m = Utils.stringFromStream(in);
//		if (m.startsWith("<?xml")) {
//			m = m.substring(m.indexOf('\n'));
//			m = m.substring(m.indexOf('<'));
//		}
		try {
		model.read(in, base, dictionary.get(type));
		} catch (Exception e) {
			//e.printStackTrace();
			throw new IOException(e.getClass() + ":" + e.getMessage());
//			System.out.println("Model: \n");
			//System.out.println(m);
		}
//		in.close();
		return model;
	}

	public static Model modelFromFile(String inputFileName) throws IOException {
		// use the FileManager to find the input file
		InputStream in = FileManager.get().open( inputFileName );
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + inputFileName + " not found");
		}

		return modelFromStream(in, IConstants.RDF_XML, null);
	}

	public static String normalizeContentType (String contentType) {
		if (contentType != null) {
			String [] CT = contentType.split(";");
			for (String ct : CT) {
				String rv = dictionary.get(ct.trim());
				if (rv != null)
					return ct;
			}
		}
		return null;
	}

	/**
	 * converts the map to a CSV with optional different separator.
	 * @param idMap Map o Strings
	 * @param separator Sttring which if null, than comma is used.
	 * @return String of lines with items in the map.
	 */
	public static String mapS2csv(Map<String, String> idMap, String separator) {
		if (null == separator)
			separator = ",";
		StringBuffer sb = new StringBuffer();
		for (String key : idMap.keySet()) {
			sb.append(key).append(separator).append(idMap.get(key)).append('\n');
		}
		return sb.toString();
	}

	/**
	 * converts the map to a CSV with optional different separator.
	 * @param idMap Map of Resource-es
	 * @param separator Sttring which if null, than comma is used.
	 * @return String of lines with items in the map.
	 */
	public static String mapR2csv(Map<Resource, Resource> idMap, String separator) {
		if (null == separator)
			separator = ",";
		StringBuffer sb = new StringBuffer();
		for (Resource key : idMap.keySet()) {
			Resource mappedResource = idMap.get(key);
			String mappedUri = mappedResource.getURI();
			String x = mappedResource.toString();
			if (null == mappedUri)
				mappedUri = x;
			sb.append(key.getURI()).append(separator).append(mappedUri).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Tests the request if it will accept the contentType.
	 * @param contentType http content type value;
	 * @param req HTTPRequest being processed.
	 * @return boolean true if accept patterns in the request matches that content type value.
	 */
	public static boolean willAccept(String contentType, HttpServletRequest req) {
		return null != willAcceptAny(new String[] {contentType}, req);
	}
	/**
	 * Like willAccept, but uses an array of options and returns the first one that matches.
	 * @param contentTypes - array of content types.
	 * @param req
	 * @return
	 */
	public static String willAcceptAny(String contentTypes[], HttpServletRequest req) {
		HashSet<String> setOfTypes = new HashSet<String>();
		for (String s: contentTypes) setOfTypes.add(s);
		Enumeration<String> acceptHds = req.getHeaders(HttpHeaders.ACCEPT);
		while( acceptHds.hasMoreElements()) {
			String acceptHdr = acceptHds.nextElement();
//			String acceptHdr = header.getValue();
			// split up the individual content types
			String[] hdrs = acceptHdr.split(",");
			for (String acctHdrVal : hdrs) {
//				AcceptType acceptType = new AcceptType( acctHdrVal);
				if(setOfTypes.contains(acctHdrVal))
					return acctHdrVal;
			}
		}
		return null ;
	}

	protected String acceptContentType(String[] prioritizedContentTypes, HttpServletRequest req) {
		// simplified content negotiation, first get all accepted types, and order by qs
		// then we walk through prioritized content types trying to find a match, in the
		// order that the server prefers
		List<AcceptType> acceptableTypes = new ArrayList<AcceptType>();

		Enumeration<String> acceptHds = req.getHeaders(HttpHeaders.ACCEPT);
		while(acceptHds.hasMoreElements()) {
			String acceptHdr = (String) acceptHds.nextElement();
			// split up the individual content types
			String[] hdrs = acceptHdr.split(",");
			for (String acctHdrVal : hdrs) {
				acceptableTypes.add( new AcceptType( acctHdrVal) );
			}
			// now sort by weight
			Collections.sort(acceptableTypes);

			// now find the most appropriate header
			for (String contentType : prioritizedContentTypes) {
				if( acceptableTypes.contains(new AcceptType(contentType) )  ) {
					return contentType;
				}
			}
		}
		return null;
	}

	/**
	 * Utility to get a multipart POST of a file. Answers with a map in which every part is saved in it as two keys
	 * One for the name and one for the value. E.g., field.name = 'name of the fiels', and field.value = 'value of the field.<br>
	 * The 'msg' key is mapped to a message about the success of failure of this method. "" means ok.
	 *
	 * @param request
	 *            HttpRequest of the post multipart.
	 * @param is
	 *            InputStream from the request which is used to build the context for uploading this multipar
	 *            content.
	 * @param service
	 *            SmService reference for various services.
	 * @param ContentType
	 *            Should be application/multipart
	 * @param database
	 *            Database of the configuration
	 * @return Map<String, Sring> of keys to values, where keys are as described above.
	 * @throws IOException
	 */
	public static Map<String, Object> loadModelFromFileHelper(HttpServletRequest request, InputStream is, SmService service, String ContentType) throws IOException
	{
	    RequestContext ctx = new Utils.ServiceRequestContext(request, is, ContentType);
	    boolean isMultipart = FileUploadBase.isMultipartContent(ctx);
	    Map<String, Object> map = new HashMap<String, Object>();
	    map.put("msg", "");
	    if (false == isMultipart)
	    {
	        map.put("msg", map.get("msg") + "Error: upload must use multipart/form-data content type post.\n");
	        return map;
	    }

	    //		String id=null;
	    //		String contents = null;

	    FileUpload uploader = new FileUpload(new Utils.ServiceFileItemFactory());
	    try
	    {
	        FileItemIterator iterator = uploader.getItemIterator(ctx);
	        while (iterator.hasNext())
	        {
	            FileItemStream stream = iterator.next();
	            String field = stream.getFieldName();
	            String name = stream.getName();
	            String contentType = stream.getContentType();
	            InputStream inputStream = stream.openStream();
	            List<byte[]> value = Utils.buffListFromStream(inputStream);
	            map.put(field + ".name", name);
	            map.put(field + ".value", value);
	            map.put(field + ".contentType", contentType);
	        }
	    }
	    catch (FileUploadException e)
	    {
	        e.printStackTrace();
	        map.put("msg", map.get("msg") + "Error: failed to upload model with [" + e.getClass().getName() + "]: [" + e.getMessage() + "].\n");
	    }

	    return map;
	}

	/**
	 * Answers with a FileItemIterator for a multipart post message.
	 * @param request
	 * @param is
	 * @param contentType
	 * @return
	 */
	static public FileItemIterator getMultipartIterator(HttpServletRequest request, InputStream is, String contentType) {
	    RequestContext ctx = new Utils.ServiceRequestContext(request, is, contentType);
		boolean isMultipart = FileUploadBase.isMultipartContent(ctx);
		if (false == isMultipart)
			return null;
		FileUpload uploader = new FileUpload(new Utils.ServiceFileItemFactory());
		FileItemIterator iterator = null;
	    try {
			iterator = uploader.getItemIterator(ctx);
		} catch (FileUploadException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return iterator;
	}

	/**
	 * Answers with a list of buffers, containing all the contents coming out of that stream.
	 * @param inputStream
	 * @return List of byte arrays.
	 * @throws IOException
	 */
	private static List<byte[]> buffListFromStream(InputStream inputStream) throws IOException {
		List<byte[]> result = new ArrayList<byte[]>();
		while (true) {
			byte buff[] = new byte[1024];
			int p= 0;
			while (p < buff.length){
				int r = inputStream.read(buff, p, buff.length - p);
				if (r < 1)
					break;
				p+= r;
			}
			if (p < buff.length) { // finished
				if (p > 0) {
					byte b[] = ArrayUtils.subarray(buff, 0, p);
					result.add(b);
				}
				return result;
			}
			result.add(ArrayUtils.clone(buff));
		}
	}

	public static String replaceAll(String string, String replace, String value) {
		if (null == value) value = "";
		int pos = string.indexOf(replace);
		while (pos >= 0) {
			string = string.substring(0, pos) + value + string.substring(pos + replace.length());
			pos = string.indexOf(replace, pos + value.length());
		}
		return string;
	}

	private static class AcceptType implements Comparable<AcceptType> {
		public String type;
		public int weight;
		public AcceptType(String typeExp) {
			int pos = typeExp.indexOf(';');
			if( pos > 0 ) {
				this.type = typeExp.substring(0,pos).trim();
				String qualifier = typeExp.substring(pos+1);
				if( qualifier.startsWith("q=") ) {
					try{
						float w = Float.parseFloat(qualifier.substring(2)) * 1000;
						this.weight = (int) w;
					}catch( NumberFormatException e ) {
						this.weight = 1000;
					}
				} else {
					this.weight = 1000;
				}

			} else {
				this.type = typeExp;
				this.weight = new Integer(1000);
			}
		}

		public int compareTo(AcceptType other) {
			return ((AcceptType)other).weight - weight;
		}

		@Override
		public boolean equals(Object obj) {
			if( obj instanceof AcceptType ) {
				AcceptType otherAcceptType = (AcceptType) obj;
				// now we need to sort out wildcards
				String[] otc = otherAcceptType.type.split("/");
				String[] ttc = this.type.split("/");
				boolean seg1 = compareSegment(otc[0], ttc[0]);
				boolean seg2 = compareSegment(otc[1], ttc[1]);
				return seg1 && seg2;
			} else if( obj instanceof String ) {
				String[] otc = ((String)obj).split("/");
				String[] ttc = this.type.split("/");
				boolean seg1 = compareSegment(otc[0].trim(), ttc[0].trim());
				boolean seg2 = compareSegment(otc[1].trim(), ttc[1].trim());
				return seg1 && seg2;
			}
			return false;
		}

		public boolean compareSegment(String s1, String s2 ) {
			if( s1.equals("*") || s2.equals("*") ) {
				return true;
			}
			return s1.equals(s2);
		}
	}

	/**
	 * Helper class to work with Apache file upload package.
	 *
	 * @author shani
	 */
	public static class ServiceRequestContext implements RequestContext
	{

	    private final HttpServletRequest request;
	    private final String      contentType;
	    private final InputStream is;

	    public ServiceRequestContext(HttpServletRequest request, InputStream is, String contentType)
	    {
	        super();
	        this.request = request;
	        this.is = is;
	        this.contentType = contentType;
	    }

	    public String getCharacterEncoding()
	    {
	        return "UTF-8";
	    }

	    public int getContentLength()
	    {
	        int len = -1;
	        String contentLengthHdr = request.getHeader(HttpHeaders.CONTENT_LENGTH);
	        if (null == contentLengthHdr) return len;
	        String contentLengthStr = contentLengthHdr; //.getValue();
	        try
	        {
	            len = Integer.parseInt(contentLengthStr);
	        }
	        catch (Exception e)
	        {}
	        return len;
	    }

	    public String getContentType()
	    {
	        return contentType;
	    }

	    public InputStream getInputStream()
	    {
	        return is;
	    }

	}

	public static class ServiceFileItemFactory implements FileItemFactory
	{

	    public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName)
	    {
	        FileItem fileItem = new DiskFileItem("fileName", contentType, false, fileName, 100, new File(Database.SM_MODEL_FOLDER));
	        return fileItem;
	    }

	}

	/**
	 * Utility to safely convert to String also with null objects which are converted to "".
	 * @param object some Object
	 * @return String of the object, or a "" if that object is null.
	 */
	public static String stringify(Object object) {
		if (null == object)
			return "";
		return object.toString().trim();
	}

	public static String forHtml(String msg) {
		return msg.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
	public static String escapeQuotes(String text) {
		return replaceAll(replaceAll(text, "\"", "\\\""), "\'", "\\\'");
	}
	public static String escapeQuotesAndNL(String text) {
		return replaceAll(escapeQuotes(text), "\n", " ");
	}

	/**
	 * A discrtionary mapping HTTP content types to those that Jena can represent in a text syntax.
	 */
	static Map<String, String> dictionary = createDictionary();
	/**
	 * A Set of HTTP content types which can transmit the textual representation of an RDF dataset in one of its syntaxes which Jena can work with.
	 */
	static Set<String> rdfContentType = dictionary.keySet();

	/**
	 * Static generator of the disctionary defined above.
	 * @return a Disctionary Map.
	 */
	static private Map<String, String> createDictionary() {
		Map<String, String> map = new HashMap<String, String>();
		map.put(IConstants.RDF_XML, "RDF/XML");
		map.put(IConstants.RDF_XML + "-ABBREV", "RDF/XML-ABBREV");
		map.put(IConstants.N_TRIPLE, "N-TRIPLE");
//		map.put(IConstants.NTRIPLE, "N-TRIPLE");
		map.put(IConstants.TURTLE, "TURTLE");
		map.put(IConstants.N3, "N3");
		return map;
	}
	
	/**
	 * Answers with a String representation of the model according to a given media type, with
	 * default rdf+xml.
	 * @param model Model to be represented as text.
	 * @param acceptType Media type. If null, default is <code>application/rdf+xml</code>
	 * @return String representation of model contents.
	 */
	public static String modelToText(Model model, String acceptType)
	{
		StringWriter sw = new StringWriter();
		if (Strings.isNullOrEmpty(acceptType) || null == dictionary.get(acceptType))
			acceptType = IConstants.RDF_XML;
		model.write(sw, dictionary.get(acceptType));
		String rdf = " " + sw.toString();
		return rdf;
	}


	/**
	 * answers with a Jena format name based on the accept hdr item of the request.
	 * @param request a Request containing an acceptType hdr by which the resolution is done.
	 * @return String for hte Jena format name as defined in the dictionary
	 */
	public static String formatFromAcceptType4Jena(HttpServletRequest request) {
		Set<String> keys = dictionary.keySet();
		for (String key : keys) {
//			String value = dictionary.get(key);
			if (willAccept(key, request))
				return key;
		}
		return null;
	}

	/**
	 * Answers with the object of a property as String, or the string "none" if not found.
	 * @param model Model to search
	 * @param resource Resource of the property
	 * @param property Property of the resource
	 * @return String "none" if none found, or a String version of the object.
	 */
	public static String object4PropertyAsString(Model model,
			Resource resource, Property property) {
		return object4PropertyAsString(model, resource, new Property [] { property });
	}
	/**
	 * Answers with the object of a property as String, or the string "none" if not found.
	 * @param model Model to search
	 * @param resource Resource of the property
	 * @param properties JSONArray of properties string URIs.
	 * @return String "none" if none found, or a String version of the object.
	 * @throws JSONException 
	 */
	public static String object4PropertyAsString(Model model,
			Resource resource, JSONArray properties) throws JSONException {
		Property[] ps = new Property[properties.length()];
		for (int i = 0; i < properties.length(); i++) {
			ps[i] = model.getProperty((String) properties.get(i));
		}
		return object4PropertyAsString(model, resource, ps);
	}

	/**
	 * Answers with the object of a property as String, or the string "none" if not found.
	 * @param model Model to search
	 * @param resource Resource of the property
	 * @param properties Properties to look for, the first property found will be returned
	 * @return String "-" if none found, or a String version of the object.
	 */
	public static String object4PropertyAsString(Model model,
			Resource resource, Property [] properties) {
		for (Property property : properties) {
			Statement stmt = model.getProperty(resource, property);
			if (null != stmt)
				return stmt.getObject().toString();
		}
		return "-" +
				"";
	}

	/**
	 * Answers with a legal prefixes portion of a SPARQL query for a given map which shoule
	 * be a prefixes map for a certain model.
	 * @param map is a prefix map taken out of a certain model
	 * @return String of the prefix part of an SPARQL query.
	 */
	public static String makePrefix4Query(Map<String,String> map) {
		StringBuffer sb = new StringBuffer();
		Iterator<String> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			sb.append("PREFIX ").append(key).append(": <").append(map.get(key)).append(">\n");
		}
		return sb.toString();
	}

	/**
	 * Assumes the field is a camel and turns it into words in capitals.
	 * @param field Camel style String.
	 * @return String with separate capitalized words as a title.
	 */
	public static String wordsFromCamel(String field) {
		StringBuffer sb = new StringBuffer();
		int p = 0, i = 0;
		for (i = p; i < field.length(); i++) {
			if (Character.isUpperCase(field.charAt(i))) {
				if (sb.length() > 0)
					sb.append(' ');
				sb.append(Character.toUpperCase(field.charAt(p)));
				if (i > p)
					sb.append(field.substring(p+1, i));
				p = i;
			}
		}
		if (i > p) {
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(Character.toUpperCase(field.charAt(p)) + (i > p ? field.substring(p+1, i):""));
		}

		return sb.toString();
	}

	/**
	 * Convert the resource URI to a prefixed resource format based on the map provided
	 * @param uri String URI to be reformatted
	 * @param map Map of prefix to its value.
	 * @return String of the URI prefixed according to the map.
	 */
	public static String prefixForResource(String uri, Map<String, String> map) {
		int len = 0;
		String prefix = null;
		if (null == uri) {
			return "null";
		}
		for (String prefixCandidate : map.keySet()) {
			String prefixValue = map.get(prefixCandidate);
			if (uri.startsWith(prefixValue)) {
				int nLen = prefixValue.length();
				if (nLen > len) {
					len = nLen;
					prefix = prefixCandidate;
				}
			}
		}
		if (null != prefix) {
			return prefix + ":" + uri.substring(len);
		}
		return uri;
	}

	/**
	 * Takes a Resource iterator and answers with a sorted collection of the resources URI String-s
	 * <br>
	 * Sorts the strings in ignore case comparison.
	 * @param iter ResIterator of resoruces from a certain model.
	 * @return Sorted collection of the URIs of the resources.
	 */
	public static Collection<String> sortResources(ResIterator iter) {
		return sortResources(iter, new HashSet<String>());
	}
	public static Collection<String> sortResources(ResIterator iter, Collection<String> filter) {
		List<String> result = new ArrayList<String>();
		while (iter.hasNext()) {
			String uri = iter.next().getURI();
			if (null == uri || filter.contains(uri))
				continue;
			result.add(uri);
		}
		Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
		return result;
	}

	/**
	 * Convert the input name String to a string which can be part of a URL.
	 * @param name String to be fixed.
	 * @return String of the fixed name prepended with a /.
	 */
	public static String makeName(String name) {
		if (Strings.isNullOrEmpty(name))
			return "";
		String parts[] = name.split(" ");
		name = parts[0];
		if (parts.length > 1) // only two parts.
			name += "_" + parts[1];
		// now clear the name
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (Character.isJavaIdentifierPart(ch))
				continue;
			name.replace(ch, '_');
		}
		return "/" + name;
	}

	public static String IDENT = "IDENT";
/**
 * Answers with a String which can be an identifier.
 * @param name String to be fixed.
 * @return String of the fixed name prepended with a /.
 */
public static String makeIdentifier (String name) {
	if (Strings.isNullOrEmpty(name))
		return IDENT;
	for (int i = 0; i < name.length(); i++) {
		char ch = name.charAt(i);
		if (false == Character.isJavaIdentifierPart(ch))
			name = name.replace(ch, '_');
	}
	return name;
}
/**
 * Answers with a String which can be a prefix for a name space.
 * @param ns String of name space to be prefixed.
 * @return String of the prefix.
 */
public static String makePrefix (String ns) {
	try {
		URL url = new URL(ns);
		ns = url.getPath();
	} catch (Exception e) {
		return null;
	}
	if (Strings.isNullOrEmpty(ns))
		return null;
	if (ns.endsWith("#"))
		ns = ns.substring(0, ns.length()-1);
	if (ns.endsWith("/"))
		ns = ns.substring(0, ns.length()-1);
	int p = ns.lastIndexOf('/');
	if (p < 0)
		return ns;
	return ns.substring(p);
}

	/**
	 * Utility which answers with the enriched revision of the htmlPage with the content in Json format
	 * given to it. <br>
	 * The Json object may contain keys associated with String values, or JSONArray values.
	 * Keys with String values are simply replaced in the htmlPage String, where the key is prepended and appended with an
	 * underscore character.
	 * Keys with JSONArray values are going through a more complex replacement policy via a call to <code>mergeJsonArray</code>
	 * @see #mergeJsonArray(String, JSONArray, String)
	 * @param htmlPage
	 * @param contents
	 * @return
	 * @throws JSONException 
	 */
	public static String mergeJsonWithHtml(String htmlPage, JSONObject contents) throws JSONException {
		@SuppressWarnings("rawtypes")
		Iterator keys = contents.keys();
		while (keys.hasNext()) {
			String key = (String)keys.next();
//		for (Object key : keys) {
//			if (key.equals("num")) {
//				System.out.println("key=" + key);
//			}
			Object val = Utils.safeGet(contents, key.toString());
			if (null == val)
				continue;
			boolean asIs = false;
			if (((String)key).startsWith("@")) {
					asIs = true;
					key = ((String)key).substring(1);
			}
			if (val instanceof JSONArray)
				htmlPage = mergeJsonArray(htmlPage, (JSONArray)val, (String)key);
			else {
				String v = val.toString();
				if (v.startsWith("@")) {
					asIs = true;
					v = v.substring(1);
				}
				if (! asIs)
					v = Utils.forHtml(v);
				htmlPage = replaceAll(htmlPage, "_" + key + "_", v);
			}
		}
		return htmlPage;
	}

	/**
	 * Answers an error message to the client based on the OSLC specifications that requires the answer to 
	 * be formatted according to the Accept specifications of the request. The source of the message can 
	 * be a String, or an exception from which the message texg is generated. If both are provider (i.e., not null)
	 * than the exception is preferred over the msg text, which in this case is ignored.
	 * @param msg - text of the error to be returned.
	 * @param e - Exception from which the text of the error message is generated.
	 * @param response
	 * @param request
	 */
	public static void responseForOslcError(String msg, Exception e, HttpServletResponse response, HttpServletRequest request) {
		if (Strings.isNullOrEmpty(msg))
			msg = "Error with no explanations";
		String extendedMsg = "";
		if (null != e) {
			msg = "Exception [" + e.getClass().getSimpleName() + "]: " + e.getMessage();
			Throwable ext = e.getCause();
			if (ext != null) {
				extendedMsg = "Caused by [" + ext.getClass().getSimpleName() + "]: " + ext.getMessage();
			}
		}
		String logMsg = "";
		String rdfAcceptType = Utils.formatFromAcceptType4Jena(request);
		String acceptType = "text/plain";
		if (false == Strings.isNullOrEmpty(rdfAcceptType)) {
			acceptType = rdfAcceptType;
			Model m = ModelFactory.createDefaultModel();
			m.setNsPrefix("oslc", OSLCConstants.OSLC_V2); 
			Resource error = m.createResource();
			m.add(error, RDF.type, m.createResource(OSLCConstants.OSLC_V2 + "Error"));
			m.add(error, m.createProperty(OSLCConstants.OSLC_V2 + "message"), msg);
			if (false == Strings.isNullOrEmpty(extendedMsg)) {
				Resource extendedError = m.createResource();
				m.add(extendedError, RDF.type, m.createResource(OSLCConstants.OSLC_V2 + "ExtendedError"));
				m.add(error, m.createProperty(OSLCConstants.OSLC_V2 + "extendedError"), extendedError);			
			}
			logMsg= Utils.modelToText(m, Utils.formatFromAcceptType4Jena(request));
		} else {
			logMsg = "Error. Msg [" + msg + "], extended Msg [" + extendedMsg + "]";
		}
		System.out.println("Error occurred:\n" + logMsg);
		Utils.respondWithText(response, logMsg, acceptType);
	}
	/**
	 * Applies a JSON contents object to a template, after getting it out of the
	 * class path, and perform the response as an HTML content type.
	 * <p>
	 * If the response is not null, the text is also pushed through it.
	 * @param contents JSONObject of the contents to be merged into the htmlPage template.
	 * @param templateName String path to the template html file in the class path.
	 * @param response HttpResonse to make the response with the merged contents.
	 * @param type String for the media type of the response.
	 * @throws JSONException 
	 */
	public static String respondGetWithTemplateAndJson(JSONObject contents,
			String templateName, HttpServletResponse response, String type) throws JSONException {
		String htmlPage = Utils.getHtmlTemplate(templateName);
		htmlPage = Utils.mergeJsonWithHtml(htmlPage, contents);
		if (null != response)
			respondWithText(response, htmlPage, type);
		return htmlPage;
	}
	/**
	 * Feeds the result of combining a template with content to the response,
	 * unless it is null, in which case the resuls are returned.
	 * @param contents
	 * @param templateName
	 * @param response
	 * @return
	 * @throws JSONException 
	 */
	public static String respondGetWithTemplateAndJson(JSONObject contents,
			String templateName, HttpServletResponse response) throws JSONException {
		return respondGetWithTemplateAndJson(contents, templateName, response, DEFAULT_CONTENT_TYPE);
	}

	/**
	 * Responds with html content.
	 * @param response
	 * @param text
	 */
	public static void respondWithText(HttpServletResponse response, String text) {
		respondWithText(response, text, DEFAULT_CONTENT_TYPE);
	}

	/**
	 * Fill up response with a textual content.
	 * @param response
	 * @param text String text to be content of response.
	 * @param type String for the media type of the response.
	 */
	public static void respondWithText(HttpServletResponse response, String text, String type) {
		response.setHeader(HttpHeaders.CONTENT_TYPE, type);
		response.setStatus(HttpStatus.SC_OK);
		try {
			setEntity(response, text);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean isDm(HttpRequest request) {
//		IJfsClient jfsClient = JfsClient.create(request);
//		String p = 	jfsClient.getConfigurationContext().getProjectUri();
//		return false == Strings.isNullOrEmpty(p);
		return false;
	}
	
	public static void respondWithBytes(HttpServletResponse response, byte[] contents,
			String type) throws IOException {
		response.setHeader(HttpHeaders.CONTENT_TYPE, type);
		response.setStatus(HttpStatus.SC_OK);
		response.setContentLength(contents.length);
		response.getOutputStream().write(contents);
		// Entity(new ByteArrayEntity(contents));
	}

	public static void respondWithFile(HttpServletResponse response, File file,
			String type) {
		response.setHeader(HttpHeaders.CONTENT_TYPE, type);
		response.setStatus(HttpStatus.SC_OK);
		response.setContentLength((int)file.length());
		OutputStream os = null;
		FileInputStream is = null;
		try {
			os = response.getOutputStream();
			is = new FileInputStream(file);
			byte[] buffer = new byte[16*1024];
			int s = 0;
			while ((s = is.read(buffer)) > 0) {
				os.write(buffer, 0, s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != is)	try {is.close();} catch (IOException e) {}
			if (null != os) try {os.close();} catch (IOException e) {}
		}
		// Entity(new FileEntity(file, type));
	}


	public static void respondWithJson(HttpServletResponse response,
			JSONObject contents) {
		respondWithText(response, contents.toString(), IConstants.JSON);
//		
//		response.setHeader(HttpHeaders.CONTENT_TYPE, ContentTypes.JSON);
//		response.setStatus(HttpStatus.SC_OK);
//		try {
//			setEntity(response, contents.toString());
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
	}

	public static void respondWithAttachment(HttpServletResponse response, String filename, byte contents[]) throws IOException {
		// TODO - Do we want to do anything with the file name?
		// response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
		response.setHeader("Content-Disposition", "attachment; filename=" + filename);
		response.setStatus(HttpStatus.SC_OK);
		respondWithBytes(response, contents, IConstants.APPLICATION_OCTET_STREAM);
	}


	/**
	 * Utility to return the content of a template file in the class path as a String.
	 * <br> Objects are in the templates/ folder.
	 * @param template String path to the file in the class path. If not suffixed with ".html", or prefixed
	 * with the folder templates/ - that is fixed automatically.
	 * @return String content of the file.
	 */
    public static String getHtmlTemplate(String template)
    {
        if (null == template) template = "templates/manager.html";
        int p = template.indexOf('#');
        String part = null, part_e = null;
        if (p >= 0) {
        	part = template.substring(p) + "#";
        	part_e = "#/" + template.substring(p+1) + "#";
        	template = template.substring(0, p);
        }
        if (false == template.endsWith(".html"))
        	template += ".html";
        if (false == template.startsWith("templates/") && false == template.startsWith("/templates/"))
        	template = "templates/" + template;
        String htmlPage =  loadFromClassPath(template);
        if (null == htmlPage)
        	return htmlPage = "<html><body>Template [" + template + "] is missing. Page cannot be shown.</body><htnl>";
        if (null != part) {
        	p = htmlPage.indexOf(part);
        	if (p >=0)
        		htmlPage = htmlPage.substring(p + part.length());
        	p = htmlPage.indexOf(part_e);
        	if (p >= 0)
        		htmlPage = htmlPage.substring(0, p);
        }
       	return htmlPage;
    }

    /**
	 * Utility to return the content of a file in the class path as a String.
	 * @param path String path to the file in the class path.
	 * @return String content of the file.
     */
    public static String loadFromClassPath(String path) {
    		return new String(bytesFromClassPath(path));
    }

    /**
     * Answers with the content of a file in the class path as a byte array.
     * @param path
     * @return
     */
    public static byte[] bytesFromClassPath(String path) {
    		//System.out.println("path=[" + path  + "]");
		if (path.startsWith("/"))
			path = path.substring(1); // remove first / character.
        ClassLoader cl = SmManager.class.getClassLoader();
        URL url = cl.getResource(path);
        //System.out.println("url=[" + url + "]");
        if (null == url) {
        		path = "/"+path;
        		//System.out.println("path=[" + path  + "]");
        		url = cl.getResource(path);
        		//System.out.println("url=[" + url + "]");
        }
        InputStream in = cl.getResourceAsStream(path);
		try {
	        return bytesFromStream(in);
		} catch (IOException e) {
			return new byte[0];
		}
    }



	/**
	 * Answers with an enriched template resolving a repeated set of content in the array parameter passed in.<br>
	 * A template String enclosed in the htmlPage String between two markers with first one is the key, prefixed and suffixed with
	 * an underscore<br>
	 * The end marker is same, except that prepend string is "_/".<br>
	 * For each element in the array, it is assumed to be a content Json object treated recursively by the <code>mergeJsonWithHtml</code>.
	 * @param htmlPage
	 * @param val a Json array of objects to be merged with the pattern part identified in the htmlPage param. Each object is
	 * merged with that template and the results are appended.
	 * @param key String identifying the start and end of the pattern in the htmlPage paramewter.
	 * @return String of the enrixhed input parameter htmlPage.
	 * @throws JSONException 
	 * @see #mergeJsonWithHtml(String, JSONObject)
	 */
	private static String mergeJsonArray(String htmlPage, JSONArray val, String key) throws JSONException {
		String start = "_"+key+"_", end ="_/"+key+"_";
		int p = htmlPage.indexOf(start);
		if (p < 0)
			return htmlPage;
		String prefix = htmlPage.substring(0, p);
		htmlPage = htmlPage.substring(p + start.length());
		p = htmlPage.indexOf(end);
		if (p < 0)
			return htmlPage;
		String pattern = htmlPage.substring(0, p);
		String suffix = htmlPage.substring(p + end.length());
		StringBuffer sb = new StringBuffer();
		for (int i= 0; i < val.length(); i++) { //Object object : val) {
			Object object = val.get(i);
			if (object instanceof JSONObject) {
				String instance = mergeJsonWithHtml(pattern, (JSONObject)object);
				sb.append(instance).append("\n");
			}
		}
		return prefix + sb.toString() + suffix;
	}

	/**
	 * Answers with the legal host name to be used.
	 * If the client is not null, it is used, otherwise, the environment is inquired for that.
	 * @param client - IJfsClient which can be null.
	 * @return String of the legal and unique host name to be used for the server.
	 */
    public static String getHost(Object client, boolean... full) {
//    	try {
//    		RmpsConfig config = null;
//    		if (null == client)
//    			config = RmpsConfig.getInstance();
//    		else
//    			config = client.getRmpsConfig();

//    		if (null != config) {
//        		URL url = new URL(config.getRmpsFrontSideUrl());//tPublicRootUrl());
//    			String host = url.getHost();
//    			if (Utils.isOptional(false, full))
//    				return url.getProtocol() + "://" + host + ":" + url.getPort();
//    			return host;
    	String host = Database.getHost(full);
    	if (null == host) 
    		try {
    			return Utils.stringFromStream(Runtime.getRuntime().exec("hostname").getInputStream()).trim();
    		} catch (IOException e) {
    			e.printStackTrace();
    			return "hostNameUnknown";
    		}
    	//    	} catch (MalformedURLException e) {
    	//    		throw new RmpsRuntimeException(e);
    	//		}
    	return host;
    }


    /**
     * Uses <i>modelFromStream</i> to parse the string to a model. Same parameters
     * @param rdf String contaning an RDF model in some syntax.
     * @param contentType HTML content type of the model syntax to guide how to parse it. If null, deafault is application/rdf+xml.
     * @param base Optiona base name space for the model, may be null.
     * @return Jena model.
     * @throws IOException
     */
	public static Model modelFromString(String rdf, String contentType, String base) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(rdf.getBytes());
		return modelFromStream(is, contentType, null);
	}

	/**
	 * Answers with a new map in which prefixes do not contain dots as in the
	 * Jena inner NS j.0, j.1, etc.<br>
	 * This method also removes duplicate entries, keeping the one with the longest key - i.e., prefix.
	 * @param nsPrefixMap A map from a Jena model mapping prefixes to name spaces.
	 * @return a possibly modified map
	 */
	public static Map<String, String> fixNsPrefixes(Map<String, String> nsPrefixMap) {
		// remove duplicate NS-s, keep the longer ones.
		Map<String, String> inverse = new HashMap<String, String>();
		boolean hasDuplicates = false;
		for (String key: nsPrefixMap.keySet()) {
			String value = nsPrefixMap.get(key);
			if (inverse.containsValue(value)) {
				hasDuplicates = true;
				String pKey = inverse.get(value);
				if (pKey.length() < key.length()) {
					inverse.put(value, key);
				}
			} else
				inverse.put(value, key);
		}
		// Set dcterms prefix to be 'dcterms'
		if (false == "dcterms".equals(inverse.get(DCTerms.NS))) {
			inverse.put(DCTerms.NS, "dcterms");
			hasDuplicates = true;
		}
		if (hasDuplicates) { // need to recreate from the de-duplicated inverse map
			nsPrefixMap.clear();
			for (String iKey: inverse.keySet())  // These are the values, and the values are the keys
				nsPrefixMap.put(inverse.get(iKey), iKey);
		}
		boolean ok = true;
		for (String prefix : nsPrefixMap.keySet()) {
			if (prefix.contains(".")) {
				ok = false;
				break;
			}
		}
		if (ok)
			return nsPrefixMap;
		Map<String, String> newMap = new HashMap<String, String>(nsPrefixMap.size());
		for (String prefix : nsPrefixMap.keySet()) {
			newMap.put(prefix.replace('.', '_'), nsPrefixMap.get(prefix));
		}
		return newMap;
	}

	public static List<Map<String, Object>> getExtentions(String extension) {
		IExtensionRegistry extentionRegistry = Platform.getExtensionRegistry();
		IConfigurationElement[] webBundleConfig = extentionRegistry
			.getConfigurationElementsFor(extension); //$NON-NLS-1$
//		IExtensionPoint p[] = Platform.getExtensionRegistry().getExtensionPoints();
//		for (IExtensionPoint iExtensionPoint : p) {
//			String l = iExtensionPoint.getLabel();
//			String id = iExtensionPoint.getNamespaceIdentifier();
//			System.out.println(l + "; " + id);
//		}
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(webBundleConfig.length);
		for (IConfigurationElement e : webBundleConfig) {
			Map<String, Object> item = new HashMap<String, Object>();
			result.add(item);
			try {
				Object executable = e.createExecutableExtension("class");
				if (false == executable instanceof ISmModuleIntercept) {
					item.put("configurationElement", "Error: class is not an instance of ISmModuleIntercept");
				} else {
					item.put("configurationElement", 
						e);
					System.out.println("Successfuly initializing intercepter class: [" + executable.getClass().getName() + "]");
				}
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
//				e1.printStackTrace();
				System.out.println("Error initializing intercepter: [" + e1.getClass().getName() + " - " + e1.getMessage() + "]");
				item.put("configurationElement", "Error: " + e1.getClass().getName() + ": " + e1.getMessage());
			}
			String attributes[] = e.getAttributeNames();
			for (String string : attributes) {
				item.put(string, e.getAttribute(string));
			}
//			item.put("name", e.getAttribute("name")); //$NON-NLS-1$
//			item.put("class", e.getAttribute("class")); //$NON-NLS-1$
//			item.put("description", e.getAttribute("description")); //$NON-NLS-1$
//			item.put("requiresLicense", e.getAttribute("requiresLicense")); //$NON-NLS-1$
//			item.put("licenseText", e.getAttribute("licenseText")); //$NON-NLS-1$
		}
		return result;
	}

	/**
	 * Answers with a triple in which the prefixes map is applied to each element in the triple
	 * @param map Map<String, String> of ns names to prefixes.
	 * @param resources String of blank-separated resources to be converted.
	 * @return Fixed string.
	 */
	public static String applyPrefixesOnResources(Map<String, String> map,
			String resources) {
//		String parts[] = resources.split("<|>");
		String parts[] = resources.split(" ");
//		String nParts[] = new String[parts.length];
		StringBuffer sb = new StringBuffer();
		for (int i= 0; i < parts.length; i++) {
			String a = parts[i];
			if (a.matches("http.*://.*")) {
				sb.append(prefixForResource(a, map));
			} else
				sb.append(a);
			sb.append(" ");
		}
//		int p0 = 0;
//		for (int i= 0; i < nParts.length; i++) {
//			if (null != nParts[i] && false == nParts[i].equals(parts[i])) {
//				int p1 = resources.indexOf(parts[i]) - 1; // for the preceding <
//				if (p1 < 0)
//					System.out.println(resources);
//				sb.append(resources.substring(p0, p1)).append(nParts[i]);
//				p0 = p1 + parts[i].length() + 2; // for the following >
//			}
//		}
//		if (p0 < resources.length())
//			sb.append(resources.substring(p0));
		return sb.toString();
	}

	/**
	 * Deletes the file, and if it is a folder = only if it is empty, unless force is true - it deletes it recursively.
	 * @param file File of a folder
	 * @return true if all succeeded, false otherwise.
	 */
	public static boolean deleteFolder(File file, boolean force) {
		if (null == file || false == file.exists())
			return true;
		if (false == file.isDirectory())
			return file.delete();
		boolean success = file.delete();
		if (!success && force) {
			// delete contents
			String members[] = file.list();
			for (String member : members) {
				success = deleteFolder(new File(file, member), force);
				if (false == success)
					return false;
			}
			return file.delete();
		}
		return success; // this is success value from line 880 above (11 lines above).
	}

	/**
	 * Answers with the URL w/out the query part.
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 */
	public static String getUrlFullPath(String url) throws MalformedURLException {
		 String path = new URL(url).getPath();
		 url = url.substring(0, url.indexOf(path)) + path;
		 return url;
	}

	public static byte[] bytesFromFile(File file) throws IOException {
		if (false == file.canRead() || file.length() < 1)
			return new byte[0];
		byte buff[] = new byte[(int)file.length()];
		int p = 0;
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			while (p < buff.length)
				p+= in.read(buff, p, buff.length - p);
			return buff;
		} finally {
			if (null != in)
				in.close();
		}
	}

	/**
	 * Answers with a merged String of all the elements of the list as String-s, concatenated.
	 * @param buffs List of byte arrays.
	 * @return String of all bytes concatenated as String-s.
	 */
	public static String buffList2String(List<byte[]> buffs) {
		if (null == buffs)
			return null;
		StringBuffer sb = new StringBuffer();
		for (byte[] buff : buffs) {
			sb.append(new String(buff));
		}
		return sb.toString();
	}

	/**
	 * Streams the content of the list of bute arrays into the file.
	 * @param file File to write the contatenated string into.
	 * @param content List of byte arrays.
	 * @throws IOException
	 */
	public static void fileFromBuffList(File file, List<byte[]> content) throws IOException {
		OutputStream out = new FileOutputStream(file);
		for (byte[] buf : content) {
			out.write(buf);
		}
		out.flush();
		out.close();
	}

    /**
     * Utility to test an optional boolean argument for its value with a default.
     * @param def boolean default.
     * @param opt boolean optional argument to be tested.
     * @return boolean according to rules above.
     */
    public final static boolean isOptional(boolean def, boolean... opt) {
    	return (null != opt && opt.length > 0) ? opt[0] : def;
    }

    /**
     * Answers with a JSONObject from the String argument passed to it.
     * @param json String consisting of an JSON object.
     * @return JSONOBject for that input String.
     * @throws JSONException 
     */
	public static JSONObject jsonFromString(String json) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		if (false == Strings.isNullOrEmpty(json)) {
			jsonObject = new JSONObject(json);
		}
		return jsonObject;
	}

	public static Model createEmptyOntology(String ns, String version, Map<String, String> params) {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		if (Strings.isNullOrEmpty(ns))
			return model;
		if (false == (ns.endsWith("/") || ns.endsWith("#")))
			ns += "#";
//		model.createClass(ns + "CLASS-TO-BE-REMOVED-IN-AN-EMPTY-ONTOLOGY");
		boolean initializeForOSLC = "on".equals(params.get("initializeForOSLC")) ;
		String prefix = params.get("prefix");
		if (Strings.isNullOrEmpty(prefix))
			prefix = "base";

        Property rdfType = RDF.type; //model.getProperty(IConstants.RDF_NAMESPACE + "type");
        Resource owlOntology = OWL.Ontology; //model.getResource(IConstants.OWL_ONTOLOGY);
    	Map<String, String> map = model.getNsPrefixMap();
        map.put(prefix, ns);
        model.createResource(ns).addProperty(rdfType, owlOntology);
        if (false == Strings.isNullOrEmpty(version)) {
            Property owlVersionProperty = OWL.versionInfo;
            model.createResource(ns).addProperty(owlVersionProperty, /*model.createResource(*/ version); //);
        }
        // now add the top class and its standard properties for name and description to make it OSLC
        // compliant
        if (initializeForOSLC) {
        	map.put("dcterms", DCTerms.NS);
        	Resource topClass = model.createResource(ns + prefix + "_Classes");
        	topClass.addProperty(RDF.type, RDFS.Class);
        	topClass.addProperty(RDFS.label, prefix + " classes@en");
        	topClass.addProperty(RDFS.comment, "Top class for all classes in this ontology.\nRecommending that all other classes descent as subClasses of this.@en");
        	Resource dctermsTitle = model.getResource(DCTerms.title.toString());
        	dctermsTitle.addProperty(RDFS.domain, topClass);
        	dctermsTitle.addProperty(RDF.type, OWL.DatatypeProperty);
        	dctermsTitle.addProperty(RDFS.isDefinedBy, model.getResource(DCTerms.NS));
        	Resource dctermsDescription = model.getResource(DCTerms.description.toString());
        	dctermsDescription.addProperty(RDFS.domain, topClass);
        	dctermsDescription.addProperty(RDF.type, OWL.DatatypeProperty);
        	dctermsDescription.addProperty(RDFS.isDefinedBy, model.getResource(DCTerms.NS));
        	Resource topDataProperty = model.createResource(ns + prefix + "_DataProperties");
        	topDataProperty.addProperty(RDF.type, OWL.DatatypeProperty);
        	topDataProperty.addProperty(RDFS.label, prefix + " data properties@en");
        	topDataProperty.addProperty(RDFS.comment, "Top data property/predicate for all data properties in this ontology.\nRecommending that all other properties descent as subProperties of this.@en");
        	Resource topObjectProperty = model.createResource(ns + prefix + "_ObjectProperties");
        	topObjectProperty.addProperty(RDF.type, OWL.ObjectProperty);
        	topObjectProperty.addProperty(RDFS.label, prefix + " object properties@en");
        	topObjectProperty.addProperty(RDFS.comment, "Top object property/predicate for all object properties in this ontology.\nRecommending that all other properties descent as subProperties of this.@en");
        }
        model.setNsPrefixes(map);
        return model;
	}

	/**
	 * Answers with spelled-out URI for a given prefixed uri, and a map from prefixes to namespaces.
	 * @param subject URI in the format <prefix>:<uri>
	 * @param map Map from <prefix> to <ns>
	 * @return full uri String.
	 */
	public static String uriFromPrefix(String subject, Map<String, String> map) {
		String parts[] = subject.split(":");
		if (parts.length < 2)
			return subject;
		String ns = map.get(parts[0]);
		if (null == ns)
			return subject;
		return ns + parts[1];
	}

	/**
	 * Reads from the class path a file which is a (possibly encrypted) zip file, and extracting from it
	 * a String content from one of its members. If zip file is missing, or member is missing, or password protection failed,
	 * results are an empty string.
	 * @param zipPath Path in the class path of the zip file.
	 * @param password Possibly null if file is not password protected. Otherwise provide a password or results are empty.
	 * @param path Path for the file inside the zip file.
	 * @return String content, or empty string.
	 */
	public static String loadFromZipInClassPath(String zipPath, String password,
			String path) {
        ClassLoader cl = SmManager.class.getClassLoader();
        InputStream in = cl.getResourceAsStream(zipPath);
        File tmp;
        String result = "";
		try {
			tmp = File.createTempFile("css", "zip");
			tmp.deleteOnExit();
	        FileOutputStream out = new FileOutputStream(tmp);
	        int x;
	        while ((x = in.read()) >= 0)
	        	out.write(x);
	        out.close();
	        ZipFile zipFile = new ZipFile(tmp);
//	        if (zipFile.isEncrypted())
//	        	zipFile.setPassword(password);
	        FileHeader file = zipFile.getFileHeader(path);
	        if (null != file) {
	        	if (file.isEncrypted())
	        		file.setPassword(password.toCharArray());
	        	ZipInputStream zipIn = zipFile.getInputStream(file);
	        	result = stringFromStream(zipIn);
	        }
	        tmp.delete();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ZipException e) {
			e.printStackTrace();
		} catch (RuntimeException x) {
			x.printStackTrace();
		}
		return result;
	}

	/**
	 * Answers with a concatenated string of the input array, with the
	 * concatenation string as a glue.
	 * @param info Array of String to be concatenated
	 * @param glue String to glue the pieces
	 * @param fromTo optional int than can be up to two numbers: first indicated from which cell of the array. Missing means from the start (from = 0),<br>
	 * Second indicate to which cell of the array to concatenate. Missing means to the end (to = info.length -1)
	 * @return
	 */
	public static String concat(String[] info, String glue, int...fromTo) {
		if (null == info)
			return "";
		if (info.length < 2)
			return info[0];
		int from = fromTo.length>0?fromTo[0]:0;
		int to = fromTo.length>1?fromTo[1]:info.length;
		from = Math.max(0, from);
		to = Math.min(info.length, to);
		from = Math.min(to, from);
		if (null == glue)
			glue = ",";
		StringBuffer sb = new StringBuffer();
		for (int i = from; i < to; i++) { //String string : info) {
			if (sb.length() > 0)
				sb.append(glue);
			sb.append(info[i]);
		}
		return sb.toString();
	}

	/**
	 * Splits a comma separated list to an array of trimmed strings, which may also be set to lower case
	 * @param csv String to be split up.
	 * @param comma String separator which may be comma or other separators
	 * @param ignoreCase boolean that if true will set all strings to lc.
	 * @return String array of processed tokens of the input CSV
	 */
	public static String[] csv2array(String csv, String comma, boolean ignoreCase) {
		if (Strings.isNullOrEmpty(comma))
			comma = ",";
		String [] result = csv.split(comma);
		for (int i= 0; i < result.length; i++) {
			String s = result[i].trim();
			if (ignoreCase)
				s = s.toLowerCase();
			result[i]= s;
		}
		return result;
	}

	/**
	 * Answer with a filtered list so that only elements having tags as in the list provided in the filter parameter
	 * are passed. If filter is empty of missing - no filter is applied.
	 * @param items a list of ADatabaseRow elements to be filtered.
	 * @param filter array of tags to be applied as a filter.
	 * @return filtered list.
	 */
	public static List<ADatabaseRow> filter(List<ADatabaseRow> items, String[] filter) {
		if (null == filter || filter.length < 1)
			return items;
		List<String> fTags = Arrays.asList(filter);
		List<ADatabaseRow> result = new ArrayList<ADatabaseRow>(items.size());
		for (ADatabaseRow item : items) {
			if (filter(item, fTags))
				result.add(item);
		}
		return result;
	}
	/**
	 * Answer true if the item parameter is ok by the filter in the fTags parameter, false if it is filtered out.
	 * @param item a ADatabaseRow element to be filtered.
	 * @param fTags is a List of String tags to be applied as a filter.
	 * @return boolean true if item parameter is ok with the filter.
	 */
	public static boolean filter(ADatabaseRow item, List<String> fTags) {
		if (null == fTags || fTags.size() < 1)
			return true;
		String rTags[] = Utils.csv2array(item.getTags(), ",", true);
		for (String rTag : rTags) {
			if (fTags.contains(rTag))
				return true;
		}
		return false;
	}

	public static void setEntity(HttpServletResponse response, String text)
	throws IOException
	{
//		StringEntity entity = new StringEntity(text, HTTP.UTF_8);
		//entity.setContentEncoding(HTTP.UTF_8);
		response.setContentLength(text.length());
		response.setCharacterEncoding(IConstants.UTF_8); 
		response.getWriter().append(text);
//		
//		response.setEntity(entity);
	}

	/**
	 * Answers with a boolean indicating if the value is a legal URL.
	 * @param v1 String that may be a URL to be tested for validity.
	 * @return boolean of the resulting check.
	 */
	public static boolean isURL(String v1) {
		try {
			new URL(v1);
			return true;
		} catch (MalformedURLException e) {};
		return false;
	}

	/**
	 * Answers null if the uri parameter is legal URI, or explanation for its failure to be so.
	 * @param url
	 * @return String explanation of the error, or null if okay
	 */
	public static String isLegalURI(String url, boolean...simple) {
		try {
			URI x = new URI(url);
//			System.out.println(x);
			if (null != simple && simple.length > 0 && simple[0])
				return null;
			if (Strings.isNullOrEmpty(x.getScheme()))
				return "Missing Schema part.";
			if (Strings.isNullOrEmpty(x.getPath()))
				return "Missing Path part.";
		} catch (URISyntaxException  e) {
			return e.getMessage();
		}
		return null;
	}

	/**
	 * Safely gets from a JSONObject a possibly empty field.
	 * @param obj	JSONObject to be inquired
	 * @param key String to query about.
	 * @return string or null if missing.
	 * @throws JSONException 
	 */
	public static Object safeGet(JSONObject obj, String key)  {
		try {
			if (! obj.has(key))
				return null;
			return obj.get(key);
		} catch (JSONException e) {
			return null;
		}
	}
	/**
	 * Safely gets from a JSONObject a possibly empty field, with a default value
	 * @param obj	JSONObject to be inquired
	 * @param key String to query about.
	 * @param def - default value to return;
	 * @return string or null if missing.
	 * @throws JSONException 
	 */
	public static Object safeGet(JSONObject obj, String key, Object def) {
		Object result = safeGet(obj, key);
		if (null == result)
			return def;
		return result;
	}

	/**
	 * Wrapper on the SecutiryManager.checkExec() method to return boolean indicating 
	 * that the cmd parameter is indeed na executable command (path or a command in the 
	 * path env. variable.
	 * @param cmd String of a command to be executed.
	 * @return boolean true if possible, false if not.
	 */
	public static boolean checkExec(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			p.destroy();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * Answers a true if the resource has a triple with the property and resource value to it.
	 * @param resource A Resource subject of a triple.
	 * @param aProp Property predicate of the triple
	 * @param aValue Resource value of the triple
	 * @return true if such a triple exists in the model of the resource.
	 */
	public static boolean resourceHasPropertyValue(Resource resource,
			Property aProp, Resource aValue) {
    	StmtIterator stmts = resource.listProperties(aProp);
    	while (stmts.hasNext()) {
    		Statement stmt = stmts.next();
    		if (stmt.getObject().equals(aValue))
    			return true;
    	}

		return false;
	}

	/**
	 * Takes an RDF string in one format, and converts it to another format. Formats are specified
	 * in HTTP Mime Types.
	 * @param rdf String of an RDF
	 * @param rdfXml the mime type for the syntax of that RDF. For instance - rdf/xml. If null, default is application/rdf_xml.
	 * @param turtle the mime type for the syntax of the returned RDF string. For instance - turtle. If null, default is text/turtle.
	 * @return
	 * @throws IOException 
	 */
	public static String convertModelSyntax(String rdf, String rdfXml, String turtle) throws IOException {
		if (Strings.isNullOrEmpty(rdfXml))
			rdfXml = IConstants.RDF_XML;
		if (Strings.isNullOrEmpty(turtle))
			turtle = IConstants.TURTLE;
		Model model = modelFromString(rdf, rdfXml, null);
		if (null != model)
			return modelToText(model, turtle);
		return null;
	}

	public static String mapToString(Map<String, String> pchange) {
		return Maps.newHashMap(pchange).toString();
	}

	/*
	 * Answers with the resource of type ontology in this ontology model. If that is missing, it is not a proper ontology,
	 * But we assume that perhaps the prefix "base" is used to denote the ontology, so we get it.
	 * Which may end up in a null in the worst case and cause a failure down the line.
	 */
	public static String getOntologyNS(Model ontology) {
		ResIterator iter = ontology.listResourcesWithProperty(RDF.type, OWL.Ontology);
		if (iter.hasNext())
			return iter.next().toString();
		return ontology.getNsPrefixURI("base");
	}

	/**
	 * Answers with the value of the System environment variables, and if missing, than with the default).
	 * If default is null, it will return null as well if the environment variable is missing.
	 * @param name - Name of the environment variable.
	 * @param pDefault - Default value if the variable is not defined.
	 * @return the defined value, or the default.
	 */
	public static String getenv(String name, String pDefault) {
		String env = System.getenv(name);
		return (null == env) ? pDefault: env;
	}

	/**
	 * Snwers with true if the name spces compare, including an optional hash-tag at the end that is ignored.
	 * @param nameSpaceUri
	 * @param classUri
	 * @return
	 */
	public static boolean compareOntologyNamespace(String namespace1, String namespace2) {
		return
				trimOptional(namespace1,"#").equals(trimOptional(namespace2, "#"));
	}

	/**
	 * Answers with the string input trimmed from its ending strings t.
	 * @param string
	 * @param t
	 * @return
	 */
	public static String trimOptional(String string, String t) {
		while (string.endsWith(t))
			string = string.substring(0, string.length()-t.length());
		return string;
	}

	public static String StringFromStatements(StmtIterator stmts) {
		StringBuffer sb = new StringBuffer();
		while (stmts.hasNext()) {
			sb.append(stmts.next().toString());
			if (stmts.hasNext())
				sb.append(",\n");
		}
		return sb.toString();
	}
}


