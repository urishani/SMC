
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.common.base.Strings;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.ibm.rhapsody.sm.IRhpConstants;
import com.telelogic.rhapsody.core.IRPAttribute;
import com.telelogic.rhapsody.core.IRPCollection;
import com.telelogic.rhapsody.core.IRPDependency;
import com.telelogic.rhapsody.core.IRPEventReception;
import com.telelogic.rhapsody.core.IRPFlow;
import com.telelogic.rhapsody.core.IRPInstance;
import com.telelogic.rhapsody.core.IRPInstanceValue;
import com.telelogic.rhapsody.core.IRPLink;
import com.telelogic.rhapsody.core.IRPLiteralSpecification;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPOperation;
import com.telelogic.rhapsody.core.IRPPackage;
import com.telelogic.rhapsody.core.IRPPort;
import com.telelogic.rhapsody.core.IRPStereotype;
import com.telelogic.rhapsody.core.IRPSysMLPort;
import com.telelogic.rhapsody.core.IRPTag;
import com.telelogic.rhapsody.core.IRPType;
import com.telelogic.rhapsody.core.RPConstraint;

public class WorkModel {
	private static boolean DEBUG = false;
	private final IRPModelElement mRoot;
	private final Properties mProps;
	private final Map<String, Element> mElements = new HashMap<String, Element>();
	private final List<Element> mOrderedElements = new ArrayList<Element>();
	private class ElementPair {
		public IRPModelElement element;
		public String type;
		public ElementPair(IRPModelElement element, String type) {
			this.element = element;
			this.type = type;
		}
	}
	private final LinkedList<ElementPair> mTraversalQueue = new LinkedList<ElementPair>();

	public WorkModel(IRPModelElement root, Properties props) {
		this.mRoot = root;
		this.mProps = props;
	}

	public static void setDebugMode(boolean debug) {
		DEBUG = debug;
	}

	public static void debugTrace(String trace) {
		if (DEBUG)
			System.out.println(trace);
	}

	public IRPModelElement getRoot() {
		return mRoot;
	}

	private Model myModel = null;

	/**
	 * genererates a model to be used by the Element
	 * @return
	 */
	public Model getModel() {
		if (null == myModel) synchronized (Model.class) {
			if (null == myModel) {
				myModel = ModelFactory.createDefaultModel();
				myModel.setNsPrefixes(Element.getHeaderMap());
			}
		}
		return myModel;
	}


	
	/**
	 * Factory function
	 * @param modelElement
	 * @param list
	 * @param type String for the suggested type based on the classification analysis done already.
	 * @return an existing element, or a new one.
	 */
	public Element getElement(IRPModelElement modelElement, List<Element> list, String type) {
//		if (modelElement.getName().contains("_39")) 
//			System.err.println("getElement for _39 [" + modelElement.getName() + "]");
		Element element = mElements.get(Element.getGUID(modelElement));
		if(element == null) {
			element = new Element(modelElement, mProps, type, this); //new Properties()); // TBD: Properties???
			mElements.put(element.getGUID(), element);
			list.add(element);
		}
		if (false == Strings.isNullOrEmpty(type) && false == type.equals(element.get(Element.CLASS))) {
			System.err.println("Different type suggestion to an element [" + type + "]!=[" + element.get(Element.CLASS) + "]: " + element);
			element.setType(type);
		}
		return element;
	}

	/**
	 * Verifies if a certain element has already been processed or is considered for processing.
	 * @param modelElement
	 * @return
	 */
	public boolean hasElement(IRPModelElement modelElement) {
		return mElements.containsKey(Element.getGUID(modelElement));
	}

	/**
	 * Perform some query over the keys (GUIDs, or other)<br>
	 * Return ordered list of resources of the subset of elements selected.
	 * @param query - currently not implemented.
	 * @return List of ordered list of elements.
	 */
	public Collection<Element> queryElements(String query) {
		Collection<String> keys = mElements.keySet();
		List<Element> result = new ArrayList<Element>(keys.size());
		for (Element element : mOrderedElements) {
			if (keys.contains(element.getGUID()))
				result.add(element);

		}
		return result;
	}

	public String getRDFHeader() {
		return Element.getHeader();
	}

	public String getRDFFooter() {
		return Element.getFooter();
	}

	public Collection<IRPModelElement> getRhpElements(String query) {
        ArrayList<IRPModelElement> list = new ArrayList<IRPModelElement>();
        list.add(mRoot);
		return list;
	}

	public String pushTraversalQueue(IRPModelElement element, String type) {
		if (hasElement(element))
			return "";
		mTraversalQueue.addFirst(new ElementPair(element, type)); //push()
		return "";
	}

	public ElementPair popTraversalQueue() {
		return mTraversalQueue.pollLast();
	}


//	/**
//	 * Adds a relation to a resource which is an ontology individual and not a real resource
//	 * in the model.
//	 * @param element
//	 * @param relation
//	 * @param individual
//	 */
//	@Deprecated
//	private boolean addRelation(Element element, String relation, String individual) {
//		element.addRelation(relation, individual);
//		debugTrace(relation + "(" + element.getId() + ") -> " + individual);
//		return true;
//	}

	/**
	 * Adds an object relation to a resource in the model.
	 * @param element
	 * @param relation
	 * @param other
	 * @return
	 */
	public boolean addRelation(Element element, String relation, IRPModelElement other, String...type) {
		boolean done = false;
		// Next use of 'trail' is part of the debugging tricks.
		if(other != null) {
//			String n = other.getName();
			if (other.getFullPathName().endsWith("_39"))
				System.err.println("debugging _39 [" + other.getFullPathName() + "]");
			boolean b = ModelHelpers.isInternalRhapsodyElement(other);
			if (b)
				System.err.println("O-Boy! adding relation to internal element with wrong NS! [" + other.getFullPathName() + "]");
			String trail = "";
			if (relation.endsWith("_")) {
				trail = "_";
				relation = relation.substring(0,relation.length()-1);
			}
			if (false == "OTHER".equals(relation)) {
				if (element.addRelation(relation, other)) // Other is traversible. Otherwise, it is an internal entity.
					pushTraversalQueue(other, type.length>0?type[0]:null);
			} else {
				System.err.println("+++ OTHER relation encountered");
				other = ((IRPInstanceValue)other).getValue();
			}
			debugTrace(relation + trail + "(" + element.getId() + ") -> " + 
					other.getName()+ ":" + 
					other.getUserDefinedMetaClass() + "/" + 
					other.getMetaClass() + "[" + Element.getGUID(other) + "]");
			done = true;
		}
		return done;
	}

	/**
	 * When an element is encountered, we need to know if the owner is also in the model, or chain it
	 * up.
	 * @param element current element
	 * @param relation relation from parent to current element
	 * @param parent parent element
	 * @return true if a new element resulted for the parent, so it is chased up as well until the top.
	 */
	public boolean addOwnerRelation(Element element, String relation, IRPModelElement parent, List<Element> list, String...type) {
		boolean parentIsNew = (false == hasElement(parent));
		if(parent != null) {
			Element parentElement = getElement(parent, list, null);
			parentElement.addRelation(relation, element.getIrpModelElement()); // same relation is not added more than once.
			// pushTraversalQueue(other); - we don't want to traverse this one.  BUT: we want to fill up its full properties and relations.
			debugTrace("going up: " + relation + "(" + parentElement.getId() + ") -> " + element);
		}
		return (null != parent) && parentIsNew;
	}

	/**
	 * Major service which traverses the model according to some meta-model.
	 * <p>
	 * Future versions will use the meta-model to guide such traversal.
	 * @param rhpPlugin 
	 */
	@SuppressWarnings("unchecked")
	public String traverse(RhpPlugin rhpPlugin) {
		pushTraversalQueue(getRoot(), null);
		IRPModelElement irpElement = null;
		ElementPair elementPair; 
		StringWriter msg = new StringWriter();
		PrintWriter pw = new PrintWriter(msg);

		while (null != (elementPair = popTraversalQueue())) {
			try {
				irpElement = elementPair.element;
				debugTrace("-----------" + irpElement.getName() + ":" + irpElement.getUserDefinedMetaClass() + ":" + irpElement.getMetaClass() + "-" + elementPair.type + "--------" );
				if (irpElement.getName().contains("RhpInteger"))
					System.err.println("Debugging RhpInteger");
				if (irpElement.getName().contains("Default")) {
					System.err.println("Default [" + irpElement.getName() + "]");
				}
				if (ModelHelpers.isInternalRhapsodyElement(irpElement))
					continue;
				if (ModelHelpers.isOntologyPrimitive(irpElement))
					continue;
//				if (irpElement.getName().startsWith("TechnicalLib")) 
//					System.out.println("Found [" + irpElement.getName() + "].");
				Element element = getElement(irpElement, mOrderedElements, elementPair.type);
				Collection<IRPModelElement> components = asCollection(irpElement.getNestedElements());
//				if ("dataRate".equals(irpElement.getName())) {
//					System.out.println("reached dataRate tag");
//					System.out.println(irpElement.getFullPathName());
//					System.out.println(irpElement.getMetaClass());
//					System.out.println(irpElement.getUserDefinedMetaClass());
//				}
//				if (ModelHelpers.isIRPStereotype(irpElement)) {
//					String qName = ModelHelpers.getQualifiedName(irpElement);
//					addProperty(element, Element.QUALIFIED_NAME, qName);
					// nothing to do more.
//					continue;
//				}

//				irpElement.getNewTermStereotype();
				
				if (ModelHelpers.isIRPPackage(irpElement)) { // need to also find all its contents in addition.
					IRPPackage pack = (IRPPackage)irpElement;
					if (false == ModelHelpers.isIRPProject(pack)) try {
						components.addAll(asCollection(pack.getBehavioralDiagrams()));
					} catch (Exception e) {
						System.err.println("Package [" + irpElement + "][" + irpElement.getFullPathName() + "] failed to get behavioral diagrams");
					}
					components.addAll(asCollection(pack.getGlobalFunctions()));
					components.addAll(asCollection(pack.getGlobalObjects()));
					components.addAll(asCollection(pack.getGlobalVariables()));
					components.addAll(asCollection(pack.getLinks()));
				//	components.addAll(asCollection(pack.getf)
				}
				if (ModelHelpers.isIRPClassifier(irpElement))
					components.addAll(asCollection(ModelHelpers.asIRPClassifier(irpElement).getLinks()));
				components.addAll(asCollection(irpElement.getAnnotations()));
				

				// Do follow uplinks for legal links.
				IMethod doUplinks = new IMethod(){
					public boolean apply(String relation, IRPModelElement owner, IRPModelElement component,
							WorkModel ctx, List<Element> list, String...type){
						return ctx.addOwnerRelation(ctx.getElement(component, list, type.length > 0?type[0]:null), relation, owner, list, type);}
				};

				IRPModelElement owner = irpElement.getOwner();
				IRPModelElement irpCurrent = irpElement;
				List<Element> ownerList = new ArrayList<Element>();
				
				// Next section is only done when the element is a real one in the project and not a reference to 
				// another references and separately stored module. 
				// That block is presently not applied only to stereotypes.
				if (false == ModelHelpers.isIncludedStereotype(irpElement)) {
					while (null!= owner) { // climb up to set owners of this element until Project level.
						if (owner.getName().equals("TopLevel")) {
							IRPModelElement ownerOwner = owner.getOwner();
							if ("Package".equals(ownerOwner.getMetaClass())) {
								System.out.println("Reached the TopLevel element with Package owner - skipped");
								owner = null;
								break;
							} else
								System.err.println("!!! Reached the TopLevel element with no Package owner - not skipped !!!");
						}
						if (null != owner && ModelHelpers.isInternalRhapsodyElement(owner)) {
							owner = null;
							break;
						}
						if (false == classifyComponent(doUplinks, owner, irpCurrent, this, ownerList, rhpPlugin))
							break;// no point to continue climbing up.
						irpCurrent = owner;
						owner = owner.getOwner();
					}
					for (int i= ownerList.size() -1; i >= 0; i--)  // go in reverse order
						mOrderedElements.add(ownerList.get(i));

					// Do containment relations with components
					IMethod doDownlinks = new IMethod(){
						public boolean apply(String relation, IRPModelElement owner, IRPModelElement component, WorkModel ctx, List<Element> list, String...type) {
							return ctx.addRelation(ctx.getElement(owner, list, null), relation, component, type);}
					};
					// Now add relations to the contained elements, if the elementis not RO:
					if (false == ModelHelpers.isReadOnlyPrimitive(irpElement))
						for (IRPModelElement component : components) {
							classifyComponent(doDownlinks, irpElement, component, this, mOrderedElements, rhpPlugin);
						}
				}
				// Work with the stereotypes
				List<IRPModelElement> stereotypes = irpElement.getStereotypes().toList();;// Helper.getProfiledStereotypes(irpElement);
//				System.out.println("------\nStereotypes for [" + irpElement.getName() + " - " + irpElement.getUserDefinedMetaClass() + "]: [");
//				boolean ft = true;
				for (IRPModelElement s: stereotypes) {
					if (((IRPStereotype)s).getIsNewTerm() <= 0)
						addRelation(element, "stereotype_", s);
				}
				
				if (ModelHelpers.isIRPStereotype(irpElement) && false == ModelHelpers.isIncludedStereotype(irpElement)) {
					IRPStereotype st = (IRPStereotype)irpElement;
					String applicableTo = st.getOfMetaClass();
					addProperty(element, Element.APPLICABLE_TO, applicableTo);

				}
				
				// Do something with that.
//				if (ModelHelpers.isIRPClass(irpElement)) {
//					IRPClass irpClass = (IRPClass)irpElement;
//					IRPCollection gs = irpClass.getGeneralizations();
//					System.out.println("Class [" + irpElement.getFullPathName() + "] has [" + gs.getCount() + "] generalizations.");
//					for (int i = 1; i <= gs.getCount(); i++) {
//						IRPGeneralization g = (IRPGeneralization) gs.getItem(i);
//						addRelation(element, "generalization_", g);
//					}	
//				} else 
				if (ModelHelpers.isIRPLink(irpElement)) {
					IRPLink connectorElement = (IRPLink)irpElement;

					IRPInstance end1 = connectorElement.getFrom();
					IRPInstance end2 = connectorElement.getTo();

					IRPModelElement port1 = connectorElement.getFromSysMLPort();
					if (port1 == null)
						port1 = connectorElement.getFromPort();
					if (port1 == null) {
						port1 = end1;
						end1 = null;
					}

					IRPModelElement port2 = connectorElement.getToSysMLPort();
					if (port2 == null)
						port2 = connectorElement.getToPort();
					if (port2 == null) {
						port2 = end2;
						end2 = null;
					}

					if (null != end1)
						addRelation(element, "EndPoint_1_", end1);
					if (null != end2)
						addRelation(element, "EndPoint_2_", end2);
					addProperty(element, Element.MULTIPLICITY_1, Element.multiplicityLUT.get(connectorElement.getEnd1Multiplicity()));
					addProperty(element, Element.MULTIPLICITY_2, Element.multiplicityLUT.get(connectorElement.getEnd2Multiplicity()));

					addRelation(element, "Port_1_", port1);
					addRelation(element, "Port_2_", port2);
				} else if (ModelHelpers.isConstraint(irpElement)) { // take care of anchors:
					RPConstraint constraint = (RPConstraint)irpElement;
					Collection<IRPModelElement> anchored = asCollection(constraint.getAnchoredByMe());
					for (IRPModelElement classifier : anchored) {
						addRelation(element, "Anchor_", classifier);
					}
				}
				else if (ModelHelpers.isFlow(irpElement)) {
					IRPFlow flowElement = (IRPFlow)irpElement;
					IRPSysMLPort end1 = (IRPSysMLPort) flowElement.getEnd1();
					IRPSysMLPort end2 = (IRPSysMLPort) flowElement.getEnd2();
					String direction = flowElement.getDirection();
					if (null != end1)
						addRelation(element, "Port_1_", end1);
					if (null != end2)
						addRelation(element, "Port_2_", end2);
					if (null != direction)
						addProperty(element, Element.FLOW_DIRECTION, Element.flowDirectionLUT.get(direction));
				}
				else if (ModelHelpers.isFlowPort(irpElement)) {
					IRPSysMLPort flowPort = (IRPSysMLPort)irpElement;
					IRPModelElement o = flowPort.getType();
					addRelation(element, "portType_", o);
					addProperty(element, Element.MULTIPLICITY, Element.multiplicityLUT.get(flowPort.getMultiplicity()));
					if ("FlowSpecification".equalsIgnoreCase(o.getUserDefinedMetaClass())) {
						boolean isReversed =  flowPort.getIsReversed() != 0;
						addProperty(element, Element.IS_REVERSED,  new Boolean(isReversed)); //, "boolean");
					} else {
						String portDirection = flowPort.getPortDirection();
						if (irpElement.getName().contains("_39"))
							System.err.println("Encountered _39");
						String portDirectionUri = Element.portDirectionLut.get(portDirection);
						if (false == Strings.isNullOrEmpty(portDirectionUri))
							addProperty(element, Element.PORT_DIRECTION, portDirectionUri);
					}
				}
				else if (ModelHelpers.isIRPPort(irpElement)) {
					// TODO - We shall support just "Implicit" contracts for the forseeable future
					// No - adding contracts now.
					if (false == ModelHelpers.hasImplicitInterface(irpElement))
						addRelation(element, "contract_", ((IRPPort)irpElement).getContract());
					addRelations(element, "provided_", asCollection(((IRPPort)irpElement).getProvidedInterfaces()));
					addRelations(element, "required_", asCollection(((IRPPort)irpElement).getRequiredInterfaces()));
				}
				else if (ModelHelpers.isIRPRelation(irpElement)) {
					if (ModelHelpers.hasImplicitType(irpElement)) {
						element.addResourceProperty(Element.TYPE,  IRhpConstants.IMPLICIT_TYPE);
//						addRelation(element, "type", IRhpConstants.IMPLICIT_TYPE);
					} else
						addRelation(element, "type_", ((IRPInstance)irpElement).getOtherClass());
					System.out.println(element);
				}
				else if (ModelHelpers.isAttribute(irpElement)) {
					IRPAttribute attribute = (IRPAttribute)irpElement;
					String declaration = ModelHelpers.getTypeDeclaration(attribute.getType());
					if (null != declaration)
						addProperty(element, Element.TYPE_DECLARATION, StringEscapeUtils.escapeXml(declaration));
					else 
						addRelation(element, "attributeType_", attribute.getType());

					// Next is not needed. It is done in the Element() constructor for all items in a generic way.
					addProperty(element, Element.VISIBILITY, Element.visibilityLUT.get(attribute.getVisibility()));
					IRPTag direction = irpElement.getTag("direction");
					if (null != direction)
						addProperty(element, Element.PORT_DIRECTION, Element.portDirectionLut.get(direction.getValue()));
				}
				else if (ModelHelpers.isTag(irpElement)) {
					IRPTag tag = (IRPTag)irpElement;
					//					tag.getMultiplicity();
					String declaration = ModelHelpers.getTypeDeclaration(tag.getType());
					if (null != declaration)
						addProperty(element, Element.TYPE_DECLARATION, StringEscapeUtils.escapeXml(declaration));
					else
						addRelation(element, "tagType_", tag.getType());
					addProperty(element, Element.MULTIPLICITY, Element.multiplicityLUT.get(tag.getMultiplicity()));
//					String value = tag.getValue();
//					List<String> values = new ArrayList<String>();
//					if (null != value)
//						values.add(value);
//					List<RPModelElement> nValues = tag.getValueSpecifications().toList();
//					for (RPModelElement v: nValues) {
//						values.add(vs);
//					}
					// Now add them as properties:
//					for (String val: values)
//						addProperty(element, "tagValue_", val);
				}
				else if (ModelHelpers.isPrimitiveOperation(irpElement) || ModelHelpers.isTriggeredOperation(irpElement)) {
					addRelation(element, "returnType_", ((IRPOperation)irpElement).getReturns());
				}
				else if (ModelHelpers.isIRPDependency(irpElement)) {
					IRPModelElement dependensOn = ((IRPDependency)irpElement).getDependsOn();
					if (ModelHelpers.isIRPClass(dependensOn))
						addRelation(element, "dependsOn_", dependensOn);
				}
				else if (ModelHelpers.isReception(irpElement) ) {
					addRelation(element, "onEvent_", ((IRPEventReception)irpElement).getEvent());
				}
			}
			catch (Exception e) {
				pw.println("WARNING: Element [" + irpElement.getName() + "] of class [" + irpElement.getUserDefinedMetaClass() + "] could not be exported");
				pw.println("  Reason: [" + e.getClass() + ": " + e.getMessage());
				StackTraceElement st[] = e.getStackTrace();
				for (StackTraceElement ste: st) {
					pw.println(ste.toString());
				}
			}
		}

		return msg.toString();
	}


	public interface IMethod {
		public boolean apply(String relation, IRPModelElement owner, IRPModelElement component,
				WorkModel ctx, List<Element> list, String...type);
	}

	public boolean classifyComponent(IMethod method, IRPModelElement owner, IRPModelElement component,
			WorkModel ctx, List<Element> list, RhpPlugin rhpPlugin) {
//		if (component.getName().equals("wiper"))
//			System.out.println(owner.getName());
		if (ModelHelpers.isUpdmFunction(component)) 
				return method.apply("updm:Function", owner, component, ctx, list, "updm:Function");

		if (ModelHelpers.isUpdmComponent(component)) 
			return method.apply("updm:Component", owner, component, ctx, list, "updm:Component");

		if (ModelHelpers.isUpdmSystem(component)) 
			return method.apply("updm:System", owner, component, ctx, list, "updm:System");

		if (ModelHelpers.isUpdmResourceInterface(component)) 
			return method.apply("updm:ResourceInterface", owner, component, ctx, list, "updm:ResourceInterface");

		if (ModelHelpers.isUpdmResourcePort(component)) 
			return method.apply("updm:ResourcePort", owner, component, ctx, list, "updm:ResourcePort");

		if (ModelHelpers.isIRPStatechart(component))
			return false; // ignore it
		else if (ModelHelpers.isIRPProfile(component))
			return false;
		else if (ModelHelpers.isIRPGeneralization(component))
			return method.apply("generalization", owner, component, ctx, list);
		else if (ModelHelpers.isIRPStereotype(component) && false == ModelHelpers.isIncludedStereotype(component))
			return method.apply("defStereotype", owner, component, ctx, list);
		else if (ModelHelpers.isIRPEvent(component))
			return method.apply("event", owner, component, ctx, list);
		else if (ModelHelpers.isInterface(component)) {
			if (ModelHelpers.hasImplicitInterface(owner))
				return true;
			return method.apply("interface", owner, component, ctx, list);
		} else if (ModelHelpers.isTag(component)) {
			return method.apply("tag", owner, component, ctx, list);
		} else if (ModelHelpers.isIRPPort(component))
			return method.apply("port", owner, component, ctx, list);
		else if (ModelHelpers.isFlowPort(component))
			return method.apply("port", owner, component, ctx, list);
		else if (ModelHelpers.isFlow(component))
			return method.apply("flow", owner, component, ctx, list);
		else if (ModelHelpers.isObject(component))
			return method.apply("part", owner, component, ctx, list);
		else if (ModelHelpers.isOperation(component))
			return method.apply("operation", owner, component, ctx, list);
		else if (ModelHelpers.isIRPEventReception(component))
			return method.apply("reception", owner, component, ctx, list);
		else if (ModelHelpers.isIRPPackage(component))
			return method.apply("package", owner, component, ctx, list);
		else if (ModelHelpers.isAttribute(component))
			return method.apply("attribute", owner, component, ctx, list);
		else if (ModelHelpers.isIRPLink(component)) {
			return method.apply(ModelHelpers.isConnector(component)?"connector":"link", owner, component, ctx, list);
//		else if (ModelHelpers.isIRPConnector(component))
//			return method.apply("connector", owner, component, ctx, list);
		} else if (ModelHelpers.isIRPInstanceValue(component)) {
			IRPInstanceValue v = (IRPInstanceValue)component;
			IRPModelElement vv = v.getValue();
//			String n = vv.getName();
			return method.apply("instanceValue", owner, vv, ctx, list);
		}
		else if (ModelHelpers.isIRPType(component)) {
			if (null == ModelHelpers.getTypeDeclaration((IRPType)component))
				return method.apply("dataType", owner, component, ctx, list);
			else
				return true;
		}
		else if (ModelHelpers.isIRPLiteralSpecification(component)) {
			Element parentElement = getElement(owner, list, null);
			if (null == owner) {
				System.err.println("Cannot find owner of an IRP Literal Specification! [" + component.getFullPathName() + "].");
				return true;
			} else if (false == ModelHelpers.isTag(owner)) {
				System.err.println("Owner of Literal Specification! [" + component.getFullPathName() + "] is [" + owner.getFullPathName() + "], and is not a Tag. Processed as 'hasValue' anyway.");
			}
			parentElement.addProperty(Element.VALUE, ((IRPLiteralSpecification)component).getValue());
			return true;
		}
		else if (ModelHelpers.isIRPDependency(component)) {
			if (component.getUserDefinedMetaClass().equals(IRhpConstants.ActivityPerformedByPerformer))
				return method.apply(IRhpConstants.RHP_UPDM_MODEL_PREFIX + ":ActivityPerformedByPerformer", owner, component, ctx, list);
			else
				return method.apply("dependency", owner, component, ctx, list);
		}
		else if (ModelHelpers.isIRPAnnotation(component)) {
			if (ModelHelpers.isIRPConstraint(component)) {
				return method.apply("constraint", owner, component, ctx, list);
			}
			else if (ModelHelpers.isIRPComment(component)) {
				return method.apply("comment", owner, component, ctx, list);
			}
			else if (ModelHelpers.isIRPRequirement(component)) {
				return method.apply("requirement", owner, component, ctx, list);
			} else
				return method.apply("annotation", owner, component, ctx, list);
		}
		else if (ModelHelpers.isObject(component)) {
			if (ModelHelpers.hasImplicitType(owner))
				return true;
			return method.apply((ModelHelpers.isBlock(component))?"block":"class",
					owner, component, ctx, list);
		} else if (ModelHelpers.isModelDiagram(component)) {
			rhpPlugin.console("Info: diagrams [" + component.getMetaClass() + "] are not handled.");
			return true;
		} else if (ModelHelpers.isBlock(component)) {
			return method.apply("block", owner, component, ctx, list);
		} else
			rhpPlugin.console("Warning: unhandled relation between ["+ owner.getMetaClass() + " " + owner.getFullPathName() + "]" + 
					"\n\t\t\t\t\t\t\tand its component [" + component.getMetaClass() + " " + component.getFullPathName() + "].");
		    return false;
//			return method.apply("OTHER", owner, component, ctx, list);
	}

	public void addRelations(Element element, String relation, Collection<IRPModelElement> others, String...type) {
		for(IRPModelElement other: others)
			addRelation(element, relation, other, type);
	}

	public void addProperty(Element element, String property, String value) {
		element.addProperty(property, value);
		debugTrace(property + "(" + element.getId() + ") -> [" + value + "]");
	}

	public void addProperty(Element element, String property, String value, String type) {
		element.addProperty(property, value + "^^xsd:" + type);
		debugTrace(property + "(" + element.getId() + ") -> [" + value + "^^" + type + "]");
	}

	/**
	 * When the property is a types opject - use this method.
	 * @param element
	 * @param isReversed
	 * @param boolean1
	 */
	private void addProperty(Element element, String property,
			Object value) {
		element.addProperty(property, value);
	}

	public static Collection<IRPModelElement> asCollection(IRPCollection irpCollection) {
		ArrayList<IRPModelElement> list = new ArrayList<IRPModelElement>();
		for(Object irpModelElement: irpCollection.toList()) {
			list.add((IRPModelElement) irpModelElement);
		}
		return list;
	}
}
