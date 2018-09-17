package eu.trentorise.smartcampus.mobility.util;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.aac.AACException;
import eu.trentorise.smartcampus.aac.AACService;
import eu.trentorise.smartcampus.aac.model.TokenData;
import eu.trentorise.smartcampus.network.RemoteConnector;

@Component
public class TokenHelper extends RemoteConnector {

	private static final String PATH_TOKEN = "oauth/token";

	public static final String MS_APP = "core.mobility";

	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	@Autowired
	@Value("${smartcampus.clientId}")
	private String clientId;
	@Autowired
	@Value("${smartcampus.clientSecret}")
	private String clientSecret;

	private AACService aacService;
	
	private String token = null;
	private Long expiresAt = null;
	
	public TokenHelper() {
	}

	@PostConstruct
	public void init() {
		aacService = new AACService(aacURL, clientId, clientSecret);
	}
	
	@SuppressWarnings("rawtypes")
	public String getToken() {
		if (token == null || System.currentTimeMillis() + 10000 > expiresAt) {
			try {
				TokenData td = aacService.generateClientToken();
				expiresAt = System.currentTimeMillis() + td.getExpires_in() * 1000;
				token = td.getAccess_token();
				return token;
			} catch (AACException e) {
				e.printStackTrace();
			}
		}

		return null;
	}
	
}
