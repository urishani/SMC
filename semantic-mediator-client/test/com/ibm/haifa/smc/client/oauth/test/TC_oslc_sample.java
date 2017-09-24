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
package com.ibm.haifa.smc.client.oauth.test;

import static org.junit.Assert.fail;
import static com.ibm.haifa.smc.client.oauth.test.DanseOslcSample.*;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.ibm.haifa.smc.client.oauth.OAuthCommunicatorException;

public class TC_oslc_sample
{
    private static final Logger l = Logger.getLogger(TC_oslc_sample.class);

    @Test
    public void test_withNormalHostName() throws Exception
    {
        final String host = "danse.haifa.il.ibm.com";
        try
        {
            String uri = post(host);
            put(uri);
        }
        catch (OAuthCommunicatorException e)
        {
            HttpResponse response = e.getResponse();
            if (response != null) l.trace("entityBody of the responce:\n" + EntityUtils.toString(response.getEntity()));
            e.printStackTrace();
            fail("OAuthCommunicatorException");
        }
    }

    @Test
    public void test_withIPAdress() throws Exception
    {
        final String host = "10.110.40.43";
        try
        {
            String uri = post(host);
            put(uri);
        }
        catch (OAuthCommunicatorException e)
        {
            HttpResponse response = e.getResponse();
            if (response != null) l.trace("entityBody of the responce:\n" + EntityUtils.toString(response.getEntity()));
            e.printStackTrace();
            fail("OAuthCommunicatorException");
        }
    }

}
