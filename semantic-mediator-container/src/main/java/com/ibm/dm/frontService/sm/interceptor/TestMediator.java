
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
package com.ibm.dm.frontService.sm.interceptor;

import com.google.common.base.Strings;
import org.apache.jena.rdf.model.*;
import com.ibm.dm.frontService.sm.intfc.IInputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyLocator;
import com.ibm.dm.frontService.sm.intfc.IOutputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IRulesHandle;
import com.ibm.dm.frontService.sm.intfc.ISmLogger;
import com.ibm.dm.frontService.sm.intfc.ISmModuleContext;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.intfc.MediationException;

public class TestMediator implements ISmModuleIntercept {

/*
	 *+------------------------------------------------------------------------+
	 
	 *| Copyright IBM Corp. 2011, 2013.
	 *|                                                                        |
	 *+------------------------------------------------------------------------+
	 */

	private ISmLogger mLogger = null;
	
	public TestMediator(ISmLogger mLogger) {
		super();
		this.mLogger = mLogger;
	}

	public TestMediator() {
		super();
		// TODO Auto-generated constructor stub
	}

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

	public boolean invoke(IOntologyHandle sourceOntology,
			IInputModelHandle sourceModel, IOntologyHandle targetOntology,
			IRulesHandle mediationRules, IOutputModelHandle targetModel)
			throws MediationException {
//		try {
			startPhase("BuildInputOntologies");
			//Model inOnt = collectOntologies(sourceOntology);
			startPhase("BuildOutputOntologies");
			//Model outOnt = collectOntologies(targetOntology);
			startPhase("BuildRulesOntologies");
			//Model rules = collectOntologies(mediationRules);
			startPhase("GetInputModel");
			Model input = sourceModel.getModel();
			startPhase("GetOutputModel");
			//Model output = targetModel.getModel();
			endPhase("GetOutputModel");
			targetModel.setModel(input); // Do nothing
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return false;
	}

	private void endPhase(String string) {
		startPhase("");
	}

	private String lastPhase = "";
	private long t = 0;
	private void startPhase(String string) {
		if (null == mLogger) {
			if (t > 0 && false == Strings.isNullOrEmpty(string)) {
				long nt = System.currentTimeMillis();
				String duration = ((nt - t + 500)/1000) + " seconds";
				System.out.println("\t phase [" + lastPhase + "] ended in " + duration);
			}
			t = System.currentTimeMillis();
			if (false == Strings.isNullOrEmpty(string))
					System.out.println("Starting phase " + string);
			lastPhase = string;
			return;
		}
		if (false == Strings.isNullOrEmpty(lastPhase)) {
			mLogger.endPhase(lastPhase);
		}
		if (false == Strings.isNullOrEmpty(string))
			mLogger.startPhase(string);
		lastPhase = string;
	}

//	private Model collectOntologies(IOntologyHandle sourceOntology) throws IOException {
//		Model m = ModelFactory.createOntologyModel();
//		Collection<IOntologyHandle> o = new ArrayList<IOntologyHandle>();
//		o.add(sourceOntology);
//		collect(m, o);
//		return m;
//	}

//	private void collect(Model model, Collection<IOntologyHandle> dependencies) throws IOException {
//		if (null == dependencies || dependencies.size() == 0) 
//		  return;
//		for (IOntologyHandle iOntologyHandle : dependencies) {
//			String rdf = iOntologyHandle.getRdf();
//			Model m = Utils.modelFromString(rdf);
//			model.add(m);
//			collect(model, iOntologyHandle.getDependencies());
//		}
//	}

	public boolean invoke(IOntologyLocator ontologyLocator,
			IInputModelHandle sourceModel, IOutputModelHandle targetModel)
			throws MediationException {
		// TODO Auto-generated method stub
		return false;
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
