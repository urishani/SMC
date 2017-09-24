
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
 
 *| Copyright IBM Corp. 2011, 2013.
 *|                                                                        |
 *+------------------------------------------------------------------------+
 */

/**
 * The work leading to these results have received funding from the Seventh Framework Programme
 * SPRINT ICT-2009.1.3 Project Number: 257909
 */

package com.ibm.dm.frontService.sm.controller;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import org.apache.jena.rdf.model.*;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Port;
import com.ibm.dm.frontService.sm.service.ARdfRepository;
import com.ibm.dm.frontService.sm.utils.Utils;

@RestController
public class LuceneController  extends SmcBaseController {

	@RequestMapping(value="/smLucene", method=RequestMethod.GET, produces="text/plain" )
	public String doLuceneQuery(
			@RequestParam(value="id", required=false, defaultValue="") String id, 
			@RequestParam(value="query", required=false, defaultValue="") String query,
			@RequestParam(value="version", required=false, defaultValue="") String version,
			HttpServletRequest request,  HttpServletResponse response
			) throws Exception {
		boolean failed = false;
		String msg = "";
		onEntry(request);
		try {
			Database db = getDatabase(request);
			//      CloudantDatabase.needsCommit(db, false); not needed. it is the default.
			//		System.out.println( "called [/smLucene] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
			//	    	System.out.println("Lucene Provider called with id=["+id+"], query=[" + query + "], version=[" + version + "].");
			Port p = db.getPort(id);
			ARdfRepository rp = p.getModelRepository();
			Model m = rp.getModel();
			if (false == Strings.isNullOrEmpty(version)) {
				Model vm = rp.getModelForVersion(version, null);
				if (null != vm)
					m = vm;
			}
			Set<Property> properties = new HashSet<Property>();
			JSONArray viewConfig = p.getShowViewConfig();
			for (int i=0; i < viewConfig.length(); i++) {
				JSONObject c = (JSONObject) viewConfig.get(i); //i.next();
				boolean v = Boolean.parseBoolean(Utils.safeGet(c, "forText").toString());
				if (v) {
					JSONArray ps = (JSONArray)Utils.safeGet(c, "content");
					if (null != ps) {
						for (int k=0; k < ps.length(); k++) {
							String pS = ps.get(k).toString();
							properties.add(m.createProperty(pS));
						}
					}
				}
			}
			//	    	}
			if (properties.size() < 1) {
				msg =  "Error: Ontology of this model is not configured for text indexing and searching.";
				failed = true;
				return msg;
			}
			Directory dir = new RAMDirectory();
			dir.createOutput("stam", IOContext.DEFAULT);
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			IndexWriter writer = new IndexWriter(dir, iwc);


			ResIterator rs = m.listSubjects();
			boolean hasContents = false;
			while (rs.hasNext()) {
				Resource res = rs.next();
				Document doc = new Document();
				Field field = new StringField("url", res.getURI(), Field.Store.YES);
				doc.add(field);
				StringBuffer cont = new StringBuffer();
				for (Property property : properties) {
					NodeIterator ni = m.listObjectsOfProperty(res, property);
					while (ni.hasNext()) {
						cont.append(" ").append(ni.next().toString());
					}
				}
				field = new TextField("description", cont.toString(), Field.Store.YES);
				if (false == Strings.isNullOrEmpty(cont.toString().trim()))
					hasContents = true;
				doc.add(field);
				writer.addDocument(doc);
			}
			writer.close();
			// If no index there, then return now.
			if (!hasContents)
				return "Error: No information to search on. Nothing found.";
			// now search
			//	    	String x[] = dir.listAll();
			//	    	long l = dir.fileLength(x[0]);
			IndexReader reader = DirectoryReader.open(dir);
			IndexSearcher searcher = new IndexSearcher(reader);
			QueryParser parser = new QueryParser("description", analyzer);
			StringBuffer sb = new StringBuffer();
			try {
				Query q = parser.parse(query);
				TopDocs results = searcher.search(q, 1000);
				ScoreDoc[] hits = results.scoreDocs;
				for (int n=0; n < hits.length; n++) {
					Document doc = searcher.doc(hits[n].doc);
					sb.append(doc.get("url")).append("; \n");
				}
			} catch (ParseException e) {
				e.printStackTrace();
				//failed = true;
				msg = "Error: Bas search expression [" + e.getMessage() + "].";
				return msg;
			}
			if (sb.length() < 1) {
				//failed = true;
				msg = "Error: No resources match query!";
				return msg;
			}
			return sb.toString();
		} catch (Exception e) {
			failed = true;
			msg += "Error [" + e.getClass().getName() + ":" + e.getMessage();
			return msg;
	    } finally {
			onExit(request, failed, msg, false);
		}
	}
}
