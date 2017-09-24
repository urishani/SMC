
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

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.rhapsody.sm.IRhpConstants;
import com.ibm.rhapsody.sm.IRhpConstants.RHP_USR_CLASS_NAME;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPProject;

public class Repository {
	public class Element extends HashMap<String, Object>{
		private static final long serialVersionUID = 1L;};
		
	public Map<String, Element> elements = new HashMap<String, Element>();
	public PrintStream out = null;
	public File tmpFile = null;
	public String modifiedDate;
	public static 	SimpleDateFormat sSdf = new SimpleDateFormat();
	public static String[] sInlineAtts = new String[] {IRhpConstants.NAME, IRhpConstants.USR_CLASS_NAME, IRhpConstants.MODIFIED, IRhpConstants.VERSION};
	protected Properties m_sii_guid_mapping;

	public Repository(IRPProject project, Properties p_sii_guid_mapping) throws Exception  {
		m_sii_guid_mapping = p_sii_guid_mapping;
		sSdf.applyPattern(IRhpConstants.datePattern); //"2011-08-22T23:34:45.978+03:00" --> "2011-08-22T23:34:45.978+0300"
		String modified = project.getSaveUnit().getFilename();
		Date modifiedT = new Date(new File(modified).lastModified());
		modifiedDate = sSdf.format(modifiedT);
		tmpFile = File.createTempFile(project.getName(), ".xml");
		out = new PrintStream(tmpFile);
		out.print(
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				"<rdf:RDF\nxmlns:" + IRhpConstants.RHP_MODEL_PREFIX + 
				"=\"" + IRhpConstants.RHP_ONTOLOGY_NS + "\"\n" +
				"xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
				"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");

		checkType(project, RHP_USR_CLASS_NAME.SysML.toString(), RHP_USR_CLASS_NAME.Project.toString());
		if (addPart(project)) {
			Element projectElement = elements.get(getGUID(project));
			projectElement.put(IRhpConstants.MODIFIED, modifiedDate);
			projectElement.put(IRhpConstants.VERSION, IRhpConstants.VERSION_LEVEL);
		}

	}
	
	/**
	 * Generates a GUID that can be valid as part of a URI
	 * @param part a Rhapsody element
	 * @return String for the modified GUID of that element.
	 */
	public static String getGUID(IRPModelElement part) {
		String guid = part.getGUID();
		return guid.replace(' ', '-');
	}
	
	/**
	 * Add a part and reports whether the part was created or if it exists already.
	 * @param part Rhp model component to be added
	 * @return boolean indicating successful or failure to add the part.
	 */
	public boolean addPart(IRPModelElement part) {
		String guid = getGUID(part);
		Element element = elements.get(guid);
		if (null != element)
			return true;
		element = new Element();
		try {
			String metaClass = part.getUserDefinedMetaClass();
//			RHP_USR_CLASS_NAME className = RHP_USR_CLASS_NAME.valueOf(RHP_USR_CLASS_NAME.class, metaClass);
			element.put(IRhpConstants.NAME, part.getName());
			String description = part.getDescriptionHTML();
			if (null != description && false == "".equals(description.trim()))
				element.put(IRhpConstants.DESCRIPTION, description);
			element.put(IRhpConstants.USR_CLASS_NAME, metaClass);
			String siiUri = m_sii_guid_mapping.getProperty(getGUID(part)); //part.getPropertyValueExplicit(IRhpConstants.SII_URI);
//			if (null != siiUri) {
//				element.put(IRhpConstants.SII_URI, siiUri);
//			}
//			switch (className) {
//			case Flow: RPFlow flow = (RPFlow)part;
//				String direction = flow.getDirection();
//				element.put(IRhpConstants.RHP_PROPERTY_FLOW_DIRECTION, direction);
////				printStatement(out, guid, IRhpConstants.RHP_PROPERTY_FLOW_DIRECTION, false, direction);
//				break; 
//			}
			elements.put(guid, element);
			return true;
		} catch (Exception e) {
			return false;
		}

	}

	/**
	 * utility for debugging
	 * @param list
	 * @return
	 */
	private String flatten(String title, String[] list) {
		StringBuffer sb = new StringBuffer();
		for (String item : list) {
			sb.append(item +  (sb.length() > 0?", ":""));
		}
		return "[" + sb.append("]").toString();
	}
	
	private void checkType(IRPModelElement part, String... expected) throws Exception {
		String metaClass = part.getUserDefinedMetaClass();
		List<String> list = Arrays.asList(expected);
		if (expected.length > 0 && false == list.contains(metaClass))
			throw new Exception("Meta class of element [" + part.getName() + "] is [" + metaClass + "]. Possible values are " + flatten("", expected) + ".");
	}
	/**
	 * Create a boolean relation
	 * @param element
	 * @param siiTag
	 * @param b boolean true if this element has been tagged with sii.
	 */
//	private void addBoolean(Element element, String tag, boolean b) {
//		element.put(tag, new Boolean(b));
//	}

	public void close() throws Exception {
		if (null == out)
			return;
		traverse(elements);
		out.print ("</rdf:RDF>\n");
		out.close();
	}

	@SuppressWarnings("unchecked")
	private void traverse(Map<String, Element> elements) throws Exception {
		Set<String> guids = elements.keySet();
		for (String guid : guids) {
			out.print("<rdf:Description rdf:about=\"" + IRhpConstants.RHP_INSTANCE_NS + guid + "\"\n   ");
			Map<String, Object> element = elements.get(guid);
			Set<String> atts = element.keySet();
			List<String> inlines = Arrays.asList(sInlineAtts);
			for (String att : atts) {
				if (inlines.contains(att)) {
					String val =  element.get(att).toString();
					//val = XML.escape(val);
					out.print(IRhpConstants.RHP_MODEL_PREFIX + ":" + att + "=\"" + val + "\"\n   ");
				}
			}
			out.print(">\n   ");
			for (String att : atts) {
				if (inlines.contains(att))
					continue;
				Object val = element.get(att);
//				if (val instanceof Set && ((Set)val).size() == 1)
				//						val = ((Set)val).toArray()[0];
				if (val instanceof String) {
					out.print("<" + IRhpConstants.RHP_MODEL_PREFIX + ":" + att + ">\n      " + val + "\n   </" + IRhpConstants.RHP_MODEL_PREFIX + ":" + att + ">\n   ");
				} else if (val instanceof Set<?>) {
					Set<String>valSet = (Set<String>)val;
					if (valSet.size() > 0) {
						if (IRhpConstants.VERSION_LEVEL.equals("1.0")) {
							out.print("<" + IRhpConstants.RHP_MODEL_PREFIX + ":" + att + "><rdf:Seq>\n       ");
							int seq = 1;
							for (String value : (Set<String>)val) {
								out.print("<rdf:_" + seq + " rdf:resource=\"" + value + "\"/>\n       ");
								seq++;
							}
							out.print("</rdf:Seq></" + IRhpConstants.RHP_MODEL_PREFIX + ":" + att + ">\n   ");
						} else { // for versions 2.0 and up
							for (String value : (Set<String>)val) {
								out.print("<" + IRhpConstants.RHP_MODEL_PREFIX + ":" + att + 
										" rdf:resource=\"" + value + "\"/>\n   ");
							}
//							out.print("</rdf:Seq></" + IRhpConstants.RHP_MODEL_PREFIX + ":" + att + ">\n   ");
						}
					} else {
						/*out.print("<" + IRhpConstants.RHP_MODEL_PREFIX + ":" + att + " rdf:resource=\"" + ((Set)val).toArray()[0] + "\"/>\n   ");*/
					}
				} else
					throw new Exception ("Value of attribute [" + att + "] is improper.");
			}
			out.print("</rdf:Description>\n");
		}
	}

	/**
	 * Adds a relation to an existing element GUID. 
	 * @param guid
	 * @param prop
	 * @param part
	 * @throws Exception in case element is missing.
	 */
	@SuppressWarnings("unchecked")
	public void addRelation(String guid, String prop, IRPModelElement part) throws Exception {
		Map<String, Object> element = elements.get(guid);
		if (null == element)
			throw new Exception("Element [" + guid + "] does not exist.");
		if (false == addPart(part))
			throw new Exception("Adding a relation [" + prop + "] to illegal part [" + getGUID(part) + "].");
		Set<String> values = (Set<String>) element.get(prop);
		if (null == values) {
			values = new HashSet<String>();
			element.put(prop, values);
		}
		values.add(IRhpConstants.RHP_INSTANCE_NS + getGUID(part));
//		printRelation(out, guid, IRhpConstants.PART, " :" + getGUID(part));
	}
	public void addTypeRelation(String guid, IRPModelElement type) throws Exception {
		addRelation(guid, IRhpConstants.TYPE, type);
	}
	public void addPartRelation(String guid, IRPModelElement part) throws Exception {
		addRelation(guid, IRhpConstants.PART, part);
	}
	
}
