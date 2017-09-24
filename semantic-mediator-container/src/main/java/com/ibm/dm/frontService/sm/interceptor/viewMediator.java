
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
 
 
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

/**
 * Licensed Material - Property of IBM
 * Copyright IBM  2011 All Rights Reserved
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 */
package com.ibm.dm.frontService.sm.interceptor;

import java.io.IOException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.ReasonerVocabulary;

import com.ibm.dm.frontService.sm.intfc.IInputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IOntologyLocator;
import com.ibm.dm.frontService.sm.intfc.IOutputModelHandle;
import com.ibm.dm.frontService.sm.intfc.IRulesHandle;
import com.ibm.dm.frontService.sm.intfc.ISmModuleContext;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.intfc.MediationException;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * Interceptor class which implements efficient mediation using Fraunhoffer rules.
 * Used to learn and experiment.
 * @author shani
 */
public class viewMediator implements ISmModuleIntercept{
	// TODO Auto-generated method stub

	public boolean invoke(IOntologyLocator ontologyLocator,
			IInputModelHandle sourceModel, IOutputModelHandle targetModel)
	throws MediationException {
		throw new MediationException("invoke with ontologyLocator for " + this.getClass().getName() + " Is not implemented.");
	}

	/**
	 * Assumes that the model has embedded the rules.<br>
	 * The parent(s) of the root according to the rules:containmentRelations rule are returned as a Set.
	 * @param model a Model with the rules in it.
	 * @param rootString a Resource of the root element to follow on.
	 * @return
	 */
	public boolean invoke(IOntologyHandle sourceOntology,
			IInputModelHandle sourceModel, IOntologyHandle targetOntology,
			IRulesHandle mediationRules, IOutputModelHandle targetModel)
	throws MediationException {

		Model fromModel =  sourceModel.getModel();
		Model rulesModel;
		try {
			rulesModel = Utils.modelFromStream(mediationRules.getStream(), null, null);
		} catch (IOException e) {
			throw new MediationException(e.getMessage());		
		}
		
		Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
		reasoner.setParameter(ReasonerVocabulary.PROPsetRDFSLevel,
				ReasonerVocabulary.RDFS_SIMPLE);
		reasoner.bindSchema(rulesModel);
		InfModel inf = ModelFactory.createInfModel(reasoner, fromModel);
		ResIterator iter = inf.listSubjects();
		Model result = ModelFactory.createDefaultModel();
		QueryExecution qexec = null;
		try {
			while (iter.hasNext()) {
				Resource r = iter.next();
				String sparql = "CONSTRUCT { <" + r.getURI() + "> ?p ?o } \n WHERE  { <" + r.getURI() + "> ?p ?o }\n";
				Query query =  QueryFactory.create(sparql);
				try {
					qexec = QueryExecutionFactory.create(query.toString(), inf);
					qexec.execConstruct(result);
				} catch (Exception e) { 
					continue;
				}
			}
		} finally {
			if (null != qexec)
				qexec.close();
		}
        targetModel.setModel(result);
		return false;
	}


//	/**
//	 * Executes a query for resources in a single var select statement<br>
//	 * E.g.., SELECT ?var WHERE { a b c. }, where the var name is provided as a param as
//	 * well as the model.
//	 * @param queryString String of the query as described above.
//	 * @param model Model to apply the query on
//	 * @param var String for the var name.
//	 * @return Set<Resource> of answers to the query.
//	 */
//	private Set<Resource> getResultsOfQuery(String queryString, Model model,
//			String... var) {
//		Query query =  QueryFactory.create(queryString);
//		QueryExecution qexec = null;
//		try {
//			qexec = QueryExecutionFactory.create(query.toString(), model);
//			ResultSet results = qexec.execSelect();
//			Set<Resource> answer = new HashSet<Resource>();
//			while (results.hasNext()) {
//				QuerySolution soltn = results.next();
//				for (int i= 0; i < var.length; i++) {
//					Resource resource = (Resource) soltn.get(var[i]);
//					answer.add(resource);
//				}
//			}
//			return answer;
//		} finally {
//			if (null != qexec)
//				qexec.close();
//		}
//	}


	public boolean close() {
		return false;
	}

	public boolean created() {
		return false;
	}

	public boolean initialized(ISmModuleContext context) {
		return false;
	}


	public boolean reset() {
		return false;
	}

	public boolean tearDown() {
		return false;
	}
}

