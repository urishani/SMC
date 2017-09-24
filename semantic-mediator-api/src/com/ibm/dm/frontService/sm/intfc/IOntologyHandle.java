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

import java.io.InputStream;
import java.util.Collection;

public interface IOntologyHandle {
	/**
	 * Returns an InputStream by which the content of the ontology can be read to be parsed by
	 * an RDF parser or other ontology parsing tools.
	 * @return InputStream for the ontology. Should be closed at the end, but also cannot be
	 * used more than once. To read the ontology again, this method must be called again.
	 */
	InputStream getStream();
	/**
	 * Returns a String that contains the entire ontology RDF secription in RDF/XML format.
	 * @return String containing the ontology in XML/RDF format.
	 */
	String getRdf();
	/**
	 * An ontology may depend on other ontologies (such as via includes). The list of all
	 * these dependent ontologies is returned in a Collection, which may be empty
	 * if no dependencies exist. 
	 * <br>
	 * This method does not do an exhaustive traversal of the dependencies tree recursively.
	 * @return A Collection of directly dependent ontologies of the present ontology.
	 */
	Collection<IOntologyHandle> getDependencies();
	// TODO
	String getBaseNS();
	// TODO
	String getVersionIRI();
}
