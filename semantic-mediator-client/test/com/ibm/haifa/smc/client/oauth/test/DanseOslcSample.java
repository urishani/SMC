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

import java.io.StringReader;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import com.google.common.base.Charsets;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC_10;
import com.ibm.haifa.smc.client.oauth.IUserCredentials;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicator;

public class DanseOslcSample
{

    public static void main(String[] args) throws Exception
    {
        final String host = "10.110.40.43";
        //final String host = "danse.haifa.il.ibm.com";

        String uri = post(host);
        put(uri);
    }

    /**
     * Creates an RDF model with one dummy resource and exports is into port oslc_sample_post
     *
     * @return The URI of the new resource at the oslc_sample repository
     * @throws Exception
     */
    public static String post(final String host) throws Exception
    {
        /* Create a model with some dummy resource */
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dc", DC_10.getURI());
        Resource res = model.createResource("http://dummy.resource");
        res.addProperty(DC_10.title, model.createLiteral("Ariel Landau"));
        res.addProperty(DC_10.description, model.createLiteral("A dummy resource, created by Ariel Landau"));

        /* Write out the model into a string (here I've chosen the rdf+xml format, but other options are equally valid) */
        String modelAsRdfXml = RDFUtils.writeModel(model);
        System.out.println("MODEL TO BE EXPORTED: ");
        System.out.println();
        System.out.println(modelAsRdfXml);

        /* Post it into the oslc_sample_post port */
        HttpPost post = new HttpPost("https://" + host + ":9444/dm/sm/tool/oslc_sample_post");
        post.setEntity(new StringEntity(modelAsRdfXml));
        post.setHeader("Content-Type", "application/rdf+xml");
        HttpResponse resp = comm.execute(post);

        System.out.println("RESPONSE STATUS: ");
        System.out.println(resp.getStatusLine());
        System.out.println();

        String responseContent = EntityUtils.toString(resp.getEntity(), Charsets.UTF_8);
        Scanner content = new Scanner(responseContent);
        String localUri = content.next(), remoteUri = content.next();
        System.out.println("LOCAL RESOURCE URI: " + localUri);
        System.out.println("REMOTE RESOURCE URI: " + remoteUri);

        System.out.println("--------------------------------------");
        System.out.println();

        return remoteUri;
    }

    public static void put(String uri) throws Exception
    {
        /* Get the resource with the given URI */
        HttpGet get = new HttpGet(uri);
        get.setHeader("Accept", "application/rdf+xml");
        HttpResponse resp = comm.execute(get);
        String content = EntityUtils.toString(resp.getEntity(), Charsets.UTF_8);

        System.out.println("GET RESPONSE STATUS: ");
        System.out.println(resp.getStatusLine());
        System.out.println();

        System.out.println("RETRIEVED MODEL: ");
        System.out.println();
        System.out.println(content);

        /* Read the retrieved content into a model object */
        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(content), null, "RDF/XML");

        /* Modify the resource */
        Resource res = model.getResource(uri);
        res.removeAll(DC_10.title);
        res.addProperty(DC_10.title, model.createLiteral("Daniel Sojka"));
        res.removeAll(DC_10.description);
        res.addProperty(DC_10.description, model.createLiteral("A dummy resource, modified by Daniel Sojka"));

        /* Write out the modified model into a string */
        String modelAsRdfXml = RDFUtils.writeModel(model);
        System.out.println("UPDATED MODEL: ");
        System.out.println();
        System.out.println(modelAsRdfXml);

        /* Put the modified model */
        HttpPut put = new HttpPut(uri);
        put.setHeader("Content-Type", "application/rdf+xml");
        put.setEntity(new StringEntity(modelAsRdfXml));
        resp = comm.execute(put);

        System.out.println("PUT RESPONSE STATUS: ");
        System.out.println(resp.getStatusLine());
        System.out.println();

        System.out.println("PUT RESPONSE'S CONTENT: ");
        System.out.println(EntityUtils.toString(resp.getEntity(), Charsets.UTF_8));
        System.out.println("--------------------------------------");
        System.out.println();
    }

    private static final OAuthCommunicator comm = getCommunicator();

    public static OAuthCommunicator getCommunicator()
    {
        try
        {
            return new OAuthCommunicator(new IUserCredentials() {
                public String getUserId()
                {
                    return "test";
                }

                public String getPassword()
                {
                    return "test";
                }
            });
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }
}
