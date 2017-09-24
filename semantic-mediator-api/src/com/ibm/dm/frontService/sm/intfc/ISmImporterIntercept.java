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
/**
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 *
 */

package com.ibm.dm.frontService.sm.intfc;

public interface ISmImporterIntercept {

	/**
	 * A method to set up the importer and verify that the configuration parameters are legal.
	 * This method may have parameters as indicated in the extension point, so using it will
	 * employ reflective Java API.
	 * @throws Exception
	 */
	void config() throws Exception;
	/**
	 * Imports a model, which is produced in the model handle, and which is designed according to 
	 * the given ontology.
	 * @param ontology an {@link IOntologyHandle} for the ontology governing the content of the model.
	 * @param model an {@link IModelHandle} which is produced by this importer.
	 */
	void importModel(IOntologyHandle ontology, IOutputModelHandle model) throws Exception;

}
