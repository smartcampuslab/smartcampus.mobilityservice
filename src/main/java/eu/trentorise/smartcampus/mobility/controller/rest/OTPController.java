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
import it.sayservice.platform.smartplanner.data.message.otpbeans.Stop;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.mobility.processor.handlers.BikeSharingHandler;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerHelper;
import eu.trentorise.smartcampus.mobility.util.ConnectorException;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.resourceprovider.controller.SCController;
import eu.trentorise.smartcampus.resourceprovider.model.AuthServices;

@Controller
public class OTPController extends SCController {

	private Logger logger = Logger.getLogger(this.getClass());

	@Autowired
	private SmartPlannerHelper smartPlannerHelper;
	@Autowired
	private BikeSharingHandler bikeSharingCache;

	@Autowired
	private AuthServices services;
	@Override
	protected AuthServices getAuthServices() {
		return services;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/getroutes/{agencyId}")
	public @ResponseBody
	void getRoutes(HttpServletResponse response, @PathVariable String agencyId) throws InvocationException{
		try {
			//String address =  otpURL + OTP + "getroutes/" + agencyId;
			//String routes = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			String routes = smartPlannerHelper.routes(agencyId);

			logger.info("-"+getUserId()  + "~AppConsume~routes=" + agencyId);
	
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
//			String address =  otpURL + OTP + "getstops/" + agencyId + "/" + routeId;
//			String stops = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			String stops = smartPlannerHelper.stops(agencyId, routeId);
			logger.info("-"+getUserId()  + "~AppConsume~stops=" + agencyId);

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
//			String address =  otpURL + OTP + "getstops/" + agencyId + "/" + routeId + "/" + latitude + "/" + longitude + "/" + radius;
//			String stops = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			logger.info("-"+getUserId()  + "~AppConsume~stops=" + agencyId);
			String stops = smartPlannerHelper.stops(agencyId, routeId, latitude, longitude, radius);

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
	List<Stop> getGeolocalizedStops(
			HttpServletRequest request, 
			HttpServletResponse response, 
			@PathVariable String agencyId, 
			@RequestParam double lat, 
			@RequestParam double lng, 
			@RequestParam double radius,
			@RequestParam(required=false) Integer page,
			@RequestParam(required=false) Integer count) {
		try {

//			String address =  otpURL + OTP + "getGeolocalizedStops";
//			String res = HTTPConnector.doPost(address, content, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
			return smartPlannerHelper.stops(agencyId, lat, lng, radius, page, count);
//			String res2 = new String(res.getBytes(), Charset.forName("UTF-8"));
//			List result = mapper.readValue(res2, List.class);
//			return result;

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
//			String address =  otpURL + OTP + "gettimetable/" + agencyId + "/" + routeId + "/" + stopId;
//			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, null);
			String timetable = smartPlannerHelper.stopTimetable(agencyId, routeId, stopId);
			logger.info("-"+getUserId()  + "~AppConsume~timetable=" + agencyId);

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
			logger.info("-"+getUserId()  + "~AppConsume~timetable=" + agencyId);
//			String address =  otpURL + OTP + "getlimitedtimetable/" + agencyId + "/" + stopId + "/" + maxResults;
//			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			String timetable = smartPlannerHelper.stopTimetable(agencyId, stopId, maxResults);
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
//			String address =  otpURL + OTP + "getTransitTimes/" + routeId + "/" + from + "/" + to;
//			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");
			String timetable = smartPlannerHelper.transitTimes(routeId, from, to);
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
			logger.info("-"+getUserId()  + "~AppConsume~delays=" + routeId);
//			String address =  otpURL + OTP + "getTransitDelays/" + routeId + "/" + from + "/" + to;
//			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");
			String timetable = smartPlannerHelper.delays(routeId, from, to);
			response.setContentType("application/json; charset=utf-8");
			
			response.getWriter().write(timetable);
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	// /////////////////////////////////////////////////////////////////////////////
	
		@RequestMapping(method = RequestMethod.GET, value = "/getparkingsbyagency/{agencyId}")
		public @ResponseBody
		void getParkingsByAgency(HttpServletResponse response, @PathVariable String agencyId) throws InvocationException {
			try {
	//			String address = otpURL + SMARTPLANNER + "getParkingsByAgency?agencyId=" + agencyId;
	//			HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
	
				String routes = smartPlannerHelper.parkingsByAgency(agencyId);  
	
				response.setContentType("application/json; charset=utf-8");
				response.getWriter().write(routes);
	
			} catch (ConnectorException e0) {
				response.setStatus(e0.getCode());
			} catch (Exception e) {
				e.printStackTrace();
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}

	@RequestMapping(method = RequestMethod.GET, value = "/bikesharing/{comune}")
	public @ResponseBody
	void bikeSharingByComune(HttpServletResponse response, @PathVariable String comune) throws InvocationException {
		response.setContentType("application/json; charset=utf-8");
		try {
			response.getWriter().write(JsonUtils.toJSON(bikeSharingCache.getStations(comune)));
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/getbikesharingbyagency/{agencyId}")
		public @ResponseBody
		void getBikeSharingByAgency(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId) throws InvocationException {
			try {
	//			String address = otpURL + SMARTPLANNER + "getBikeSharingByAgency?agencyId=" + agencyId;
	//			HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
	
				String routes = smartPlannerHelper.bikeSharingByAgency(agencyId); 
	
				response.setContentType("application/json; charset=utf-8");
				response.getWriter().write(routes);
	
			} catch (ConnectorException e0) {
				response.setStatus(e0.getCode());
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}

	@RequestMapping(method = RequestMethod.GET, value = "/getroadinfobyagency/{agencyId}/{from}/{to}")
		public @ResponseBody
		void getRoadInfoByAgency(HttpServletResponse response, @PathVariable String agencyId, @PathVariable Long from, @PathVariable Long to) throws InvocationException {
			try {
	//			String address = otpURL + SMARTPLANNER + "getAR?agencyId=" + agencyId + "&from=" + from + "&to=" + to;
	//			HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
	
				String roadInfo = smartPlannerHelper.roadInfoByAgency(agencyId, from, to); 
	
				response.setContentType("application/json; charset=utf-8");
				response.getWriter().write(roadInfo);
	
			} catch (ConnectorException e0) {
				response.setStatus(e0.getCode());
			} catch (Exception e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}	

	@Override
	protected String getUserId() {
		try {
			return super.getUserId();
		} catch (Exception e) {
			return null;
		}
	}

}
