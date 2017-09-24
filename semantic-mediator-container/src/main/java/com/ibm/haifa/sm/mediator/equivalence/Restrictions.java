
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
package com.ibm.haifa.sm.mediator.equivalence;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.*;
import com.ibm.haifa.mediator.base.Rules;
import com.ibm.haifa.sm.mediator.utils.StronglyTypedMap;
import com.ibm.haifa.sm.mediator.utils.StronglyTypedSet;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class Restrictions extends Rules {

	public String toString() {
		return target + ": " + valueMap + ", "  + classMap;
	}

	public Map<Property, Set<Resource>> classMap = new StronglyTypedMap(Property.class, Set.class);
	public Map<Property, Set<Property>> eqProps  = new StronglyTypedMap(Property.class, Set.class);
	public Map<Property, Set<Property>> invProps = new StronglyTypedMap(Property.class, Set.class);

	public Map<Property, RDFNode> valueMap;
	public final String target;

	public static Restrictions newRestrictions(Model model, RDFNode node) {
		if (node != null && node.isResource() && node.isAnon()) {
			Restrictions j = new Restrictions(model, node.asResource());
			if (j.valueMap == null)
				return null;
			else
				return j;
		}
		return null;
	}

	private void init() {
		if (valueMap == null)
			valueMap = new HashMap<Property, RDFNode>();
	}

	private void init(Property p) {
		init();
		if (!classMap.containsKey(p))
			classMap.put(p, new StronglyTypedSet<Resource>(Resource.class));
		if (!eqProps.containsKey(p))
			eqProps.put(p, new StronglyTypedSet<Property>(Property.class));
		if (!invProps.containsKey(p))
			invProps.put(p, new StronglyTypedSet<Property>(Property.class));
	}

	private Restrictions(Model model, Resource subj) {
		super(model);
		assert subj.isAnon();

		RDFNode target = getNodeObject(subj, DOMAIN, model);
		this.target = target != null ? target.toString() : "";

		Resource node = getResourceObject(subj, INTERSECTION_OF, model);
		while (node != null) {
			parse(model, getResourceObject(node, FIRST, model));
			node = getResourceObject(node, REST, model);
		}

	}

	private void parse(Model model, Resource subj) {
		Statement stmt = getTypeStmt(subj, model);
		if (stmt != null) {
			RDFNode type = stmt.getObject();
			if (RESTRICTION.equals(type)) {
				Property prop = getPropertyObject(subj, ON_PROPERTY, model);
				RDFNode value = getNodeObject(subj, HAS_VALUE, model);
				Resource clazz = getUriResourceObject(subj, SOME_VALUES_FROM, model);
				if (prop != null && value != null) {
					init();
					valueMap.put(prop, value);
					return;
				}
				if (prop != null && clazz != null) {
					init(prop);

					eqProps.get(prop).add(prop);
					classMap.get(prop).add(clazz);

					for (Resource r : getOthers(model, prop, EQUIVALENT_PROPERTY))
						eqProps.get(prop).add(model.createProperty(r.getURI()));
					for (Resource r : getOthers(model, prop, INVERSE_OF))
						invProps.get(prop).add(model.createProperty(r.getURI()));
					classMap.get(prop).addAll(getOthers(model, clazz, EQUIVALENT_CLASS));
				}
			}
		}

		if (subj != null && subj.isURIResource()) {
			init();
			valueMap.put(TYPE, subj);
			return;
		}
	}
}
