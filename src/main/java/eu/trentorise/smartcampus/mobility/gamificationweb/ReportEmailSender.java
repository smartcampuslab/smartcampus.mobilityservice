package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept.ChallengeDataType;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.MailImage;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Notification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConceptPeriod;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Summary;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekConfData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekPrizeData;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ConfigUtils;

@RestController
@EnableScheduling
public class ReportEmailSender {

	private static final String IMAGE_PNG = "image/png";

	/**
	 * 
	 */
	private static final String START_SURVEY = "start";

	@Autowired
	private WebLinkUtils utils;
	
	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;
	@Autowired
	@Value("${mobilityURL}")
	private String mobilityUrl;

	@Value("${mail.send}")
	private boolean mailSend;
	@Value("${mail.to}")
	private String mailTo;
	@Autowired
	@Value("${mail.redirectUrl}")
	private String mailRedirectUrl;

	@Autowired
	@Value("${certificatesDir}")
	private String certificatesDir;	
	
	private static final String ITA_LANG = "it";
	private static final String ENG_LANG = "en";

	@Autowired
	private StatusUtils statusUtils;
	
	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;

	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;

	@Autowired
	private EmailService emailService;
	
	@Autowired
	private BadgesCache badgesCache;
	
	@Autowired
	private ConfigUtils configUtils;

	private static final Logger logger = Logger.getLogger(ReportEmailSender.class);

	private Map<String, List<WeekPrizeData>> weekPrizeData = new HashMap<>();
	
	@GetMapping("/gamificationweb/test1")
	public void sendNotification() throws Exception {
		sendWeeklyNotification();
		System.out.println("DONE");
	}
//	
//	@GetMapping("/gamificationweb/test2")
//	public synchronized void sendPDFMail() throws Exception {
//		sendPDFReportMail();
//		System.out.println("DONE");
//	}	
	
	@Scheduled(cron="0 0 16 * * FRI")
	public void sendWeeklyNotification() throws Exception {
		logger.info("Sending weekly notifications");
		for (AppInfo appInfo : appSetup.getApps()) {
			try {
				if (appInfo.getGameId() != null && !appInfo.getGameId().isEmpty()) {
					GameInfo game = gameSetup.findGameById(appInfo.getGameId());
					if (game.getSend() == null || !game.getSend()) {
						logger.info("Skipping email for " + appInfo.getAppId() + ", " + game.getId());
						continue;
					}
					logger.info("Sending email for " + appInfo.getAppId() + ", " + game.getId());
					sendWeeklyNotification(appInfo.getAppId());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
	
//	@Scheduled(cron="0 45 15 14 3 ?")
	public void sendPDFReportMail() throws Exception {
		logger.info("Sending PDF email");
		for (AppInfo appInfo : appSetup.getApps()) {
			logger.info("Sending PDF for app " + appInfo.getAppId());
			try {
				if (appInfo.getGameId() != null && !appInfo.getGameId().isEmpty()) {
					sendPDFReportMail(appInfo.getAppId());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
	
	
	/**
	 * Send a generic mail to all the subscribed users
	 * @param body
	 * @param subject
	 * @param appId
	 */
	public void sendGenericMailToAll(String body, String subject, String appId) {
		logger.info("Sending generic mail to all");
		String gameId = getGameId(appId);
		Iterable<Player> iter = playerRepositoryDao.findAllByGameId(gameId);

		for (Player p : iter) {
			logger.debug(String.format("Profile found  %s", p.getNickname()));

			if (p.isSendMail()) {
				try {
					emailService.sendGenericMail(body, subject, p.getNickname(), p.getMail(), Locale.forLanguageTag(getPlayerLang(p)));
				} catch (Exception e) {
					logger.error("Failed to send message to "+p.getMail(), e);
				}
			}
		}	
	}

	/**
	 * Send a generic mail to the specific list of users. If one of the users does not exist or is not subscribed, error is returned
	 * @param body
	 * @param subject
	 * @param appId
	 * @param emails
	 */
	public void sendGenericMailToUsers(String body, String subject, String appId, Set<String> emails) {
		logger.info("Sending generic mail to users: "+emails);
		String gameId = getGameId(appId);
		for (String email: emails) {
			Player p = playerRepositoryDao.findByGameIdAndMail(gameId, email);
			if (p == null || !p.isSendMail()) {
				throw new IllegalArgumentException("Incorrect email " + email);
			}
			logger.debug(String.format("Profile found  %s", p.getNickname()));

			if (p.isSendMail()) {
				try {
					emailService.sendGenericMail(body, subject, p.getNickname(), p.getMail(), Locale.forLanguageTag(getPlayerLang(p)));
				} catch (Exception e) {
					logger.error("Failed to send message to "+p.getMail(), e);
				}
			}
		}	
	}

	public void sendWeeklyNotification(String appId) throws Exception {
		LocalDate now = LocalDate.now();
		List<Summary> summaryMail = Lists.newArrayList();
		long millis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // Delta in millis of one week //long millis = 1415660400000L; //(for test)

		URL resource = getClass().getResource("/public/");
		String path = resource.getPath();

		List<MailImage> standardImages = Lists.newArrayList();

		standardImages.add(new MailImage("foglie03", Resources.asByteSource(Resources.getResource("public/img/mail/foglie03.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("foglie04", Resources.asByteSource(Resources.getResource("public/img/mail/foglie04.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("greenScore", Resources.asByteSource(Resources.getResource("public/img/mail/green/greenLeavesbase.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("healthScore", Resources.asByteSource(Resources.getResource("public/img/mail/health/healthLeavesBase.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("prScore", Resources.asByteSource(Resources.getResource("public/img/mail/pr/prLeaves.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("footer", Resources.asByteSource(Resources.getResource("public/img/mail/templateMail.png")).read(), IMAGE_PNG));

		List<WeekConfData> mailConfigurationFileData = new ArrayList<>(configUtils.getWeekConfData());

		List<WeekPrizeData> mailPrizeActualData = Lists.newArrayList();
		String current_week_theme = "";

		WeekConfData nextWeekConfData = mailConfigurationFileData.stream().filter(x -> x.nextWeek()).findFirst().orElse(new WeekConfData());
		WeekConfData currentWeekConfData = mailConfigurationFileData.stream().filter(x -> x.currentWeek()).findFirst().orElse(new WeekConfData());

		String gameId = getGameId(appId);
		Iterable<Player> iter = playerRepositoryDao.findAllByGameId(gameId);

		logger.info("Sending notifications for game " + gameId);

		for (Player p : iter) {
			logger.info("Sending notifications to " + p.getNickname());
			logger.debug(String.format("Profile found  %s", p.getNickname()));

			if (!p.isSendMail()) {
				logger.info("Mail non inviata a " + p.getNickname() + ". L'utente ha richiesto la disattivazione delle notifiche.");
				continue;
			}

			String compileSurveyUrl = utils.createSurveyUrl(p.getPlayerId(), gameId, START_SURVEY, getPlayerLang(p));
			String unsubcribeLink = utils.createUnsubscribeUrl(p.getPlayerId(), gameId);
			List<Notification> notifications = null;
			List<BadgesData> someBadge = null;
			List<ChallengesData> challenges = null;
			List<ChallengesData> lastWeekChallenges = null;
			Locale mailLoc = Locale.ITALIAN;

			String urlWSState = "gengine/state/" + gameId + "/" + p.getPlayerId();

			String completeState = getAllChallenges(urlWSState, appId);
			String language = p.getLanguage();
			if (language == null || language.isEmpty()) {
				language = ITA_LANG;
			}

			switch (language) {
			case ENG_LANG:
				current_week_theme = nextWeekConfData.getWeekThemeEng();
				mailLoc = Locale.ENGLISH;
				mailPrizeActualData = configUtils.getWeekPrizes(nextWeekConfData.getWeekNum(), ENG_LANG);
				break;
			default:
				current_week_theme = nextWeekConfData.getWeekTheme();
				mailLoc = Locale.ITALIAN;
				mailPrizeActualData = configUtils.getWeekPrizes(nextWeekConfData.getWeekNum(), ITA_LANG);
			}

			PlayerStatus completePlayerStatus = statusUtils.convertPlayerData(completeState, p.getPlayerId(), gameId, p.getNickname(), mobilityUrl + "/gamificationweb/", 0, language);
			List<PointConcept> states = completePlayerStatus.getPointConcept();
			int point_green = 0;
			int point_green_w = 0;
			if (states != null && states.size() > 0) {
				point_green = states.get(0).getScore();
				LocalDate cws = LocalDate.parse(currentWeekConfData.getWeekStart());
				LocalDate cwe = LocalDate.parse(currentWeekConfData.getWeekEnd());
				
				PointConceptPeriod pcp = states.get(0).getInstances().stream().filter(x -> {
					LocalDate ws = Instant.ofEpochMilli(x.getStart()).atZone(ZoneId.systemDefault()).toLocalDate();
					LocalDate we = Instant.ofEpochMilli(x.getEnd()).atZone(ZoneId.systemDefault()).toLocalDate();
					return (cwe.compareTo(we) <= 0) && (cws.compareTo(ws) == 0);
				}).findFirst().orElse(null);
				
				if (pcp != null) {
					point_green_w = pcp.getScore();
				}
			}
			
			
			ChallengeConcept challLists = completePlayerStatus.getChallengeConcept();

			if (challLists != null) {
				challenges = challLists.getChallengeData().get(ChallengeDataType.ACTIVE);
				if (challenges != null) {
					challenges = challenges.stream().filter(x -> !x.getSuccess().booleanValue()).collect(Collectors.toList());
				}
				lastWeekChallenges = challLists.getChallengeData().get(ChallengeDataType.OLD);
			}

			notifications = getBadgeNotifications(appId, p.getPlayerId());

			if (notifications != null && !notifications.isEmpty()) {
				List<BadgesData> allBadge = getAllBadges(path);
				someBadge = checkCorrectBadges(allBadge, notifications);
			}

			String mailto = null;
			mailto = p.getMail();
			String playerName = p.getNickname();
			if (mailto == null || mailto.isEmpty()) {
				mailto = mailTo;
			}

			if (mailSend && playerName != null && !playerName.isEmpty()) { // && !noMailingPlayers.contains(p.getSocialId())
				try {
					this.emailService.sendMailGamification(playerName, point_green, point_green_w, nextWeekConfData, 
							current_week_theme, (notifications != null ? someBadge : null), challenges, lastWeekChallenges, mailPrizeActualData, standardImages, mailto,
							mailRedirectUrl, compileSurveyUrl, unsubcribeLink, mailLoc);
				} catch (MessagingException e) {
					logger.error(String.format("Errore invio mail : %s", e.getMessage()));
				}
			} 
		}

	}
	
	public String getPlayerLang(Player p) {
		return p.getLanguage() != null ? p.getLanguage() : ITA_LANG;
	}

	private synchronized void sendPDFReportMail(String appId) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {

		boolean showFinalEvent = true; // (System.currentTimeMillis() <= millisNoEvent) ? true : false;

		URL resource = getClass().getResource("/");
		String path = resource.getPath();
		logger.debug(String.format("class path : %s", path));

		List<MailImage> standardImages = Lists.newArrayList();

		standardImages.add(new MailImage("foglie03", Resources.asByteSource(Resources.getResource("public/img/mail/foglie03.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("foglie04", Resources.asByteSource(Resources.getResource("public/img/mail/foglie04.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("greenScore", Resources.asByteSource(Resources.getResource("public/img/mail/green/greenLeavesbase.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("healthScore", Resources.asByteSource(Resources.getResource("public/img/mail/health/healthLeavesBase.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("prScore", Resources.asByteSource(Resources.getResource("public/img/mail/pr/prLeaves.png")).read(), IMAGE_PNG));
		standardImages.add(new MailImage("footer", Resources.asByteSource(Resources.getResource("public/img/mail/templateMail.png")).read(), IMAGE_PNG));

		String gameId = getGameId(appId);
		List<Player> iter = playerRepositoryDao.findAllByGameId(gameId);
		logger.info("Found " + iter.size() + " players for game " + gameId);

		for (Player p : iter) {
			logger.info(String.format("Profile found  %s", p.getNickname()));

			if (!p.isSendMail()) {
				logger.info("Mail non sent to " + p.getNickname() + " (" + p.getPlayerId() + "). Email notifications are disabled.");
				continue;
			}

			String moduleName = certificatesDir + "/Certificato_TrentoRoveretoPlayAndGo_" + p.getPlayerId() + ".pdf";
			try {
				File finalModule = new File(moduleName);
				// String compileSurveyUrl = utils.createSurveyUrl(p.getId(), gameId, START_SURVEY, getPlayerLang(p));
				// List<State> states = null;
				Locale mailLoc = Locale.ITALIAN;

				try {
					String language = p.getLanguage();
					if (language == null || language.isEmpty()) {
						language = ITA_LANG;
						mailLoc = Locale.ITALIAN;
					} else {
						mailLoc = Locale.ENGLISH;
					}

					String mailto = null;
					mailto = p.getMail();
					String playerName = p.getNickname();
					if (mailto == null || mailto.isEmpty()) {
						mailto = mailTo;
					}
					// TODO FIXME !! should be dynamic but for which survey
					Boolean surveyCompiled = false;// (p.getSurveyData() != null) ? true : false;

					if (mailSend && playerName != null && !playerName.isEmpty()) { // && !noMailingPlayers.contains(p.getSocialId())
						try {
							this.emailService.sendMailGamificationWithReport(playerName, finalModule, standardImages, mailto, mailRedirectUrl, showFinalEvent, mailLoc);

							logger.info("Mail Sent to " + p.getNickname() + " (" + p.getPlayerId() + ").");
						} catch (MessagingException e) {
							logger.error(String.format("Error sending email : %s", e.getMessage()));
						}
					}
				} catch (Exception ex) {
					logger.info("Mail not sent to " + p.getNickname() + " (" + p.getPlayerId() + "). PDF not found.");
				}

			} catch (Exception e) {
				logger.error("Error sending email", e);
			}
		}
	}
	
	private List<BadgesData> getAllBadges(String path) throws IOException {
		return badgesCache.getAllBadges();
	}

	private List<BadgesData> checkCorrectBadges(List<BadgesData> allB, List<Notification> notifics) throws IOException {
		List<BadgesData> correctBadges = Lists.newArrayList();

		for (Notification n: notifics) {
			correctBadges.addAll(allB.stream().filter(x -> n.getBadge().equals(x.getTextId())).collect(Collectors.toList()));
		}
		
		return correctBadges;
	}

	@SuppressWarnings("serial")
	HttpHeaders createHeaders(String appId) {
		return new HttpHeaders() {
			{
				AppInfo app = appSetup.findAppById(appId);
				GameInfo game = gameSetup.findGameById(app.getGameId());
				String auth = game.getUser() + ":" + game.getPassword();
				byte[] encodedAuth = Base64.encode(auth.getBytes(Charset.forName("UTF-8")));
				String authHeader = "Basic " + new String(encodedAuth);
				set("Authorization", authHeader);
			}
		};
	}

	/**
	 * Method used to retrieve the state of a specific user and to send the find data via mail
	 * 
	 * @param urlWS:
	 *            url of the ws
	 * @return string complete state
	 * @throws InterruptedException
	 */
	private String getAllChallenges(String urlWS, String appId) throws InterruptedException {
		RestTemplate restTemplate = new RestTemplate();
		logger.debug("Challenges WS GET " + urlWS);
		String result = "";
		ResponseEntity<String> tmp_res = null;
		try {
			tmp_res = restTemplate.exchange(gamificationUrl + urlWS, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
		} catch (Exception ex) {
			logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage()));
		}
		result = tmp_res.getBody();
		return result;
	}

	@SuppressWarnings("rawtypes")
	private List<Notification> getBadgeNotifications(String appId, String userId) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		AppInfo app = appSetup.findAppById(appId);
		
		String url = gamificationUrl + "notification/game/" + app.getGameId() + "/player/" + userId + "?includeTypes=BadgeNotification&fromTs=0";
		
		RestTemplate restTemplate = new RestTemplate();
		
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);		
		
		List nots = mapper.readValue(res.getBody(), List.class);
		List<Notification> notifications = Lists.newArrayList();
		
		for (Object not: nots) {
			Notification msg = mapper.convertValue(not, Notification.class);
			notifications.add(msg);
		}		
		
		return notifications;
		
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
	
}
