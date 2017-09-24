
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
 * Licensed Material - Property of IBM
 * Copyright IBM  2011 All Rights Reserved
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *   
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 *  
 */
package com.ibm.rhapsody.sm;

import java.io.InputStream;
import java.text.SimpleDateFormat;

public class Utils {
	public static SimpleDateFormat sSdf;
	
	public Utils() {
		sSdf = new SimpleDateFormat();
		sSdf.applyPattern(IRhpConstants.datePattern);
	}

//	static public String result2table(List<Map<String, RioValue>> results) {
//		StringBuffer sb = new StringBuffer();
//		sb.append("<table border=1>\n");
//		boolean needsHeaderRow = true;
//		Iterator<Map<String,RioValue>> rowIterator = results.iterator();
//		while( rowIterator.hasNext() ) {
//			Map<String,RioValue> row = (Map<String,RioValue>) rowIterator.next();
//			Set<String> columns = row.keySet();
//			if (needsHeaderRow) {
//				sb.append("<tr>\n");
//				for(String heading : columns ) {
//					RioValue val = row.get(heading);
//					sb.append("<th>" + heading + "<br/>(" + val.getType()+")</th>");
//				}
//				sb.append("</tr>\n");
//				needsHeaderRow = false;
//			}
//			sb.append("<tr>\n");
//			for(String col : columns ) {
//				RioValue val = row.get(col);
//				sb.append("<td>" + StringUtils.forHtml(val.stringValue()) + "</td>");
//			}
//			sb.append("</tr>\n");
//		}
//		sb.append("</table>\n");
//		return sb.toString();
//	}

	/**
	 * Joins the elements of the array as a list.
	 * @param array String[] of items.
	 * @return String of joined items in the array.
	 */
	public static String join(String[] array) {
		StringBuffer sb = new StringBuffer();
		for (String string : array) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(string);
		}
		return sb.toString();
	}
	
	/**
	 * Reads input stream to its full size.
	 * @param len int length of the input
	 * @param in InputStream of the soruce of data.
	 * @return byte[] of the contents.
	 * @throws Exception
	 */
	public static byte[] readInput(int len, InputStream in) throws Exception {
		byte buff[] = null;
		buff = new byte[len];
		int read = 0;
		while (read < len) {
			int l = in.read(buff, read, len-read);
			if (l < 0) break;
			read += l;
		}
		if (read < len)
			throw new Exception("Failed to read all input from server.");
		return buff;
	}


}
