
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

package com.ibm.dm.frontService.sm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

public abstract class Catalog {
	
	private final static String FILE_NAME_PROPERTY = ".fileName";
	private final static String PROPERTY1 = ".P1";
	private final static String PROPERTY2 = ".P2";
	private final static String PROPERTY3 = ".P3";
	private final static String PROPERTY4 = ".P4";
	private final Properties mProps;
	private final String mName;
	public Catalog(String name) throws IOException {
		mName = name;
		File file = new File("SM.catalog." + name + ".props");
		mProps = new Properties();
		if (false == file.canRead()) {
			file.createNewFile();
			mProps.put("fileName", file.getName());
		}		else {
			mProps.load(new FileInputStream(file));
		}
	}
	
	public void save() throws FileNotFoundException, IOException {
		mProps.store(new FileOutputStream(new File(mProps.getProperty("fileName"))), "SM." + mName + ".catalog");
	}
	public void setFileName(String id, String fileName) {
		mProps.setProperty(id + FILE_NAME_PROPERTY, fileName);
	}
	public String getFileName(String id) {
		return mProps.getProperty(id + FILE_NAME_PROPERTY);
	}
	protected String getP1(String id) {
		return mProps.getProperty(id + PROPERTY1);
	}
	protected String getP2(String id) {
		return mProps.getProperty(id + PROPERTY2);
	}
	protected String getP3(String id) {
		return mProps.getProperty(id + PROPERTY3);
	}
	protected String getP4(String id) {
		return mProps.getProperty(id + PROPERTY4);
	}
	protected void setP1(String id, String val) {
		mProps.setProperty(id + PROPERTY1, val);
	}
	protected void setP2(String id, String val) {
		mProps.setProperty(id + PROPERTY2, val);
	}
	protected void setP3(String id, String val) {
		mProps.setProperty(id + PROPERTY3, val);
	}
	protected void setP4(String id, String val) {
		mProps.setProperty(id + PROPERTY4, val);
	}
	
	public Collection<String> getKeys() {
		Collection<String> result = new ArrayList<String>(); 
		Enumeration<Object> list = mProps.keys();
		while (list.hasMoreElements()) {
			String key = (String)list.nextElement();
			if (key.endsWith(FILE_NAME_PROPERTY)) {
				result.add(key.substring(0, key.length()-FILE_NAME_PROPERTY.length()));
			}
		}
		return result;
	}
	
	public void delete(String id) throws FileNotFoundException, IOException {
		String fileName = mProps.getProperty(id + FILE_NAME_PROPERTY);
		mProps.remove(id + FILE_NAME_PROPERTY);
		mProps.remove(id + PROPERTY1);
		mProps.remove(id + PROPERTY2);
		mProps.remove(id + PROPERTY3);
		new File(fileName).delete();
		save();
	}
}

