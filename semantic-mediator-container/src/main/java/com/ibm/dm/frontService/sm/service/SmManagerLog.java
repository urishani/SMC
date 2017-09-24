
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

package com.ibm.dm.frontService.sm.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * Manages a log of messages to be exchanged among users of the manager as long as it is not instrumented
 * with synchronization mechanism over multiple concurrent users.
 * @author shani
 *
 */
public class SmManagerLog {
	static SmManagerLog smManagerLog = null;
	static public SmManagerLog create(Database owner) {
		if (null == smManagerLog) synchronized (SmManagerLog.class) {
			if (null == smManagerLog)
				smManagerLog = new SmManagerLog(owner);
		}
		return smManagerLog;
	}
	
	static final String FILE_NAME = "SmManager.log";
	static final String ENTRY_FILE_NAME = "SmManager.lastEntry.log";
	final Database mOwner;
	final File folder;
	final File migrationReportFolder;
	File migrationReport;
	final File msgs;
	private SmManagerLog(Database owner) {
		mOwner = owner;
		folder = new File(mOwner.getLogFolder());
		folder.mkdirs();
		migrationReportFolder = new File(folder, "migrations");
		migrationReport = new File(migrationReportFolder, "report.txt");
		msgs = new File(folder, "msgs");
		migrationReportFolder.mkdirs();
		if (false == msgs.canRead()) {
			try {
				msgs.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Saves a migration report after renaming previous one accordingly.<br>
	 * @param msg
	 */
	public synchronized void reportMigration(String msg) {
		renameCurrent();
		try {
			Utils.fileFromBytes(migrationReport, msg.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void renameCurrent() {
		if (false == migrationReport.canRead())
			return;
		long t = migrationReport.lastModified();
		String d = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(t));
		File dest = new File(migrationReportFolder, "report-" + d + ".txt");
		// rename:
		migrationReport.renameTo(dest);
		migrationReport = new File(migrationReportFolder, "report.txt");
	}
	/**
	 * Saves the text parameter as an entry at the end of the file with a date stamp.
	 * @param text String text to be appended in the file.
	 * @return
	 */
	public boolean saveEntry(String text) {
		File logFile = new File(FILE_NAME);
		File logEntryFile = new File(ENTRY_FILE_NAME);
			try {
				if (false == logFile.canWrite())
					logFile.createNewFile();
				if (logEntryFile.canWrite()) 
					logEntryFile.delete();
				logEntryFile.createNewFile();
				FileOutputStream fos = new FileOutputStream(logFile, true);
				String dateLine = ("\n------------- " + (new Date()) + " --------\n");
				String entry = dateLine + text;
				FileOutputStream fos1 = new FileOutputStream(logEntryFile);
				fos.write(entry.getBytes());
				fos1.write(entry.getBytes());
				fos.flush();
				fos.close();
				fos1.flush();
				fos1.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		return true;
	}
	
	/**
	 * Returns the contents of the logs file.
	 * @return
	 */
	public String getLog() {
		File logFile = new File(FILE_NAME);
		try {
			if (false == logFile.canWrite())
				logFile.createNewFile();
			return Utils.stringFromStream(new FileInputStream(logFile));
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	/**
	 * Returns the contents of the last log entry.
	 * @return
	 */
	public String getLogEntry() {
		File logFile = new File(ENTRY_FILE_NAME);
		try {
			if (false == logFile.canWrite())
				logFile.createNewFile();
			return Utils.stringFromStream(new FileInputStream(logFile));
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
}

