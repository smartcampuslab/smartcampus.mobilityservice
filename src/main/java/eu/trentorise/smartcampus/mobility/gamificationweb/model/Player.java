package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="player")
public class Player {
	
	@Id
	private String pid;
	private String socialId;
	
	private String name;
	private String surname;
	private String nickname;
	private String mail;
	private String language;
	private boolean sendMail;
	private Map<String, Object> personalData;
	private SurveyData surveyData;
	private boolean checkedRecommendation;
	private List<Event> eventsCheckIn;
	
	public Player() {
		super();
	}

	public Player(String pid, String socialId, String name, String surname, String nickname,
			//String mail, String language, boolean sendMail, PersonalData personalData, SurveyData surveyData, String type) {
			String mail, String language, boolean sendMail, Map<String, Object> personalData, SurveyData surveyData, boolean checkRecommendation) {
		super();
		this.pid = pid;
		this.socialId = socialId;
		this.name = name;
		this.surname = surname;
		this.nickname = nickname;
		this.mail = mail;
		this.language = language;
		this.sendMail = sendMail;
		this.personalData = personalData;
		this.surveyData = surveyData;
		this.checkedRecommendation = checkRecommendation;
	}

	public String getName() {
		return name;
	}

	public String getSurname() {
		return surname;
	}

	public String getNickname() {
		return nickname;
	}

	public String getMail() {
		return mail;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public void setNickname(String nickName) {
		this.nickname = nickName;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getPid() {
		return pid;
	}

	public String getSocialId() {
		return socialId;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public void setSocialId(String socialId) {
		this.socialId = socialId;
	}

	public Map<String, Object> getPersonalData() {
		return personalData;
	}

	public void setPersonalData(Map<String, Object> personalData) {
		this.personalData = personalData;
	}
	
	public SurveyData getSurveyData() {
		return surveyData;
	}

	public void setSurveyData(SurveyData surveyData) {
		this.surveyData = surveyData;
	}

	public boolean isSendMail() {
		return sendMail;
	}

	public void setSendMail(boolean sendMail) {
		this.sendMail = sendMail;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean isCheckedRecommendation() {
		return checkedRecommendation;
	}

	public void setCheckedRecommendation(boolean checkedRecommendation) {
		this.checkedRecommendation = checkedRecommendation;
	}

	public List<Event> getEventsCheckIn() {
		return eventsCheckIn;
	}

	public void setEventsCheckIn(List<Event> eventsCheckIn) {
		this.eventsCheckIn = eventsCheckIn;
	}

	public String toJSONString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.JSON_STYLE);
		return tsb.build();
	}
	
}
