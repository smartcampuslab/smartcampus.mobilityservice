package eu.trentorise.smartcampus.mobility.security;

import org.springframework.stereotype.Component;

//TODO reenable
@Component
public class BannedChecker {

//	@Autowired
//	@Value("${externalDataDir}")
//	private String externalDataDir;			
//	
//	private Multimap<String, String> banned;
//	
//	private static Log logger = LogFactory.getLog(BannedChecker.class);
//	
//	public BannedChecker() {
//		banned = ArrayListMultimap.create();
//	}
//	
//	@PostConstruct
//	private void init() {
//		String src = externalDataDir + "/banned.csv";
//		List<String> lines = null;
//		try {
//			lines = Resources.readLines(new File(src).toURI().toURL(), Charsets.UTF_8);
//		} catch (IOException e) {
//			logger.error("Error reading " + src);
//			return;
//		}
//		for (String line: lines) {
//			String[] vals = line.split(",");
//			if (vals.length != 2) {
//				logger.error("Bad format for line: " + line);
//				continue;
//			}
//			banned.put(vals[0], vals[1]);
//		}
//	}
	
	public boolean isBanned(String playerId, String gameId) {
//		if (!banned.containsKey(gameId)) {
//			return false;
//		}
//		if (banned.get(gameId).contains(playerId)) {
//			return true;
//		}
		return false;
	}
	
}
