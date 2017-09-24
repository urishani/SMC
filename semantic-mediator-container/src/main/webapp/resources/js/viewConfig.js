
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
 *| File Name: viewConfig.js                                               |
 *+------------------------------------------------------------------------+
 */


	function updateViewConfig(item) {
	   var td = item.parentElement;
	   var tr = td.parentElement;
	   var title = tr.cells[3];
	   var title = title.firstChild;
	   titleV = title.value;
	   var tag = tr.cells[4];
	   var tag = tag.firstChild;
	   tagV = tag.value;
	   var predicate = tr.cells[0].innerHTML.trim();
	   var tChecked = tr.cells[1];
	   tChecked = tChecked.firstChild.checked;
	   var lChecked = tr.cells[2];
	   lChecked = lChecked.firstChild.checked;
	      var el = {};
	      el['title']=titleV;
	      el['tag']=tagV;
	      el['content']=[ predicate ];
	      viewConfig = vc_replace(viewConfig, predicate, el);
	      el['forText']=tChecked;
	      el['forView']=lChecked;
	      title.disabled= lChecked;
	      tag.disabled= lChecked;
          var x = JSON.stringify(viewConfig, null, 2);
	   actualViewConfig.value = x;
	   vc_setViewConfig = true;
	}
	function vc_replace(config, p, e) { // replace the element whose 'content' contains p with e, in config.
	    if (! config)
	       config = [];
	    var i = vc_findPredicate(config, p);
	    if (i >= 0) {
            config[i] = e;
	        return cleanArray(config);
	    }
	    if (e)
	       config[config.length]= e; // add it if not null.
	    return config;
	}

    function vc_findPredicate(config, p) {
       for (var i=0; i < config.length; i++) {
	       var el = config[i]; 
	       var ps = el['content'];
	       if (ps.length < 1)
	          continue;
	       for (var j=0; j<ps.length; j++)
	          if (ps[j] == p) {
	             return i;
	          }
	    }
    	return -1;
    }	
    
	function cleanArray(arr) {
	   var x = [];
	   var j= 0;
	   for (i=0;i<arr.length;i++)
	      if (null != arr[i]) {
	         x[j]=arr[i];
	         j++;
	      }
	   return x;
	}
	function vc_addRow(but) {
	   var tbl = document.getElementById('predicateTable');
	   var mytd = but.parentElement;
	   var mytr = mytd.parentElement;
	   var predicate = mytr.cells[1].firstChild.value;
	   var i = vc_findPredicate(actualViewConfig, predicate);
	   if (i >= 0) {
	      alert('Predicate [' + predicate + '] already exists. Pick a different one or delete the old one.');
	      return;
	   }
	   var p1 = predicate.lastIndexOf("#"); 
	   var p2 = predicate.lastIndexOf("/"); 
	   p1 = Math.max(p1,p2);
	   var name = "";
	   if (predicate.length > p1 +1) {
	      name = predicate.substring(p1 +1 ); 
	      name = name[0].toUpperCase() + name.substring(1);
	   }
	   var body = tbl.firstElementChild;
	   body.insertRow(-1); // add row at the end.
	   var trs = body.rows;
	   trs[trs.length-1].innerHTML = mytr.innerHTML; // copies the button to last row.
	   var x = trs[1].innerHTML;
	   trs[trs.length-2].innerHTML = x // copies td structure of the first "real" row to the one before last - the new row.
	   var tds = trs[trs.length-2].cells;
	   tds[0].innerHTML=predicate;
//	   alert(tds[3]); alert(tds[3].firstChild);
	   if (name)
	      tds[3].firstChild.value = name;
	   tds[0].firstChild.disabled=false;
	   updateViewConfig(tds[1].firstChild);
	}
	function vc_removeRow(but) {
	   var mytd = but.parentElement;
	   var mytr = mytd.parentElement;
	   var predicate = mytr.cells[0].innerHTML.trim();
	   viewConfig = vc_replace(viewConfig, predicate, null);
       var x = JSON.stringify(viewConfig, null, 2);
	   actualViewConfig.value = x;
	   mytr.hidden=true;
	   vc_setViewConfig = true;
	}
	function showViewConfig(but) {
	   var td = document.getElementById('viewConfigTd');
	   var toShow = but.value == 'show';
	   but.value = toShow?'hide':'show';
	   but.innerHTML = but.value;
       td.hidden = !toShow;
	}
	
	var vc_setViewConfig = false;   // used to flag if the configuration was changed.
	var vc_wasReset = false;
	var vc_msg = null;
	var savedViewConfig = null;
    var actualViewConfig = null;
    var viewConfig = null;
	
	function vc_onLoad() {
	   vc_msg = document.getElementById('viewConfigMessage');
	   savedViewConfig = document.getElementById('savedViewConfig');
	   actualViewConfig = document.getElementById('actualViewConfig');
	   viewConfig = eval(actualViewConfig.value);
	   vc_msg.hidden = savedViewConfig.value != ""; // show this message if using default.
	}
	var vc_resetText = 'Reset to default ';
	var vc_customizeText = 'Customize ';
	function vc_toggleViewTable(but) {
	   var tbl = document.getElementById('viewConfigTable')
	   var txt = but.innerHTML;
	   var hide = txt.indexOf('Reset')==0; 
	   if (hide) { // reset to default
	      if (actualViewConfig.value) {
	         if (false == confirm("Discard of all customizations?"))
	            return false;
	      }
	      txt = vc_customizeText + txt.substring(vc_resetText.length);
	      vc_msg.hidden = false;
	      actualViewConfig.value = ""; // on update, it will be copied to the saved view config to be updated on the server.
	      vc_setViewConfig = true;
	      vc_wasReset = true;
	      but.disabled = vc_wasReset;
	   } else { // turn on customization
	      txt = vc_resetText + txt.substring(vc_customizeText.length);
	      vc_msg.hidden = true;
	   }
	   but.innerHTML = txt;
	   tbl.hidden = hide;
	   vc_setViewConfig = hide;
	   return false;
	}
	
	function vc_doUpdate() {
	   if (vc_setViewConfig) { // copy edited area content to the viewConfig field to be sent back.
	      savedViewConfig.value = actualViewConfig.value;
	   }
	   return true;
	}
	
