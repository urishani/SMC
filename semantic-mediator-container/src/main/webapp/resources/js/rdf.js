
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
 *| File name: rdf.js                                                      |
 *+------------------------------------------------------------------------+
 */
// Requires utils.js


		var _id_;
		var _contentType_;
		var _host_;
        function onLoad(idP, contentTypeP, hostP) {
            _host_ = hostP;
            _id_ = idP;
            if (!_host_ || _host_ == "_host_") _host_ = "";
            _contentType_ = contentTypeP;
            if (! _id_ || _id_ == '_id_') {
                x = document.getElementById('listButton');
                x.hidden = 'true';
            }
            if (! _contentType_ || _contentType_ == "_contentType_")  
               _contentType_ = 'text/turtle';
            var cte = document.getElementById('contentType');
            if (cte)
               cte.value = _contentType_;
               
            //if ('_isResource_' == 'true')  -- remove the download button altogether now.
            {
                x = document.getElementById('downloadButton');
                //      x.hidden='true';
            }
            rdfE = document.getElementById('text');
            rdf = rdfE.value;
            rdf = rdf.split('\n');
            var props = setProps4contentType(_contentType_);
            var ancor = "https://";
            if (_host_ != "_host_")
               ancor = _host_;
            for ( i = 0; i < rdf.length; i++) {
                rdf[i] = makeLink(rdf[i].replace(/\ /g, '&nbsp;').replace(/\</g, '&lt;').replace(/\>/g, '&gt;'), props.ma, props.mb, ancor);
            }
            //alert("rdf [" + rdf + "]");
            rdfE.hidden = 'true';
            div = document.getElementById('textDiv');
            div.innerHTML = rdf.join('<br>');
        }
        
        function setContentType(sw) {
           var ct = sw.value;
           if (!ct || ct == _contentType_)
             return false;
           return true;
        }
