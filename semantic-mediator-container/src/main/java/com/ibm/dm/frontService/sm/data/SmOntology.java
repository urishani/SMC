
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

package com.ibm.dm.frontService.sm.data;

import java.io.IOException;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.intfc.imp.OntologyDescription;
import com.ibm.dm.frontService.sm.service.ModelRepository;
import com.ibm.dm.frontService.sm.utils.Utils;

@SuppressWarnings("serial")
public class SmOntology extends Ontology {
	@Override
	public boolean canBuild() {
		return false;
	}

	@Override
	public String importModel() {
		return "Error: Cannot import SM Ontology - it is built in."; 
	}

	@Override
	public String loadModel(String contents, String rdfContentType) {
		return "Error: Cannot load SM Ontology - it is built in."; 
	}

	@Override
	public boolean isImported() {
		return true; // that will prevent Protege from attempting to edit this ontology.
	}

	@Override
	public void markModified(long t) {
		return; // do not mark this item as modified. Also, ensure it never says it is "dirty".
	}
	

	@Override
	public boolean isDirty() {
		return false;
	}


	static private SmOntology smOntology = null;
	public static SmOntology create() {
		assert null != smOntology;
//		if (null == smOntology)
//			System.err.println("SmOntology must not be null after startup!");
		return smOntology;
	}
	public static SmOntology create(Database owner, Ontology ont) {
		if (null == smOntology) synchronized (SmOntology.class) {
			if (null == smOntology)
				try {
					smOntology = new SmOntology();
					smOntology.setOwner(owner);
					smOntology.init(ont);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return smOntology;
	}
	private SmOntology() throws IOException {
		super();
		id = ID;
		prefix = "smc";
	}

	final private static String NAME = "SM base ontology";
	public static final String ID = "OntSM";
	public static final String base = "http://com.ibm.ns/haifa/sm#";
	public static final String mimeType = base + "isMimeType";
	public static final String attachment = base + "hasAttachment";
	public static final String attachmentType = base + "hasAttachmentType";

	private void init(Ontology ont) throws IOException {
		// Create it all from the resources in the code:
		String ontology = Utils.loadFromClassPath("ontologies/smOntology.owl");
		Model newM = Utils.modelFromString(ontology, null, null);
		OntologyDescription newOd = OntologyDescription.fromModel(getDatabase(), newM, null);
		setOntologyDescription(newOd);
//		this.ontologyDescription = newOd;
		ModelRepository mr = (ModelRepository)getModelRepository();
		Model oldM = mr.getModel();
		OntologyDescription oldOd = OntologyDescription.fromModel(getDatabase(), oldM, null);
		mr.init(newM, true);
		this.viewConfig = getDefaultViewConfig().toString();

		// updates
		super.dirty = super.dirty || null == ont; // ensure it is not "dirty", unless we initialize it in the first time.
		super.name = NAME;
		super.setArchived(false);
//		version = newOd.getVersionIRI();
//		modelInstanceNamespace = newOd.getBase();
		if (null != ont) {
			dateCreated = ont.getDateCreatedAsLong();
			lastModified = ont.getDateModifiedAsLong();
		}
		if (false == NAME.equals(ont != null?ont.getName():null)) 
			markModified();
		if (null != ont && ont.isArchived()) 
			markModified();
		if (false == oldM.isIsomorphicWith(newM)  || 
				null == oldOd ||
				false == newOd.toString().equals(oldOd.toString())) { // Need to update the ontology
			mr.setDirty();
			mr.save();
			markModified();
		}
		status = ADatabaseRow.STATUS.READY; // no need to make this one not ready just because of that.
//		if (false == isReady()) {
//			markModified();
//		} else
		fileName = "loaded"; // likewise as above.
	}

	public Model getModel() {
		return getOntologyDescription().getModel();
	}
	public OntModel getOntModel() {
		OntModel result = ModelFactory.createOntologyModel();
		result.add(getModel());
		return result;
	}
	
	@Override
	public boolean canImport() { 
		return false; }
	@Override
	public boolean canLoad() { 
		return false; }
	@Override
	public boolean canShow() { 
		return true; }
	@Override
	public boolean isLegal() {
		return true; 
	}
	@Override
	public void setField(String field, String value) {	}
	@Override
	public void setFileName(String fileName) { 
		if (Strings.isNullOrEmpty(this.fileName)) {
			super.setFileName(fileName);
		}
	}
//	@Override
//	public void setOntologyDescription(OntologyDescription ontologyDescription) { }
	@Override
	public void setName(String name) { }
	@Override
	public boolean canClear() {
		return false; }
	@Override
	public boolean canDelete() {
		return false; }
	@Override
	public boolean canEdit() {
		return false; }
	@Override
	public boolean isPermanent() {
		return true; }
	@Override
	public boolean isReady() {
		super.status = ADatabaseRow.STATUS.READY;
		return true; 
	}
//	@Override
//	public boolean isDirty() {
//		return false; }
	
//	@Override
//	protected String migrateFilesForV2_4(ARdfRepository repository, File oldFolder) {
//		String msg = "\nMigrating SmOntology. Renaming ontology.owl* to smontology.owl*\n";
//		File smfolder = getDatabase().getFolder(SmOntology.create(getDatabase(), null));
//		String members[] = smfolder.list();
//		String baseName = "ontology.owl"; //new File(SmOntology.create().fileName).getName();
//		String toBaseName = "sm" + baseName; //new File(repository.getFileName()).getName();
//		for (String member : members) {
//			if (member.startsWith(baseName)) {
//				File from = new File(smfolder, member);
//				msg += "\t Renaming [" + from.getAbsolutePath() + "]: ";
//				File to = new File(smfolder, toBaseName + member.substring(baseName.length()) );
//				boolean done = from.renameTo(to);
//				msg += (done?" successfully ":" failed to ") + "rename to [" + to.getAbsolutePath() + "].\n";
//			}
//		}
//		fileName = repository.getFileName();
//		markModified();
//		return msg;
//	}
	
	public static class vocabulary {
		public static final String uri=base;

	    protected static final Resource resource( String local )
	        { return ResourceFactory.createResource( uri + local ); }
	    protected static final Property property( String local )
        { return ResourceFactory.createProperty( uri + local ); }
	    public static final Property catalogMember = property("catalogMember");
		public static final Property mimeType = property("mimeType");
		public static final Property attachment = property("attachment");
		public static final Property attachmentType = property("attachmentType");
		public static final Property memberRoot = property("memberRoot");;
		public static final Property root = property("root");
	}
}
