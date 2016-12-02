package eu.trentorise.smartcampus.mobility.util;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.network.JsonUtils;
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

	private String token = null;
	private Long expiresAt = null;
	
	public TokenHelper() {
	}

	@SuppressWarnings("rawtypes")
	public String getToken() {
		if (token == null || System.currentTimeMillis() + 10000 > expiresAt) {
			final HttpResponse resp;
			if (!aacURL.endsWith("/"))
				aacURL += "/";
			String url = aacURL + PATH_TOKEN + "?grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
			final HttpGet get = new HttpGet(url);
			get.setHeader("Accept", "application/json");
			try {
				resp = getHttpClient().execute(get);
				final String response = EntityUtils.toString(resp.getEntity());
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					Map map = JsonUtils.toObject(response, Map.class);
					expiresAt = System.currentTimeMillis() + (Integer) map.get("expires_in") * 1000;
					token = (String) map.get("access_token");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return token;
	}

}
