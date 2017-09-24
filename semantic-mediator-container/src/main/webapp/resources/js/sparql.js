
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
 *| File Name: sparql.js                                                   |
 *+------------------------------------------------------------------------+
 */

            var base = "_base_";
            var version = "";
            var id = "_id_";
            function onload(_base_, _version_, _moreless_, _id_) {
               base = _base_;
               id = _id_;
               if (_version_)
                  version = "?version="+_version_;
               if (_moreless_.endsWith('More'))  {
            	   var x = document.getElementById('morelessButton');
            	   moreLess(x);
               }
               var t = document.getElementById('table');
      		            
               var trs = t.rows;
               for (i=1; i< trs.length; i++) {
                  var tds = trs[i].cells;
                  for (j=0; j < tds.length; j++) {
                     var td = tds[j];
                     var v = td.innerHTML;
                     var p = v.indexOf('base:');
                     if ( p == 0) {
                        td.innerHTML = "<a href='" + base + v.substring('base:'.length) +
                        version + "?id="+ id + "'>" + v + "</a>";
                     } 
                  }
               }
            }
            function init() {
               var div = document.getElementById('prefixes');
               var prefixes = div.innerHTML;
               var ps = prefixes.split('\n');
               var prefs = '';
               for (var p=0; p < ps.length; p++) {
                  prefs += ps[p].trim() + '<br>' + '\n';
               }
               div.innerHTML = prefs;
            }
            function usedBy(el) {
               updateVar(el.checked, 'usedBy', '   optional { ?usedBy ?usedAs ?resource .}');
               updateVar(el.checked, 'usedAs', '');
               updateVar(el.checked, 'usedByName', '  optional { ?usedBy rdfs:label ?usedByName .}');
            }
            function update(el) {
                var ch = el.checked;
                var v = el.name;
                updateVar(ch, v, '   ?resource %v ?%vr .');
                                // Now update the orderby parts:
                
                var ob = document.getElementById('sort' + v);
                ob.disabled = !ch;
                // enable if main is checked.
                if (ob.disabled)
                    ob.checked = false;
                var obd = document.getElementById('desc' + v);
                obd.disabled = !ob.checked;
                // enable if prev. is checked.
                if (obd.disabled)
                    obd.checked = false;
                return doOrderBy(ob);
            }
            function updateVar(ch, v, template) {
                var sEl = document.getElementById('select');
                var s = sEl.value;
                var sterms = s.split(' ');
                var wEl = document.getElementById('where');
                var w = wEl.value;
                var wterms = w.split('\n');
                // find index of closing }
                var last;
                for ( last = 0; last < wterms.length; last++) {
                    if (wterms[last].trim() == '}')
                        break;
                }
//                var ch = el.checked;
//                var v = el.name;
                var vr = v.replace(':', '_');
                // look for v in wterms
                for (var i = 0; i < wterms.length; i++) {
                    if (wterms[i].trim().indexOf(v) >= 0)
                        break;
                }
                var rm = !ch;
                if (i < wterms.length) {// found it
                    if (!ch) {// remove it
                        wterms[i] = null;
                    }
                } else {// did not find it
                    if (ch) {// add it
                        var tmp = wterms[last];
                        template = template.replace("%vr", vr).replace("%v", v);
                        wterms[last] = template;
                        wterms[last + 1] = tmp;
                    }
                }
                if (rm) {// need to also remove the variable form the select list
                    for ( i = 0; i < sterms.length; i++)
                        if (sterms[i].trim() == ('?' + vr))
                            sterms[i] = null;
                    // remove it
                } else {// need to add it
                    sterms[sterms.length] = ' ?' + vr;
                }
                // now rebuild the strings
                s = join(sterms, ' ');
                w = join(wterms, '\n');
                sEl.value = s;
                wEl.value = w;
                //   alert('update for [' + el + '] ' + el.checked + ' - ' + el.value + ' - ' + el.name); // + '\n' +
                //   alert(   'select =[' + s.value + ']; where= [' + w.value + ']');
            }

            function join(v, d) {
                var t = '';
                for (var i = 0; i < v.length; i++)
                    if (v[i])
                        t += v[i] + d;
                return t;
            }

            function setMoreless() {
            	var morelessVal = document.getElementById('morelessButton').value;
            	morelessVal = morelessVal.endsWith("More")?"...Less":"...More";
            	var x = document.getElementById('morelessParam');
            	x.value = morelessVal;
            	return false;
            }
            
            // handles the order by part of the query and is called whenever the status of a certain predicate
            // changes.
            // Parameter is the orderBy checkbox element.
            function doOrderBy(oEl) {
                var n = oEl.name;
                var p = n.substring("sort".length);
                var dEl = document.getElementById('desc' + p);
                var pEl = document.getElementById('prop' + p);
                oEl.disabled = !pEl.checked;
                if (oEl.disabled)
                    oEl.checked = false;
                dEl.disabled = !oEl.checked;
                if (dEl.disabled)
                    dEl.checked = false;

                // parse present status
                var oTermEl = document.getElementById('orderBy');
                var o = oTermEl.value;
                var oterms = o.split(' ');

                // remove old term if exists
                p = '?' + p.replace(':', '_');
                for (var i = 0; i < oterms.length; i++) {
                    if (oterms[i].trim().indexOf(p) >= 0) {
                        oterms[i] = null;
                    }
                }

                var term = makeTerm(oEl.checked, dEl.checked, p);
                // add new one:
                oterms[oterms.length] = term;
                // rebuid the terms
                var mo = join(oterms, ' ');
                oTermEl.value = mo;
                return false;
            }

            function makeTerm(o, d, p) {
                var t = '';
                if (o)
                    t = p;
                if (d)
                    t = 'DESC(' + p + ')';
                return t;
            }

            // param is the id of the orderby element so it will reprocess the status of that.
            function doDESC(id) {
                var ob = document.getElementById(id);
                return doOrderBy(ob);
            }

	// Added on July 16, 2014.            
		function setupSave() {
		   var sn = document.getElementById('saveName');
		   var ss = document.getElementById('saveSelect');
		   var rs = document.getElementById('restore');
		   var sv = document.getElementById('name_');
		   var dl = document.getElementById('delete');
		   rs.checked=false;
		   sv.checked=false;
		   dl.checked=false;
		   var n = sn.value;
		   if (!n) 
		      n = "NaN";
		   var ssc = ss.children;
		   for (i=0; i< ssc.length; i++) {
		      var ssc_ = ssc[i];
		      if (ssc_ && ssc_.value == n) {
		         ssc_.selected = true;
		         break;
		      }
		   }
		}
		
		function setName(select) {
		   var n = select.value;
		   if (n == "NaN")
		      n = "";
		   var sn = document.getElementById('saveName');
		   if (sn.value == n) return;
		   sn.value = n;	
		   if (!n) return;	   
		   var restore = document.getElementById("restore");
		   restore.checked = true;
		   var sb = document.getElementById('execute');
		   var f = sb.form;
		   f.submit();
		}
		
		function changed(input, text) {
		   var chButId = input.name + '_';
		   var chBut = document.getElementById(chButId);
		   if (text)
		   	   chBut.checked = (input.value.trim() != "");
		   else {
			   input.value = 1*input.value;
		   	   chBut.checked = (input.value > 0);
		   }
		}
