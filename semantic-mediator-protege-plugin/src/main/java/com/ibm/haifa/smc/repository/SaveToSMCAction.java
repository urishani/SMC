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

import java.awt.event.ActionEvent;

import org.protege.editor.core.ui.action.ProtegeAction;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;



/**
 * Author: Uri Shani
 * IBM
 */
public class SaveToSMCAction extends ProtegeAction {

    /**
     * 
     */
    private static final long serialVersionUID = 8969543617298643589L;


    public void actionPerformed(ActionEvent e) {
        try {
        	handleSaveAs();
        }
        catch (Exception e1) {

            ErrorLogPanel.showErrorDialog(e1);
        }
    }

    
    private IRI getOwlIri() {
    	OWLEditorKit ek = (OWLEditorKit) getEditorKit();
        OWLOntology ont = ek.getModelManager().getActiveOntology();
        OWLOntologyID id = ont.getOntologyID();
        IRI iri = id.getOntologyIRI();
        return iri;
	}


    private void handleSaveAs() throws Exception {
    	IRI iri = getOwlIri();
    	SMCPanel.showActionDialog(iri);
    }

    public void initialise() throws Exception {
    }


    public void dispose() {
    }
}
