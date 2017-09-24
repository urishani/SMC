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

package com.ibm.haifa.smc.repository;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import java.util.List;

import javax.swing.JFrame;

import org.protege.editor.core.ProtegeManager;
import org.protege.editor.core.editorkit.EditorKitFactoryPlugin;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.ibm.haifa.smc.repository.SMCPanel.SMC_URIListItem;



/**
 * Author: Uri Shani
 * IBM
 */
public class SMCAction extends ProtegeOWLAction {

    /**
     * 
     */
    private static final long serialVersionUID = 8969543617298643589L;


    public void actionPerformed(ActionEvent e) {
        try {
        	JFrame frame = SMCPanel.getInstance().getFrame();
            frame.setExtendedState(Frame.NORMAL);
       		SMCPanel.getInstance().setForAll();
            frame.setVisible(true);
//            handleAction();
        }
        catch (Exception e1) {

            ErrorLogPanel.showErrorDialog(e1);
        }
    }
    
//    /**
//     * Taken from the OWLEditorKit.handleSaveAs() and modified to cojmplyh with the design of the SMC connectivity.
//     * @param ont OW
//     * @return
//     * @throws Exception
//     */
//    private boolean handleAction() throws Exception {
//        return false;
//    }

    private boolean saveOntology() {
    	SMCPanel panel = SMCPanel.getInstance();
    	SMC_URIListItem item = panel.getCurrentItem(); //SMCPanel.showActionDialog(iri);
    	if (null != item) {
    		File file = item.getFile();
    		if (file != null) {
    			OWLEditorKit ek = (OWLEditorKit) getEditorKit();
    			OWLOntology ont = ek.getModelManager().getActiveOntology();
    			OWLOntologyManager man = ek.getModelManager().getOWLOntologyManager();
//    			man.setOntologyFormat(ont, format);
    			man.setOntologyDocumentIRI(ont, IRI.create(file));
    			try {
					ek.getModelManager().save(ont);
				} catch (OWLOntologyStorageException e) {
					e.printStackTrace();
					return false;
				}
 //   			ek.addRecent(file.toURI());
    			boolean success = panel.copyToRemote(item);
    			String msg = "Ontology save " + (success?"succeeded":"failed");
				panel.setLog(msg);
    			panel.setForAll();
    			return success;
    		}
    	}
    	return false;
    }
    
	public void initialise() throws Exception {
    	final SMCPanel panel = SMCPanel.getInstance();
    	panel.addCommandListeners(new SMCPanel.CommandListener(){
    		public void action(int code) {	doAction(code);}
    	});
    	panel.getFrame().setAlwaysOnTop(true);
    }

   	protected void doAction_(int code) {
    	SMCPanel panel = SMCPanel.getInstance();
    	panel.getFrame().setAlwaysOnTop(false);
    	panel.getFrame().validate();
    	panel.setEnabled(false);
		switch (code) {
		default: break;
		case SMCPanel.CommandListener.OPEN: panel.openButton.setEnabled(false);
				try {
//				int answer = JOptionPane.showConfirmDialog(null,  
//						"Open in a new workspace?" ,
//						"Workspace selection", JOptionPane.YES_NO_OPTION);
//
//				if (JOptionPane.YES_OPTION == answer)
//					openInNewWorkspace();
//				else if (JOptionPane.NO_OPTION == answer)
					openInCurrentWorkspace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			panel.openButton.setEnabled(false);
			break;
		case SMCPanel.CommandListener.SAVE: panel.saveButton.setEnabled(false);
			try {
				handleSaveAs();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		panel.setEnabled(true);
	}

   	protected void doAction(final int code) {
   		new Thread() {
   			@Override
   			public void run() {
   				doAction_(code);			
   		    };
   		}.start();
    }


    /**
     * Taken from the OWLEditorKit.handleSaveAs() and modified to cojmplyh with the design of the SMC connectivity.
     * @param ont OW
     * @return
     * @throws Exception
     */
    private void handleSaveAs() throws Exception {
    	 saveOntology();
    }
    
    public void openInNewWorkspace() throws Exception {
        List<URI> uris = getPhysicalURI2Open();
        boolean first = true;
        for (URI uri : uris) {
            if (first) {
            	first = false;
                for (EditorKitFactoryPlugin plugin : ProtegeManager.getInstance().getEditorKitFactoryPlugins()) {
                    if (plugin.getId().equals(getEditorKit().getEditorKitFactory().getId())) {
                        ProtegeManager.getInstance().loadAndSetupEditorKitFromURI(plugin, uri);
                        break;
                    }
                }
            } else
            	getOWLEditorKit().handleLoadFrom(uri);
		}
    }

    private List<URI> getPhysicalURI2Open() {
        return SMCPanel.getInstance().getPhysicalURI2Open();
    }

    public void openInCurrentWorkspace() throws Exception {
        List<URI> uris = getPhysicalURI2Open();
        for (URI uri : uris) {
            getOWLEditorKit().handleLoadFrom(uri);
		}
    }

    public void dispose() {
    }
}
