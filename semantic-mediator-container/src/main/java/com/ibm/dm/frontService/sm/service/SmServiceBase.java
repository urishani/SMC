
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
 *
 */

package com.ibm.dm.frontService.sm.service;

//import net.openservices.rio.store.RioServerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.utils.Utils;

public class SmServiceBase implements ISmService{

	public SmServiceBase(Database db) {
		super();
		this.mDatabase = db;
	}

	protected String mUser;
	protected String mProject = "";
//	protected IJfsClient mJfsClient;
	private long mTime;
	private final Database mDatabase;
	
	protected Database getDatabase() {
		return mDatabase;
	}

	/**
	 * To be called on service exit and report total execution time and other
	 * statistics.
	 * @param request
	 * @param response
	 * @param failed boolean to indicate service success.
	 * @throws Exception 
	 */
	void onExit(HttpServletRequest request, HttpServletResponse response, boolean failed, String...msg) throws Exception {
		String m = "no message";
		if (msg.length > 0)
			m = msg[0];
		try {
			getDatabase().save();
		} catch (Exception e) {
			e.printStackTrace();
			m += " + " + e.getMessage();
			throw new Exception(m);
		} finally {
			System.out.format("[%1$tF %1$tR] %2$s. Execution in [%3$.3f seconds]. %4$s.%n",
					new Date(), (failed?"Failed":"Succeeded"), (System.currentTimeMillis() - this.mTime)/1000.0,
					(failed?("Return code [" +	": " + m + "]"):""));
			if (failed)
				throw new Exception(m);
		}
	}

	/**
	 * to be called on service entry, record stating time and report various
	 * service invocation attributes in a standard way.
	 * @param request
	 * @param response
	 */
	void onEntry(HttpServletRequest request, HttpServletResponse response) {
		mUser = request.getRemoteUser();
		System.out.format("SERVICE: [%1$tF %1$tR] Max memory is [%2$.3f GB]. User: [%3$s], %n",
				new Date(), Runtime.getRuntime().maxMemory()/1024.0/1024.0/1024.0, mUser);
		setProject(request);
		System.out.println("mUser: " + mUser + "; mProject: " + mProject);
		//HttpParams parms = request.getParams();
		System.out.format("SERVICE: [%1$tF %1$tR] %2$s. from [%3$s]%n", new Date(), request.getMethod() + ": " + request.getRequestURL() + "?" + request.getQueryString(), request.getRemoteHost());
		this.mTime = System.currentTimeMillis();
	}

	private void setProject(HttpServletRequest request) {
	//	final String PROJECT_ID = "projectId"; //$NON-NLS-1$
	//	mProject = RmpsFrontService.getFirstParameter(request, PROJECT_ID);
//		mProject = RmpsFrontService.getFirstParameter(request, IConstants.PARAMETER_ROOT_RESOURCE);
	//	System.out.println("project1 would be [" + mProject + "]");
	//	mProject = mJfsClient.getConfigurationContext().getProjectUri();
	//	System.out.println("project2 would be [" + mProject + "]");
		mProject = ""; // for now - use only default project area.
	}

	public String inputStreamToString(InputStream is) throws IOException {
		return Utils.stringFromStream(is);
	}

	public String getParam(HttpServletRequest request, String string) {
		return request.getParameter(string);
	}

}


