package com.nous.dailyenglish;

import java.util.List;

public class Task {
    public String id;
    public TaskType type;
    public Difficulty difficulty;
    public String level;       // a0, a1, a2, b1, b2
    public String instruction; // "Craft a wooden pickaxe"
    public String target;      // "WOODEN_PICKAXE" / "OAK_SAPLING" / topic
    public int amount;         // for CRAFT/PLANT type
    public int minWords;       // for DESCRIBE type
    public String topic;       // for DESCRIBE type
    public List<String> quizOptions;  // for QUIZ type
    public int quizCorrect;         // index for QUIZ type

    public Task() {}

    public double getReward(double easy, double medium, double hard) {
        switch (difficulty) {
            case EASY: return easy;
            case MEDIUM: return medium;
            case HARD: return hard;
            default: return easy;
        }
    }

    public String getTargetLower() {
        return target != null ? target.toLowerCase() : "";
    }
}
