
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
 * Licensed Material - Property of IBM
 * Copyright IBM 2011 All Rights Reserved
 * The work leading to these results have received funding from the Seventh Framework Programme
 * SPRINT ICT-2009.1.3 Project Number: 257909
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 */
package com.ibm.dm.frontService.sm.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.http.HttpStatus;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.service.Attachment;
import com.ibm.dm.frontService.sm.service.ISmService;
import com.ibm.dm.frontService.sm.service.Repository;
import com.ibm.dm.frontService.sm.utils.Utils;

public class SmBlobs
{

	/////////////////////////////////////////////////////////////////////////

    private static Map<String, SmBlobs> blobs = new HashMap<String, SmBlobs>();
    private static Model smModel = SmOntology.create().getModel();
    private static String smBase = smModel.getNsPrefixMap().get("");

    public static final String ATTACHMENTS_PORT_ID = "PrtAR";
    public static final String HAS_ATTACHMENT_PROPERTY = smBase + "hasAttachment";
    public static final String IS_MIME_TYPE_PROPERTY = smBase + "isMimeType";
	private static final String IS_ATTACHMENT_SIZE_PROPERTY = smBase + "isAttachmentSize";    
    public static final String HAS_ATTACHMENT_TYPE_PROPERTY = smBase + "hasAttachmentType";
    public static final String HAS_ATTACHMENT_SIZE_UNIT_PROPERTY = smBase + "hasAttachmentSizeUnit";
	public static final String BLOBS = "blobs";
//    public static final String HAS_ATTACHMENT_SIZE_UNIT_PROPERTY = smBase + "hasAttachmentSizeUnit";
//	private static final String DC = "http://purl.org/dc/terms/";
//	private static final String DC_TITLE_PROPERTY = DC + "title";
//	private static final String DC_DESCRIPTION_PROPERTY = DC + "description";
//	private static final String DC_CREATOR_PROPERTY = DC + "creator";
//	private static final String DC_CONTRIBUTOR_PROPERTY = DCTerms.contributor; 
	static Property titleP = DCTerms.title; //model.getProperty(DC_TITLE_PROPERTY);
	static Property descriptionP = DCTerms.description; //model.getProperty(DC_DESCRIPTION_PROPERTY);
	static Property creatorP = DCTerms.creator; //model.getProperty(DC_CREATOR_PROPERTY);
	static Property contributorP = DCTerms.contributor; //model.getProperty(DC_CONTRIBUTOR_PROPERTY);
	private final Database mOwner ;
    
    public void doBlobGet(HttpServletRequest request, HttpServletResponse response, String[] segments, ISmService smService) 
    {
    	// Segments would be: "blobs", <blob-set>, "resource", <blob-id>
    	String blobSet = segments.length > 1 ? segments[2] : null;
    	String blobId = segments.length > 3 ? segments[4] : null;
    	
        if (segments.length < 4)
        {
        	fail(response, "URL does not exist.");
            return;
        }
        String url = request.getRequestURL().toString(); //RmpsConfig.getInstance().getBaselinesUrl();

        try
        {
        	Repository ra = getArRepository();
            Model model = ra.getModel();
            url = Utils.getUrlFullPath(url);
            
            StmtIterator stmts = model.listStatements(null, model.getProperty(HAS_ATTACHMENT_PROPERTY), model.createResource(url));
            Resource raResource = null;
            if (stmts.hasNext()) {
            	Statement stmt = stmts.next();
            	raResource = stmt.getSubject();
            }
            if (null == raResource) {
            	fail(response, "Resource [" + url + "] not managed");
            	return;
            }
            Statement stmt = model.getProperty(raResource, model.getProperty(IS_MIME_TYPE_PROPERTY));
            String mimeType = null;
            if (null != stmt)
            	mimeType = stmt.getObject().toString().trim();
//            if (stmts.hasNext()) {
//            	Statement stmt = stmts.next();
//            	mimeType = stmt.getObject().toString().trim();
//            }
            if (Strings.isNullOrEmpty(mimeType))
            	mimeType = "text/plain";
            Attachment blob = Attachment.create(blobSet, blobId); //new File(new File(Database.SM_MODEL_FOLDER), blobSet);
//            if (false == blob.canRead()) {
//            	fail(response, "Url [" + url + "] has no contents to rerurn.");
//            	return;
//            }
//            byte contents[] = blob.get(); //Utils.stringFromStream(Utils.streamFromFile(blob.getAbsolutePath()));
            Utils.respondWithFile(response, blob.getFile(), mimeType);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail( response, "Request for url [" + url + "] failed [" + e.getMessage() + "].");
        }

    }

    


    private static void fail(HttpServletResponse response, String string) {
    	response.setStatus(HttpStatus.SC_NOT_ACCEPTABLE);
        Utils.respondWithText(response, string);
	}



    /**
     * Creates the resource from the posted input.
     * @param request
     * @param response
     * @param is
     * @param contentType
     * @param file
     * @param params
     * @throws Exception
     */
	public void createResource(HttpServletRequest request, HttpServletResponse response, String contentType, MultipartFile file, JSONObject params) throws Exception 
	{
//		FileItemIterator iterator = Utils.getMultipartIterator(request, is, contentType);
//		InputStream contentStream = null;
        String hasSmResource = (String)Utils.safeGet(params, "hasSmResource");
        String description = (String)Utils.safeGet(params, "description"); 
        String title = (String)Utils.safeGet(params, "title");
        String creator = (String)Utils.safeGet(params, "creator"); 
        String contributor = (String)Utils.safeGet(params, "contributor"); 
        File tmpFile = Attachment.createTemp();
        if (!file.isEmpty())
        		Utils.fileFromStream(tmpFile.getAbsolutePath(), file.getInputStream());
        String payloadContentType = file.getContentType();
        String set = (String)Utils.safeGet(params, "set"); 
//        while (iterator.hasNext()) {
//			FileItemStream stream = iterator.next();
//			String field = stream.getFieldName();
//			String name = stream.getName();
//			contentType = stream.getContentType();
//			InputStream content = stream.openStream();
//			if ("attachment-file".equalsIgnoreCase(field)) {
//				if (null != contentStream) {
//					fail(response, "More than one contents part. Illegal.");
//					return;
//				}
//				contentStream = content;
//				Utils.fileFromStream(tmpFile.getAbsolutePath(), contentStream);
//		        payloadContentType = contentType;
//			} else if ("attachment-file".equalsIgnoreCase(field)) {
//				if (null != contentStream) {
//					fail(response, "More than one contents part. Illegal.");
//					return;
//				}
//				contentStream = content;
//				Utils.fileFromStream(tmpFile.getAbsolutePath(), contentStream);
//		        payloadContentType = contentType;
//			} else if ("hasSmResource".equalsIgnoreCase(field)) {
//		        hasSmResource = Utils.stringFromStream(content);				
//			} else if ("description".equalsIgnoreCase(field)) {
//		        description = Utils.stringFromStream(content);
//			} else if ("title".equalsIgnoreCase(field)) {
//		        title = Utils.stringFromStream(content);
//			} else if ("creator".equalsIgnoreCase(field)) {
//		        creator = Utils.stringFromStream(content);
//			} else if ("contributor".equalsIgnoreCase(field)) {
//		        contributor = Utils.stringFromStream(content);
//			} else if ("set".equalsIgnoreCase(field)) {
//		        set = Utils.stringFromStream(content);
//			} 
//		}
        // Create the atachment repository resource for this upload
        Repository ar = getArRepository();
        Model model = ar.getModel();
        Property hasAttachmentP = model.getProperty(HAS_ATTACHMENT_PROPERTY);
        Property mimeTypeP = model.getProperty(IS_MIME_TYPE_PROPERTY);
//        Property hasSmResourceP = model.getProperty(IRhpConstants.SM_PROPERTY_SM_RESOURCE_FULL);
        Property setP = model.getProperty(HAS_ATTACHMENT_TYPE_PROPERTY);
        Property sizeUnitP = model.getProperty(HAS_ATTACHMENT_SIZE_UNIT_PROPERTY);
        Property sizeP = model.getProperty(IS_ATTACHMENT_SIZE_PROPERTY);
        
        long blobSize = tmpFile.length();
        String unit = "KB";
        int blobUnits = (int) Math.round(blobSize / 1024.0);
        if (blobSize < 1024*1024*1024 && blobSize >= 1024*1024) {
        	unit = "MB";
        	blobUnits = (int) Math.round(blobSize / (1024.0 * 1024));
        } else if (blobSize >= 1024 * 1024 * 1024){ 
        	unit = "GB";
        	blobUnits = (int) Math.round(blobSize / (1024.0 * 1024 * 1024));
        }
        Resource sizeUnit = model.createResource(smBase + "SmSize" + unit);
        
        boolean isNew = false;
        Resource resource = null;
        if (null != hasSmResource) {
        		resource = ar.getResource(hasSmResource);
        }
        if (null == resource) {
        		resource = ar.getResource();
        		isNew = true;
        }
        
        // update the mime type of the attachment:
        resource.removeAll(mimeTypeP);
        resource.addLiteral(mimeTypeP, model.createLiteral(payloadContentType));
       	
        if (null != title) { // update title only if one is provided in the message
            resource.removeAll(titleP);
            resource.addLiteral(titleP, model.createLiteral(title));
        }
        if (null != description) { // update description only if one is provided in the message
            resource.removeAll(descriptionP);
            resource.addLiteral(descriptionP, model.createLiteral(description));
        }
        
        // figure out the type of attachment according to the SM ontology.
        if (null != set) { 
        		Resource smAttachmentTypeR = smModel.createResource(smBase + "SmAttachmentType");
       		Resource r = smModel.getResource(smBase + set);
       		if (smModel.contains(r, RDF.type, smAttachmentTypeR))
       			set = r.toString();
       		else
       			set = null;
 		}
        	if (null == set) 
        		set = smBase + "SmBlob";
        // Now set up the set.
        	resource.removeAll(setP);
        	resource.addProperty(setP, model.createResource(set));
        
        	if (null != creator && isNew) { // update creator only if one is provided in the message, and it is a new resource.
        		resource.addLiteral(creatorP, model.createLiteral(creator));
        	}
        
        	if (isNew)
        		resource.addProperty(RDF.type, smModel.createResource(smBase + "SmAttachment"));
        	String num = resource.getURI();
        	num = num.substring(num.lastIndexOf('/')+1);
        	if (null != contributor) { // update contributor only if one is provided in the message
        		resource.removeAll(contributorP);
        		resource.addLiteral(contributorP, model.createLiteral(contributor));
        	}
        
        	// Now add size and unit:
        	resource.removeAll(sizeP);
        	resource.removeAll(sizeUnitP);
        	resource.addLiteral(sizeP, model.createLiteral(new Integer(blobUnits).toString()));
        	resource.addLiteral(sizeUnitP, sizeUnit);
        
        	// Now store the final blob in its final location:
        	// Create the attachment and obtain its URL.
        	String attType = set.substring(smBase.length());
        	Attachment attachment = Attachment.create(attType, num);
        	boolean saved = attachment.save(tmpFile);
        if (!saved) {
        		fail(response, "Cannot save contents. Failed.");
        		return;
        }
        
        String uri = request.getRequestURL().toString();
        // Replace in the Uri trailing following the "repository" with the attachment id in the "blobs" segment/
        int p = uri.indexOf(Repository.REPOSITORY);
        if (p > 0)
        		uri = uri.substring(0, p) + BLOBS + "/" + attachment.getId();
        else {
        		fail(response, "Request URI [" + uri + "] is illegal.");
        		return;
        }
        resource.removeAll(hasAttachmentP);
        resource.addProperty(hasAttachmentP, model.createResource(uri));
        ar.setDirty();
        ar.save();

        Utils.respondWithText(response, uri);
	}

	/**
	 * Helper to get the AR repository.
	 * @return
	 * @throws Exception
	 */
	private Repository getArRepository() throws Exception {
        Database db = getOwner();
        Port raPort = db.getPort(ATTACHMENTS_PORT_ID);
//        String domain = raPort.getAccessName();
        Repository ra = (Repository) raPort.getModelRepository(); //Repository.create(Utils.getHost(null), Database.SM_MODEL_FOLDER, domain);
		return ra;
	}



	public Database getOwner() {
		return mOwner;
	}




	private SmBlobs(Database owner)
    {
        super();
//        if (null == owner)
//        	owner = Database.create();
        mOwner  = owner;

    }

    public static synchronized SmBlobs getBlobs(Database owner)
    {
    	String project = owner.getProject();
    	
    	if (null == blobs.get(project))
    		blobs.put(project, new SmBlobs(owner));
    	return blobs.get(project);
    }

	public static boolean deleteResource(String object) {
		String myHost = Utils.getHost(null);
		try {
			if (false == myHost.equalsIgnoreCase(new URL(object).getHost()))
				return false;
			int p = object.indexOf(BLOBS); 
			if (p < 0)
				return false;
			URL url = new URL(object);
			String segments[] = url.getPath().split("/");
	    	String blobSet = segments.length > 3 ? segments[4] : null;
	    	String blobId = segments.length > 5 ? segments[6] : null;
	    	
	        if (segments.length < 6)
	        	return false;
			
	        try {
				Attachment attachment = Attachment.create(blobSet, blobId);
				return attachment.delete(); 
		
			} catch (IOException e) {
				return false;
			}

		} catch (MalformedURLException e) {
			return false;
		}

		
	}

}
