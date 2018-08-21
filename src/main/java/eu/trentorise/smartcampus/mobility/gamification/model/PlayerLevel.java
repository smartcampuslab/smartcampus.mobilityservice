package eu.trentorise.smartcampus.mobility.gamification.model;

public class PlayerLevel {
	private String levelName;
	private String levelValue;
	private String pointConcept;
	private double toNextLevel;

	public PlayerLevel() {
	}

	public PlayerLevel(String levelName, String pointConcept, String levelValue, double toNextLevel) {
		this.levelName = levelName;
		this.pointConcept = pointConcept;
		this.levelValue = levelValue;
		this.toNextLevel = toNextLevel;
	}

	public String getLevelName() {
		return levelName;
	}

	public String getPointConcept() {
		return pointConcept;
	}

	public double getToNextLevel() {
		return toNextLevel;
	}

	public String getLevelValue() {
		return levelValue;
	}

}
