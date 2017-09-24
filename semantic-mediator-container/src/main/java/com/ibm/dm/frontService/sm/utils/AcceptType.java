
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
 
 
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

package com.ibm.dm.frontService.sm.utils;

public class AcceptType implements Comparable<AcceptType> {
	public String type;
	public int weight;
	public AcceptType(String typeExp) {
		int pos = typeExp.indexOf(';'); 
		if( pos > 0 ) {
			this.type = typeExp.substring(0,pos).trim();
			String qualifier = typeExp.substring(pos+1);
			if( qualifier.startsWith("q=") ) {
				try{
					float w = Float.parseFloat(qualifier.substring(2)) * 1000;
					this.weight = (int) w;
				}catch( NumberFormatException e ) {
					this.weight = 1000;
				}
			} else {
				this.weight = 1000;
			}
			
		} else {
			this.type = typeExp;
			this.weight = new Integer(1000);
		}
	}

	public int compareTo(AcceptType other) {
		return ((AcceptType)other).weight - weight;
	}
	
	@Override
	public boolean equals(Object obj) {
		if( obj instanceof AcceptType ) {
			AcceptType otherAcceptType = (AcceptType) obj;
			// now we need to sort out wildcards
			String[] otc = otherAcceptType.type.split("/");
			String[] ttc = this.type.split("/");
			boolean seg1 = compareSegment(otc[0], ttc[0]);
			boolean seg2 = compareSegment(otc[1], ttc[1]);
			return seg1 && seg2;
		} else if( obj instanceof String ) {
			String[] otc = ((String)obj).split("/");
			String[] ttc = this.type.split("/");
			boolean seg1 = compareSegment(otc[0].trim(), ttc[0].trim());
			boolean seg2 = compareSegment(otc[1].trim(), ttc[1].trim());
			return seg1 && seg2;
		}
		return false;
	}
	
	public boolean compareSegment(String s1, String s2 ) {
		if( s1.equals("*") || s2.equals("*") ) {
			return true; 
		} 
		return s1.equals(s2);
	}
}


