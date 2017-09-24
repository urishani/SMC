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

public interface IUserCredentials
    {
        public String getUserId();

        public String getPassword();
        
        public boolean doAuth();

        public static final class DefaultUserCredentials implements IUserCredentials
        {
            private final String userId;
            private final String password;
            private final boolean auth;

            public DefaultUserCredentials(String username, String password, boolean auth)
            {
                super();
                this.userId = username;
                this.password = password;
                this.auth = auth;
            }

            @Override
            public String getUserId()
            {
                return userId;
            }

            @Override
            public String getPassword()
            {
                return password;
            }
            
            @Override
            public boolean doAuth() 
            {
            	return auth;
            }
        }
    }

