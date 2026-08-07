package com.thinh.smartgym.common.enums;

public enum FitnessGoal {
    BULK,
    CUT,
    MAINTAIN,
    MUSCLE_GAIN,
    WEIGHT_GAIN,
    FAT_LOSS,
    WEIGHT_LOSS;

    public boolean requiresCalorieSurplus() {
        return this == BULK || this == MUSCLE_GAIN || this == WEIGHT_GAIN;
    }

    public boolean requiresCalorieDeficit() {
        return this == CUT || this == FAT_LOSS || this == WEIGHT_LOSS;
    }
}
