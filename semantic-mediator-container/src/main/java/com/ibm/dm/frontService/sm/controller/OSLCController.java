
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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.dm.frontService.sm.service.OSLCService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@Api(value = "OSLC-API", tags="API")
public class OSLCController extends SmcBaseController {

		public OSLCController() {
			super();
		}

	    @ApiOperation(value = "Get the Oslc Catalogue for this platform", notes="Start with the catalog to get info on all service providers and capabilities of the platform", nickname = "getCatalog")
		@RequestMapping(value="/sm/oslc_am", method = RequestMethod.GET, produces = { "text/turtle", "text/html" })
	    @ApiResponses(value = { 
	            @ApiResponse(code = 200, message = "Success", response = String.class)})
	    public void getOslc(//@RequestParam() Map<String, String> params,
				HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			System.out.println( "called [/sm/oslc_am] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
			new OSLCService(getDatabase(request))
				.getOslc(request, response,	new String[] { "sm", "oslc_am" });
		}


}
