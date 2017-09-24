
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
package com.ibm.haifa.sm.mediator;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.jena.rdf.model.*;

import com.google.common.base.Joiner;
import com.ibm.dm.frontService.sm.utils.Utils;
import com.ibm.haifa.mediator.base.Rules;
import com.ibm.haifa.sm.mediator.equivalence.MultiRestrictionEquivalence;
import com.ibm.haifa.sm.mediator.equivalence.SimpleEquivalence;
import com.ibm.haifa.sm.mediator.utils.StronglyTypedMap;

public class Mediator extends Rules {

	private static void writeFile (String folder, String file, Model output) throws Exception
	{
		PrintWriter w = new PrintWriter(System.out); //new PrintWriter(new FileWriter(new File(folder, file)));
		//PrintWriter x = new PrintWriter(new FileWriter(new File(folder, "triples.rdf")));
		output.write(w, "TURTLE");
		//output.write(x, "N-TRIPLES");
		w.close();// x.close();
	}

	StringWriter sw = new StringWriter();
	private PrintWriter mediationTrace = new PrintWriter(sw);
	@Override
	protected void p(Object o) {
		// TODO Auto-generated method stub
		super.p(o);
		mediationTrace.println(o.toString());
	}
	
	public String getMediationTrace() {
		return sw.toString();
	}

	private static Model readModel (String folder, String file) throws Exception
	{
		System.out.println("Reading [" + folder + "/" + file + "]");
		Model model = ModelFactory.createDefaultModel();
		model.read(new FileReader(new File(folder, file)), null, "RDF/XML"); //"TURTLE");
		return model;
	}

	private static PrintWriter pw = pw();

	private static PrintWriter pw() {
		try {
			return new PrintWriter(System.out);
			//return new PrintWriter("output.txt");
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	public static void main (String [] args) throws Exception
	{
		long t = System.currentTimeMillis();

		/* args: input-ontology.rdf output-ontology.rdf rules.rdf input-model.rdf output-model.rdf */

		if (args.length < 4) {
			System.out.println("Params: inOntologyId, outOntologyId, rulesOntologyId, inputModelId");
			return;
		}
		System.out.println(new File("").getAbsolutePath());
		String f = "D:/Eclipse/eclipse-jee-luna-SR2-win32-x86_64"; //new File("").getAbsolutePath();
		Model inputOntology = readModel(f+"/SM.model.rdf/Ontology/"+ args[0], "ontology.owl"), //., args[0], args[1]),
			  outputOntology = readModel(f+"/SM.model.rdf/Ontology/"+ args[1], "ontology.owl"),//args[0], args[2]),
			  rules = readModel(f+"/SM.model.rdf/RuleSet/"+ args[2], "ruleset.owl"),//args[0], args[3]),
			  inputModel = readModel(f+"/SM.model.rdf/Port/"+ args[3], args[3]+".owl"); //args[0], args[4]);

		Mediator mediator = new Mediator(inputOntology, outputOntology, rules);
		Model output = mediator.mediate(inputModel);
		writeFile("","",output); //args[0], args[5], output);
		pw.println("Done! [" + (System.currentTimeMillis() - t) + "]");
		pw.close();
	}

	// END OF HELPER STUFF

	private final String INPUT_BASE, OUTPUT_BASE;
	private final String [] OUTPUT_EXTENDED_BASE;

	final Model inputOntology, outputOntology, rules;
	final SimpleEquivalence classes, properties, values, superClasses, superProperties;
	final MultiRestrictionEquivalence multiRestriction;

	//private String MODEL_INPUT_BASE;

	public Mediator(Model inputOntology, Model outputOntology, Model rules)
	{
		super(rules);

		this.inputOntology = inputOntology;
		this.outputOntology = outputOntology;
		this.rules = rules;

		//TODO what is the proper NS for an ontology, not necessarily the "base" prefix.
		INPUT_BASE = Utils.getOntologyNS(inputOntology); //.getNsPrefixURI(inputNsPrefix);
		OUTPUT_BASE = Utils.getOntologyNS(outputOntology); //.getNsPrefixURI(outputNsPrefix);
		OUTPUT_EXTENDED_BASE = outputOntology.getNsPrefixMap().values().toArray(new String [0]);

		classes = new SimpleEquivalence(rules, new Property [] { EQUIVALENT_CLASS }, new Property [0]);
		pmap("Classes", classes.getReplace(OUTPUT_BASE));
		properties = new SimpleEquivalence(rules, new Property [] { EQUIVALENT_PROPERTY }, new Property [0]);
		pmap("Properties", properties.getReplace(OUTPUT_BASE));
		values = new SimpleEquivalence(rules, new Property [] { SAME_AS }, new Property [0]);
		pmap("Values", values.getReplace(OUTPUT_BASE));
		superClasses = new SimpleEquivalence(rules, new Property [] { EQUIVALENT_CLASS }, new Property [] { SUB_CLASS_OF });
		pmap("SuperClasses", superClasses.getReplace(OUTPUT_BASE));
		superProperties = new SimpleEquivalence(rules, new Property [] { EQUIVALENT_PROPERTY }, new Property [] { SUB_PROPERTY_OF });
		pmap("SuperProperties", superProperties.getReplace(OUTPUT_BASE));

		multiRestriction = new MultiRestrictionEquivalence(rules);
		p(multiRestriction.toString());
	}

	private void pmap(String title, Map<Resource, Resource> replace) {
		p(title + ": " + (null==replace?"null":"\n\t" + Joiner.on("\n\t").withKeyValueSeparator(" = ").join(replace)));		
	}

	public Model mediate(Model inputModel) {

		//Model output = input;
		Model output = ModelFactory.createDefaultModel();
		output.add(inputModel);

		// This one is not-at-all essential, so we may let it fail
		try { output.setNsPrefix(baseOutputName(), OUTPUT_BASE); }
		catch (Exception e) { e.printStackTrace(); }

		// 1. Expand/Contract by domain/class/range
		expandByDomainClassRange(output);
		contractByDomainClassRange(output);

		// 2. Infer sub-property from range
		inferSubPropertyFromRange(output);

		// 3. Handle multi-restriction equivalence
		replaceWithEquivalent(output, multiRestriction);

		// 4. Add referenced constants
		addConstants(output);

		// 5. Replace classes/properties/values by their equivalents in the target ontology
		replaceWithEquivalent(output, classes, properties, values);

		// 6. Infer RDF type by the domain/range of some property
		inferTypeFromDomainOrRange(output, rules);
		inferTypeFromDomainOrRange(output, outputOntology);

		// 7. Replace with super-classes, and super-properties
		// Description of what is going on.
		// s:P owl:euivalentProperty t:Q --> 
		//    m:X s:P m:o --> m:X t:Q m:o
		// If nothing matches, then, there may be INVERSE:
		// s:P owl.inverseOf x:R, and x:R owl:equivalentProperty t:U, x being any: s, t, m.
		// Than, the actual transformation will be:
		//    m:X s:P m:o --> m:X t:U m:o

		replaceWithEquivalent(output, superClasses, superProperties, null);

		// 8. Add referenced constants
		setDefaults(output);

		// 9. Diagnose
		diagnose(output);

		// 10. Clean
		clean(output);

		return output;
	}

	private void inferSubPropertyFromRange(Model output) {
		Model tmp = ModelFactory.createDefaultModel();
		//Model dump = ModelFactory.createDefaultModel();

		/* on transforming from model m in ontology s: to m in ontology t:
		 * If t:C is a sub-property* of s:D, on range t:B
		 * and s:A is a sub-class* of t:B,
		 *
		 * Then, m:S s:D m:X and m:X type s:A --> m:S t:C m:X
		 */

		StmtIterator sub = rules.listStatements(null, SUB_PROPERTY_OF, (RDFNode)null);
		while(sub.hasNext()) {
			Statement rule = sub.next();
			Resource c = rule.getSubject();
			if (c.isURIResource()) {
				Property C = rules.createProperty(c.getURI());
				RDFNode d = rule.getObject();
				if (d.isURIResource()) {
					Property D = rules.createProperty(d.asResource().getURI());
					if (!D.getURI().equals(OUTPUT_BASE)) {
						NodeIterator range = rules.listObjectsOfProperty(C, RANGE);
						if (range.hasNext()) {
							RDFNode b = range.next();
							if (b.isURIResource()) {
								Resource B = b.asResource();
								StmtIterator it = output.listStatements();
								while(it.hasNext()) {
									Statement stmt = it.next();
									RDFNode x = stmt.getObject();
									if (x.isURIResource()) {
										Resource X = x.asResource();
										Resource A = getType(X, output);
										Property P = stmt.getPredicate();
										if (superClasses.isValidReplacement(A, B) && D.equals(P)) {
											it.remove();
											tmp.add(stmt.getSubject(), C, X);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		output.add(tmp);
	}

	private void addConstants(Model output) {
		List<Resource> tmp = new ArrayList<Resource>();

		ResIterator it = rules.listSubjectsWithProperty(CONSTANT);
		while(it.hasNext()) {
			Resource c = it.next();
			StmtIterator s = output.listStatements();
			while (s.hasNext()) {
				Statement stmt = s.next();
				if (c.equals(stmt.getObject()));
				tmp.add(c);
			}
		}

		for (Resource c : tmp)
			addConstant(output, c);
	}

	private void addConstant(Model model, Resource subj) {
		if (model.listStatements(subj, null, (RDFNode)null).hasNext())
			return;

		StmtIterator it = rules.listStatements(subj, null, (RDFNode)null);
		while (it.hasNext()) {
			Statement stmt = it.next();
			if (stmt.getPredicate().getURI().startsWith(SM))
				continue;
			model.add(stmt);
			RDFNode obj = stmt.getObject();
			if (obj.isURIResource() && getNodeObject(obj.asResource(), CONSTANT, rules) != null)
				addConstant(model, obj.asResource());
		}

		Resource containerType = getUriResourceObject(subj, CONTAINER, rules);
		Property predicate = getPropertyObject(subj, PREDICATE, rules);

		Model tmp = ModelFactory.createDefaultModel();
		if (containerType != null && predicate != null) {
			it = model.listStatements(null, TYPE, containerType);
			if (it.hasNext())
				tmp.add(it.next().getSubject(), predicate, subj);
		}

		model.add(tmp);
	}

	private void setDefaults(Model output) {
		Model tmp = ModelFactory.createDefaultModel();

		ResIterator def = rules.listSubjectsWithProperty(DEFAULT);
		while(def.hasNext()) {
			Resource d = def.next();
			RDFNode domain = getNodeObject(d, DOMAIN, rules);
			if (domain == null || !OUTPUT_BASE.equals(domain.toString()))
				continue;
			Property property = getPropertyObject(d, ON_PROPERTY, rules);
			if (property == null)
				continue;
			RDFNode value = getNodeObject(d, HAS_VALUE, rules);

			ResIterator res = output.listSubjects();
			while (res.hasNext()) {
				Resource subj = res.next();
				StmtIterator it = output.listStatements(subj, property, (RDFNode)null);
				if (!it.hasNext())
					tmp.add(subj, property, value);
			}
		}

		output.add(tmp);
	}

	private void replaceWithEquivalent(Model output, MultiRestrictionEquivalence multiRestriction) {
		Model tmp = ModelFactory.createDefaultModel();
		Model dump = ModelFactory.createDefaultModel();

		ResIterator it = output.listSubjects();
		while (it.hasNext()) {
			Resource subj = it.next();
			if (subj.toString().endsWith("0157"))
				System.err.println("replaceWithEquivalent:: got on 0157");

			List<Statement> q = multiRestriction.get(output, subj, INPUT_BASE, OUTPUT_BASE);
			if (q != null) {
				StmtIterator it2 = output.listStatements(subj, null, (RDFNode)null);
				while (it2.hasNext())
					dump.add(it2.next());
				tmp.add(q);
			}
		}

		output.remove(dump).add(tmp);
	}

	/**
	 * Description of what is going on.
	 * s:P owl:euivalentProperty t:Q --> 
	 *    m:X s:P m:o --> m:X t:Q m:o
	 * If nothing matches, then, there may be INVERSE:
	 * s:P owl.inverseOf x:R, and x:R owl:equivalentProperty t:U, x being any: s, t, m.
	 * Than, the actual transformation will be:
	 *    m:X s:P m:o --> m:X t:U m:o. Uri: Should be m:o t:U m:X ?
	 * -------------------------------------------------------   
	 * @param output
	 * @param classes
	 * @param properties
	 * @param values
	 */
	private void replaceWithEquivalent(Model output, SimpleEquivalence classes, SimpleEquivalence properties, SimpleEquivalence values) {
		Model tmp = ModelFactory.createDefaultModel();
		StmtIterator it = output.listStatements();
		Resource inverseProperty = null;

		while (it.hasNext()) {
			Statement stmt = it.next();
			if (stmt.getSubject().toString().endsWith("0157"))
				System.err.println("replaceWithEquivalent:: got on 0157 for [" + stmt.getPredicate().toString() + "]");
			
			MyStatement newStmt = new MyStatement(stmt);

			// Replace object class
			if (classes != null && stmt.getObject().isResource()) {
				Resource oldClass = stmt.getObject().asResource();
				Resource newClass = classes.get(oldClass, OUTPUT_EXTENDED_BASE);
				if (newClass != null)
					newStmt.setObject(newClass);
			}

			// Replace predicate property
			if (properties != null) {
				Property oldProp = stmt.getPredicate();
				Resource newProp = properties.get(oldProp, OUTPUT_EXTENDED_BASE);
				if (newProp != null)
					newStmt.setPredicate(output.createProperty(newProp.getURI()));
				else {
					// If property has not been replaced, try finding an equivalent of an inverse
					List<Resource> inv = getOthers(output, oldProp, INVERSE_OF);
					for (Resource i : inv)
						if ((inverseProperty = properties.get(i, OUTPUT_EXTENDED_BASE)) != null)
							break;
				}
			}

			// Replace value
			if (values != null && stmt.getObject().isResource()) {
				Resource oldValue = stmt.getObject().asResource();
				Resource newValue = values.get(oldValue, OUTPUT_EXTENDED_BASE);
				if (newValue != null)
					newStmt.setObject(newValue);
			}

			if (inverseProperty != null) {
				Resource s = newStmt.getSubject();
				RDFNode o = newStmt.getObject();
				if (o.isResource()) {
					newStmt.setSubject(o.asResource());
					newStmt.setPredicate(output.createProperty(inverseProperty.getURI()));
					newStmt.setObject(s);
				}
			}

			if (newStmt.isDirty()) {
				it.remove();
				tmp.add(newStmt.getSubject(), newStmt.getPredicate(), newStmt.getObject());
			} else
				p("NOT FOUND: " + stmt + " --> " + OUTPUT_BASE);
		}

		output.add(tmp);
	}

	private void inferTypeFromDomainOrRange(Model output, Model ontology) {
		Model tmp = ModelFactory.createDefaultModel();
		Model dump = ModelFactory.createDefaultModel();

		ResIterator res = output.listSubjects();
		out: while (res.hasNext()) {
			Resource subj = res.next();
			Statement typeStmt = getTypeStmt(subj, output);
			if (typeStmt == null || typeStmt.getObject() == null || !typeStmt.getObject().asResource().getURI().startsWith(OUTPUT_BASE)) {
				StmtIterator triples = output.listStatements(subj, null, (RDFNode)null);
				while (triples.hasNext()) {
					Statement stmt = triples.next();
					for (Resource prop : properties.getAll(stmt.getPredicate())) {
						if (setInferredType(ontology, DOMAIN, prop, tmp, dump, typeStmt, subj))
							continue out;
					}
				}

				triples = output.listStatements(null, null, subj);
				while (triples.hasNext()) {
					Statement stmt = triples.next();
					for (Resource prop : properties.getAll(stmt.getPredicate())) {
						if (setInferredType(ontology, RANGE, prop, tmp, dump, typeStmt, subj))
							continue out;
					}
				}
			}
		}

		output.remove(dump).add(tmp);
	}

	private boolean setInferredType(Model ontology, Property p, Resource prop, Model tmp, Model dump, Statement typeStmt, Resource subj) {
		Resource type = getUriResourceObject(prop, p, ontology);
		type = classes.get(type, OUTPUT_EXTENDED_BASE, true);
		if (type != null) {
			tmp.add(subj, TYPE, type);
			if (null != typeStmt) { //TODO Ariel to explain. Uri added condition since typeStmt may be null
				dump.add(typeStmt);
				if (subj.toString().endsWith("0157"))
					System.err.println("setInferredType:: got on 0157");

				p("INFERRED type of " + subj + ": " + typeStmt.getObject() + " --> " + type + " by " + p);
			}
			return true;
		} else
			return false;
	}

	private void expandByDomainClassRange(Model output) {
		Property domain = rules.createProperty(SM + "domainProperty");
		Property clazz = rules.createProperty(SM + "propertyClass");
		Property range = rules.createProperty(SM + "rangeProperty");

		/* Given A p B, if p has domain, clazz, and range, then:
		 *
		 * 1. A domain(p) X
		 * 2. X rdf:type clazz(p)
		 * 3. X range(p) B
		 *
		 * [X is a new resource]
		 */

		Model tmp = ModelFactory.createDefaultModel();

		StmtIterator it = output.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();
			if (stmt.getSubject().toString().endsWith("0157"))
				System.err.println("expandByDomainClassRange:: got on 0157 for [" + stmt.getPredicate().toString() + "]");
			Property p = stmt.getPredicate();
			Resource dp = getUriResourceObject(p, domain, rules), cp = getUriResourceObject(p, clazz, rules), rp = getUriResourceObject(p, range, rules);
			if (p.getURI().startsWith(INPUT_BASE) && dp != null && cp != null && rp != null) {
				Resource A = stmt.getSubject(), X = output.createResource(dummyUri());
				RDFNode B = stmt.getObject();
				tmp.add(A, output.createProperty(dp.getURI()), X);
				tmp.add(X, TYPE, cp);
				tmp.add(X, output.createProperty(rp.getURI()), B);
				it.remove();
				p("EXPANDED " + stmt + " INTO 3 TRIPLES!");
			}
		}

		output.add(tmp);
	}

	private void contractByDomainClassRange(Model output) {
		Property domain = rules.createProperty(SM + "domainProperty");
		Property clazz = rules.createProperty(SM + "propertyClass");
		Property range = rules.createProperty(SM + "rangeProperty");

		/* Given a property p, that has domain, clazz, and range, if
		 *
		 * 1. A domain(p) X
		 * 2. X rdf:type clazz(p)
		 * 3. X range(p) B
		 *
		 * for some resources A, B, X;
		 * then A p B
		 */

		Model tmp = ModelFactory.createDefaultModel(), dump = ModelFactory.createDefaultModel();
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, List<Resource>> domains = new StronglyTypedMap(String.class, List.class);
		Map<String, Resource []> properties = new StronglyTypedMap<String, Resource []>(String.class, Resource [].class);

		ResIterator res = rules.listSubjects();
		while (res.hasNext()) {
			Resource p = res.next();
			if (p.toString().endsWith("0157"))
				System.err.println("contractByDomainClassRange:: got on 0157");
			Resource dp = getUriResourceObject(p, domain, rules), cp = getUriResourceObject(p, clazz, rules), rp = getUriResourceObject(p, range, rules);
			if (p.getURI() != null && p.getURI().startsWith(OUTPUT_BASE) && dp != null && cp != null && rp != null) {
				if (!domains.containsKey(dp.getURI()))
					domains.put(dp.getURI(), new ArrayList<Resource>());
				domains.get(dp.getURI()).add(p);
				properties.put(p.getURI(), new Resource [] { dp, cp, rp });
			}
		}

		StmtIterator it = output.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();
			Property d = stmt.getPredicate();
			if (domains.containsKey(d.getURI())) {
				Resource A = stmt.getSubject(), X = stmt.getObject().asResource();
				for (Resource q : domains.get(d.getURI())) {
					Property p = output.createProperty(q.getURI());
					Resource [] dcr = properties.get(p.getURI());
					Resource c = getUriResourceObject(X, TYPE, output);
					if (c != null && c.getURI() != null && c.getURI().equals(dcr[1].getURI())) {
						Property r = output.createProperty(dcr[2].getURI());
						Resource B = getUriResourceObject(X, r, output);
						if (B != null) {
							dump.add(A, d, X);
							dump.add(X, TYPE, output.createProperty(c.getURI()));
							dump.add(X, r, B);
							Statement newStmt = tmp.createStatement(A, p, B);
							tmp.add(newStmt);
							p("CONTRACTED 3 TRIPLES INTO ONE STATEMENT: " + newStmt);
						}
					}
				}
			}
		}

		output.add(tmp);
		output.remove(dump);
	}

	private void diagnose(Model output) {
		TreeSet<String> predicates = new TreeSet<String>(), objects = new TreeSet<String>();

		StmtIterator it = output.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();
			Property p = stmt.getPredicate();
			if (!p.getURI().startsWith(OUTPUT_BASE))
				predicates.add(p.getURI());
			RDFNode o = stmt.getObject();
			if (o.isResource() && !o.toString().startsWith(OUTPUT_BASE) && !isDummy(o.toString()))
				objects.add(o.toString());
		}

		p("");
		p("Unhandled Predicates:");
		p("---------------------");
		p("");

		for (String p : predicates)
			p(p);

		p("");
		p("Unhandled Objects:");
		p("------------------");
		p("");

		for (String o : objects)
			p(o);
	}

	private void clean(Model output) {
		StmtIterator it = output.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();
			Property p = stmt.getPredicate();
			RDFNode o = stmt.getObject();
			if (p.getURI().startsWith(INPUT_BASE))
				it.remove();
			// Problem! This one causes trouble when the input-ontology base starts with the input-model base!
			else if (o.toString().startsWith(INPUT_BASE))
				it.remove();
		}
	}

	private String baseOutputName() {
		String name = OUTPUT_BASE.substring(0, OUTPUT_BASE.length() - 1);
		name = name.substring(name.lastIndexOf('/') + 1);
		return name;
	}

	private static final String DUMMY = "https://dummy.uri.org/";

	private String dummyUri() {
		return DUMMY + UUID.randomUUID().toString();
	}

	private boolean isDummy(String uri) {
		return uri != null && uri.startsWith(DUMMY);
	}
}
