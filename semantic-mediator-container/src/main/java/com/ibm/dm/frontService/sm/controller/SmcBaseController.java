
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

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.CloudantDatabase;
import com.ibm.dm.frontService.sm.data.Database;

public class SmcBaseController {
	//static private Logger log = Logger.getLogger(SMCController.class);
	private long mTime = System.currentTimeMillis();
	private Database db = null;

	/**
	 * to be called on service entry, record stating time and report various
	 * service invocation attributes in a standard way.
	 * @param request
	 * @param response
	 * @throws Exception 
	 */
	void onEntry(HttpServletRequest request) throws Exception {
//		mUser = request.getRemoteUser();
		getDatabase(request);

		System.out.format("CONTROLLER: [%1$tF %1$tR] Max memory is [%2$.3f GB].%n", // User: [%3$s], %n",
				new Date(), Runtime.getRuntime().maxMemory()/1024.0/1024.0/1024.0);  //, mUser);
//		setProject(request);
//		System.out.println("mUser: " + mUser + "; mProject: " + mProject);
		//HttpParams parms = request.getParams();
		System.out.format("CONTROLLER: [%1$tF %1$tR] %2$s. from [%3$s]%n", new Date(), request.getMethod() + ": " + request.getRequestURL() + "?" + request.getQueryString(), request.getRemoteHost());
		this.mTime = System.currentTimeMillis();
	}
	/**
	 * To be called on service exit and report total execution time and other
	 * statistics.
	 * 
	 * @param request
	 * @param response
	 * @param failed  boolean to indicate service success.
	 * @param commit boolean to indicate that a commit is required at the end of this action
	 * pending that the database has indeed changed and that it is an RDB system.
	 * @throws Exception
	 */

	void onExit(HttpServletRequest request,
			boolean failed, String msg, boolean commit) throws Exception {
		String m = "no message";
		if (false == Strings.isNullOrEmpty(msg))
			m = msg;
		try {
			Database db = getDatabase(request);
			if (null != db)
				db.save();
		} catch (Exception e) {
			e.printStackTrace();
			m += " + " + e.getMessage();
			throw new Exception(m);
		} finally {
			System.out
					.format("[%1$tF %1$tR] %2$s. Execution in [%3$.3f seconds]. %4$s.%n",
							new Date(), (failed ? "Failed" : "Succeeded"),
							(System.currentTimeMillis() - this.mTime) / 1000.0,
							(failed ? ("Return code [" + ": " + m + "]") : ""));
			if (commit && getDatabase(null) instanceof CloudantDatabase) {
				CloudantDatabase cdb = (CloudantDatabase)getDatabase(null);
				cdb.commit();
			}
			if (failed)
				throw new Exception(m);
		}
	}

	protected Database getDatabase(HttpServletRequest request) throws Exception {
		// TODO Auto-generated method stub
		if (null == request)
			return db;
		if (null == db)
			synchronized (CloudantDatabase.class) {
				if (null == db)
					db = CloudantDatabase.create(request);
			}
		return db;
	}



}
