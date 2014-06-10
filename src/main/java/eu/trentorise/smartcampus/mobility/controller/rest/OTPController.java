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

import it.sayservice.platform.client.InvocationException;
import it.sayservice.platform.smartplanner.data.message.otpbeans.GeolocalizedStopRequest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.mobility.util.ConnectorException;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.resourceprovider.controller.SCController;
import eu.trentorise.smartcampus.resourceprovider.model.AuthServices;

@Controller
public class OTPController extends SCController {

	@Autowired
	@Value("${otp.url}")
	private String otpURL;	
	
	public static final String OTP  = "/smart-planner/rest/";

	@Autowired
	private AuthServices services;
	@Override
	protected AuthServices getAuthServices() {
		return services;
	}

    private static ObjectMapper fullMapper = new ObjectMapper();
    static {
        fullMapper.setAnnotationIntrospector(NopAnnotationIntrospector.nopInstance());
        fullMapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);
        fullMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        fullMapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);

        fullMapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
        fullMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
    }

	
	@RequestMapping(method = RequestMethod.GET, value = "/getroutes/{agencyId}")
	public @ResponseBody
	void getRoutes(HttpServletResponse response, @PathVariable String agencyId) throws InvocationException{
		try {
			String address =  otpURL + OTP + "getroutes/" + agencyId;
			
			String routes = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(routes);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/getstops/{agencyId}/{routeId}")
	public @ResponseBody
	void getStops(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String routeId) throws InvocationException{
		try {
			String address =  otpURL + OTP + "getstops/" + agencyId + "/" + routeId;
			
			String stops = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(stops);
			
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
	@RequestMapping(method = RequestMethod.GET, value = "/getstops/{agencyId}/{routeId}/{latitude}/{longitude}/{radius:.+}")
	public @ResponseBody
	void getStops(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable double latitude, @PathVariable double longitude, @PathVariable double radius) throws InvocationException {
		try {
			String address =  otpURL + OTP + "getstops/" + agencyId + "/" + routeId + "/" + latitude + "/" + longitude + "/" + radius;
			
			String stops = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(stops);
			
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}			
	
	@RequestMapping(method = RequestMethod.GET, value = "/geostops/{agencyId}")
	public @ResponseBody
	List<Object> getGeolocalizedStops(
			HttpServletRequest request, 
			HttpServletResponse response, 
			@PathVariable String agencyId, 
			@RequestParam double lat, 
			@RequestParam double lng, 
			@RequestParam double radius,
			@RequestParam(required=false) Integer page,
			@RequestParam(required=false) Integer count) {
		try {
			String address =  otpURL + OTP + "getGeolocalizedStops";
			
			GeolocalizedStopRequest gsr = new GeolocalizedStopRequest();
			gsr.setAgencyId(agencyId);
			gsr.setCoordinates(new double[]{lat,lng});
			gsr.setRadius(radius);
			gsr.setPageSize(count == null ? 100 : count);
			gsr.setPageNumber(page == null ? 0 : page);
			ObjectMapper mapper = new ObjectMapper();
			String content = mapper.writeValueAsString(gsr);


			String res = HTTPConnector.doPost(address, content, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
			
			
			List result = mapper.readValue(res, List.class);
			
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}	
	}
	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettimetable/{agencyId}/{routeId}/{stopId:.*}")
	public @ResponseBody
	void getTimeTable(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable String stopId) throws InvocationException{
		try {
			String address =  otpURL + OTP + "gettimetable/" + agencyId + "/" + routeId + "/" + stopId;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, null);

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(timetable);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
	@RequestMapping(method = RequestMethod.GET, value = "/getlimitedtimetable/{agencyId}/{stopId}/{maxResults:.*}")
	public @ResponseBody
	void getLimitedTimeTable(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String stopId, @PathVariable Integer maxResults) throws InvocationException{
		try {
			String address =  otpURL + OTP + "getlimitedtimetable/" + agencyId + "/" + stopId + "/" + maxResults;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(timetable);
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}		
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettransittimes/{routeId}/{from}/{to}")
	public @ResponseBody
	void getTransitTimes(HttpServletResponse response, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
		try {
			String address =  otpURL + OTP + "getTransitTimes/" + routeId + "/" + from + "/" + to;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(timetable);
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}			
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettransitdelays/{routeId}/{from}/{to}")
	public @ResponseBody
	void getTransitDelays(HttpServletResponse response, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
		try {
			String address =  otpURL + OTP + "getTransitDelays/" + routeId + "/" + from + "/" + to;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			
			response.getWriter().write(timetable);
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
}
