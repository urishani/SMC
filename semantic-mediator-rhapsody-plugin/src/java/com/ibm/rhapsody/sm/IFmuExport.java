
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
 *|                                   |
 *| Copyright IBM Corp. 2011-2013.
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

/**
 * Licensed Material - Property of IBM
 * Copyright IBM  2013 All Rights Reserved
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 *
 */

package com.ibm.rhapsody.sm;

import com.telelogic.rhapsody.core.IRPModelElement;

/**
 * @author ariel
 *
 * An interface for exporting a Rhapsody model element into an FMU ZIP file.
 */
public interface IFmuExport {

	/**
	 * Performs the export of a ready FMU object on the file system, which is associated with a certain 
	 * block element, to the SMC server.
	 * @param block A Rhapsody model element for which FMU-generation is requested.
	 * @param path A full, absolute path name where the generated FMU should be written to, as a ZIP file.
	 * @throws Exception If anything goes wrong (except invalid arguments).
	 * @throws IllegalArgumentException If any of the passed arguments are invalid.
	 */
	public void exportElement(IRPModelElement block, String path) throws Exception, IllegalArgumentException;

//	P2 - There is a way to verify that a given element in focus is FMU-able:
//	     boolean FmuApi.canExport2Fmu(IRPModelElement). provided by Lev.

	/**
	 * Answers with an indication that it is legal and possible to export an FMU object for a particular block element.
	 * @param block A Rhapsody model element for which we want to verify whether it's FMU-able
	 * @return true/false
	 * @throws Exception If anything goes wrong.
	 */
	public boolean canExport2Fmu(IRPModelElement block) throws Exception;
}
