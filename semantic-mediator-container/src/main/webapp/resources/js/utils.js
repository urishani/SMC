
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
 *| File Name: utils.js                                                    |
 *+------------------------------------------------------------------------+
 */

    // Return with the resource in the format base:number from a gigen resource URL
    function resourceNum(url) {
       var idx = url.indexOf('?');
       if (idx >= 0)
          url = url.substring(0, idx);
       idx = url.indexOf('resource/');
       if (idx >= 0)
          url = url.substring(idx + 'resource/'.length);
       return 'base:' + url;
    }

    // Make a csv from an object's values. If an array, than these are the array members.
    function makeList(group) {
       var result = '';
       for (item in group) {
          if (! group[item]) 
             continue;
          result += group[item] + ',';
       }
       // remove trailing comma
       return result.substring(0, result.length - 1); 
    }
    

	// Recursive function to timely execute modifications as per the sAction and fAction functions.
	// sAction is done per each step, fAction - is the action on the final step.
	// First use is to dimm and brighten the sidebar on its expansion and contraction 
	function timelyExcecution(f, t, inc, delay, sAction, fAction) {
   		if ((inc > 0 && f > t) || (inc < 0 && f < t)) {
      		fAction();
      		return;
   		}
   		sAction(f);
   		f += inc;
   		setTimeout(function(){timelyExcecution(f, t, inc, delay, sAction, fAction)}, delay);
	}


	function addClass(element, className) {
	   element.className += " "+ className;
	}
	
	function removeClass(element, className) {
	   element.className = element.className.replace(className, "");
	}
	
	function saveVar(varName, varValue, refresh) {
		//alert('var='+varName + '; val=' + varValue);
		var target = "/dm/sm?var=" + varName + "&val=" + varValue;
		//alert('target [' + target);
		if (parent.useFrames)
			parent[varName] = varValue;
		this[varName] = varValue;
		//alert("ontologyVar: " + parent.ontology);
		if (refresh)
			document.location = target + '&refresh=1';
		else {
			xmlhttp.open("GET", target, true, null, null);
			xmlhttp.send("");
		}
	}
	
	function getProjectId() {
		if (parent.useFrames)
			return parent.project;
		else
		    return "";
	}
	// answer with modified text so all subtexts between markers ma and mb, in input text,
    // input text is rdf format.
    // which start ancor are made into an <a> link with same contents.
    // Param noBackLink is true sets up to not create trigger to view back link. Only forward links.
    function makeLink(text, ma, mb, ancor, level, noBackLink) {
       var newtext = "";
       var maL = ma.length;
       var mbL = mb.length;
       while (text.length > 0) {
           var p = text.indexOf(ma + ancor);
           if (p < 0) {
               newtext += text;
               break;
           }
           var q = text.substring(p + maL).indexOf(mb);
           if (q < 0 ) { //|| (text.indexOf("/dm/sm/repository") < 0 && text.indexOf("/dm/sm/catalog"))) {
               newtext += text.substring(0, maL);
               text = text.substring(maL);
               continue;
           }
           q += p + maL;
           var pre = text.substring(0, p);
           var post = text.substring(q + mbL);
           var a = text.substring(p + maL, q);
           var b = a;
           if (_id_ != "_id_" && _id_ && b.indexOf('id=') < 0)
              b += "?id=" + _id_;
           if (_version_) {
              if (b.indexOf("?")<0)
                 b += "?"
              else
                 b += "&"
              b += _version_;   
           }
           newtext += pre + ma + "<a href='" + b + "'>" + a + "</a>&nbsp;;&nbsp;" + "<a href='javascript:peek(\"" + b + "\", " + level + ")'>[&rarr;]</a>";
           if (!noBackLink) 
              newtext += "&nbsp;;&nbsp;" + "<a href='javascript:back_peek(\"" + b + "\", " + level + ")'>[&larr;]</a>"
           newtext += mb;
           
           text = post;
       }
       return newtext;
   }

   function getResource(a, saver, type) {
      var req = new XMLHttpRequest();
      if (! type)
        type = 'application/rdf+xml';
      req.open("GET", a, true, null, null);
      req.setRequestHeader('accept', type);
      req.onreadystatechange = function() {
         var ok = false
         if (this.readyState == 4) {
            if (this.status == 200) {
               var r = this.responseText;
               saver(r);
               ok = true;
            }
         }
         var msg = document.getElementById("messages");
         if (msg) {
               msg.innerHTML = ok? " OK " : "failed. ReadyState [" + this.readyState + "], status [" + this.status + "]";
 	     }
 	  }   
      req.send('');
   }
   
   // Gets and stores textual content into an element
   function getContent(where, link) {
      if (!where) 
         return;
      getResource(link, function (r) {
         where.innerHTML = r;
      }, 'text/plain');
   }
   
   function getLinkedResources(a, saver, type) {
      var req = new XMLHttpRequest();
      if (! type)
        type = 'text/plain';
        // strip off the resource number
        var i = a.lastIndexOf("/")
        var n = a.substring(i+1);
        var id = n.substring(n.indexOf('?')+1);
		// n is in the format "0171?id=prt-575", and we need to separate the two.
        n = n.substring(0, n.indexOf('?'));
        var oldA = a;
        a = a.substring(0, i) + '?sparql&select=?usedBy &where=?usedBy ?r base:'+ n +' .&' + id ;
      req.open("GET", a, true, null, null);
      req.setRequestHeader('accept', type);
      req.onreadystatechange = function() {
         var ok = false
         if (this.readyState == 4) {
            if (this.status == 200) {
               var r = this.responseText;
               saver(r);
               ok = true;
            }
         }
         var msg = document.getElementById("messages");
         if (msg) {
               msg.innerHTML = ok? " OK " : "failed. ReadyState [" + this.readyState + "], status [" + this.status + "]";
 	     }
 	  }   
      req.send('');
   }

   function setProps4contentType(contentType) {
      var ma = '"';
      var mb = '"';
      var nsPrefix = 'xmlns:';
      if (contentType == 'text/turtle') {
         ma = '&lt;';
         mb = '&gt;';
         nsPrefix = '@prefix';
      } else if (contentType == 'application/n3') {
         ma = '&lt;';
         mb = '&gt;';
         nsPrefix = '@prefix';
      } else if (contentType == 'application/n-triples') { 
         ma = '&lt;';
         mb = '&gt;';
         nsPrefix = 'NoPrefix';
      } else if (contentType == 'application/rdf+xml-ABBREV') {
      }
      return {'ma': ma, 'mb': mb, 'nsPrefix' : nsPrefix};
   }
	
   // Some utilities for edit panels.
   // When called from a cancel button, will activate the update submit with the cancel message.
   function doCancel() {
   	var b = document.getElementById("updateButton")
   	b.value="Cancel"
   	b.click()
   }

