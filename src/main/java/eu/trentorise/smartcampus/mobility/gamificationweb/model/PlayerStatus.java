package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PlayerStatus {

	private Map<String, Object> playerData = Maps.newTreeMap();
	private List<PointConcept> pointConcept = Lists.newArrayList();
	private List<BadgeCollectionConcept> badgeCollectionConcept = Lists.newArrayList();
	private ChallengeConcept challengeConcept = new ChallengeConcept();
	
	public PlayerStatus() {
		super();
	}

	public List<BadgeCollectionConcept> getBadgeCollectionConcept() {
		return badgeCollectionConcept;
	}

	public void setBadgeCollectionConcept(List<BadgeCollectionConcept> badgeCollectionConcept) {
		this.badgeCollectionConcept = badgeCollectionConcept;
	}

	public Map<String, Object> getPlayerData() {
		return playerData;
	}

	public ChallengeConcept getChallengeConcept() {
		return challengeConcept;
	}

	public void setPlayerData(Map<String, Object> playerData) {
		this.playerData = playerData;
	}

	public void setChallengeConcept(ChallengeConcept challengeConcept) {
		this.challengeConcept = challengeConcept;
	}

	public List<PointConcept> getPointConcept() {
		return pointConcept;
	}

	public void setPointConcept(List<PointConcept> pointConcept) {
		this.pointConcept = pointConcept;
	}
	
}
