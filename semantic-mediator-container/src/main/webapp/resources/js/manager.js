
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
 
 *| Copyright IBM Corp. 2011, 2013.                                        |
 *| File Name: manager.js                                                  |
 *+------------------------------------------------------------------------+
 */
	//debugger;
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {/* do nothing*/
	}

	var Ont
	var Rul
	var Prt
	var Mdt
	var Frn
	var ontImgVar
	var operImgVar
	var filterVar
	var tagsData
	var project

	var pages;


	function getVars() {
		if (parent.useFrames) {
			Ont = parent.Ont;
			Rul = parent.Rul;
			Prt = parent.Prt
			Mdt = parent.Mdt;
			Frn = parent.Frn;
			project = parent.project;
			ontImgVar = parent.ontImgVar;
			operImgVar = parent.operImgVar;
			filterVar = parent.filterVar;
		}
		pages = {
			"Ont" : Ont,
			"Rul" : Rul,
			"Prt" : Prt,
			"Mdt" : Mdt,
			"Frn" : Frn
		};
	}

	function onload(_Ont_, _Rul_, _Prt_, _Mdt_, _Frn_, _ontImgVar_, _operImgVar_, _filterVar_, _tagsData_, _project_) {
		Ont= _Ont_;
		Rul= _Rul_
		Prt= _Prt_
		Mdt= _Mdt_
		Frn= _Frn_
		ontImgVar= _ontImgVar_
		operImgVar= _operImgVar_
		filterVar= _filterVar_
		tagsData= _tagsData_
		project= _project_; // _metaProject_ this is sprint/danse, not the actual project. obsolete by now.
	
		//alert('load');
		if (parent.useFrames) {
			//alert("frames");
			if (!parent.initialized) {
				//alert("initializing");
				parent.initialized = 1;
				parent.Ont = Ont;
				parent.Rul = Rul;
				parent.Prt = Prt;
				parent.Mdt = Mdt;
				parent.Frn = Frn;
				parent.ontImgVar = ontImgVar;
				parent.operImgVar = operImgVar;
				parent.filterVar = filterVar;
			}
			if (parent.sidebarShrinked)
			   shrink_expand(document.getElementById("shrinkExpandBut"), true);
		}
		getVars();  // as a result - there parent.project is not defined, and so it the local var project.
//		var pid = getProjectId();
		if (project) { // remove extra logo - we work from within the dm web
		   document.getElementById('not-in-dm-Title').style.display="none";
		}
		e = document.getElementById('filter');
		e.value = filterVar;
		filter();
	}


	function shrink_expand(but, immediate) {
		var e=document.getElementById("sidebar");
		var s1 = function(f) {
          		e.style.opacity = f;
       		};
       	var f1 = function(){
	      		e.style.display='none';
          		but.className = 'button_right_arrow';
	      		but.disabled = false;
	      		but.className = 'button_right_arrow';
          		but.title='Show sidebar';
          		parent.sidebarShrinked = 1;
       		};
       	var s2 = function(f) {
          		e.style.opacity = f;
       		};
       	var f2 = function(){
          		but.className = 'button_right_arrow';
	      		but.disabled = false;
	      		but.className = 'button_left_arrow';
          		but.title='Hide sidebar';
          		parent.sidebarShrinked = 0;
	   		}
       	
		
		if (but.title.indexOf('Hide')==0) { // shrink
		    if (immediate) { 
		       f1(); return;
		    }
	   		but.disabled=true;
	   		timelyExcecution(0.9, 0.1, -0.2, 100, s1, f1); 
		} else { // expand
		    if (immediate) { 
		       f2(); return;
		    }
	   		but.disabled=true;
       		e.style.display='block';
	   		timelyExcecution(0.2, 1, 0.1, 100, s2, f2);
		}
	}
			
	function toggle(who, but, noSave, refresh) {
		var coll = (this[who] + 1) % 2;
		this[who] = coll;
		if (!noSave)
			saveVar(who, coll);
		if (refresh)
			filter();
		return false;
	}
	function toggle2(who, but, noSave, refresh) {
		var coll = (this[who] + 1) % 2;
		this[who] = coll;
		if (!noSave)
			saveVar(who, coll);
		if (refresh)
			filter();
		return false;
	}

	function showRows(who, rowIds, coll, kBase) {
		var show = "table-row";
		var k = 1;
		var tab = document.getElementById('mainTable');
		var trs = tab.getElementsByTagName('tr');
		var but = document.getElementById('toggle_' + who);
		if (but) {
			//var value = but.value ? "value" /* IE */: "innerHTML" /* rest of the world */;
			//but[value] = (coll == 1) ? '+' : '-';
			but.style.opacity = (coll == 1) ? '0.3' : '1.0';
			but.title = ((coll == 1) ? 'Expand ' : 'Collapse ') + who + ' section';
		}

		for ( var i = 0, j = trs.length; i < j; i++) {
			var tr = trs[i];
			var trid = tr.id;
			if (trid) {
				if (trid.indexOf(who) != 0)
					continue;
			} else
				continue;
			var toShow = 'none';
			if (!coll) {
				//var keys = Object.keys(rowIds);
				// if (keys.length > 0) {
				if (rowIds['*'] || rowIds[trid])
					toShow = show;
				//} else
				//	toShow = show;
			}
			tr.style.display = toShow;
			if (toShow==='none') 
			   continue;
			//removeClass(tr, 'smRow0');
			//addClass(tr, 'smRow' + (k%2));
			if (k > kBase)
				tr.className = 'smRow' + ((k-kBase+1)%2);
//			if (k%2) 
//				tr.style.background-color='#e0f0e0';
//			else
//				tr.style.background-color='white';
			k++;
		}
		return false;
	}



//	function hideDomains() {
//		var domainsTd = document.getElementById('Domains');
//		domainsTd.style.display = 'none';
//	}
//	function showDomains() {
//		var domainsTd = document.getElementById('Domains');
//		domainsTd.style.display = 'block';
//	}
	function collapseAll() {
		var pageNames = Object.keys(pages);
		for ( var i = 0; i < pageNames.length; i++)
			collapse(pageNames[i]);
		filter();
		return false;
	}

	function expandAll() {
		var pageNames = Object.keys(pages);
		for ( var i = 0; i < pageNames.length; i++)
			collapse(pageNames[i], 0, '+');
		filter();
		return false;
	}

	function collapse(who, noSave, marker) {
		if (arguments.length < 3)
			marker = '-';
		if ((this[who] && marker === '-') || (!this[who] && marker === '+'))
			return;
		//    var but = document.getElementById("toggle_" + who)
		//    var value = but.value ? "value" /* IE */ : "innerHTML" /* rest of the world */;
		//    if (but[value] == marker)
		return toggle(who, null, noSave);
	}

	function showImg(id) {
		var e = document.getElementById(id)
		var v = e.value;
		window.open(v);
		// save in the frames if you use that:
		if (parent.useFrames)
			parent[id] = v;
		saveVar(id + 'Var', v);
		return false;
	}

	function repositorySync() {
		var req = new XMLHttpRequest();
		req.open("GET", "/dm/server/management/sync?t=" + new Date().getTime(),
				true, null, null);
		req.onreadystatechange = function() {
			if (this.readyState == 4) {
				if (this.status == 200) {
					var status = eval('[' + this.responseText + ']')[0];
					for ( var prt in status.repositories) {
						var sync = document.getElementById(prt);
						var val = status.repositories[prt];
						if (sync && val) {
							var value = sync.innerText ? "innerText"
									: "innerHTML";
							sync[value] = val;
						}
					}
					for ( var btt in status.buttons) {
						var button = document.getElementById(btt);
//						if (!button) alert('null button [' + btt + ']');
						if (button) {
							var canClear = status.buttons[btt];
							var title = button.title;
							if (title.indexOf('[disabled] ') == 0) 
			 			    	title = title.substring('[disabled] '.length);
							if (canClear) {
								button.disabled = false;
								button.title = title;
								button.style.backgroundColor = 'yellow';
							} else {
								button.disabled = true;
								button.title='[disabled] '+ title;
								button.style.backgroundColor = '#FFFFC0';
							}
						}
					}
				}
				resetSync();
			}
		};
		req.send("");
	}

	function resetSync() {
		setTimeout(repositorySync, 5000);
	}

	repositorySync();

	// New tags management function, will add it and remove it based on tagSelect.checked value.
	function updateTag(tagSelect) {
		var tag = tagSelect.value;
		var add = tagSelect.checked;
		var e = document.getElementById('filter');
		var f = e.value;
		tags = f.split(',');
		var nf = '';
		var contained = false;
		for ( var i = 0; i < tags.length; i++) {
			var aTag = tags[i];
			if (!aTag)
				continue;
			if (aTag == tag) {
				contained = true;
				if (!add)
					continue; // remove it
			}
			if (nf.length > 0)
				nf += ',';
			nf += aTag;
		}
		if (!contained && add) {
			if (nf.length > 0)
				nf += ',';
			nf += tag;
		}
		e.value = nf;
		saveVar('filterVar', nf);
		updateForCreateItems(nf);
		filter();
		return false;
	}

	// Next one obsolete - to be removed.
	function updateFilter(tag) {
		//   alert('filterChanged');
		var e = document.getElementById('filter');
		var f = e.value;
		tags = f.split(',');
		var nf = '';
		var contained = false;
		for ( var i = 0; i < tags.length; i++) {
			var aTag = tags[i];
			if (!aTag)
				continue;
			if (aTag == tag) {
				contained = true;
				continue; // remove it
			}
			if (nf.length > 0)
				nf += ',';
			nf += aTag;
		}
		if (!contained) {
			if (nf.length > 0)
				nf += ',';
			nf += tag;
		}
		e.value = nf;
		saveVar('filterVar', nf);
		updateForCreateItems(nf);
		filter();
		return false;
	}

	function updateForCreateItems(nf) {
		var pageNames = Object.keys(pages);
		var e = null;
		for ( var i = 0; i < pageNames.length; i++) {
			e = document.getElementById('new' + pageNames[i] + 'Tags');
			e.value = nf;
		}
	}

	function clearAllFilters() {
		var oldf = document.getElementById('filter').value;
		document.getElementById('filter').value = '';
		var olda = document.getElementById('showActive').checked;
		document.getElementById('showActive').checked = false;
		var tagsDiv = document.getElementById('tags');
		var fonts = tagsDiv.children;
		for (i = 0; i < fonts.length; i++) {
			var tag = fonts[i].children[0];
			if (tag && tag.checked)
			   tag.checked = false;
		}
		if (oldf != '')
			saveVar('filterVar', '');
		if (olda != false)
			saveVar("showActive", "");
		if (oldf != '' && olda == false)
			filter();
		if (olda != false)
			refresh();
	}

	function filter() {
		var filterTxt = document.getElementById('filter').value;
		var tags = [];
		if (filterTxt.trim().length > 0)
			tags = filterTxt.split(',');
		var ids = {};
		if (tags.length > 0) {
			for ( var i = 0; i < tags.length; i++) {
				var candidates = tagsData[tags[i]];
				if (candidates) {
					for ( var j = 0; j < candidates.length; j++) {
						var x = candidates[j];
						ids[x] = true;
					}
				}
			}
		   ids['OntT'] = true; // add the section heading row
		   ids['Ont0'] = true; // add the section heading row
		   ids['OntSM'] = true; // for OntSM
		   ids['RulT'] = true; // add the section heading row
		   ids['Rul0'] = true; // add the section heading row
		   ids['Rul1'] = true; // add the second section heading row
		   ids['PrtT'] = true; // add the section heading row
		   ids['Prt0'] = true; // add the section heading row
		   ids['PrtAR'] = true;// for PrtAR.
		   ids['MdtT'] = true; // add the section heading row
		   ids['Mdt0'] = true; // add the section heading row
		   ids['FrnT'] = true; // add the section heading row
		   ids['Frn0'] = true; // add the section heading row
		} else 
		   ids['*']=true;
		
		getVars();
		var pageNames = Object.keys(pages);
		var k=[2,3,2,2,2];
		for ( var i = 0; i < pageNames.length; i++)
			showRows(pageNames[i], ids, pages[pageNames[i]], k[i]);
	}

	function refresh() {
		document.location = '/dm/sm?refresh=1';
	}
