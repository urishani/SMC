
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

package com.ibm.rhapsody.sm.plugin;



import java.awt.FileDialog;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileUtils;
import com.ibm.haifa.smc.client.oauth.IUserCredentials;
import com.ibm.haifa.smc.client.oauth.InvalidUserCredentials;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicator;
import com.ibm.haifa.smc.client.oauth.OAuthCommunicatorException;
//import com.ibm.haifa.smc.repository.SMCPanel.UserCreds;
import com.ibm.rhapsody.sm.IFmuExport;
import com.ibm.rhapsody.sm.IRhpConstants;
import com.ibm.rhapsody.sm.Utils;
import com.ibm.rhapsody.sm.IRhpConstants.RHP_USR_CLASS_NAME;
import com.ibm.rhapsody.sm.rdfsync.Helper;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPClass;
import com.telelogic.rhapsody.core.IRPClassifier;
import com.telelogic.rhapsody.core.IRPInstance;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPPort;
import com.telelogic.rhapsody.core.IRPProject;
import com.telelogic.rhapsody.core.RPPackage;
import com.telelogic.rhapsody.core.RhapsodyAppServer;
import com.telelogic.rhapsody.core.RhapsodyRuntimeException;



public class RhpPlugin { //extends RPUserPlugin {

//	static {
//		sdf.applyPattern("yyyy-MM-ddTHH:mm:ss.SSSZ"); //"2011-08-22T23:34:45.978+03:00" --> "2011-08-22T23:34:45.978+0300"
//	}
//	public RhpPlugin() {
//		super();
//	}

	/**
	 * A helper internal class to be used to maintain the internal management of triples
	 * to be published.
	 * @author shani
	 *
	 */

	protected static RhpPlugin rhpPlugin = null;
	protected IRPApplication m_rhpApplication = null;
//	protected RhpTemplateConfiguration m_cfg;

	protected String PLUGIN_NAME = "Simple Plugin";
	protected MyConfigData m_configProps; // = new MyConfigData();
	protected final static String MAP_PROPERTIES_DO_AUTH = "sii.doAuth";
	protected final static String MAP_PROPERTIES_FILE_NAME = "sii.props";
	protected final static String MAP_PROPERTIES_FILE_SUFFIX = "props";
	protected final static String MAP_PROPERTIES_TOOL_NAME = "sii.tool";
	protected final static String MAP_PROPERTIES_USER_NAME = "sii.user";
	protected final static String MAP_PROPERTIES_PWD_NAME = "sii.pwd";
	protected final static String PROPERTIES_FILE_NAME = "sii.props";
	protected String mServerURL = null;
	protected String mRdfServerCleaningURL = null;
	protected String mSiiSystemUri = null;
	protected Properties m_sii_guid_mapping = new Properties();
	protected boolean mLoggedIn = false;

	protected final static String FMU_EXPORT_CLASS_NAME = "com.ibm.rhapsody.fmu.FmuExport";
	protected final static String ATTACHMENTS_REPOSITORY_PATH = "/dm/sm/repository/attachments";
	protected final static String HAS_FMU_PROPERTY = IRhpConstants.SM_PROPERTY_NS + "hasFMU";
	protected boolean mIsFmuExportAvailable = false;

	// Following static methods are required for this plugin to be compatible with
	// version 8.
	// These methods serve as a proxy for thet original implementations of version 7.6.

	// called when the plug-in is loaded
	public static void RhpPluginInit(final IRPApplication rpyApplication) {
		rhpPlugin = new RhpPlugin();
		rhpPlugin.myRhpPluginInit(rpyApplication);
	}
	public static void RhpPluginInvokeItem() {
		rhpPlugin.myRhpPluginInvokeItem();
	}
	public static void OnMenuItemSelect(String menuItem) {
		rhpPlugin.myOnMenuItemSelect(menuItem);
	}
	public static void OnMenuItemSelect() {
//		rhpPlugin.myOnMenuItemSelect("null");
	}
	//called when the project is closed
	public static void RhpPluginCleanup(){
		rhpPlugin.myRhpPluginCleanup();
	}

	//called when Rhapsody exits
	public static void RhpPluginFinalCleanup(){
		rhpPlugin.myRhpPluginFinalCleanup();
		rhpPlugin = null;
	}

	public void myRhpPluginInit(final IRPApplication rpyApplication) {
		m_rhpApplication = rpyApplication;
		String line = "----------------------------- SMC Driver Initialization % ----------------------\n";
		console(line.replace("%", ""));
		console("Initializing SMC interface plugin. Protocol version " + IRhpConstants.VERSION_LEVEL);
		console(IRhpConstants.RELEASE_MSG);
		console("Using (optional) " + OAuthCommunicator.getRelease());
		testFmuExportPlugin(); // Check whether FMU Export is available
		IRPProject proj = m_rhpApplication.activeProject();
		console( "project [" + proj.getName() + "]: " + proj.getUserDefinedMetaClass() + ".\n");
		console(line.replace("%", " done"));
		//ApplicationListenerPlugin.RhpPluginInit(rpyApplication);
	}


	static private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	public void console(String msg) {
		msg = msg.trim();
		m_rhpApplication.writeToOutputWindow(PLUGIN_NAME , "[Rhp-SMC on " + dateFormat.format(new Date()) + "]: " + msg + "\n");
	}
	// called when the plug-in menu item under the "Tools" menu is selected
	public void myRhpPluginInvokeItem() {
		console( "Calling RhpPluginInvokeItem .....\n");
		try {
			console( "Obsolete!\n");
		} catch (RuntimeException rte) {
			console( "!!! Exception !!!\n");
			rte.printStackTrace();
		}
		console( ".... After calling RhpPluginInvokeItem.\n");
	}

	// called when the plug-in pop-up menu (if applicable) is selected
	public void myOnMenuItemSelect(String menuItem) {
		long time = System.currentTimeMillis();
		try {
			console( "-- Executing [" + menuItem + "]....\n");
			File folder = setupConfig();
			if (menuItem.startsWith("SMC\\Export") || menuItem.startsWith("Export")) {
				if (menuItem.contains("Export to SMC"))
					exportToSII(folder);
				else if (menuItem.contains("Export to FILE"))
					exportToFile(folder);
				else if (menuItem.contains("Export FMU"))
					exportFmu(folder);
			}
			else if (menuItem.startsWith("SMC\\Import") || menuItem.startsWith("Import")) {
				if (menuItem.contains("Import from FILE"))
					importFromFile(folder);
				else
					importFromSII(folder);
			}
			else if (menuItem.startsWith("SMC\\SysML") || menuItem.startsWith("SysML")) {
				exportSysMLToFile(folder);
			}
			else if (menuItem.startsWith("test")) {
				doTests(folder);
			}
			else if (menuItem.startsWith("SMC\\Reset") || menuItem.startsWith("Reset")) {
				// logIn(false); we need this only if we also reset the server.
				int result = resetStatusSII(folder);
				console( "State has" +
						((NO_RESET == result)?" NOT":"")	 + " been reset !\n");
				if (result == RESET_ALL)
					console( "Repository has also been cleaned.\n");
				if (result < 0)
					console( "Repository reset failed [" + result + "].\n");
			} else {
				console( "!!! Service not implemented !!!\n");
			}
		} catch (InvalidUserCredentials e) {
			console( "Login failed. Redo operation and fill in correct user/pwd info.\n");
			mLoggedIn = false;
		} catch(OAuthCommunicatorException e) {
        	e.printStackTrace();
			console( "Failed with HTTP error [" + e.getErrorStatus() + "]: " + e.getErrorMessage() + ".\n");
		} catch (Exception e) {
			e.printStackTrace();
			console( "Operation aborted [" + e.getClass().getName() + ": " + e.getMessage() + "]\n");
		} finally {
			time = System.currentTimeMillis() - time;
			console("Total execution time [" + time/1000.0 + "] seconds.");
			try {
				saveProps(m_sii_guid_mapping);
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
		console( "\nCompleted calling OnMenuItemSelect.\n");
	}

	// ---------------- GUI staff ----------------------

	/**
	 * Performs log in dialog only if not done already. Yet it is possible to force doing that.
	 * @param force boolean to mean that dialog should be performed.
	 * @throws Exception - in case the login is aborted by the user.
	 */
	public void logIn(boolean force) throws Exception {
		getCredentials();
		if (mLoggedIn && force == false)
			return;
		obtainCredentials();
		if (false == mLoggedIn)
			throw new Exception("Login aborted");
	}
	/**
	 * Ensure that credentials are not null or empty, and creates a credentials object for further communications.
	 * <br>If needed - invoke dialog.
	 * <br>Does not ensure these are legal.
	 * @return IUserCredentials object for use in communications.
	 */
	private IUserCredentials getCredentials() {
		while (true) {
			String user = (String) m_configProps.get(MAP_PROPERTIES_USER_NAME);
			String pwd = (String) m_configProps.getProperty(MAP_PROPERTIES_PWD_NAME);
			boolean doAuth = Boolean.valueOf(m_configProps.getProperty(MAP_PROPERTIES_DO_AUTH));
			while (doAuth && (Strings.isNullOrEmpty(user) || Strings.isNullOrEmpty(pwd))) {
				obtainCredentials();
				user = m_configProps.getProperty(MAP_PROPERTIES_USER_NAME);
				pwd = m_configProps.getProperty(MAP_PROPERTIES_PWD_NAME);
				doAuth = Boolean.valueOf(m_configProps.getProperty(MAP_PROPERTIES_DO_AUTH));
			}
		return new UserCreds(user, pwd, doAuth );
		}
	}
	/**
	 * Invokes the credentials dialog and sets up the properties holding the credentials data.
	 */
	private void obtainCredentials() {
		String user = m_configProps.getProperty(MAP_PROPERTIES_USER_NAME);
		String pwd = m_configProps.getProperty(MAP_PROPERTIES_PWD_NAME);
		boolean doAuth = Boolean.valueOf(m_configProps.getProperty(MAP_PROPERTIES_DO_AUTH));
		PasswordDialog dialog = new PasswordDialog(user, pwd, "SMC Login Dialog", doAuth);
		if (dialog.isOk()) {
			user = dialog.getUser();
			pwd = dialog.getPwd();
			doAuth = dialog.doAuth();
			m_configProps.setProperty(MAP_PROPERTIES_USER_NAME, user);
			m_configProps.setProperty(MAP_PROPERTIES_PWD_NAME, pwd);
			m_configProps.setProperty(MAP_PROPERTIES_DO_AUTH, Boolean.toString(doAuth));
			mLoggedIn = true;
		}
	}


	//private final static int RESET = 0;
	private final static int NO_RESET = 1;
	private final static int RESET_ALL = 2;
	//private final static int RESET_FAILED = -1;

	private int resetStatusSII(File folder) throws Exception {
		JFrame frame = new JFrame();
		frame.setLocationRelativeTo(null);
		Object[] options = {"Yes",
                "No, thanks"};
		int n = JOptionPane.showOptionDialog(frame,
				"Would you like to reset the URL mappings for server [" + m_configProps.getRdfServerUrl() + "]?",
				"Configm reset",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[1]);
		if (NO_RESET == n)
			return n;
		loadProperties(folder);
		String path = m_sii_guid_mapping.getProperty(MAP_PROPERTIES_FILE_NAME);
		m_sii_guid_mapping.clear();
		m_sii_guid_mapping.setProperty(MAP_PROPERTIES_FILE_NAME, path);
		m_sii_guid_mapping.setProperty(MAP_PROPERTIES_TOOL_NAME, m_configProps.getRdfServerUrl());
//		if (RESET_ALL == n) {
//			logIn(false);
//			setMRdfCleaningURL("Cleaning URL");
//			HttpGet httpget = new HttpGet(mRdfServerCleaningURL);
//			HttpResponse resp = null;
//			OAuthCommunicator conn = null;
//			try {
//				conn = new OAuthCommunicator(getCredentials());
//
//				//Send the request and return the response
//				resp = conn.execute(httpget); //DefaultHttpClient().execute(httppost);
//				if (resp.getStatusLine().getStatusCode() != IConstants.SC_OK)
//					return -1;
//			} catch (InvalidUserCredentials e) {
//				throw e;
//			} catch (Exception e) {
//				return -1;
//			}
//		}
		return n;
	}

	/**
	 * Export model to an RDF file of the user's choice
	 * @param folder String so this call matches the signature of exportToSII.
	 * @throws Exception
	 */
	private void exportToFile(File folder) throws Exception {
		IRPProject project = m_rhpApplication.activeProject();
		checkType(project, RHP_USR_CLASS_NAME.SysML.toString(), RHP_USR_CLASS_NAME.Project.toString());
		File tmpFile = setOutputRdfFile();
		if (null == tmpFile)
			return;
		exportToFile(folder, tmpFile);
		console("No export to server");
	}

	private void exportToFile(File folder, File tmpFile) throws Exception {
		m_configProps.setRdfServerUrl(FileUtils.toURL(tmpFile.getAbsolutePath()));
		loadProperties(folder);
		doExport(tmpFile, null); // do the file only.
	}

	/**
	 * Export model from Rhapsody to SII
	 * @throws Exception
	 */
	public void exportToSII(File folder) throws Exception {
		IRPProject project = m_rhpApplication.activeProject();
		checkType(project, RHP_USR_CLASS_NAME.SysML.toString(), RHP_USR_CLASS_NAME.Project.toString());
		File tmpFile = File.createTempFile(project.getName(), ".xml");
		logIn(false);
		if (false == setMRdfServerURL("Export to"))
			return;
		loadProperties(folder);
		doExport(tmpFile, mServerURL);
	}

	/**
	 * Export a (selected) block's FMU to SII
	 * @throws Exception
	 */
	private void exportFmu(File folder) throws Exception {
		if (false == testFmuExportPlugin())
			return;
		IRPModelElement block = m_rhpApplication.getSelectedElement();
		// Preparing the exporter which may be defined in a configuration via reflection.
		IFmuExport fmuExport = (IFmuExport)(Class.forName(FMU_EXPORT_CLASS_NAME).newInstance());
		try {
			// Temporary - Used only for dummy implementation
			// So far, we have only one implementation, in which setfolder() is defined.
			fmuExport.getClass().getMethod("setFolder", File.class).invoke(fmuExport, folder);
		} catch (Exception e) {}
		if (false == fmuExport.canExport2Fmu(block)) {
			console("Illegal request. Cannot export FMU for selected block");
			return;
		}
		logIn(false);
		if (false == setMRdfServerURL("Export to"))
			return;
		Map<String, String> resourceMap = canExportTo(block);
		if (false == resourceMap.containsKey(mServerURL)) {
			console("Cannot export FMU for selected block. Needs to Export that block to SMC firstly");
			return;
		}
		File path = new File(folder, block.getGUID()+ ".zip");
		path.deleteOnExit();
		fmuExport.exportElement(block, path.getAbsolutePath());
		exportFmu(block, path, folder);
		path.delete();
	}

	public void doExport(File tmpFile, String serverURL) throws Exception {
		WorkModel.debugTrace("\n==========================\n");
		FileWriter out = new FileWriter(tmpFile);

		IRPModelElement top = m_rhpApplication.getSelectedElement();
//		System.out.println("Starting with [" + top.getName() + "]: " + top.getFullPathName());
//		IRPModelElement ptop = top.getOwner();
//		System.out.println("Owner of that [" + ptop.getName() + "]: " + ptop.getFullPathName());
//		ptop = ptop.getOwner();
//		System.out.println("Owner of owner of that [" + ptop.getName() + "]: " + ptop.getFullPathName());
//		IRPPackage pptop = (IRPPackage)ptop;
//		IRPCollection c = pptop.getClasses();
//		for (int i=1; i <= c.getCount(); i++) {
//			IRPModelElement e = (IRPModelElement) c.getItem(i);
//			System.out.println("class [" + i+ "]: " + e.getName() + "]: " + e.getFullPathName() + " MC [" + e.getMetaClass() + "]");
//			
//		}
		
		WorkModel workModel = new WorkModel(top, m_sii_guid_mapping);
		WorkModel.debugTrace("Traversing...");
		String msg = workModel.traverse(this);
		if (false == Strings.isNullOrEmpty(msg))
			console(msg);
		WorkModel.debugTrace("Serializing...");
		workModel.getModel().write(out);
		out.close();
//		Element.serializeAll(model.queryElements(null), out);
		console( "Output generated in [" + tmpFile.getAbsolutePath() + "]");

		if (null != serverURL) {
			console( "\nExporting to " + m_configProps.getRdfServerUrl() + "...");
			String report = export(tmpFile);
			console( "\n" + report);
		}
	}

	private void checkType(IRPModelElement part, String... expected) throws Exception {
		String metaClass = part.getUserDefinedMetaClass();
		List<String> list = Arrays.asList(expected);
		if (expected.length > 0 && false == list.contains(metaClass)) ;  // cancel this check.
//			throw new Exception("Meta class of element [" + part.getName() + "] is [" + metaClass + "]. Possible values are [" + flatten("", expected) + ".");
	}
//	/**
//	 * utility for debugging
//	 * @param list
//	 * @return
//	 */
//	private String flatten(String title, String[] list) {
//		StringBuffer sb = new StringBuffer();
//		for (String item : list) {
//			sb.append(item +  (sb.length() > 0?", ":""));
//		}
//		return "[" + sb.append("]").toString();
//	}
	public void loadProperties(File folder) {
		File mapPropFile = null;
		String fName = makePropertiesFname();
		if (null == folder) {
			mapPropFile = new File(fName);
		} else {
			mapPropFile = new File(folder.getAbsolutePath() + File.separator + fName);
		}
		try {
			if (mapPropFile.canRead()) {
				try {
					m_sii_guid_mapping.load(new FileInputStream(mapPropFile));
				} catch (FileNotFoundException e) {
					console( "No properties file [" + mapPropFile.getAbsolutePath() + "] found.\n");
				} catch (IOException e) {
					console( "No properties file [" + mapPropFile.getAbsolutePath() + "] found.\n");
				}
			}
		} finally {
			m_sii_guid_mapping.setProperty(PROPERTIES_FILE_NAME, mapPropFile.getAbsolutePath());
			String server = m_configProps.getRdfServerUrl();
			if (null != server)
				m_sii_guid_mapping.setProperty(MAP_PROPERTIES_TOOL_NAME, server);
//			m_configProps.put(PROPERTIES_FILE_NAME, cfgPropFile.getAbsolutePath());
		}
	}


	/**
	 * Creates a properties file name to be used depending on the mRdfServerURL used in the
	 * present export/import.
	 * @return String composed of the path of the URL, with separators replaced with _.
	 */
	private String makePropertiesFname() {
		String fName = MAP_PROPERTIES_FILE_NAME;
		String serverURL = m_configProps.getRdfServerUrl();
		if (Strings.isNullOrEmpty(serverURL)) {
			console("No target server to reset for");
			return fName;
		}
		try {
			URI server = new URI(serverURL);
			String host = server.getHost();
			if (null != host)
				fName = host.toLowerCase() + '_' + server.getPort() + '_' + server.getPath() + '_' + fName;
			else
				fName = server.toString().replace(':','_') + "_" + fName;
			fName = fName.replace('.','_').replace(File.separator.charAt(0), '_').replace('/', '_');
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println("Properties file name [" + fName + "]");
		return fName;
	}


	private void saveProps(Properties props) throws FileNotFoundException, IOException {
		String fileName = props.getProperty(PROPERTIES_FILE_NAME);
		if (null == fileName) {
			console( "Poperties could not be saved.\n");
			return;
		}
		File outFile = new File(fileName);
		FileOutputStream out = new FileOutputStream(outFile);
		props.store(out, "SII to GUID and GUID to SII mappings");
		out.close();
	}


	/**
	 * Performs input of RDF from a file rather than the http.
	 * @param folder File (where config data can be located.)
	 * @throws FileNotFoundException
	 */
	private void importFromFile(File folder) throws Exception {
		File inputFile = setInputRdfFile();
		console("Importing from [" + inputFile + "]");
		if (null == inputFile)
			return;
		m_configProps.setRdfServerUrl(FileUtils.toURL(inputFile.getAbsolutePath()));
		InputStream input= new FileInputStream(inputFile);
		doImport(folder, input);
	}

	/**
	 * Import models from SII
	 * @throws Exception
	 */
	private void importFromSII(File folder) throws Exception {
		logIn(false);
		if (false == setMRdfServerURL("Import from"))
			return;
		if (false == setMSiisystemUri())
			return;
		console( mServerURL+"?" + IRhpConstants.GET_REQUEST_PARAM + "=" + mSiiSystemUri + "\n");
		OAuthCommunicator aConn = null;
		HttpResponse resp = null;
		try {
			HttpGet httpget = new HttpGet(mServerURL+"?" + IRhpConstants.GET_REQUEST_PARAM + "=" + URLEncoder.encode(mSiiSystemUri, "UTF-8"));
			httpget.addHeader(HttpHeaders.ACCEPT, ContentTypes.RDF_XML);

			//Send the request and return the response
			IUserCredentials credentials = getCredentials();
			if (credentials.doAuth()) {
				aConn = new OAuthCommunicator(credentials);
				resp = aConn.execute(httpget); //new DefaultHttpClient().execute(httpget);
			} else {
				DefaultHttpClient conn = new DefaultHttpClient();
				resp = conn.execute(httpget);				
			}
			// get the results
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				InputStream input = resp.getEntity().getContent();
				doImport(folder, input);
			} else
				console( "HTTP Error code [ " + resp.getStatusLine().getStatusCode() + "].\n");
        } catch (Exception e) {
        	throw (e);
        } finally {
        	if (null != aConn)
        		aConn.cleanupConnections(resp);
        }
	}

	/**
	 * Export to file from built-in SysML service of Rhapsody
	 * @param folder File of working folder.
	 * @throws Exception
	 */
	private void exportSysMLToFile(File folder) throws Exception {
		IRPProject project = m_rhpApplication.activeProject();
		checkType(project, RHP_USR_CLASS_NAME.SysML.toString(), RHP_USR_CLASS_NAME.Project.toString());
		File outFile = setOutputRdfFile();
		console("outFile [" + outFile.getAbsolutePath() + "]");
//		if (null == outFile)
//			return;
		outFile.getParentFile().mkdirs();
//		folder = new File(folder, "tmp");
//		folder.mkdirs();
//		File tmpFile = File.createTempFile("SysML", ".rpy.nt", folder);
//		console("saving SysML in tmpFile [" + tmpFile.getAbsolutePath() + "]");
		project.saveAs(outFile.getAbsolutePath());
//		boolean ok = tmpFile.renameTo(outFile);
//		console("renaming to [" + outFile.getAbsolutePath() + "]: " + (ok?"OK":"Failed"));
		console("Saved output in [" + outFile.getAbsolutePath() + "].");
		m_configProps.setRdfServerUrl(FileUtils.toURL(outFile.getAbsolutePath()));
		loadProperties(folder);
		console("No export to server");
	}

	/**
	 * Performs the import from either a file or a server
	 * @param folder
	 * @param server
	 * @param system
	 */
	private void doImport(File folder, InputStream input) throws Exception {
		loadProperties(folder);
		com.hp.hpl.jena.rdf.model.Model model = Helper.loadModel(input);
		console( "RDF model received OK.\n");
		Helper.syncRDF2IRP(model, m_rhpApplication.activeProject(), m_sii_guid_mapping, this);
		console( "OK. Project imported.");
}

//	/**
//	 * Looks up a model element in the root element among its components, so that its SII_URI property
//	 * is same as the siiUri parameter. Null if not found.
//	 * @param siiUri String URI from SII to identify that element for update.
//	 * @return
//	 */
//	private IRPModelElement findElementBySiiUri(String siiUri) {
//		String guid = m_sii_guid_mapping.getProperty(siiUri);
//		if (null != guid)
//			return getFromGuid(guid);
////
////		List<IRPModelElement> parts = project.getNestedElements().toList();
////		for (IRPModelElement irpModelElement : parts) {
////			String rSiiUri = m_props.getProperty(irpModelElement.getGUID()); //irpModelElement.getPropertyValue(IRhpConstants.SII_URI);
////			if (rSiiUri.equals(siiUri))
////				return irpModelElement;
////		}
//		return null;
//	}




	@SuppressWarnings({ "unused", "unchecked" })
	private void addPortsInType(Repository repo, IRPClassifier type) throws Exception {
		String typeGuid = Repository.getGUID(type);
		String typeName = type.getName();
		List<IRPPort> ports = type.getPorts().toList();
		if (ports.size() < 1)
			return;
		Map<String, Object> element = repo.elements.get(typeGuid);
		if (null == element) {
			if (false == repo.addPart(type))
				throw new Exception("Type [" + typeName + "] is illegal, but it is used.");
			element = repo.elements.get(typeGuid);
		}
		for (IRPPort irpPort : ports) {
			if (false == element.containsKey(Repository.getGUID(irpPort))) {
				repo.addPart(irpPort);
				repo.addRelation(typeGuid, IRhpConstants.PORT, irpPort);
			}
		}
	}


	/**
	 * Finds from a list of elements the one containing the candidate element.
	 * @param parents List of elements
	 * @param candidate Element that is contained in one of the parents
	 * @return the parent, or null
	 */
	@SuppressWarnings("unused")
	private IRPModelElement findPackageFor(List<IRPModelElement> parents,
			IRPModelElement candidate) {
		for (IRPModelElement irpModelElement : parents) {
			if (irpModelElement.getNestedElements().toList().contains(candidate))
				return irpModelElement;
		}
		return null;
	}

	/**
	 * Utility to find out instances of a certain class
	 * @param aClass Class element.
	 * @return List of the filtered elements.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private List<IRPModelElement> getInstancesFromClass(IRPClass aClass) {
		List<IRPModelElement> elements = aClass.getRelations().toList();
		List<IRPModelElement> result = new LinkedList<IRPModelElement>();
		for (IRPModelElement element : elements) {
			if (element instanceof IRPInstance)
				result.add(element);
		}
		return result;
	}


	/**
	 * utility for debugging
	 * @param list
	 * @return
	 */

	@SuppressWarnings("unused")
	private String join(Set<String> elements) {
		StringBuffer sb = new StringBuffer();
		for (String string : elements) {
			sb.append(string + ", ");
		}
		return sb.toString();
	}


	@SuppressWarnings("unused")
	private void addParts(Repository repo, String guid, List<IRPModelElement> parts) throws Exception {
		for (IRPModelElement part : parts) {
			if (repo.addPart(part))
				repo.addPartRelation(guid, part);
		}
	}


	/**
	 * Prints a turtle statement of subject, property and value, all in the Rhapsody name space.
	 * @param out
	 * @param guid
	 * @param prop
	 * @param isRelation boolean indicating that the value is not a quoted literal.
	 * @param value... String array of optional arguments which may be values for the same property.
	 */
//	private void printStatement(PrintStream out, String guid, String prop, boolean isRelation, String... values) {
//		if (0 == values.length)
//			return;
//		if (null == guid) guid = "   ";
//			else guid = ":" + guid;
//		for (int i= 0; i < values.length; i++) {
//			String value = values[i];
//			if (!isRelation) {
//				if (value.contains("\n\r"))
//					value = "\"\"" + value + "\"\""; // long text with line breaks.
//				value = "\"" + value + "\"";
//			}
//			boolean lastLine = (i == values.length -1);
//			boolean firstLine = (0 == i);
//			String statement = (firstLine?guid:"     ") + " " + ":" +  prop + " " + value + " " + (lastLine? ".":",") + "\n";
//			out.print(statement);
//		}
//	}
//
//
	/**
	 * Generates multiple statements with same subject, and multiple pairs of prop,value.
	 * Value may be not literal if the boolean isRelation is false;
	 * @param out
	 * @param guid
	 * @param isRelation
	 * @param pairs
	 */
//	private void printStatement(PrintStream out, String guid, boolean isRelation, String[]... pairs) {
//		if (0 == pairs.length)
//			return;
//		if (null == guid) guid = "   ";
//			else guid = ":" + guid;
//		for (int i= 0; i < pairs.length; i++) {
//			String[] pair = pairs[i];
//			String prop = pair[0];
//			String value = pair[1];
//			if (!isRelation) {
//				if (value.contains("\n\r"))
//					value = "\"\"" + value + "\"\""; // long text with line breaks.
//				value = "\"" + value + "\"";
//			}
//			boolean lastLine = i == pairs.length -1;
//			boolean firstLine = (0 == i);
//			String statement = (firstLine?guid:"    ") + " " + ":" +  prop + " " + value + " " + (lastLine? ".":";") + "\n";
//			out.print(statement);
//		}
//	}

	/**
	 * handle the exportation of an element
	 * @param elem IRP model element.
	 * @param recursive boolean which
	 */

	/**
	 * Exports the content of the file, being an NT file format to some RDF repository via RESTful API.
	 * @param tmpFile
	 * @throws Exception
	 * @throws IllegalStateException
	 */
	@SuppressWarnings("unused")
	public String export(File tmpFile) throws IllegalStateException, Exception {

		HttpPost httppost = new HttpPost(mServerURL);
		HttpResponse resp = null;
		byte content[] = null;
		OAuthCommunicator aConn = null;
		String msg = "No Msg";
		try {
			content = Utils.readInput((int)tmpFile.length(), new FileInputStream(tmpFile));
			StringEntity entity = new StringEntity(new String(content, "UTF-8"), "UTF-8");
			httppost.setEntity(entity);
			IUserCredentials credentials = getCredentials();
			httppost.addHeader("Content-Type", ContentTypes.RDF_XML);
			try {
				if (credentials.doAuth()) {
					aConn = new OAuthCommunicator(credentials);
					//Send the request and return the response
					resp = aConn.execute(httppost); //DefaultHttpClient().execute(httppost);
				} else {
					DefaultHttpClient conn = new DefaultHttpClient();
					resp = conn.execute(httppost);
				}
			} catch (Exception e) {
				e.printStackTrace();
				msg = "Error [" + e.getClass().getName() + "]: [" + e.getMessage() + "]";
				console(msg);
				return msg;
			}

			// get the results
			if (null == resp) {
				msg =  "Error: Nothing returned.";
				console(msg);
				return msg;
			}
			byte reply[] = null;
			int len = (int) resp.getEntity().getContentLength();
			reply = Utils.readInput(len, resp.getEntity().getContent());

			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				if (reply.length < 1) {
					msg = "OK but no results returned - must be a bad service!!";
					console(msg);
					return msg;
				}
				String responseLines[] = new String(reply).split("\n");
				for (String line : responseLines) {
					String items[] = line.split(" ");
					if (items.length < 2)
						continue;
					String guid = items[0];
					String sii_id = items[1];
					String modified = null;
					if (items.length > 2) {
						modified = items[2];
					}
					String status = "";
					if (items.length > 3)
						status = items[3];
					// Register both relations in the properties
					// guid here is full http URI and it needs to be reduced to GUID as it is done
					// in imports.
					guid = Helper.uri2LegalGUID(guid);
					m_sii_guid_mapping.put(guid, sii_id);
					m_sii_guid_mapping.put(sii_id, guid);
				}
				msg =  "OK. Posting report:\n" + new String(reply);
//				console(msg);
				return msg;
			} else {
				msg = "Error [" + resp.getStatusLine().getStatusCode() + "]. Reason [" + resp.getStatusLine().getReasonPhrase() + "]:\n" + new String(reply) ;
				console(msg);
				return msg;
			}
		} catch (Exception e) {
			e.printStackTrace();
			msg = "Error [" + e.getClass().getName() + " [" + e.getMessage() + "]:\n";
			console(msg);
			System.out.println(msg);
			return msg;
		} finally {
			if (null != aConn)
				aConn.cleanupConnections(resp);
		}
	}

	/**
	 * Performs the actual export, assuming the FMU is the tmpFile, 
	 * @param tmpFile the FMU object
	 * @param resourceUrl
	 * @param flder TODO ??
	 * @return
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	private void exportFmu(IRPModelElement block, File tmpFile, File folder) throws Exception {
		String attachmentUrl = fmuMultipart(tmpFile, block.getName(), "Rhapsody FMU export to SMC for a Block in zip format, and it is tied to a model resource of this block.");
		addFmuProperty(block, attachmentUrl, folder);
	}

	/**
	 * Answers with the URL of the exported FMU as an SMC attachment.
	 * <br> See the attachment tutorial.
	 * @param tmpFile the FMU object.
	 * @param title - String optional title for the FMU object.
	 * @param description - String optional to describe this element.
	 * @return String for the URL of the attachment.
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws OAuthCommunicatorException
	 */
	private String fmuMultipart(File tmpFile, String title, String description) throws ClientProtocolException, IOException, OAuthCommunicatorException {
		MultipartEntity reqEntity = new MultipartEntity();
		//reqEntity.addPart("hasSmResource", new StringBody("https://shani1-tp.haifa.ibm.com:9444/dm/sm/repository/attachments/resource/0006"));
		reqEntity.addPart("set", new StringBody("SmFMU")); // One of the sets of attachments: SmBlob, SmFMU, SmStateChartXMI, SmPicturem, SmMovie.
		if (false == Strings.isNullOrEmpty(title)) 
			reqEntity.addPart("title", new StringBody(title));
		if (false == Strings.isNullOrEmpty(description)) 
			reqEntity.addPart("description", new StringBody(description));
		
		FileBody contents = new FileBody(tmpFile, MediaType.ZIP.toString());
		reqEntity.addPart("attachment-file", contents);
		URL serverUrl = new URL(mServerURL);
		HttpPost httppost = new HttpPost(serverUrl.getProtocol() + "://" + serverUrl.getHost() + ":" + serverUrl.getPort() + ATTACHMENTS_REPOSITORY_PATH);
		httppost.setEntity(reqEntity);

		OAuthCommunicator aConn = null;		
		HttpResponse resp = null;
		try {
			//Send the request and return the response
			IUserCredentials credentials = getCredentials();
			if (credentials.doAuth()) {
				aConn = new OAuthCommunicator(credentials);
				resp = aConn.execute(httppost); //new DefaultHttpClient().execute(httpget);
			} else {
				DefaultHttpClient conn = new DefaultHttpClient();
				resp = conn.execute(httppost);				
			}
			if (resp.getStatusLine().getStatusCode() != 200)
				throw new RhapsodyRuntimeException("Failed call [" + resp.getStatusLine().getReasonPhrase() + "]");
			String url = readContent(resp);
			return url;			
		} catch (Exception e) {
			return null;
		} finally {
			if (null != aConn)
				aConn.cleanupConnections(resp);
		}
	}

	/**
	 * Handling the HTTP response. See the attachment tutorial.
	 * @param resp
	 * @return
	 */
	private String readContent(HttpResponse resp) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream(); int b;
			InputStream input = resp.getEntity().getContent();
			while ((b = input.read()) != -1)
				output.write((byte)b);
			return output.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates an RDF, extend it with triple associating the block with the FMU, and posting the modified model to the SMC (SII). 
	 * @param block element for which FMU is exported.
	 * @param attachmentUrl URL of the remote block element
	 * @param folder File folder of the project work space (project folder).
	 * @throws Exception
	 */
	private void addFmuProperty(IRPModelElement block, String attachmentUrl, File folder) throws Exception {
		/* Export block to a file - implicitly, this includes the block's containment structure (project, package, etc.) */
		File tmpFile = File.createTempFile("fmu-", null, folder); // tmp filie in which RDF is generated.
		tmpFile.deleteOnExit();
		loadProperties(folder);
		doExport(tmpFile, null); // exports a full sub-mode in RDF for the block in focus.

		/* Read exported file back into a Jena model */
		com.hp.hpl.jena.rdf.model.Model model = Helper.loadModel(new FileInputStream(tmpFile));
		tmpFile.delete();

		/* Search for the exported block resource */
		Resource res = model.getResource(IRhpConstants.RHP_INSTANCE_NS + block.getGUID().replaceAll(" ", "-"));

		/* Add/Update the hasFmu property */
		Property hasFmu = model.createProperty(HAS_FMU_PROPERTY);
		res.removeAll(hasFmu);
		res.addProperty(hasFmu, model.getResource(attachmentUrl));

		/* Write out the modified model into a string */
		String modelAsRdfXml = writeModel(model);

		/* Post the modified model */
		HttpPost post = new HttpPost(mServerURL);
		post.setHeader("Content-Type", "application/rdf+xml");
		post.setEntity(new StringEntity(modelAsRdfXml, "UTF-8"));
		OAuthCommunicator comm = new OAuthCommunicator(getCredentials());
		HttpResponse response = comm.execute(post);
		if (response.getStatusLine().getStatusCode() != 200)
			throw new RhapsodyRuntimeException("Failed call [" + response.getStatusLine().getReasonPhrase() + "]");
	}

	/**
	 * Asnwers with the RDF/XML representation on the model in a String. 
	 * @param model
	 * @return String of the RDF model in XML syntax.
	 */
	private String writeModel(com.hp.hpl.jena.rdf.model.Model model) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			model.write(output, "RDF/XML");
			output.close();
			return output.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// ---------------- GUI staff -------------------

	/**
	 * Answers with a file which the user picks up to use
	 * @return File to be used.
	 */
	private File setOutputRdfFile() {
		JFrame frame = new JFrame();
		String fName = m_configProps.getProperty("fName");
		if (null == fName)
			fName = "";
		fName = (String)JOptionPane.showInputDialog(
                frame,
                "Enter File name for RDf output",
                "Output RDF File dialog",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                fName);
		if (null == fName) {
			console( "Cancelled...");
			return null;
		}
		File result = new File(fName);
		if (result.isDirectory()) {
			console(fName + " is a folder. Try again");
			return null;
		}
		if (result.canRead()) {
			console(fName + " exists and Will be overwritten!");
		}
		m_configProps.setProperty("fName", fName);
		return result;
	}

	/**
	 * Answers with a file which the user picks up to use for loading an RDF data.
	 * @return File to be used.
	 */
	private File setInputRdfFile() {
		JFrame frame = new JFrame();
		String fName = m_configProps.getProperty("fName");
		if (null == fName)
			fName = "";
		FileDialog fd = new FileDialog(frame, "Input RDF File Dialog", FileDialog.LOAD);
		fd.setFile(fName);
		fd.setVisible(true);
		fName = fd.getFile();
		if (null == fName) {
			console( "Cancelled...");
			return null;
		}
		fName = new File(fd.getDirectory()).getAbsolutePath() + File.separator + fName;
		File result = new File(fName);
		if (false == result.canRead()) {
			console(fName + " does not exist!");
			return null;
		}
		m_configProps.setProperty("fName", fName);
		return result;
	}


	/**
	 * Sets the rdf sii server url parameter, and senses whether the
	 * user clicked on cancel.
	 * @param function a String title to distinct the reason for issueing this method.
	 * @return boolean that is false if user licked "cancel", true otherwise.
	 */
	public boolean setMRdfServerURL(String function) {
		JFrame frame = new JFrame();
		mServerURL = m_configProps.getRdfServerUrl();
		String s = (String)JOptionPane.showInputDialog(
                frame,
                "Enter SMC server URL",
                function + " SMC dialog",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                mServerURL);
		if (null == s) {
			console( "Cancelled...");
			return false;
		}
		if (s.length() > 0 && s != mServerURL) {
			m_configProps.setRdfServerUrl(s);
			mServerURL = s;
		}
		return true;
	}

//	/**
//	 * Sets the rdf sii cleaner url parameter, and senses whether the
//	 * user clicked on cancel.
//	 * @param function a String title to distinct the reason for issueing this method.
//	 * @return boolean that is false if user licked "cancel", true otherwise.
//	 */
//	private boolean setMRdfCleaningURL(String function) {
//		JFrame frame = new JFrame();
//		mRdfServerCleaningURL = m_configProps.getProperty(RDF_CLEANING_PROPERTY);
//		String s = (String)JOptionPane.showInputDialog(
//                frame,
//                "Enter SMC cleaning URL",
//                function + " SMC dialog",
//                JOptionPane.PLAIN_MESSAGE,
//                null,
//                null,
//                mRdfServerCleaningURL);
//		if (null == s) {
//			console( "Cancelled...");
//			return false;
//		}
//		if (s.length() > 0 && s != mRdfServerURL) {
//			m_configProps.setProperty(RDF_CLEANING_PROPERTY, s);
//			mRdfServerCleaningURL = s;
//		}
//		return true;
//	}


	/**
	 * Sets the source component uri in the sii server, and senses whether the
	 * user clicked on cancel.
	 * @return boolean that is false if user licked "cancel", true otherwise.
	 */
	private boolean setMSiisystemUri() {
		JFrame frame = new JFrame();
		mSiiSystemUri = m_configProps.getSiiSystemUrl();
		String s = (String)JOptionPane.showInputDialog(
				frame,
				"Enter source element URI",
				"Import from SMC dialog",
				JOptionPane.PLAIN_MESSAGE,
				null,
				null,
				mSiiSystemUri);
		if (null== s) {
			console( "Cancelled...");
			return false;
		}
		if (s.length() > 0 && s != mSiiSystemUri) {
			m_configProps.setSiiSystemUrl(s);
			mSiiSystemUri = s;
		}
		return true;
	}

	/**
	 * Finds an IRP model element based on its GUID.
	 * @param guid String of a modified GUID that needs to be adapted back to
	 * the format used by Rhapsody.
	 * @return IRPModelElement or null if non was found.
	 */
	IRPModelElement getFromGuid(String guid) {
		guid = "GUID " + guid.substring(5);
		IRPModelElement element = m_rhpApplication.activeProject().findElementByGUID(guid);
		return element;
	}

	// called when the plug-in popup trigger (if applicable) fired
	public void OnTrigger(String trigger) {

	}

	// called when the project is closed - if true is returned the plugin will
	// be unloaded
	public boolean myRhpPluginCleanup() {
		//JOptionPane.showMessageDialog(null,		"Hello world from SimplePlugin.RhpPluginCleanup");
		//cleanup
		m_rhpApplication = null;
		//return true so the plug-in will be unloaded now (on project close)
		return true;
	}

	// called when Rhapsody exits
	public void myRhpPluginFinalCleanup() {

	}

	/**
	 * Answers with the folder of the project where config data is managed + properties.
	 * Initializes the m_configProps member of this class.
	 * @return File of the current rhapsody project folder.
	 */
	private File setupConfig() {
		IRPProject project = m_rhpApplication.activeProject();
		String fileName = project.getCurrentDirectory();
		File folder = new File(fileName);
		if (null != folder && false == folder.isDirectory())
			folder = folder.getParentFile();
		console("Using folder [" + folder + "].");
		m_configProps = new MyConfigData(folder);
		return folder;
	}
	public static void main(String[] args) throws IllegalStateException, Exception {
		WorkModel.setDebugMode(true);
		String msg = "Hello world from RDF dump plugin init.\n";
		System.out.println(msg);
		//RhpPlugin rhpPlugin = new RhpPlugin();
		final IRPApplication rpyApplication = RhapsodyAppServer.getActiveRhapsodyApplication();
		RhpPlugin.RhpPluginInit(rpyApplication);
		// Selects a menu command to activate
		String menuText = "Export a block";
		while(null != menuText) {
			rhpPlugin.setupConfig();
			String s = rhpPlugin.m_configProps.getProperty("mainMenu");
			if (null != s)
				menuText = s;

			JFrame frame = new JFrame("Enter menu command");
			menuText = (String) JOptionPane.showInputDialog(frame,
					"What command to test?",
					"Menu emulation",
					JOptionPane.QUESTION_MESSAGE,
					null,
					new String[] {"tests", "Export to SMC", "Import from SMC", "Export to FILE", "Import from FILE", "Reset state with SMC", "Export FMU", "SysML"},
					menuText);
			if (null != menuText) {
				rhpPlugin.m_configProps.setProperty("mainMenu", menuText);
				RhpPlugin.OnMenuItemSelect(menuText);
			}
		}

		RhpPlugin.RhpPluginFinalCleanup();
		System.out.println("Done");
		System.exit(0);
//		try {
//			InputStream in = app.getClass().getClassLoader().getResourceAsStream(app.CONFIG);
//			if (null == in)
//				System.out.println("No config data found in class path");
//			else
//				app.mProps.load(in);
//		} catch (FileNotFoundException e) {
////			e.printStackTrace();
//		} catch (IOException e) {
////			e.printStackTrace();
//		}
//		app.mRdfServerURL = "http://localhost:8080/rio-am/rdf/rhapsody"; //(String) app.mProps.get(app.RDF_SERVER_PROPERTY);
//		if (null == app.mRdfServerURL)
//			System.out.println("No server URL provided in the config props file.");
//		else
//			System.out.println(app.export(new File(fn)));
	}

	/**
	 * The services of FMU export are from Lev's plugin. The existence of this plugin can be verified. Like by attempting to load the main API class.
	 * <br>
	 * Note: this was not tested with the 8.0.5 product version of the plugin. 
	 * TODO Perhaps more things should be done here.
	 */
	private boolean testFmuExportPlugin() {
		try {
			Class.forName(FMU_EXPORT_CLASS_NAME);
			mIsFmuExportAvailable = true;
			console ("FMU Export enabled");
			return true;
		} catch (ClassNotFoundException e) {
			console("FMU Export not enabled");
			return false;
		}
	}

	/*
	 * P3 - The element in focus has been exported and is associated with a SM repository. This identifies the target SM tool access point, and the target resource to be used later on in the process: new method in the RhpPlugin:
   Map<String, String> RhpPlugin.canExportTo(IRPModelElement). If the Map is empty - cannot export that FMU. Nees to Export to SMC firstly.

	 */
	/**
	 * Finds SM-repository associations for an element. This identifies the target SM tool access point, and the target resource to be used later on in the process.
	 * If the returned map is empty, cannot export that FMU. Needs to "Export to SMC firstly".
	 *
	 * @param block A Rhapsody model element
	 * @return The keys are URLs of tool access points (in this case, POST ports), and the values are corresponding SM resource URLs associated with the block.
	 * If the block was never exported, or its associations have been cleared, the returned map will be empty.
	 */
	Map<String, String> canExportTo(IRPModelElement block) throws RhapsodyRuntimeException {
		try {
			Map<String, String> associations = new HashMap<String, String>();
			String guid = block.getGUID();
			File folder = setupConfig();
			File [] files = folder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File folder, String file) {
					return file.endsWith(MAP_PROPERTIES_FILE_SUFFIX);
				}
			});
			for (File file : files) {
				Properties props = new Properties();
				props.load(new FileReader(file));
				if (props.containsKey(guid))
					associations.put(props.getProperty(MAP_PROPERTIES_TOOL_NAME), props.get(guid).toString());
			}
			System.out.println(associations);
			return associations;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RhapsodyRuntimeException(e.getMessage());
		}
	}

	//-------------------------- Test area ----------------------------------

	private void doTests(File folder) {
//		IRPProject project = m_rhpApplication.activeProject();
		while (true) {
			IRPModelElement el = m_rhpApplication.getSelectedElement();
			if (el instanceof RPPackage) {
				System.out.println("package " + el.getName());
				RPPackage p = (RPPackage)el;
				for (Object ch : p.getNestedElements().toList()) {
					IRPModelElement che = (IRPModelElement)ch;
					System.out.println("che.getName: " + che.getName());
					System.out.println("che.getDescription: " + che.getDescription());
					System.out.println("che.getUserDefinedMetaClass: " + che.getUserDefinedMetaClass());
				}
			}
			String meta = el.getUserDefinedMetaClass();
			for(Object es : el.getStereotypes().toList()) {
				IRPModelElement mes = (IRPModelElement)es;
				System.out.println(el.getName() + " has stereotype " + mes.getName() + "." );
				if (meta.equals(mes.getName())) {
					System.out.println("--> Not a user attached stereotype!");
					continue;
				}
				System.out.println("mes.getName: " + mes.getName());
				System.out.println("mes.getDescription: " + mes.getDescription());
			}
			System.out.println("Kill me to stop me!");
			synchronized (el) {
				try {
					el.wait(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
