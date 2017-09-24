/**
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
package com.ibm.haifa.smc.client.sample;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import com.google.common.io.LineReader;
import com.ibm.haifa.smc.client.SmcClient;
import com.ibm.haifa.smc.client.SmcListener;

public class SampleClient extends SmcClient {

	public SampleClient(String title, ImageInputStream icon) {
		super(title, icon);
	}

	@Override
	public void console(String txt) {
		System.out.println("CONSOLE SAYS: " + txt);
	}

	@Override
	public void getLocalElement(String name) {
		localElement = UUID.randomUUID().toString(); // Sets local element to a dummy value - just to test the "Update" button
	}

	static private String fileName = readConf();
	
	static String readConf() 
	{
		String def = "sample/sample.rdf";
		File conf = new File("smcClient/smcClient.dat");
		boolean b = conf.getParentFile().mkdirs();
		try {
			if (false == conf.canRead()) 
				conf.createNewFile();
			def =  new LineReader(new FileReader(conf)).readLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return def;
	}
	
	static void saveConf() {
		File conf = new File("smcClient/smcClient.dat");
		conf.mkdirs();
		if (conf.exists())
			conf.delete();
		try {
			conf.createNewFile();
			FileWriter c = new FileWriter(conf);
			c.write(fileName);
			c.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/** 
	 * Dialog to open a file for RDF export
	 * @return File to rea RDF from.
	 */
	static File openFile() {
		JFrame frame = new JFrame();
		fileName = readConf();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500,300);
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BorderLayout());
		
		JFileChooser dialog = new JFileChooser(fileName); //(new JFrame(), "Input RDF File", FileDialog.LOAD);
		dialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int selection = dialog.showOpenDialog(frame);  
//		dialog.setFile(fileName);
		if (selection == JFileChooser.APPROVE_OPTION) {
			File f = dialog.getSelectedFile();
			fileName = f.getAbsolutePath();
			saveConf();
			return f;
		} else {
			System.out.println( "Cancelled...");
			return null;
		}
	}	


	public static void main(String [] args) throws Exception {
		boolean isModal = false;
		boolean exportEnabled = true;
		boolean importEnabled = true;

		File input = openFile();
		if (null == input)
			return;
		FileInputStream rdf = new FileInputStream(input);
		final StringWriter sw = new StringWriter();
		int c; while ((c = rdf.read()) != -1)
			sw.write(c);
		//System.err.println(sw);

		ImageInputStream logo = ImageIO.createImageInputStream(new FileInputStream("sample/smc_favicon_16x16.png"));
		final SmcClient client = new SampleClient(title(isModal, exportEnabled, importEnabled), logo);
		client.setListener(new SmcListener() {
			public void handleEvent(SmcEvent event) {
				if (SmcEvent.EXPORT.equals(event)) {
					System.err.println("GOT AN EXPORT! GOING TO SETUP THE RDF!");
					Map<String, String> associations = client.getAssociationMap();
					System.err.println("FOR THE CURRENT URL, I HAVE AN ASSOCIATION MAP WITH " + associations.size() + " ENTRIES, TO MATCH BETWEEN LOCAL AND REMOTE RESOURCES");
					client.setExportRdf(sw.toString());
					System.err.println("DID IT! RDF IS NOW READY FOR EXPORT");
				}
				else if (SmcEvent.IMPORT.equals(event)) {
					System.err.println("GOT AN IMPORT! HERE'S THE IMPORTED RDF:");
					String rdf = client.getImportRdf();
					System.err.println("THE IMPORTED RDF CONTAINS " + rdf.length() + " BYTES");
					System.err.println("AFTER PROCESSING IT, I CAN UPDATE THE ASSOCIATION MAP...");
				}
				else if (SmcEvent.CANCEL.equals(event)) {
					System.err.println("BYE BYE!");
					System.exit(0);
				}
			}});

		client.show(isModal, exportEnabled, importEnabled);
		System.err.println("SHOWING CLIENT WINDOW...");
		// Exit after closing window is done by SMC event listener
	}

	private static String title(boolean isModal, boolean exportEnabled, boolean importEnabled) {
		String title = "Sample Panel (";
		if (exportEnabled && !importEnabled)
			title += "only export";
		else if (!exportEnabled && importEnabled)
			title += "only import";
		else
			title += "import/export";

		title += ", ";

		if (isModal)
			title += "modal";
		else
			title += "non-modal";

		title += ")";

		return title;

	}
}
