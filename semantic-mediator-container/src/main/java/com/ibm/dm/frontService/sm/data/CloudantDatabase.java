
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
package com.ibm.dm.frontService.sm.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.lightcouch.NoDocumentException;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.Response;
import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.dm.frontService.b.utils.U;
import com.ibm.dm.frontService.sm.utils.SMCLogger;

public class CloudantDatabase extends Database implements CloudantItem {

	String _id = "";
	public String getId() { return _id; }
	String _rev = null;
	public String getRev() { return _rev; }
	public void setRev(String rev) {this._rev = rev; }

	
	private static Logger logger = null;
	public static void log(Level level, String msg, Exception...ex) {
        if (null == CloudantDatabase.logger )
        	logger = Logger.getLogger(CloudantDatabase.class.getName());
        if (null != ex && ex.length > 0)
        	logger.log(level,msg,ex[0]);
        else
        	logger.log(level, msg);
	}

	public static boolean createCloudantItem (com.cloudant.client.api.Database cloudantDb, CloudantItem data) {
		Response r = cloudantDb.post(data);
		if (false == Strings.isNullOrEmpty(r.getError())) {
			log(Level.SEVERE, "Error from posting <" + data.getId() + ">: [err=" + r.getError() + "; reason=" + r.getReason() + "]");
			return false;
		} else {
			log(Level.INFO, "Initially creating <" + data.getId() + ">: successful [rev=" + r.getRev() + "].");
			data.setRev(r.getRev());
		}
		return true;
	}
	public static boolean updateCloudantItem (com.cloudant.client.api.Database cloudantDb, CloudantItem data) {
		Response r = cloudantDb.update(data);
		if (false == Strings.isNullOrEmpty(r.getError())) {
			log(Level.SEVERE, "Error from posting <" + data.getId() + ">:<" + data.getRev() + "> - [err=" + r.getError() + "; reason=" + r.getReason() + "]");
			return false;
		} else {
			log(Level.INFO, "Updating creating <" + data.getId() + ">:<" + data.getRev() + "> - successful [rev=" + r.getRev() + "].");
			data.setRev(r.getRev());
		}
		return true;
	}


	private transient com.cloudant.client.api.Database cloudantDb = null;
	private transient boolean needsCommit = false;
	
	public static void needsCommit(Database db, boolean needsCommit) {
		if (null != db && db instanceof CloudantDatabase)
			((CloudantDatabase)db).needsCommit = true;
	}

	public static Database create(HttpServletRequest request) throws Exception  {
		Database db = create_(request);
		return db;
	}
	
	private static Database create_(HttpServletRequest request) throws Exception  {
    	Database db = mDatabase.get(Database.mProject); 
    	if (null != db && db instanceof CloudantDatabase) {
    		return db;
    	}

//    	Database.setHost(request);
		CloudantClient cc = createClient();
		if (null == cc) {
			db = Database.create(request);
			SMCLogger.init(db);
			return db;
		}
		java.util.List<String> ldb = cc.getAllDbs();
		boolean hasDatabase = false;
		System.out.println("Cloudant dbs: ");
		if (null != ldb) {
			Iterator<String> it = ldb.iterator();
			while (it.hasNext()) { 
				String dbn = it.next();
				System.out.println(dbn);
				if (dbn.equalsIgnoreCase(mDbName))
					hasDatabase = true;
			}
		}
		System.out.println("Working with Cloudant db [" + mDbName + "]: " + (hasDatabase?"existing.":"new."));
		com.cloudant.client.api.Database cloudantDb = cc.database(mDbName, true);
		if (hasDatabase) {
			try {cloudantDb.find("database"); // check that the main object is in the database.
			} catch (NoDocumentException e) {
				hasDatabase = false;
			}
		}
		if (! hasDatabase) { // create the database
			db = Database.create(request);
			JsonElement jsonDb = U.getGson().toJsonTree(db);
			JsonObject jsonOb = jsonDb.getAsJsonObject();
			jsonOb.addProperty("_id", "database");
//			jsonOb.addProperty("dbName", mDbName);
			CloudantDatabase cdb = U.getGson().fromJson(jsonOb, CloudantDatabase.class);
//			String cdb_id = cdb._id;
			if (! createCloudantItem(cloudantDb, cdb)) {
				log(Level.WARNING, "Failed to create a Cloudant Database, using file-based system");
				return db;
			}
			else {
//				Database.mDatabase.put(Database.mProject, cdb);
//				Database.setHost(request);
				cdb.finalizeDb();
				SMCLogger.init(cdb);
				cdb.cloudantDb = cloudantDb;
				return cdb;
			}
		} else {
			CloudantDatabase cdb = null;
			try{
				cdb = cloudantDb.find(CloudantDatabase.class, "database");

//				Database.mDatabase.put(Database.mProject, cdb);
//				Database.setHost(request);
				cdb.finalizeDb();
				SMCLogger.init(cdb);
				cdb.cloudantDb = cloudantDb;
	    		try {
	    			String cdbj = cdb.toJson();
	    			System.out.println(cdbj);
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
			} catch (NoDocumentException nde) {
				nde.printStackTrace();
				cdb = null;
			}
			if (null == cdb) {
				log(Level.WARNING, "Failed to get database from the Cloudant repository");
				return Database.create(request);
			} else {
				return cdb;
			}
		}
	}

	public static CloudantClient createClient() {

		// VCAP_SERVICES is a system environment variable
		// Parse it to obtain the  NoSQL DB connection info
		String VCAP_SERVICES = System.getenv("VCAP_SERVICES");
		System.out.println("VCAP_SERVICES: " + VCAP_SERVICES);
		String serviceName = null;
		
		if (Strings.isNullOrEmpty(VCAP_SERVICES)) {
			InputStream csv = 
					CloudantDatabase.class.getResourceAsStream("/VCAP_SERVICES");
			if (null == csv) {
				log(Level.WARNING, "Cannot read resource VCAP_SERVICES");
				return null;
			}
				
			StringBuffer sb = new StringBuffer();

			int c = -1;
			try {
				while ((c = csv.read()) >= 0) {
					sb.append((char)c);
				}
				VCAP_SERVICES = sb.toString();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
    	if (Strings.isNullOrEmpty(VCAP_SERVICES))
    		return null;
    	

    	// parse the VCAP JSON structure
    	JsonObject obj =  (JsonObject) new JsonParser().parse(VCAP_SERVICES);
    	Entry<String, JsonElement> dbEntry = null;
    	Set<Entry<String, JsonElement>> entries = obj.entrySet();
    	// Look for the VCAP key that holds the cloudant no sql db information
    	for (Entry<String, JsonElement> eachEntry : entries) {				
    		if (eachEntry.getKey().equals("cloudantNoSQLDB")) {
    			dbEntry = eachEntry;
    			break;
    		}
    	}
    	if (dbEntry == null) {			
    		log(Level.WARNING, "Could not find cloudantNoSQLDB key in VCAP_SERVICES env variable");
    		return null;
    	}

    	obj =(JsonObject) ((JsonArray)dbEntry.getValue()).get(0);		
    	serviceName = (String)dbEntry.getKey();
    	System.out.println("Service Name - "+serviceName);

    	obj = (JsonObject) obj.get("credentials");

    	String user = obj.get("username").getAsString();
    	String password = obj.get("password").getAsString();
    	String host = obj.get("host").getAsString();
    	
    	if (Strings.isNullOrEmpty(user) || Strings.isNullOrEmpty(password) || Strings.isNullOrEmpty(host)) {
    		log(Level.WARNING, "Credentials are missing for Claudant use: [" + user + "; " + password + "; " + host + "]");
    		return null;
    	}

    	try {
    		return new CloudantClient(user, user, password);
    	}
    	catch(org.lightcouch.CouchDbException e) {
    		log(Level.SEVERE, "Unable to connect to repository", e);
    		return null;
    	}
	}


	public CloudantDatabase() {
		super();
	}

	private static final long serialVersionUID = 1L;

	//	@Override
	boolean store() {
		if (!setCloudantDb()) {
			log(Level.SEVERE, "Cannot save database to Cloudant - cannot get access to database.");
			return false;
		}
		boolean result = updateCloudantItem(cloudantDb, this);
		if (result && needsCommit )
			commit();
		return result;
	}

	private boolean setCloudantDb() {
		if (null == cloudantDb) synchronized (CloudantDatabase.class) {
			if (null == cloudantDb) {
				CloudantClient cc = createClient();
				if (null == cc)
					return false;
				cloudantDb = cc.database(mDbName , false);
			}
		}
		return null != cloudantDb;
	}
	
	public void commit() {
		if (null != cloudantDb)
			cloudantDb.ensureFullCommit();
		
	}

}
