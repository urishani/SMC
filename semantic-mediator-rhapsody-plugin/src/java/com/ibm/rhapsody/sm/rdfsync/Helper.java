
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

package com.ibm.rhapsody.sm.rdfsync;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.ibm.rhapsody.sm.IRhpConstants;
import com.ibm.rhapsody.sm.plugin.ModelHelpers;
import com.ibm.rhapsody.sm.plugin.RhpPlugin;
import com.telelogic.rhapsody.core.IRPAttribute;
import com.telelogic.rhapsody.core.IRPClass;
import com.telelogic.rhapsody.core.IRPClassifier;
import com.telelogic.rhapsody.core.IRPCollection;
import com.telelogic.rhapsody.core.IRPEvent;
import com.telelogic.rhapsody.core.IRPEventReception;
import com.telelogic.rhapsody.core.IRPFlow;
import com.telelogic.rhapsody.core.IRPInstance;
import com.telelogic.rhapsody.core.IRPLink;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPOperation;
import com.telelogic.rhapsody.core.IRPPackage;
import com.telelogic.rhapsody.core.IRPPort;
import com.telelogic.rhapsody.core.IRPProject;
import com.telelogic.rhapsody.core.IRPStereotype;
import com.telelogic.rhapsody.core.IRPSysMLPort;
import com.telelogic.rhapsody.core.IRPTag;
import com.telelogic.rhapsody.core.IRPType;
import com.telelogic.rhapsody.core.RPInstance;
import com.telelogic.rhapsody.core.RhapsodyAppServer;
import com.telelogic.rhapsody.core.RhapsodyRuntimeException;

public class Helper {
	static final Set<String> ANNOTATIONS = new HashSet<String>(Arrays.asList(
			new String[] {
				"Consraint", "Requirement", "Comment"	
			}));
	
	static final Set<String> NON_OWNING_RELATIONS = new HashSet<String>(Arrays.asList(
			new String[] {
				    "hasRequired", "hasContract", "hasProvided", "hasAttributeType", "hasPortType", "hasType", "hasReturnType", "hasOnEvent",
				    "hasPort_1", "hasPort_2", "hasEndPoint_1", "hasEndPoint_2", IRhpConstants.SM_PROPERTY_SM_RESOURCE_FULL,
				    "hasStereotype", "hasTagType", "hasInstanceValue", "hasAnchor", "hasAssociation"}));
	static final Set<String> POST_PROCESSING_RELATIONS = new HashSet<String>(Arrays.asList(
			new String[] { 
					"hasLink", "hasConnector", "hasAnchor", "hasAssociation"
			}));
	static final Set<String> GLOBAL_COMPONENTS = new HashSet<String>(Arrays.asList(
			new String[] {"Object", "Variable", "Function"}));
	
	public static final Map<String, String> multiplicityLUT = new HashMap<String, String>();
	public static final Map<String, String>  visibilityLUT = new HashMap<String, String>();
	public static Map<String, String> flowDirectionLUT = new HashMap<String, String>();
	public static Map<String, String> portDirectionLut = new HashMap<String, String>();

	// Initialize the statics
	static
	{
		multiplicityLUT.put("AnyMultiplicity", "*");
		multiplicityLUT.put("ExactlyOneMultiplicity", "1");
		multiplicityLUT.put("AtMostOneMultiplicity", "0,1");
		multiplicityLUT.put("OneOrMoreMultiplicity", "1..*");

		visibilityLUT.put("PublicVisibility", "public");
		visibilityLUT.put("PrivateVisibility", "private");
		visibilityLUT.put("ProtectedVisibility", "protected");

		flowDirectionLUT.put("Bidirectional", "bidirectional");
		flowDirectionLUT.put("ToEnd1Direction", "toEnd1");
		flowDirectionLUT.put("ToEnd2Direction", "toEnd2");

		portDirectionLut.put("BidirectionalPort", "InOut");
		portDirectionLut.put("OutputPort", "Out");
		portDirectionLut.put("InputPort", "In");
	}

	public static Set<String> includedProfiles = null;
	static {
		includedProfiles = Sets.newHashSet(new String[] {"SysML" });
	}
	//======================= methods for Stereotypes ===============================
	
	/**
	 * Answer with all the relevant stereotypes for an element
	 * @param e - an {@link IRPModelElement} to analyze.
	 * @return List of {@link IRPStereotype}-s, except for the built in one of that element.
	 */
	public static List<IRPStereotype> getProfiledStereotypes(IRPModelElement e)
    {
 
        List<IRPStereotype> result = new ArrayList<IRPStereotype>();
        if (e == null) return result;

        IRPCollection stereotypes = e.getStereotypes();
    
        // Now remove those that are not from a profile.

        for (Object o : stereotypes.toList())
        {
            IRPStereotype s = (IRPStereotype) o;
            if (ModelHelpers.isIncludedStereotype(s))
            	continue;
            result.add(s);
        }
        return result;
    }

	/**
	 * Answers with a specific stereotype in an element, by the stereortype GUID
	 * @param e - an {@link IRPModelElement} to be analyzed.
	 * @param stereotypeGuid - String of the GUID to look up.
	 * @return an {@link IRPStereotype} if one with that GUID exists.
	 */
    public static IRPStereotype findStereotype(IRPModelElement e, String stereotypeGuid)
    {
        if (e == null || stereotypeGuid == null) return null;

        IRPCollection stereotypes = e.getStereotypes();
        for (Object o : stereotypes.toList())
        {
            IRPStereotype s = (IRPStereotype) o;
            if (stereotypeGuid.equals(s.getGUID())) return s;
        }
        return null;
    }

    /**
     * Answers with a stereotype in possibly a certain package
     * @param project - an {@link IRPProject} to be searched.
     * @param profileName - the name of a package in that project which is a profile of some stereotypes.
     * @param stereotypeName - String name of the stereotype we are looking for.
     * @return an {@link IRPStereotype} matching all the parameter conditions.
     */
    public static IRPStereotype getPluginStereotype(IRPProject project, String profileName, String stereotypeName) {
        return getPluginElement(project, profileName, stereotypeName, "Stereotype");
    }

    /**
     * A generic service to look up certain meta-class elements in a profile.
     * @param project - an {@link IRPProject} to be searched.
     * @param profileName - the name of a package in that project which is a profile of some stereotypes.
     * @param instanceName - String name of the meta-class instance we are looking for.
     * @param metaClass - string of the meta-class of the sought element.
     * @return an {@link IRPModelElement} matching the parameters.
     */
	@SuppressWarnings("unchecked")
	public static <T extends IRPModelElement> T getPluginElement(IRPProject project, String profileName, String instanceName, String metaClass) {
		IRPModelElement owner = profileName == null ? project : project.findNestedElement(profileName, "Package");
        if (owner == null) return null;

		return (T) owner.findNestedElement(instanceName, metaClass);
	}
	
	//============================= End of Stereotypes =====================================
	
    public static Model loadModel(String inputFileName) {
    	// use the FileManager to find the input file
    	InputStream in = FileManager.get().open( inputFileName );
    	if (in == null) {
    	    throw new IllegalArgumentException(
    	                                 "File: " + inputFileName + " not found");
    	}

    	return loadModel(in);
    }
	public static Model loadModel(InputStream in) {
		// create an empty model
		try {
			Model model = ModelFactory.createDefaultModel();
//			try {
//				int l = -1;
//				while ((l = in.read(buff)) >= 0) {
//					System.out.println(new String(buff, 0, l));
//				}
			model.read(in, null);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			model.write(System.out);
//			Resource a12 = model.getResource("https://10.110.40.53:9444/dm/sm/repository/ra/resource#12");
//			StmtIterator iter = a12.listProperties();
//			boolean ex = iter.hasNext();
//			Resource a3 = model.getResource("https://10.110.40.53:9444/dm/sm/repository/ra/resource#3");
//			iter = a3.listProperties();
//			ex = iter.hasNext();
			return model;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
		// read the RDF/XML file
	}
	public interface URITransform {
		// May return null, it means no transformation available
		String transform(String uri);
	}
	public static class PrefixURITransform implements URITransform {
		String srcPrefix, tgtPrefix;
		public PrefixURITransform(String srcPrefix, String tgtPrefix) {
			this.srcPrefix = srcPrefix;
			this.tgtPrefix = tgtPrefix;
		}

		@Override
		public String transform(String uri) {
			if (!uri.startsWith(srcPrefix))
				return null;
			return uri.replace(srcPrefix, tgtPrefix);
		}
	}

    public static Map<String, String> extractIdMap(Model model, URITransform uriTransform) {
    	Map<String, String> idMap = HashBiMap.create();
    	ResIterator iter = model.listSubjects();
    	while (iter.hasNext()) {
    	    Resource r = iter.nextResource();
    	    String uri = r.getURI();
    	    String transformedURI = uriTransform.transform(uri);
    	    if(transformedURI != null) {
				idMap.put(uri, transformedURI);
	    	    System.out.println("Resource with URI: " + uri + " mapped to " + transformedURI);
    	    } else {
	    	    System.err.println("Resource with URI: " + uri + " was not mapped.");
    	    }
    	}
    	return idMap;
    }
    public static Map<String, String> transformURIs(Model model, URITransform uriTransform) {
    	Map<String, String> idMap = HashBiMap.create();
    	ResIterator iter = model.listSubjects();
    	while (iter.hasNext()) {
    	    Resource r = iter.nextResource();
    	    String uri = r.getURI();
    	    String transformedURI = uriTransform.transform(uri);
    	    if(transformedURI != null) {
    			idMap.put(uri, transformedURI);
        	    ResourceUtils.renameResource(r, transformedURI);

        	    System.out.println("Resource with URI: " + uri + " renamed to " + transformedURI);
    	    } else {
	    	    System.err.println("Resource with URI: " + uri + " was not renamed.");
    	    }
    	}

    	return idMap;
    }
    public static void syncRDF2IRP(Model model, IRPProject p, Properties props, RhpPlugin rhpPlugin) throws Exception {
	    System.out.println("Resources with rdf:type :");

	    Collection<Resource> resourcesWithType = getResourcesWithType(model);
    	for (Resource r : resourcesWithType) {
			String typeURI = r.getProperty(RDF.type).asTriple().getObject().getURI();
			System.out.println("Resource with URI: " + r.getURI() + ", type: " + typeURI);
    	}
    	Collection<Resource> connectorOwners = importRDF(resourcesWithType, p, props, true, false, rhpPlugin);  // First, recreate structure - "owning" relations
    	importRDF(resourcesWithType, p, props, false, false, rhpPlugin); // Then, recreate cross-references
    	importRDF(connectorOwners, p, props, true, true, rhpPlugin);  // First, recreate structure - "owning" relations
    	importRDF(connectorOwners, p, props, false, true, rhpPlugin); // Then, recreate cross-references
    }

    private static List<Resource> getResourcesWithType(Model model) {
		ResIterator resourcesWithType = model.listResourcesWithProperty(RDF.type);
		List<Resource> list = new ArrayList<Resource>();
		while (resourcesWithType.hasNext())
			list.add(resourcesWithType.next());
		return list;
    }

    private static class MyLinkedList<r> extends LinkedList<r> {
		private static final long serialVersionUID = 1L;
		@Override
		public void addFirst(r element) {
			System.out.println("************ Adding [" + element.toString() + "]");
			if (element.toString().startsWith("http://com.ibm.ns/rhapsody/haifa/sm#"))
				System.err.println("----- debug MyLinkedList.addFirst(" + element + ")");
			super.addFirst(element);
		}

		@Override
		public boolean add(r element) {
			System.out.println("************ Adding [" + element.toString() + "]");
			if (element.toString().startsWith("http://com.ibm.ns/rhapsody/haifa/sm#"))
				System.err.println("----- debug MyLinkedList.add(" + element + ")");
			return super.add(element);
		}
		
	};
	private static Collection<Resource> importRDF(Collection<Resource> resources, IRPProject p, Properties props, boolean processOwning, boolean postProcessing, RhpPlugin rhpPlugin) 
		throws Exception {

		LinkedList<Resource> processingQueue = new MyLinkedList<Resource> (); // Assumption: in queue, everyone has counterpart in Rhapsody already, and in idMap
    	Set<Resource> processedResources = new HashSet<Resource>(); // Never add a resource twice to the queue

    	for (Resource r : resources) {
			String typeURI = r.getProperty(RDF.type).asTriple().getObject().getURI();
//			System.out.println("Resource with URI: " + r.getURI() + ", type: " + typeURI);
			if(postProcessing == false) {
				if (typeURI.equals(IRhpConstants.SysML) || 
						typeURI.equals(IRhpConstants.Project) || 
						typeURI.equals(IRhpConstants.SOS)) {
					processingQueue.addFirst(r);
					props.setProperty(r.getURI(), p.getGUID());
					props.setProperty(p.getGUID(), r.getURI());
				}
			} else {
				if (processedResources.add(r))
					processingQueue.add(r);
			}
		}

    	Set<Resource> connectorOwners = new HashSet<Resource>();

    	while(!processingQueue.isEmpty()) {
    		Resource r = processingQueue.pollLast();
    		if (r.toString().contains("/0312"))
    			System.err.println("Working on resource /0312");
    		String guid = props.getProperty(r.getURI());
    		IRPModelElement modelElement = p.findElementByGUID(uri2LegalGUID(guid));
    		if(modelElement == null) {
    			System.err.println("OMG: assumption failed, ModelElement not in the Rhapsody project for resource:" + r.getURI());
    			continue;
    		}
    		System.out.println("<< Element from queue: name=" + modelElement.getName() + ", guid=" + guid + ", uri=" + r.getURI());
//    		if (null != r && r.getURI().indexOf("0077") >= 0)
//    			System.err.println("----- debugging 0077");

			StmtIterator properties = r.listProperties();
			while(properties.hasNext()) {
				Statement statement = properties.nextStatement();
				System.out.println("property: " + statement.asTriple().getPredicate() + " = " + (statement.asTriple().getObject().isURI()?"uri:":statement.asTriple().getObject().isLiteral()?"lit:":"unk") + statement.asTriple().getObject());
				if (statement.asTriple().getPredicate().toString().contains("hasStereotype")) {
					System.err.println("---- debugging hasStereotype");
//					Node n = statement.asTriple().getPredicate();
//					String ns = n.getURI();
//					System.err.println(ns);
				}
				RDFNode obj = statement.getObject();
				String predicateUri = statement.asTriple().getPredicate().getURI();
//				if (predicateUri.contains("PortDirection"))
//					System.err.println("+++ debug PortDirection");
		    	if (predicateUri.endsWith("hasValue"))
	    			System.err.println("Debugging hasValue");

				if(obj.isLiteral()
						|| isPseudoRef(ontologyUri2MetaType(predicateUri))) { // Attribute
					restoreIRPPropertyFromRDF(modelElement, statement, rhpPlugin);
//				} else if (ModelHelpers.isOntologicalTerm(obj)) { // process the Ontological terms
//					// Nothing to do 
//					System.out.println("Ontological property [" + obj.toString() + "] - not a relation");
				} else { // Relation
					// Next block distinguishes connectors or non-connectors in the 4 passes as follows:
					// Process connectors only in post processing and others in the prev. passes.

					boolean processNow = false;

					if (postProcessing == false) {
						// 1. Process everything except connectors
						// 2. Add connectors to deferred list, but on first pass only (i.e., processOwning)
						boolean isconnector = isConnector(modelElement, statement);
						if (isconnector && processOwning) {
								connectorOwners.add(r);
						}
						else {
							// All resources *except* connector owners fall here
							processNow = true;
						}
					}
					else { // postProcessing == true
						// At post processing, the list of resources contains only connector-owners
						processNow = true;
					}

					if (processNow) {
						Resource other = restoreIRPRelationFromRDF(modelElement, statement, props, processOwning, postProcessing, rhpPlugin);
//						if (null != other && ModelHelpers.isStereotypeResource(other)) { // check if this is a local stereotype, or a referenced one. If neither - issue an error and skip it.
//							// if the stereotype is local to the project, we can find its element
//							String GUID = props.getProperty(other.getURI());
//							if (null == GUID) { s it a referenced stereotype?
//								Statement stmt = other.getProperty(other.getModel().getProperty(IRhpConstants.GUID));
//								other = null;
//								if (null != stmt) {
//									String sGuid = stmt.getObject().toString();
//									IRPModelElement s = null;
//									s = p.findElementByGUID(sGuid);
//									if (null == s) {
//										console(rhpPlugin, "Stereotype [GUID " + sGuid + "] Not found in project. Must load the library first. Aborting.");
//										throw new Exception("Aborting due to missing referenced stereotype.");
//									}
//								}
//								
//							}
//						}
						if(other != null)
							if (processedResources.add(other))
								processingQueue.addFirst(other);
					}
				}
			}
    	}

    	return connectorOwners;
	}

	private static boolean isConnector(IRPModelElement modelElement, Statement stmt) {
		try {
			if (stmt.toString().contains("0312"))
				System.err.println("isConnector 0312");
	    	Model model = stmt.getModel();
	    	String otherObjectURI = stmt.asTriple().getObject().getURI();
	    	Resource otherResource = model.getResource(otherObjectURI);
	    	Statement stmtOtherType = otherResource.getProperty(RDF.type);
	    	String otherTypeURI = stmtOtherType.asTriple().getObject().getURI();
			String otherMetaType = ontologyUri2MetaType(otherTypeURI);
			boolean result = "Connector".equals(otherMetaType) || "Link".equals(otherMetaType);
			if (result) {
				System.out.println("modelElement.name=[" + modelElement.getName() + "]");
				System.out.println("Is connector's (or link) stmt: [property: " + stmt.asTriple().getPredicate() + " = " + (stmt.asTriple().getObject().isURI()?"uri:":stmt.asTriple().getObject().isLiteral()?"lit:":"unk") + stmt.asTriple().getObject() + "]");
			}
			return result;
		} catch (Throwable t) {
			return false;
		}
	}

    private static Resource restoreIRPRelationFromRDF(
			IRPModelElement modelElement, Statement stmt,
			Properties props, 
			boolean processingOwning, boolean postProcessing, 
			RhpPlugin rhpPlugin) {
    	if(stmt.getPredicate().equals(RDF.type))
    		return null; // Don't process type assignments, they are processed on construction
    	IRPProject project = modelElement.getProject();
    	Model model = stmt.getModel();
    	String relationURI = stmt.asTriple().getPredicate().getURI();
    	if (relationURI.endsWith("hasValue"))
			System.err.println("Debugging hasValue");
    	if (relationURI.contains("Type"))
    		System.err.println("Debugging hasXXXType [" + relationURI + "]");
    	String relation = ontologyUri2MetaType(relationURI);
    	String otherObjectURI = stmt.asTriple().getObject().getURI();
    	if(isPseudoRef(relation)) {
        	restoreAssignment(modelElement, relation, otherObjectURI, rhpPlugin, stmt);
        	return null;
    	}

    	Resource otherResource = null;
    	IRPModelElement predefinedElement = null;
    	if (Helper.isOntologyUri(otherObjectURI)) { // it is an ontology resource.
    		otherResource = model.getResource(otherObjectURI);
    		predefinedElement = findPredefinedType(project, Helper.ontologyUri2MetaType(otherObjectURI));
    	} else {
    		otherResource = Helper.getInstanceResouce(model, otherObjectURI); 
    	}
    	if (Helper.isImplicitType(otherObjectURI)) // ignore this triple.
    		return null;

    	 

    	if(null == otherResource && null == predefinedElement) // Attempt to establish a relation to a non-resolved URI. probably a "literal" URI, like RDF.type
    		return null;

    	String otherGUID = null;
    	IRPModelElement otherElement = null;
    	
    	/*
    	 * There are 4 phases:
    	 * Phase 1 - create owned elements according to ownership relations. But not links.
    	 * Phase 2 - work out references with the non-ownership relations to elements already created. But not links.
    	 * Phase 3+4: work with elements which have links. These are ownership relations.
    	 * Phase 3 - create owned links.
    	 * Phace 4 - work on filling all the other relations within links.
    	 */
    	boolean inPhase1 = processingOwning && !postProcessing ;
    	boolean inPhase2 = !processingOwning && !postProcessing ;
    	boolean inPhase3 = processingOwning && postProcessing ;
    	boolean inPhase4 = !processingOwning && postProcessing ;
    	int phase = inPhase1?1:inPhase2?2:inPhase3?3:inPhase4?4:99;
    	
    	boolean postProcessingRelation = POST_PROCESSING_RELATIONS.contains(relation);
    	boolean referenceRelation = NON_OWNING_RELATIONS.contains(relation);
    	
    	boolean resolveReferences = inPhase2 && ! postProcessingRelation && referenceRelation ||
    		inPhase4 && referenceRelation;
    	boolean avoidReferences = (inPhase1 || inPhase3) && ! postProcessingRelation && referenceRelation;
    	
    	boolean constructElements = inPhase1 && !postProcessingRelation && !referenceRelation ||
    	    inPhase3 && postProcessingRelation;
    	boolean traverseOnly = !referenceRelation && (inPhase2 || inPhase4);
    	boolean handleProfiledEntities = false;
//		if (otherResource.toString().endsWith("017"))
//			System.err.println("++++++++ Debugging object 017");
//		if (thisElement.toString().endsWith("017"))
//			System.err.println("++++++++ Debugging element 017");
    	if (relationURI.endsWith("hasStereotype") && (inPhase1 || inPhase3) || relationURI.endsWith("hasTagType") && (inPhase2 || inPhase4)) {
    		otherGUID = ModelHelpers.isProfiledStereotypeResource(otherResource);
    		handleProfiledEntities = (null != otherGUID);
    	}
    	String otherUriNum = otherObjectURI.substring(otherObjectURI.lastIndexOf("/"));
//    	if (otherUriNum.contains("0312"))
//    		System.err.println("Found uri /0312");
    	System.out.println("::: [" + modelElement.getFullPathName() + "] --[" + relation + "]--> [" + otherUriNum + "]" +
    			(resolveReferences?"resolve":constructElements?"construct":handleProfiledEntities?"linkWprofiledEntities":traverseOnly?"traverse":"doNothing") + ": Phase [" + phase + "]");
//    	if (relation.endsWith("hasStereotype"))
//    		System.err.println("+++ debug hasStereotype");


//    	if (modelElement.getFullPathName().contains("relativeQuantity"))
//    		System.err.println(modelElement.getFullPathName());
   		
		Statement stmtOtherType = null;
		if (null != otherResource)
			stmtOtherType = otherResource.getProperty(RDF.type);
		
    	if (inPhase1 && null == stmtOtherType) { // Attempt to establish a relation to a type-less object. probably a "literal" URI, like RDF.type, or a reference to a predefined individual
//        		IRPModelElement predefinedElement = findPredefinedType(project, otherObjectURI);
//        		String n = predefinedElement.getName();
        		if (predefinedElement != null)
                	restoreAssignment(modelElement, relation, predefinedElement, rhpPlugin, stmt);
        		return null;
    	}

    	
    	if (constructElements && ModelHelpers.isOntologicalTerm(otherResource)) // Ontological terms need not be processed, like built in types.
    		return null; // no further processing at this time.
    	if (handleProfiledEntities) {//otherGUID = ModelHelpers.isProfiledStereotypeResource(otherResource);
    		//if (null != otherGUID) {  // work with items that are not owned by the project and are defined
    		// in referenced modules such as profiles.
    		otherElement = project.findElementByGUID(uri2LegalGUID(otherGUID));
    		if (null == otherElement) {
    			String fullName = "unknown";
    			stmt = otherResource.getProperty(model.getProperty(IRhpConstants.HAS_QUALIFIED_NAME));
    			if (null != stmt)
    				fullName = stmt.getObject().toString();
    			console(rhpPlugin, "Warning: Profiled resource [" + fullName + "] not in project. Add appropriate profile to get full restore of imported model.");
    			return otherResource;
    		} else {
    			props.setProperty(otherElement.getGUID(), otherObjectURI);
    			props.setProperty(otherObjectURI, otherElement.getGUID());
    			restoreAssignment(modelElement, relation, otherElement, rhpPlugin, stmt);
    			
    			return null; // nothing to do after that.
    		}
    	} 
 

    	if (resolveReferences) {
    		otherGUID = props.getProperty(otherObjectURI);
    		if (null == otherGUID) {
    			System.err.println("OMG: cannot resolve reference to [" + otherResource + "]");
    			return null;
    		}
    		otherElement = project.findElementByGUID(uri2LegalGUID(otherGUID));
    		if (null == otherElement) {
    			System.err.println("OMG: cannot resolve reference to [" + otherResource + "] with GUID [" + otherGUID + "]");
    			return null;
    		}
        	restoreAssignment(modelElement, relation, otherElement, rhpPlugin, stmt);
    		return otherResource;
    	}
    	if (traverseOnly)
    		return otherResource;
    	if (avoidReferences)
    		return null;
    	
    	// From here, we work on constructing elements.
 
    	StmtIterator iter = otherResource.listProperties();
    	while (iter.hasNext()) {
    		Statement stmt2 = iter.next();
    		System.out.println(stmt2);
    	}
    	String otherTypeURI = stmtOtherType.asTriple().getObject().getURI();
    	String otherMetaType = ontologyUri2MetaType(otherTypeURI);
    	Statement titleStatement = otherResource.getProperty(DCTerms.title);
    	String otherName = null;
    	if (null != titleStatement)
    		otherName = otherResource.getProperty(DCTerms.title).asTriple().getObject().getLiteral().toString(false);
    	if (null == otherGUID)
    		otherGUID = props.getProperty(otherObjectURI);
    	if(otherGUID != null && null == otherElement)
    		otherElement = project.findElementByGUID(uri2LegalGUID(otherGUID)); // Rhapsody element needs to be created

//		if(otherElement != null && (!otherElement.getUserDefinedMetaClass().equalsIgnoreCase(otherMetaType) || (processingOwning && !postProcessing && !otherElement.getOwner().equals(modelElement)))) {
//			// Mappings are outdated, cleanup
//			props.remove(otherObjectURI);
//			props.remove(otherGUID);
//			otherElement = null;
//			System.err.println("--- Removing mapping [" + otherObjectURI + "] <--> [" + otherGUID + "]");
//		}
		// Attempt to associate by name and type, and then fix mappings
		if(null == otherElement && null != otherName) {
			if((otherElement = modelElement.findNestedElement(otherName, otherMetaType)) != null) {
	    		props.setProperty(otherElement.getGUID(), otherObjectURI);
	    		props.setProperty(otherObjectURI, otherElement.getGUID());
			}
		}

    	if(null == otherElement) {
    		// Create new child
    		try {
//    			if (!processingOwning) {
//    				throw new Exception("Expected Object element does not exist in relations making phase. Ignored.");
//    	    					"\tTherefore: Failed to make a match for statement: [" + stmt + "], and ignoring it.");
//    	    			return null;
//    	    		}
    			if("Reception".equals(otherMetaType))
        			otherElement = ((IRPClass)modelElement).addEventReception(otherName);
    			else if ("Link".equals(otherMetaType)) {
//    				if (!postProcessing) 
//    					return otherResource;
    				// Find end port elements
    				String port1URI = null, port2URI = null;
    				String part1URI = null, part2URI = null;
    				StmtIterator properties = otherResource.listProperties();
    				while(properties.hasNext()) {
    					Statement statement = properties.nextStatement();
    					String propURI = statement.asTriple().getPredicate().getURI();
    					if(propURI.endsWith("hasPort_1"))
    						port1URI = statement.asTriple().getObject().getURI();
    					if(propURI.endsWith("hasPort_2"))
    						port2URI = statement.asTriple().getObject().getURI();
    					if(propURI.endsWith("hasEndPoint_1"))
    						part1URI = statement.asTriple().getObject().getURI();
    					if(propURI.endsWith("hasEndPoint_2"))
    						part2URI = statement.asTriple().getObject().getURI();
    				}
    				// Assumption: all referenced elements are already restored, and their URIs mapped with GUIDS
    				// Guaranteed by "hasLink" in NON_OWNING_RELATIONS
    				IRPModelElement o1 = null;
    				if (null != port1URI)
    					o1 = project.findElementByGUID(uri2LegalGUID(props.getProperty(port1URI)));
    				IRPModelElement o2 = null;
    				if (null != port2URI)
    					o2 = project.findElementByGUID(uri2LegalGUID(props.getProperty(port2URI)));
    				IRPModelElement p1 = null;
    				if (null != part1URI)
    					p1 = project.findElementByGUID(uri2LegalGUID(props.getProperty(part1URI)));
    				IRPModelElement p2 = null;
    				if (null != part2URI)
    					p2 = project.findElementByGUID(uri2LegalGUID(props.getProperty(part2URI)));
    				IRPPort port1 = null, port2 = null; 
    				IRPInstance part1 = null, part2 = null; // No proper serialization into RDF
    				if ((null != o1) && ModelHelpers.isObject(o1) || (null != o2) && ModelHelpers.isObject(o2)) {
    					part1 = (RPInstance)o1; part2 = (RPInstance)o2;
    				} else {
    					port1 = (IRPPort)o1; port2 = (IRPPort)o2;
    					if (null != p1 && null != p2) {
    						part1 = (RPInstance)p1; part2 = (RPInstance)p2;
    					}
    				}
    				// Note correct interpretation of endpoints 1 and 2 per link/connection directions:
    				// endpoint 1 is from-end point.
    				// endpoint 2 is to-end point.
    				if (ModelHelpers.isIRPPackage(modelElement))
    					otherElement = ((IRPPackage)modelElement).addLink(part1, part2, null, port1, port2);
    				else
    					otherElement = ((IRPClass)modelElement).addLink(part1, part2, null, port1, port2);
    				if(!Strings.isNullOrEmpty(otherName))
    					otherElement.setName(otherName);
    			} else if ("Connector".equals(otherMetaType)) {
    				if (!postProcessing) return null;
    				// Find end port elements
    				String part1URI = null, part2URI = null;
    				String port1URI = null, port2URI = null;
    				StmtIterator properties = otherResource.listProperties();
    				while(properties.hasNext()) {
    					Statement statement = properties.nextStatement();
    					String propURI = statement.asTriple().getPredicate().getURI();
    					if(propURI.endsWith("hasEndPoint_1"))
    						part1URI = statement.asTriple().getObject().getURI();
    					if(propURI.endsWith("hasEndPoint_2"))
    						part2URI = statement.asTriple().getObject().getURI();
    					if(propURI.endsWith("hasPort_1"))
    						port1URI = statement.asTriple().getObject().getURI();
    					if(propURI.endsWith("hasPort_2"))
    						port2URI = statement.asTriple().getObject().getURI();
    				}

    				String
    				guid = (null == port1URI)?null:uri2LegalGUID(props.getProperty(port1URI));
    				IRPModelElement port1 = (null == guid)?null:(IRPModelElement) project.findElementByGUID(guid);
    				guid = (null == port2URI)?null:uri2LegalGUID(props.getProperty(port2URI));
    				IRPModelElement port2 = (null == guid)?null:(IRPModelElement) project.findElementByGUID(guid);
    				guid = (null == part1URI)?null:uri2LegalGUID(props.getProperty(part1URI));
    				IRPInstance part1 = (null == guid)?null:(IRPInstance) project.findElementByGUID(guid);
    				guid = (null == part2URI)?null:uri2LegalGUID(props.getProperty(part2URI));
    				IRPInstance part2 = (null == guid)?null:(IRPInstance) project.findElementByGUID(guid);
    				IRPPackage pkg = findPackageOfElement(part1);

    				if (null != port1 && null != port2) {
    					if (port1 instanceof IRPSysMLPort && port2 instanceof IRPSysMLPort) {
    						if (part1 == null && part2 != null) // from-part is missing, only to-part exists.
    							otherElement = ((IRPClass)modelElement).addLinkToPartViaPort(part2, (IRPSysMLPort)port2, (IRPSysMLPort)port1, null);
//    						else if (part2 == null && part1 != null)// to-part is missing, only from-part exists. 
//    							otherElement = ((IRPClass)modelElement).addLinkToElement(part2, null, port2, port1);
//    							otherElement = ((IRPSysMLPort) port1).addLink(part1, (IRPSysMLPort)port2, null, null, pkg);
    						else
    							otherElement = ((IRPSysMLPort) port1).addLink(part1, part2, null, (IRPSysMLPort)port2, pkg);
    					}
    					else if (port1 instanceof IRPPort && port2 instanceof IRPPort) {
    						if (ModelHelpers.isIRPPackage(modelElement))
        						otherElement = ((IRPPackage)modelElement).addLink(part1, part2, null, (IRPPort)port1, (IRPPort)port2);
    						else
    							otherElement = ((IRPClass)modelElement).addLink(part1, part2, null, (IRPPort)port1, (IRPPort)port2);
    					} else
    						throw new RhapsodyRuntimeException("port1 and port2 have illegal types [" + port1.getClass() + ", " + port2.getClass() + "] - skipped.");
    				} else
    					throw new RhapsodyRuntimeException("port1 and/or port2 are null - skipped");

    				otherElement.setName("\t"); // temporary, invalid name
    				otherElement.setOwner(modelElement);
    				if (null == otherName) {
    					Integer counter = (Integer) props.get("SM.COUNTER");
    					if (null == counter)
    						counter = new Integer(0);
    					int cntr = counter.intValue();
    					otherName = "link_" +
    					((null==part1)?Integer.toString(++cntr):part1.getName()) + "_" +
    					((null==port1)?Integer.toString(++cntr):port1.getName()) + "_" +
    					((null==part2)?Integer.toString(++cntr):part2.getName()) + "_" +
    					((null==port2)?Integer.toString(++cntr):port2.getName()) + "_" + Integer.toString(++cntr);
    					props.put("SM.COUNTER", new Integer(cntr));
    				}
    				otherElement.setName(otherName);
    				otherElement.addStereotype("connector", "Link");
    			} else if ("Stereotype".equals(otherMetaType)){
    				otherElement = modelElement.addNewAggr(otherMetaType, otherName);
    				stmt = otherResource.getProperty(model.getProperty(IRhpConstants.APPLICABLE_TO));
    				if (null != stmt) {
    					String applicableTo = stmt.getObject().asLiteral().getString();
    					System.err.println("Stereotype [" + otherName + "] is applicable to [" + applicableTo + "]");
    					for (String metaClass: applicableTo.split(","))
    						((IRPStereotype)otherElement).addMetaClass(metaClass);
    				}
    			} else if ("Package".equals(modelElement.getMetaClass()) && GLOBAL_COMPONENTS.contains(otherMetaType) &&
    					null == otherGUID) {
    				if (postProcessing)
    					return null;
    				IRPPackage aPackage = (IRPPackage)modelElement;
    				String name = otherName;
    				if (null == name)
    					name = "noname";
    				if (otherMetaType.equals("Object")) {
    					Model m = otherResource.getModel();
    					Property rhpType = m.getProperty(IRhpConstants.HAS_TYPE);
    					Statement s = otherResource.getProperty(rhpType);
    					if (null == s) {
    						otherElement = aPackage.addImplicitObject(name);
    					} else {
    						String otherRhpTypeURI = s.getObject().toString();
    						if (otherRhpTypeURI.equals(IRhpConstants.IMPLICIT_TYPE)) {
    							otherElement = aPackage.addImplicitObject(name);
    							// Now remove the statement for this implicit type so it is not reprocessed 
    							// needlessly with needlessly complaints later on since this URL is not a real resource.
    							m.remove(s);
    						} else {
    							String guid = uri2LegalGUID(props.getProperty(otherRhpTypeURI));
    							IRPClassifier otherClass = (IRPClassifier) project.findElementByGUID(guid);
    							String otherClassName = otherClass.getName();
    							String otherClassPackageName = otherClass.getOwner().getName();
    							otherElement = aPackage.addGlobalObject(name, otherClassName, otherClassPackageName);
    						}
    					}
    				} else if (otherMetaType.equals("Variable"))
    					otherElement = aPackage.addGlobalVariable(name);
    				else if (otherMetaType.equals("Function"))
    					otherElement = aPackage.addGlobalFunction(name);
    			} else
    				
    				otherElement = modelElement.addNewAggr("Block".equals(otherMetaType)?"block":otherMetaType, otherName);
    			
    		} catch(Exception x) { //RhapsodyRuntimeException x) {
    			x.printStackTrace();
    			String msg = "During [" + (postProcessing?"PostProcessing":"PreProcessing") +
    				", "+ (processingOwning?"ProcessOwning":"ProcessRefs") + "] Failed adding an RDF resource to the Rhapsody project, following the cause: " + x.getMessage();
    			System.err.println(msg);
    			console(rhpPlugin, msg);
    			String context = "Relation: " + relation + " => Name: " + otherName + ", metaType=" + otherMetaType + ", URI=" + otherObjectURI;

    			console(rhpPlugin, "_  Context: " + context);
    			console(rhpPlugin, "_  Offending triple: " + stmt);

    			return null; // throw x;
    		}

    		props.setProperty(otherElement.getGUID(), otherObjectURI);
    		props.setProperty(otherObjectURI, otherElement.getGUID());
    	}

    	restoreAssignment(modelElement, relation, otherElement, rhpPlugin, stmt);
		return otherResource;
	}

    private static boolean isImplicitType(String otherObjectURI) {
    	return otherObjectURI.equals(IRhpConstants.IMPLICIT_TYPE);
	}

	private static boolean isOntologyUri(String otherObjectURI) {
		if (null == otherObjectURI)
			return false;
		return otherObjectURI.startsWith(IRhpConstants.RHP_ONTOLOGY_NS);
	}

	private static Resource getInstanceResouce(Model model,
			String otherObjectURI) {
    	Resource otherResource = model.getResource(otherObjectURI);
    	StmtIterator otherResources = model.listStatements(otherResource, null, (RDFNode)null);
    	if(false == otherResources.hasNext()) // if no real triples to it, it is not a real instance resource in the model.
    		return null;
    	return otherResource;
	}

	/**
     * Prints the msg to the rhapsody console - if not null, or to the
     * Java console.
     * @param rhpPlugin RhpPlugin reference to do the consol eprinting.
     * @param msg String to be printed.
     */
    static void console(RhpPlugin rhpPlugin, String msg) {
		if (null != rhpPlugin)
			rhpPlugin.console(msg);
		else
			System.out.println(msg);
    }
    /**
     * Answers the package containing a model element
     * @param part Model element in question
     * @return IRPPackage containing that model element, or null.
     */
	public static IRPPackage findPackageOfElement(IRPInstance part) {
		IRPModelElement pkg = part;
		while (null != pkg &&(false == (pkg instanceof IRPPackage))) {
			pkg = pkg.getOwner();
		}
		return (IRPPackage) pkg;
	}
	/**
	 * Answers with a legal GUID string for a URI of that GUID.
	 * @param aURI URI of a GUID
	 * @return String of the legal GUID.
	 */
	public static String uri2LegalGUID(String aURI) {
		if (null == aURI) {
			System.out.println("aURI is null");
			return "";
		}
		return aURI.replace(IRhpConstants.RHP_INSTANCE_NS, "").replace("GUID-", "GUID ");
	}

	/**
	 * Answers with the clean meta type from an ontological type resource.
	 * @param aURI URI of a type resource.
	 * @return String of the meta type.
	 */
	public static String ontologyUri2MetaType(String aURI) {
		if (null == aURI) {
			System.out.println("Type aURI is null");
			return "";
		}
		if (aURI.startsWith(IRhpConstants.RHP_ONTOLOGY_NS))
			return aURI.replace(IRhpConstants.RHP_ONTOLOGY_NS, "").replace("GUID-", "GUID ");
		System.out.println("!!! Type aURI is not a SysML name space!"); // it would be a updm one... :)
		return aURI; //.replace(IRhpConstants.RHP_UPDM_ONTOLOGY_NS, "").replace("GUID-", "GUID ");
	}

	@SuppressWarnings("unchecked")
	private static void addInterface(IRPPort port, String relation, IRPClass interfc) {
		List interfaces;
		if ("hasProvided".equals(relation))
			interfaces = port.getProvidedInterfaces().toList();
		else if ("hasRequired".equals(relation))
			interfaces = port.getRequiredInterfaces().toList();
		else
			return;

		for (Object i : interfaces) {
			IRPClass j = (IRPClass)i;
			if (j.getFullPathName().equals(interfc.getFullPathName()))
				return;
		}

		if ("hasProvided".equals(relation))
			port.addProvidedInterface(interfc);
		else if ("hasRequired".equals(relation))
			port.addRequiredInterface(interfc);
		else
			return;
	}

	private static void restoreAssignment(IRPModelElement irpElement,
			String relation, Object value, RhpPlugin rhpPlugin, Statement stmt) {
		System.out.print("Gonna restore assignment: " + irpElement.getFullPathName() + "[" + irpElement.getUserDefinedMetaClass() + "]." + relation + " = " + value.toString());
//		if (relation.contains("PortDirection"))
//			System.err.println("+++ debug PortDirection");
		try {
			if (relation.indexOf("Port") >= 0 || relation.indexOf("Flow") >= 0)
				System.err.println("Debugging Type or Flow");
			if(NON_OWNING_RELATIONS.contains(relation)) {
				IRPModelElement otherElement = (IRPModelElement) value;
				if("hasRequired".equals(relation)) {
					//((IRPPort)irpElement).addRequiredInterface((IRPClass) otherElement);
					addInterface((IRPPort)irpElement, relation, (IRPClass)otherElement);
//				} else if("hasContract".equals(relation)) {
//					if (otherElement.getName().endsWith("implicitContract"))
//						((IRPPort)irpElement).setImplicit();
//					else
//						((IRPPort)irpElement).setContract((IRPClass) otherElement);
				} else if("hasProvided".equals(relation)) {
					//((IRPPort)irpElement).addProvidedInterface((IRPClass) otherElement);
					addInterface((IRPPort)irpElement, relation, (IRPClass)otherElement);
				} else if("hasAttributeType".equals(relation)) {
					((IRPAttribute)irpElement).setType((IRPClassifier) otherElement);
				} else if("hasType".equals(relation)) {
					((IRPInstance)irpElement).setOtherClass((IRPClassifier) otherElement);
				} else if("hasTagType".equals(relation)) {
					((IRPTag)irpElement).setType((IRPClassifier) otherElement);
				} else if("hasPortType".equals(relation)) {
					((IRPSysMLPort)irpElement).setType((IRPClassifier) otherElement);
				} else if("hasReturnType".equals(relation)) {
					((IRPOperation)irpElement).setReturns((IRPClassifier) otherElement);
				} else if("hasOnEvent".equals(relation)) {
					((IRPEventReception)irpElement).setEvent((IRPEvent) otherElement);
				} else if("hasPort_1".equals(relation)) {
					// Do nothing, ports are done on construction
				} else if("hasPort_2".equals(relation)) {
					// Do nothing, ports are done on construction
				} else if("hasLink".equals(relation)) {
					// Do nothing, links are done on construction
				} else if("hasStereotype".equals(relation)) {
					irpElement.addSpecificStereotype((IRPStereotype)otherElement);
				} else if("hasTagType".equals(relation)) {
					((IRPTag)irpElement).setType((IRPClassifier)otherElement);
				} else if("hasInstanceValue".equals(relation)) {
					ModelHelpers.setInstanceValue(irpElement, otherElement);
				} else if("hasAnchor".equals(relation)) {
					if (!ModelHelpers.setAnchor(irpElement, otherElement))
						console(rhpPlugin, "Warning: Failed to set anchor from [" + irpElement.getFullPathName() + "] to [" + otherElement.getFullPathName() + "]");
				} else if("hasAssociation".equals(relation)) {
					if (!ModelHelpers.setAssociation(irpElement, otherElement))
						console(rhpPlugin, "Warning: Failed to set asociation from [" + irpElement.getFullPathName() + "] to [" + otherElement.getFullPathName() + "]");
				} else
					assert(false);
			} else {
				if("hasMultiplicity_1".equals(relation)) {
					((IRPLink)irpElement).setEnd1Multiplicity(multiplicityLUT.get(ontologyUri2MetaType(value.toString())));
				} else if("hasMultiplicity_2".equals(relation)) {
					((IRPLink)irpElement).setEnd2Multiplicity(multiplicityLUT.get(ontologyUri2MetaType(value.toString())));
				} else if(DCTerms.description.getURI().equals(relation)) {
					boolean predefined = irpElement instanceof IRPType && ((IRPType)irpElement).getIsPredefined() != 0;
					if (!predefined) {
						String v = unquote(value.toString());
						irpElement.setDescription(v);
					}
					//				} else if("hasVisibility".equals(relation)) {
					//					if (false == ModelHelpers.setVisibility(irpElement, visibilityLUT.get(uri2LegalGUID(value.toString())))) //)((IRPAttribute)irpElement).setVisibility(visibilityLUT.get(uri2LegalGUID(value.toString())));
					//						throw new Exception("Visibility is not appropriate to the irpElement [" + irpElement + "]");
				} else if ("hasValue".equals(relation)) {
					if (ModelHelpers.isIRPAnnotation(irpElement)) {
						ModelHelpers.asIRPAnnotation(irpElement).setBody(unquote(value.toString()));
					} else if (ModelHelpers.isIRPVariable(irpElement)) {
						ModelHelpers.asIRPVariable(irpElement).setDefaultValue(unquote(value.toString()));
					}
				} else if("hasPortDirection".equals(relation)) {
					String val = portDirectionLut.get(ontologyUri2MetaType(value.toString()));
					if (ModelHelpers.isFlowPort(irpElement))
						((IRPSysMLPort)irpElement).setPortDirection(val);
					else {
						IRPTag direction = irpElement.getTag("direction");
						if (null == direction)
							direction = (IRPTag)irpElement.addNewAggr("Tag", "direction");
						direction.setValue(val);
					}
				} else if("isReversed".equals(relation)) {
					String val = ((Node)value).getLiteralLexicalForm();
					((IRPSysMLPort)irpElement).setIsReversed(Boolean.parseBoolean(val) ? 1 : 0);
				} else if("hasFlowDirection".equals(relation)) {
					((IRPFlow)irpElement).setDirection(flowDirectionLUT.get(ontologyUri2MetaType(value.toString())));
				} else if ("hasTypeDeclaration".equals(relation)) {
					((IRPAttribute)irpElement).setTypeDeclaration(unquote(value.toString()));
				} else if ("isApplicableTo".equals(relation)) {
					console(rhpPlugin, "Info: Processing applicableTo relation with value [" + value.toString() + "]");
					if (false == Strings.isNullOrEmpty(value.toString())) {
						String v = value.toString();
						if (v.startsWith("\"")) 
							v = v.substring(1, v.length()-1); // remove leading and trailing ".
						for (String metaClass : v.split(",")) {
							if (false == Strings.isNullOrEmpty(metaClass.trim()))
								((IRPStereotype)irpElement).addMetaClass(metaClass);
						}
					}
				} else {
					System.out.println(" - nothing to do.");
					return;
				}
			}
			System.out.println(" - done");
//		} catch (RhapsodyRuntimeException x) {
//			if("hasVisibility".equals(relation))
//				throw x;
		} catch (Throwable x) {
//			System.err.print("WTF:[" + x.getMessage() + "]");
			String msg = "Error [" + x.getClass().getName() + "]: " + x.getMessage().trim();
			if(x instanceof RhapsodyRuntimeException)
				try{
					msg += ";" + RhapsodyAppServer.getActiveRhapsodyApplication().getErrorMessage().trim() ;
				} catch (Throwable t) {} // Don't process, we're in catch already
			msg += " - skipped.";
			System.err.println("\nWTF:" + msg);
			x.printStackTrace();
			console(rhpPlugin, msg);
			String context = irpElement.getFullPathName() + "[" + irpElement.getUserDefinedMetaClass() + "]." + relation + " = " + value.toString();

			console(rhpPlugin, "_  Context: " + context);
			console(rhpPlugin, "_  Offending triple: " + stmt);
		}
	}
	private static void restoreIRPPropertyFromRDF(IRPModelElement modelElement, Statement stmt, RhpPlugin rhpPlugin) {
    	String relationURI = stmt.asTriple().getPredicate().getURI();
    	String relation = ontologyUri2MetaType(relationURI);

    	restoreAssignment(modelElement, relation, stmt.asTriple().getObject(), rhpPlugin, stmt);
	}
	public static Map<String, String> inverseIdMap(Map<String, String> idMap) {
    	BiMap<String, String> biMap = (BiMap<String, String>)idMap;
    	if(biMap == null)
    	    throw new IllegalArgumentException(
                    "The argument idMap object was not created by the extractIdMap() method.");
		return biMap.inverse();
    }
	private static boolean isPseudoRef(String relation) {
		return "hasVisibility".equals(relation)
				|| "hasMultiplicity".equals(relation)
				|| "hasMultiplicity_1".equals(relation)
				|| "hasMultiplicity_2".equals(relation)
				|| "hasPortDirection".equals(relation)
				|| "isReversed".equals(relation)
				|| "hasFlowDirection".equals(relation)
//				|| "isApplicableTo".equals(relation)
				;
	}

	private static IRPModelElement findPredefinedType(IRPProject project, String uri) {
		String [] pkgs = { "PredefinedTypes", "PredefinedTypesCpp" };
		for (String pkg : pkgs) {
			IRPModelElement type = findPredefined(project, pkg, uri);
			if (null != type)
				return type;
		}
		return null;
	}

	private static IRPModelElement findPredefined(IRPProject project, String pkg, String uri) {
		// Right now, the only predefined objects we support are [some] predefined types
		// We're assuming predefined elements do not have hyphens in their names (thus, all hyphens came through replacement of blanks)
		String name = uri2LegalGUID(uri).replace('-', ' ');
		//IRPPackage predefinedTypes = (IRPPackage)project.findNestedElement("PredefinedTypesCpp", "Package");
		IRPPackage predefinedTypes = (IRPPackage)project.findNestedElement(pkg, "Package");
		if (null == predefinedTypes)
			return null;
//			System.err.println("predefined types is null");
		IRPType type = (IRPType)predefinedTypes.findNestedElement(name, "Type");
		return type;
	}

	/**
	 * Not implemented yet
	 * @param text
	 * @return
	 */
	public static String uuidEncode (String text) {
//		UUEncoder decoder = new UUEncoder();
		try {
			text = new String(text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Ariel Auto-generated method stub
		return text;
	}

	private static String unquote(String v) {
		if (v.startsWith("\""))
			v = v.substring(1, v.length()-1);
		return v;
	}

}
