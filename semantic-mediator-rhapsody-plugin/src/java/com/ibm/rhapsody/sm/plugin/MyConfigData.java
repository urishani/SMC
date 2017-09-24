
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class MyConfigData extends Properties {
	private static final long serialVersionUID = 1L;
	protected final static String CFG_PROPERTIES_FILE_NAME = "conf.props"; 
	protected final static String SII_SYSTEM_URI_PROPERTY = "SII_SYSTEM_URI";
	protected final static String RDF_SERVER_PROPERTY = "RDF_SERVER";
	protected final static String RDF_CLEANING_PROPERTY = "RDF_CLEANER";

	private File myFile;

	public MyConfigData(File dirPath) {
		String fileName = dirPath + File.separator + CFG_PROPERTIES_FILE_NAME;
		myFile = new File(fileName);
		if (myFile.canRead())
			try {
				load(new FileInputStream(myFile));
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
	}

	public String getRdfServerUrl() {
		return getProperty(RDF_SERVER_PROPERTY);
	}
	public void setRdfServerUrl(String rdfServerUri) {
		setProperty(RDF_SERVER_PROPERTY, rdfServerUri);
	}
	public String getSiiSystemUrl() {
		return getProperty(SII_SYSTEM_URI_PROPERTY);
	}
	public void setSiiSystemUrl(String siiSystemUrl) {
		setProperty(SII_SYSTEM_URI_PROPERTY, siiSystemUrl);
	}
	

	@Override
	public Object setProperty(String key, String value) {
		Object o = super.setProperty(key, value);
		try {
			this.store(new FileOutputStream(myFile), "Configuration data for SMC GUI");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return o;
	}
}
