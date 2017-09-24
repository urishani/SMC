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
package com.ibm.haifa.smc.client.oauth;

import org.apache.http.HttpResponse;

public class InvalidUserCredentials extends OAuthCommunicatorException
{
	private static final long serialVersionUID = 1L;

    public InvalidUserCredentials()
    {
        super();
    }

    public InvalidUserCredentials(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InvalidUserCredentials(String message)
    {
        super(message);
    }

    public InvalidUserCredentials(Throwable cause)
    {
        super(cause);
    }

    public InvalidUserCredentials(HttpResponse response)
    {
        super(response);
    }

	public HttpResponse getResponse() {
		return response;
	}

	public void setResponse(HttpResponse response) {
		this.response = response;
	}

	public String getErrorMessage() {
		return "Invalid user and/or password";
	}

	public int getErrorStatus() {
		return -1;
	}
}

