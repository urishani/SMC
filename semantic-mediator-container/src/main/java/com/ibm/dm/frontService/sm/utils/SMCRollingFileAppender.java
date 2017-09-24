
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
package com.ibm.dm.frontService.sm.utils;

import java.io.File;

import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.CountingQuietWriter;

public class SMCRollingFileAppender extends RollingFileAppender {

	/**
	 * Triggers rollover whenever the header is requested and the file is not empty.
	 */
	@Override
	protected void writeHeader() {
		long size = ((CountingQuietWriter) qw).getCount();
		if (size <=0) {
			String fn = getFile();
			File f = new File(fn);
			String fp = f.getAbsolutePath();
			size = f.length();
			((CountingQuietWriter)qw).setCount(size);
		}
		if (size > 0) {
			super.rollOver();
			return;
		}
		super.writeHeader();
		qw.flush();
	}

}
