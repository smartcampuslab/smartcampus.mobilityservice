package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.Avatar;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.storage.AvatarRepository;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ImageUtils;

@RestController
public class FileController {

//	@Value("${imagesDir}")
//	private String imagesDir;

	private static final String DEFAULT_USER = "default_user";

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private PlayerRepositoryDao playerRepository;
	
	@Autowired
	private AvatarRepository avatarRepository; 

	private static Log logger = LogFactory.getLog(FileController.class);

	@PostConstruct
	public void init() throws Exception {
		Avatar avatar = avatarRepository.findOne(DEFAULT_USER);
		if (avatar == null) {
			avatar = new Avatar();
			
			InputStream is = Resources.asByteSource(Resources.getResource(DEFAULT_USER + ".png")).openBufferedStream();
			
			byte cb[] = ImageUtils.compressImage(is, "image/png");
			
			Binary bb = new Binary(cb);
			avatar.setId(DEFAULT_USER);
			avatar.setAvatarData(bb);
			avatar.setContentType("image/png");
			avatar.setFileName("default.png");

			avatarRepository.save(avatar);
		}
	}
	
	@PostMapping("/gamificationweb/player/avatar")
	public @ResponseBody void uploadPlayerAvatar(@RequestParam("data") MultipartFile data, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response)
			throws Exception {
		Player player = null;

		try {
			String userId = getUserId();
			if (userId == null) {
				logger.error("User not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String gameId = appSetup.findAppById(appId).getGameId();
			if (gameId == null) {
				logger.error("GameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			player = playerRepository.findByIdAndGameId(userId, gameId);

			if (player == null) {
				logger.error("Player not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			if (data.getSize() > 10 * 1024 * 1024) {
				logger.error("Image too big.");
				response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
				return;
			}
			
			MediaType mediaType = MediaType.parseMediaType(data.getContentType());
			
			if (!mediaType.isCompatibleWith(MediaType.IMAGE_GIF) && !mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) && !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
				logger.error("Image format not supported.");
				response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
				return;				
			}
			
			Avatar av = new Avatar();
			
			byte cb[] = ImageUtils.compressImage(data.getInputStream(), data.getContentType());

			Binary bb = new Binary(cb);
			av.setId(userId + "@" + appId);
			av.setAvatarData(bb);
			av.setContentType(data.getContentType());
			av.setFileName(data.getOriginalFilename());

			avatarRepository.save(av);
		} catch (Exception e) {
			logger.error("Error in post avatar: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping(value = "/gamificationweb/player/avatar/{appId}/{playerId}") //, produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public @ResponseBody void getPlayerAvatarData(@PathVariable String appId, @PathVariable String playerId, HttpServletResponse response) throws Exception {
		Avatar avatar = avatarRepository.findOne(playerId + "@" + appId);
		if (avatar == null) {
			avatar = avatarRepository.findOne(DEFAULT_USER);
		}
		
		response.getOutputStream().write(avatar.getAvatarData().getData());
		response.setContentLength(avatar.getAvatarData().getData().length);
		response.setContentType(avatar.getContentType());

	}	
	
	protected String getUserId() {
		String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return principal;
	}

}
