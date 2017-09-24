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


public interface IRulesHandle extends IOntologyHandle {
	/**
	 * Returns a String that contains the entire rules in text mode.
	 * @return String containing the rules in text.
	 * throws MediationException to indicate that a String representation of the rules is not 
	 * applicable to this handle.
	 */
	String getRules() throws MediationException;

}
