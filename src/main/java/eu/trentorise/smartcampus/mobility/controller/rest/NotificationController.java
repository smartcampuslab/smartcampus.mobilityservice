package eu.trentorise.smartcampus.mobility.controller.rest;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertNotifier;
import eu.trentorise.smartcampus.mobility.security.AppDetails;

@Controller
@RequestMapping(value = "/web/notification")
public class NotificationController {

	@Autowired
	private AlertNotifier notifier;		
	
	@RequestMapping(method = RequestMethod.GET)
	public String notify(HttpSession session) {
		return "notification";
	}	

	@RequestMapping(method = RequestMethod.POST, value = "notify")
	public @ResponseBody Map<String, String> notify(@RequestBody Announcement announcement, HttpServletResponse response) throws Exception {
		Map<String, String> result = Maps.newTreeMap();

		try {

			String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getMessagingAppId();
			
			notifier.notifyAnnouncement(announcement, appId);

			result.put("message", "Message \"" + announcement.getTitle() + "\" sent @ " + new Date());
		} catch (Exception e) {
			result.put("error", "Exception @ " + new Date() + ": " + e.toString());
		}

		return result;
	}
	
}