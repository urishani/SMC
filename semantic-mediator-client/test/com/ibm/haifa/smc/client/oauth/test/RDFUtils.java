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
package com.ibm.haifa.smc.client.oauth.test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.ibm.team.json.JSONArray;
import com.ibm.team.json.JSONObject;

public class RDFUtils {

	public static String writeModel(Model model) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		writeModel(model, output);
		return output.toString();
	}

	public static void writeModel(Model model, String filename) throws Exception {
		writeModel(model, new FileOutputStream(filename));
	}

	public static Model readModel(JSONObject json) {
		Model model = ModelFactory.createDefaultModel();
		JSONObject results = (JSONObject)json.get("results");
		JSONArray data = (JSONArray)results.get("data");
		for (Object t : data) {
			JSONArray triple = (JSONArray)t;
			String s = (String)triple.get(0);
			String p = (String)triple.get(1);
			String o = (String)triple.get(2);

			if (s.startsWith("http")) {
				Resource subject = model.createResource(s);
				Property predicate = model.createProperty(p);
				if (o.startsWith("http")) {
					Resource object = model.createResource(o);
					subject.addProperty(predicate, object);
				} else {
					Literal object = model.createLiteral(o);
					subject.addProperty(predicate, object);
				}
			}
		}
		return model;
	}

	public static Model readModel2(JSONObject json) {
		Model model = ModelFactory.createDefaultModel();
		JSONObject results = (JSONObject)json.get("results");
		JSONArray bindings = (JSONArray)results.get("bindings");
		for (Object t : bindings) {
			JSONObject triple = (JSONObject)t;
			String s = (String)((JSONObject)triple.get("a")).get("value");
			String p = (String)((JSONObject)triple.get("b")).get("value");
			String o = (String)((JSONObject)triple.get("c")).get("value");

			// TODO - The new format includes type info, no need to guess which one is a URI!
			if (s.startsWith("http")) {
				Resource subject = model.createResource(s);
				Property predicate = model.createProperty(p);
				if (o.startsWith("http")) {
					Resource object = model.createResource(o);
					subject.addProperty(predicate, object);
				} else {
					Literal object = model.createLiteral(o);
					subject.addProperty(predicate, object);
				}
			}
		}
		return model;
	}

	private static void writeModel(Model model, OutputStream os) throws Exception {
		model.write(os, "RDF/XML");
		os.close();
	}

	public static JSONArray extractQueryResults(JSONObject json) {
		JSONArray results = (JSONArray) (((JSONObject) json.get("results")).get("data"));
		return results;
	}

}
