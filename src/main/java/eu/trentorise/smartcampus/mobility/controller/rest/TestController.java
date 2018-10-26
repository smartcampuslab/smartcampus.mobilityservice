package eu.trentorise.smartcampus.mobility.controller.rest;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.mobility.gamification.model.LevelGainedNotification;
import eu.trentorise.smartcampus.mobility.gamificationweb.NotificationsManager;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;

@Controller
public class TestController {

//	private static final String NOTIFICATION_APP = "mobility.trentoplaygo.test";
	
	@Autowired
	private NotificationsManager notificatioManager;		
	
	@Autowired
	private NotificationHelper notificatioHelper;	
	
	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;
	
	private static Log logger = LogFactory.getLog(TestController.class);
	
	@RequestMapping(method = RequestMethod.GET, value = "/test/notification")
	public @ResponseBody void notification(@RequestParam(required = true) String id, @RequestParam(required = false) String title, @RequestParam(required = false) String description, @RequestParam(required = false) String type, @RequestParam(required = true) String notificationAppId) throws Exception {
		Notification notification = notificatioManager.buildSimpleNotification("it", LevelGainedNotification.class.getSimpleName());

		if (title != null) {
			notification.setTitle(title);
		}
		if (description != null) {
			notification.setDescription(description);
		}
		if (type != null) {
			Map<String, Object> content = Maps.newTreeMap();
			content.put("type", type);
			notification.setContent(content);
		}
		
		notificatioHelper.notify(notification, id, notificationAppId);
	}	
	
	
//	@RequestMapping(method = RequestMethod.GET, value = "/test/broadcast")
//	public @ResponseBody void broadcast(@RequestParam(required = false) String title, @RequestParam(required = false) String description) throws Exception {
//		Notification notification = new Notification();
//		if (title != null) {
//			notification.setTitle(title);	
//		} else {
//			notification.setTitle("Test broadcast");
//		}
//		if (description != null) {
//			notification.setDescription(description);
//		} else {
//			notification.setDescription("...");
//		}
//		
//		notificatioHelper.notify(notification, NOTIFICATION_APP);
//		
//	}	
	
}