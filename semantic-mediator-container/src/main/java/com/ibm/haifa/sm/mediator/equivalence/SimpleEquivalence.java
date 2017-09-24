
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.*;
import com.ibm.haifa.mediator.base.Rules;
import com.ibm.haifa.sm.mediator.utils.StronglyTypedMap;
import com.ibm.haifa.sm.mediator.utils.StronglyTypedSet;

public class SimpleEquivalence extends Rules {

	public static boolean DEBUG = true;

	private final Resource [] resources;
	private final Map<Resource, Integer> map = new StronglyTypedMap<Resource, Integer>(Resource.class, Integer.class);
	private final int [][] adj;
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final Map<String, Map<Resource, Resource>> replace = new StronglyTypedMap(String.class, Map.class);
	private boolean resolved;

	public SimpleEquivalence(Model model, Property [] twoWay, Property [] oneWay) {
		super(model);

		/*
		 * 1. Make a set, then sorted array, of involved resources
		 * 2. Make an index-lookup map
		 * 3. Fill-in the adjacency matrix, and apply floyd-warshall
		 */

		Set<Resource> set = new StronglyTypedSet<Resource>(Resource.class);
		StmtIterator it = model.listStatements();
		while(it.hasNext()) {
			Statement stmt = it.next();
			set.add(stmt.getSubject());
			if (stmt.getObject().isResource())
				set.add(stmt.getObject().asResource());
		}
		resources = set.toArray(new Resource[0]);
		Arrays.sort(resources, new Comparator<Resource>() {
			public int compare(Resource r, Resource s) {
				return r.toString().compareTo(s.toString());
			}
		});

		int N = resources.length;
		for (int i = 0; i < N; ++i)
			map.put(resources[i], i);

		adj = new int [N][N];
		for (int i = 0; i < N; ++i)
			adj[i][i] = 1;

		for (Property p : twoWay) {
			it = model.listStatements(null, p, (RDFNode)null);
			while(it.hasNext()) {
				Statement stmt = it.next();
				int i = map.get(stmt.getSubject()), j = map.get(stmt.getObject().asResource());
				adj[i][j] = adj[j][i] = 1;
			}
		}

		for (Property p : oneWay) {
			it = model.listStatements(null, p, (RDFNode)null);
			while(it.hasNext()) {
				Statement stmt = it.next();
				int i = map.get(stmt.getSubject()), j = map.get(stmt.getObject().asResource());
				adj[i][j] = 1;
			}
		}
	}

	private void resolve() {
		int N = resources.length;
		if (!resolved) {
			for (int i = 0; i < N; ++i)
				for (int j = 0; j < N; ++j)
					for (int k = 0; k < N; ++k)
						adj[i][j] = Math.max(adj[i][j], adj[i][k] * adj[k][j]);
			resolved = true;
//			if (DEBUG) {
//				int i=0; p("Resources:"); for (Resource r: resources) {p(i + ":" + r); i++;}
//				int j; System.out.print("\n    "); for (j=0; j < N; j++) {System.out.format("%2d, ", j);}
//				i=0; p("Adj:"); for (int r[]: adj) { System.out.format("%n%2d: ", i); i++; for (int x: r) {System.out.format("%2d, ",x);} }
//			}
		}
	}

	private void resolve(String target) {
		resolve();
		int N = resources.length;
		if (target != null && !replace.containsKey(target)) {
			Map<Resource, Resource> map = new StronglyTypedMap<Resource, Resource>(Resource.class, Resource.class);
			for (int i = 0; i < N; ++i)
				for (int j = 0; j < N; ++j)
					if (adj[i][j] == 1)
						if (resources[j].toString().startsWith(target)) {
							map.put(resources[i], resources[j]);
							break;
						}
			replace.put(target, map);
		}
	}

	public Resource get(Resource res, String [] targets) {
		return get(res, targets, false);
	}

	public Resource get(Resource res, String [] targets, boolean strict) {
		for (String t : targets) {
			Resource r = get(res, t, strict);
			if (r != null)
				return r;
		}
		return null;
	}

	private Resource get(Resource res, String target, boolean strict) {
	if (res == null)
		return null;

	if (res.getURI().startsWith(target))
		return res;

	if (!strict)
		for (String p : PUBLIC)
			if (res.toString().startsWith(p))
				return res;

	if (!map.containsKey(res))
		return null;

	resolve(target);
	Resource rep = replace.get(target).get(res);
	if (rep != null) {
		p("FOUND! " + res + " --> " + rep);
		return rep;
	}

	//p("NOT FOUND: " + res + " --> " + target);
	return null;
	}
	
	public Map<Resource,Resource> getReplace(String target) {
		return replace.get(target);
	}

	public Resource [] getAll(Resource res) {
		resolve();
		List<Resource> R = new ArrayList<Resource>();
		if (map.containsKey(res)) {
			int i = map.get(res), N = resources.length;
			for (int j = 0; j < N; ++j)
				if (adj[i][j] == 1)
					R.add(resources[j]);
		}
		return R.toArray(new Resource[0]);
	}

	public boolean isValidReplacement(Resource A, Resource B) {
		Integer i = map.get(A), j = map.get(B);
		if (i != null && j != null)
			return adj[i][j] == 1;
		else
			return false;
	}
}
