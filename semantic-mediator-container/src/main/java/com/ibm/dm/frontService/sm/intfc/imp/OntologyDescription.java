
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
 
 
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

package com.ibm.dm.frontService.sm.intfc.imp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.google.common.base.Strings;
import com.ibm.dm.frontService.sm.data.Database;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

import thewebsemantic.Transient;

/**
 * Immutable class to hold description of an ontology model coming from an input stream
 * 
 * @author shani
 */
public class OntologyDescription // extends AbstractRDFReadyClass
{
    /**
     * this is name space of the Ontology that instance this class describes
     */
    protected String          base;
    /**
     * this is version iri of the ontology described
     */
    protected String          version;
    /**
     * TODO: this is transient - jena model of the Ontology. if needed then loaded and discarded on save
     */
    @Transient
    protected transient Model model            = null;
    protected String          rdf              = null;
    protected List<String>    imports          = new ArrayList<String>();
    protected String          label            = "";
    protected String          title            = "";

    protected String          description      = "";

    protected transient Database 	  mOwner;
    
    public OntologyDescription(Database owner)
    {
        super();
        mOwner = owner;
    }

    // Accessors
    public final String getBase()
    {
        return (null == base) ? "" : base;
    }

    /**
     * Answers with the version URI. If that is missing, than the base NS of the ontology
     * is taken rather than answering with an empty string.
     * @return String for the version IRI of the ontology, defaulted to the base of the ontology.
     */
    public final String getVersionIRI()
    {
        return (null == version) ? getBase() : version;
    }

    public final String[] getImports()
    {
        return imports.toArray(new String[0]);
    }

    // Creators

    /**
     * factory method of OntologyDescription based on an input stream of its
     * model contents in RDF/XML format.
     * 
     * @param owner Database owning this model.
     * @param in InputStream of the model contents.
     * @param base base URI for the ontology, or null if no base is needed to parse the input RDF. Deprecated parameter.
     * @param contentType HTTP media type of the model syntax. If null defaulted to application/rdf+xml.
     * @return an ontology description.
     */
    public static OntologyDescription fromStream(Database owner, InputStream in, String base, String contentType)
    {
        if (null == in) return null;
        Model model = null; //ModelFactory.createDefaultModel(); //OntologyModel();
        String type = IConstants.RDF_XML;
        if ( ! Strings.isNullOrEmpty(contentType))
        	type = contentType;
        try
        {
        		model = Utils.modelFromStream(in, contentType, base);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        		return null;
        }
        return fromModel(owner, model, base);
    }

    /**
     * Factory of an OntologyDescription based on a model.
     * 
     * @param model
     *            Model of the ontolgy content from which to build that
     *            description.
     * @return New OntologyDescription for this model.
     */
    public static OntologyDescription fromModel(Database database, Model model, String base)
    {
        OntologyDescription od = new OntologyDescription(database);
        Property rdfType = RDF.type; //model.getProperty(IConstants.RDF_NAMESPACE + "type");
        Resource owlOntology = OWL.Ontology; //model.getResource(IConstants.OWL_ONTOLOGY);
        ResIterator iter = model.listResourcesWithProperty(rdfType, owlOntology);
        Resource ontology = iter.hasNext() ? iter.next() : (Strings.isNullOrEmpty(base)?null: model.getResource(base));

        if (null != ontology)
        {
            od.base = ontology.getURI(); ///*mBase + */ontology//ont.getNameSpace();
            Property versionIRI = OWL.versionInfo;
            Property imports = OWL.imports; //model.getProperty(IConstants.OWL_IMPORTS);
            Statement stmt = model.getProperty(ontology, versionIRI);
            if (null != stmt) od.version = stmt.getObject().toString();
            od.model = model;
            NodeIterator iterImports = model.listObjectsOfProperty(ontology, imports);
            //	ExtendedIterator<OntResource> iIter = ontolgoy.listImports();
            while (iterImports.hasNext())
                od.imports.add(iterImports.next().toString());
            Property label = RDFS.label; //model.getProperty(IConstants.RDFS_LABEL);
            stmt = model.getProperty(ontology, label);
            if (null != stmt) od.label = stmt.getObject().asLiteral().getString().trim();
            Property title = DC.title; //model.getProperty(IConstants.DC_TITLE);
            stmt = model.getProperty(ontology, title);
            if (null != stmt) od.title = stmt.getObject().asLiteral().getString().trim();
            Property description = DC.description; //model.getProperty(IConstants.DC_DESCRIPTION);
            stmt = model.getProperty(ontology, description);
            if (null != stmt) od.description = stmt.getObject().asLiteral().getString().trim();
            return od;
        }
        return null;
    }

    /**
     * Obtain the ontology model of the ontology.
     * 
     * @return OntModel
     */
    public Model getModel()
    {
        return model;
    }

    /**
     * Obtain the RDF-XML String representation of the ontology
     * 
     * @return Sring of the rdf/xml of the ontology.
     */
    public String getRdf()
    {
        if (null != rdf) return rdf;
        if (null == model) return null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        model.write(os);
        rdf = os.toString();
        return rdf;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Base: [" + base + "]\n");
        sb.append("Version: [" + version + "]\n");
        if (false == "".equals(title)) sb.append("Title: [" + getTitle() + "]\n");
        if (false == "".equals(label)) sb.append("Label: [" + getLabel() + "]\n");
        if (false == "".equals(description)) sb.append("Description: [" + getDescription() + "]\n");
        sb.append(imports.size() + " import(s)" + (imports.size() > 0 ? ":" : "") + "\n");
        for (String anImport : imports)
        {
            sb.append("\t[" + anImport + "]\n");
        }
        return sb.toString();
    }

    //		public static String getVersion(Ontology ontology) {
    //			Property versionIRI = ontology.getModel().createProperty( "http://www.w3.org/2002/07/owl#versionIRI" );
    //			Statement stmt = ontology.getProperty(versionIRI);
    //			String version = null;
    //			if (null != stmt)
    //				version = stmt.getObject().asResource().getURI();
    //			if (null == version) {
    //    			Iterator<String> vIter = ontology.listVersionInfo();
    //    			version = vIter.hasNext()?vIter.next():null;
    //			}			
    //			return (null==version)?"":version;
    //		}
    public final String getLabel()
    {
        return label;
    }

    public final String getTitle()
    {
        return title;
    }

    public final String getDescription()
    {
        return description;
    }

    /**
     * Forces the reload of the ontology descriptor with the model from a stream.
     * 
     * @param is
     *            InputStream of the model contents.
     */
    public void load(InputStream is)
    {
        this.model = ModelFactory.createDefaultModel();
        this.model.read(is, this.base);
    }

    public void load(Model m)
    {
        this.model = ModelFactory.createDefaultModel();
        this.model.add(m);
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((base == null) ? 0 : base.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((imports == null) ? 0 : imports.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((rdf == null) ? 0 : rdf.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        OntologyDescription other = (OntologyDescription) obj;
        if (base == null)
        {
            if (other.base != null) return false;
        }
        else
            if (!base.equals(other.base)) return false;
        if (description == null)
        {
            if (other.description != null) return false;
        }
        else
            if (!description.equals(other.description)) return false;
        if (imports == null)
        {
            if (other.imports != null) return false;
        }
        else
            if (!imports.equals(other.imports)) return false;
        if (label == null)
        {
            if (other.label != null) return false;
        }
        else
            if (!label.equals(other.label)) return false;
        if (rdf == null)
        {
            if (other.rdf != null) return false;
        }
        else
            if (!rdf.equals(other.rdf)) return false;
        if (title == null)
        {
            if (other.title != null) return false;
        }
        else
            if (!title.equals(other.title)) return false;
        if (version == null)
        {
            if (other.version != null) return false;
        }
        else
            if (!version.equals(other.version)) return false;
        return true;
    }

}

