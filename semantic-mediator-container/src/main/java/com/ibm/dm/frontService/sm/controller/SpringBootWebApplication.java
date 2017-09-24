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

package com.ibm.dm.frontService.sm.controller;

import static springfox.documentation.builders.PathSelectors.regex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.annotations.*;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
//import springfox.documentation.swagger2.mappers.LicenseMapper.License;

@SpringBootApplication
@EnableSwagger2
@ComponentScan("com.ibm.dm.frontService.sm.controller")
/*@SwaggerDefinition(
        info = @Info(
                //description = "Gets the weather",
                version = "3.0",
        		    title = "Semantic Mediation Container API",
                //termsOfService = "http://theweatherapi.io/terms.html",
                contact = @Contact(
                   name = "Uri Shani", 
                   email = "uri.shani@gmail.com", 
                   url = "https://github.com/urishani/SMC"
                ),
                license = @License(
                   name = "Apache 2.0", 
                   url = "http://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        //consumes = {"application/json", "application/xml"},
        //produces = {"application/json", "application/xml"},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        tags = {
                @Tag(name = "oslcApi", description = "OSLC service APIs"),
                @Tag(name = "smcApi", description = "Semantic Mediation Container management APIs"),
                @Tag(name = "mediationApi", description = "Mediation service APIs")
        } 
        //externalDocs = @ExternalDocs(value = "Meteorology", url = "http://theweatherapi.io/meteorology.html")
)
*/
public class SpringBootWebApplication extends SpringBootServletInitializer {
	 @Override
	    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
	        return application.sources(SpringBootWebApplication.class);
	    }
    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebApplication.class, args);
    }
        
    @Bean
    public Docket newsApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("SMC")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(regex("/smApi.*|/sm/oslc_am.*|/sm/repository.*"))
                .build();
    }
     
	private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Semantic Mediation Container API")
                .description("Semantic Mediation Container API to perform semantic mediation services, and to work with the platform via OSLC calls")
                //.termsOfServiceUrl("http://www-03.ibm.com/software/sla/sladb.nsf/sla/bm?Open")
                .contact("Uri Shani")
                .license("Apache License Version 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .version("3.0")
                .build();
	}

}
