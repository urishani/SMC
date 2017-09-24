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

import org.apache.jena.rdf.model.Model;

public interface IModelHandle {
	/**
	 * Returns a String that contains the entire RDF model in RDF/XML format.
	 * @return String containing the RDF in XML/RDF format.
	 */
	String getRdfXML();
	/**
	 * The etag of a model is a representation of the version of the entire model while each individual
	 * resource in that model may have their own etag to reflect on their individual versions.
	 * It is possible that no such value is maintained in the model.
	 * @return a Date representing the etag of the entire model, or null if such is not possible
	 * to maintain in the model.
	 */
	IOntologyHandle getOntology();
	/**
	 * Answers with the entire model in the handler, which provides for an efficient way of
	 * obtaining that model from the handle.
	 * @return a Model that is maintained within this handle.
	 */
	Model getModel();

	/**
	 * Answers with the base URI of the model name space. This one may be null since it is not clear how to
	 * make out which NS is the base of the model.
	 * @return String for the name space URI, or null.
	 */
	String getBase();
}
