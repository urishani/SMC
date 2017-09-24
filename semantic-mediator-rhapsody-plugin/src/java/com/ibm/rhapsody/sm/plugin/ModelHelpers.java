
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Strings;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.ibm.rhapsody.sm.IRhpConstants;
import com.telelogic.rhapsody.core.IRPAnnotation;
import com.telelogic.rhapsody.core.IRPAssociationClass;
import com.telelogic.rhapsody.core.IRPAttribute;
import com.telelogic.rhapsody.core.IRPClass;
import com.telelogic.rhapsody.core.IRPClassifier;
import com.telelogic.rhapsody.core.IRPComment;
import com.telelogic.rhapsody.core.IRPComponent;
import com.telelogic.rhapsody.core.IRPConnector;
import com.telelogic.rhapsody.core.IRPConstraint;
import com.telelogic.rhapsody.core.IRPDependency;
import com.telelogic.rhapsody.core.IRPDiagram;
import com.telelogic.rhapsody.core.IRPEvent;
import com.telelogic.rhapsody.core.IRPEventReception;
import com.telelogic.rhapsody.core.IRPFlow;
import com.telelogic.rhapsody.core.IRPFlowchart;
import com.telelogic.rhapsody.core.IRPGeneralization;
import com.telelogic.rhapsody.core.IRPInstance;
import com.telelogic.rhapsody.core.IRPInstanceValue;
import com.telelogic.rhapsody.core.IRPLink;
import com.telelogic.rhapsody.core.IRPLiteralSpecification;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPPackage;
import com.telelogic.rhapsody.core.IRPPort;
import com.telelogic.rhapsody.core.IRPProfile;
import com.telelogic.rhapsody.core.IRPProject;
import com.telelogic.rhapsody.core.IRPRelation;
import com.telelogic.rhapsody.core.IRPRequirement;
import com.telelogic.rhapsody.core.IRPStatechart;
import com.telelogic.rhapsody.core.IRPStereotype;
import com.telelogic.rhapsody.core.IRPSysMLPort;
import com.telelogic.rhapsody.core.IRPTag;
import com.telelogic.rhapsody.core.IRPType;
import com.telelogic.rhapsody.core.IRPUnit;
import com.telelogic.rhapsody.core.IRPVariable;
import com.telelogic.rhapsody.core.RPConstraint;
import com.telelogic.rhapsody.core.RPPort;

/**
 * Helper class for model - offering utility classes to interrogate model elements.
 * @author shani
 *
 */
public class ModelHelpers {

	public static boolean isIRPProject(IRPModelElement element) {
		return element instanceof IRPProject;
	}

	public static IRPProject asIRPProject(IRPModelElement element) {
		return (IRPProject) element;
	}

	public static boolean isIRPClassifier(IRPModelElement element) {
		return element instanceof IRPClassifier;
	}

	public static IRPClassifier asIRPClassifier(IRPModelElement element) {
		return (IRPClassifier) element;
	}

	public static boolean isObject(IRPModelElement element) {
		if (element.getName().equals("ControllerGeneric"))
			System.out.println("is wiper an object?");
		return element instanceof IRPInstance && "Object".equals(element.getMetaClass()); // && "Object".equalsIgnoreCase((element.getUserDefinedMetaClass()));
	}

	public static IRPInstance asIRPInstance(IRPModelElement element) {
		return (IRPInstance) element;
	}

	public static boolean isBlock(IRPModelElement element) {
		return isIRPClass(element) && element.getUserDefinedMetaClass().equals("Block");
	}

	public static boolean isIRPLink(IRPModelElement element) {
		return element instanceof IRPLink;
	}
	
	public static boolean isConnector(IRPModelElement element) {
		return isIRPLink(element) && "connector".equals(element.getUserDefinedMetaClass());
	}

	/**
	 * Answers whether a part has implicit type, meanind that its otherClass is implicit, meaning that
	 * the part is the owner of the otherClass. 
	 * @param element
	 * @return
	 */
	public static boolean hasImplicitType(IRPModelElement element) {
		IRPClassifier type = null;
		if (isIRPRelation(element))
			type = asIRPRelation(element).getOtherClass();
		boolean isImplicit = (null != type && element.getGUID().equals(type.getOwner().getGUID()));
		if (isImplicit) 
			System.err.println("debugging isImplicit type [" + element.getFullPathName() + "/" + element.getName());
		return isImplicit;
	}
	
	public static boolean isIRPRelation(IRPModelElement element) {
		return element instanceof IRPRelation;
	}

	public static IRPRelation asIRPRelation(IRPModelElement element) {
		return (IRPRelation)element;
	}

	/**
	 * Answers whether a part has implicit type, meanind that its otherClass is implicit, meaning that
	 * the part is the owner of the otherClass. 
	 * @param element
	 * @return
	 */
	public static boolean hasImplicitInterface(IRPModelElement element) {
		if (false == isIRPPort(element))
				return false;
		IRPClassifier type = ((IRPInstance)element).getOtherClass();
		return (null == type || 
				(null != type && element.getGUID().equals(type.getOwner().getGUID())));
	}
	
	public static IRPLink asIRPLink(IRPModelElement element) {
		return (IRPLink) element;
	}

	public static boolean isIRPDependency(IRPModelElement element) {
		return element instanceof IRPDependency;
	}
	public static IRPDependency asIRPDependency(IRPModelElement element) {
		return (IRPDependency) element;
	}

	public static boolean isIRPConstraint(IRPModelElement element) {
		return element instanceof IRPConstraint;
	}
	public static IRPConstraint asIRPConstraint(IRPModelElement element) {
		return (IRPConstraint) element;
	}
	public static boolean isIRPComment(IRPModelElement element) {
		return element instanceof IRPComment;
	}
	public static IRPComment asIRPComment(IRPModelElement element) {
		return (IRPComment) element;
	}
	public static boolean isIRPRequirement(IRPModelElement element) {
		return element instanceof IRPRequirement;
	}
	public static IRPRequirement asIRPRequirement(IRPModelElement element) {
		return (IRPRequirement) element;
	}
	public static boolean isIRPAnnotation(IRPModelElement element) {
		return (element instanceof IRPAnnotation);
	}
	public static IRPAnnotation asIRPAnnotation(IRPModelElement element) {
		return (IRPAnnotation) element;
	}


	public static boolean isIRPFlow(IRPModelElement element) {
		return element instanceof IRPFlow;
	}

	public static IRPFlow asIRPFlow(IRPModelElement element) {
		return (IRPFlow) element;
	}

	public static boolean isAttribute(IRPModelElement element) {
		return element instanceof IRPAttribute;
	}

	public static IRPAttribute asIRPAttribute(IRPModelElement element) {
		return (IRPAttribute) element;
	}

	// not used.
	public static boolean isIRPAssociationClass(IRPModelElement element) {
		return element instanceof IRPAssociationClass;
	}

	public static IRPAssociationClass asIRPAssociationClass(IRPModelElement element) {
		return (IRPAssociationClass) element;
	}

	public static boolean isIRPClass(IRPModelElement element) {
		return null != element && element.getMetaClass().equals("Class"); // instanceof IRPClass;
	}

	public static IRPClass asIRPClass(IRPModelElement element) {
		return (IRPClass) element;
	}

	public static boolean isIRPStereotype(IRPModelElement element) {
		return element instanceof IRPStereotype;
	}

	public static IRPStereotype asIRPStereotype(IRPModelElement element) {
		return (IRPStereotype) element;
	}

	public static boolean isIRPPackage(IRPModelElement element) {
		return element instanceof IRPPackage;
	}

	public static IRPPackage asIRPPackage(IRPModelElement element) {
		return (IRPPackage) element;
	}

	public static boolean isIRPComponent(IRPModelElement element) {
		return element instanceof IRPComponent;
	}

	public static IRPComponent asIRPComponent(IRPModelElement element) {
		return (IRPComponent) element;
	}

	public static boolean isIRPModelElement(Object element) {
		System.out.println("q: " + /*mTraversalQueue.size() +*/ ", element:" + (element==null?"null":element.getClass().getName()));
		return element != null && element instanceof IRPModelElement;
	}

	public static boolean isIncludedStereotype(IRPModelElement element) {
//		if (element.getName().equals("optimized")) {
//			System.out.println(element.getOwner().getName());
//		}
		
		IRPModelElement p = element.getOwner();
		while (null != p) {
			   if ((p instanceof IRPUnit) && ((IRPUnit)p).isSeparateSaveUnit() > 0 &&
					   ((IRPUnit)p).isReferenceUnit() > 0) {
					return true;
			}
			p = p.getOwner();
		}
		return false;
	}
	
	public static String getQualifiedName(IRPModelElement irpElement) {
		return irpElement.getFullPathName();
//		StringBuffer sb = new StringBuffer(irpElement.getName());
//		IRPModelElement p = irpElement.getOwner();
//		while (null != p) {
//			sb.append("::").append(p.getName());
//			if (isIRPProfile(p))
//				return sb.toString();
//			p = p.getOwner();
//		}
//		return sb.toString();
	}

	//-------------------------------------------------
	public static Collection<IRPModelElement> getParts(IRPClass aClass) {
		List<IRPModelElement> result = new LinkedList<IRPModelElement>();
		for (Object element : aClass.getRelations().toList()) {
			if (element instanceof IRPInstance && false == element instanceof IRPPort)
				result.add((IRPModelElement) element);
		}
		return result;
	}

	public static boolean isIRPPort(IRPModelElement irpElement) {
		return irpElement instanceof IRPPort;
	}

	public static boolean isFlowPort(IRPModelElement irpElement) {
		return 	irpElement instanceof IRPSysMLPort;
	}


	public static boolean isOperation(IRPModelElement irpElement) {
		return isPrimitiveOperation(irpElement) ||
		isTriggeredOperation(irpElement) ||
		isReception(irpElement);
//		return irpElement instanceof IRPOperation;
	}

	public static boolean isReception(IRPModelElement irpElement) {
		return "Reception".equalsIgnoreCase(irpElement.getUserDefinedMetaClass());
	}

	public static boolean isTriggeredOperation(IRPModelElement irpElement) {
		return "TriggeredOperation".equalsIgnoreCase(irpElement.getUserDefinedMetaClass());
	}

	public static boolean isPrimitiveOperation(IRPModelElement irpElement) {
		return "PrimitiveOperation".equalsIgnoreCase(irpElement.getUserDefinedMetaClass());
	}

	public static boolean isInterface(IRPModelElement irpElement) {
		return irpElement instanceof IRPClass && "Interface".equalsIgnoreCase((irpElement.getUserDefinedMetaClass()));
	}

	public static boolean isIRPConnector(IRPModelElement irpElement) {
		return irpElement instanceof IRPConnector;
	}

	public static boolean isIRPInstanceValue(IRPModelElement irpElement) {
		return irpElement instanceof IRPInstanceValue;
	}

	public static boolean isIRPEvent(IRPModelElement irpElement) {
		return irpElement instanceof IRPEvent;
	}

	public static boolean isIRPEventReception(IRPModelElement irpElement) {
		return irpElement instanceof IRPEventReception;
	}

	public static boolean isIRPProfile(IRPModelElement element) {
		return element instanceof IRPProfile;
	}

	public static boolean isIRPStatechart(IRPModelElement irpElement) {
		return irpElement instanceof IRPStatechart;
	}

	public static boolean isIRPType(IRPModelElement irpElement) {
		return irpElement instanceof IRPType;
	}

	public static boolean isFlow(IRPModelElement irpElement) {
		return irpElement instanceof IRPFlow;
	}

	public static boolean isReadOnlyPrimitive(IRPModelElement irpElement) {
		boolean result =  isIRPUnit(irpElement);
		if (!result)
			return result;
		result &= false == isIRPStereotype(irpElement);
		if (!result)
			return result;
		result &= ((IRPUnit)irpElement).isReadOnly() != 0;
//		if (result) {
//			String n = irpElement.getName();
//			if (n.contains("Rhp"))
//				System.err.println("isReadOnlyPrimitive");
//		}
		return result;
	}

	private static boolean isIRPUnit(IRPModelElement irpElement) {
		return irpElement instanceof IRPUnit;
	}

	/**
	 * Answers similarly to <i>isReadOnly()</i>, excluding stereotypes in profiles.
	 * @param irpElement
	 * @return
	 */
	public static boolean isOntologyPrimitive(IRPModelElement irpElement) {
		return isReadOnlyPrimitive(irpElement); // ||  // Stereotypes should be treated differently. 
//		( ModelHelpers.isIRPStereotype(irpElement) &&   isIncludedStereotype(irpElement));
	}
	
	public static boolean isIRPGeneralization(IRPModelElement irpElement) {
		return irpElement instanceof IRPGeneralization;
	}
	
	public static String getTypeDeclaration(IRPClassifier type) {
		String declaration = null;
		if (isIRPType(type)) {
			declaration = ((IRPType)type).getDeclaration();
			if (Strings.isNullOrEmpty(declaration))
				declaration = null;
		}
		return declaration;
	}

	public static boolean isTag(IRPModelElement component) {
		return component instanceof IRPTag;
	}

	public static boolean isIRPLiteralSpecifications(IRPModelElement component) {
		return component instanceof IRPLiteralSpecification;
	}

	public static boolean isIRPLiteralSpecification(IRPModelElement component) {
		return component instanceof IRPLiteralSpecification;
	}

	public static boolean isUpdmFunction(IRPModelElement component) {
		return (component instanceof IRPFlowchart && 
				component.getUserDefinedMetaClass().equals("Function"));
	}
	
	/**
	 * Answers whether the component is not a real model element, but one used internally within
	 * Rhapsody and should not be exported. 
	 */
	public static boolean isInternalRhapsodyElement(IRPModelElement component) {
		String fp = component.getFullPathName();
		String n = component.getName();
		boolean internal =  ! component.getFullPathName().endsWith(component.getName());
		internal = internal && ! component.getMetaClass().equals("Project");
		if (internal)
			System.err.println("Internal element [" + component + "]" + component.getName() + "/" + component.getFullPathName() + "/" + component.getUserDefinedMetaClass() + "/" + component.getMetaClass());
		return internal;
	}

	/**
	 * Checks if an element is an internal Rhapsody type.
	 * @param part
	 * @return
	 */
	public static boolean isBuiltInType(IRPModelElement part) {
		IRPProject project = part.getProject();
		for (String pkg : new String[]{ "PredefinedTypes", "PredefinedTypesCpp" }) {
			IRPPackage predefinedTypes = (IRPPackage)project.findNestedElement(pkg, "Package");
			if (null == predefinedTypes)
				continue;
//				System.err.println("predefined types is null");
			  if (predefinedTypes.getNestedClassifiers().toList().contains(part))
				  return true;
		}
		return false;
	}

	public static boolean isUpdmComponent(IRPModelElement component) {
		return (//component instanceof IRPFlowchart && 
				component.getUserDefinedMetaClass().equals("Component"));
	}
	public static boolean isUpdmSystem(IRPModelElement component) {
//		String udmc = component.getUserDefinedMetaClass();
		return (//component instanceof IRPFlowchart && 
				component.getUserDefinedMetaClass().equals("System"));
	}

	public static boolean isUpdmResourceInterface(IRPModelElement component) {
		return (component.getUserDefinedMetaClass().equals("ResourceInterface"));
	}
	public static boolean isUpdmResourcePort(IRPModelElement component) {
		return (component.getUserDefinedMetaClass().equals("ResourcePort"));
	}

	/**
	 * Answer whether the resource is a stereotype
	 * @param res
	 * @return boolean
	 */
	public static boolean isStereotypeResource(Resource res) {
		if (null == res)
			return false;
//		if (other.toString().indexOf("108") >= 0)
//			System.out.println("---- debugging 108");
		Statement stmt = res.getProperty(RDF.type);
		if (null == stmt)
			return false;
		return (stmt.getObject().isURIResource() &&
			stmt.getObject().toString().equals(IRhpConstants.STEREOTYPE));	
	}

	/**
	 * Answers with the guid of a profiled stereotype or null if that resoruce is
	 * not a profiled stereotype.
	 * <br>
	 * A profiled stereotype is one that also has the property "hasGUID" by which
	 * it can be identified and located within a project. The resource will also have
	 * the property "hasCompoundName" by which it can be identified symbolically for the user
	 * performing the import of a model to Rhapsody, in case the stereotype cannot be located.
	 * @param res Resoruce made for that stereotype.
	 * @return String of the GUID associated with that resource, of null if not a proper resource.
	 */
	public static String isProfiledStereotypeResource(Resource res) {
		if (false == isStereotypeResource(res))
			return null;
		Statement stmt = res.getProperty(res.getModel().getProperty(IRhpConstants.HAS_GUID));
		if (null == stmt)
			return null;
		if (stmt.getObject().isLiteral())
			return stmt.getObject().toString();
		return null;
		
	}
	/**
	 * Generic service which finds the visibility property for the elements which can provide such attributes.
	 * @param mModelElement
	 * @return String value of the visibility attribute, or null is not found or inappropriate element.
	 */
	@Deprecated
	public static String getVisibility(IRPModelElement mModelElement) {
		Class<? extends IRPModelElement> eClass = mModelElement.getClass();
		try {
			Method toCall = eClass.getMethod("getVisibility");
			if (null != toCall)
				return (String) toCall.invoke(mModelElement);
		} catch (SecurityException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchMethodException e) {
			return null;
			// e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	/**
	 * Generic function to assign a visibility to an element which can have such an attribute.
	 * @param irpElement
	 * @param visibility
	 * @return true if applicable and false otherwise or if failed.
	 */
	@Deprecated
	public static boolean setVisibility(IRPModelElement irpElement, String visibility) {
		Class<? extends IRPModelElement> eClass = irpElement.getClass();
		try {
			if (irpElement instanceof RPPort) {
				System.err.println("----- alternative setVisibility for RPPort");
				RPPort p = (RPPort)irpElement;
				p.setAttributeValue("Visibility", visibility);
				return true;
			} 
			Method toCall = eClass.getMethod("setVisibility", String.class);
			if (null != toCall)
				toCall.invoke(irpElement, visibility);
			return true;
		} catch (SecurityException e) {
			e.printStackTrace();
			return false;
		} catch (NoSuchMethodException e) {
			return false;
			// e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return false;
		}
//		return null;
	}

	/**
	 * Answers whether the RDF node is a term in the Rhapsody Sysml or UPDM ontology.
	 * This is detected by the prefix of the URL being one of the two ontologies: SysML or UPDM of Rhapsody.
	 * @param obj RDF Node of some triple object to be tested.
	 * @return boolean indicating true if indeed that triple object is a term in the ontology.
	 */
	public static boolean isOntologicalTerm(RDFNode obj) {
		if (false == obj.isResource())
			return false;
		if (obj.toString().startsWith(IRhpConstants.RHP_ONTOLOGY_NS) ||
				obj.toString().startsWith(IRhpConstants.RHP_UPDM_ONTOLOGY_NS))
			return true;
		return false;
	}

	/**
	 * Sets the model element with a value provided through an "hasInstanceValue" relation.
	 * @param irpElement
	 * @param otherElement
	 */
	public static void setInstanceValue(IRPModelElement irpElement,
			IRPModelElement otherElement) {
		if (isTag(irpElement))
			((IRPTag)irpElement).setValue(otherElement.getName());
	}

	public static boolean setAnchor(IRPModelElement constraint,
			IRPModelElement otherElement) {
	   if (isConstraint(constraint)) {
		   if (!containsAnchorTo(constraint, otherElement)) 
			   asIRPConstraint(constraint).addAnchor(otherElement); 
		  return true;
	   } 
  	   return false;
	}

	public static boolean setAssociation(IRPModelElement block,
			IRPModelElement object) {
//	   if (isBlock(block) && isObject(object)) {
//		   asIRPClass(block).addAssociation(end1, end2, name)
//		   if (!containsAnchorTo(constraint, otherElement)) 
//			   asIRPConstraint(constraint).addAnchor(otherElement); 
//		  return true;
//	   } 
  	   return false;
	}
	
	private static boolean containsAnchorTo(IRPModelElement constraint, IRPModelElement otherElement) {
		Collection<IRPModelElement> anchors = WorkModel.asCollection(asIRPConstraint(constraint).getAnchoredByMe());
		return anchors.contains(otherElement);
	}

	public static boolean isModelDiagram(IRPModelElement component) {
		return (component instanceof IRPDiagram);
	}

	public static boolean isConstraint(IRPModelElement irpElement) {
		return (irpElement instanceof RPConstraint);
	}

	public static boolean isIRPVariable(IRPModelElement irpElement) {
		return irpElement instanceof IRPVariable;
	}

	public static IRPVariable asIRPVariable(IRPModelElement irpElement) {
		return (IRPVariable)irpElement;
	}

	

}
