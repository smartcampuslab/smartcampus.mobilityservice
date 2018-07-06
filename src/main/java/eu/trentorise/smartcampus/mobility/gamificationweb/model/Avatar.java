package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import org.bson.types.Binary;
import org.springframework.data.annotation.Id;

public class Avatar {
	
	@Id
	private String id;
	
	private Binary avatarData;
	
	private String contentType;
	private String fileName;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Binary getAvatarData() {
		return avatarData;
	}

	public void setAvatarData(Binary avatarData) {
		this.avatarData = avatarData;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
}
