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

package com.ibm.dm.frontService.sm.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.utils.Utils;

public class OSLCService extends SmServiceBase {

//	private final Map<String, String> params;

	public OSLCService(//Map<String, String> params, 
			Database db) {
		super(db);
//		this.params = params;
	}

	
	public void getOslc(HttpServletRequest request, HttpServletResponse response, String segments[]) throws Exception
	{
		onEntry(request, response);
		boolean failed = false;
		//		RmpsFrontService.checkAcceptHeader(request, defaultContentType, IConstants.acceptedContentTypeGroups)
		//String segments[] = RmpsFrontService.getServiceSegments(request);
		String path = Utils.concat(segments, "/");
		String msg = "";
		try {
			new OSLCHelper(getDatabase()).responseForServiceCatalog(path, segments, request, response, this);
		} catch (Exception e) {
			e.printStackTrace();
			failed = true;
			msg = e.getMessage();
			throw e;
	    } finally {
			onExit(request, response, failed, msg);
		}
	}

}


