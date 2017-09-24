
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

import org.apache.jena.rdf.model.*;

public class MyStatement {
	private Resource subject;
	private Property predicate;
	private RDFNode object;
	private boolean dirty;

	public MyStatement(Statement stmt) {
		subject = stmt.getSubject();
		predicate = stmt.getPredicate();
		object = stmt.getObject();
	}

	public Resource getSubject() {
		return subject;
	}

	public void setSubject(Resource subject) {
		this.subject = subject; dirty = true;
	}

	public Property getPredicate() {
		return predicate;
	}

	public void setPredicate(Property predicate) {
		this.predicate = predicate; dirty = true;
	}

	public RDFNode getObject() {
		return object;
	}

	public void setObject(RDFNode object) {
		this.object = object; dirty = true;
	}

	public boolean isDirty() {
		return dirty;
	}
}
