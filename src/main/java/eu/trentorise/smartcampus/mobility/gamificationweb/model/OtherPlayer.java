package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Maps;

@JsonInclude(Include.NON_NULL)
public class OtherPlayer {

	private String nickname;
	private int greenLeaves;
	private List<BadgeCollectionConcept> badgeCollectionConcept;
	private Map<String, Double> statistics = Maps.newTreeMap();

	private String level;
	private Double pointsToNextLevel;
	
	private String avatar;
	
	private Long updated;

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public int getGreenLeaves() {
		return greenLeaves;
	}

	public void setGreenLeaves(int greenLeaves) {
		this.greenLeaves = greenLeaves;
	}

	public List<BadgeCollectionConcept> getBadgeCollectionConcept() {
		return badgeCollectionConcept;
	}

	public void setBadgeCollectionConcept(List<BadgeCollectionConcept> badgeCollectionConcept) {
		this.badgeCollectionConcept = badgeCollectionConcept;
	}

	public Map<String, Double> getStatistics() {
		return statistics;
	}

	public void setStatistics(Map<String, Double> statistics) {
		this.statistics = statistics;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Double getPointsToNextLevel() {
		return pointsToNextLevel;
	}

	public void setPointsToNextLevel(Double pointsToNextLevel) {
		this.pointsToNextLevel = pointsToNextLevel;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public Long getUpdated() {
		return updated;
	}

	public void setUpdated(Long updated) {
		this.updated = updated;
	}
	
	
}
