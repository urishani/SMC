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
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 */

package com.ibm.dm.frontService.sm.intfc;

import java.io.InputStream;
import java.util.Date;

public interface IInputModelHandle extends IModelHandle {
	/**
	 * Returns an InputStream by which the content of the model can be read to be parsed by
	 * an RDF parser or other RDF parsing tools.
	 * @return InputStream for the RDF. Should be closed at the end, but also cannot be
	 * used more than once. To read the RDF again, this method must be called again.
	 */
	InputStream getStream();
	/**
	 * The etag of a model is a representation of the version of the entire model while each individual
	 * resource in that model may have their own etag to reflect on their individual versions.
	 * It is possible that no such value is maintained in the model.
	 * @return a Date representing the etag of the entire model, or null if such is not possible
	 * to maintain in the model.
	 */
	Date getEtag();
}
