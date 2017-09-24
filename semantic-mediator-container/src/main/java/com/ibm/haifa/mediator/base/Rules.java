
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
package com.ibm.haifa.mediator.base;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.*;
import com.ibm.dm.frontService.sm.utils.IConstants;

public abstract class Rules {
	protected static String OWL, RDF, RDFS, SM;

	protected static Property EQUIVALENT_CLASS, EQUIVALENT_PROPERTY, SAME_AS, INVERSE_OF;
	protected static Property SUB_CLASS_OF, SUB_PROPERTY_OF;
	protected static Property INTERSECTION_OF, RESTRICTION, HAS_VALUE, SOME_VALUES_FROM, ON_PROPERTY;
	protected static Property DOMAIN, RANGE, TYPE, FIRST, REST;

	protected static Property DEFAULT, CONSTANT, CONTAINER, PREDICATE;

	protected static String [] PUBLIC = { "http://www.w3.org/" };

	public Rules(Model rules)
	{
		OWL = rules.getNsPrefixURI("owl");
		RDF = rules.getNsPrefixURI("rdf");
		RDFS = rules.getNsPrefixURI("rdfs");
		SM = rules.getNsPrefixURI(IConstants.SM_PROPERTY_NS_PREFIX);

		EQUIVALENT_CLASS = rules.createProperty(OWL + "equivalentClass");
		EQUIVALENT_PROPERTY = rules.createProperty(OWL + "equivalentProperty");
		SAME_AS = rules.createProperty(OWL + "sameAs");
		INVERSE_OF = rules.createProperty(OWL + "inverseOf");

		SUB_CLASS_OF = rules.createProperty(RDFS + "subClassOf");
		SUB_PROPERTY_OF = rules.createProperty(RDFS + "subPropertyOf");

		INTERSECTION_OF = rules.createProperty(OWL + "intersectionOf");
		RESTRICTION = rules.createProperty(OWL + "Restriction");
		HAS_VALUE = rules.createProperty(OWL + "hasValue");
		SOME_VALUES_FROM = rules.createProperty(OWL + "someValuesFrom");
		ON_PROPERTY = rules.createProperty(OWL + "onProperty");

		DOMAIN = rules.createProperty(RDFS + "domain");
		RANGE = rules.createProperty(RDFS + "range");
		TYPE = rules.createProperty(RDF + "type");
		FIRST = rules.createProperty(RDF + "first");
		REST = rules.createProperty(RDF + "rest");

		DEFAULT = rules.createProperty(SM + "default");
		CONSTANT = rules.createProperty(SM + "constant");
		CONTAINER = rules.createProperty(SM + "container");
		PREDICATE = rules.createProperty(SM + "predicate");
	}

	//TODO what if there are multiple types to the resource? We strongly assume that there is only one type to any resource.
	protected Statement getTypeStmt(Resource res, Model model) {
		if (res != null) {
			StmtIterator it = model.listStatements(res, TYPE, (RDFNode)null);
			while (it.hasNext()) {
				Statement stmt = it.next();
				RDFNode node = stmt.getObject();
				if (node.isURIResource()) {
					String resType = node.asResource().getURI();
					if (resType != null)
						return stmt;
				}
			}
		}
		return null;
	}

	protected Resource getType(Resource res, Model model) {
		Statement stmt = getTypeStmt(res, model);
		if (stmt != null)
			return stmt.getObject().asResource();
		return null;
	}

	protected RDFNode getNodeObject(Resource subj, Property pred, Model model) {
		if (subj != null) {
			NodeIterator it = model.listObjectsOfProperty(subj, pred);
			if (it.hasNext())
				return it.next();
		}
		return null;
	}

	protected Resource getUriResourceObject(Resource subj, Property pred, Model model) {
		if (subj != null) {
			NodeIterator it = model.listObjectsOfProperty(subj, pred);
			while (it.hasNext()) {
				RDFNode obj = it.next();
				if (obj.isURIResource())
					return obj.asResource();
			}
		}
		return null;
	}

	protected Resource getResourceObject(Resource subj, Property pred, Model model) {
		if (subj != null) {
			NodeIterator it = model.listObjectsOfProperty(subj, pred);
			while (it.hasNext()) {
				RDFNode obj = it.next();
				if (obj.isResource())
					return obj.asResource();
			}
		}
		return null;
	}

	protected Property getPropertyObject(Resource subj, Property pred, Model model) {
		if (subj != null) {
			Resource res = getUriResourceObject(subj, pred, model);
			if (res != null)
				return model.createProperty(res.getURI());
		}
		return null;
	}

	protected void p (Object o) {
		System.out.println(o);
	}

	protected List<Resource> getOthers(Model model, Resource s, Property p) {
		StmtIterator it;
		List<Resource> res = new ArrayList<Resource>();

		it = model.listStatements(s, p, (RDFNode)null);
		while (it.hasNext()) {
			Statement stmt = it.next();
			RDFNode obj = stmt.getObject();
			if (obj != null && obj.isURIResource())
				res.add(obj.asResource());
		}

		it = model.listStatements(null, p, s);
		while (it.hasNext()) {
			Statement stmt = it.next();
			Resource subj = stmt.getSubject();
			if (subj != null && subj.isURIResource())
				res.add(subj);
		}

		return res;
	}
}
