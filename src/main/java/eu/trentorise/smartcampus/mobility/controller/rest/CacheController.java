/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.mobility.controller.rest;

import it.sayservice.platform.smartplanner.data.message.cache.CacheUpdateResponse;
import it.sayservice.platform.smartplanner.data.message.otpbeans.CompressedTransitTimeTable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CacheController {

  	@RequestMapping(method = RequestMethod.POST, value = "/cachestatus")
  	public @ResponseBody
  	Map<String, CacheUpdateResponse> cacheStatus(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody Map<String, String> versions) {
		try {
//			String address =  otpURL + OTP + "getCacheStatus";
//			
//			ObjectMapper mapper = new ObjectMapper();
//			String content = mapper.writeValueAsString(versions);
//			String res = HTTPConnector.doPost(address, content, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
//			
//			Map<String, CacheUpdateResponse> result = mapper.readValue(res, Map.class);
//			
//			return result;
			return  new HashMap<String, CacheUpdateResponse>();
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}
  	
	@SuppressWarnings("rawtypes")
	@RequestMapping(method = RequestMethod.POST, value = "/partialcachestatus")
	public @ResponseBody
	Map<String, CacheUpdateResponse> getPartialCacheStatus(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody Map<String, Map> versions) {
		try {
//			String address =  otpURL + OTP + "getPartialCacheStatus";
//			
//			ObjectMapper mapper = new ObjectMapper();
//			String content = mapper.writeValueAsString(versions);
//			String res = HTTPConnector.doPost(address, content, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
//			
//			Map<String, CacheUpdateResponse> result = mapper.readValue(res, Map.class);
//			
//			return result;
			return  new HashMap<String, CacheUpdateResponse>();

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}  	
  	

  	/**
  	 * @param request
  	 * @param response
  	 * @param session
  	 * @param versions
  	 */
  	@Deprecated
  	@RequestMapping(method = RequestMethod.POST, value = "/getcachestatus")
  	public @ResponseBody
  	Map<String, Map<String,Object>> getCacheStatus(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody Map<String, String> versions) {
		try {
			Map<String,Map<String,Object>> result = new HashMap<String, Map<String,Object>>();
			for (String agency : versions.keySet()) {
				Map<String,Object> map = new HashMap<String, Object>();
				map.put("version", Long.parseLong(versions.get(agency)));
				map.put("added", Collections.emptyList());
				map.put("removed", Collections.emptyList());
				map.put("calendar", null);
				result.put(agency, map);
			}
			return result;
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}
  	
  	@RequestMapping(method = RequestMethod.GET, value = "/getcacheupdate/{agencyId}/{fileName}")
  	public @ResponseBody
  	CompressedTransitTimeTable getCacheUpdate(HttpServletRequest request, HttpServletResponse response, HttpSession session,  @PathVariable String agencyId,  @PathVariable String fileName) {
  		try {
//			String address =  otpURL + OTP + "getCacheUpdate/" + agencyId + "/" + fileName;
//			
//			String res = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
//			
//			ObjectMapper mapper = new ObjectMapper();
//			CompressedTransitTimeTable result = mapper.readValue(res, CompressedTransitTimeTable.class);
//			
//			return result;

  			return null;
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}  	
  	
		
}