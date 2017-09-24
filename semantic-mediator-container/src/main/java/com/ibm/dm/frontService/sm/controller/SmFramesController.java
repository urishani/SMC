
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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.utils.Utils;

@Controller
public class SmFramesController extends SmcBaseController {

	@RequestMapping(value="/smframes", method=RequestMethod.GET, produces="text/html")
	public String smFrames( HttpServletRequest request) throws Exception {
        String template = Utils.getHtmlTemplate("/templates/frames.html");
        getDatabase(request);
        template = Utils.replaceAll(template, "_project_", Database.getName());

		System.out.println( "called [/smframes] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
    	return template;
	}
	

}
