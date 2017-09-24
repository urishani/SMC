
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

package com.ibm.dm.frontService.sm.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;

public class Helper {
	public static final String RHAPSODY_GUID_PREFIX = "GUID ";
	public static final String SPRINT_URI_PREFIX = "http://sprint/ID-";
	public static final String RHAPSODY_URI_PREFIX = "http://com.ibm.rhapsody/sprint/GUID-";

	
	public interface URITransform {
		// May return null, it means no transformation available
		String transform(String uri);
	}
	public static class PrefixURITransform implements URITransform {
		String srcPrefix, tgtPrefix;
		public PrefixURITransform(String srcPrefix, String tgtPrefix) {
			this.srcPrefix = srcPrefix;
			this.tgtPrefix = tgtPrefix;
		}

		public String transform(String uri) {
			if (!uri.startsWith(srcPrefix))
				return null;
			return uri.replace(srcPrefix, tgtPrefix);
		}
	}

    public static Map<String, String> extractIdMap(Model model, URITransform uriTransform) {
    	Map<String, String> idMap = new HashMap<String, String>();
    	ResIterator iter = model.listSubjects();
    	while (iter.hasNext()) {
    	    Resource r = iter.nextResource();
    	    String uri = r.getURI();
    	    String transformedURI = uriTransform.transform(uri);
    	    if(transformedURI != null) {
				idMap.put(uri, transformedURI);
	    	    System.out.println("Resource with URI: " + uri + " mapped to " + transformedURI);
    	    } else {
	    	    System.err.println("Resource with URI: " + uri + " was not mapped.");
    	    }
    	}
    	return idMap;
    }
    public static Map<String, String> transformURIs(Model model, URITransform uriTransform) {
    	Map<String, String> idMap = new HashMap<String, String>();
    	ResIterator iter = model.listSubjects();
    	while (iter.hasNext()) {
    	    Resource r = iter.nextResource();
    	    String uri = r.getURI();
    	    String transformedURI = uriTransform.transform(uri);
    	    if(transformedURI != null) {
    			idMap.put(uri, transformedURI);
        	    ResourceUtils.renameResource(r, transformedURI);
        	    
        	    System.out.println("Resource with URI: " + uri + " renamed to " + transformedURI);
    	    } else {
	    	    System.err.println("Resource with URI: " + uri + " was not renamed.");
    	    }
    	}
    		
    	return idMap;
    }
}

