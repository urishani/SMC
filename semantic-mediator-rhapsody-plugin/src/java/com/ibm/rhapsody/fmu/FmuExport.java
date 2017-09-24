
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

package com.ibm.rhapsody.fmu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.ibm.rhapsody.sm.IFmuExport;
import com.telelogic.rhapsody.core.IRPModelElement;

public class FmuExport implements IFmuExport {

	@Override
	public void exportElement(IRPModelElement block, String path)
			throws Exception, IllegalArgumentException {
		if (canExport2Fmu(block)) {
			File fmu = getFmuFile(block);
			copyFile(fmu, new File(path));
		}
	}

	@Override
	/**
	 * This implementation checks that the FMU object has been created for the block.
	 */
	public boolean canExport2Fmu(IRPModelElement block) throws Exception {
		return getFmuFile(block).exists();
	}

	private void copyFile(File src, File dst) throws IOException {
		FileInputStream from = new FileInputStream(src);
		FileOutputStream to = new FileOutputStream(dst);
		int b;

		while ((b = from.read()) != -1)
			to.write(b);

		from.close();
		to.close();
	}

	public void setFolder(File folder) {
		this.folder = folder;
	}

	private File folder;

	/**
	 * Answers with the file containig the FMU for a certain block. <br>
	 * The FMU may be either at the generation location where the FMU plugin works, or in a working 
	 * folder where it might have been copied into. Using the same naming convention.
	 * @param block element for which FMU is needed.
	 * @return File for either of these files. Note, this value may still be a non existing or not a readable file. 
	 * @throws Exception
	 */
	private File getFmuFile(IRPModelElement block) throws Exception {
		File blockFolder = new File(folder, block.getName() + "FMU");
		if (false == blockFolder.exists())
			blockFolder = new File(folder, block.getName() + "_MSVCDLL");
		File fmuFolder = new File(blockFolder, "FMU");
		File generatedFmu = new File(fmuFolder, block.getName() + ".fmu");
		File copiedFmu = new File(folder, block.getName() + ".fmu");
		return generatedFmu.exists() ? 
				generatedFmu : 
					copiedFmu;
	}
}
