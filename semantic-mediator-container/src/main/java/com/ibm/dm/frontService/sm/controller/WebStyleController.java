
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibm.dm.frontService.sm.utils.Utils;

@Controller
public class WebStyleController extends SmcBaseController {

	@RequestMapping(value="/smWebstyles")
	public void smWebStyles(@RequestParam(value="css", required=false, defaultValue="") String css,
			HttpServletRequest request, HttpServletResponse response) {
    	String text = Utils.loadFromZipInClassPath("templates/css.zip", "smc", "css/" + css);
		System.out.println( "called [/smWebstyles] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
    	Utils.respondWithText(response, text, "text/css");
	}
	

}
