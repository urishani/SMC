
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
 *|                                   |
 *| Copyright IBM Corp. 2011-2013.
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

/**
 * Licensed Material - Property of IBM
 * Copyright IBM  2013 All Rights Reserved
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 *
 */

package com.ibm.rhapsody.sm.plugin;
import com.ibm.haifa.smc.client.oauth.IUserCredentials;


public class UserCreds implements IUserCredentials {
	private final String user;
	private final String pwd;
	private final boolean auth;
	public UserCreds( String user, String pwd, boolean auth ) {
		this.user = user;
		this.pwd = pwd;
		this.auth = auth;
	}

	public String getPassword() {
		return pwd;
	}

	public String getUserId() {
		return user;
	}
	
	public boolean doAuth() { return auth; }
}

