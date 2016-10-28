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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.io.ByteStreams;

import eu.trentorise.smartcampus.mobility.model.Timetable;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerHelper;
import eu.trentorise.smartcampus.mobility.util.ConnectorException;
import eu.trentorise.smartcampus.network.JsonUtils;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Stop;
import it.sayservice.platform.smartplanner.data.message.otpbeans.TransitTimeTable;

@Controller
public class OTPController {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private SmartPlannerHelper smartPlannerHelper;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final int DAY = 1000*60*60*24-1;
	
	@RequestMapping(method = RequestMethod.GET, value = "/getroutes/{agencyId}")
	public @ResponseBody
	void getRoutes(HttpServletResponse response, @PathVariable String agencyId) {
		try {
			//String address =  otpURL + OTP + "getroutes/" + agencyId;
			//String routes = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			String routes = smartPlannerHelper.routes(agencyId);

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
	void getStops(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String routeId) {
		try {
//			String address =  otpURL + OTP + "getstops/" + agencyId + "/" + routeId;
//			String stops = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			String stops = smartPlannerHelper.stops(agencyId, routeId);
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
	void getStops(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable double latitude, @PathVariable double longitude, @PathVariable double radius)  {
		try {
//			String address =  otpURL + OTP + "getstops/" + agencyId + "/" + routeId + "/" + latitude + "/" + longitude + "/" + radius;
//			String stops = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
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
	void getTimeTable(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable String stopId) {
		try {
//			String address =  otpURL + OTP + "gettimetable/" + agencyId + "/" + routeId + "/" + stopId;
//			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, null);
			String timetable = smartPlannerHelper.stopTimetable(agencyId, routeId, stopId);
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
	void getLimitedTimeTable(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String stopId, @PathVariable Integer maxResults) {
		try {
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
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettransittimes/{agencyId}/{routeId}/{from}/{to}")
	public @ResponseBody
	void getTransitTimes(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
		try {
//			String address =  otpURL + OTP + "getTransitTimes/" + routeId + "/" + from + "/" + to;
//			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");
			String timetable = smartPlannerHelper.transitTimes(agencyId, routeId, from, to);
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(timetable);
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}		
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettransittimes/{agencyId}/{routeId}/{from}/{to}/extended")
	public @ResponseBody
	void getExtendedTransitTimes(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
		try {
//			String address =  otpURL + OTP + "getTransitTimes/" + routeId + "/" + from + "/" + to;
//			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");
			String timetable = smartPlannerHelper.extendedTransitTimes(agencyId, routeId, from, to);
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(timetable);
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/timetable/{agencyId}/{routeId}")
	public @ResponseBody
	void getTodayTransitTimes(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String routeId)  {
		try {
			
			long from = DATE_FORMAT.parse(DATE_FORMAT.format(new Date())).getTime();
			String timetable = smartPlannerHelper.transitTimes(agencyId, routeId, from, from+DAY);
			TransitTimeTable ttt = JsonUtils.toObject(timetable, TransitTimeTable.class);
			Timetable tt = Timetable.fromTransitTimeTable(ttt);
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(JsonUtils.toJSON(tt));
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}			
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettransitdelays/{agencyId}/{routeId}/{from}/{to}")
	public @ResponseBody
	void getTransitDelays(HttpServletResponse response, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
		try {
			String timetable = smartPlannerHelper.delays(agencyId, routeId, from, to);
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
		void getParkingsByAgency(HttpServletResponse response, @PathVariable String agencyId)  {
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

	@RequestMapping(method = RequestMethod.GET, value = "/getbikesharingbyagency/{agencyId}")
		public @ResponseBody
		void getBikeSharingByAgency(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId)  {
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
		void getRoadInfoByAgency(HttpServletResponse response, @PathVariable String agencyId, @PathVariable Long from, @PathVariable Long to)  {
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

	@RequestMapping(method = RequestMethod.GET, value = "/getTaxiStation/{latitude}/{longitude}/{radius}")
	public @ResponseBody void getTaxiStations(HttpServletRequest request, HttpServletResponse response,
			HttpSession session, @PathVariable double latitude, @PathVariable double longitude,
			@PathVariable double radius)  {
		try {

			String stops = smartPlannerHelper.getTaxiStations(latitude, longitude, radius);

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(stops);

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/getTaxiStation/")
	public @ResponseBody void getAllTaxiStations(HttpServletRequest request, HttpServletResponse response,
			HttpSession session)  {
		try {

			String stops = smartPlannerHelper.getAllTaxiStations();

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(stops);

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/getTaxiAgencyContacts/")
	public @ResponseBody void getTaxiAgencyContacts(HttpServletRequest request, HttpServletResponse response,
			HttpSession session)  {
		try {

			String contacts = smartPlannerHelper.getTaxiAgencyContacts();

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(contacts);

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
		
  	@RequestMapping(method = RequestMethod.GET, value = "/gtfs/{agencyId}", produces = "application/zip")
  	public @ResponseBody
  	void getRoutesDB(HttpServletRequest request, HttpServletResponse response, HttpSession session,  @PathVariable String agencyId) {
  		try {
  			response.setContentType("application/zip");
			response.setHeader("Content-Disposition", "attachment; filename=\"gtfs_" + agencyId + ".zip\""); 
			
			InputStream is = smartPlannerHelper.gtfs(agencyId);
			
			ByteStreams.copy(is, response.getOutputStream());
  		} catch (ConnectorException e0) {
  			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}   	
	
	protected String getUserId() {
			return null;
	}

}
