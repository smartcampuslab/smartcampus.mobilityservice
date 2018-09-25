package eu.trentorise.smartcampus.mobility.controller.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;

@Controller
public class TestController {

	private static final String NOTIFICATION_APP = "mobility.trentoplaygo.test";
	
	@Autowired
	private NotificationHelper notificatioHelper;	
	
	private static Log logger = LogFactory.getLog(TestController.class);
	
	@RequestMapping(method = RequestMethod.GET, value = "/test/notification")
	public @ResponseBody void notification(@RequestParam(required = false) String id, @RequestParam(required = false) String text) throws Exception {
		Notification notification = new Notification();
		notification.setTitle("Test notification");
		if (text != null) {
			notification.setDescription(text);
		} else {
			notification.setDescription("...");
		}
		
		notificatioHelper.notify(notification, (id == null ? "8" : id), NOTIFICATION_APP);
		
	}	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/test/broadcast")
	public @ResponseBody void broadcast(@RequestParam(required = false) String text) throws Exception {
		Notification notification = new Notification();
		notification.setTitle("Test broadcast");
		if (text != null) {
			notification.setDescription(text);
		} else {
			notification.setDescription("...");
		}
		
		notificatioHelper.notify(notification, NOTIFICATION_APP);
		
	}	
	
		
}