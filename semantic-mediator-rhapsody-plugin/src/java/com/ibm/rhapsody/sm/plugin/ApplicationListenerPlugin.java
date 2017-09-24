
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

import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPDiagram;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPProject;
import com.telelogic.rhapsody.core.RPApplicationListener;

/**
 * ApplicationListenerPlugin is a client application that listens Rhapsody
 * for the events defined in RPApplicationListener.
 * ApplicationListenerPlugin is implemented as a Rhapsody plugin,
 * so it contains the methods that are required for a Rhapsody plugin
 * along with the implementation of the methods in RPApplicationListener
 */ 
public class ApplicationListenerPlugin extends RPApplicationListener
{
	static IRPApplication rhpApp = null; // reference to Rhapsody
	
	/**
	 * plugin method called when a Rhapsody plugin is loaded
	 */ 
	public static void RhpPluginInit(final IRPApplication rpyApplication) 
	{
		ApplicationListenerPlugin.rhpApp = rpyApplication;
		
		javax.swing.JOptionPane.showMessageDialog(null, "Initializing ApplicationListenerPlugin");	
		
		// create an instance of ApplicationListenerPlugin
		ApplicationListenerPlugin plugin = new ApplicationListenerPlugin();
		
		// connect to the running Rhapsody instance
		plugin.connect(ApplicationListenerPlugin.rhpApp);
	}
	
	/**
	 * plugin method called when a Rhapsody project is closed
	 */ 
	public static void RhpPluginCleanup()
	{	
		ApplicationListenerPlugin.rhpApp = null;		
	}
	
	/**
	 * plugin method called when Rhapsody exits
	 */ 
	public static void RhpPluginFinalCleanup()
	{
		ApplicationListenerPlugin.rhpApp = null;
	}
	
	/** 
	 * 	the method that is required by RPApplicationListener interface
	 *  and called by Rhapsody to get the ID of the registered client.
	 */ 
	public String getId()
	{
		return "Java ApplicationListener Plug-in";
	}
	
	/** 
	 * 	the method that will be called upon "afterProjectClose" event
	 *  occurs in Rhapsody. For this client, upon afterProjectClose event
	 *  we will tell the user which Rhapsody project is closed
	 * 	According to specification of the afterProjectClose event
	 *	in RPApplicationListener interface, the value returned
	 *	from this method does not matter.
	 */
	public boolean afterProjectClose(String bstrProjectName)
	{
			javax.swing.JOptionPane.showMessageDialog(null, "Project \""
					+ bstrProjectName + "\" is closed", this.getId(), javax.swing.JOptionPane.OK_OPTION);
		return false; // or return true.
	}
	
	/**
	 * the method that will be called upon "beforeProjectClose" event
	 * occurs in Rhapsody. Returned value tells Rhapsody whether
	 * to stop proceeding with "project close" action
	 * For this client, upon that event we want to confirm
	 * if the user surely wants to close the project.
	 */ 
	public boolean beforeProjectClose(IRPProject pProject)
	{
		int answer = 
			javax.swing.JOptionPane.showConfirmDialog(null, "Do you want to close the project \"" + pProject.getFilename() 
						+ "\"", this.getId(), javax.swing.JOptionPane.YES_NO_OPTION);
		if (answer == javax.swing.JOptionPane.NO_OPTION)
			return true; // so Rhapsody will not close the project
		return false; // so Rhapsody will close the project
	}
	
	/**
	 * the method that will be called upon "onDiagramOpen" event
	 * occurs in Rhapsody. Returned value tells Rhapsody whether
	 * to stop proceeding with "diagram open" action.
	 * For this client, upon that event we want to tell the user
	 * what diagram is about to be opened and ask if s/he wants to
	 * stop Rhapsody to open this diagram.
	 */  
	public boolean onDiagramOpen(IRPDiagram pDiagram)
	{
		int answer = 
			javax.swing.JOptionPane.showConfirmDialog(null, "onDiagramOpen event is recieved for \"" 
					+ pDiagram.getFilename()+ "\" \nDo you want Rhapsody to stop openning this diagram?", 
							this.getId(), javax.swing.JOptionPane.YES_NO_OPTION);
		
		if (answer == javax.swing.JOptionPane.YES_OPTION)
			return true; // so Rhapsody will not open the diagram
		return false; // so Rhapsody will open the diagram	
	}
	
	/**
	 * the method that will be called upon "onFeaturesOpen" event
	 * occurs in Rhapsody. Returned value tells Rhapsody whether
	 * to stop proceeding with "features dialog open" action.
	 * For this client, upon that event we want to tell the user
	 * what element's features dialog is about to be opened
	 * and ask if s/he wants to stop Rhapsody to opent that features dialog.
	 */  
	public boolean onFeaturesOpen(IRPModelElement pModelElement)
	{
		int answer = 
			javax.swing.JOptionPane.showConfirmDialog(null, "onFeaturesOpen event is recieved for " 
					+ pModelElement.getMetaClass() + " \"" + pModelElement.getName() 
					+ "\" \nDo you want Rhapsody to stop openning this features dialog?",
					this.getId(), javax.swing.JOptionPane.YES_NO_OPTION);
		
		if (answer == javax.swing.JOptionPane.YES_OPTION)
			return true; // so Rhapsody will open the features dialog
		return false; // so Rhapsody will not open the features dialog
	}

	@Override
	public boolean afterAddElement(IRPModelElement pModelElement) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDoubleClick(IRPModelElement pModelElement) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onSelectionChanged() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
