
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
package com.ibm.haifa.sm.mediator.utils;

import java.util.*;

public class StronglyTypedSet<T> extends HashSet<T> {
	private static final long serialVersionUID = 1L;
	private final Class<T> type;
	public StronglyTypedSet(Class<T> type) {
		super();
		this.type = type;
	}
	public boolean contains(Object o) {
		 return super.contains(type.cast(o));
	}
	public boolean remove(Object o) {
		 return super.remove(type.cast(o));
	}
}
