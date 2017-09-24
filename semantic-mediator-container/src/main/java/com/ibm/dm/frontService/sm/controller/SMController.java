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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ibm.dm.frontService.sm.service.SmService;
import com.ibm.dm.frontService.sm.utils.IConstants;
import com.ibm.dm.frontService.sm.utils.Utils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@Api(value = "SMC-Platform-API", tags="API")
public class SMController extends SmcBaseController {

	public SMController() {
		super();
	}
	
/*	@RequestMapping("/*")
	@ResponseBody
	public String fallbackMethod(HttpServletRequest request) {
		return "<html><meta charset=\"utf-8\"/>SMController fall-back method for [" + request.getRequestURL() + " -- " + request.getQueryString() + "]</html>" ;
	}
*/	/**
	 * Local implementation of a parameter providing wrapper.
	 * 
	 * @author shani
	 *
	 */

	@RequestMapping(value="/sm", method = RequestMethod.GET)
	public void sm0(@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println( "called [/sm] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );

	//	log.info("Just passed using Log Manager from Apache");
		getDatabase(request);
//		response.getWriter().append(
//				"<html><body><h1>___Hi I am here<p>Host: " + Utils.getHost(null, true, true) + "</body></html>");
//
		new SmService(getDatabase(request)).get(request, response,
				new String[] { "sm" });
	}

	@RequestMapping(value = "/sm/{p1}", method = RequestMethod.GET)
	public void sm1(@PathVariable String p1,
	//		@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println( "called [/sm/{p1}] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
		new SmService(getDatabase(request)).get(request, response,
				new String[] { "sm", p1 });
	}

	@RequestMapping(value = "/sm/repository/{domain}", method = RequestMethod.GET, produces = { "text/turtle", "text/html" , "application/rdf+xml"})
	@ApiOperation(value = "OSLC Respository Access", notes = "Provides OSLC repository as RDF model")
    @ApiImplicitParams({
		@ApiImplicitParam(name = "domain", value = "domain name of a repository", required = true, dataType = "string", paramType = "path"),
		@ApiImplicitParam(name = "query", value = "a query token identifying a certain resource type, or a query varible.", required = false, 
							dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "id", value = "a helper information of the item id of the domain", required = false, 
							dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "version", value = "a helper token identifying a version of the domain", required = false, 
							dataType = "string", paramType = "query")
	})
	@ApiResponses(value = { 
            @ApiResponse(code = 200, message = "Success", response = String.class)})
	public void sm2(
				@PathVariable String domain,
				HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println( "called [/sm/repository/{domain}] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
		new SmService(getDatabase(request)).get(request, response,
				new String[] { "sm", "repository", domain });
	}

	
	@RequestMapping(value = "/sm/{p1}/{p2}", method = RequestMethod.GET)
	public void sm2(@PathVariable String p1, @PathVariable String p2,
	//		@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println( "called [/sm/{p1}/{p2}] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
		new SmService(getDatabase(request)).get(request, response,
				new String[] { "sm", p1, p2 });
	}

	@RequestMapping(value = "/sm/{p1}/{p2}/{p3}", method = RequestMethod.GET)
	public void sm3(@PathVariable String p1, @PathVariable String p2,
			@PathVariable String p3,
	//		@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println( "called [/sm/{p1}/{p2}/{p3}] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
		new SmService(getDatabase(request)).get(request, response,
				new String[] { "sm", p1, p2, p3 });
	}

	@RequestMapping(value = "/sm/{p1}/{p2}/{p3}/{p4}", method = RequestMethod.GET)
	public void sm4(@PathVariable String p1, @PathVariable String p2,
			@PathVariable String p3, @PathVariable String p4,
	//		@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println( "called [/sm/{p1}/{p2}/{p3}/{p4}] API [" + request.getRequestURL() + " -- " + request.getQueryString() + "]" );
		new SmService(getDatabase(request)).get(request, response,
				new String[] { "sm", p1, p2, p3, p4 });
	}


	@RequestMapping(value = "/sm/repository/attachments", method = RequestMethod.POST)
	@ApiOperation(value="POST an attachment", notes = "Submitting a new or updated attachment to the SMC plaform") 
    @ApiImplicitParams({
		@ApiImplicitParam(name = "set", value = "Type of data, may be any of SmBlob, SmFMI, SmXML, SmImage, with default or in case of illegal value - SmBlob", required = false, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "title", value = "The title property of the attachment to be assigned to it in the attachment repository content directory.", required = false, 
			dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "description", value = "The description property of the attachment to be assigned to it in the attachment repository content directory.", required = false, 
			dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "creator", value = "The creator of this attachment. This is a property of the attachment to be assigned to it in the attachment repository content directory.", required = false, 
			dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "contributor", value = "The contributor property of the attachment to be assigned to it in the attachment repository content directory.", required = false, 
			dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "hasSmResource", value = "Indicates whether this attachment is only modified of an existing one.", required = false, 
			dataType = "boolean", paramType = "query"),
	})
	@ApiResponses(value = { 
            @ApiResponse(code = 200, message = "Success", response = String.class)})
	public void postAttachment(@RequestParam("attachment-file") MultipartFile file,
		@RequestParam(value="set", required=false) String set,
		@RequestParam(value="title", required=false) String title,
		@RequestParam(value="description", required=false) String description,
		@RequestParam(value="creator", required=false) String creator,
		@RequestParam(value="contributor", required=false) String contributor,
		@RequestParam(value="hasSmResource", required=false) String hasSmResource,
		
	//		@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println("POSTing to sm/repository/attachments");
		new SmService(getDatabase(request)).post(request, response,
				new String[] { "sm", "repository", "attachments" },
				file,
				new JSONObject().put("set",  set).put("title",  title).put("description", description).put("creator", creator).put("contributor", contributor).put("hasSmResource", hasSmResource));
	}

	@RequestMapping(value = "/sm/{p1}/{p2}", method = RequestMethod.POST)
	public void post(@PathVariable String p1, @PathVariable String p2,
			@RequestParam("attachment-file") MultipartFile file,
			@RequestParam("set") String set,
			@RequestParam("title") String title,
			@RequestParam("description") String description,
	//		@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println("POSTing to sm/{p1}/{p2}");
		new SmService(getDatabase(request)).post(request, response,
				new String[] { "sm", p1, p2 },
				file,
				new JSONObject().put("set",  set).put("title",  title).put("description", description));
	}

	
	@RequestMapping(value = "/sm/repository/{id}/resource/{num}", method = RequestMethod.PUT,
			consumes = { IConstants.TURTLE, IConstants.RDF_XML, IConstants.RDF_XML + "-ABBREV", IConstants.N_TRIPLE, IConstants.N3 }, 
			produces = { IConstants.TURTLE, IConstants.RDF_XML, IConstants.RDF_XML + "-ABBREV", IConstants.N_TRIPLE, IConstants.N3, IConstants.PLAIN_TEXT_TYPE, IConstants.HTML })
    @ApiResponses(value = { 
            @ApiResponse(code = 200, message = "Success", response = String.class)})
	@ApiOperation(value="PUT a resource into an RDF repository", notes = "Submitting a new or updated resource to the SMC plaform")
    @ApiImplicitParams({
		@ApiImplicitParam(name = "id", value = "Repository ID of a repository into which a resource is being replaced.", 
				required = true, dataType = "string", paramType = "path"),
		@ApiImplicitParam(name = "num", value = "Each resource in an SMC RDF repository has a number as a veryh short identifier.", 
				required = true, dataType = "string", paramType = "path"),
		@ApiImplicitParam(name = "rdf", value = "The resource RDF structure according to its content type.", 
				required = true)
	})
	public void put(
		    @RequestBody(required= true) String rdf,
			@PathVariable String id, 
			@PathVariable String num, 
	//		@RequestParam() Map<String, String> params,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		new SmService(getDatabase(request)).put(rdf, request, response,
				new String[] { "sm", "repository", id, "resource", num });
	}


}
