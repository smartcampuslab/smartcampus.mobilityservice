package eu.trentorise.smartcampus.mobility.controller.rest;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.gamification.model.ItineraryDescriptor;
import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.Activity;
import eu.trentorise.smartcampus.mobility.geolocation.model.Battery;
import eu.trentorise.smartcampus.mobility.geolocation.model.Coords;
import eu.trentorise.smartcampus.mobility.geolocation.model.Device;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.GeolocationsEvent;
import eu.trentorise.smartcampus.mobility.geolocation.model.Location;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.resourceprovider.controller.SCController;
import eu.trentorise.smartcampus.resourceprovider.model.AuthServices;

@Controller
@RequestMapping(value = "/gamification")
public class GamificationController extends SCController {

	 @Autowired
	 private DomainStorage storage;

	@Autowired
	private AuthServices services;
	
	@Autowired
	@Value("${geolocations.db.dir}")
	private String geolocationsDBDir;	
	
	@Autowired
	@Value("${geolocations.db}")
	private String geolocationsDB;	
	
	@Autowired
	@Value("${gamification.gameId}")
	private String gameId;	
	
	@Autowired
	@Value("${aacURL}")
	private String aacURL;	
	
	private BasicProfileService basicProfileService;
	
	@Autowired
	private GamificationHelper gamificationHelper;

	private static Log logger = LogFactory.getLog(GamificationController.class);
	
	private Connection connection;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
	private SimpleDateFormat shortSdf = new SimpleDateFormat("YYYY/MM/dd");

	private final static String CREATE_DB = "CREATE TABLE IF NOT EXISTS geolocations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT, device_id TEXT, device_model TEXT, latitude REAL,  longitude REAL, accuracy INTEGER, altitude REAL, speed REAL, heading REAL, activity_type TEXT, activity_confidence INTEGER, battery_level REAL, battery_is_charging BOOLEAN, is_moving BOOLEAN, geofence TEXT, recorded_at DATETIME, created_at DATETIME, userId TEXT, travelId TEXT)";

	@PostConstruct
	public void init() throws Exception {
		basicProfileService = new BasicProfileService(aacURL);
		
		File f = new File(geolocationsDBDir);
		if (!f.exists()) {
			f.mkdir();
		}

		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:" + geolocationsDBDir + "/" + geolocationsDB);

		Statement statement = connection.createStatement();
		statement.setQueryTimeout(30);
		statement.executeUpdate(CREATE_DB);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/geolocations")
	public @ResponseBody String storeGeolocationEvent(@RequestBody GeolocationsEvent geolocationsEvent, @RequestParam String token, HttpServletResponse response) throws Exception {
		logger.info("Receiving geolocation events, token = "+token+", "+ geolocationsEvent.getLocation().size() +" events");
		ObjectMapper mapper = new ObjectMapper();
//		logger.info(mapper.writeValueAsString(geolocationsEvent));
		try {
			String userId = null;
			try {
				userId = basicProfileService.getBasicProfile(token).getUserId();
			} catch (SecurityException e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return "";
			}

			logger.info("UserId: " + userId);

//			Geolocation lastGeolocation = storage.getLastGeolocationByUserId(userId);
			String lastTravelId = null;
//			if (lastGeolocation != null) {
//				lastTravelId = lastGeolocation.getTravelId();
//			}

			Multimap<String, Geolocation> geolocationsByItinerary = ArrayListMultimap.create();

			Collections.sort(geolocationsEvent.getLocation());
			
			if (geolocationsEvent.getLocation() != null) {
				for (Location location : geolocationsEvent.getLocation()) {
					String locationTravelId = null;
					if (location.getExtras() != null && location.getExtras().containsKey("idTrip")) {
						locationTravelId = (String) location.getExtras().get("idTrip");
						lastTravelId = locationTravelId;
					} else {
						// now the plugin supports correctly the extras for each location.
						// locations with empty idTrip are possible only upon initialization/synchronization.
						// we skip them here
						logger.info("location without idTrip, user: "+userId);
						continue;
//						if (lastTravelId != null) {
//							locationTravelId = lastTravelId;
//						} else {
//							continue;
//						}
					}
					
					Coords coords = location.getCoords();
					Device device = geolocationsEvent.getDevice();
					Activity activity = location.getActivity();
					Battery battery = location.getBattery();

					Geolocation geolocation = new Geolocation();

					geolocation.setUserId(userId);



					geolocation.setTravelId(locationTravelId);

					geolocation.setUuid(location.getUuid());
					if (device != null) {
						geolocation.setDevice_id(device.getUuid());
						geolocation.setDevice_model(device.getModel());
					} else {
						geolocation.setDevice_model("UNKNOWN");
					}
					if (coords != null) {
						geolocation.setLatitude(coords.getLatitude());
						geolocation.setLongitude(coords.getLongitude());
						double c[] = new double[2];
						c[0] = geolocation.getLongitude();
						c[1] = geolocation.getLatitude();
						geolocation.setGeocoding(c);
						geolocation.setAccuracy(coords.getAccuracy());
						geolocation.setAltitude(coords.getAltitude());
						geolocation.setSpeed(coords.getSpeed());
						geolocation.setHeading(coords.getHeading());
					}
					if (activity != null) {
						geolocation.setActivity_type(activity.getType());
						geolocation.setActivity_confidence(activity.getConfidence());
					}
					if (battery != null) {
						geolocation.setBattery_level(battery.getLevel());
						geolocation.setBattery_is_charging(battery.getIs_charging());
					}

					geolocation.setIs_moving(location.getIs_moving());

					geolocation.setRecorded_at(new Date(location.getTimestamp().getTime()));
					geolocation.setCreated_at(new Date(System.currentTimeMillis()));

					if (location.getGeofence() != null) {
						geolocation.setGeofence(mapper.writeValueAsString(location.getGeofence()));

					}

					Statement statement = connection.createStatement();
					String s = buildInsert(geolocation);
					statement.execute(s);
					statement.close();

					geolocation.setGeofence(location.getGeofence());

					String day = shortSdf.format(geolocation.getRecorded_at());
					geolocationsByItinerary.put(geolocation.getTravelId() + "@" + day, geolocation);

					storage.saveGeolocation(geolocation);
				}
			}

			for (String key : geolocationsByItinerary.keySet()) {

				String splitKey[] = key.split("@");
				String travelId = splitKey[0];
				String day = splitKey[1];

				Map<String, Object> pars = new TreeMap<String, Object>();
				pars.put("clientId", travelId);
				pars.put("day", day);
				TrackedInstance res = storage.searchDomainObject(pars, TrackedInstance.class);
				if (res == null) {
					res = new TrackedInstance();
					res.setClientId(travelId);
					res.setDay(day);
					res.setUserId(userId);
					pars.remove("day");
					ItineraryObject res2 = storage.searchDomainObject(pars, ItineraryObject.class);
					if (res2 == null) {
						pars = new TreeMap<String, Object>();
						pars.put("itinerary.clientId", travelId);
						SavedTrip res3 = storage.searchDomainObject(pars, SavedTrip.class);
						if (res3 != null) {
							res.setItinerary(res3.getItinerary());
						}
					} else {
						res.setItinerary(res2);
					}
				}

				for (Geolocation geoloc : geolocationsByItinerary.get(key)) {
					res.getGeolocationEvents().add(geoloc);
				}

				if (res.getItinerary() != null) {
					if (!res.getStarted() && !res.getComplete()) {
						sendIntineraryDataToGamificationEngine(gameId, userId, res.getItinerary());
					}

					res.setComplete(true);
					ValidationResult vr = GamificationHelper.checkItineraryMatching(res.getItinerary(), res.getGeolocationEvents());
					res.setValidationResult(vr);
					res.setValid(vr.getValid());
				}

				storage.saveTrackedInstance(res);
				
				logger.info("Saved geolocation events");
			}

		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "";
		}
		return "{\"storeResult\":\"OK\"}";
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/geolocations")
	public @ResponseBody List<Geolocation> searchGeolocationEvent(@RequestParam  Map<String, Object> query, HttpServletResponse response) throws Exception {
		
		Criteria criteria = new Criteria();
		for (String key: query.keySet()) {
			criteria = criteria.and(key).is(query.get(key));
		}
		
		Query mongoQuery = new Query(criteria).with(new Sort(Sort.Direction.DESC, "created_at"));;
		
		return storage.searchDomainObjects(mongoQuery, Geolocation.class);
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/journey/{itineraryId}")
	public @ResponseBody void startItinerary(@RequestBody String device, @PathVariable String itineraryId, HttpServletResponse response) throws Exception {
		logger.info("Starting journey for gamification, device = "+device);
		try {
			String userId = getUserId();
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();

			pars.put("clientId", itineraryId);
			ItineraryObject res = storage.searchDomainObject(pars, ItineraryObject.class);
			if (res != null && !userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				logger.info("Unauthorized.");
				return;
			}
			if (res == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				logger.info("Bad request.");
				return;
			}
			
			Date date = new Date(System.currentTimeMillis());
			String day = shortSdf.format(date);
			pars.put("day", day);
			TrackedInstance res2 = storage.searchDomainObject(pars, TrackedInstance.class);
			if (res2 == null) {
				res2 = new TrackedInstance();
				res2.setClientId(itineraryId);
				res2.setDay(day);
				res2.setUserId(userId);
			}
			res2.setItinerary(res);
			
			if (res2.getStarted() == false) {
				sendIntineraryDataToGamificationEngine(gameId, userId, res);
			}
			if (device != null) {
				res2.setDeviceInfo(device);
			}
			res2.setStarted(true);
			storage.saveTrackedInstance(res2);
			
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/console/validate")
	public @ResponseBody void validate(HttpServletResponse response) throws Exception {
		List<TrackedInstance> result = storage.searchDomainObjects(new TreeMap<String, Object>(), TrackedInstance.class);
		for (TrackedInstance ti: result) {
			try {
				ValidationResult vr = GamificationHelper.checkItineraryMatching(ti.getItinerary(), ti.getGeolocationEvents());
				ti.setValidationResult(vr);
				ti.setValid(vr.getValid());
				storage.saveTrackedInstance(ti);
			} catch (Exception e) {
				logger.error("Failed to validate tracked itinerary: " + ti.getId());
				e.printStackTrace();
			}
			
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/r353nd")
	public @ResponseBody void resend(HttpServletResponse response) throws Exception {
		List<TrackedInstance> result = storage.searchDomainObjects(new TreeMap<String, Object>(), TrackedInstance.class);
		int i = 0;
		for (TrackedInstance ti: result) {
			try {
				logger.info("Sending for player " + ti.getItinerary().getUserId() + ", itinerary: " + ti.getItinerary().getName() + " (" + ti.getId() + " / " + ti.getItinerary().getClientId() + ")");
				sendIntineraryDataToGamificationEngine(gameId, ti.getItinerary().getUserId(), ti.getItinerary());
				i++;
				logger.info("Resent " + i + "/" + result.size());
				Thread.sleep(1000);
			} catch (Exception e) {
				logger.error("Failed to resend gamification data for: " + ti.getId());
			}
			
		}
	}	
	
	
	@RequestMapping("/console")
	public String vewConsole() {
		return "viewconsole";
	}

	@RequestMapping("/console/itinerary")
	public @ResponseBody List<ItineraryDescriptor> getItineraryList() {
		List<ItineraryDescriptor> list = new ArrayList<ItineraryDescriptor>();
		Map<String,ItineraryDescriptor> map = new HashMap<String, ItineraryDescriptor>();
		List<TrackedInstance> instances = storage.searchDomainObjects(Collections.<String,Object>emptyMap(), TrackedInstance.class);
		if (instances != null) {
			for (TrackedInstance o : instances) {
				ItineraryDescriptor descr = map.get(o.getClientId());
				if (map.get(o.getClientId()) == null) {
					descr = new ItineraryDescriptor();
					if (o.getUserId() != null) {
						descr.setUserId(o.getUserId());
					} else {
						ItineraryObject itinerary = storage.searchDomainObject(Collections.<String,Object>singletonMap("clientId", o.getClientId()), ItineraryObject.class);
						if (itinerary != null) {
							descr.setUserId(itinerary.getUserId());
						} else {
							continue;
						}
					}
					descr.setTripId(o.getClientId());
					descr.setStartTime(o.getItinerary().getData().getStartime());
					descr.setEndTime(o.getItinerary().getData().getEndtime());
					descr.setTripName(o.getItinerary().getName());
					descr.setRecurrency(o.getItinerary().getRecurrency());
					descr.setInstances(new ArrayList<TrackedInstance>());
					map.put(o.getClientId(), descr);
					list.add(descr);
				}
				descr.getInstances().add(o);
			}
		}
		return list;
	}

	@RequestMapping("/console/itinerary/{instanceId}")
	public @ResponseBody TrackedInstance getItineraryData( @PathVariable String instanceId) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		return instance;
	}
	
	private String buildInsert(Geolocation geolocation) {
		String s = "INSERT INTO geolocations VALUES($next_id,";
		s += convertToInsert(geolocation.getUuid()) + "," 
				+ convertToInsert(geolocation.getDevice_id()) + "," 
				+ convertToInsert(geolocation.getDevice_model()) + ","
				+ convertToInsert(geolocation.getLatitude()) + ","
				+ convertToInsert(geolocation.getLongitude()) + ","
				+ convertToInsert(geolocation.getAccuracy()) + "," 
				+ convertToInsert(geolocation.getAltitude()) + ","
				+ convertToInsert(geolocation.getSpeed()) + ","
				+ convertToInsert(geolocation.getHeading()) + ","
				+ convertToInsert(geolocation.getActivity_type()) + ","
				+ convertToInsert(geolocation.getActivity_confidence()) + ","
				+ convertToInsert(geolocation.getBattery_level()) + ","
				+ convertToInsert(geolocation.getBattery_is_charging()) + ","
				+ convertToInsert(geolocation.getIs_moving()) + ","
				+ convertToInsert(geolocation.getGeofence()) + ","
				+ convertToInsert(geolocation.getRecorded_at()) + ","
				+ convertToInsert(geolocation.getCreated_at()) + ","
				+  convertToInsert(geolocation.getUserId()) + ","
				+ convertToInsert(geolocation.getTravelId());
		s += ")";
		return s;
	}
	
	private String convertToInsert(Object o) {
		if (o instanceof String) {
			return "\"" + escape(o) + "\"";
		}
		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue() ? "1" : "0";
		}
		if (o instanceof Date) {
			return "\"" + sdf.format((Date)o)+ "\"";
		}
		if (o != null) {
			return o.toString();
		} else {
			return null;
		}
	}
	
	private String escape(Object o) {
		return ((o != null)?o.toString().replace("\"", "\"\""):"");
	}	
	
	private void sendIntineraryDataToGamificationEngine(String gameId, String playerId, ItineraryObject itinerary) throws Exception {
		logger.info("Send data for user " + playerId + ", trip " + itinerary.getClientId());
//		Criteria criteria = new Criteria("userId").is(playerId).and("travelId").is(itinerary.getClientId());
//		Query mongoQuery = new Query(criteria).with(new Sort(Sort.Direction.DESC, "created_at"));
//		
//		List<Geolocation> geolocations = storage.searchDomainObjects(mongoQuery, Geolocation.class);
		
		gamificationHelper.saveItinerary(itinerary, gameId, playerId);
	}

	@Override
	protected AuthServices getAuthServices() {
		return services;
	}
}
