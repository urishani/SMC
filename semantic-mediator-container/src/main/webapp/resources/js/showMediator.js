
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
 		// File Name: showMediator.js
 		
         var sav =[];
         var cont =[];
		 var _toDisplayId, _fromDisplayId; 
		 function onLoad(toDisplayId, fromDisplayId) {
		    _toDisplayId = toDisplayId;
		    _fromDisplayId = fromDisplayId;
		 }
         function showGraph(a, fResource, tResource, radius) 
          {
           var p = a.parentNode;
           var tds = p.parentNode.cells;
           var ntd = tds[0];
           var ftd = tds[2];
           var ttd = tds[4];

           //var gFr = document.getElementById("graphFImg");
           //var gTo = document.getElementById("graphTImg");
           //var td = document.getElementById("graph");
           //var rs = document.getElementById("graphResource");
           var about = "Showing resource: from:" + fResource + " and to:" + tResource;
           var fId=_fromDisplayId.split(':')[0];
           var flink = "/dm/smGraph?resource=base:"+fResource+"&id="+fId+"&action=showGraph&radius="+radius;
           var tId=_toDisplayId.split(':')[0];
           var tlink = "/dm/smGraph?resource=base:"+tResource+"&id="+tId+"&action=showGraph&radius="+radius;
           var fw = ftd.clientWidth;
           var tw = ttd.clientWidth;
           var w = (fw > tw) ? fw:tw;
           ftd.innerHTML = setGraph(ftd.innerHTML, flink);
           var img = ftd.childNodes[2];
           img.style.maxWidth=w;
           ttd.innerHTML = setGraph(ttd.innerHTML, tlink);
           img = ttd.childNodes[2];
           img.style.maxWidth=w;
          }


         function setGraph(html, link) 
          {           
           var imgP = html.indexOf("<img");
           if (imgP >= 0)
              return html;
           return html + "<br><img src='" + link + "'>";
          }
         function load(a, fr, tr, fn, tn)
          {
           var p = a.parentNode;
           var tds = p.parentNode.cells;
           var ntd = tds[0];
           var ftd = tds[2];
           var ttd = tds[4];
           sav[fr] = ftd.innerHTML;
           sav[tr] = ttd.innerHTML;
           var close = '<button onclick=   "myDone(this, \''   + fr + '\', \'' + tr + '\')">   Close</button>';
           var graph = '<br><button onclick="showGraph(this, \''   + fn  + '\', \'' + tn  + '\', 0)">Graph</button>';
           sav[ntd.innerHTML]=p.innerHTML; 
           if (cont[fr]) {
              ftd.innerHTML = cont[fr];
              ttd.innerHTML = cont[tr];
              p.innerHTML = close + graph;
              return false;
           }
           p.innerHTML= 'loading...';
           var fxmlhttp, txmlhttp;
           if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
              fxmlhttp=new XMLHttpRequest();
              txmlhttp=new XMLHttpRequest();
           } else {// code for IE6, IE5
              fxmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
              txmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
           }
           
           var cnt = 0;
           function f(r, td, xmlhttp)  {
              if (xmlhttp.readyState==4 && xmlhttp.status==200)   {
                 var resp = xmlhttp.responseText;
                 resp = resp.replace(/</g,'&lt;');
                 resp = resp.replace(/>/g,'&gt;');
                 var iH = '<pre>' + resp + '</pre>';
                 cont[r] = iH;
                 td.innerHTML= iH;
                 cnt++;
                 if (cnt == 2)
                    p.innerHTML = close + graph;
			  } else if (xmlhttp.readyState == 4 && xmlhttp.status != 200)
				 p.innerHTML = 'ERROR !! ';
           }
         
           fxmlhttp.onreadystatechange= function() {f(fr, ftd, fxmlhttp);};
           fxmlhttp.open("GET",fr,true);
           fxmlhttp.setRequestHeader("Accept","text/turtle");
           fxmlhttp.send();

           txmlhttp.onreadystatechange= function() {f(tr, ttd, txmlhttp);};
           txmlhttp.open("GET",tr,true);
           txmlhttp.setRequestHeader("Accept","text/turtle");
           txmlhttp.send();
           
           return false;
        }
        
        function myDone(a, fr, tr) {
           var p = a.parentNode;
           var tds = p.parentNode.cells;
           var ntd = tds[0];
           var ftd = tds[2];
           var ttd = tds[4];
           p.innerHTML = sav[ntd.innerHTML];
           sav[ntd.innerHTML] = null;
           ftd.innerHTML = sav[fr];
           ttd.innerHTML = sav[tr];            
           return false;
        }
