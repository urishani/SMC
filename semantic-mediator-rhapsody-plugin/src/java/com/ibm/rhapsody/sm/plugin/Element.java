
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Strings;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.ibm.rhapsody.sm.IRhpConstants;
import com.ibm.rhapsody.sm.rdfsync.Helper;
import com.telelogic.rhapsody.core.IRPModelElement;


/**
 * Create elements and serialize them as RDF resources.<p>
 * Use these methods:<br>
 * <ol>
 * <li>Element(part) creates the part for a certain GUID, and sets up some literal properties.
 * <li>AddRelation(prop, part) - adds a relation with property prop, and for another element
 * <li>AddProperty(prop, val) - adds a literal property.
 * @author shani
 *
 */
public class Element extends HashMap<String, Object> {
//public String get(String v) { return "";};
//public void put(String k, Object o) {};

//	public final static String  RHP_MODEL_PREFIX = "rhp_model";
//	public final static String  RHP_MODEL_NS = "http://com.ibm.rhapsody/sprint/";
	public final static String  TITLE = "dc:title";
	public final static String  CLASS = "rdf:type";
//	public final static String  SPRINT_RESOURCE = IRhpConstants.RHP_MODEL_PREFIX + ":hasSprintResource";
	public final static String  VERSION = IRhpConstants.RHP_MODEL_PREFIX + ":version";
	public final static String  MULTIPLICITY = IRhpConstants.RHP_MODEL_PREFIX + ":hasMultiplicity";
	public final static String  PORT_DIRECTION = IRhpConstants.RHP_MODEL_PREFIX + ":hasPortDirection";

	public final static String  MULTIPLICITY_1 = IRhpConstants.RHP_MODEL_PREFIX + ":hasMultiplicity_1";
	public final static String  MULTIPLICITY_2 = IRhpConstants.RHP_MODEL_PREFIX + ":hasMultiplicity_2";
	public final static String  VISIBILITY = IRhpConstants.RHP_MODEL_PREFIX + ":hasVisibility";
	public static final String  FLOW_DIRECTION = IRhpConstants.RHP_MODEL_PREFIX + ":hasFlowDirection";
	public final static String  IS_REVERSED = IRhpConstants.RHP_MODEL_PREFIX + ":isReversed";
	public final static String  VALUE = IRhpConstants.RHP_MODEL_PREFIX + ":hasValue";
	public final static String  TYPE_DECLARATION = IRhpConstants.RHP_MODEL_PREFIX + ":hasTypeDeclaration";
	public final static String  TYPE = IRhpConstants.RHP_MODEL_PREFIX + ":hasType";
	public final static String  QUALIFIED_NAME= IRhpConstants.RHP_MODEL_PREFIX + ":hasQualifiedName";
	public final static String  HAS_GUID= IRhpConstants.RHP_MODEL_PREFIX + ":hasGUID";
	public final static String  APPLICABLE_TO= IRhpConstants.RHP_MODEL_PREFIX + ":isApplicableTo";
	public final static String  COMMENT= IRhpConstants.RHP_MODEL_PREFIX + ":hasComment";
	public final static String  CONSTRAINT= IRhpConstants.RHP_MODEL_PREFIX + ":hasConstraint";
	public final static String  REQUIREMENT= IRhpConstants.RHP_MODEL_PREFIX + ":hasRequirement";
	public final static String  RDF_TYPE = "rdf:type";

	public final static String  DESCRIPTION = "dc:description";

	private static final long serialVersionUID = -7322031395610497151L;
	private final String m_guid;
	private final Resource m_resource;
	private final WorkModel context;
	private final IRPModelElement m_modelElement;
	public static Collection<String> sInlineAtts, sResourceAtts, sTypedAtts;
	public static Map<String, String> multiplicityLUT = new HashMap<String, String>();
	public static Map<String, String> visibilityLUT = new HashMap<String, String>();
	public static Map<String, String> flowDirectionLUT = new HashMap<String, String>();
	public static Map<String, String> portDirectionLut = new HashMap<String, String>();

	static final Set<String> UPDM_TYPES = new HashSet<String>(Arrays.asList(
			new String[] {
				    "ResourcePort", "ResourceInterface", "System", "Component", "Function"}));

	// Initialize the statics
	static {
		multiplicityLUT.put("*", "rhp_model:AnyMultiplicity");
		multiplicityLUT.put("1", "rhp_model:ExactlyOneMultiplicity");
		multiplicityLUT.put("0,1", "rhp_model:AtMostOneMultiplicity");
		multiplicityLUT.put("1..*", "rhp_model:OneOrMoreMultiplicity");

		visibilityLUT.put("public", "PublicVisibility");
		visibilityLUT.put("private", "PrivateVisibility");
		visibilityLUT.put("protected", "ProtectedVisibility");

		flowDirectionLUT.put("bidirectional", "Bidirectional");
		flowDirectionLUT.put("toEnd1", "ToEnd1Direction");
		flowDirectionLUT.put("toEnd2", "ToEnd2Direction");

		portDirectionLut.put("InOut", "BidirectionalPort");
		portDirectionLut.put("Out", "OutputPort");
		portDirectionLut.put("In", "InputPort");

		sInlineAtts = Arrays.asList(new String[] {TITLE, VERSION, TYPE_DECLARATION, QUALIFIED_NAME, "rhp_model:GUID", VALUE, APPLICABLE_TO });
		sResourceAtts = Arrays.asList(new String[] {VISIBILITY, MULTIPLICITY, MULTIPLICITY_1, MULTIPLICITY_2, RDF_TYPE, FLOW_DIRECTION, PORT_DIRECTION });
		sTypedAtts = Arrays.asList(new String[] { IS_REVERSED} );
	}

	public String toString() {
		String l = super.toString().replaceAll(", ", ",\n\t");
		return getId() + "\n" + l;
	}
	public String getId() {
		return get(CLASS) + ":" + get(TITLE) + "[" + m_guid + "]";
	}
	


	/**
	 * Adds a literal property that will be typed if not a string to the resource of this element.
	 * @param prop prefixed property. If this is an rdf:type, the value will replace the current
	 * type of the resource. Otherwise, it will be added.
	 * @param val
	 */
	private void addLiteral(String prop, Object val) {
		Model model = context.getModel();
		Property p = model.createProperty(model.expandPrefix(prop));
		if (prop.equals(RDF_TYPE)) {
			m_resource.removeAll(p);
			System.err.println("Type should not be added as a literal for this element [" + toString() + "]");
		}
		if (val instanceof String)
			m_resource.addProperty(p, model.createLiteral(val.toString()));
		else
			m_resource.addProperty(p, model.createTypedLiteral(val));
	}

	/**
	 * Adds property also as an RDF property to the element resource in the Jena model. 
	 */
	private void addAsLiteral(String prop) {
		addLiteral(prop, get(prop));
	}
	/**
	 * Adds a literal property that will be typed if not a string to the resource of this element.
	 * @param prop prefixed property. If this is an rdf:type, the value will replace the current
	 * type of the resource. Otherwise, it will be added.
	 * @param val
	 */
	private void addResource(String prop, String val) {
		Model model = context.getModel();
		Property p = model.createProperty(model.expandPrefix(prop));
		if (val.contains(":"))
			val = model.expandPrefix(val);
		else
			val = IRhpConstants.RHP_ONTOLOGY_NS + val;
		if (prop.equals(RDF_TYPE)) {
			m_resource.removeAll(p);
		}
		m_resource.addProperty(p, model.createResource(val));
	}
	/**
	 * Adds resource property also as an RDF property to the element resource in the Jena model. 
	 */
	private void addAsResource(String prop) {
		addResource(prop, (String)get(prop));
	}
	

	public Element(IRPModelElement element, Properties sii_guid_mapping, String type, WorkModel ctx) {
		this.context = ctx;
		this.m_modelElement = element;
		this.m_guid = getGUID(m_modelElement);
		this.m_resource = context.getModel().createResource(IRhpConstants.RHP_INSTANCE_NS + m_guid);
		String metaClass = element.getUserDefinedMetaClass();
		if (ModelHelpers.isIRPProject(m_modelElement)) {
			if (metaClass.toUpperCase().startsWith("UPDM")) {
				metaClass = "SOS";
				type = IRhpConstants.RHP_UPDM_MODEL_PREFIX + ":SOS";
			} else {
				metaClass = "SysML";
				type = IRhpConstants.RHP_MODEL_PREFIX + ":SysML";
			}
		}
		else if (ModelHelpers.isIRPPackage(m_modelElement)) // Turns out the meta class is the stereotype of the package.
			metaClass = "Package";
//		String name = m_modelElement.getName(); 
//		if ("connector_26__1400000".equals(name)) {
//			System.out.println(name);
//		}
		put(TITLE, m_modelElement.getName());	
		addAsLiteral(TITLE);
		put("rhp_model:GUID", m_modelElement.getGUID()); 
		addAsLiteral("rhp_model:GUID");
		put("rhp_model:Metaclass", m_modelElement.getMetaClass()); 
		addAsLiteral("rhp_model:Metaclass");
		put(QUALIFIED_NAME, m_modelElement.getFullPathName()); 
		addAsLiteral(QUALIFIED_NAME);
		if (ModelHelpers.isIncludedStereotype(element)) {
			put(HAS_GUID, m_modelElement.getGUID()); 
			addAsLiteral(HAS_GUID);
//			put(QUALIFIED_NAME, m_modelElement.getFullPathName());
		}
		if (ModelHelpers.isIRPAnnotation(m_modelElement)) {
			put("rhp_model:hasValue", ModelHelpers.asIRPAnnotation(m_modelElement).getBody());
			addAsLiteral("rhp_model:hasValue");
		}
//		String visibility = ModelHelpers.getVisibility(m_modelElement);
//		if (null != visibility)
//			addProperty(VISIBILITY, Element.visibilityLUT.get(visibility));
		String description = m_modelElement.getDescription();
		if (false == Strings.isNullOrEmpty(description)) {
			description = Helper.uuidEncode(description);
			put(DESCRIPTION, description);
			addAsLiteral(DESCRIPTION);
		}
		if (ModelHelpers.isObject(element))
			metaClass = "Object";
		if (Strings.isNullOrEmpty(type)) {
			type = capitalize(metaClass);
			if (UPDM_TYPES.contains(metaClass))
				type = IRhpConstants.RHP_UPDM_MODEL_PREFIX + ":" + type;
		}
//		put(CLASS, type); 
		setType(type);
		String guid = Helper.uri2LegalGUID(IRhpConstants.RHP_INSTANCE_NS + m_guid); //part.getPropertyValueExplicit(IRhpConstants.SII_URI
		String siiUri = sii_guid_mapping.getProperty(guid);
		if (null != siiUri) {
			put(IRhpConstants.SM_PROPERTY_SM_RESOURCE, Arrays.asList(new String[] {siiUri}));
			addResource(IRhpConstants.SM_PROPERTY_SM_RESOURCE, siiUri);
		}
	}

	
	protected void setType(String type) {
		if (false == type.contains(":"))
			type = IRhpConstants.RHP_ONTOLOGY_NS + type;
			
		m_resource.removeAll(RDF.type);
		Model model = m_resource.getModel();
		Resource t = model.createResource(model.expandPrefix(type));
		if (false == t.toString().startsWith("http"))
			System.err.println("bad url");
		m_resource.addProperty(RDF.type, t);
		put(CLASS, type);
	}
	/**
	 * Adds an object property to a fixed resource which is an ontology individual, provided as a string.
	 * @param prop an object property suffix to be completed according to our convention.
	 * @param individual a String of the ontological individual member.
	 */
	public void addRelation(String prop, String individual) {
		String relation = IRhpConstants.RHP_MODEL_PREFIX + ":has" + capitalize(prop);
		addProperty(relation, individual);
		addResource(relation, individual);
	}

	/**
	 * Adds an object property to a resource in the model
	 * @param property
	 * @param part
	 */
	public boolean addRelation(String property, IRPModelElement part) {
		String prefix = IRhpConstants.RHP_MODEL_PREFIX;
		if (property.contains(":")) {
			String s[] = property.split(":");
			prefix = s[0];
			property = s[1];
		}
//		if (part instanceof IRPType) {
//			IRPType tp = (IRPType)part;
//			String n = tp.getName();
//			String fn = tp.getFullPathName();
//			String guid = tp.getGUID();
//			String d = tp.getDeclaration();
//			IRPProject prj = part.getProject();
//			IRPType ntp = prj.findType(tp.getName());
//			String nguid = ntp.getGUID();
//			System.err.println("----- debug addRelation("+property+", " + part + ")");
//		}
		String relation =  prefix +  ":has" + capitalize(property);
//		if (property.contains("attributeType"))
//			System.err.println("debugging attributeType");
		if (ModelHelpers.isInternalRhapsodyElement(part) || ModelHelpers.isBuiltInType(part)) {
			if (this.m_modelElement.getFullPathName().contains("_39"))
				System.err.println("_39");
			if (ModelHelpers.hasImplicitInterface(this.m_modelElement) ||
					ModelHelpers.hasImplicitType(this.m_modelElement))
				addResourceProperty(relation, IRhpConstants.IMPLICIT_TYPE);
			else
				addResourceProperty(relation, IRhpConstants.RHP_ONTOLOGY_NS + part.getName());
			return false;
		} else {
			addResourceProperty(relation, IRhpConstants.RHP_INSTANCE_NS + getGUID(part));
			return true;
		}
	}

	public static String capitalize(String prop) {
		return (prop.length() < 1)?"": Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
	}

	public void addProperty(String property, Object value) {
		if (value instanceof String && ((String)value).indexOf("Exactly") >= 0) {
			System.out.println(value);
		}
		if (!sResourceAtts.contains(property) || sInlineAtts.contains(property) || sTypedAtts.contains(property)) {
			put(property, Helper.uuidEncode((String)value));
			addAsLiteral(property);
		} else {
			String prefix = IRhpConstants.RHP_MODEL_PREFIX;
			if (property.contains(":")) {
				String s[] = property.split(":");
				prefix = s[0];
				property = s[1];
			}
			property = prefix + ":" + property;
			put(property, value);
//			addAsLiteral(property);
			addResource(property, (String)value);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addResourceProperty(String prop, String value) {
		Set<String> values = (Set<String>) get(prop);
		if (null == values) {
			values = new HashSet<String>();
			put(prop, values);
		}
		if (false == values.contains(value)) // ensure same property value is no assigned twice for same relation.
			values.add(value);
		addResource(prop, value);
	}

	/**
	 * Serializes the element into an RDF resource in RDF/XML format.
	 * @return String of the serialized element
	 */
	@SuppressWarnings("unchecked")
	public String serialize() {
		StringBuffer result = new StringBuffer(
				"<rdf:Description rdf:about=\"" + IRhpConstants.RHP_INSTANCE_NS + m_guid + "\"\n   ");
		Set<String> atts = keySet();
		for (String att : atts) {
			if (sInlineAtts.contains(att)) {
				String val =  get(att).toString();
				if (false == att instanceof String) // It should be a typed li8teral
					val = getModel().createTypedLiteral(att).toString();
				result.append(att + "=\"" + val + "\"\n   ");
			}
		}
		result.append(">\n   ");
		for (String att : atts) {
			if (sInlineAtts.contains(att))
				continue;
			Object val = get(att);
			if (null == val) {
				System.out.println("Att [" + att + "] in [" + this.toString() + "] is missing.");
				continue;
			}
			if (sTypedAtts.contains(att)) {
				String parts[] = ((String)val).split("\\^\\^");
				result.append("<" + att + " rdf:datatype=\"http://www.w3.org/2001/XMLSchema#" + parts[1] + "\">" + parts[0] + "</" + att + ">\n   " );
			} else if (val instanceof String) {
				if (sResourceAtts.contains(att)) {
					String ns = IRhpConstants.RHP_ONTOLOGY_NS;
					String v = (String)val;
					if (v.indexOf(':') >=0) {
						String vs[] = v.split(":");
						v = vs[1];
						if (vs[0].equals("updm"))
							ns = IRhpConstants.RHP_UPDM_ONTOLOGY_NS;
					}
					result.append("<" + att + " rdf:resource=\"" + ns + v + "\"/>\n   ");
//					result.append("<" + att + "> " + IRhpConstants.RHP_MODEL_PREFIX + ":" + val + "</" + att + ">\n   ");
				} else {
					result.append("<" + att + ">" + val + "</" + att + ">\n   ");
				}
			} else {
				Collection<String> valSet = (Collection<String>)val;
				if (null == valSet) {
					System.out.println("null");
				}
				if (valSet.size() > 0) {
					for (String value : valSet) {
						result.append("<" + att +
								" rdf:resource=\"" + value + "\"/>\n   ");
					}
				}
			}
		}
		result.append("</rdf:Description>\n");
		return result.toString();
	}

	private Model getModel() {
		return context.getModel();
	}
	/**
	 * Generates a GUID that can be valid as part of a URI
	 * @param part a Rhapsody element
	 * @return String for the modified GUID of that element.
	 */
	public static String getGUID(IRPModelElement part) {
		String guid = ModelHelpers.isOntologyPrimitive(part) ? part.getName() : part.getGUID();
		return guid.replace(' ', '-');
	}

	public final IRPModelElement getIrpModelElement() {
		return m_modelElement;
	}

	public final String getGUID() {
		return m_guid;
	}

	public static final String getHeader() {
		return
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				"<rdf:RDF\nxmlns:" + IRhpConstants.RHP_MODEL_PREFIX +
				"=\"" + IRhpConstants.RHP_ONTOLOGY_NS + "\"\n" +
				"xmlns:" + IRhpConstants.RHP_UPDM_MODEL_PREFIX +
				"=\"" + IRhpConstants.RHP_UPDM_ONTOLOGY_NS + "\"\n" +
				"xmlns:" + IRhpConstants.RHP_INSTANCE_PREFIX +
				"=\"" + IRhpConstants.RHP_INSTANCE_NS + "\"\n" +
				"xmlns:" + IRhpConstants.SM_PROPERTY_NS_PREFIX +
				"=\"" + IRhpConstants.SM_PROPERTY_NS + "\"\n" +
				"xmlns:dc=\"http://purl.org/dc/terms/\"\n" +
				"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n";
	}
	
	/**
	 * Alternative to getHeader() for using Jena.
	 * @return Map of prefixes and name spaces.
	 */
	public static final Map<String, String> getHeaderMap() {
		HashMap<String, String> m = new HashMap<String, String>();
		m.put(IRhpConstants.RHP_MODEL_PREFIX, IRhpConstants.RHP_ONTOLOGY_NS);
		m.put(IRhpConstants.RHP_INSTANCE_PREFIX, IRhpConstants.RHP_INSTANCE_NS);
		m.put(IRhpConstants.RHP_UPDM_MODEL_PREFIX, IRhpConstants.RHP_UPDM_ONTOLOGY_NS);
		m.put(IRhpConstants.SM_PROPERTY_NS_PREFIX, IRhpConstants.SM_PROPERTY_NS);
		m.put("dc", "http://purl.org/dc/terms/");
		return m;
	}

	public static final String getFooter() {
		return "</rdf:RDF>";
	}

	/**
	 * Either use this method, or take it as an example for producing the output file.
	 * @param elements List of elements to be serialized.
	 * @param out PrintStream to serialize to.
	 * @throws IOException
	 */
	public static void serializeAll(Collection<Element> elements, FileWriter out) throws IOException {
		
		out.write(getHeader());
		for (Element element : elements) {
			out.write(element.serialize());
			out.write("\n");
		}
		out.write(getFooter());
		out.close();
	}

	/**
	 * Checks if a link already exists for an element
	 * @param element
	 * @param relation
	 * @return
	 */
	public boolean linkedWith(Element element, String relation) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
