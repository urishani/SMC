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

import java.net.URI;

public interface IOntologyLocator {
	/**
	 * Given a class URI, this method locates the ontology in which this class is defined.
	 * <br>
	 * In case there are multiple such ontologies, than the first one is returned.
	 * @param classUri A URI of that class.
	 * @return IOntologyHandler, or null if no appropriate ontology was found for that class.
	 */
	IOntologyHandle getOntologyForClass ( URI classUri); 

}
