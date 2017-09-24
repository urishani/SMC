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

/**
 * The work leading to these results have received funding from the Seventh Framework Programme
 * SPRINT ICT-2009.1.3 Project Number: 257909
 */
package com.ibm.dm.frontService.sm.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.protocol.HTTP;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.rdf.model.Model;
import org.springframework.core.io.support.EncodedResource;
//import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.data.Mediator;
import com.ibm.dm.frontService.sm.data.RuleSet;
import com.ibm.dm.frontService.sm.intfc.IOntologyHandle;
import com.ibm.dm.frontService.sm.intfc.IRulesHandle;
import com.ibm.dm.frontService.sm.intfc.ISmModuleIntercept;
import com.ibm.dm.frontService.sm.intfc.imp.InputModelHandle;
import com.ibm.dm.frontService.sm.intfc.imp.OutputModelHandle;
import com.ibm.dm.frontService.sm.service.SmContainer;
import com.ibm.dm.frontService.sm.service.SmManager;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;
import com.ibm.haifa.mediator.base.Rules;
import com.ibm.haifa.sm.mediator.MediatorContext;

import io.swagger.annotations.*;



@RestController
@Api(value = "Mediation-Api", tags="API") //description="A long description")
public class ApiController  extends SmcBaseController {

	public enum Goal {
		MEDIATE ("mediate", "An input model is mediated to an output model which is returned as a result of the API call"),
		DIAGNOSE ("diagnose", "The mediation of an input model producdes logging info that is produced as a result of this call.");
		
		private final String key;
		private final String description;
		
		Goal(String key, String description) {
			this.key = key;
			this.description = description;
		}
		
		private String key() { return key; }
		private String description() { return description; }
				
	}
	
	public ApiController() {
		super();
	}
 
	/*@RequestMapping(value="/smApi", method = RequestMethod.GET, produces = { "text/html"})
    @ApiResponse(code = 200, message = "Success", response = String.class)
	@ApiOperation(value = "Invoke Mediator through a GUI form", notes = "With an HTML forum, an RDF model is posted, mediated and shows the results visually.")
    @ApiImplicitParams({
		@ApiImplicitParam(name = "mediatorId", value = "Mediator identification via its ID which will be used to mediate an RDF model", required = true, dataType = "string", paramType = "query") 
    })
	public void smApiGet(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String mediatorId) 
			{ //throws Exception {
		try {
		Database database = getDatabase(request);
		SmManager sm = new SmManager(database);
		String msg = sm.testMediator(response, mediatorId, database);
		if (null == msg || "".equals(msg.trim())) 
			return;
		Utils.respondWithText(response, "Test mediator request failed with message [" + msg + "].");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
    }
*/
	 
	@RequestMapping(value="/smApi", method = RequestMethod.POST, 
			consumes = { IConstants.TURTLE, IConstants.RDF_XML, IConstants.RDF_XML + "-ABBREV", IConstants.N_TRIPLE, IConstants.N3 }, 
			produces = { IConstants.TURTLE, IConstants.RDF_XML, IConstants.RDF_XML + "-ABBREV", IConstants.N_TRIPLE, IConstants.N3, IConstants.PLAIN_TEXT_TYPE, IConstants.HTML })
    @ApiResponses(value = { 
            @ApiResponse(code = 200, message = "Success", response = String.class)})
	@ApiOperation(value = "Invoke Mediator through a POST API call", 
		notes = "Must provide an RDF which used the proper ontology, and returns a mediated RDF model which is according to the mediator output ontology.")
    @ApiImplicitParams({
		@ApiImplicitParam(name = "rulesId", value = "RuleSet identification via its ID which will be used to mediate an RDF model", required = true, dataType = "string", paramType = "query"), 
		@ApiImplicitParam(name = "rdf", value = "RDF model data to be mediated", required = true), //, dataType = "string", paramType = "body"),
		@ApiImplicitParam(name = "inverse", value = "Indicates to activate the mediation in the inerse direction", required = false, dataType = "boolean", paramType = "query"),
		@ApiImplicitParam(name = "goal", value = "Indicates the goal of this activation with possible values being 'mediate' or 'diagnose', "
				+ "with default being 'mediate'. "
				+ "'diagnose' can only return html or text results which are self explanatory.", 
				required = false, dataType = "Goal", paramType = "query",
				defaultValue="MEDIATE",
				allowableValues= "MEDIATE, DIAGNOSE" )
    })
    public void smApiPost(HttpServletRequest request, HttpServletResponse response,
    		    @RequestBody(required= true) String rdf,
      		@RequestParam (name= "rulesId") String rulesId,
    			@RequestParam (name="inverse") boolean inverse,
    			@RequestParam (name="goal", required=false) String goal) {
		try {
			doSmApiPost(request, response, rdf, rulesId, inverse, goal);
		} catch (Exception e) {
			Utils.responseForOslcError(null, e, response, request);
		}
	}
	private void doSmApiPost(HttpServletRequest request, HttpServletResponse response, String rdf, String rulesId, boolean inverse, String goal)
      		throws Exception {
				Database database = getDatabase(request);
				String ct = request.getContentType();
				RuleSet rules = database.getRuleSet(rulesId);
				if (null == rules) {
					throw new Exception("Rules [" + rulesId + "]could not be found");
				}
				Goal theGoal = Goal.valueOf(goal);
				if (false == Strings.isNullOrEmpty(goal ) && null == theGoal)
					throw new Exception("goal must be one of 'mediate' or 'diagnose' with default being 'mediate'.");
				if (null == theGoal) 
					theGoal = Goal.MEDIATE;
				switch (theGoal) {
				case MEDIATE: if (null == Utils.formatFromAcceptType4Jena(request))
					throw new Exception("Accept type is not legal for an RDF dataset response");
					break;
				case DIAGNOSE: if (false == Utils.willAccept(IConstants.HTML, request) && false == Utils.willAccept(IConstants.PLAIN_TEXT_TYPE, request))
					throw new Exception("Accept type is not legal for TRACE or DIAGNOSE result");
					break;
				default: throw new Exception("Goal can only be one of 'mediate' or 'diagnose'");
				}
				
				if (false == rules.isReady()) {
					throw new Exception("Rules [" + rulesId + "] is not ready for activation yet.");
				}
				boolean reversible = rules.getField(RuleSet.IFields.REVERSIBLE).equals("Yes");
				if (inverse && !reversible) {
					throw new Exception("Rules [" + rulesId + "] is not reversible.");
				}
				Model sourceM = Utils.modelFromString(rdf, ct, null);
				if (null == sourceM ) {
					throw new Exception("Input model failed to parse as an RDF with content type [" + ct + "]");
				}
				OutputModelHandle targetModel = new OutputModelHandle();
				InputModelHandle sourceModel = new InputModelHandle(sourceM);

				MediatorContext context = new MediatorContext();

				try {
					String interceptorClassName = rules.getInterceptorClass();
					System.out.println("interceptorClassName [" + interceptorClassName + "]");
					ISmModuleIntercept interceptor = rules.getInterceptor();
					interceptor.initialized(context); //(goal == Goal.DIAGNOSE) ? context : null ) ;

					RuleSet.END_POINT inputEP = inverse? RuleSet.END_POINT.SECOND : RuleSet.END_POINT.FIRST;
					RuleSet.END_POINT outputEP = inverse? RuleSet.END_POINT.FIRST : RuleSet.END_POINT.SECOND;
					IOntologyHandle inputOntology = SmContainer.getOntologyHandle(rules.getEndPointOntology(inputEP)) ;
					IOntologyHandle outputOntology = SmContainer.getOntologyHandle(rules.getEndPointOntology(outputEP));
					IRulesHandle ruleSet = (IRulesHandle) SmContainer.getOntologyHandle(rules);
					interceptor.invoke(inputOntology, sourceModel, outputOntology, ruleSet, targetModel);
					if (theGoal == Goal.DIAGNOSE) {
						Utils.respondWithText(response, "Diagnostics trace for the mediation process:\n" + context.getMediationTrace());
						return;
					}
					Model targetM = targetModel.getModel();
					String result = Utils.modelToText(targetM, Utils.formatFromAcceptType4Jena(request));
					Utils.respondWithText(response, result, request.getHeader(HttpHeaders.CONTENT_TYPE));
				} catch(Exception e) {
					//mediationPhase.ended("Failed [" + e.getClass() + ": " + e.getMessage() + "]");
					//currentTask.ended("Failed");
					throw e;
				}
				//System.out.println(m.toString());
    }

}
