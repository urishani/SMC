
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
 *| File Name: interceptor.js                                                    |
 *+------------------------------------------------------------------------+
 */
 
 // PErforms activities in a page where an interceptor is selected by the user.
            var registration = {};
            var licenseText = {};
            function register(id, flag, desc) {
                registration[id] = flag;
                licenseText[id] = desc;
            }
    
            function confirmLicense() {
                var interceptorNameSelector = document.getElementById('interceptor');
                var id = interceptorNameSelector.value;
                var flag = registration[id];
                var requiresLicense = (flag == true);
                if (requiresLicense) {
                    var text = "Need to confirm licensed use of this mediator.\n";
                    if (licenseText[id])
                        text += licenseText[id] + "\n";
                    text += "Please confirm:";
                    var confirmed = confirm(text);
                    if (confirmed)
                        return true;
                    interceptorNameSelector.value = 'NaN';
                    return false;
                } else
                    return true;
            }
