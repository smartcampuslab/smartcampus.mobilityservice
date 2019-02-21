package eu.trentorise.smartcampus.mobility.controller.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.gamificationweb.EncryptDecrypt;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerProfile;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerWaypoint;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerWaypoints;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameSetup;

@RestController
public class ProfileController {

	@Value("${gamification.secretKey1}")
	private String secretKey1;
	@Autowired
	@Value("${gamification.secretKey2}")
	private String secretKey2;	
	
	private static final Logger logger = Logger.getLogger(ProfileController.class);

	private static FastDateFormat shortSdfApi = FastDateFormat.getInstance("yyyy-MM-dd");
	private static FastDateFormat shortSdfDb = FastDateFormat.getInstance("yyyy/MM/dd");
	private static FastDateFormat shortSdfGameInfo = FastDateFormat.getInstance("dd/MM/yyyy");
	private static FastDateFormat extendedSdf = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss,Z");
	private static FastDateFormat monthSdf = FastDateFormat.getInstance("yyyy-MM");

	private ObjectMapper mapper = new ObjectMapper();
	
	@Value("${waypointsDir}")
	private String waypointsDir;

	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private GameSetup gameSetup;
	
	private EncryptDecrypt cryptUtils;

	@GetMapping("/profile/{campaignId}")
	public @ResponseBody List<PlayerProfile> profile(@PathVariable String campaignId, @RequestParam(required = false) String date_from, @RequestParam(required = false) String date_to)
			throws Exception {
		AppInfo app = appSetup.findAppById(campaignId);
		String gameId = app.getGameId();
		Date startGame = shortSdfGameInfo.parse(gameSetup.findGameById(gameId).getStart());
		Calendar c = new GregorianCalendar();
		c.setTime(startGame);
		int year = c.get(Calendar.YEAR);

		Criteria criteria = new Criteria("gameId").is(gameId);
		List<Criteria> criterias = Lists.newArrayList();
		if (date_from != null) {
			long from = shortSdfApi.parse(date_from).getTime();
			criterias.add(new Criteria("personalData.timestamp").gte(from));
		}
		if (date_to != null) {
			long to = shortSdfApi.parse(date_to).getTime();
			criterias.add(new Criteria("personalData.timestamp").lte(to));
		}
		if (!criterias.isEmpty()) {
			Criteria[] cs = criterias.toArray(new Criteria[criterias.size()]);
			criteria = criteria.andOperator(cs);
		}

		Query query = new Query(criteria);

		List<Player> players = template.find(query, Player.class);

		List<PlayerProfile> profiles = players.stream().map(x -> convertPlayer(x, year)).collect(Collectors.toList());
		Collections.sort(profiles);

		return profiles;
	}

	@Scheduled(cron="0 0 14 * * *")
//	@PostConstruct
	public void generateWaypoints() throws Exception {
		cryptUtils = new EncryptDecrypt(secretKey1, secretKey2);
		
		Runnable r= new Runnable() {
			
			@Override
			public void run() {
				logger.info("Starting waypoints generation");
				List<String> campaignIds = appSetup.getApps().stream().map(x -> x.getAppId()).collect(Collectors.toList());
				for (String campaignId: campaignIds) {
					try {
						generateWaypoints(campaignId);
					} catch (Exception e) {
						logger.info("Error generating waypoints");
					}
				}
				logger.info("Ended waypoints generation");
			}
		};
		
		Thread t = new Thread(r);
		t.start();
		
	}
	
	
	public void generateWaypoints(String campaignId) throws Exception {
		AppInfo app = appSetup.findAppById(campaignId);
		String gameId = app.getGameId();
		if (gameId == null) {
			return;
		}
		
		Stopwatch sw = Stopwatch.createStarted();
		int created = 0;
		
		logger.info("Generating waypoints for " + campaignId);

		Date startGame = shortSdfGameInfo.parse(gameSetup.findGameById(gameId).getStart());		
		
		String currentMonth = monthSdf.format(new Date());
		String gameStart = shortSdfDb.format(startGame);		
		
		SortedSet<String> months = findMonths(campaignId, gameStart);		
		
		Criteria criteria0 = new Criteria("gameId").is(gameId);
		Query query0 = new Query(criteria0);
		List<Player> players = template.find(query0, Player.class);
//		List<Player> players = template.findAll(Player.class);
		Collections.sort(players, new Comparator<Player>() {

			@Override
			public int compare(Player o1, Player o2) {
				return Strings.padStart(o1.getPlayerId(), 6, '0').compareTo(Strings.padStart(o2.getPlayerId(), 6, '0'));
			}
		});
		
		for (String month: months) {
			created += createMissingMonth(month, currentMonth, campaignId, null);
		}
		
		sw.stop();
		logger.info("Total Waypoints generated: " + created + ", time elapsed: " + sw.elapsed(TimeUnit.SECONDS));
		
	}

	private PlayerProfile convertPlayer(Player p, int year) {
		PlayerProfile pp = new PlayerProfile();
		try {
			pp.setUser_id(cryptUtils.encrypt(p.getPlayerId()));
		} catch (Exception e) {
			logger.error("Error encrypting player id", e);
		}
		Long rd = (Long) p.getPersonalData().getOrDefault("timestamp", 0);
		pp.setDate_of_registration(shortSdfApi.format(rd));

		completeWithSurvey(pp, p, year);

		return pp;
	}

	private void completeWithSurvey(PlayerProfile pp, Player p, int year) {
		if (!p.getSurveys().containsKey("start")) {
			return;
		}
		Map<String, Object> survey = p.getSurveys().get("start");
		String gender;
		switch ((String) survey.getOrDefault("gender", "")) {
		case "maschio":
			gender = "male";
			break;
		case "femmina":
			gender = "female";
			break;
		default:
			gender = "unknown";
		}
		pp.setGender(gender);

		if (survey.containsKey("age")) {
			String age = (String) survey.get("age");
			if (age.contains("+")) {
				pp.setYear_of_birth_max(findYear(age.replace("+", ""), year));
			} else {
				String years[] = age.split("-");
				pp.setYear_of_birth_min(findYear(years[1], year));
				pp.setYear_of_birth_max(findYear(years[0], year));
			}
		}

		if (survey.containsKey("mean")) {
			String mean = null;
			switch ((String) survey.get("mean")) {
			case "auto":
				mean = "private car";
				break;
			case "public":
				mean = "public transport";
				break;
			case "bike":
				mean = "bike";
				break;
			case "walk":
				mean = "walking";
				break;
			case "bikesharing":
				mean = "bike sharing";
				break;
			case "carsharing":
				mean = "car sharing/pooling";
				break;
			}
			pp.setMain_mode_pre_campaign(mean);
		}

		if (survey.containsKey("kms")) {
			String kms = (String) survey.get("kms");
			if (kms.contains("+")) {
				pp.setDaily_travelled_distance_min(kms.replace("+", ""));
			} else {
				String km[] = kms.split("-");
				pp.setDaily_travelled_distance_min(km[0]);
				pp.setDaily_travelled_distance_max(km[1]);
			}
		}
	}

	private String findYear(String age, int year) {
		return "" + (year - Integer.parseInt(age));
	}

	private int createMissingMonth(String date, String currentDate, String campaignId, List<Player> players) throws Exception {
		List<PlayerWaypoints> result = Lists.newArrayList();

		String from = date + "/01";
		String to = date + "/31";
		
		String suffix = date.replace("/", "-");
		File f = new File(waypointsDir + "/" + campaignId + "_" + suffix + ".zip");
		if (f.exists()) {
			if (suffix.equals(currentDate)) {
				logger.info("Overwriting current waypoints " + suffix);
			} else {
				logger.info("Skipping existing waypoints " + suffix);
				return 0;
			}
		} else {
			logger.info("Writing missing waypoints " + suffix);
		}
		
		Stopwatch sw = Stopwatch.createStarted();
		
		FileOutputStream fos0 = new FileOutputStream(f);
		ZipOutputStream fos = new ZipOutputStream(fos0);
		ZipEntry ze = new ZipEntry(campaignId + "_" + suffix + ".json");
		fos.putNextEntry(ze);
		
		ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
		try (SequenceWriter sequenceWriter = writer.writeValues(fos)) {
			sequenceWriter.init(true);
			
			if (players != null) {
			for (Player p : players) {
				Criteria criteria = new Criteria("appId").is(campaignId).and("userId").is(p.getPlayerId()).and("freeTrackingTransport").ne(null);
				Criteria criteriaFrom = new Criteria("day").gte(from);
				Criteria criteriaTo = new Criteria("day").lte(to);
				
				criteria = criteria.andOperator(criteriaFrom, criteriaTo);

				Query query = new Query(criteria);

				CloseableIterator<TrackedInstance> it = template.stream(query, TrackedInstance.class);
				while (it.hasNext()) {
					TrackedInstance ti = it.next();
					PlayerWaypoints pws = convertTrackedInstance(ti, sequenceWriter);
					result.add(pws);
				}
			}
			} else {
				Criteria criteria = new Criteria("appId").is(campaignId).and("freeTrackingTransport").ne(null);
				Criteria criteriaFrom = new Criteria("day").gte(from);
				Criteria criteriaTo = new Criteria("day").lte(to);
				
				criteria = criteria.andOperator(criteriaFrom, criteriaTo);

				Query query = new Query(criteria);

				CloseableIterator<TrackedInstance> it = template.stream(query, TrackedInstance.class);
				while (it.hasNext()) {
					TrackedInstance ti = it.next();
					PlayerWaypoints pws = convertTrackedInstance(ti, sequenceWriter);
					result.add(pws);
				}
			}
		}

		sw.stop();
		logger.info("Waypoints generated: " + result.size() + ", time elapsed: " + sw.elapsed(TimeUnit.SECONDS));
		return result.size();
	}
	
	private SortedSet<String> findMonths(String appId, String gameStart) {
		Criteria criteria = new Criteria("appId").is(appId).and("freeTrackingTransport").ne(null).and("day").gte(gameStart);
		Query query = new Query(criteria);
		query.fields().include("day");
		
		Set<String> dates = template.find(query, TrackedInstance.class).stream().map(x -> x.getDay().substring(0, x.getDay().lastIndexOf("/"))).sorted().collect(Collectors.toSet());
		SortedSet<String> sortedDates = Sets.newTreeSet(dates);
		return sortedDates;
	}	

	private PlayerWaypoints convertTrackedInstance(TrackedInstance ti, SequenceWriter writer) throws Exception {
		PlayerWaypoints pws = new PlayerWaypoints();

		try {
			pws.setUser_id(cryptUtils.encrypt(ti.getUserId()));
		} catch (Exception e) {
			logger.error("Error encrypting player id", e);
		}
		pws.setActivity_id(ti.getId());
		pws.setActivity_type(ti.getFreeTrackingTransport());		
		for (Geolocation loc : ti.getGeolocationEvents()) {
			PlayerWaypoint pw = new PlayerWaypoint();

			pw.setLatitude(loc.getLatitude());
			pw.setLongitude(loc.getLongitude());
			pw.setTimestamp(extendedSdf.format(loc.getRecorded_at()));
			pw.setAccuracy(loc.getAccuracy());
			pw.setSpeed(loc.getSpeed());
			pw.setWaypoint_activity_confidence(loc.getActivity_confidence());
			pw.setWaypoint_activity_type(loc.getActivity_type());

			pws.getWaypoints().add(pw);
		}
		writer.write(pws);

		return pws;
	}

}
