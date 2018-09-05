package eu.trentorise.smartcampus.mobility.gamification.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeChoice.ChoiceState;
import eu.trentorise.smartcampus.mobility.gamification.model.Inventory.ItemChoice.ChoiceType;
import eu.trentorise.smartcampus.mobility.gamification.model.Level.Config;

public class Inventory {

    private List<ChallengeChoice> challengeChoices = new ArrayList<>();

    private int challengeActivationActions;

    public Inventory() {}

    public static class ItemChoice {
        private ChoiceType type;
        private String name;

        public ItemChoice() {

        }

        public ItemChoice(ChoiceType type, String name) {
            this.type = type;
            this.name = name;
        }

        public enum ChoiceType {
            CHALLENGE_MODEL
        }


        public ChoiceType getType() {
            return type;
        }

        public void setType(ChoiceType type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public Inventory upgrade(Config levelConfig) {
        if (levelConfig != null) {
            List<ChallengeChoice> levelChoices = levelConfig.getAvailableModels().stream()
                    .map(availableModel -> new ChallengeChoice(availableModel,
                            ChoiceState.AVAILABLE))
                    .collect(Collectors.toList());
            challengeChoices.addAll(levelChoices);
            challengeActivationActions += levelConfig.getChoices();
        }

        return this;
    }

    public Inventory activateChoice(ItemChoice choice) {
        if (challengeActivationActions == 0) {
            throw new IllegalArgumentException("No activation actions available");
        }
        boolean found = false;
        if (isChallengeChoice(choice)) {
            for (ChallengeChoice challengeChoice : challengeChoices) {
                if (challengeChoice.getModelName().equals(choice.getName())) {
                    found = true;
                    if (challengeChoice.getState() == ChoiceState.AVAILABLE) {
                        challengeChoice.update(ChoiceState.ACTIVE);
                        challengeActivationActions--;
                    }
                }
            }
            if (!found) {
                throw new IllegalArgumentException(String
                        .format("Choice %s is not available for the player", choice.getName()));
            }
        }
        return this;
    }

    public int size() {
        return challengeChoices.size();
    }

    public List<ChallengeChoice> getChallengeChoices() {
        return challengeChoices;
    }

    public int getChallengeActivationActions() {
        return challengeActivationActions;
    }

    private boolean isChallengeChoice(ItemChoice choice) {
        return choice != null && choice.getType() == ChoiceType.CHALLENGE_MODEL;
    }
}
