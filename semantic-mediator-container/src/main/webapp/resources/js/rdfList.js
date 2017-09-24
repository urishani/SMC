
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
 *| File Name: rdfList.js                                                  |
 *+------------------------------------------------------------------------+
 */

var _id_, _base_, _query_, _hasRefs_, _isTemp_, _enableSave_, _collectionName_, _classes_, _version_, _portType_;
function onLoad(idP, baseP, queryP, hasRefsP, isTempP, enableSaveP, collectionNameP, classesP, versionP, portTypeP) {
				_id_ = idP; _base_=baseP; _query_=queryP; 
				_hasRefs_=hasRefsP; _isTemp_ = isTempP; _enableSave_ = enableSaveP;
				_collectionName_ = collectionNameP; _classes_ = classesP;
				_version_ = versionP;
				_portType_ = portTypeP;
                setupAttachmentDialog();
                setupLuceneDialog();
                //alert('onload');
                var ulel = document.getElementById('ul');
                var lis = ulel.childNodes;
                var i = 0;
                map = new Object();
                for ( var p = 0; p < lis.length; p++) {
                    var pdef = lis[p].innerHTML;
                    if ( typeof pdef === 'undefined')
                        continue;
                    var parts = pdef.split(':=');
                    var prefix = parts[0].trim();
                    var ns = parts[1].substring(4).split('&gt;')[0];
                    map[prefix] = ns;
                }
                var resCell = 1;
                if (_collectionName_ == 'Port')
                   resCell= 2;
                var tbl = document.getElementById('table');
                var rows = tbl.rows;
                for ( var r = 1; r < rows.length; r++) {// we skip headers row
                    var tr = rows[r];
                    var cells = tr.cells;
                    var res = cells[resCell];
                    var resource = res.innerHTML.replace(/^\s+/, '').replace(/\s+$/, '');
                    if (resource.indexOf(_base_ +':') == 0) {
                        url = makeUrl(resource, map);
                        resource = makeLink(url, resource);
                    }
                    res.innerHTML = resource;
                    var type = cells[resCell+1];
                    var typeInfo = type.innerHTML.replace(/^\s+/, '').replace(/\s+$/, '');
                    if (typeInfo != '-') {
                        var url = _query_ + typeInfo + '&id=' + _id_ + ((_version_)?'&':'') + _version_;
                        typeInfo = makeTypeQuery(url, typeInfo);
                    }
                    type.innerHTML = typeInfo;
                }
            }

			var isAttachment = false;
            function setupAttachmentDialog() {
                var element = document.getElementById('attachments');
                if (!element)
                    return;
                // it is not there anyway!
                element.hidden = true;
                element = document.getElementById('attachmentsButton');
                if (!element)
                    return;
                // it is not there anyway!
                isAttachment = ( _id_ == 'PrtAR')
                element.hidden = ! isAttachment;
            }

            function setupEditButton() {
                var isTemp = _isTemp_;
                var enableSave = _enableSave_;
                var canEdit = (!isTemp && _collectionName_ == 'Port' && _portType_ == 'tool');
                var editButton = document.getElementById('edit');
                if (canEdit)
                    editButton.hidden = false;

                var isEditable = isTemp;
                var saveButton = document.getElementById('saveButton');
                var done_cancelButton = document.getElementById('done_cancelButton');
                var doneButton = document.getElementById('doneButton');
                if (isEditable) {
                    saveButton.hidden = false;
                    done_cancelButton.hidden = false;
                    doneButton.hidden = true;
                    if (enableSave)
                        saveButton.disabled = false;
                }
                var show = isEditable ? 'block' : 'none';
                for ( i = 1; ; i++) {
                    var d = document.getElementById('edit' + i);
                    if (!d)
                        break;
                    d.style.display = show;
                }
                if (isEditable)
                    setupClassOptions();
                //alert(document.getElementById('edit1').innerHTML);
            }

            function setupClassOptions() {
                var classes = _classes_;
                var sel = document.getElementById('classSelect');
                //   var opts = sel.children;
                for (var i in classes) {
                    var opt = new Option(classes[i]);
                    sel.add(opt);
                    //opts[opts.length] = opt;
                }
                var t = sel.innerHTML;
                //alert(t);

            }

            function enableUp() {
                var element = document.getElementById('attachments');
                if (!element)
                    return false;
                // it is not there anyway!
                element.hidden = false;
                element = document.getElementById('attachmentsButton');
                if (!element)
                    return false;
                // it is not there anyway!
                element.hidden = true;
                return false;
            }

            function disableDown() {
                var element = document.getElementById('attachments');
                if (!element)
                    return false;
                // it is not there anyway!
                element.hidden = true;
                element = document.getElementById('attachmentsButton');
                if (!element)
                    return false;
                // it is not there anyway!
                element.hidden = false;
                return false;
            }

            function makeUrl(info, map) {
                var url = info;
                if (url.indexOf(':') >= 0) {
                    var parts = info.split(':');
                    var prefix = parts[0];
                    var ns = map[prefix];
                    url = ns + parts[1];
                }
                return url;
            }

            function makeTypeQuery(url, typeInfo) {
                if (_hasRefs_ == 'true')
                    return '<a href="' + url + '">' + typeInfo + '</a>';
                return typeInfo;
            }

            function makeLink(url, resource) {
                return '<a href="' + url + '?id=' + _id_ + ((_version_)?'&':'') + _version_ + '">' + resource + '</a>';
            }

            function doVersion1(el) {
                //Register value only if different from version2
                ver1 = el.value;
                document.getElementById('compare').disabled = (ver1 == ver2 || ver1 == '' || ver2 == '') ? 'disabled' : '';
            }

            var ver2 = 'current';
            function doVersion2(el) {
                ver2 = el.value;
                document.getElementById('compare').disabled = (ver1 == ver2 || ver1 == '' || ver2 == '') ? 'disabled' : '';
            }

            var oldTrHTML;
            var oldTr;
            function editSubject(tdItem, resource, id) {
                if (oldTr) {
                    oldTr.innerHTML = oldTrHTML;
                    oldTr = null;
                }
                oldTr = tdItem.parentElement;
                oldTrHTML = oldTr.innerHTML;

                expandRow(oldTr, resource, id);
                return false;
            }

            function expandRow(tr, resource, id) {
                var target = "/dm/sm/repository?action=editIndividual&id=" + id + "&resource=" + resource;
                var req = new XMLHttpRequest();
                req.open("GET", target, true, null, null);
                req.onreadystatechange = function() {
                    if (this.readyState == 4) {
                        if (this.status == 200) {
                            tr.innerHTML = this.responseText;
                        }
                    }
                };
                req.send("");
            }
            
            function setupLuceneDialog() {
               var luceneDiv = document.getElementById('luceneDiv');
               if (_id_.indexOf('Prt') < 0)
                  luceneDiv.hidden=true;
            }
            
            var oldText = '';
            function luceneQuery(button) {
               var text = document.getElementById('luceneQuery');
               var resetButton = document.getElementById('resetQuery');
               var query = text.value;
               if (query.trim() == '') return;
               oldText = query;
               text.value = '... working...';
               text.disabled=true;
               button.disabled=true;
               var xmlhttp;
           	   if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
                   xmlhttp=new XMLHttpRequest();
               } else {// code for IE6, IE5
                   xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
               }
               xmlhttp.onreadystatechange=function()  {
                   if (xmlhttp.readyState==4 && xmlhttp.status==200)   {
                      var resp = xmlhttp.responseText;
                      text.value= resp;
                      select(resp);
			       } else if (xmlhttp.readyState == 4 && xmlhttp.status != 200)
				      text.value = 'ERROR !! ';
				   resetButton.disabled = false;
               }
               query = query.replace(/</g,'&lt;');
               query = query.replace(/>/g,'&gt;');
               xmlhttp.open("GET","/dm/smLucene?id=" + _id_ + "&query=" + query + ((_version_)?'&':'') + _version_, true);
               xmlhttp.setRequestHeader("Accept","text/plain");
               xmlhttp.send();
               return false;    
            }
            
            function resetQuery(resetButton) {
               var queryArea = document.getElementById('luceneQuery');
               var runQuery = document.getElementById('runQuery');
               resetButton.disabled=true;
               queryArea.value = oldText;
               queryArea.disabled = false;
               runQuery.disabled = false;
               select();
            }
            
            function select(list) {
                if (list && list.indexOf('Error:') == 0)
                   list = false;
                var items = {};
                var l = [];
                if (list) {
                   l = list.split(';');
                   for (var i=0; i < l.length; i++) {
                      var x = l[i].trim();
                      x = _base_ + ":" + x.substring(x.indexOf('resource/') + 'resource/'.length);
                      items[x]=x; 
                   }
                }
                var tbl = document.getElementById('table');
                var rows = tbl.rows;
                for ( var r = 1; r < rows.length; r++) {// we skip headers row
                    var tr = rows[r];
                    if (!list) {
                       tr.hidden=false;
                       continue;
                    }
                    var cells = tr.cells;
                    var res = cells[2];
                    res = res.firstChild.innerHTML;
                    tr.hidden = !(res in items);
                }
           }
           
           // Added July 16, 2014 to handle graph display embedded within the table.
           
          function showResourceGraph(a, resource, id, radius, version) 
          {          
           var p = a.parentNode;
           var tds = p.parentNode.cells;
           var ntd = tds[0];
           var td = tds[2];
           var about = "Showing resource: from:" + resource;
           var link = "/dm/smGraph?resource="+resource+"&id="+id+"&action=showGraph&radius="+radius;
           if (version) {
             if (link.indexOf("?") < 0)
                link += "?";
             else 
                link += "&";
             link += "version="+version;
           }

           var h = setGraph(td.innerHTML, link);
           if (h)
              td.innerHTML = h;
         }


         function setGraph(html, link) 
          {           
           var imgP = html.indexOf("<img");
           if (imgP <= 0)
              return null;
           return html + "<br><img src='" + link + "'>";
          }
           