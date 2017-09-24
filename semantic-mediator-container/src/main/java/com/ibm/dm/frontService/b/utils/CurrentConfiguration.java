
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

package com.ibm.dm.frontService.b.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

public class CurrentConfiguration
{
    static final public String PROPERTIES_FILE = "d:/danse/configuration.ini";
    private Properties properties      = new Properties();

    private CurrentConfiguration()
    {
        File file = new File(PROPERTIES_FILE);
        try
        {
        	ByteSource bs = Files.asByteSource(file);
        	InputStream is = bs.openBufferedStream();
            getProperties().loadFromXML(is);
        }
        catch (InvalidPropertiesFormatException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static CurrentConfiguration instance = null;

    public static CurrentConfiguration get()
    {
        if (instance == null)
        {
            instance = new CurrentConfiguration();
        }
        return instance;
    }

    public static void createProperties()
    {
        Properties p = new Properties();
        p.setProperty(ServerWorkDir, "d:/danse/serverWorkDir/");
        p.setProperty(UploadTempDir, "d:/danse/upload/");
        p.setProperty(DatastorePersistenceFile, "d:/danse/datastore/datastore.xml");

        File file = new File(PROPERTIES_FILE);
        try
        {
            Files.createParentDirs(file);
            OutputStream os = Files.asByteSink(file).openStream();
            p.storeToXML(os, "Those are configuration values that control Architect cockpit Optimization clien and hell knows what else");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static final String ServerWorkDir             = "ServerWorkDir";
    public static final String UploadTempDir             = "UploadTempDir";

    public static final String DatastorePersistenceFile  = "DatastorePersistenceFile";
    public String getServerWorkDir()
    {
        return getProperties().getProperty(ServerWorkDir);
        //return "d:/danse/serverWorkDir/";
    }

    public String getUploadTempDir()
    {
        return getProperties().getProperty(UploadTempDir);
        //return "d:/danse/upload/";
}

    public static final String DATASTORE_PATH          = "c:/danse/datastore/datastore.xml";
    /**
     * Allows loading of other properties that can be in the underlying properties files
     * call forwarded to Properties.getProperty(key);
     * */
    public String getProperty(String key)
    {
        return getProperties().getProperty(key);
    }

    /**
     * Allows loading of other properties that can be in the underlying properties files
     * call forwarded to Properties.getProperty(key, defaultValue);
     * */
    public String getProperty(String key, String defaultValue) {
        return getProperties().getProperty(key, defaultValue);
    }

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public Properties getProperties() {
		return properties;
	}

}

