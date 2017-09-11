package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.MailImage;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Notification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Summary;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekConfData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekPrizeData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekWinnersData;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ConfigUtils;

@Controller
@EnableScheduling
public class ReportEmailSender {

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
	@Value("${weeklyDataDir}")
	private String weeklyDataDir;	

	private static final String ITA_LANG = "it";
	private static final String ENG_LANG = "en";

	@Autowired
	private StatusUtils statusUtils;
	
	private int i = 0;

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
	private List<WeekConfData> weekConfData = null;
	
//	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/test1")
//	public synchronized void sendNotification() throws Exception {
//		sendWeeklyNotification();
//		System.out.println("DONE");
//	}
	
//	@Scheduled(cron="0 10 10 * * *")
	@Scheduled(cron="0 0 17 * * FRI")
	public void sendWeeklyNotification() throws Exception {
//		System.err.println("TIME " + new Date());
		logger.info("Sending weekly notifications");
		for (AppInfo appInfo : appSetup.getApps()) {
			logger.info("Sending notifications for app " + appInfo.getAppId());
			try {
				sendWeeklyNotification(appInfo.getAppId());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
	
//	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/test2")
//	public synchronized void checkWinnersNotification() throws Exception {
//		for (AppInfo appInfo : appSetup.getApps()) {
//			checkWinnersNotification(appInfo.getAppId());
//		}
//	}	
//	
//	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/test3")
//	public synchronized void sendReportMail() throws Exception {
//		for (AppInfo appInfo : appSetup.getApps()) {
//			sendReportMail(appInfo.getAppId());
//		}
//	}		
	

	// Here I insert a task that invoke the WS notification
	// @Scheduled(fixedRate = 5*60*1000) // Repeat every 5 minutes
	// @Scheduled(cron="0 0 17 * * FRI") // Repeat every Friday at 17:00 PM
	public void sendWeeklyNotification(String appId) throws Exception {
		List<Summary> summaryMail = Lists.newArrayList();
		long millis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // Delta in millis of one week //long millis = 1415660400000L; //(for test)

		URL resource = getClass().getResource("/public/");
		String path = resource.getPath();
		
//		List<BadgesData> tmpBadge = getAllBadges(path);
		
		List<MailImage> standardImages = Lists.newArrayList();

		standardImages.add(new MailImage("foglie03", Resources.asByteSource(Resources.getResource("public/img/mail/foglie03.png")).read(), "image/png"));
		standardImages.add(new MailImage("foglie04", Resources.asByteSource(Resources.getResource("public/img/mail/foglie04.png")).read(), "image/png"));
		standardImages.add(new MailImage("greenScore", Resources.asByteSource(Resources.getResource("public/img/mail/green/greenLeavesbase.png")).read(), "image/png"));
		standardImages.add(new MailImage("healthScore", Resources.asByteSource(Resources.getResource("public/img/mail/health/healthLeavesBase.png")).read(), "image/png"));
		standardImages.add(new MailImage("prScore", Resources.asByteSource(Resources.getResource("public/img/mail/pr/prLeaves.png")).read(), "image/png"));
		standardImages.add(new MailImage("footer", Resources.asByteSource(Resources.getResource("public/img/mail/templateMail.png")).read(), "image/png"));

		// List<BadgesData> allBadgeTest = getAllBadges(path);
		// try {
		// this.emailService.sendMailGamification("NikName", "43", "32", "112", null, null, allBadgeTest, standardImages ,mailTo, Locale.ITALIAN);
		// } catch (MessagingException e1) {
		// e1.printStackTrace();
		// }
		// New method
		logger.debug(String.format("Check Notification task. Cycle - %d", i++));
		// Here I have to read the mail conf file data
		String conf_directory = "conf_file";
		List<WeekConfData> mailConfigurationFileData = new ArrayList<>(configUtils.getWeekConfData());
		
		List<WeekWinnersData> mailWinnersFileData = readWeekWinnersFile(weeklyDataDir + "/game_week_winners.csv");
		List<WeekPrizeData> mailPrizeActualData = Lists.newArrayList();
		// here I have to add the new mail parameters readed from csv files
		int actual_week = 0;
		String actual_week_theme = "";
		String actual_week_theme_it = "";
		String actual_week_theme_eng = "";
		int last_week = -1;
		Boolean are_chall = false;
		Boolean are_prizes = false;
		Boolean are_prizes_last_week = false;

		for (int i = 0; i < mailConfigurationFileData.size(); i++) {
			WeekConfData tmpWConf = mailConfigurationFileData.get(i);
			if (tmpWConf.currentWeek()) {
				actual_week = tmpWConf.getWeekNum();
				actual_week_theme_it = tmpWConf.getWeekTheme();
				actual_week_theme_eng = tmpWConf.getWeekThemeEng();
				last_week = actual_week - 1;
				are_chall = tmpWConf.isChallenges();
				are_prizes = tmpWConf.isPrizes();
				are_prizes_last_week = tmpWConf.isPrizesLast();
				mailPrizeActualData = getWeekPrizes(actual_week, ITA_LANG);
			}
		}

		String gameId = getGameId(appId);
		Iterable<Player> iter = playerRepositoryDao.findAllByGameId(gameId);

		logger.info("Sending notifications for game " + gameId);
		
		for (Player p : iter) {
			logger.info("Sending notifications to " + p.getNickname());
			logger.debug(String.format("Profile found  %s", p.getNickname()));

			if (p.isSendMail()) {
				String compileSurveyUrl = utils.createSurveyUrl(p.getId(), gameId, START_SURVEY, getPlayerLang(p));
				String unsubcribeLink = utils.createUnsubscribeUrl(p.getId(), gameId);
				List<PointConcept> states = null;
				List<Notification> notifications = null;
				List<BadgesData> someBadge = null;
				List<ChallengesData> challenges = null;
				List<ChallengesData> lastWeekChallenges = null;
				Locale mailLoc = Locale.ITALIAN;

				try {
					// WS State Invocation
					String urlWSState = "gengine/state/" + gameId + "/" + p.getId();
					// states = getState(urlWSState);
					// Challenges correction
					String completeState = getAllChallenges(urlWSState, appId);
					String language = p.getLanguage();
					if (language == null || language.compareTo("") == 0) {
						language = ITA_LANG;
					}
					if (language.compareTo(ENG_LANG) == 0) {
						actual_week_theme = actual_week_theme_eng;
						mailLoc = Locale.ENGLISH;
						mailPrizeActualData = getWeekPrizes(actual_week, ENG_LANG);
					} else {
						actual_week_theme = actual_week_theme_it;
						mailLoc = Locale.ITALIAN;
						mailPrizeActualData = getWeekPrizes(actual_week, ITA_LANG);
					}
					try {
						PlayerStatus completePlayerStatus = statusUtils.correctPlayerData(completeState, p.getId(), gameId, p.getNickname(), mobilityUrl + "/gamificationweb/", 0, language);
						states = completePlayerStatus.getPointConcept();
						ChallengeConcept challLists = completePlayerStatus.getChallengeConcept();
						// @SuppressWarnings("rawtypes")
						// List<List> challLists = challUtils.correctCustomData(completeState, 0);
						if (challLists != null) {
							challenges = challLists.getActiveChallengeData();
							lastWeekChallenges = challLists.getOldChallengeData();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}

					// WS Notification Invocation
					notifications = getBadgeNotifications(millis, appId, p.getId());
				} catch (InterruptedException ie) {
					logger.error(String.format("Ws invoke sleep exception  %s", ie.getMessage()));
				}

				if (notifications != null && notifications.size() > 0) {
					List<BadgesData> allBadge = getAllBadges(path);
					someBadge = checkCorrectBadges(allBadge, notifications);
				}

				String mailto = null;
				mailto = p.getMail();
				String playerName = p.getNickname();
				if (mailto == null || mailto.compareTo("") == 0) {
					mailto = mailTo;
				}
				// if(specialPlayers.contains(p.getSocialId())){
				if (mailSend && playerName != null && !playerName.isEmpty()) { // && !noMailingPlayers.contains(p.getSocialId())
					try {
						if (notifications != null) {
							if (states != null && states.size() > 0) {
								this.emailService.sendMailGamification(playerName, states.get(0).getScore() + "", null, null, null, null, // health and pr point are null
										actual_week, actual_week_theme, last_week, are_chall, are_prizes, are_prizes_last_week, someBadge, challenges, lastWeekChallenges, mailPrizeActualData,
										mailWinnersFileData, standardImages, mailto, mailRedirectUrl, compileSurveyUrl, unsubcribeLink, mailLoc);
							} else {
								this.emailService.sendMailGamification(playerName, "0", "0", "0", null, null, actual_week, actual_week_theme, last_week, are_chall, are_prizes,
										are_prizes_last_week, someBadge, challenges, lastWeekChallenges, mailPrizeActualData, mailWinnersFileData, standardImages, mailto, mailRedirectUrl,
										compileSurveyUrl, unsubcribeLink, mailLoc);
							}
						} else {
							if (states != null && states.size() > 0) {
								this.emailService.sendMailGamification(playerName, states.get(0).getScore() + "", null, null, null, null, // health and pr point are null
										actual_week, actual_week_theme, last_week, are_chall, are_prizes, are_prizes_last_week, null, challenges, lastWeekChallenges, mailPrizeActualData,
										mailWinnersFileData, standardImages, mailto, mailRedirectUrl, compileSurveyUrl, unsubcribeLink, mailLoc);
							} else {
								this.emailService.sendMailGamification(playerName, "0", "0", "0", null, null, actual_week, actual_week_theme, last_week, are_chall, are_prizes,
										are_prizes_last_week, null, challenges, lastWeekChallenges, mailPrizeActualData, mailWinnersFileData, standardImages, mailto, mailRedirectUrl, compileSurveyUrl,
										unsubcribeLink, mailLoc);
							}
						}
					} catch (MessagingException e) {
						logger.error(String.format("Errore invio mail : %s", e.getMessage()));
					}
				} else {
					if (notifications != null) {
						if (states != null && states.size() > 0) {
							logger.debug(String.format("Invio mail a %s con notifica : %s e stato: %s", playerName, notifications.toString(), states.toString()));
						} else {
							logger.debug(String.format("Invio mail a %s con notifica : %s", playerName, notifications.toString()));
						}
					} else {
						if (states != null && states.size() > 0) {
							logger.debug(String.format("Invio mail a %s con stato: %s", playerName, states.toString()));
						} else {
							logger.debug(String.format("Invio mail a %s", playerName));
						}
					}
					if (challenges != null && !challenges.isEmpty()) {
						logger.debug(String.format("Invio mail a %s con challenges: %s", playerName, challenges.toString()));
					}
					if (lastWeekChallenges != null && !lastWeekChallenges.isEmpty()) {
						logger.debug(String.format("Invio mail a %s con challenges scorsa settimana: %s", playerName, lastWeekChallenges.toString()));
					}
				}
				summaryMail.add(new Summary(p.getName() + " " + p.getSurname() + ": " + p.getNickname(), (states != null && !states.isEmpty()) ? Integer.toString(states.get(0).getScore()) : "",
						(notifications != null) ? notifications.toString() : ""));
			} else {
				logger.info("Mail non inviata a " + p.getNickname() + ". L'utente ha richiesto la disattivazione delle notifiche.");
			}
		}

		// Send summary mail
//		if (mailSend && iter.iterator().hasNext()) {
//			// Here I send the summary mail (only if the sendMail parameter is true)
//			try {
//				this.emailService.sendMailSummary("Amministratore", "0", "0", "0", summaryMail, standardImages, mailTo, Locale.ITALIAN);
//			} catch (MessagingException e) {
//				logger.error(String.format("Errore invio mail notifica : %s", e.getMessage()));
//			}
//		} else {
//			logger.info("Ended mail sending process: no mail send (param in conf file set to false)");
//		}
		// }
	}
	
	public String getPlayerLang(Player p) {
		return p.getLanguage() != null ? p.getLanguage() : ITA_LANG;
	}

	// @Scheduled(fixedRate = 5*60*1000) // Repeat every 5 minutes
	// @Scheduled(cron="0 15 15 * * MON") // Repeat every Monday at 15:15
	public synchronized void checkWinnersNotification(String appId) throws Exception {
		List<Summary> summaryMail = Lists.newArrayList();
		long millis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // Delta in millis of N days: now 7 days
		
		URL resource = getClass().getResource("/");
		String path = resource.getPath();
		logger.debug(String.format("class path : %s", path));

		List<MailImage> standardImages = Lists.newArrayList();

		standardImages.add(new MailImage("foglie03", Resources.asByteSource(Resources.getResource("./public/img/mail/foglie03.png")).read(), "image/png"));
		standardImages.add(new MailImage("foglie04", Resources.asByteSource(Resources.getResource("./public/img/mail/foglie04.png")).read(), "image/png"));
		standardImages.add(new MailImage("greenScore", Resources.asByteSource(Resources.getResource("./public/img/mail/green/greenLeavesbase.png")).read(), "image/png"));
		standardImages.add(new MailImage("healthScore", Resources.asByteSource(Resources.getResource("./public/img/mail/health/healthLeavesBase.png")).read(), "image/png"));
		standardImages.add(new MailImage("prScore", Resources.asByteSource(Resources.getResource("./public/img/mail/pr/prLeaves.png")).read(), "image/png"));
		standardImages.add(new MailImage("footer", Resources.asByteSource(Resources.getResource("./public/img/mail/templateMail.png")).read(), "image/png"));		

		// New method
		logger.debug(String.format("Check Notification task. Cycle - %d", i++));
		// Here I have to read the mail conf file data
		String conf_directory = "conf_file";
		List<WeekConfData> mailConfigurationFileData = new ArrayList<>(configUtils.getWeekConfData());
		List<WeekWinnersData> mailWinnersFileData = readWeekWinnersFile(weeklyDataDir + "/game_week_winners.csv");		
		
		List<WeekPrizeData> mailPrizeActualData = Lists.newArrayList();
		// here I have to add the new mail parameters readed from csv files
		int actual_week = 0;
		String actual_week_theme = "";
		String actual_week_theme_it = "";
		String actual_week_theme_eng = "";
		int last_week = -1;
		Boolean are_chall = false;
		Boolean are_prizes = false;
		Boolean are_prizes_last_week = false;
		for (int i = 0; i < mailConfigurationFileData.size(); i++) {
			WeekConfData tmpWConf = mailConfigurationFileData.get(i);
			if (tmpWConf.currentWeek()) {
				actual_week = tmpWConf.getWeekNum();
				actual_week_theme_it = tmpWConf.getWeekTheme();
				actual_week_theme_eng = tmpWConf.getWeekThemeEng();
				last_week = actual_week - 1;
				are_chall = tmpWConf.isChallenges();
				are_prizes = tmpWConf.isPrizes();
				are_prizes_last_week = tmpWConf.isPrizesLast();
				mailPrizeActualData = getWeekPrizes(actual_week, ITA_LANG);
			}
		}
		String gameId = getGameId(appId);
		Iterable<Player> iter = playerRepositoryDao.findAllByGameId(gameId);

		for (Player p : iter) {
			logger.debug(String.format("Profile found  %s", p.getNickname()));

			if (p.isSendMail()) {
				String compileSurveyUrl = utils.createSurveyUrl(p.getId(), gameId, START_SURVEY, getPlayerLang(p));
				String unsubcribeLink = utils.createUnsubscribeUrl(p.getId(), gameId);
				// List<State> states = null;
				List<PointConcept> states = null;
				List<Notification> notifications = null;
				List<BadgesData> someBadge = null;
				List<ChallengesData> challenges = null;
				List<ChallengesData> lastWeekChallenges = null;
				Locale mailLoc = Locale.ITALIAN;

				try {
					// WS State Invocation
					String urlWSState = "gengine/state/" + gameId + "/" + p.getId();
					// states = getState(urlWSState);
					// Challenges correction
					String completeState = getAllChallenges(urlWSState, appId);
					String language = p.getLanguage();
					if (language == null || language.compareTo("") == 0) {
						language = ITA_LANG;
					}
					if (language.compareTo(ENG_LANG) == 0) {
						actual_week_theme = actual_week_theme_eng;
						mailLoc = Locale.ENGLISH;
						mailPrizeActualData = getWeekPrizes(actual_week, ENG_LANG);
					} else {
						actual_week_theme = actual_week_theme_it;
						mailLoc = Locale.ITALIAN;
						mailPrizeActualData = getWeekPrizes(actual_week, ITA_LANG);
					}
					try {
						PlayerStatus completePlayerStatus = statusUtils.correctPlayerData(completeState, p.getId(), gameId, p.getNickname(), mobilityUrl + "/gamificationweb/", 0, language);
						states = completePlayerStatus.getPointConcept();
						ChallengeConcept challLists = completePlayerStatus.getChallengeConcept();
						// @SuppressWarnings("rawtypes")
						// List<List> challLists = challUtils.correctCustomData(completeState, 0);
						if (challLists != null) {
							challenges = challLists.getActiveChallengeData();
							lastWeekChallenges = challLists.getOldChallengeData();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}

					// WS Notification Invocation
					notifications = getBadgeNotifications(millis, appId, p.getId());
				} catch (InterruptedException ie) {
					logger.error(String.format("Ws invoke sleep exception  %s", ie.getMessage()));
				}

				if (notifications != null && notifications.size() > 0) {
					List<BadgesData> allBadge = getAllBadges(path);
					someBadge = checkCorrectBadges(allBadge, notifications);
				}

				String mailto = null;
				mailto = p.getMail();
				String playerName = p.getNickname();
				if (mailto == null || mailto.compareTo("") == 0) {
					mailto = mailTo;
				}

				if (mailSend && playerName != null && !playerName.isEmpty()) { // && !noMailingPlayers.contains(p.getSocialId())
					try {
						if (notifications != null) {
							if (states != null && states.size() > 0) {
								this.emailService.sendMailGamificationForWinners(playerName, states.get(0).getScore() + "", null, null, null, null, // health and pr point are null
										actual_week, actual_week_theme, last_week, are_chall, are_prizes, are_prizes_last_week, someBadge, challenges, lastWeekChallenges, mailPrizeActualData,
										mailWinnersFileData, standardImages, mailto, mailRedirectUrl, compileSurveyUrl, unsubcribeLink, mailLoc);
							} else {
								this.emailService.sendMailGamificationForWinners(playerName, "0", "0", "0", null, null, actual_week, actual_week_theme, last_week, are_chall, are_prizes,
										are_prizes_last_week, someBadge, challenges, lastWeekChallenges, mailPrizeActualData, mailWinnersFileData, standardImages, mailto, mailRedirectUrl,
										compileSurveyUrl, unsubcribeLink, mailLoc);
							}
						} else {
							if (states != null && states.size() > 0) {
								this.emailService.sendMailGamificationForWinners(playerName, states.get(0).getScore() + "", null, null, null, null, // health and pr point are null
										actual_week, actual_week_theme, last_week, are_chall, are_prizes, are_prizes_last_week, null, challenges, lastWeekChallenges, mailPrizeActualData,
										mailWinnersFileData, standardImages, mailto, mailRedirectUrl, compileSurveyUrl, unsubcribeLink, mailLoc);
							} else {
								this.emailService.sendMailGamificationForWinners(playerName, "0", "0", "0", null, null, actual_week, actual_week_theme, last_week, are_chall, are_prizes,
										are_prizes_last_week, null, challenges, lastWeekChallenges, mailPrizeActualData, mailWinnersFileData, standardImages, mailto, mailRedirectUrl, compileSurveyUrl,
										unsubcribeLink, mailLoc);
							}
						}
					} catch (MessagingException e) {
						logger.error(String.format("Errore invio mail : %s", e.getMessage()));
					}
				} else {
					if (notifications != null) {
						if (states != null && states.size() > 0) {
							logger.debug(String.format("Invio mail a %s con notifica : %s e stato: %s", playerName, notifications.toString(), states.toString()));
						} else {
							logger.debug(String.format("Invio mail a %s con notifica : %s", playerName, notifications.toString()));
						}
					} else {
						if (states != null && states.size() > 0) {
							logger.debug(String.format("Invio mail a %s con stato: %s", playerName, states.toString()));
						} else {
							logger.debug(String.format("Invio mail a %s", playerName));
						}
					}
					if (challenges != null && !challenges.isEmpty()) {
						logger.debug(String.format("Invio mail a %s con challenges: %s", playerName, challenges.toString()));
					}
					if (lastWeekChallenges != null && !lastWeekChallenges.isEmpty()) {
						logger.debug(String.format("Invio mail a %s con challenges scorsa settimana: %s", playerName, lastWeekChallenges.toString()));
					}
				}
				summaryMail.add(new Summary(p.getName() + " " + p.getSurname() + ": " + p.getNickname(), (states != null && !states.isEmpty()) ? Integer.toString(states.get(0).getScore()) : "",
						(notifications != null) ? notifications.toString() : ""));
			} else {
				logger.info("Mail non inviata a " + p.getNickname() + ". L'utente ha richiesto la disattivazione delle notifiche.");
			}
		}
		// Send summary mail
		if (mailSend && iter.iterator().hasNext()) {
			// Here I send the summary mail (only if the sendMail parameter is true)
			try {
				this.emailService.sendMailSummary("Amministratore", "0", "0", "0", summaryMail, standardImages, mailTo, Locale.ITALIAN);
			} catch (MessagingException e) {
				logger.error(String.format("Errore invio mail notifica : %s", e.getMessage()));
			}
		}
	}

	// @Scheduled(fixedRate = 5*60*1000) // Repeat every 5 minutes
	// @Scheduled(cron="0 30 11 * * WED") // Repeat every WED at 11:30 AM
	public synchronized void sendReportMail(String appId) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
		List<Summary> summaryMail = Lists.newArrayList();
		long millis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // Delta in millis of N days: now 7 days
		long millisNoEvent = 1480978800000L; // Tue Dec 06 2016 00:00:00 GMT+0100
		boolean showFinalEvent = (System.currentTimeMillis() <= millisNoEvent) ? true : false;

		URL resource = getClass().getResource("/");
		String path = resource.getPath();
		logger.debug(String.format("class path : %s", path));

		List<MailImage> standardImages = Lists.newArrayList();

		standardImages.add(new MailImage("foglie03", Resources.asByteSource(Resources.getResource("./public/img/mail/foglie03.png")).read(), "image/png"));
		standardImages.add(new MailImage("foglie04", Resources.asByteSource(Resources.getResource("./public/img/mail/foglie04.png")).read(), "image/png"));
		standardImages.add(new MailImage("greenScore", Resources.asByteSource(Resources.getResource("./public/img/mail/green/greenLeavesbase.png")).read(), "image/png"));
		standardImages.add(new MailImage("healthScore", Resources.asByteSource(Resources.getResource("./public/img/mail/health/healthLeavesBase.png")).read(), "image/png"));
		standardImages.add(new MailImage("prScore", Resources.asByteSource(Resources.getResource("./public/img/mail/pr/prLeaves.png")).read(), "image/png"));
		standardImages.add(new MailImage("footer", Resources.asByteSource(Resources.getResource("./public/img/mail/templateMail.png")).read(), "image/png"));		
		
		String gameId = getGameId(appId);
		Iterable<Player> iter = playerRepositoryDao.findAllByGameId(gameId);

		for (Player p : iter) {
			logger.debug(String.format("Profile found  %s", p.getNickname()));

			if (p.isSendMail()) {
				String moduleName = "mail/certificates-pdf/Certificato_TrentoPlayAndGo_" + p.getId() + ".pdf";
				try {
					File finalModule = new File(path + moduleName);
					String compileSurveyUrl = utils.createSurveyUrl(p.getId(), gameId, START_SURVEY, getPlayerLang(p));
					String unsubcribeLink = utils.createUnsubscribeUrl(p.getId(), gameId);
					// List<State> states = null;
					List<PointConcept> states = null;
					List<Notification> notifications = null;
					List<ChallengesData> challenges = null;
					List<ChallengesData> lastWeekChallenges = null;
					Locale mailLoc = Locale.ITALIAN;

					try {
						// WS State Invocation
						String urlWSState = "gengine/state/" + gameId + "/" + p.getId();
						// Challenges correction
						String completeState = getAllChallenges(urlWSState, appId);
						String language = p.getLanguage();
						if (language == null || language.compareTo("") == 0) {
							language = ITA_LANG;
						}
						if (language.compareTo(ENG_LANG) == 0) {
							mailLoc = Locale.ENGLISH;
						} else {
							mailLoc = Locale.ITALIAN;
						}
						try {
							PlayerStatus completePlayerStatus = statusUtils.correctPlayerData(completeState, p.getId(), gameId, p.getNickname(), mobilityUrl + "/gamificationweb/", 0,
									language);
							states = completePlayerStatus.getPointConcept();
							ChallengeConcept challLists = completePlayerStatus.getChallengeConcept();
							if (challLists != null) {
								challenges = challLists.getActiveChallengeData();
								lastWeekChallenges = challLists.getOldChallengeData();
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}

						// WS Notification Invocation
						notifications = getBadgeNotifications(millis, appId, p.getId());
					} catch (InterruptedException ie) {
						logger.error(String.format("Ws invoke sleep exception  %s", ie.getMessage()));
					}

					String mailto = null;
					mailto = p.getMail();
					String playerName = p.getNickname();
					if (mailto == null || mailto.compareTo("") == 0) {
						mailto = mailTo;
					}
					// TODO FIXME !! should be dynamic but for which survey
					Boolean surveyCompiled = false;// (p.getSurveyData() != null) ? true : false;

					if (mailSend && playerName != null && !playerName.isEmpty()) { // && !noMailingPlayers.contains(p.getSocialId())
						try {
							if (states != null && states.size() > 0) {
								this.emailService.sendMailGamificationWithReport(playerName, states.get(0).getScore() + "", null, null, null, null, // health and pr point are null
										null, null, null, null, null, null, surveyCompiled, finalModule, challenges, lastWeekChallenges, null, null, standardImages, mailto, mailRedirectUrl,
										compileSurveyUrl, compileSurveyUrl, showFinalEvent, unsubcribeLink, mailLoc);
							} else {
								this.emailService.sendMailGamificationWithReport(playerName, "0", "0", "0", null, null, null, null, null, null, null, null, surveyCompiled, finalModule, challenges,
										lastWeekChallenges, null, null, standardImages, mailto, mailRedirectUrl, compileSurveyUrl, compileSurveyUrl, showFinalEvent, unsubcribeLink, mailLoc);
							}
						} catch (MessagingException e) {
							logger.error(String.format("Errore invio mail : %s", e.getMessage()));
						}
					}
					summaryMail.add(new Summary(p.getName() + " " + p.getSurname() + ": " + p.getNickname(), (states != null && !states.isEmpty()) ? Integer.toString(states.get(0).getScore()) : "",
							(notifications != null) ? notifications.toString() : ""));
				} catch (Exception ex) {
					logger.info("Mail non inviata a " + p.getNickname() + ". Non esiste il pdf del modulo finale.");
				}
			} else {
				logger.info("Mail non inviata a " + p.getNickname() + ". L'utente ha richiesto la disattivazione delle notifiche.");
			}
		}
		// Send summary mail
		if (mailSend && iter.iterator().hasNext()) {
			// Here I send the summary mail (only if the sendMail parameter is true)
			try {
				emailService.sendMailSummary("Amministratore", "0", "0", "0", summaryMail, standardImages, mailTo, Locale.ITALIAN);
			} catch (MessagingException e) {
				logger.error(String.format("Errore invio mail notifica : %s", e.getMessage()));
			}
		}
	}
	
	private List<BadgesData> getAllBadges(String path) throws IOException {
		return badgesCache.getAllBadges();
	}

//	private ArrayList<BadgesData> oldGetAllBadges(String path) throws IOException {
//		ArrayList<BadgesData> allBadges = new ArrayList<BadgesData>();
//		// files for green badges
//		File greenKing = new File(path + "img/mail/green/greenKingWeek.png");
//		File green50 = new File(path + "img/mail/green/greenLeaves50.png");
//		File green100 = new File(path + "img/mail/green/greenLeaves100.png");
//		File green200 = new File(path + "img/mail/green/greenLeaves200.png");
//		File green400 = new File(path + "img/mail/green/greenLeaves400.png");
//		File green800 = new File(path + "img/mail/green/greenLeaves800.png");
//		File green1500 = new File(path + "img/mail/green/greenLeaves1500.png");
//		File green2500 = new File(path + "img/mail/green/greenLeaves2500.png");
//		File green5000 = new File(path + "img/mail/green/greenLeaves5000.png");
//		File green10000 = new File(path + "img/mail/green/greenLeaves10000.png");
//		File green20000 = new File(path + "img/mail/green/greenLeaves20000.png");
//		File greenBronze = new File(path + "img/mail/leaderboard/green/leaderboardGreen3.png");
//		File greenSilver = new File(path + "img/mail/leaderboard/green/leaderboardGreen2.png");
//		File greenGold = new File(path + "img/mail/leaderboard/green/leaderboardGreen1.png");
//
//		allBadges.add(
//				new BadgesData(greenKing.getName(), FileUtils.readFileToByteArray(greenKing), "image/png", "king_week_green", "Re della Settimana - Green Leaves", "King of the Week - Green Leaves"));
//		allBadges.add(new BadgesData(green50.getName(), FileUtils.readFileToByteArray(green50), "image/png", "50_point_green", "50 Punti Green Leaves", "50 Green Leaves Points"));
//		allBadges.add(new BadgesData(green100.getName(), FileUtils.readFileToByteArray(green100), "image/png", "100_point_green", "100 Punti Green Leaves", "100 Green Leaves Points"));
//		allBadges.add(new BadgesData(green200.getName(), FileUtils.readFileToByteArray(green200), "image/png", "200_point_green", "200 Punti Green Leaves", "200 Green Leaves Points"));
//		allBadges.add(new BadgesData(green400.getName(), FileUtils.readFileToByteArray(green400), "image/png", "400_point_green", "400 Punti Green Leaves", "400 Green Leaves Points"));
//		allBadges.add(new BadgesData(green800.getName(), FileUtils.readFileToByteArray(green800), "image/png", "800_point_green", "800 Punti Green Leaves", "800 Green Leaves Points"));
//		allBadges.add(new BadgesData(green1500.getName(), FileUtils.readFileToByteArray(green1500), "image/png", "1500_point_green", "1500 Punti Green Leaves", "1500 Green Leaves Points"));
//		allBadges.add(new BadgesData(green2500.getName(), FileUtils.readFileToByteArray(green2500), "image/png", "2500_point_green", "2500 Punti Green Leaves", "2500 Green Leaves Points"));
//		allBadges.add(new BadgesData(green5000.getName(), FileUtils.readFileToByteArray(green5000), "image/png", "5000_point_green", "5000 Punti Green Leaves", "5000 Green Leaves Points"));
//		allBadges.add(new BadgesData(green10000.getName(), FileUtils.readFileToByteArray(green10000), "image/png", "10000_point_green", "10000 Punti Green Leaves", "10000 Green Leaves Points"));
//		allBadges.add(new BadgesData(green20000.getName(), FileUtils.readFileToByteArray(green20000), "image/png", "20000_point_green", "20000 Punti Green Leaves", "20000 Green Leaves Points"));
//		allBadges.add(new BadgesData(greenBronze.getName(), FileUtils.readFileToByteArray(greenBronze), "image/png", "bronze-medal-green", "Medaglia di Bronzo - Green Leaves",
//				"Bronze Medal - Green Leaves"));
//		allBadges.add(new BadgesData(greenSilver.getName(), FileUtils.readFileToByteArray(greenSilver), "image/png", "silver-medal-green", "Medaglia d'Argento - Green Leaves",
//				"Silver Medal - Green Leaves"));
//		allBadges.add(new BadgesData(greenGold.getName(), FileUtils.readFileToByteArray(greenGold), "image/png", "gold-medal-green", "Medaglia d'Oro - Green Leaves", "Gold Medal - Green Leaves"));
//
//		// files for health badges
//		File healthKing = new File(path + "img/mail/health/healthKingWeek.png");
//		File health10 = new File(path + "img/mail/health/healthLeaves10.png");
//		File health25 = new File(path + "img/mail/health/healthLeaves25.png");
//		File health50 = new File(path + "img/mail/health/healthLeaves50.png");
//		File health100 = new File(path + "img/mail/health/healthLeaves100.png");
//		File health200 = new File(path + "img/mail/health/healthLeaves200.png");
//		File healthBronze = new File(path + "img/mail/health/healthBronzeMedal.png");
//		File healthSilver = new File(path + "img/mail/health/healthSilverMedal.png");
//		File healthGold = new File(path + "img/mail/health/healthGoldMedal.png");
//
//		allBadges.add(new BadgesData(healthKing.getName(), FileUtils.readFileToByteArray(healthKing), "image/png", "king_week_health", "Re della Settimana - Salute", "King of the Week - Health"));
//		allBadges.add(new BadgesData(health10.getName(), FileUtils.readFileToByteArray(health10), "image/png", "10_point_health", "10 Punti Salute", "10 Health Points"));
//		allBadges.add(new BadgesData(health25.getName(), FileUtils.readFileToByteArray(health25), "image/png", "25_point_health", "25 Punti Salute", "25 Health Points"));
//		allBadges.add(new BadgesData(health50.getName(), FileUtils.readFileToByteArray(health50), "image/png", "50_point_health", "50 Punti Salute", "50 Health Points"));
//		allBadges.add(new BadgesData(health100.getName(), FileUtils.readFileToByteArray(health100), "image/png", "100_point_health", "100 Punti Salute", "100 Health Points"));
//		allBadges.add(new BadgesData(health200.getName(), FileUtils.readFileToByteArray(health200), "image/png", "200_point_health", "200 Punti Salute", "200 Health Points"));
//		allBadges.add(new BadgesData(healthBronze.getName(), FileUtils.readFileToByteArray(healthBronze), "image/png", "bronze_medal_health", "Medaglia di Bronzo - Salute", "Bronze Medal - Health"));
//		allBadges.add(new BadgesData(healthSilver.getName(), FileUtils.readFileToByteArray(healthSilver), "image/png", "silver_medal_health", "Medaglia d'Argento - Salute", "Silver Medal - Health"));
//		allBadges.add(new BadgesData(healthGold.getName(), FileUtils.readFileToByteArray(healthGold), "image/png", "gold_medal_health", "Medaglia d'Oro - Salute", "Gold Medal - Health"));
//
//		// files for pr badges
//		File prKing = new File(path + "img/mail/pr/prKingWeek.png");
//		File pr10 = new File(path + "img/mail/pr/prLeaves10.png");
//		File pr20 = new File(path + "img/mail/pr/prLeaves20.png");
//		File pr50 = new File(path + "img/mail/pr/prLeaves50.png");
//		File pr100 = new File(path + "img/mail/pr/prLeaves100.png");
//		File pr200 = new File(path + "img/mail/pr/prLeaves200.png");
//		File prBronze = new File(path + "img/mail/pr/prBronzeMedal.png");
//		File prSilver = new File(path + "img/mail/pr/prSilverMedal.png");
//		File prGold = new File(path + "img/mail/pr/prGoldMedal.png");
//		File prManifattura = new File(path + "img/mail/pr/prPioneerManifattura.png");
//		File prStadio = new File(path + "img/mail/pr/prPioneerStadio.png");
//		File prRagazzi99 = new File(path + "img/mail/pr/prPioneerRagazzi99.png");
//		File prLidorno = new File(path + "img/mail/pr/prPioneerLidorno.png");
//		File prViaFersina = new File(path + "img/mail/pr/prPioneerViaFersina.png");
//		File prAreaZuffo = new File(path + "img/mail/pr/prPioneerAreaZuffo.png");
//		File prMonteBaldo = new File(path + "img/mail/pr/prPioneerMonteBaldo.png");
//		File prVillazzanoFS = new File(path + "img/mail/pr/prPioneerVillazzanoStazioneFS.png");
//
//		allBadges.add(new BadgesData(prKing.getName(), FileUtils.readFileToByteArray(prKing), "image/png", "king_week_pr", "Re della Settimana - Park&Ride", "King of the Week - Park&Ride"));
//		allBadges.add(new BadgesData(pr10.getName(), FileUtils.readFileToByteArray(pr10), "image/png", "10_point_pr", "10 Punti Park&Ride", "10 Park&Ride Points"));
//		allBadges.add(new BadgesData(pr20.getName(), FileUtils.readFileToByteArray(pr20), "image/png", "20_point_pr", "20 Punti Park&Ride", "20 Park&Ride Points"));
//		allBadges.add(new BadgesData(pr50.getName(), FileUtils.readFileToByteArray(pr50), "image/png", "50_point_pr", "50 Punti Park&Ride", "50 Park&Ride Points"));
//		allBadges.add(new BadgesData(pr100.getName(), FileUtils.readFileToByteArray(pr100), "image/png", "100_point_pr", "100 Punti Park&Ride", "100 Park&Ride Points"));
//		allBadges.add(new BadgesData(pr200.getName(), FileUtils.readFileToByteArray(pr200), "image/png", "200_point_pr", "200 Punti Park&Ride", "200 Park&Ride Points"));
//		allBadges.add(new BadgesData(prBronze.getName(), FileUtils.readFileToByteArray(prBronze), "image/png", "bronze_medal_pr", "Medaglia di Bronzo - Park&Ride", "Bronze Medal - Park&Ride"));
//		allBadges.add(new BadgesData(prSilver.getName(), FileUtils.readFileToByteArray(prSilver), "image/png", "silver_medal_pr", "Medaglia d'Argento - Park&Ride", "Silver Medal - Park&Ride"));
//		allBadges.add(new BadgesData(prGold.getName(), FileUtils.readFileToByteArray(prGold), "image/png", "gold_medal_pr", "Medaglia d'Oro - Park&Ride", "Gold Medal - Park&Ride"));
//		allBadges.add(new BadgesData(prManifattura.getName(), FileUtils.readFileToByteArray(prManifattura), "image/png", "Manifattura_parking", "Parcheggio Manifattura - Park&Ride",
//				"Manifattura Park - Park&Ride"));
//		allBadges.add(new BadgesData(prStadio.getName(), FileUtils.readFileToByteArray(prStadio), "image/png", "Stadio_parking", "Parcheggio Stadio - Park&Ride", "Stadio Park - Park&Ride"));
//		allBadges.add(new BadgesData(prRagazzi99.getName(), FileUtils.readFileToByteArray(prRagazzi99), "image/png", "Via Ragazzi del '99_parking", "Via ragazzi del 99 - Park&Ride",
//				"Via ragazzi del 99 Park - Park&Ride"));
//		allBadges.add(new BadgesData(prLidorno.getName(), FileUtils.readFileToByteArray(prLidorno), "image/png", "Via Lidorno_parking", "Via Lidorno - Park&Ride", "Via Lidorno Park - Park&Ride"));
//		allBadges.add(new BadgesData(prViaFersina.getName(), FileUtils.readFileToByteArray(prViaFersina), "image/png", "Ghiaie via Fersina_parking", "Ghiaie via Fersina - Park&Ride",
//				"Ghiaie Park - Park&Ride"));
//		allBadges.add(new BadgesData(prAreaZuffo.getName(), FileUtils.readFileToByteArray(prAreaZuffo), "image/png", "Ex-Zuffo_parking", "Ex Zuffo - Park&Ride", "Ex Zuffo Park - Park&Ride"));
//		allBadges.add(
//				new BadgesData(prMonteBaldo.getName(), FileUtils.readFileToByteArray(prMonteBaldo), "image/png", "Monte Baldo_parking", "Monte Baldo - Park&Ride", "Monte Baldo Park - Park&Ride"));
//		allBadges.add(new BadgesData(prVillazzanoFS.getName(), FileUtils.readFileToByteArray(prVillazzanoFS), "image/png", "Via Asiago, Stazione FS Villazzano_parking",
//				"Stazione FS Villazzano - Park&Ride", "Villazzano FS Station - Park&Ride"));
//
//		// files for special badges
//		File specialEmotion = new File(path + "img/mail/special/emotion.png");
//		File specialZeroImpact = new File(path + "img/mail/special/impatto_zero.png");
//		File specialStadioPark = new File(path + "img/mail/special/special_p_quercia.png");
//		File specialManifattura = new File(path + "img/mail/special/special_special_p_manifattura.png");
//		File specialCentroStorico = new File(path + "img/mail/special/special_p_centro.storico.png");
//		File specialParcheggioCentro = new File(path + "img/mail/special/special_p_centro.png");
//		File specialPleALeoni = new File(path + "img/mail/special/special_p_p.le.a.leoni.png");
//
//		allBadges.add(new BadgesData(specialEmotion.getName(), FileUtils.readFileToByteArray(specialEmotion), "image/png", "e-motion", "E-Motion", "E-Motion"));
//		allBadges.add(new BadgesData(specialZeroImpact.getName(), FileUtils.readFileToByteArray(specialZeroImpact), "image/png", "zero-impact", "Impatto Zero", "Zero Impact"));
//		allBadges.add(new BadgesData(specialStadioPark.getName(), FileUtils.readFileToByteArray(specialStadioPark), "image/png", "Stadio-park", "Parcheggio Stadio Quercia", "Stadio Quercia Park"));
//		allBadges.add(new BadgesData(specialManifattura.getName(), FileUtils.readFileToByteArray(specialManifattura), "image/png", "Ex Manifattura-park", "Parcheggio Ex Manifattura",
//				"Ex Manifattura Park"));
//		allBadges.add(new BadgesData(specialCentroStorico.getName(), FileUtils.readFileToByteArray(specialCentroStorico), "image/png", "Centro Storico-park", "Parcheggio Centro Storico",
//				"Centro Storico Park"));
//		allBadges.add(
//				new BadgesData(specialParcheggioCentro.getName(), FileUtils.readFileToByteArray(specialParcheggioCentro), "image/png", "Parcheggio Centro-park", "Parcheggio Centro", "Centro Park"));
//		allBadges
//				.add(new BadgesData(specialPleALeoni.getName(), FileUtils.readFileToByteArray(specialPleALeoni), "image/png", "P.le A.Leoni-park", "Parcheggio Piazzale Leoni", "Piazzale Leoni Park"));
//
//		// files for bike
//		File bike1 = new File(path + "img/mail/bike/bikeAficionado1.png");
//		File bike5 = new File(path + "img/mail/bike/bikeAficionado5.png");
//		File bike10 = new File(path + "img/mail/bike/bikeAficionado10.png");
//		File bike25 = new File(path + "img/mail/bike/bikeAficionado25.png");
//		File bike50 = new File(path + "img/mail/bike/bikeAficionado50.png");
//		File bike100 = new File(path + "img/mail/bike/bikeAficionado100.png");
//		File bike200 = new File(path + "img/mail/bike/bikeAficionado200.png");
//		File bike500 = new File(path + "img/mail/bike/bikeAficionado500.png");
//
//		allBadges.add(new BadgesData(bike1.getName(), FileUtils.readFileToByteArray(bike1), "image/png", "1_bike_trip", "1 Viaggio in Bici", "1 Bike Trip"));
//		allBadges.add(new BadgesData(bike5.getName(), FileUtils.readFileToByteArray(bike5), "image/png", "5_bike_trip", "5 Viaggi in Bici", "5 Bike Trips"));
//		allBadges.add(new BadgesData(bike10.getName(), FileUtils.readFileToByteArray(bike10), "image/png", "10_bike_trip", "10 Viaggi in Bici", "10 Bike Trips"));
//		allBadges.add(new BadgesData(bike25.getName(), FileUtils.readFileToByteArray(bike25), "image/png", "25_bike_trip", "25 Viaggi in Bici", "25 Bike Trips"));
//		allBadges.add(new BadgesData(bike50.getName(), FileUtils.readFileToByteArray(bike50), "image/png", "50_bike_trip", "50 Viaggi in Bici", "50 Bike Trips"));
//		allBadges.add(new BadgesData(bike100.getName(), FileUtils.readFileToByteArray(bike100), "image/png", "100_bike_trip", "100 Viaggi in Bici", "100 Bike Trips"));
//		allBadges.add(new BadgesData(bike200.getName(), FileUtils.readFileToByteArray(bike200), "image/png", "200_bike_trip", "200 Viaggi in Bici", "200 Bike Trips"));
//		allBadges.add(new BadgesData(bike500.getName(), FileUtils.readFileToByteArray(bike500), "image/png", "500_bike_trip", "500 Viaggi in Bici", "500 Bike Trips"));
//
//		// files for bike sharing
//		File bikeShareBrione = new File(path + "img/mail/bike_sharing/bikeSharingPioneerBrione.png");
//		File bikeShareLizzana = new File(path + "img/mail/bike_sharing/bikeSharingPioneerLizzana.png");
//		File bikeShareMarco = new File(path + "img/mail/bike_sharing/bikeSharingPioneerMarco.png");
//		File bikeShareMunicipio = new File(path + "img/mail/bike_sharing/bikeSharingPioneerMunicipio.png");
//		File bikeShareNoriglio = new File(path + "img/mail/bike_sharing/bikeSharingPioneerNoriglio.png");
//		File bikeShareOrsi = new File(path + "img/mail/bike_sharing/bikeSharingPioneerOrsi.png");
//		File bikeShareOspedale = new File(path + "img/mail/bike_sharing/bikeSharingPioneerOspedale.png");
//		File bikeSharePaoli = new File(path + "img/mail/bike_sharing/bikeSharingPioneerPaoli.png");
//		File bikeSharePROsmini = new File(path + "img/mail/bike_sharing/bikeSharingPioneerPRosmini.png");
//		File bikeShareQuercia = new File(path + "img/mail/bike_sharing/bikeSharingPioneerQuercia.png");
//		File bikeShareSacco = new File(path + "img/mail/bike_sharing/bikeSharingPioneerSacco.png");
//		File bikeShareStazione = new File(path + "img/mail/bike_sharing/bikeSharingPioneerStazione.png");
//		File bikeShareZonaIndustriale = new File(path + "img/mail/bike_sharing/bikeSharingPioneerZonaIndustriale.png");
//		File bikeShareMart = new File(path + "img/mail/bike_sharing/bikeSharingPioneerMART.png");
//		// Real TN bike station url
//		File bikeShareFFSSOspedale = new File(path + "img/mail/bike_sharing/bikeSharingPioneerFFSSOspedale.png");
//		File bikeSharePiazzaVenezia = new File(path + "img/mail/bike_sharing/bikeSharingPioneerPiazzaVenezia.png");
//		File bikeSharePiscina = new File(path + "img/mail/bike_sharing/bikeSharingPioneerPiscina.png");
//		File bikeSharePiazzaMostra = new File(path + "img/mail/bike_sharing/bikeSharingPioneerPiazzaMostra.png");
//		File bikeShareCentroSantaChiara = new File(path + "img/mail/bike_sharing/bikeSharingPioneerCentroSantaChiara.png");
//		File bikeSharePiazzaCenta = new File(path + "img/mail/bike_sharing/bikeSharingPioneerPiazzaCenta.png");
//		File bikeShareBiblioteca = new File(path + "img/mail/bike_sharing/bikeSharingPioneerBiblioteca.png");
//		File bikeShareStazioneAutocorriere = new File(path + "img/mail/bike_sharing/bikeSharingPioneerStazioneAutocorriere.png");
//		File bikeShareUniversita = new File(path + "img/mail/bike_sharing/bikeSharingPioneerUniversita.png");
//		File bikeShareBezzi = new File(path + "img/mail/bike_sharing/bikeSharingPioneerBezzi.png");
//		File bikeShareMuse = new File(path + "img/mail/bike_sharing/bikeSharingPioneerMuse.png");
//		File bikeShareAziendaSanitaria = new File(path + "img/mail/bike_sharing/bikeSharingPioneerAziendaSanitaria.png");
//		File bikeShareTopCenter = new File(path + "img/mail/bike_sharing/bikeSharingPioneerTopCenter.png");
//		File bikeShareBrenCenter = new File(path + "img/mail/bike_sharing/bikeSharingPioneerBrenCenter.png");
//		File bikeShareLidorno = new File(path + "img/mail/bike_sharing/bikeSharingPioneerLidorno.png");
//		File bikeShareGardolo = new File(path + "img/mail/bike_sharing/bikeSharingPioneerGardolo.png");
//		File bikeShareAeroporto = new File(path + "img/mail/bike_sharing/bikeSharingPioneerAeroporto.png");
//
//		allBadges.add(new BadgesData(bikeShareBrione.getName(), FileUtils.readFileToByteArray(bikeShareBrione), "image/png", "Brione - Rovereto_BSstation", "Parcheggio Bike Sharing Brione",
//				"Brione Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareLizzana.getName(), FileUtils.readFileToByteArray(bikeShareLizzana), "image/png", "Lizzana - Rovereto_BSstation", "Parcheggio Bike Sharing Lizzana",
//				"Lizzana Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareMarco.getName(), FileUtils.readFileToByteArray(bikeShareMarco), "image/png", "Marco - Rovereto_BSstation", "Parcheggio Bike Sharing Marco",
//				"Marco Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareMunicipio.getName(), FileUtils.readFileToByteArray(bikeShareMunicipio), "image/png", "Municipio - Rovereto_BSstation",
//				"Parcheggio Bike Sharing Municipio", "Municipio Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareNoriglio.getName(), FileUtils.readFileToByteArray(bikeShareNoriglio), "image/png", "Noriglio - Rovereto_BSstation", "Parcheggio Bike Sharing Noriglio",
//				"Noriglio Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareOrsi.getName(), FileUtils.readFileToByteArray(bikeShareOrsi), "image/png", "Orsi - Rovereto_BSstation", "Parcheggio Bike Sharing Piazzale Orsi",
//				"Piazzale Orsi Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareOspedale.getName(), FileUtils.readFileToByteArray(bikeShareOspedale), "image/png", "Ospedale - Rovereto_BSstation", "Parcheggio Bike Sharing Ospedale",
//				"Ospedale Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeSharePaoli.getName(), FileUtils.readFileToByteArray(bikeSharePaoli), "image/png", "Via Paoli - Rovereto_BSstation", "Parcheggio Bike Sharing Via Paoli",
//				"Via Paoli Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeSharePROsmini.getName(), FileUtils.readFileToByteArray(bikeSharePROsmini), "image/png", "P. Rosmini - Rovereto_BSstation",
//				"Parcheggio Bike Sharing P. Rosmini", "P. Rosmini Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareQuercia.getName(), FileUtils.readFileToByteArray(bikeShareQuercia), "image/png", "Quercia - Rovereto_BSstation", "Parcheggio Bike Sharing Quercia",
//				"Quercia Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareSacco.getName(), FileUtils.readFileToByteArray(bikeShareSacco), "image/png", "Sacco - Rovereto_BSstation", "Parcheggio Bike Sharing Sacco",
//				"Sacco Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareStazione.getName(), FileUtils.readFileToByteArray(bikeShareStazione), "image/png", "Stazione FF.SS. - Rovereto_BSstation",
//				"Parcheggio Bike Sharing Stazione FF.SS.", "Stazione FF.SS. Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareZonaIndustriale.getName(), FileUtils.readFileToByteArray(bikeShareZonaIndustriale), "image/png", "Zona Industriale - Rovereto_BSstation",
//				"Parcheggio Bike Sharing Zona Industriale", "Zona Industriale Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareMart.getName(), FileUtils.readFileToByteArray(bikeShareMart), "image/png", "Mart - Rovereto_BSstation", "Parcheggio Bike Sharing MART",
//				"MART Bike Sharing Park"));
//		// TN bikeSharing stations
//		allBadges.add(new BadgesData(bikeShareFFSSOspedale.getName(), FileUtils.readFileToByteArray(bikeShareFFSSOspedale), "image/png", "Stazione FFSS - Ospedale - Trento_BSstation",
//				"Parcheggio Bike Sharing Stazione FF.SS. Ospedale", "Stazine FF.SS Ospedale Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeSharePiazzaVenezia.getName(), FileUtils.readFileToByteArray(bikeSharePiazzaVenezia), "image/png", "Piazza Venezia - Trento_BSstation",
//				"Parcheggio Bike Sharing Piazza Venezia", "Piazza Venezia Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeSharePiscina.getName(), FileUtils.readFileToByteArray(bikeSharePiscina), "image/png", "Piscina - Trento_BSstation", "Parcheggio Bike Sharing Piscina",
//				"Piscina Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeSharePiazzaMostra.getName(), FileUtils.readFileToByteArray(bikeSharePiazzaMostra), "image/png", "Piazza della Mostra - Trento_BSstation",
//				"Parcheggio Bike Sharing Piazza Mostra", "Piazza Mostra Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareCentroSantaChiara.getName(), FileUtils.readFileToByteArray(bikeShareCentroSantaChiara), "image/png", "Centro Santa Chiara - Trento_BSstation",
//				"Parcheggio Bike Sharing Centro S.Chiara", "Centro S.Chiara Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeSharePiazzaCenta.getName(), FileUtils.readFileToByteArray(bikeSharePiazzaCenta), "image/png", "Piazza di Centa - Trento_BSstation",
//				"Parcheggio Bike Sharing Piazza di Centa", "Piazza Centa Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareBiblioteca.getName(), FileUtils.readFileToByteArray(bikeShareBiblioteca), "image/png", "Biblioteca - Trento_BSstation",
//				"Parcheggio Bike Sharing Biblioteca", "Biblioteca Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareStazioneAutocorriere.getName(), FileUtils.readFileToByteArray(bikeShareStazioneAutocorriere), "image/png", "Stazione Autocorriere - Trento_BSstation",
//				"Parcheggio Bike Sharing Stazione Autocorriere", "Stazione Autocorriere Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareUniversita.getName(), FileUtils.readFileToByteArray(bikeShareUniversita), "image/png", "Università - Trento_BSstation",
//				"Parcheggio Bike Sharing Universita'", "Universita Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareBezzi.getName(), FileUtils.readFileToByteArray(bikeShareBezzi), "image/png", "Bezzi - Trento_BSstation", "Parcheggio Bike Sharing Bezzi",
//				"Bezzi Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareMuse.getName(), FileUtils.readFileToByteArray(bikeShareMuse), "image/png", "Muse - Trento_BSstation", "Parcheggio Bike Sharing Muse",
//				"Muse Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareAziendaSanitaria.getName(), FileUtils.readFileToByteArray(bikeShareAziendaSanitaria), "image/png", "Azienda Sanitaria - Trento_BSstation",
//				"Parcheggio Bike Sharing Azienda Sanitaria", "Azienda Sanitaria Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareTopCenter.getName(), FileUtils.readFileToByteArray(bikeShareTopCenter), "image/png", "Top Center - Trento_BSstation",
//				"Parcheggio Bike Sharing Top Center", "Top Center Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareBrenCenter.getName(), FileUtils.readFileToByteArray(bikeShareBrenCenter), "image/png", "Bren Center - Trento_BSstation",
//				"Parcheggio Bike Sharing Bren Center", "Bren Center Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareLidorno.getName(), FileUtils.readFileToByteArray(bikeShareLidorno), "image/png", "Lidorno - Trento_BSstation", "Parcheggio Bike Sharing Lidorno",
//				"Lidorno Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareGardolo.getName(), FileUtils.readFileToByteArray(bikeShareGardolo), "image/png", "Gardolo - Trento_BSstation", "Parcheggio Bike Sharing Gardolo",
//				"Gardolo Bike Sharing Park"));
//		allBadges.add(new BadgesData(bikeShareAeroporto.getName(), FileUtils.readFileToByteArray(bikeShareAeroporto), "image/png", "Aeroporto - Trento_BSstation", "Parcheggio Bike Sharing Aeroporto",
//				"Aeroporto Bike Sharing Park"));
//
//		// files for recommendation
//		File recommendations3 = new File(path + "img/mail/recommendation/inviteFriends3.png");
//		File recommendations5 = new File(path + "img/mail/recommendation/inviteFriends5.png");
//		File recommendations10 = new File(path + "img/mail/recommendation/inviteFriends10.png");
//		File recommendations25 = new File(path + "img/mail/recommendation/inviteFriends25.png");
//
//		allBadges.add(new BadgesData(recommendations3.getName(), FileUtils.readFileToByteArray(recommendations3), "image/png", "3_recommendations", "3 Amici Invitati", "3 Friends recommendation"));
//		allBadges.add(new BadgesData(recommendations5.getName(), FileUtils.readFileToByteArray(recommendations5), "image/png", "5_recommendations", "5 Amici Invitati", "5 Friends recommendation"));
//		allBadges.add(
//				new BadgesData(recommendations10.getName(), FileUtils.readFileToByteArray(recommendations10), "image/png", "10_recommendations", "10 Amici Invitati", "10 Friends recommendation"));
//		allBadges.add(
//				new BadgesData(recommendations25.getName(), FileUtils.readFileToByteArray(recommendations25), "image/png", "25_recommendations", "25 Amici Invitati", "25 Friends recommendation"));
//
//		// files for public transport
//		File publicTrans5 = new File(path + "img/mail/public_transport/publicTransportAficionado5.png");
//		File publicTrans10 = new File(path + "img/mail/public_transport/publicTransportAficionado10.png");
//		File publicTrans25 = new File(path + "img/mail/public_transport/publicTransportAficionado25.png");
//		File publicTrans50 = new File(path + "img/mail/public_transport/publicTransportAficionado50.png");
//		File publicTrans100 = new File(path + "img/mail/public_transport/publicTransportAficionado100.png");
//		File publicTrans200 = new File(path + "img/mail/public_transport/publicTransportAficionado200.png");
//		File publicTrans500 = new File(path + "img/mail/public_transport/publicTransportAficionado500.png");
//
//		allBadges.add(new BadgesData(publicTrans5.getName(), FileUtils.readFileToByteArray(publicTrans5), "image/png", "5_pt_trip", "5 Viaggi Mezzi Pubblici", "5 Public Trasport Trips"));
//		allBadges.add(new BadgesData(publicTrans10.getName(), FileUtils.readFileToByteArray(publicTrans10), "image/png", "10_pt_trip", "10 Viaggi Mezzi Pubblici", "10 Public Trasport Trips"));
//		allBadges.add(new BadgesData(publicTrans25.getName(), FileUtils.readFileToByteArray(publicTrans25), "image/png", "25_pt_trip", "25 Viaggi Mezzi Pubblici", "25 Public Trasport Trips"));
//		allBadges.add(new BadgesData(publicTrans50.getName(), FileUtils.readFileToByteArray(publicTrans50), "image/png", "50_pt_trip", "50 Viaggi Mezzi Pubblici", "50 Public Trasport Trips"));
//		allBadges.add(new BadgesData(publicTrans100.getName(), FileUtils.readFileToByteArray(publicTrans100), "image/png", "100_pt_trip", "100 Viaggi Mezzi Pubblici", "100 Public Trasport Trips"));
//		allBadges.add(new BadgesData(publicTrans200.getName(), FileUtils.readFileToByteArray(publicTrans200), "image/png", "200_pt_trip", "200 Viaggi Mezzi Pubblici", "200 Public Trasport Trips"));
//		allBadges.add(new BadgesData(publicTrans500.getName(), FileUtils.readFileToByteArray(publicTrans500), "image/png", "500_pt_trip", "500 Viaggi Mezzi Pubblici", "500 Public Trasport Trips"));
//
//		// files for zero impact
//		File zeroImpact1 = new File(path + "img/mail/zero_impact/zeroImpact1.png");
//		File zeroImpact5 = new File(path + "img/mail/zero_impact/zeroImpact5.png");
//		File zeroImpact10 = new File(path + "img/mail/zero_impact/zeroImpact10.png");
//		File zeroImpact25 = new File(path + "img/mail/zero_impact/zeroImpact25.png");
//		File zeroImpact50 = new File(path + "img/mail/zero_impact/zeroImpact50.png");
//		File zeroImpact100 = new File(path + "img/mail/zero_impact/zeroImpact100.png");
//		File zeroImpact200 = new File(path + "img/mail/zero_impact/zeroImpact200.png");
//		File zeroImpact500 = new File(path + "img/mail/zero_impact/zeroImpact500.png");
//
//		allBadges.add(new BadgesData(zeroImpact1.getName(), FileUtils.readFileToByteArray(zeroImpact1), "image/png", "1_zero_impact_trip", "1 Viaggio Impatto Zero", "1 Zero Impact Trip"));
//		allBadges.add(new BadgesData(zeroImpact5.getName(), FileUtils.readFileToByteArray(zeroImpact5), "image/png", "5_zero_impact_trip", "5 Viaggi Impatto Zero", "5 Zero Impact Trips"));
//		allBadges.add(new BadgesData(zeroImpact10.getName(), FileUtils.readFileToByteArray(zeroImpact10), "image/png", "10_zero_impact_trip", "10 Viaggi Impatto Zero", "10 Zero Impact Trips"));
//		allBadges.add(new BadgesData(zeroImpact25.getName(), FileUtils.readFileToByteArray(zeroImpact25), "image/png", "25_zero_impact_trip", "25 Viaggi Impatto Zero", "25 Zero Impact Trips"));
//		allBadges.add(new BadgesData(zeroImpact50.getName(), FileUtils.readFileToByteArray(zeroImpact50), "image/png", "50_zero_impact_trip", "50 Viaggi Impatto Zero", "50 Zero Impact Trips"));
//		allBadges.add(new BadgesData(zeroImpact100.getName(), FileUtils.readFileToByteArray(zeroImpact100), "image/png", "100_zero_impact_trip", "100 Viaggi Impatto Zero", "100 Zero Impact Trips"));
//		allBadges.add(new BadgesData(zeroImpact200.getName(), FileUtils.readFileToByteArray(zeroImpact200), "image/png", "200_zero_impact_trip", "200 Viaggi Impatto Zero", "200 Zero Impact Trips"));
//		allBadges.add(new BadgesData(zeroImpact500.getName(), FileUtils.readFileToByteArray(zeroImpact500), "image/png", "500_zero_impact_trip", "500 Viaggi Impatto Zero", "500 Zero Impact Trips"));
//
//		// files for leaderboard top 3
//		File firstOfWeek = new File(path + "img/mail/leaderboard/leaderboard1.png");
//		File secondOfWeek = new File(path + "img/mail/leaderboard/leaderboard2.png");
//		File thirdOfWeek = new File(path + "img/mail/leaderboard/leaderboard3.png");
//
//		allBadges.add(new BadgesData(firstOfWeek.getName(), FileUtils.readFileToByteArray(firstOfWeek), "image/png", "1st_of_the_week", "Primo della settimana", "First of the Week"));
//		allBadges.add(new BadgesData(secondOfWeek.getName(), FileUtils.readFileToByteArray(secondOfWeek), "image/png", "2nd_of_the_week", "Secondo della settimana", "Second of the Week"));
//		allBadges.add(new BadgesData(thirdOfWeek.getName(), FileUtils.readFileToByteArray(thirdOfWeek), "image/png", "3rd_of_the_week", "Terzo della settimana", "Third of the Week"));
//
//		return allBadges;
//	}

	private List<BadgesData> checkCorrectBadges(List<BadgesData> allB, List<Notification> notifics) throws IOException {
		List<BadgesData> correctBadges = Lists.newArrayList();

		for (int i = 0; i < allB.size(); i++) {
			for (int j = 0; j < notifics.size(); j++) {
				if (notifics.get(j).getBadge().compareTo(allB.get(i).getTextId()) == 0) {
					logger.debug(String.format("Notification check notifications: %s, badge :%s", notifics.get(j).getBadge(), allB.get(i).getTextId()));
					correctBadges.add(allB.get(i));
				}
			}
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

	/*	*//**
			 * Method used to retrieve the state of a specific user and to send the find data via mail
			 * 
			 * @param urlWS:
			 *            url of the ws
			 * @return state List
			 * @throws InterruptedException
			 *//*
			 * private List<State> getState(String urlWS) throws InterruptedException{ RestTemplate restTemplate = new RestTemplate(); logger.debug("State WS GET " + urlWS); String result = ""; ResponseEntity<String> tmp_res = null; try { //result = restTemplate.getForObject(gamificationUrl + urlWS, String.class); //I pass the timestamp of the scheduled start time tmp_res = restTemplate.exchange(gamificationUrl + urlWS, HttpMethod.GET, new HttpEntity<Object>(createHeaders()),String.class); } catch (Exception ex){ logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage())); } List<State> states = null; result = tmp_res.getBody(); if(result != null && result.compareTo("") != 0){
			 * logger.debug(String.format("State Result Ok: %s", result)); states = chekState(result); } else { logger.error(String.format("State Result Fail: %s", result)); } return states; }
			 */

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
			// result = restTemplate.getForObject(gamificationUrl + urlWS, String.class); //I pass the timestamp of the scheduled start time
			tmp_res = restTemplate.exchange(gamificationUrl + urlWS, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
		} catch (Exception ex) {
			logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage()));
		}
		result = tmp_res.getBody();
		return result;
	}

	/**
	 * Method used to retrieve the notification of a specific user and to send the find data via mail
	 * 
	 * @param urlWS:
	 *            url of the ws
	 * @param timestamp:
	 *            timestamp for the new notifications
	 * @return notification List
	 * @throws InterruptedException
	 */
//	private List<Notification> getNotifications(String urlWS, String timestamp, String appId) throws InterruptedException {
//
//		RestTemplate restTemplate = new RestTemplate();
//		logger.debug("Notification WS GET " + urlWS);
//		String result = "";
//		ResponseEntity<String> tmp_res = null;
//		try {
//			// result = restTemplate.getForObject(gamificationUrl + urlWS + timestamp, String.class); //I pass the timestamp of the scheduled start time
//			tmp_res = restTemplate.exchange(gamificationUrl + urlWS + timestamp, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
//		} catch (Exception ex) {
//			logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage()));
//		}
//
//		List<Notification> notifications = null;
//		result = tmp_res.getBody();
//		if (result != null) {
//			logger.debug(String.format("Notification Result Ok: %s", result));
//			notifications = chekNotification(result);
//
//		} else {
//			logger.error(String.format("Notification Result Fail: %s", result));
//		}
//
//		return notifications;
//	}

	@SuppressWarnings("rawtypes")
	public List<Notification> getBadgeNotifications(long timestamp, String appId, String userId) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		AppInfo app = appSetup.findAppById(appId);
		
		String url = gamificationUrl + "notification/game/" + app.getGameId() + "/player/" + userId + "?includeTypes=BadgeNotification&fromTs=" + timestamp * 0;
		
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
	
	/**
	 * Method checkNotification: convert the result JSON string in an array of objects
	 * 
	 * @param result:
	 *            input string with the json of the ws
	 * @return Notification List
	 */
	/*
	 * private List<State> chekState(String result){ List<State> stateList = new List<State>(); logger.debug(String.format("Result from WS: %s", result)); try { JSONObject JOStates = new JSONObject(result); JSONObject JOMyState = JOStates.getJSONObject(JSON_STATE); JSONArray JScores = (!JOMyState.isNull(JSON_POINTCONCEPT)) ? JOMyState.getJSONArray(JSON_POINTCONCEPT) : null; if(JScores != null){ for(int i = 0; i < JScores.length(); i++){ String id = ""+i; JSONObject JOScore = JScores.getJSONObject(i); String name = JOScore.getString(JSON_NAME); String score = cleanStringFieldScore(JOScore.getString(JSON_SCORE)); State state = new State(id, name, score); stateList.add(state); } } } catch (JSONException e) { logger.error(String.format("Exception in parsing player state: %s",
	 * e.getMessage())); } return orderState(stateList); }
	 */

	/*
	 * private String cleanStringFieldScore(String fieldString){ String field = fieldString.trim(); Float score_num_f = Float.valueOf(field); int score_num_i = score_num_f.intValue(); String cleanedScore = Integer.toString(score_num_i); return cleanedScore; }
	 */

	/**
	 * Method orderState: used to order the state array
	 * 
	 * @param toOrder
	 * @return
	 */
	/*
	 * private List<State> orderState(List<State> toOrder){ List<State> orderedList = new List<State>(); // I order the list with green score at the first, health score at the second and pr at the third for(int i = 0; i < toOrder.size(); i++){ if(toOrder.get(i).getName().compareTo("green leaves") == 0){ orderedList.add(toOrder.get(i)); break; } } for(int i = 0; i < toOrder.size(); i++){ if(toOrder.get(i).getName().compareTo("health") == 0){ orderedList.add(toOrder.get(i)); break; } } for(int i = 0; i < toOrder.size(); i++){ if(toOrder.get(i).getName().compareTo("p+r") == 0){ orderedList.add(toOrder.get(i)); break; } } return orderedList; }
	 */


	// Method used to read a week prizes file and store all data in a list of WeekPrizeData object
	private List<WeekPrizeData> readWeekPrizesFile(String src) throws Exception {
		String cvsSplitBy = ",";
		List<WeekPrizeData> prizeWeekFileData = Lists.newArrayList();

//		List<String> lines = Resources.readLines(Resources.getResource(src), Charsets.UTF_8);
		List<String> lines = Resources.readLines(new File(src).toURI().toURL(), Charsets.UTF_8);

		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.trim().isEmpty()) continue;

			// use comma as separator
			String[] weekPrizeValues = line.split(cvsSplitBy);
			int weekNum = Integer.parseInt(weekPrizeValues[0]);
			String weekPrize = weekPrizeValues[1];
			String target = weekPrizeValues[2];
			String sponsor = weekPrizeValues[3];
			logger.debug(String.format("Week prize file: week num %s, prize %s, target %s, sponsor %s", weekNum, weekPrize, target, sponsor));
			WeekPrizeData wPrize = new WeekPrizeData(weekNum, weekPrize, target, sponsor);
			prizeWeekFileData.add(wPrize);
		}

		return prizeWeekFileData;
	}

	// Method used to read the week prizes data from conf file. More prizes for one week are allowed
	public List<WeekPrizeData> getWeekPrizes(int weeknum, String lang) {
		List<WeekPrizeData> allPrizes = weekPrizeData.get(lang);
		try {
			if (allPrizes == null) {
				allPrizes = readWeekPrizesFile(weeklyDataDir + "/game_week_prize_"+lang+".csv");
				weekPrizeData.put(lang, allPrizes);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		
		List<WeekPrizeData> prizeWeekData = Lists.newArrayList();
		for (int i = 0; i < allPrizes.size(); i++) {
			if (allPrizes.get(i).getWeekNum() == weeknum) {
				prizeWeekData.add(allPrizes.get(i));
			}
		}
		return prizeWeekData;
	}

	public List<WeekWinnersData> readWeekWinnersFile(String src) throws Exception {
		// TODO read from gamification engine ???
		
		String cvsSplitBy = ",";
		List<WeekWinnersData> winnerWeekFileData = Lists.newArrayList();

		List<String> lines = Resources.readLines(new File(src).toURI().toURL(), Charsets.UTF_8);

		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.trim().isEmpty()) continue;

			// use comma as separator
			String[] weekWinnerValues = line.split(cvsSplitBy);
			int weekNum = Integer.parseInt(weekWinnerValues[0]);
			String player = weekWinnerValues[1];
			String prize = weekWinnerValues[2];
			String target = weekWinnerValues[3];
			logger.debug(String.format("Week winner file: week num %s, player %s, prize %s, target %s", weekNum, player, prize, target));
			WeekWinnersData wWinners = new WeekWinnersData(weekNum, player, prize, target);
			winnerWeekFileData.add(wWinners);
		}

		return winnerWeekFileData;
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
