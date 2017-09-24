
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
 *| File Name: rdf4oslc.js                                                 |
 *+------------------------------------------------------------------------+
 */
// Requires utils.js

			var _id_ = "_id_";
			var _contentType_;
			var resource = null;
			var content = null;
            var replacement = null;
            var _host_;
			var _canmodify_;
			var _version_ = "";
			var noBackLinks = false;
			var resources = [];
            function onLoad(id, contentType, hostP, canmodifyP, versionP, noBackLinksP) {
            	    checkComments();
                resource = resourceNum(document.URL);
                resources[-1]=resource;
                _host_ = hostP;
                _canmodify_ = canmodifyP;
                _id_ = id;
                if (versionP != "_version_")
                   _version_ = "version=" + versionP;
                _contentType_ = contentType;
                if (noBackLinksP && noBackLinksP != "_noBackLinks_")
                   noBackLinks = true;
                if (! _contentType_ || _contentType_ == '_contentType_') 
                   _contentType_ = 'text/turtle';
                if (! _id_ || _id_ == '_id_') _id_ = "";
                if (! _version_ || _version_ == '_version_') _version_="";
 		        if (!_host_ || _host_ == "_host_") _host_ = "";
                
                   
                var cte = document.getElementById('contentType');
                if (cte)
                   cte.value = _contentType_;
                if (! _canmodify_)
                   document.getElementById('modify').style.display='none';
                hidePopups();
                rdfE = document.getElementById('text');
                var rdf = rdfE.value;
                rdf = process(rdf, -1);
                rdfE.hidden = 'true';
                div = document.getElementById('textDiv');
                div.innerHTML = rdf;
                resource = window.location.pathname;
                replacement = document.getElementById("editableRDF").innerHTML;
                
            }

            function checkComments() {
            	   var span = document.getElementById("comments");
            	   var spanText = span.lastChild.data;
            	   if (spanText == "_comments_")
            		   span.hidden = true;
            }
            
			function strip(query) {
			   var p = query.indexof("?");
			   if (p >= 0)
			      query = query.substring(0,p);
			   return query;
			}
			
			// Scan all lines and if longer than len, split along separator sep.
			function splitLines(lines, len, sep) {
			   if (!sep) sep = ',';
			   if (!len) len = 100;
			   var nlines = [];
			   for (i in lines) {
			      var line = lines[i];
			      if (line.length > len) {
			         var segs = line.split(sep);
			         for (j in segs) {
			            var seg = segs[j];
			            while (seg.length > len) {
			               var p = Math.round(len * 0.9);
			               var k = p;
			               for (; k < seg.length && seg[k]!=' '; k++);
			               if (k >= seg.length)
			                  break;
			               nlines[nlines.length] = seg.substring(0, k);
			               seg = seg.substring(k);
			            }
			            nlines[nlines.length] = seg;			              
			         }
			      } else
			         nlines[nlines.length]=line;
			   }
			   return nlines;
			}
			
            function process(rdf, level) {
                rdf = rdf.split('\n');
                rdf = splitLines(rdf, 100);
                var ok = false;
                var nrdf = [];
                var props = setProps4contentType(_contentType_);
                for ( i = 0; i < rdf.length; i++) {
                    if (level >= 0 && rdf[i].trim().indexOf(props.nsPrefix)==0)
                       continue;
                    if (false) { // dont do that. skip to support other rdf formats besides xml.
                        if (!ok && rdf[i].indexOf("<rdf:Description") >= 0)
                            ok = true;
                        else if (ok && rdf[i].indexOf("</rdf:Description") >= 0)
                            ok = false;
                        if (!ok)
                            continue;
                    }
                    rdf[i] = rdf[i].replace(/\ /g, '&nbsp;').replace(/\</g, '&lt;').replace(/\>/g, '&gt;').
                    	replace(/&gt; , &lt;/g, '&gt; ,<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;');
                    var ancor = 'https';
                    if (_host_ != "_host_")
                       ancor = _host_;
                    if (level <= maxPop - 1)
                        rdf[i] = makeLink(rdf[i], props.ma, props.mb, ancor, level + 1, noBackLinks);
                    nrdf[nrdf.length] = rdf[i];
                }
                return nrdf.join('<br>');
            }

			function appendVersion(a) {
			    if (!_version_)
			       return a;
			    if (a.indexOf("version=") >= 0)
			       return a;
			    if (a.indexOf("?") >= 0)
			       a +="&";
			    else
			       a +="?";
			    return a + _version_;
			    
			}
			
			// Show the content as a next level of details.
            function peek(a, level) {
                  resources[level] = resourceNum(a);
                  var but = document.getElementById('showGraph');
                  //resetGraph(but);
                  showResourceGraph(but, true);
                  a = appendVersion(a);
                  getResource(a, function(rdf) { f(rdf, level);}, _contentType_);
            }
            function f(rdf, level) {
                    var t = process(rdf, level); //this.responseText, level);
                    var popup = document.getElementById('popup' + level);
                    popup.innerHTML = t;
                    popup.parentElement.parentElement.style.display = 'block';
                    popup.focus();
                    hidePopups(level)
                    window.scroll(0, document.body.offsetHeight);
                  }
			// Show the list of resources linking with the present one, or that ther are none to show.
            function back_peek(a, level) {
                  a = appendVersion(a);
                  getLinkedResources(a, function(rdf) { f(rdf, level);});
            }

            function hidePopups(level) {
                if (level == undefined)
                    level = popLevel;
                for (var i = level + 1; i <= maxPop; i++) {
                    resources[i]='';
                    document.getElementById('popup' + i).parentElement.parentElement.style.display = 'none'
                }
            }

            var popLevel = -1;
            var maxPop = 4
            function none() {
            }
            
            var theTd = null;
            var textarea = null;
            
            function edit(a) {
               theTd = a.parentElement.parentElement;
               content = theTd.innerHTML;
               if (! resource)
                  return false;
               getResource(resource, function(rdf) {
//                  alert(rdf);
                  theTd.innerHTML = replacement;
                  textarea = theTd.children[4];
                  msgarea = theTd.children[2];
                  textarea.value = rdf;
               }, _contentType_);
               return false;
            }
            
            function cancel(a) {
               window.location.reload()
            }
            
            function save(a, type) {
                var req = new XMLHttpRequest();
                if (! type)
                   type = _contentType_;
                if (! type)
                   type = 'text/turtle';                
                msgarea.value="Working...";
                req.open("PUT", resource, false, null, null);
                req.setRequestHeader("Content-type",type);
                req.onreadystatechange = function() {
                    if (this.readyState == 4) {
                        if (this.status == 200) {
                            var r = this.responseText;
                            msgarea.value = "Succeeded!";
                        } else
                           msgarea.value = "Failed with status: " + this.status;
                    } else
                       msgarea.value = "Failed for bad ready state: " + this.readyState; 
                }
                req.send(textarea.value);
            }
            
        function setContentType(sw) {
           var ct = sw.value;
           if (!ct || ct == _contentType_)
             return false;
           return true;
        }
        
        // requires to use showGraph.js
        function showResourceGraph(a, cond) {
           var url = makeList(resources);
           var link = 'resource=' + url + '&id=' + _id_ + '&' + _version_ + '&action=showGraph&radius=0';
  	       setImg_src(link);
  	       showGraph(a, 0, cond); 
		}
        
            
