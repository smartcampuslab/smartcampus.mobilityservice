package eu.trentorise.smartcampus.mobility.gamification.model;

public class ChallengeCompletedNotication extends Notification {
	private String challengeName;

	public String getChallengeName() {
		return challengeName;
	}

	public void setChallengeName(String challengeName) {
		this.challengeName = challengeName;
	}

	@Override
	public String toString() {
		return String.format("[gameId=%s, playerId=%s, challengeName=%s]", getGameId(), getPlayerId(), challengeName);
	}

}
