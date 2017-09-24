
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
package com.ibm.rhapsody.sm.plugin;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import com.ibm.rhapsody.plugins.RhpTemplateConfiguration;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RPUserPlugin;
import com.telelogic.rhapsody.core.RhapsodyAppServer;
import com.telelogic.rhapsody.core.RhapsodyRuntimeException;


public class DiscoveryPlugin extends RPUserPlugin {
	private static final String DIS_GEN_TEMPLATE_PREFIX = "./discovery.t";
	private RhpTemplateConfiguration m_cfg;
	private IRPApplication m_app;

	@Override
	public void RhpPluginInit(IRPApplication rpyApplication) {
		// keep the application interface for later use
		// Initialize the FreeMarker configuration;
		// - Create a configuration instance
		m_cfg = new RhpTemplateConfiguration();
		m_app = rpyApplication;
		m_app.writeToOutputWindow("Model Discovery Plugin", "Ready to discover Rhapsody model.\n");
	}

	@Override
	public void RhpPluginInvokeItem() {
        File dir = new File(m_app.activeProject().getCurrentDirectory());
        File project = new File(dir, m_app.activeProject().getFilename());
        String absName = project.getAbsolutePath();
        int dotPos = absName.lastIndexOf('.');
		if(dotPos > 0) {
			String baseFileName = absName.substring(0, dotPos+1);
			try {
				String modFileName = baseFileName + "rdf";
				FileWriter out = new FileWriter(new File(modFileName));

				m_cfg.applyTemplate(new Model(m_app.activeProject(), new Properties()), out, DIS_GEN_TEMPLATE_PREFIX, "rdf");
				m_app.writeToOutputWindow("Model Discovery Plugin", "Your .rdf file has been created succesfully\n");
			} catch (Exception e) {
				System.err.println(m_app.getErrorMessage());
				e.printStackTrace();
				m_app.writeToOutputWindow("Model Discovery Plugin", "Failed: " + e.getMessage() + ", cause: " + m_app.getErrorMessage() + "\n");
			}
        }
	}

	@Override
	public void OnMenuItemSelect(String menuItem) {
	}

	@Override
	public void OnTrigger(String trigger) {
	}

	@Override
	public boolean RhpPluginCleanup() {
		return false;
	}

	@Override
	public void RhpPluginFinalCleanup() {
	}

	static DiscoveryPlugin createDiscoveryPlugin() {
		IRPApplication rhpApp = null;
		try {
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
			}
			DiscoveryPlugin plugin = new DiscoveryPlugin();
			plugin.RhpPluginInit(rhpApp);

			return plugin;
		} catch (Throwable x) {
			if(rhpApp != null)
				System.err.println(rhpApp.getErrorMessage());
			x.printStackTrace();
			return null;
		}
	}
	
	public static void main(String args[]) {
		DiscoveryPlugin plugin = createDiscoveryPlugin();
		plugin.RhpPluginInvokeItem();
	}
}
