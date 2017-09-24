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

/**
 * Interface for a semantic mediation module.
 * @author shani
 *
 */
public interface ISmModuleIntercept {
	/**
	 * An optional initialization with a parameter that can be passed to the invokation incident of 
	 * this mediator.
	 * <p>Yet - this is a late addition, and will not be part of the interface until we move to Java 8
	 * and provide a backward compatibility through a default empty implementation of this method.
	 * <p>Therefore this method is commented.
	 * @param param
	 * @return
	 */
	//boolean init(String[] param);
	
	/**
	 * Called by the interception container once a new module is created. For future use 
	 * when a pool of modules is maintained by the container. Should be called once
	 * per the lifetime of the module.
	 * @return boolean to indicate that all creation-related processing are completed
	 * in case a chain of agents may be implemented by the container. A true value indicates
	 * that no further creation-related work is needed.
	 */
	boolean created();
	/**
	 * Called by the interception container to initialize the module with specific
	 * context information so that the module can initialize various states.<br>
	 * This may be called many times during the life-time of a modul whenever there are 
	 * changes in the context.
	 * @param context a reference to a context structure TBD.
	 * @return boolean to indicate that all initialization-related processing are completed
	 * in case a chain of activities may be implemented by the container. A true value indicates
	 * that no further initialization-related work is needed.
	 */
	boolean initialized(ISmModuleContext context);
	/**
	 * Resets state of the module to an initialization type of state. 
	 * @return boolean to indicate that all resetting-related processing are completed
	 * in case a chain of activities may be implemented by the container. A true value indicates
	 * that no further resetting-related work is needed.
	 */
	boolean reset();

	/**
	 * Answers true if all mediation has been completed by this method execution. 
	 * A false result will mean that a chain of invocations may be applied by the 
	 * container in case such policy is implemented. 
	 * @param sourceOntology OntologyHandle for the input (source) ontology model.
	 * @param sourceModel ModelHandle for the (input) Model to be mediated.
	 * @param targetOntology OntologyHandle for the output (target) ontology model.
	 * @param mediationRules OntologyHandle for the ontology containing rules to govern the mediation.
	 * @param targetModel ModelHandle for the (output) Model of the mediated input model.
	 * @return boolean to indicate if (true) further processing is required in a chain of mediators (in case the 
	 * container implements such an option.
	 * @throws MediationException
	 */
	boolean invoke(IOntologyHandle sourceOntology, IInputModelHandle sourceModel, IOntologyHandle targetOntology,
			IRulesHandle mediationRules, IOutputModelHandle targetModel) throws MediationException;
	/**
	 * 
	 * @param ontologyLocator an OntologyLocator is a facility to locate ontologies in a repository that is 
	 * shared among mediators to facilitate reuse or computing resources and improve overall efficiency of the 
	 * container. 
	 * @param sourceModel ModelHandle for the (input) Model to be mediated.
	 * @param targetModel ModelHandle for the (output) Model of the mediated input model.
	 * @return boolean to indicate if (true) further processing is required in a chain of mediators (in case the 
	 * container implements such an option.
	 * @throws MediationException
	 */
	boolean invoke(IOntologyLocator ontologyLocator, IInputModelHandle sourceModel, IOutputModelHandle targetModel) throws MediationException;
	/**
	 * Kills the resources used by this module in case the container implements a pool of
	 * modules so that this module can release all its allocated resources.
	 * <br>
	 * Can be called only once at the end of the lifetime of a module.
	 * @return boolean to indicate that all tear-down-related activities are done in case the 
	 * interception container implements a chain of activities.
	 */
	boolean tearDown();
	/**
	 * Resets the module back to an inactive state that needs to be initialized before use.
	 * <br>
	 * Can be called multiple times during lifetime of a module.
	 * @return boolean to indicate that all close-related activities are done in case the 
	 * interception container implements a chain of activities.
	 */
	boolean close();

}
