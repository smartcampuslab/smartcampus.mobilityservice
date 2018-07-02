package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;

import com.google.common.collect.Lists;


public class ChallengeConcept {

	private List<ChallengesData> activeChallengeData = Lists.newArrayList();
	private List<ChallengesData> oldChallengeData = Lists.newArrayList();
	
	public ChallengeConcept() {
		super();
	}

	public List<ChallengesData> getActiveChallengeData() {
		return activeChallengeData;
	}

	public List<ChallengesData> getOldChallengeData() {
		return oldChallengeData;
	}

	public void setActiveChallengeData(List<ChallengesData> activeChallengeData) {
		this.activeChallengeData = activeChallengeData;
	}

	public void setOldChallengeData(List<ChallengesData> oldChallengeData) {
		this.oldChallengeData = oldChallengeData;
	}

}
