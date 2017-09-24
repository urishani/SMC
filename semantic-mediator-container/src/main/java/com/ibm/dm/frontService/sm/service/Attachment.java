
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
package com.ibm.dm.frontService.sm.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.utils.Utils;

/**
 * Represents an attachment (blob) on the disk or other DM means later on.
 * @author shani
 *
 */
public class Attachment {

	private static final String SM_ATTACHMENTS_FOLDER = "attachments";
	private static final File mAttachmentsFolder = new File(Database.SM_MODEL_FOLDER, SM_ATTACHMENTS_FOLDER);
	static {
		mAttachmentsFolder.mkdirs();
	}

	/**
	 * Answers with the File for the folder holding blobs.
	 * @return File of existing folder.
	 */
	public static File getAttachmentsFolder() {
		return mAttachmentsFolder;
	}
	
	private final File file;
//	private final String mSet;
//	private final String mNum;
	private final String mId;
	private final static String PREFIX = "blob.";
	private final static String SUFFIX = ".dat";
	private Attachment(String set, String num) throws IOException {
//		this.mSet = set;
//		this.mNum = num;
		this.mId = set + "/resource/" + num;
		File setFolder = new File(mAttachmentsFolder, set);
		setFolder.mkdirs();
		file = new File(setFolder, PREFIX + num + SUFFIX);
	} // dot use new on this one.
	
	public boolean delete() {
		boolean done = false;
		if (file.canRead())
			done = file.delete();
		if (done)
			System.out.println("Deleting attachment [" + file + "] - " + done);
		else 
			return false;
		File parent = file.getParentFile();
		if (parent.exists() && parent.list().length <= 0) {
			done = parent.delete();
			if (done)
				System.out.println("\t Deleting also folder [" + parent + "] - " + done);
		}
		return true;
	}
	/**
	 * Answers with a temp file in the folder of the blobs that can be filled with
	 * data and than moved (renamed) to the final file of the blob. 
	 * @return File to be written with contents.
	 * @throws IOException
	 */
	public static File createTemp() throws IOException {
		return File.createTempFile("temp-" + PREFIX, SUFFIX, mAttachmentsFolder);
	}
	public static Attachment create(String set, String num) throws IOException {
		Attachment attachment = new Attachment(set, num);
		return attachment;
	}


	public byte[] get() throws FileNotFoundException, IOException {
			return Utils.bytesFromFile(file);
	}
	
	public boolean save(List<byte[]> content) throws IOException {
		File tmp = new File(file.getAbsolutePath() + ".tmp");
		File sav = new File(file.getAbsolutePath() + ".sav");
		Utils.fileFromBuffList(tmp, content);
		sav.delete();
		file.renameTo(sav);
		boolean done = tmp.renameTo(file);
		if (done) 
			sav.delete();
		else
			sav.renameTo(file);
		return done;
	}

	/**
	 * Saves content in a file which can be renamed.
	 * @param content File containng content. If renaming the file fails, the file is read
	 * and written.
	 * @return
	 * @throws IOException
	 */
	public boolean save(File content) throws IOException {
		File tmp = content;
		File sav = new File(file.getAbsolutePath() + ".sav");
		if (sav.exists())
			sav.delete();
		if (file.exists())
			file.renameTo(sav);
		
		if (tmp.renameTo(file)) { 
			if (sav.exists())
				sav.delete();
			return true;
		} else { // try reading content to the file;
			try {
				InputStream is = new FileInputStream(tmp);
				Utils.fileFromStream(tmp.getAbsolutePath(), is);
				return true;
			} catch (IOException e) {
				sav.renameTo(file);
				tmp.delete();
				return false;
			}
		}
	}
	/**
	 * Answers with an ID for the attachment which can be used to construct a URL for it.
	 * @return String.
	 */
	public String getId() {
		return mId;
	}

	public File getFile() {
		return file;
	}
}

