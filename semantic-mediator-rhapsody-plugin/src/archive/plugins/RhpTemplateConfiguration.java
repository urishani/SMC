
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
package com.ibm.rhapsody.plugins;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class RhpTemplateConfiguration extends Configuration {
	public RhpTemplateConfiguration() {
        // - Templates are stoted in the WEB-INF/templates directory of the Web app.
        //this.setServletContextForTemplateLoading(
        //        getServletContext(), "WEB-INF/templates");
        // - Set update dealy to 0 for now, to ease debugging and testing.
        //   Higher value should be used in production environment.
        this.setTemplateUpdateDelay(0);
        // - Set an error handler that prints errors so they are readable with
        //   a HTML browser.
        this.setTemplateExceptionHandler(
                TemplateExceptionHandler.DEBUG_HANDLER);
        // - Use beans wrapper (recommmended for most applications)
        this.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
        // - Set the default charset of the template files
        this.setDefaultEncoding("ISO-8859-1");
        // - Set the charset of the output. This is actually just a hint, that
        //   templates may require for URL encoding and for generating META element
        //   that uses http-equiv="Content-type".
        this.setOutputEncoding("UTF-8");
        // - Set the default locale
        this.setLocale(Locale.US);
        
        // Set template loader from the project resources
		this.setTemplateLoader(new ClassTemplateLoader(getClass(), "/"));
	}
	
	public void applyTemplate(Object model, FileWriter out, String templatePrefix, String ext) throws IOException, TemplateException {
		applyTemplate(model, out, templatePrefix + ext);
	}

	//TODO Sergey - provide this class.
	interface NameModifier {public String alter(String typeName);};
	public void applyTemplate(Object model, Writer out, String templateName) throws IOException, TemplateException {
		Map<String, Object> root = new HashMap<String, Object>();
		root.put("model", model);
		root.put("typeMod", new NameModifier() {
			public String alter(String typeName) {
				if("RhpInteger".equals(typeName))
					return "int";
				if("RhpReal".equals(typeName))
					return "float";
				if("RhpBoolean".equals(typeName))
					return "boolean"; // string???
				if("RhpString".equals(typeName))
					return "string";
				return typeName;
			}});

		Template t = this.getTemplate(templateName);
		t.process(root, out);
	}
}
