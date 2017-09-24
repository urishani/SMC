
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

/**
 * 
 */
package com.ibm.dm.frontService.b.data.interfaces;

/**
 * This is tagging interface references as a kind of hack in the DAOService that takes case of serialization of Java Objects to the JFS
 * all instances of classes that implement this interface will post processed after jenaBean powered conversion to Jena Model by addition of
 * <p>
 * &lt;rdf:type rdf:resource="http://www.ibm.com/sow#Compartment"/>
 * <p>
 * &lt;rdf:type rdf:resource="http://www.ibm.com/1.0/dm/core#Document"/>
 * <p>
 * to their RDF XML representation before stored to the JFS
 * 
 * @author borisd
 */
public interface IDmDtkIndexed
{

}

