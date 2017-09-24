
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
/**
 * Used to maintain ontological repositories.
 * 
 * @author shani
 *
 */
package com.ibm.dm.frontService.sm.service;

import com.ibm.dm.frontService.sm.data.AModelRow;
import com.ibm.dm.frontService.sm.utils.IConstants;

public class RulesRepository extends ModelRepository {
	protected RulesRepository(AModelRow item) {
		super(item);
	}
	
	static {
		preferredPrefixes.put(IConstants.SM_PROPERTY_NS_PREFIX, IConstants.SM_PROPERTY_NS); // "http://com.ibm.ns/haifa/sm#");
	}

	public static ModelRepository create(AModelRow model) {
		return new RulesRepository(model);
	}

}
