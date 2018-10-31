package eu.trentorise.smartcampus.mobility.gamification.model;

public class Invitation {

	public enum ChallengeModelNames {
		groupCompetitivePerformance
	}		
	
	private String attendeeId;
	private String challengeName;
	private ChallengeModelNames challengeModelName;
	private String challengePointConcept;

	public String getAttendeeId() {
		return attendeeId;
	}

	public void setAttendeeId(String attendee) {
		this.attendeeId = attendee;
	}

	public String getChallengeName() {
		return challengeName;
	}

	public void setChallengeName(String challengeName) {
		this.challengeName = challengeName;
	}

	public ChallengeModelNames getChallengeModelName() {
		return challengeModelName;
	}

	public void setChallengeModelName(ChallengeModelNames challengeModelName) {
		this.challengeModelName = challengeModelName;
	}

	public String getChallengePointConcept() {
		return challengePointConcept;
	}

	public void setChallengePointConcept(String challengePointConcept) {
		this.challengePointConcept = challengePointConcept;
	}

}
