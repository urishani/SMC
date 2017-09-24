
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
package com.ibm.rhapsody.sprint.rdfsync;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.ibm.rhapsody.sm.rdfsync.Helper;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RhapsodyAppServer;
import com.telelogic.rhapsody.core.RhapsodyRuntimeException;

/**
 * @author sergeyzo
 *
 */
public class HelperTests {
	private static final String RHAPSODY_GUID_PREFIX = "GUID ";
	private static final String SPRINT_URI_PREFIX = "http://sprint/ID-";
	private static final String RHAPSODY_URI_PREFIX = "http://com.ibm.rhapsody/sprint/GUID-";

	/**
	 * Test method for {@link com.ibm.rhapsody.sm.rdfsync.Helper#loadModel(java.lang.String)}.
	 */
	@Test
	public void testLoadModel() {
		assertNotNull(Helper.loadModel(getRDFTestStream()));
	}

	/**
	 * Test method for {@link com.ibm.rhapsody.sm.rdfsync.Helper#extractIdMap(com.hp.hpl.jena.rdf.model.Model)}.
	 */
	@Test
	public void testExtractIdMap() {
		Model model = Helper.loadModel(getRDFTestStream());
		Map<String, String> idMap = Helper.extractIdMap(model, new Helper.URITransform() {
			@Override
			public String transform(String uri) {
				return "_" + uri;
			}
		});
		assertNotNull(idMap);
		
		assertTrue("idMap must not be empty", idMap.size() > 0);
		
		Properties p = new Properties();
		p.putAll(idMap);
		p.putAll(Helper.inverseIdMap(idMap));
		
		assertTrue("Properties should be twice as big as idMap", idMap.size() * 2 == p.size());
		
		System.out.println("Successfully created Properties of total: " + p.size());
		//p.list(System.out);
	}

	/**
	 * Test method for {@link com.ibm.rhapsody.sm.rdfsync.Helper#transformURIs(com.hp.hpl.jena.rdf.model.Model)}.
	 * @throws IOException 
	 */
	@Test
	public void testTransformURIs() throws IOException {
		Model model = Helper.loadModel(getRDFTestStream());
		Map<String, String> idMap1 = Helper.transformURIs(model, new Helper.PrefixURITransform(RHAPSODY_URI_PREFIX, SPRINT_URI_PREFIX));
		assertNotNull(idMap1);
		System.out.println("------ Successfully created Jena RDF model of total: " + idMap1.size());
		
		assertTrue("idMap1 must not be empty", idMap1.size() > 0);
		
		Map<String, String> idMap2 = Helper.extractIdMap(model, new Helper.PrefixURITransform(SPRINT_URI_PREFIX, RHAPSODY_GUID_PREFIX));
		assertNotNull(idMap2);
		
		assertTrue("idMap2 must not be empty", idMap2.size() > 0);
		
		Properties p = new Properties();
		p.putAll(idMap2);
		p.putAll(Helper.inverseIdMap(idMap2));
		
		assertTrue("Properties should be twice as big as idMap", idMap2.size() * 2 == p.size());
		
		System.out.println("------ Successfully created Properties of total: " + p.size());
		//p.list(System.out);
		
		URL rootDirURL = Helper.class.getResource("/");
		File rootDir = new File(rootDirURL.getFile()).getParentFile();

		File rdfFile = new File(rootDir, "transformedIds.rdf");
		FileOutputStream rdfOutStream = new FileOutputStream(rdfFile);
		model.write(rdfOutStream);
		rdfOutStream.close();
		System.out.println("------ Successfully saved RDF model to file: " + rdfFile.getAbsolutePath());
		
		File propFile = new File(rootDir, "idmap.prop");
		FileOutputStream propOutStream = new FileOutputStream(propFile);
		p.save(propOutStream, "Auto-generated for test purposes, please do not modify manually");
		propOutStream.close();
		System.out.println("------ Successfully saved Properties to file: " + propFile.getAbsolutePath());
	}

	/**
	 * Test method for {@link com.ibm.rhapsody.sm.rdfsync.Helper#syncRDF2IRP(com.hp.hpl.jena.rdf.model.Model)}.
	 * @throws IOException 
	 */
	@Test
	public void testsyncRDF2IRP() throws Exception {
		Model model = Helper.loadModel(getRDFTestStream());
		Map<String, String> idMap1 = Helper.transformURIs(model, new Helper.PrefixURITransform(RHAPSODY_URI_PREFIX, SPRINT_URI_PREFIX));
		assertNotNull(idMap1);
//		System.out.println("------ Successfully created Jena RDF model of total: " + idMap1.size());
		
//		assertTrue("idMap1 must not be empty", idMap1.size() > 0);
		
		Map<String, String> idMap2 = Helper.extractIdMap(model, new Helper.PrefixURITransform(SPRINT_URI_PREFIX, RHAPSODY_GUID_PREFIX));
		assertNotNull(idMap2);
		
//		assertTrue("idMap2 must not be empty", idMap2.size() > 0);
		
		Properties p = new Properties();
		p.putAll(idMap2);
		p.putAll(Helper.inverseIdMap(idMap2));
		
		assertTrue("Properties should be twice as big as idMap", idMap2.size() * 2 == p.size());
		
		System.out.println("------ Successfully created Properties of total: " + p.size());
		
		// open RHP project
		IRPApplication rhpApp = null;;
		try {
			rhpApp = openRhpProject("D:\\temp\\rhp\\EmptyTest\\EmptyTest.rpy");
			Helper.syncRDF2IRP(model, rhpApp.activeProject(), p, null);
			rhpApp.saveAll();
		} catch(Exception x) {
			if(rhpApp != null)
				System.err.println("Fail cause: " + rhpApp.getErrorMessage());
			throw x;
		} finally {
			if(rhpApp != null) {
				if(rhpApp.activeProject() != null)
					rhpApp.activeProject().close();
				rhpApp.quit();
			}
			RhapsodyAppServer.CloseSession();
//			RhapsodyAppServer.CloseSessionNative();
		}
	}
	
	IRPApplication openRhpProject(String rpy) {
		IRPApplication rhpApp = null;;
		try {
			rhpApp = RhapsodyAppServer.getActiveRhapsodyApplication();
		} catch(RhapsodyRuntimeException x) {} // Ignore and fallback

		if (rhpApp == null) {
			try {
				rhpApp = RhapsodyAppServer.createRhapsodyApplicationDllServer();
			} catch(RhapsodyRuntimeException x) {} // Ignore and fallback
			if (rhpApp == null)
				rhpApp = RhapsodyAppServer.createRhapsodyApplication();
			if (rhpApp == null) {
				System.out.println("Error. Cannot create Rhapsody instance.");
				return null;
			}
			if(rhpApp.activeProject() == null) {
				if(rpy != null)
					rhpApp.openProject(rpy);
			}
		}
		return rhpApp;
	}

	// Utility method
	private static InputStream getRDFTestStream() {
		try {
			return new FileInputStream(new File("data/rhapsody.import.rdf"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		//Helper.class.getResourceAsStream("/error-data-for-bug.xml");
	}
}
