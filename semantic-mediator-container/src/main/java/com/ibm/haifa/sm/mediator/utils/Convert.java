
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
package com.ibm.haifa.sm.mediator.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;


public class Convert {

	private static void writeFile (String folder, String file, Model output, String format) throws Exception
	{
		PrintWriter w = new PrintWriter(new FileWriter(new File(folder, file)));
		output.write(w, format);
		w.close();
	}

	private static Model readModel (String folder, String file, String format) throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		model.read(new FileReader(new File(folder, file)), null, format);
		return model;
	}

	public static void main (String [] args) throws Exception
	{
		Model m = readModel(".", "model-in.rdf", "TURTLE");
		writeFile(".", "model-out.rdf", m, "RDF/XML");
	}

}
