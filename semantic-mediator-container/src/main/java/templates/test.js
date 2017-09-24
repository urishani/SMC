
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
function toggle(but) {
   var td = but.parentNode;
   var id = td.childNodes[1];
   id = id.value;
   var dv = td.childNodes[2];
   but.disabled=true;
   var curValue= but.value;
   but.value="Busy...";
   var newValue = "More...";
   if (curValue=="More...")
      newValue = "Less...";

   var xmlhttp;
   if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
       xmlhttp=new XMLHttpRequest();
   } else {// code for IE6, IE5
      xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
   }
   xmlhttp.onreadystatechange=function()  {
       if (xmlhttp.readyState==4 && xmlhttp.status==200)   {
           var resp = xmlhttp.responseText;
           resp = resp.replace(/</g,'&lt;');
           resp = resp.replace(/>/g,'&gt;');
           dv.innerHTML= resp;
           but.value="Less...";
           but.disabled=false;
	   } else if (xmlhttp.readyState == 4 && xmlhttp.status != 200)
          dv.innerHTML = 'ERROR !! ';
   }
   xmlhttp.open("GET","/dm/smLogs?UpdgradeLog=" + id + "&Action=" + substr(curValue,0,1), true);
   xmlhttp.setRequestHeader("Accept","text/plain");
   xmlhttp.send();
}
