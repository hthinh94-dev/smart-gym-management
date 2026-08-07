export type Gender = "MALE" | "FEMALE";

export type FitnessGoal = "BULK" | "CUT" | "MAINTAIN" | "MUSCLE_GAIN" | "WEIGHT_GAIN" | "FAT_LOSS" | "WEIGHT_LOSS";

export type FitnessLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

export type ActivityLevel = "SEDENTARY" | "LIGHTLY_ACTIVE" | "MODERATELY_ACTIVE" | "VERY_ACTIVE";

export type DietaryPreference = "OMNIVORE" | "VEGETARIAN" | "VEGAN";

export type Equipment = "BARBELL" | "DUMBBELL" | "MACHINE" | "CABLE" | "BENCH";

export type MuscleGroup =
    | "CHEST"
    | "BACK"
    | "SHOULDERS"
    | "ARMS"
    | "LEGS"
    | "GLUTES"
    | "CORE"
    | "CARDIO"
    | "FULL_BODY";

export type ContraindicationTag =
    | "KNEE_FLEXION_LIMITED"
    | "OVERHEAD_MOVEMENT_LIMITED"
    | "LOWER_BACK_LOAD_LIMITED"
    | "WRIST_FLEXION_LIMITED"
    | "NECK_LOAD_LIMITED";

export type BioProfile = {
    gender: Gender;
    dateOfBirth: string;
    heightCm: number;
    weightKg: number;
    fitnessGoal: FitnessGoal;
    fitnessGoals?: FitnessGoal[];
    targetWeightKg?: number | null;
    mobilityLimitNotes?: string | null;
    fitnessLevel: FitnessLevel;
    activityLevel: ActivityLevel;
    workoutDaysPerWeek: number;
    maxSessionMinutes: number;
    availableEquipment: Equipment[];
    targetMuscleGroups: MuscleGroup[];
    injuryConstraints: ContraindicationTag[];
};

export type NutritionProfile = {
    dietaryPreference: DietaryPreference;
    foodAllergies: string[];
    excludedFoods: string[];
    mealsPerDay: number;
};

export type CalculatedTargets = {
    bmi: number;
    bmiCategory?: "UNDERWEIGHT" | "NORMAL" | "OVERWEIGHT" | "OBESE";
    bmr: number;
    tdee: number;
    dailyCaloriesKcal: number;
    proteinGrams: number;
    fatGrams: number;
    carbGrams: number;
};

export type MemberProfileUpsertRequest = BioProfile & NutritionProfile & {
    fitnessGoals: FitnessGoal[];
    targetWeightKg?: number;
};

export type MemberProfile = {
    memberId: number;
    bioProfile: BioProfile;
    nutritionProfile: NutritionProfile;
    calculatedTargets: CalculatedTargets;
    updatedAt: string;
};

export type MemberProfileErrorCode =
    | "PROF-001"
    | "ACC-004"
    | "ACC-005"
    | "ACC-006"
    | "VAL-001"
    | "NETWORK-001"
    | "SYS-001";

export type MemberProfileApiSuccess = {
    success: true;
    message: string;
    data: MemberProfile;
};

export type MemberProfileApiErrorResponse = {
    success: false;
    errorCode: MemberProfileErrorCode;
    message: string;
    details: Record<string, unknown>;
};
