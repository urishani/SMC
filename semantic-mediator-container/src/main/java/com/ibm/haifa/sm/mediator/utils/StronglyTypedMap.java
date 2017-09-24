
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

import java.util.HashMap;

public class StronglyTypedMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 1L;
	private final Class<K> keyType;
	private final Class<V> valueType;
	public StronglyTypedMap(Class<K> keyType, Class<V> valueType) {
		super();
		this.keyType = keyType;
		this.valueType = valueType;
	}
	public boolean containsKey(Object o) {
		 return super.containsKey(keyType.cast(o));
	}
	public boolean containsValue(Object o) {
		 return super.containsKey(valueType.cast(o));
	}
	public V get(Object o) {
		return super.get(keyType.cast(o));
	}
	public V remove(Object o) {
		return super.remove(keyType.cast(o));
	}
}
