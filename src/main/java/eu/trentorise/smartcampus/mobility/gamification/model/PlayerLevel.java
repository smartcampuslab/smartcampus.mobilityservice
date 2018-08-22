package eu.trentorise.smartcampus.mobility.gamification.model;

public class PlayerLevel {
    private String levelName;
    private String levelValue;
    private String pointConcept;
    private double startLevelScore;
    private double endLevelScore;
    private double toNextLevel;

    public PlayerLevel() {
    }
    
    public PlayerLevel(String levelName, String pointConcept, String levelValue,
            double toNextLevel, double startLevelScore, double endLevelScore) {
        this.levelName = levelName;
        this.pointConcept = pointConcept;
        this.levelValue = levelValue;
        this.toNextLevel = toNextLevel;
        this.startLevelScore = startLevelScore;
        this.endLevelScore = endLevelScore;
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

    public double getStartLevelScore() {
        return startLevelScore;
    }

    public double getEndLevelScore() {
        return endLevelScore;
    }
}
