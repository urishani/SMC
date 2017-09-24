
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import org.apache.jena.rdf.model.*;
import com.ibm.haifa.mediator.base.Rules;
import com.ibm.haifa.sm.mediator.utils.StronglyTypedMap;
import com.ibm.haifa.sm.mediator.utils.StronglyTypedSet;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class MultiRestrictionEquivalence extends Rules {

	private final Map<Restrictions, Restrictions> replace = new StronglyTypedMap<Restrictions, Restrictions>(Restrictions.class, Restrictions.class);
	private final Map<Restrictions, Resource> types = new StronglyTypedMap<Restrictions, Resource>(Restrictions.class, Resource.class);

	public String toString() {
		return "MultiRestrictionEquivalence:\n" +
				"replace:\n" + Joiner.on("\n\t").withKeyValueSeparator(" ==>> ").join(replace) + 
				"\ntypes:\n" + Joiner.on("\n\t").withKeyValueSeparator(" ==>> ").join(types);
	}
	
	public MultiRestrictionEquivalence(Model model) {
		super(model);

		StmtIterator it = model.listStatements();
		while(it.hasNext()) {
			Statement stmt = it.next();
			Resource s = stmt.getSubject();
			Property p = stmt.getPredicate();
			RDFNode o = stmt.getObject();

			Restrictions a = Restrictions.newRestrictions(model, s); //stmt.getSubject());
			Restrictions b = Restrictions.newRestrictions(model, o); //stmt.getObject());

			if (a != null && b != null && p.equals(SAME_AS)) {
				replace.put(a, b);
				replace.put(b, a);
			}
			if (a != null && p.equals(EQUIVALENT_CLASS) && o != null && o.isURIResource())
				types.put(a, o.asResource());
			if (b != null && p.equals(EQUIVALENT_CLASS) && s != null && s.isURIResource())
				types.put(b, s);
		}
	}

	public List<Statement> get(Model model, Resource subj, String source, String target) {
		Map<Property, Set<RDFNode>> map = new StronglyTypedMap(Property.class, Set.class);
		StmtIterator it = model.listStatements(subj, null, (RDFNode)null);
		while (it.hasNext()) {
			Statement statement = it.next();
			Property pred = statement.getPredicate();
			RDFNode obj = statement.getObject();
			if (!map.containsKey(pred))
				map.put(pred, new StronglyTypedSet<RDFNode>(RDFNode.class));
			map.get(pred).add(obj);
		}

		boolean applies;

		applies = false;
		out: for (Restrictions r : types.keySet()) {
			Resource type = types.get(r);
			if (type.getURI().startsWith(source))
				continue;

			if (!testRestrictions(model, r, subj, map))
				continue out;

			applies = true;
			for (Property p : r.valueMap.keySet())
				map.get(p).remove(r.valueMap.get(p));

			map.get(TYPE).add(type);
		}

		if (applies) {
			List<Statement> res = new ArrayList<Statement>();
			for (Property p : map.keySet())
				for (RDFNode o : map.get(p))
					res.add(model.createStatement(subj, p, o));
			return res;
		}

		applies = false;
		out: for (Restrictions r : replace.keySet()) {
			Restrictions s = replace.get(r);
			if (!s.target.equals(target))
				continue;

			if (!testRestrictions(model, r, subj, map))
				continue out;

			applies = true;
			for (Property p : r.valueMap.keySet())
				map.get(p).remove(r.valueMap.get(p));

			for (Property p : s.valueMap.keySet()) {
				if (!map.containsKey(p))
					map.put(p, new StronglyTypedSet<RDFNode>(RDFNode.class));
				map.get(p).add(s.valueMap.get(p));
			}
		}

		if (applies) {
			List<Statement> res = new ArrayList<Statement>();
			for (Property p : map.keySet())
				for (RDFNode o : map.get(p))
					res.add(model.createStatement(subj, p, o));
			return res;
		}

		return null;
	}

	private boolean testRestrictions(Model model, Restrictions r, Resource subj, Map<Property, Set<RDFNode>> map)
	{
		for (Property prop : r.valueMap.keySet())
			if (map.containsKey(prop) && map.get(prop).contains(r.valueMap.get(prop))) {}
			else
				return false;

		Set<Property> pending = new StronglyTypedSet<Property>(Property.class);
		pending.addAll(r.classMap.keySet());

		for (Property prop : r.classMap.keySet()) {
			for (Property p : r.eqProps.get(prop)) {
				NodeIterator it = model.listObjectsOfProperty(subj, p);
				while(it.hasNext()) {
					RDFNode obj = it.next();
					if  (obj.isURIResource())
						if (r.classMap.get(prop).contains(getType(obj.asResource(), model)))
							pending.remove(prop);
				}
			}

			for (Property q : r.invProps.get(prop)) {
				ResIterator it = model.listSubjectsWithProperty(q, subj);
				while(it.hasNext()) {
					Resource s = it.next();
					if  (s.isURIResource())
						if (r.classMap.get(prop).contains(getType(s, model)))
							pending.remove(prop);
				}
			}
		}

		if (pending.size() > 0)
			return false;

		return true;
	}
}
