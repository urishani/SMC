
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

package com.ibm.dm.frontService.sm.service;

public interface ISmInfo {

	static String VERSION = "[version of: Sept 4 2017, v3.00]";
	static String NOTES =
		"   <li>Sept 4 2017, v3.00 - Swagger API mechanism, exposing mediation API services and enabling cli invocation of service, with default example database to start with.\n" +
		"   <li>May 17 2016, v2.03 - Same autmoation is also applicable to Rulesets. including API service for the rules ontologies.\n" +
		"   <li>May 16 2016, v2.03 - API service for ontologies established, including show document using parot online services.\n" +
		"   <li>May 16 2016, v2.03 - Added automatic generation of original ontologies. Still missing API service.\n" +
		"   <li>May 09 2016, v2.03 - Ready for Docker dpeloyment, multi projects at a session, and bug fixes. Better UX and UI.\n" +
		"   <li>Aug 24 2015, v2.02 - Tools are now distinguished items.Several usability and automation improvements.\n" +
		"   <li>Aug 18 2015, v2.01 - Operationa graph shows tools, and additional info.\n" +
		"   <li>July 29 2015, v2.01 - Add 'Tools' - a repository that can be edited, and posted internally.\n" +
		"   <li>July 19 2015, v2.00 - OSLC core-compliant ontology creation.\n" +
		"   <li>May 20 2015, v2.00 - WAR package for Tomcat and Bluemix version. With Partial Cloudant support.\n" +
		"   <li>Mar 08 2015, v1.29 - Fixed: Now reloading ontologies when changed.\n" +
		"   <li>Feb 17 2015, v1.29 - Fixing a bug with queries where rdf: is missing NS prefix.\n" +
		"   <li>Feb 11 2015, v1.29 - Fixing again the bug reported from SODIUS.\n" +
		"   <li>Feb 08 2015, v1.29 - Allow to archive bad row items.\n" +
		"   <li>Jan 26 2015, v1.29 - Added mechanism to support mediators with initialize parameter.\n" +
		"   <li>Jan 14 2015, v1.28 - fixed bug reported from SODIUS.\n" +
		"   <li>Dec 15 2014, v1.28- MDW mediator now works.\n" +
		"   <li>Nov 17 2014, v1.27- More info when an interceptor is found invalid.\n" +
		"   <li><form action='/dm/server/management/notesHistory'><button type='submit'>REST OF ALL NOTICES</button></form>";
	static String NOTES_HISTORY = "<html><title>SMC Notes history</title><body><ul>" + 
		"   <li>Oct 19 2014, v1.27- Showing port type icons, and options to build any port type from ontology.\n" +
		"   <li>Oct 7 2014, v1.27- Dynamic resource diagrams browsing.\n" +
		"   <li>Oct 2 2014, v1.27- Active resource diagrams for browsing at a click.\n" +
		"	<li>Sept 30 2014, v1.26- Cannot create port, nor mediators, except via ontologies and rules-sets.\n" +
		"   <li>Sept 17 2014, v1.26- Some complementing info in the SM base ontology.\n" +
		"   <li>Sept 10 2014, v1.26- Added show origins of ontologies and rules.\n" +
		"   <li>Sep 08 2014, v1.26- Resolved some UI bugs.\n" +
		"   <li>Aug 17 2014, v1.26- Resolved bug 8136:  Ontologies and Ruleset update problem.\n" +
		"   <li>Aug 13 2014, v1.26- Resolved bug 8120:  Ruleset initiation problem.\n" +
		"   <li>Aug 13 2014, v1.26- Resolved bug 8120:  Ruleset initiation problem.\n" +
		"   <li>Aug 12 2014, v1.26- Bugs resolved: graphs, proper use of ontology version IRI, and icons fixing.\n" +
		"   <li>Aug  7 2014, v1.25- Problems with Friends imports and multi workspaces solved.\n" +
		"   <li>Aug  7 2014, v1.25- Problems with Friends imports and multi workspaces solved.\n" +
		"   <li>Jul 17 2014, v1.25- Graphics browsing and repository editing.\n" +
		"   <li>Jul 13 2014, v1.24- SPARQL UI improved\n" +
		"   <li>Jul  2 2014, v1.24- Graphing of SPARQL queries\n" +
		"   <li>Jun 30 2014, v1.23- More SPARQL queries management\n" +
		"   <li>Jun 23 2014, v1.23- SPARQLs expanded to full syntax\n" +
		"   <li>Jun 16 2014, v1.23- SPARQLs can be saved with names and reused\n" +
		"   <li>Jun 12 2014, v1.22- Import from origin, and not the intermediary friends.\n" +
		"   <li>Jun 06 2014, v1.21- Protect imported ontologies from modifications, load and protege editing.\n" +
		"   <li>Jun 02 2014, v1.21- Extended distribution: can share ontologies and rules among SMC servers.\n" +
		"   <li>May 22 2014, v1.20- Extended automation to ontologies and rules, incl testing of direct repositories.\n" +
		"   <li>May 18 2014, v1.19- cleaned GUI problems\n" +
		"   <li>Apr 27 2014, v1.19- Improved sparql option 'Used By' + 'look back' in the resource RDF browsing.\n" +
		"   <li>Apr 23 2014, v1.19- <br>Added command <img width='20' height='20' src='/dm/resources/graphics/buttons/build_obj32.png'> for ontologies automation.\n" +
		"   <li>Feb 23 2014, v1.18- Mediation view shows associated resources inline.\n" +
		"   <li>Feb 18 2014, v1.18- Ontologies governance: distincting import from upload.\n" +
		"   <li>Feb 13 2014, v1.18- Export ports inform better on their validation; Large models now work in pages for the table view option.\n" +
		"   <li>Jan 26 2014, v1.18- Ontologies/repositories graphs show blank nodes properly now + configuring rul-sets view + unified style of tables.\n" +
		"   <li>Jan 23 2014, v1.18- L&F with tabs + Uploading of ontologies now support multiple formats + Ontology compare fixed a bug.\n" +
		"   <li>Jan 21 2014, v1.17- Improved L&F + fixed bug in ontology creation/update/edit.\n" +
		"   <li>Jan 19 2014, v1.17- Improved split screen with sidebar with animation.\n" +
		"   <li>Jan 16 2014, v1.17- Split screen with sidebar - onward to modern style.\n" +
		"   <li>Jan 7 2014,  v1.16 - Clean start after new installation + buttons + adoption for projects in progress." +	"   <li>Dec 20 2013, v1.15 - Graph view of repositories, using graphviz.\n" +
		"   <li>Dec 05 2013, v1.14 - DM compliant version - can work with dm/web projects + Fixed bug in configuration migration.\n" +
		"   <li>Dec 02 2013, v1.13 - Repository comparison improved. Byte coding of textual fields is now fully respected.\n" +
		"   <li>Nov 17 2013, v1.13 - OSLC AM Service Provider comes to life. TBD. Follow this new Icon:<img width='25' height='25' src='http://open-services.net/css/images/logo-forflip.png'>.\n" +
		"   <li>Nov 15, 13: v1.12 - New Graph visualization of ontologies and operations w/Graphviz.\n" +
		"   <li>Nov  4, 13: v1.12 - RDF format is user-selectible for view, browsing and modifications.\n" +
		"   <li>Oct 27, 13: v1.11 - Bug in repositories editing fixed.\n" +
		"   <li>Oct 17, 13: v1.11 - Bug in repositories fixed.\n" +
		"   <li>Oct 14, 13: v1.11 - New feature: Can now modify individual resources manually. Using OSLC PUT over the web.\n" +
		"   <li>Oct 10, 13: 2013, v1.10 - Direct post from tools to repositories enabled.\n" +
		"   <li>Oct  8, 13: v1.9 - Bug fix in oslc GET.\n" +
		"   <li>Oct  3, 13: v1.9 - New version. More informative logging, configurable text search and display format, repository resiliency improved.\n" +
		"   <li>Sep 24, 13: v1.8 - New version. Text search queries added for Model repositories.\n" +
		"	<li>Sep 23, 13: More bugs in configuration UI fixed.\n" +
		"	<li>Sep 16, 13: Some bug fixes and performance improvements.\n" +
		"	<li>Aug 21, 13: New revision 1.7: Supports logging. Traces presently jobs level. New button to view.\n" +
		"	<li>Aug  8, 13: New revision 1.6: Distributed SMC introducing Friends. CSS styles used for L&F.\n" +
		"	<li>Jul 25, 13: Initial support for collaboration among SM Container servers.\n" +
		"	<li>Jul 11, 13: Customization possible for list mode browsing of repositories.\n" +
		"	<li>Jun 23, 13: Added Jazz Look and feel style, only for RTC widget services.\n" +
		"	<li>May 28, 13: Added license \"click through\" service for mediators.\n" +
		"	<li>May 15, 13: Fix: problems in modified date of ontologies and rules.\n" +
		"	<li>May  7, 13: Integrated online Protege ontology editing.\n" +
		"	<li>Apr 14, 13: Repository bug fixed.\n" +
		"	<li>Apr  9, 13: Improved repository model RDF browsing.\n" +
		"	<li>Mar 27, 13: Some bug fixes.\n" +
		"	<li>Mar 20, 13: <i>Clear all</i> filters button + some internal refactoring.\n" +
		"	<li>Mar 18, 13: Fixed bug in repository comparisons.\n" +
		"	<li>Mar 13, 13: Downloads available for ontologies and repositories RDF contents.\n" +
		"	<li>Mar 11, 13: Blob support with permanent attachment repository using a permanent built-in SM ontology.\n" +
		"	<li>Feb 28, 13: Item editing filters candidates according to roles and tags.\n" +
		"	<li>Feb 19, 13: Mediator extension points problem fixed. Comparisons of repositories improved with hot-points.\n" +
		"	<li>Feb 13, 13: Mediators config now specify interceptors from a list of names in the plugin extension point SmMediator.\n" +
		//"      The names are listed in a new plugin extention.\n" +
		"	<li>Feb  3, 13: Added Widget oriented view of repositories (action=ShowShort)\n" +
		"	<li>Jan 21, 13: Mediators now keep links and can show them.\n" +
		"	<li>Jan  8, 13: Item tagging working to filter display.\n" +
		"	<li>Jan  8, 13: This limitation obsolete: <font style='text-decoration:line-through'>Container flows should be limited to single stages: Tools &lrarr; Repositories.</font>\n" +
		"   <li>Jan  1, 13: Batch (no-wait) mediation; Buttons reorganized; Graphics are persisted.\n" +
		"   <li>Nov 21, 12: Full recursive genuine status updates.\n" +
		"   <li>Nov 20, 12: Show port access names in refs. Indicates if missing ports/ontology/rules in references.\n" +
		"   <li>Oct 30, 12: Old separate repositories view removed. Use repository Ports for that.\n" +
		"   <li>Oct 28, 12: Supports <i>linked data</i> in model exploration.\n" +
		"   <li>Oct 21, 12: Improved support for SPARQL and OSLC in repositories display.\n" +
		"   <li>Oct 15, 12: Better information in errors reporting.\n" +
		"   <li>Oct  6, 12: Repositories resources URL now use / instead of #.\n" +
		"   <li>Oct  6, 12: Collapse/Expand works!, Graphics buttons work too! Using meaningful names." +
	    "</ul></body></html>";
}

