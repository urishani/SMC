
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
// Javascript to show a graph, starting with a structure such as in the following template, within a table.
//                    <td  rowspan='6' style='border: 2px solid blue;vertical-align:top' '>
//                          <input id='showGraph' class='button_imageB button_showGraph' style='vertical-align:top' type='button' onclick='showGraph(this);' title='Show a Graph'>
//                          <input hidden class='button_imageB button_minusB' style='vertical-align:top' type='button' onclick='showGraph(this, 0.75);' title='Scale down'>
//                          <input hidden class='button_imageB button_plusB' style='vertical-align:top' type='button' onclick='showGraph(this, 1.33);' title='Scale up'>
//                          <input hidden class='button_image button_fullScreen' style='vertical-align:top' type='button' onclick='window.open(img_src);' title='Show the graph in full screen'>
//                          <br>
//                          <img hidden>
//                    </td>
//
// requires the sm.css styles for buttons.



		var img_src = '/dm/smGraph?img&id=_id_&query=';
		var img_map_src =  '/dm/smGraph?map&id=_id_&query=';
		
		// call this to set up a different template to be used.
		function setImg_src(link) {
		   img_src = '/dm/smGraph?img&' + link;
		   img_map_src = '/dm/smGraph?map&' + link;
		}
		
		function resetGraph(but) {
//		   var img = but.parentElement.children[5];
//		   img.outerHTML = "<img src=''>";
		   but.title='Show a Graph';
           but.className='button_imageB button_showGraph';
		}
		// If graph mode is "refresh", a "refresh icon and function will be used.
		var graphMode = "Hide a Graph";
		// shows a graph associated with this button but, 
		// and optionally zoom according to dSize, 
		// and if the optional cond is false, than unconditionally turn it on.
		// if cond is true, do not turn it on (from hidden, leave that property as is.
		function showGraph(but, dSize, cond) {
		   var img = but.parentElement.children[5];
		   var map = but.parentElement.children[6];
		   var showBut = but.parentElement.children[0];
		   var mBut = but.parentElement.children[1];
		   var pBut = but.parentElement.children[2];
		   var fullScreen = but.parentElement.children[3];
		   if (showBut != but) {
		      if (showBut.title == 'Show a Graph')
		         return;
		      var w = img.width;
		      if (w*dSize > 100) {
		         img.style.maxWidth = (w = Math.round(w*dSize)) + "px";
		         if (!img.style.maxWidth)
		            img.width = w;
		      }
		      var h = img.height;
		      if (h*dSize > 100) {
		         img.style.maxHeight = (h = Math.round(h*dSize)) + "px";
		         if (!img.style.maxHeight) 
		            img.height = h;
		      }   
		      return;
		   }
		   if (but.title == 'Show a Graph') {
		      if (!cond) {
		         mBut.hidden = false;
		         pBut.hidden = false;
		         fullScreen.hidden = false;
		         but.title='Hide a Graph';
		         but.className='button_imageB button_hideGraph';
  		         img.hidden=false;
		      }   
//		      if (!img.src)
		         img.src=img_src;
		         getContent(map, img_map_src);
		   } else {
		      mBut.hidden = true;
		      pBut.hidden = true;
		      fullScreen.hidden = true;
		      but.title='Show a Graph';
		      but.className='button_imageB button_showGraph';
		      img.hidden=true;
		   }
		}
		function moreLess(but) {
		   var table = but.parentElement.children[3];
		   if (but.value == 'More...') {
		      table.hidden=false;
		      but.value='...Less';
		   } else {
		      table.hidden=true;
		      but.value='More...';		      
		   }
		}
            
