
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

import java.io.IOException;
import java.io.StringReader;

import org.apache.jena.rdf.model.*;

//import com.ibm.dm.frontService.sm.intfc.ASmModuleIntercept;
import com.ibm.dm.frontService.sm.intfc.*;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;
import com.ibm.haifa.sm.mediator.Mediator;
import com.ibm.haifa.sm.mediator.MediatorContext;

public class HaifaSemanticMediator implements ISmModuleIntercept { // extends ASmModuleIntercept {

	public boolean requiresLicense() {
		return true;
	}
	
	//@Override
	public String licenseText() {
		return "This mediator is an IBM (TM) 2013 copyright (R), and is provided by IBM for parties working under a valid agreement with IBM.\n It is used \"AS-IS\" w/no liabilities from IBM. ";
	}

	//@Override
	public String description() {
		return "An ontology-and-rule-based mediator using the mediation rules originated by Fraunhofer/Fokus, and extended by IBM. This implementation is a totally new and efficient execution of these rules.";
	}

	
	//@Override
	public String name() {
		return "IBM Haifa Semantic Mediator(R)";
	}

	public boolean created() {
		// TODO Auto-generated method stub
		return false;
	}

	private MediatorContext context = null;
	public boolean initialized(ISmModuleContext context) {
		// TODO Auto-generated method stub
		this.context = (MediatorContext) context;
		return false;
	}

	public boolean reset() {
		// TODO Auto-generated method stub
		return false;
	}

	private Model toModel(IOntologyHandle h) {
		Model m = ModelFactory.createDefaultModel();
		m.read(new StringReader(h.getRdf()), null);
		return m;
	}

	public boolean invoke(IOntologyHandle sourceOntology,
			IInputModelHandle sourceModel, IOntologyHandle targetOntology,
			IRulesHandle mediationRules, IOutputModelHandle targetModel)
			throws MediationException {
		Model inputOntology = toModel(sourceOntology);
		Model outputOntology = toModel(targetOntology);
		Model rules = toModel(mediationRules);
		Mediator mediator = new Mediator(inputOntology, outputOntology, rules);

		Model inputModel = sourceModel.getModel();
		Model outputModel = mediator.mediate(inputModel);
		targetModel.setModel(outputModel);
		String mediationRulesInTurtle = "";
		try {
			mediationRulesInTurtle = Utils.convertModelSyntax(mediationRules.getRdf(), IConstants.RDF_XML, IConstants.TURTLE);
		} catch (IOException e) {
			e.printStackTrace();
			mediationRulesInTurtle = "Could not convert to turtle format. Orioginal RDF:\n" + mediationRules.getRdf();
		}
		String trace = "---------------------------\nRules execution trace:\n---------------------------\n" + 
				mediator.getMediationTrace() + 
				"\n---------------------------\nMediation rules:\n---------------------------\n" + 
				mediationRulesInTurtle;
		context.setMediationTrace(trace);

		return false;
	}

	public boolean invoke(IOntologyLocator ontologyLocator,
			IInputModelHandle sourceModel, IOutputModelHandle targetModel)
			throws MediationException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean tearDown() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean close() {
		// TODO Auto-generated method stub
		return false;
	}

}
