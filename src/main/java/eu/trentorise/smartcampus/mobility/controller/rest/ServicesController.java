package eu.trentorise.smartcampus.mobility.controller.rest;

import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.service.NewAlertSender;

@Controller
public class ServicesController {
	
	@Autowired
	private NewAlertSender alertSender;	
	
	@RequestMapping(method = RequestMethod.GET, value = "servicedata/ping")
	public @ResponseBody void pushAlertParkings(HttpServletResponse response) throws Exception {
		System.out.println("OK");
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "servicedata/publishAlertParkings")
	public @ResponseBody void pushAlertParkings(HttpServletResponse response, @RequestBody List<Map> data) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		List<AlertParking> alerts = Lists.newArrayList();
		for (Map map: data) {
			AlertParking ap = mapper.convertValue(map, AlertParking.class);
			alerts.add(ap);
		}
		alertSender.publishParkings(alerts);
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "servicedata/publishAlertDelays")
	public @ResponseBody void pushAlertDelays(HttpServletResponse response, @RequestBody List<Map> data) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		List<AlertDelay> alerts = Lists.newArrayList();
		for (Map map: data) {
			AlertDelay ap = mapper.convertValue(map, AlertDelay.class);
			alerts.add(ap);
		}
		alertSender.publishTrains(null);
	}	
	
	
}
