package eu.trentorise.smartcampus.mobility.controller.rest;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.model.Station;
import eu.trentorise.smartcampus.mobility.processor.handlers.BikeSharingCache;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@ApiIgnore
public class ServicesController {
	
	@Autowired
	private AlertSender alertSender;	
	
	@Autowired
	private BikeSharingCache bikeSharingCache;
	
	@RequestMapping(method = RequestMethod.POST, value = "servicedata/publishAlertParkings")
	public @ResponseBody void pushAlertParkings(HttpServletResponse response, @RequestBody(required=false) List<Map> data) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		List<AlertParking> alerts = Lists.newArrayList();
		for (Map map: data) {
			AlertParking ap = mapper.convertValue(map, AlertParking.class);
			alerts.add(ap);
		}
		alertSender.publishParkings(alerts);
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "servicedata/publishAlertDelays")
	public @ResponseBody void pushAlertDelays(HttpServletResponse response, @RequestBody(required=false) List<Map> data) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		List<AlertDelay> alerts = Lists.newArrayList();
		for (Map map: data) {
			AlertDelay ap = mapper.convertValue(map, AlertDelay.class);
			alerts.add(ap);
		}
        alertSender.publishTrains(alerts);
    }    
    
    @RequestMapping(method = RequestMethod.POST, value = "servicedata/publishAlertRoads")
    public @ResponseBody void pushAlertRoads(HttpServletResponse response, @RequestBody(required=false) List<Map> data) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<AlertRoad> alerts = Lists.newArrayList();
        for (Map map: data) {
            AlertRoad ar = mapper.convertValue(map, AlertRoad.class);
            alerts.add(ar);
        }
        alertSender.publishRoadWorkAlerts(alerts);
    }    

    @RequestMapping(method = RequestMethod.POST, value = "servicedata/publishBikeStations/{comune}/{agencyId}")
    public @ResponseBody void publishBikeStations(HttpServletResponse response, @RequestBody(required=false) List<Map> data, @PathVariable String comune, @PathVariable String agencyId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Station> stations = Lists.newArrayList();
        for (Map map: data) {
        	Station s = mapper.convertValue(map, Station.class);
            stations.add(s);
        }
        bikeSharingCache.setStations(comune, agencyId, stations);
    } 	
    
    
	
}
