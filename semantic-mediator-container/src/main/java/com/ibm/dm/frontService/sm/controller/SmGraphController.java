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
package com.ibm.dm.frontService.sm.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibm.dm.frontService.sm.service.SmGraphService;

@Controller
public class SmGraphController extends SmcBaseController {

	public SmGraphController() {
		super();
	}
	
	@RequestMapping("/smGraph")//, method = RequestMethod.GET)
	public void smGraphService(@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println( "called [/smGraph] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
		new SmGraphService(getDatabase(request)).get(request, response,
				new String[] { "sm", "graph" });

	}
}