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

import java.io.OutputStream;

import org.apache.jena.rdf.model.Model;

public interface IOutputModelHandle extends IModelHandle {
	/**
	 * Returns an OutputStream by which the content of the model can be written by a
	 * text producer from an internally created model. It is expected to be written
	 * in an RDF/XML format.
	 * @return OutputStream for the RDF. Should be closed at the end, and should not be
	 * used more than once. To write the RDF again, this method must be called again.
	 */
	OutputStream getStream();
	/**
	 * Sets the model content from an RDF string in RDF/XML format.
	 */
	void setRdf(String rdf);
	/**
	 * Sets the model managed by this handle to the specified model thus replacing the entire
	 * model content of this handle. That allows a quick change of content although it has
	 * to be done in great care.
	 * @param model
	 */
	void setModel (Model model);
}
