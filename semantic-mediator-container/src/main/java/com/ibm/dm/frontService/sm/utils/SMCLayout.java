
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
 * 
 */
package com.ibm.dm.frontService.sm.utils;

import java.util.Date;

import org.apache.log4j.HTMLLayout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.utils.SMCLogger.MyMessage;

public class SMCLayout extends HTMLLayout {
	public SMCLayout() { super();}
	public SMCLayout(String rootName) {
		this();
//		if (false == Strings.isNullOrEmpty(rootName))
//			rootName = "/" + rootName;
		setTitle("Semantic Mediation Container Log for [" + /*Utils.getHost(null) +*/ rootName + "]"); 
		if (null == header) {
			header = Utils.getHtmlTemplate("templates/log.html#header");
			row = Utils.getHtmlTemplate("templates/log.html#row");
			footer = Utils.getHtmlTemplate("templates/log.html#footer");
		}
	}

	static String header = null;
	static String row = null;
	static String footer = null;
	
	@Override
	public String format(LoggingEvent event) {
		String rowt = new String(row);
		String info = "";
		if (false == event.getMessage() instanceof MyMessage) {
			info = super.format(event);
		} else {
			MyMessage mm = (MyMessage)event.getMessage();
			rowt = Utils.replaceAll(rowt, "_time_", new Date(event.timeStamp).toString());
			rowt = Utils.replaceAll(rowt, "_id_", mm.getId());
			rowt = Utils.replaceAll(rowt, "_duration_", mm.getDuration());
			rowt = Utils.replaceAll(rowt, "_thread_", event.getThreadName());
			boolean error = event.getLevel().isGreaterOrEqual(Level.ERROR);
			String font = "<font color='blue'>";
			if (error)
				font = "<font color='red'>";
			rowt = Utils.replaceAll(rowt, "_level_", font + event.getLevel().toString() + "</font>");
			rowt = Utils.replaceAll(rowt, "_category_", mm.getEvent());
//			if(getLocationInfo()) {
//				rowt = Utils.replaceAll(rowt, "_location_", event.getLocationInformation().fullInfo);
//			} else
//				rowt = Utils.replaceAll(rowt, "_location_","--");
			rowt = Utils.replaceAll(rowt, "_job_", mm.getJob());
			String task = mm.getTask();
			if (Strings.isNullOrEmpty(task)) task = "--";
			rowt = Utils.replaceAll(rowt, "_task_", task);
			String phase = mm.getPhase();
			if (Strings.isNullOrEmpty(phase)) phase = "--";
			rowt = Utils.replaceAll(rowt, "_phase_", phase);
			rowt = Utils.replaceAll(rowt, "_info_", font + mm.getInfo() + "</font>");
			info = rowt;
//			String marker2 = "</td>";
//			int p1 = info.indexOf(marker1);
//			String result = info;
//			if (p1 >= 0) {
//				result = info.substring(0, p1) + marker1;
//				String rest = info.substring(p1 + marker1.length());
//				int p2 = rest.indexOf(marker2);
//				if (p2 >= 0) {
//					result += "<b>" + rest.substring(0, p2) + "</b>" + rest.substring(p2); 
//				} else
//					result += rest;
//			}
//			return result;
		}
		return info;
	}

	@Override
	public void setLocationInfo(boolean flag) {
		super.setLocationInfo(flag);
	}

	@Override
	public String getFooter() {
		String footert = new String(footer);
		return footert;
	}

	@Override
	public String getHeader() {
		String headert = new String(header);
		headert = Utils.replaceAll(headert, "_title_", getTitle());
		headert = Utils.replaceAll(headert, "_logStartTime_", new Date(LoggingEvent.getStartTime()).toString());
		
		return headert;
	}
}