package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;

public class PlayerClassification {

	private ClassificationData actualUser;
	private List<ClassificationData> classificationList;
	
	public PlayerClassification() {
		super();
	}

	public ClassificationData getActualUser() {
		return actualUser;
	}

	public List<ClassificationData> getClassificationList() {
		return classificationList;
	}

	public void setActualUser(ClassificationData actualUser) {
		this.actualUser = actualUser;
	}

	public void setClassificationList(List<ClassificationData> classificationList) {
		this.classificationList = classificationList;
	}

}
