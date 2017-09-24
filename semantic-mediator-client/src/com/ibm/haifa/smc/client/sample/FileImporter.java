/**
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
package com.ibm.haifa.smc.client.sample;

import org.apache.jena.rdf.model.Model;

import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IOutputModelHandle;
import com.ibm.dm.frontService.sm.intfc.ISmImporterIntercept;

public class FileImporter implements ISmImporterIntercept{

	private String importFileName = null;
	
	@Override 
	public void config() throws Exception {
		throw new Exception("Must call config with at least one parameter: String fileName.");
	}
	public void config(String fileName) throws Exception {
		importFileName = fileName;
	}
	@Override
	public void importModel(IOntologyHandle ontology, IOutputModelHandle model) throws Exception {
		model.setModel(this.model); 
	}

	private transient Model model = null;
	/**
	 * Sets the model in this importer to the one coming from the web interface, loading a model
	 * into the system.
	 * @param model provides a model to be set internally.
	 */
	public void setModel(Model model, String fileName) {
		this.model = model;
	}
	

}
