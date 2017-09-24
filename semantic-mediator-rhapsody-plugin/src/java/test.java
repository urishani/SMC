
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import com.google.common.net.MediaType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicator;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicatorException;
import com.ibm.rhapsody.sm.plugin.UserCreds;


public class test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			multipart("https://localhost:9444/dm/sm/repository/attachments");
		modelFromFile("C:\\Users\\shani\\AppData\\Local\\Temp\\BasicImportExportTestWithPorts709827903839621934.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static Model modelFromFile(String inputFileName) {
		// use the FileManager to find the input file
		InputStream in = FileManager.get().open( inputFileName );
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + inputFileName + " not found");
		}

		return modelFromStream(in);
	}

	static void multipart(String server) throws ClientProtocolException, IOException, OAuthCommunicatorException {

//		FileBody bin = new FileBody(new File(fileName));

		MultipartEntity reqEntity = new MultipartEntity();
//		reqEntity.addPart("bin", bin);
		reqEntity.addPart("description", new StringBody("description of the attachment."));
		reqEntity.addPart("title", new StringBody("title"));
		reqEntity.addPart("hasSmResource", new StringBody("https://shani1-tp.haifa.ibm.com:9444/dm/sm/repository/attachments/resource/0006"));
		boolean doPlainBytes = false;
		if (doPlainBytes) { // plain XML text
			reqEntity.addPart("set", new StringBody("SmBlob")); // One of the sets of attachments: SmBlob, SmFMU, SmStateChartXMI, SmPicturem, SmMovie.
			String mediaType = MediaType.XML_UTF_8.toString();
			ByteArrayBody contents = new ByteArrayBody("<xml> </xml>".getBytes(), mediaType, "no fileame");
			reqEntity.addPart("attachment-data", contents);
		} else { // image from a file
			reqEntity.addPart("set", new StringBody("SmPicture")); // One of the sets of attachments: SmBlob, SmFMU, SmStateChartXMI, SmPicturem, SmMovie.
			File png = new File("data/TowBotSimpleExample.png");
			FileBody contents = new FileBody(png, MediaType.PNG.toString());
			reqEntity.addPart("attachment-file", contents);
		}
		HttpPost httppost = new HttpPost(server);
		httppost.setEntity(reqEntity);

		OAuthCommunicator conn = new OAuthCommunicator(new UserCreds("test", "test", false));
		HttpResponse response = conn.execute(httppost);
		if (response.getStatusLine().getStatusCode() != 200) {
			System.err.println("Failed call [" + response.getStatusLine().getReasonPhrase() + "]");
			return; }
		HttpEntity resEntity = response.getEntity();
		InputStream is = resEntity.getContent();
		byte[] url = new byte[(int) resEntity.getContentLength()];
		is.read(url); // assume it is small to be read in one shot.
		System.out.println("Returned URL: [" + new String(url) + "]");
	}

	public static Model modelFromStream(InputStream in) {
		// create an empty model
		Model model = ModelFactory.createDefaultModel();

		// read the RDF/XML file
		model.read(in, null);
		return model;
	}



}
