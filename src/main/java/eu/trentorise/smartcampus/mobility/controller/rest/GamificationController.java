package eu.trentorise.smartcampus.mobility.controller.rest;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.gamification.GamificationManager;
import eu.trentorise.smartcampus.mobility.gamification.GamificationValidator;
import eu.trentorise.smartcampus.mobility.gamification.model.ItineraryDescriptor;
import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance.ScoreStatus;
import eu.trentorise.smartcampus.mobility.gamification.model.TravelDetails;
import eu.trentorise.smartcampus.mobility.gamification.model.UserDescriptor;
import eu.trentorise.smartcampus.mobility.gamification.statistics.AggregationGranularity;
import eu.trentorise.smartcampus.mobility.gamification.statistics.GlobalStatistics;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsBuilder;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsGroup;
import eu.trentorise.smartcampus.mobility.geolocation.model.Activity;
import eu.trentorise.smartcampus.mobility.geolocation.model.Battery;
import eu.trentorise.smartcampus.mobility.geolocation.model.Coords;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.GeolocationsEvent;
import eu.trentorise.smartcampus.mobility.geolocation.model.Location;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.security.AppDetails;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.TType;

@Controller
@RequestMapping(value = "/gamification")
public class GamificationController {

	private static final String TRAVEL_ID = "travelId";
	public static final String START_TIME = "startTime";

	/**
	 * 
	 */
	private static final int SAME_TRIP_INTERVAL = 5 * 60 * 1000; // 5 minutes

	@Autowired
	private DomainStorage storage;

	@Autowired
	@Value("${geolocations.db.dir}")
	private String geolocationsDBDir;

	@Autowired
	@Value("${geolocations.db.name}")
	private String geolocationsDB;

	@Autowired
	@Value("${aacURL}")
	private String aacURL;

	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;
	
	@Autowired
	private StatisticsBuilder statisticsBuilder;

	private BasicProfileService basicProfileService;

	@Autowired
	private GamificationValidator gamificationValidator;		
	
	@Autowired
	private GamificationManager gamificationManager;	

	private static Log logger = LogFactory.getLog(GamificationController.class);

	private Connection connection;

	private static SimpleDateFormat shortSdf = new SimpleDateFormat("yyyy/MM/dd");
	private static SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm");
	private static SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

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
	public @ResponseBody String storeGeolocationEvent(@RequestBody(required=false) GeolocationsEvent geolocationsEvent, @RequestHeader(required = true, value = "appId") String appId,
			HttpServletResponse response) throws Exception {
//		logger.info("Receiving geolocation events, token = " + token + ", " + geolocationsEvent.getLocation().size() + " events");
		ObjectMapper mapper = new ObjectMapper();

		// logger.info(mapper.writeValueAsString(geolocationsEvent));
		try {
			String userId = getUserId();
			if (userId == null) {
				logger.warn("Storing geolocations, user not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return "";
			}

			logger.info("Storing geolocations for " + userId + ", " + geolocationsEvent.getDevice());

			String gameId = getGameId(appId);
			if (gameId == null) {
				logger.warn("Storing geolocations, gameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return "";
			}

			Multimap<String, Geolocation> geolocationsByItinerary = ArrayListMultimap.create();
			Map<String, String> freeTracks = new HashMap<String, String>();
			Map<String, Long> freeTrackStarts = new HashMap<String, Long>();

			if (geolocationsEvent.getLocation() != null && !geolocationsEvent.getLocation().isEmpty()) {
				Location lastOk = geolocationsEvent.getLocation().get(geolocationsEvent.getLocation().size() - 1);
				ArrayList<Location> toKeep = Lists.newArrayList();
				toKeep.add(lastOk);
				for (int i = geolocationsEvent.getLocation().size() - 2; i >= 0; i--) {
					Location l1 = geolocationsEvent.getLocation().get(i);

					Date dOk = lastOk.getTimestamp();
					Date d1 = l1.getTimestamp();
					if (d1 == null) {
						logger.warn("Missing timestamp in location object: "+l1.toString());
						continue;
					}

					int comp = d1.compareTo(dOk);
					if (comp < 0) {
						lastOk = l1;
						toKeep.add(l1);
					} else {
						String tidOk = null;
						String tid1 = null;

						if (lastOk.getExtras() != null && lastOk.getExtras().containsKey("idTrip")) {
							tidOk = (String) lastOk.getExtras().get("idTrip");
						}
						if (l1.getExtras() != null && l1.getExtras().containsKey("idTrip")) {
							tid1 = (String) l1.getExtras().get("idTrip");
						}						
						logger.debug("'Unordered' events for user: " + userId + ", tripId: " + tid1 + " / " + tidOk + ", times: " + d1 + " / " + dOk + ", coordinates: " + l1.getCoords() + " / "
								+ lastOk.getCoords());
					}
				}

				
				geolocationsEvent.setLocation(toKeep);

				Collections.sort(geolocationsEvent.getLocation());
			} else {
				logger.info("No geolocations found.");
			}

			long now = System.currentTimeMillis();
			Map<String, Object> device = geolocationsEvent.getDevice();
			String deviceInfo = mapper.writeValueAsString(device);

			if (geolocationsEvent.getLocation() != null) {
				for (Location location : geolocationsEvent.getLocation()) {
					String locationTravelId = null;
					Long locationTs = null;
					if (location.getExtras() != null && location.getExtras().containsKey("idTrip")) {
						locationTravelId = (String) location.getExtras().get("idTrip");
						locationTs = location.getExtras().get("start") != null ? Long.parseLong("" + location.getExtras().get("start")) : null;
					} else {
						// now the plugin supports correctly the extras for each
						// location.
						// locations with empty idTrip are possible only upon
						// initialization/synchronization.
						// we skip them here
						// logger.info("location without idTrip, user: "+userId);
						continue;
						// if (lastTravelId != null) {
						// locationTravelId = lastTravelId;
						// } else {
						// continue;
						// }
					}

					if (location.getTimestamp() == null) {
						logger.warn("Missing timestamp in location object: "+location.toString());
						continue;
					}
					
					if (locationTs == null) {
						locationTs = location.getTimestamp().getTime();
					}

					// discard event older than 2 days
					if (now - 2 * 24 * 3600 * 1000 > location.getTimestamp().getTime()) {
						logger.warn("Timestamp too old, skipping.");
						continue;
					}

					Coords coords = location.getCoords();
					Activity activity = location.getActivity();
					Battery battery = location.getBattery();

					Geolocation geolocation = new Geolocation();

					geolocation.setUserId(userId);

					geolocation.setTravelId(locationTravelId);

					geolocation.setUuid(location.getUuid());
					if (device != null) {
						geolocation.setDevice_id((String)device.get("uuid"));
						geolocation.setDevice_model((String)device.get("model"));
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
					geolocation.setCreated_at(new Date(now++));

					if (location.getGeofence() != null) {
						geolocation.setGeofence(mapper.writeValueAsString(location.getGeofence()));

					}

					// Statement statement = connection.createStatement();
					// String s = buildInsert(geolocation);
					// statement.execute(s);
					// statement.close();

					geolocation.setGeofence(location.getGeofence());

					String day = shortSdf.format(new Date(locationTs));
					String key = geolocation.getTravelId() + "@" + day;
					geolocationsByItinerary.put(key, geolocation);
					if (StringUtils.hasText((String) location.getExtras().get("transportType"))) {
						freeTracks.put(key, (String) location.getExtras().get("transportType"));
					}
					if (StringUtils.hasText((String) location.getExtras().get("btDeviceId"))) {
						geolocation.setCertificate((String) location.getExtras().get("btDeviceId"));
					}
					freeTrackStarts.put(key, locationTs);

					// storage.saveGeolocation(geolocation);
				}
			}

			if (geolocationsByItinerary.keySet() == null || geolocationsByItinerary.keySet().isEmpty()) {
				logger.warn("No geolocationsByItinerary set.");
			}
			
			for (String key : geolocationsByItinerary.keySet()) {

				String splitKey[] = key.split("@");
				String travelId = splitKey[0];
				String day = splitKey[1];

				Map<String, Object> pars = new TreeMap<String, Object>();
				pars.put("clientId", travelId);
				pars.put("day", day);
				pars.put("userId", userId);
				TrackedInstance res = storage.searchDomainObject(pars, TrackedInstance.class);
				if (res == null) {
					logger.warn("No existing TrackedInstance found.");
					res = new TrackedInstance();
					res.setClientId(travelId);
					res.setDay(day);
					res.setUserId(userId);
					res.setId(ObjectId.get().toString());
					pars.remove("day");
					ItineraryObject res2 = storage.searchDomainObject(pars, ItineraryObject.class);
					if (res2 == null) {
						logger.warn("No existing ItineraryObject found.");
						pars = new TreeMap<String, Object>();
						pars.put("itinerary.clientId", travelId);
						pars.put("itinerary.userId", userId);
						SavedTrip res3 = storage.searchDomainObject(pars, SavedTrip.class);
						if (res3 != null) {
							res.setItinerary(res3.getItinerary());
						} else {
							logger.warn("No existing SavedTrip found.");
						}
					} else {
						res.setItinerary(res2);
						res.setTime(timeSdf.format(geolocationsByItinerary.get(key).iterator().next().getRecorded_at()));
					}
					if (res.getItinerary() == null) {
						String ftt = freeTracks.get(key);
						if (ftt == null) {
							logger.error("No freetracking transport found, extracting from clientId.");
							String[] cid = travelId.split("_");
							if (cid != null && cid.length > 1) {
								ftt = cid[0];
							} else {
								logger.error("Cannot find transport type for " + key);
							}
						}
						res.setFreeTrackingTransport(ftt);
						if (freeTrackStarts.containsKey(key)) {
							res.setTime(timeSdf.format(new Date(freeTrackStarts.get(key))));
						}
					}
				}

				if (geolocationsByItinerary.get(key) != null) {
					logger.info("Adding " + geolocationsByItinerary.get(key).size() + " geolocations to result.");
				}
				for (Geolocation geoloc : geolocationsByItinerary.get(key)) {
					res.getGeolocationEvents().add(geoloc);
				}
				
				// boolean canSave = true;
				if (res.getItinerary() != null) {
					if (!res.getComplete()) {
						ValidationResult vr = gamificationValidator.validatePlannedJourney(res.getItinerary(), res.getGeolocationEvents(), appId);
						res.setValidationResult(vr);

						if (vr != null && TravelValidity.VALID.equals(vr.getTravelValidity())) {
							Map<String, Object> trackingData = gamificationValidator.computePlannedJourneyScore(appId, res.getItinerary().getData(), res.getGeolocationEvents(), false);
							if (trackingData.containsKey("estimatedScore")) {
								res.setScore((Long) trackingData.get("estimatedScore"));
							}
							trackingData.put(TRAVEL_ID, res.getId());
							trackingData.put(START_TIME, getStartTime(res));
							gamificationManager.sendIntineraryDataToGamificationEngine(appId, userId, travelId + "_" + day, res.getItinerary(), trackingData);
							res.setScoreStatus(ScoreStatus.SENT);
						}
						res.setComplete(true);
					}
				} else if (res.getFreeTrackingTransport() != null) {
					if (!res.getComplete()) {
						ValidationResult vr = gamificationValidator.validateFreeTracking(res.getGeolocationEvents(), res.getFreeTrackingTransport(), appId);
						res.setValidationResult(vr);
						if (vr != null && TravelValidity.VALID.equals(vr.getTravelValidity())) {
							// canSave =
							Map<String, Object> trackingData = gamificationValidator.computeFreeTrackingScore(appId, res.getGeolocationEvents(), res.getFreeTrackingTransport(), vr.getValidationStatus());
							if (trackingData.containsKey("estimatedScore")) {
								res.setScore((Long) trackingData.get("estimatedScore"));
							}
							trackingData.put(TRAVEL_ID, res.getId());
							trackingData.put(START_TIME, getStartTime(res));
							gamificationManager.sendFreeTrackingDataToGamificationEngine(appId, userId, travelId, res.getGeolocationEvents(), res.getFreeTrackingTransport(), trackingData);
							res.setScoreStatus(ScoreStatus.SENT);
						} else {
							logger.debug("Validation result null, not sending data to gamification");
						}
					}
					res.setComplete(true);
				}

				res.setAppId(appId);
				res.setDeviceInfo(deviceInfo);
				storage.saveTrackedInstance(res);
				
				logger.info("Saved geolocation events: " + res.getId() + ", " + res.getGeolocationEvents().size() + " events.");
			}

		} catch (Exception e) {
			logger.error("Failed storing events: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"storeResult\":\"FAIL\"}";
		}
		return "{\"storeResult\":\"OK\"}";
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/geolocations")
	public @ResponseBody List<Geolocation> searchGeolocationEvent(@RequestParam Map<String, Object> query, HttpServletResponse response) throws Exception {

		Criteria criteria = new Criteria();
		for (String key : query.keySet()) {
			criteria = criteria.and(key).is(query.get(key));
		}

		Query mongoQuery = new Query(criteria).with(new Sort(Sort.Direction.DESC, "created_at"));

		return storage.searchDomainObjects(mongoQuery, Geolocation.class);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/freetracking/{transport}/{itineraryId}")
	public @ResponseBody void startFreeTracking(@PathVariable String transport, @PathVariable String itineraryId,
			@RequestHeader(required = true, value = "appId") String appId, @RequestHeader(required = false, value = "device") String device, HttpServletResponse response) throws Exception {
		logger.info("Starting free tracking for gamification, device = " + device);
		try {
			String userId = getUserId();
			if (userId == null) {
				logger.warn("Start freetracking, user not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String gameId = getGameId(appId);
			if (gameId == null) {
				logger.warn("Start freetracking, gameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();

			pars.put("clientId", itineraryId);
			pars.put("userId", userId);
			Date date = new Date(System.currentTimeMillis());
			String day = shortSdf.format(date);
			TrackedInstance res2 = storage.searchDomainObject(pars, TrackedInstance.class);
			if (res2 == null) {
				res2 = new TrackedInstance();
				res2.setClientId(itineraryId);
				res2.setDay(day);
				res2.setUserId(userId);
				res2.setTime(timeSdf.format(date));
			}

			if (device != null) {
				res2.setDeviceInfo(device);
			}
			res2.setStarted(true);
			res2.setFreeTrackingTransport(transport);
			res2.setAppId(appId);
			storage.saveTrackedInstance(res2);

		} catch (Exception e) {
			logger.error("Error in start freetracking: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/journey/{itineraryId}")
	public @ResponseBody void startItinerary(@PathVariable String itineraryId, @RequestHeader(required = true, value = "appId") String appId, @RequestHeader(required = false, value = "device") String device, HttpServletResponse response)
			throws Exception {
		logger.info("Starting journey for gamification, device = " + device);
		try {
			String userId = getUserId();
			if (userId == null) {
				logger.warn("Start planned journey, user not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String gameId = getGameId(appId);
			if (gameId == null) {
				logger.warn("Start planned journey, gameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();

			pars.put("clientId", itineraryId);
			pars.put("userId", userId);
			ItineraryObject res = storage.searchDomainObject(pars, ItineraryObject.class);
			if (res != null && !userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				logger.info("Unauthorized.");
				return;
			}
			if (res == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				// TODO report problem better
				logger.info("Start planned journey, itinerary not found.");
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
				res2.setTime(timeSdf.format(date));
			}
			res2.setItinerary(res);

			// boolean canSave = true;
//			if (!res2.getStarted() && !res2.getComplete()) {
//				// canSave =
//				gamificationManager.sendIntineraryDataToGamificationEngine(appId, userId, itineraryId + "_" + day, res);
//			}

			if (device != null) {
				res2.setDeviceInfo(device);
			}
			res2.setStarted(true);
			res2.setAppId(appId);
			storage.saveTrackedInstance(res2);

		} catch (Exception e) {
			// TODO correct log, report relevant info
			logger.error("Error in start planned journey: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@RequestMapping(method = RequestMethod.PUT, value = "/temporary")
	public @ResponseBody void startTemporaryItinerary(@RequestBody(required=true) ItineraryObject itinerary, @RequestHeader(required = true, value = "appId") String appId, @RequestHeader(required = false, value = "device") String device, HttpServletResponse response)
			throws Exception {
		logger.info("Starting temporary journey for gamification, device = " + device);
		try {
			String userId = getUserId();
			if (userId == null) {
				logger.warn("Start temporary journey, user not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String gameId = getGameId(appId);
			if (gameId == null) {
				logger.warn("Start temporary journey, gameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			Date date = new Date(System.currentTimeMillis());
			String day = shortSdf.format(date);
			TrackedInstance ti = new TrackedInstance();

			ti.setClientId(itinerary.getClientId());
			ti.setDay(day);
			ti.setUserId(userId);
			ti.setTime(timeSdf.format(date));
			
			convertParkWalk(itinerary);
			ti.setItinerary(itinerary);

			if (device != null) {
				ti.setDeviceInfo(device);
			}
			ti.setStarted(true);
			ti.setAppId(appId);
			storage.saveTrackedInstance(ti);

		} catch (Exception e) {
			logger.error("Error in start temporary journey: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
	private void convertParkWalk(ItineraryObject itinerary) {
		for (Leg leg: itinerary.getData().getLeg()) {
			if (leg.getTransport() != null && TType.PARKWALK.equals(leg.getTransport().getType())) {
				leg.getTransport().setType(TType.WALK);
			}
		}
	}
	
	@RequestMapping("/traveldetails/{id}")
	public @ResponseBody TravelDetails getTravelDetails(@PathVariable String id, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws ParseException {
		String userId = getUserId();
		if (userId == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}

		Criteria criteria = new Criteria("id").is(id).and("appId").is(appId).and("userId").is(userId);
		
		Query query = new Query(criteria);

		TrackedInstance instance = storage.searchDomainObject(query, TrackedInstance.class);
		
		TravelDetails result = null;
		if (instance != null) {
			result = new TravelDetails();
			result.setFreeTrackingTransport(instance.getFreeTrackingTransport());
			result.setItinerary(instance.getItinerary());
			if (instance.getGeolocationEvents() != null && !instance.getGeolocationEvents().isEmpty()) {
				List<Geolocation> geo = Lists.newArrayList(instance.getGeolocationEvents());
				Collections.sort(geo);				
				result.setGeolocationPolyline(GamificationHelper.encodePoly(geo));
			}
			result.setValidationResult(instance.getValidationResult());
			if (instance.getChangedValidity() != null) {
				result.setValidity(instance.getChangedValidity());	
			} else {
				result.setValidity(instance.getValidationResult().getTravelValidity());
			}			
		}
		
		return result;
	}	
	
	@RequestMapping(method = RequestMethod.POST, value = "/console/validate")
	public @ResponseBody void validate(@RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate, @RequestParam(required = false) Boolean excludeZeroPoints, @RequestParam(required = false) Boolean toCheck, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws Exception {

		Criteria criteria = new Criteria("appId").is(appId);

		if (excludeZeroPoints != null && excludeZeroPoints.booleanValue()) {
			criteria = criteria.and("estimatedScore").gt(0);
		}
		if (toCheck != null && toCheck.booleanValue()) {
			criteria = criteria.and("toCheck").is(true);
		}	
//		if (fromDate != null) {
//			criteria = criteria.and("geolocationEvents.recorded_at").gte(new Date(fromDate));
//		}
//		if (toDate != null) {
//			criteria = criteria.andOperator(new Criteria("geolocationEvents.recorded_at").lte(new Date(toDate)));
//		}
		
		if (fromDate != null) {
			String fd = shortSdf.format(new Date(fromDate));
			criteria = criteria.and("day").gte(fd);
		}
		
		if (toDate != null) {
			String td = shortSdf.format(new Date(toDate));
			criteria = criteria.andOperator(new Criteria("day").lte(td));
		}
		
		Query query = new Query(criteria);		
		
		List<TrackedInstance> result = storage.searchDomainObjects(query, TrackedInstance.class);
		
		
		for (TrackedInstance ti : result) {
			try {
				if (ti.getItinerary() != null) {
					logger.info("Validating planned " + ti.getId());
					ValidationResult vr = gamificationValidator.validatePlannedJourney(ti.getItinerary(), ti.getGeolocationEvents(), appId);
					ti.setValidationResult(vr);
					Map<String, Object> data = gamificationValidator.computePlannedJourneyScore(appId, ti.getItinerary().getData(), ti.getGeolocationEvents(), false);
					if (ti.getScoreStatus() == null || ScoreStatus.UNASSIGNED.equals(ti.getScoreStatus())) {
						ti.setScoreStatus(ScoreStatus.COMPUTED);
					}
					ti.setScore((Long) data.get("estimatedScore"));
					storage.saveTrackedInstance(ti);

				} else {
					logger.info("Validating free tracking " + ti.getId());
					ValidationResult vr = gamificationValidator.validateFreeTracking(ti.getGeolocationEvents(), ti.getFreeTrackingTransport(), appId);
					ti.setValidationResult(vr);
					Map<String, Object> data = gamificationValidator.computeFreeTrackingScore(appId, ti.getGeolocationEvents(), ti.getFreeTrackingTransport(), vr.getValidationStatus());
					if (ti.getScoreStatus() == null || ScoreStatus.UNASSIGNED.equals(ti.getScoreStatus())) {
						ti.setScoreStatus(ScoreStatus.COMPUTED);
					}					
					ti.setScore((Long) data.get("estimatedScore"));
					storage.saveTrackedInstance(ti);
				}
			} catch (Exception e) {
				// TODO fix log
				logger.error("Failed to validate tracked itinerary: " + ti.getId(), e);
			}

		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/console/assignScore")
	public @ResponseBody TrackedInstance assignScore(@PathVariable String instanceId, HttpServletResponse response) throws Exception {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		if (instance.getScoreStatus() != null && !ScoreStatus.UNASSIGNED.equals(instance.getScoreStatus())) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			return null;
		}
		
		if (instance.getItinerary() != null) {
			Map<String, Object> trackingData = gamificationValidator.computePlannedJourneyScore(instance.getAppId(), instance.getItinerary().getData(), instance.getGeolocationEvents(), false);
			if (trackingData.containsKey("estimatedScore")) {
				instance.setScore((Long) trackingData.get("estimatedScore"));
			}
			trackingData.put(TRAVEL_ID, instance.getId());
			trackingData.put(START_TIME, getStartTime(instance));
			gamificationManager.sendIntineraryDataToGamificationEngine(instance.getAppId(), instance.getUserId(), instance.getClientId() + "_" + instance.getDay(), instance.getItinerary(), trackingData);
			instance.setScoreStatus(ScoreStatus.SENT);
		} else if (instance.getFreeTrackingTransport() != null) {
			Map<String, Object> trackingData = gamificationValidator.computeFreeTrackingScore(instance.getAppId(), instance.getGeolocationEvents(), instance.getFreeTrackingTransport(), instance.getValidationResult().getValidationStatus());
			trackingData.put(TRAVEL_ID, instance.getId());
			trackingData.put(START_TIME, getStartTime(instance));
			if (trackingData.containsKey("estimatedScore")) {
				instance.setScore((Long) trackingData.get("estimatedScore"));
			}
			
			gamificationManager.sendFreeTrackingDataToGamificationEngine(instance.getAppId(), instance.getUserId(), instance.getClientId(), instance.getGeolocationEvents(), instance.getFreeTrackingTransport(), trackingData);
			instance.setScoreStatus(ScoreStatus.SENT);
		}
		
		storage.saveTrackedInstance(instance);
		return instance;
	}
	

	@RequestMapping(method = RequestMethod.POST, value = "/console/itinerary/changeValidity/{instanceId}")
	public @ResponseBody TrackedInstance changeValidity(@PathVariable String instanceId, @RequestParam(required = true) TravelValidity value) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		instance.setChangedValidity(value);
		storage.saveTrackedInstance(instance);
		logger.info("Changed validity for " + instanceId + " to " + value);
		return instance;
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/console/itinerary/toCheck/{instanceId}")
	public @ResponseBody TrackedInstance toCheck(@PathVariable String instanceId, @RequestParam(required = true) boolean value) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		instance.setToCheck(value);
		storage.saveTrackedInstance(instance);
		logger.info("Changed \"to check\" for " + instanceId + " to " + value);
		return instance;
	}	

	@RequestMapping(method = RequestMethod.POST, value = "/console/approveFiltered")
	public @ResponseBody void approveFiltered(@RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate, @RequestParam(required = false) Boolean excludeZeroPoints, @RequestParam(required = false) Boolean toCheck) throws Exception {
		Criteria criteria = new Criteria("changedValidity").ne(null).and("approved").ne(true);

		if (excludeZeroPoints != null && excludeZeroPoints.booleanValue()) {
			criteria = criteria.and("estimatedScore").gt(0);
		}
		if (toCheck != null && toCheck.booleanValue()) {
			criteria = criteria.and("toCheck").is(true);
		}	
//		if (fromDate != null) {
//			criteria = criteria.and("geolocationEvents.recorded_at").gte(new Date(fromDate));
//		}
//		if (toDate != null) {
//			criteria = criteria.andOperator(new Criteria("geolocationEvents.recorded_at").lte(new Date(toDate)));
//		}
		
		if (fromDate != null) {
			String fd = shortSdf.format(new Date(fromDate));
			criteria = criteria.and("day").gte(fd);
		}
		
		if (toDate != null) {
			String td = shortSdf.format(new Date(toDate));
			criteria = criteria.andOperator(new Criteria("day").lte(td));
		}		
		
		Query query = new Query(criteria);

		List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
		for (TrackedInstance ti : instances) {
			logger.info("ApproveAndSendScore for " + ti.getId());
			approveAndSendScore(ti);
		}
	}
	
	private void approveAndSendScore(TrackedInstance instance) throws Exception {
		if (!TravelValidity.VALID.equals(instance.getValidationResult().getTravelValidity()) && TravelValidity.VALID.equals(instance.getChangedValidity())) {
			logger.info("Sending approved itinerary data to GE: " + instance.getId());
			if (instance.getItinerary() != null) {
				Map<String, Object> trackingData = gamificationValidator.computePlannedJourneyScore(instance.getAppId(), instance.getItinerary().getData(), instance.getGeolocationEvents(), false);
				if (trackingData.containsKey("estimatedScore")) {
					instance.setScore((Long) trackingData.get("estimatedScore"));
				}
				trackingData.put(TRAVEL_ID, instance.getId());
				trackingData.put(START_TIME, getStartTime(instance));
				gamificationManager.sendIntineraryDataToGamificationEngine(instance.getAppId(), instance.getUserId(), instance.getClientId() + "_" + instance.getDay(), instance.getItinerary(),
						trackingData);
				instance.setScoreStatus(ScoreStatus.SENT);
			} else if (instance.getFreeTrackingTransport() != null) {
				Map<String, Object> trackingData = gamificationValidator.computeFreeTrackingScore(instance.getAppId(), instance.getGeolocationEvents(), instance.getFreeTrackingTransport(), instance.getValidationResult().getValidationStatus());
				if (trackingData.containsKey("estimatedScore")) {
					instance.setScore((Long) trackingData.get("estimatedScore"));
				}
				trackingData.put(TRAVEL_ID, instance.getId());
				trackingData.put(START_TIME, getStartTime(instance));
				gamificationManager.sendFreeTrackingDataToGamificationEngine(instance.getAppId(), instance.getUserId(), instance.getClientId(), instance.getGeolocationEvents(),
						instance.getFreeTrackingTransport(), trackingData);
				instance.setScoreStatus(ScoreStatus.SENT);
			}
		}

		instance.setApproved(true);
		storage.saveTrackedInstance(instance);
	}

	@RequestMapping(value = "/console/report")
	public @ResponseBody void generareReport(HttpServletResponse response, @RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate) throws IOException {
		Criteria criteria = new Criteria("changedValidity").ne(null).and("approved").ne(true);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		String fileName = "report";

//		if (fromDate != null) {
//			criteria = criteria.and("geolocationEvents.recorded_at").gte(new Date(fromDate));
//			fileName += "_" + sdf.format(new Date(fromDate));
//		}
//		if (toDate != null) {
//			criteria = criteria.andOperator(new Criteria("geolocationEvents.recorded_at").lte(new Date(toDate)));
//			fileName += "_" + sdf.format(new Date(toDate));
//		}
		
		if (fromDate != null) {
			String fd = shortSdf.format(new Date(fromDate));
			criteria = criteria.and("day").gte(fd);
			fileName += "_" + sdf.format(new Date(fromDate));
		}
		
		if (toDate != null) {
			String td = shortSdf.format(new Date(toDate));
			criteria = criteria.andOperator(new Criteria("day").lte(td));
			fileName += "_" + sdf.format(new Date(toDate));
		}		
		
		Query query = new Query(criteria).with(new Sort(Direction.DESC, "userId"));

		List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
		StringBuffer sb = new StringBuffer("userId;id;freeTracking;itineraryName;score;valid\r\n");
		for (TrackedInstance ti : instances) {
			if (ti.getScore() == null) {
				if (ti.getItinerary() != null) {
					Itinerary itinerary = ti.getItinerary().getData();
					long score = gamificationValidator.computeEstimatedGameScore(ti.getAppId(), itinerary, ti.getGeolocationEvents(), false);
					ti.setScore(score);
					storage.saveTrackedInstance(ti);
				}
			}
			sb.append(ti.getUserId() + ";" + ti.getId() + ";" + (ti.getFreeTrackingTransport() != null) + ";" 
		+ ((ti.getItinerary() != null) ? ti.getItinerary().getName() : "") + ";" + ti.getScore() + ";" + ((ti.getChangedValidity() != null) ? ti.getChangedValidity() : ti.getValidationResult().getTravelValidity()) + "\r\n");
		}

		response.setContentType("application/csv; charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".csv\"");
		response.getWriter().write(sb.toString());
	}

	@RequestMapping("/console")
	public String viewConsole() {
		return "gamificationconsole";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/console/appId", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	public @ResponseBody String getAppId(HttpServletResponse response) throws Exception {
		String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
		return appId;
	}

	@RequestMapping("/console/useritinerary/{userId}")
	public @ResponseBody List<ItineraryDescriptor> getItineraryListForUser(@PathVariable String userId, @RequestHeader(required = true, value = "appId") String appId,
			@RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate, @RequestParam(required = false) Boolean excludeZeroPoints,
			@RequestParam(required = false) Boolean unapprovedOnly, @RequestParam(required = false) Boolean toCheck) throws Exception {
		List<ItineraryDescriptor> list = new ArrayList<ItineraryDescriptor>();

		try {
			Criteria criteria = new Criteria("userId").is(userId).and("appId").is(appId);
			if (excludeZeroPoints != null && excludeZeroPoints.booleanValue()) {
				criteria = criteria.and("estimatedScore").gt(0);
			}
			if (unapprovedOnly != null && unapprovedOnly.booleanValue()) {
				criteria = criteria.and("approved").ne(true).and("changedValidity").ne(null);
			}
			if (toCheck != null && toCheck.booleanValue()) {
				criteria = criteria.and("toCheck").is(true);
			}

			// if (fromDate != null) {
			// criteria = criteria.and("geolocationEvents.recorded_at").gte(new Date(fromDate));
			// }
			// if (toDate != null) {
			// criteria = criteria.andOperator(new Criteria("geolocationEvents.recorded_at").lte(new Date(toDate)));
			// }

			if (fromDate != null) {
				String fd = shortSdf.format(new Date(fromDate));
				criteria = criteria.and("day").gte(fd);
			}

			if (toDate != null) {
				String td = shortSdf.format(new Date(toDate));
				criteria = criteria.andOperator(new Criteria("day").lte(td));
			}

			Query query = new Query(criteria);

			logger.debug("Start itinerary query for " + userId);
			List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
			logger.debug("End itinerary query for " + userId);

			Map<String, Double> scores = gamificationManager.getScoreNotification(appId, userId);

			if (instances != null) {
				for (TrackedInstance o : instances) {
					List<Geolocation> geo = Lists.newArrayList(o.getGeolocationEvents());
					Collections.sort(geo);
					o.setGeolocationEvents(geo);

					if (scores.containsKey(o.getId()) && !ScoreStatus.ASSIGNED.equals(o.getScoreStatus())) {
						o.setScore(scores.get(o.getId()).longValue());
						o.setScoreStatus(ScoreStatus.ASSIGNED);
						storage.saveTrackedInstance(o);
					}
				}

				// instances = aggregateFollowingTrackedInstances(instances);
				for (TrackedInstance o : instances) {
					ItineraryDescriptor descr = new ItineraryDescriptor();
					if (o.getUserId() != null) {
						descr.setUserId(o.getUserId());
					} else {
						ItineraryObject itinerary = storage.searchDomainObject(Collections.<String, Object>singletonMap("clientId", o.getClientId()), ItineraryObject.class);
						if (itinerary != null) {
							descr.setUserId(itinerary.getUserId());
						} else {
							continue;
						}
					}
					descr.setTripId(o.getClientId());

					if (o.getGeolocationEvents() != null && !o.getGeolocationEvents().isEmpty()) {
						Geolocation event = o.getGeolocationEvents().iterator().next();
						descr.setStartTime(event.getRecorded_at().getTime());
					} else if (o.getDay() != null && o.getTime() != null) {
						String dt = o.getDay() + " " + o.getTime();
						descr.setStartTime(fullSdf.parse(dt).getTime());
					} else if (o.getDay() != null) {
						descr.setStartTime(shortSdf.parse(o.getDay()).getTime());
					}

					if (o.getItinerary() != null) {
						descr.setEndTime(o.getItinerary().getData().getEndtime());
						descr.setTripName(o.getItinerary().getName() + " (" + o.getId() + ")");
						descr.setRecurrency(o.getItinerary().getRecurrency());
					} else {
						descr.setFreeTrackingTransport(o.getFreeTrackingTransport());
						descr.setTripName(o.getId());
					}
					descr.setInstance(o);
					list.add(descr);
				}
			}

			Collections.sort(list);
			Collections.reverse(list);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return list;
	}

	@RequestMapping("/console/users")
	public @ResponseBody List<UserDescriptor> getTrackInstancesUsers(@RequestHeader(required = true, value = "appId") String appId, @RequestParam(required = false) Long fromDate,
			@RequestParam(required = false) Long toDate, @RequestParam(required = false) Boolean excludeZeroPoints, @RequestParam(required = false) Boolean unapprovedOnly,
			@RequestParam(required = false) Boolean toCheck) throws ParseException {
		List<UserDescriptor> userList = null;

		try {
			Map<String, UserDescriptor> users = new HashMap<String, UserDescriptor>();
			Set<String> keys = new HashSet<String>();
			keys.add("userId");
			keys.add("validationResult");
			keys.add("approved");
			keys.add("changedValidity");

			Criteria criteria = new Criteria("appId").is(appId);
			if (excludeZeroPoints != null && excludeZeroPoints.booleanValue()) {
				criteria = criteria.and("estimatedScore").gt(0);
			}
			if (unapprovedOnly != null && unapprovedOnly.booleanValue()) {
				criteria = criteria.and("approved").ne(true).and("changedValidity").ne(null);
			}
			if (toCheck != null && toCheck.booleanValue()) {
				criteria = criteria.and("toCheck").is(true);
			}
			// if (fromDate != null) {
			// criteria = criteria.and("geolocationEvents.recorded_at").gte(new Date(fromDate));
			// }
			// if (toDate != null) {
			// criteria = criteria.andOperator(new Criteria("geolocationEvents.recorded_at").lte(new Date(toDate)));
			// }

			if (fromDate != null) {
				String fd = shortSdf.format(new Date(fromDate));
				criteria = criteria.and("day").gte(fd);
			}

			if (toDate != null) {
				String td = shortSdf.format(new Date(toDate));
				criteria = criteria.andOperator(new Criteria("day").lte(td));
			}

			Query query = new Query(criteria);

			List<TrackedInstance> tis = storage.searchDomainObjects(query, keys, TrackedInstance.class);

			for (TrackedInstance ti : tis) {
				String userId = ti.getUserId();
				if (userId == null) {
					continue;
				}
				UserDescriptor ud = users.get(userId);
				if (ud == null) {
					ud = new UserDescriptor();
					ud.setUserId(userId);
					ud.setValid(0);
					ud.setTotal(0);
					users.put(userId, ud);
				}
				ud.setTotal(ud.getTotal() + 1);
				TravelValidity validity = ti.getValidationResult().getTravelValidity();
				if (ti.getApproved() != null && ti.getApproved().booleanValue() && ti.getChangedValidity() != null) {
					validity = ti.getChangedValidity();
				}
				switch (validity) {
				case VALID:
					ud.setValid(ud.getValid() + 1);
					break;
				case INVALID:
					ud.setInvalid(ud.getInvalid() + 1);
					break;
				case PENDING:
					ud.setPending(ud.getPending() + 1);
					break;
				}
			}

			userList = new ArrayList<UserDescriptor>(users.values());
			Collections.sort(userList);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return userList;
	}

	@RequestMapping("/console/itinerary/{instanceId}")
	public @ResponseBody TrackedInstance getItineraryData(@PathVariable String instanceId) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		return instance;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/statistics/player")
	public @ResponseBody StatisticsGroup playerStatistics(@RequestParam(required=false) Long from, @RequestParam(required=false) Long to, @RequestParam(required=false) AggregationGranularity granularity,
			@RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws Exception {
		StatisticsGroup result = null;
		try {
			String userId = null;
			try {
				userId = getUserId();
			} catch (SecurityException e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}	
			
			long start = 0;
			AppInfo ai = appSetup.findAppById(appId);
			if (ai == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return null;
			}
			GameInfo game = gameSetup.findGameById(ai.getGameId());			
			
			String startDate = game.getStart();
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			start = sdf.parse(startDate).getTime();
			
			if (from == null) {
				from = start;
			} else {
				from = Math.max(from, start);
			}			
			if (to == null) {
				to = System.currentTimeMillis();
			}
			if (granularity == null) {
				granularity = AggregationGranularity.total;
			}
			

			result = statisticsBuilder.computeStatistics(userId, from, to, granularity);
			
		} catch (Exception e) {
			logger.error("Failed retrieving player statistics events: "+e.getMessage(),e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return result;
		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/statistics/global/player")
	public @ResponseBody GlobalStatistics globalPlayerStatistics(@RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws Exception {
		GlobalStatistics result = null;
		try {
			String userId = null;
			try {
				userId = getUserId();
			} catch (SecurityException e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}	
			
			long start = 0;
			AppInfo ai = appSetup.findAppById(appId);
			if (ai == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return null;
			}
			GameInfo game = gameSetup.findGameById(ai.getGameId());
			
			String startDate = game.getStart();
			SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yyyy");
			start = sdf1.parse(startDate).getTime();
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
			startDate = sdf2.format(new Date(start));
			

			result = statisticsBuilder.getGlobalStatistics(userId, startDate, true);
			
		} catch (Exception e) {
			logger.error("Failed retrieving player statistics events: "+e.getMessage(),e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return result;
		
	}	
	
	private List<TrackedInstance> aggregateFollowingTrackedInstances(List<TrackedInstance> instances) {
		List<TrackedInstance> sortedInstances = Lists.newArrayList(instances);
		Collections.sort(sortedInstances, new Comparator<TrackedInstance>() {

			@Override
			public int compare(TrackedInstance o1, TrackedInstance o2) {
				if (o1.getGeolocationEvents() == null || o1.getGeolocationEvents().isEmpty()) {
					return -1;
				}
				if (o2.getGeolocationEvents() == null || o2.getGeolocationEvents().isEmpty()) {
					return 1;
				}
				return (o1.getGeolocationEvents().iterator().next().compareTo(o2.getGeolocationEvents().iterator().next()));
			}
		});

		int groupId = 1;
		if (sortedInstances.size() > 1) {
			for (int i = 1; i < sortedInstances.size(); i++) {
				List<Geolocation> ge1 = (List) sortedInstances.get(i).getGeolocationEvents();
				List<Geolocation> ge2 = (List) sortedInstances.get(i - 1).getGeolocationEvents();

				if (ge1.isEmpty() || ge2.isEmpty()) {
					continue;
				}
				if (Math.abs(ge2.get(ge2.size() - 1).getRecorded_at().getTime() - ge1.get(0).getRecorded_at().getTime()) < SAME_TRIP_INTERVAL) {
					sortedInstances.get(i).setGroupId(groupId);
					sortedInstances.get(i - 1).setGroupId(groupId);
				} else {
					groupId++;
				}
			}
		}

		return sortedInstances;
	}

	private long getStartTime(TrackedInstance trackedInstance) throws ParseException {
		long time = 0;
		if (trackedInstance.getGeolocationEvents() != null && !trackedInstance.getGeolocationEvents().isEmpty()) {
			Geolocation event = trackedInstance.getGeolocationEvents().stream().sorted().findFirst().get();
//			Geolocation event = trackedInstance.getGeolocationEvents().iterator().next();
			time = event.getRecorded_at().getTime();
		} else if (trackedInstance.getDay() != null && trackedInstance.getTime() != null) {
			String dt = trackedInstance.getDay() + " " + trackedInstance.getTime();
			time = fullSdf.parse(dt).getTime();
		} else if (trackedInstance.getDay() != null) {
			time = shortSdf.parse(trackedInstance.getDay()).getTime();
		}
		return time;
	}
	
	private String getGameId(String appId) {
		if (appId != null) {
			AppInfo ai = appSetup.findAppById(appId);
			if (ai == null) {
				return null;
			}
			String gameId = ai.getGameId();
			return gameId;
		}
		return null;
	}

	protected String getUserId() {
		String principal = (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return principal;
	}
}
