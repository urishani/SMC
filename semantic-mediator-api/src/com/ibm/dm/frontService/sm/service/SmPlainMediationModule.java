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
package com.ibm.dm.frontService.sm.service;

import org.apache.jena.rdf.model.Model;

import com.ibm.dm.frontService.sm.intfc.IInputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyLocator;
import com.ibm.dm.frontService.sm.intfc.IOutputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IRulesHandle;
import com.ibm.dm.frontService.sm.intfc.ISmModuleContext;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.intfc.MediationException;

/**
 * A default null mediation module which passes the input model as is to the 
 * output.
 * @author shani
 *
 */
public class SmPlainMediationModule implements ISmModuleIntercept {

	public boolean close() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean created() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean initialized(ISmModuleContext context) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * This implementation assumes that the ontologies are null.
	 */
	public boolean invoke(IOntologyHandle sourceOntology,
			IInputModelHandle sourceModel, IOntologyHandle targetOntology,
			IRulesHandle mediationRules, IOutputModelHandle targetModel)
			throws MediationException {
		Model model = sourceModel.getModel();
		targetModel.setModel(model);
		return false;
	}

	public boolean invoke(IOntologyLocator ontologyLocator,
			IInputModelHandle sourceModel, IOutputModelHandle targetModel)
			throws MediationException {
		return invoke(null, sourceModel, null, null, targetModel);
	}

	public boolean reset() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean tearDown() {
		// TODO Auto-generated method stub
		return false;
	}

}
