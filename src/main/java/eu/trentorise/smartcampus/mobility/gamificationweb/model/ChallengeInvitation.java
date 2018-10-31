package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChallengeInvitation {

    private String gameId;
    private ChallengePlayer proposer;
    private List<ChallengePlayer> guests = new ArrayList<>();

    private String challengeName;
    private String challengeModelName;
    private Date challengeStart;
    private Date challengeEnd;
    private PointConceptRef challengePointConcept;
    private Reward reward;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public ChallengePlayer getProposer() {
        return proposer;
    }

    public void setProposer(ChallengePlayer proposer) {
        this.proposer = proposer;
    }

    public List<ChallengePlayer> getGuests() {
        return guests;
    }

    public void setGuests(List<ChallengePlayer> guests) {
        this.guests = guests;
    }

    public PointConceptRef getChallengePointConcept() {
        return challengePointConcept;
    }

    public void setChallengePointConcept(PointConceptRef challengePointConcept) {
        this.challengePointConcept = challengePointConcept;
    }

    public Reward getReward() {
        return reward;
    }

    public void setReward(Reward reward) {
        this.reward = reward;
    }

    public Date getChallengeStart() {
        return challengeStart;
    }

    public void setChallengeStart(Date challengeStart) {
        this.challengeStart = challengeStart;
    }

    public Date getChallengeEnd() {
        return challengeEnd;
    }

    public void setChallengeEnd(Date challengeEnd) {
        this.challengeEnd = challengeEnd;
    }

    public String getChallengeModelName() {
        return challengeModelName;
    }

    public void setChallengeModelName(String challengeModelName) {
        this.challengeModelName = challengeModelName;
    }

    public String getChallengeName() {
        return challengeName;
    }

    public void setChallengeName(String challengeName) {
        this.challengeName = challengeName;
    }

    public static class PointConceptRef {
        private String name;
        private String period;

        public PointConceptRef() {
        }

        public PointConceptRef(String name, String period) {
            this.name = name;
            this.period = period;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        @Override
        public String toString() {
            return String.format("{name=%s, period=%s}", name, period);
        }
    }    

    public static class ChallengePlayer {
        private String playerId;

        public ChallengePlayer() {
        }

        public ChallengePlayer(String playerId) {
        	this.playerId = playerId;
        }        
        
        public String getPlayerId() {
            return playerId;
        }

        public void setPlayerId(String playerId) {
            this.playerId = playerId;
        }

    }    
    
    public static class Reward {
        private double percentage;
        private double threshold;
        private PointConceptRef calculationPointConcept;
        private PointConceptRef targetPointConcept;

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }

        public PointConceptRef getCalculationPointConcept() {
            return calculationPointConcept;
        }

        public void setCalculationPointConcept(PointConceptRef calculationPointConcept) {
            this.calculationPointConcept = calculationPointConcept;
        }

        public PointConceptRef getTargetPointConcept() {
            return targetPointConcept;
        }

        public void setTargetPointConcept(PointConceptRef targetPointConcept) {
            this.targetPointConcept = targetPointConcept;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public String toString() {
            return String.format(
                    "{percentage=%s, threshold=%s, calculationPointConcept=%s, targetPointConcept=%s}",
                    percentage, threshold, calculationPointConcept, targetPointConcept);
        }
    }    
    
}
