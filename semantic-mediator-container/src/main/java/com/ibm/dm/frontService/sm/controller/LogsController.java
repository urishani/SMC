
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

/**
 * The work leading to these results have received funding from the Seventh Framework Programme
 * SPRINT ICT-2009.1.3 Project Number: 257909
 */

package com.ibm.dm.frontService.sm.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.utils.SMCLogger;
import com.ibm.dm.frontService.sm.utils.Utils;

@RestController
public class LogsController extends SmcBaseController {

	@RequestMapping(value="/smLogs", method=RequestMethod.GET) //, produces={"text/html", "text/plain", "application/json"})
	public String getLog(
			@RequestParam(value="ShowLog", required=false, defaultValue="") String showLog,
			@RequestParam(value="UpgradeLog", required=false, defaultValue="") String upgradeLog,
			@RequestParam(value="Action", required=false, defaultValue="") String action,
			@RequestHeader("accepts") Map<String,String> headers,
			HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		onEntry(request);
		String msg = "";
		boolean failed = false;
		Database db = getDatabase(request);
 		System.out.println("getLog called!!");

		try {
//			try {
//				Class.forName("com.ibm.dm.frontService.sm.utils.SMCLayout");
//				Class.forName("com.ibm.dm.frontService.sm.utils.SMCRollingFileAppender");
//				Class.forName("com.ibm.dm.frontService.sm.utils.SMCLogger");
//				System.out.println("At GetLogs: Loaded classes successfully");
//			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
//			}
			if (false == Strings.isNullOrEmpty(upgradeLog)) {
				String result = handleUpgradeLogs(db, upgradeLog, action, headers.get("accepts"));
				if (Strings.isNullOrEmpty(result))
					result = "UpgradeLog came up with nothing!";
				return result;
			}
			String fn = SMCLogger.getFileName(db).getAbsolutePath();
			if (false == Strings.isNullOrEmpty(showLog)) {
				fn += "." + showLog;
			}
			File ff = new File(fn);
			String html = Utils.stringFromStream(new FileInputStream(ff));
			html = Utils.replaceAll(html, "_current_", Strings.isNullOrEmpty(showLog)?"0":showLog);
			int pages=1;
			while (true) {
				File f = new File(SMCLogger.getFileName(db) + "." + pages);
				if (false == f.canRead())
					break;
				pages++;
			}
			pages--;
			html = Utils.replaceAll(html, "_pages_", "" + pages);
			return html;
		} catch (Exception e) {
			msg = "LogsProvider: " + e.getCause() + " [" + e.getMessage() + "].";
			failed = true;
			return "<html><body>" + msg + "</body></html>";
		} finally {
			onExit(request, failed, msg, false);
		}
//		return "<html><body><html>No Log file found</body></html>";
	}

	private String handleUpgradeLogs(Database db, String upgradeLog, String action, String accepts) throws JSONException {
    		File f = SMCLogger.getFileName(db);
    		File logs = f.getParentFile().getParentFile();
    		File migrationsFolder = new File(logs,"migrations");
			String reportTxt = "No contents to show";
    		
    		if (false == upgradeLog.equals("all")) {
    			String id = upgradeLog;
    			File report = new File(migrationsFolder, id);
				try {
					reportTxt = Utils.stringFromStream(new FileInputStream(report));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
    			if ("L".equals(action)) {
    				return reportTxt.split("\n")[0] + " ...";
    			} else
    				return reportTxt;
    		}
    		
    		final class comparableFile extends File implements Comparable<File> {
 				private static final long serialVersionUID = 1L;
				public comparableFile(File f) {super(f.getAbsolutePath());}
				@Override
				public int compareTo(File pathname) {
					if (getName().equals("report.txt"))
						return 1;
					else if (pathname.getName().equals("report.txt"))
						return -1;
					else if (getName().equals(pathname.getName()))
						return 0;
					return super.compareTo(pathname);
				}
    		}
    		File[] rs = migrationsFolder.listFiles();
    		comparableFile[] reports = new comparableFile[rs.length];
    		int i=0;
    		for (File r: rs) {
    			reports[i]= new comparableFile(r);
    			i++;
    		}
    		Arrays.sort(reports);
    		JSONObject contents = new JSONObject();
    		String rootName = Database.getRootName();
    		if (false == Strings.isNullOrEmpty(rootName))
    			rootName = "/ " + rootName;
    		contents.put("title", "Semantic Mediation Container Log for [" + Utils.getHost(null) + rootName + "]");
    		JSONArray rows = new JSONArray();
    		contents.put("row", rows);
    		for (File report : reports) {
        		JSONObject row = new JSONObject();
        		rows.put(row);
        		String name = report.getName();
        		String date = name;
        		if (date.equals("report.txt"))
        			date = "current";
        		else
        			date = name.substring("report-".length(), name.indexOf('.'));
    			row.put("id", name);
    			row.put("date", date);
				reportTxt = "No contents to show";
    			try {
					reportTxt = Utils.stringFromStream(new FileInputStream(report));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				row.put("details", reportTxt.split("\n")[0] + "..."); // only one first line
    		}
    		
    		if (null != accepts && accepts.equals("application/json"))
    			return contents.toString();
    		
    		String html = Utils.getHtmlTemplate("upgradeLog.html");
    		return Utils.mergeJsonWithHtml(html, contents);
		}
}
