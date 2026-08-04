import axios from "axios";
import { httpClient } from "../../../../lib/httpClient";
import type {
    ActivityLevel,
    ContraindicationTag,
    DietaryPreference,
    Equipment,
    FitnessGoal,
    FitnessLevel,
    Gender,
    MemberProfile,
    MemberProfileApiErrorResponse,
    MemberProfileApiSuccess,
    MemberProfileUpsertRequest,
    MemberProfileErrorCode,
    MuscleGroup,
} from "../types/memberProfile.types";

const PROFILE_ERROR_CODES = new Set<MemberProfileErrorCode>([
    "PROF-001",
    "ACC-004",
    "ACC-005",
    "ACC-006",
    "VAL-001",
    "NETWORK-001",
    "SYS-001",
]);

const GENDERS = new Set<Gender>(["MALE", "FEMALE"]);
const FITNESS_GOALS = new Set<FitnessGoal>(["BULK", "CUT", "MAINTAIN"]);
const FITNESS_LEVELS = new Set<FitnessLevel>(["BEGINNER", "INTERMEDIATE", "ADVANCED"]);
const ACTIVITY_LEVELS = new Set<ActivityLevel>([
    "SEDENTARY",
    "LIGHTLY_ACTIVE",
    "MODERATELY_ACTIVE",
    "VERY_ACTIVE",
]);
const DIETARY_PREFERENCES = new Set<DietaryPreference>(["OMNIVORE", "VEGETARIAN", "VEGAN"]);
const EQUIPMENT = new Set<Equipment>(["BARBELL", "DUMBBELL", "MACHINE", "CABLE", "BENCH"]);
const MUSCLE_GROUPS = new Set<MuscleGroup>([
    "CHEST",
    "BACK",
    "SHOULDERS",
    "ARMS",
    "LEGS",
    "GLUTES",
    "CORE",
    "CARDIO",
    "FULL_BODY",
]);
const CONTRAINDICATION_TAGS = new Set<ContraindicationTag>([
    "KNEE_FLEXION_LIMITED",
    "OVERHEAD_MOVEMENT_LIMITED",
    "LOWER_BACK_LOAD_LIMITED",
    "WRIST_FLEXION_LIMITED",
    "NECK_LOAD_LIMITED",
]);

function isRecord(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === "object";
}

function isFiniteNumber(value: unknown): value is number {
    return typeof value === "number" && Number.isFinite(value);
}

function isInteger(value: unknown): value is number {
    return isFiniteNumber(value) && Number.isInteger(value);
}

function isEnumValue<T extends string>(value: unknown, values: Set<T>): value is T {
    return typeof value === "string" && values.has(value as T);
}

function isEnumArray<T extends string>(value: unknown, values: Set<T>): value is T[] {
    return Array.isArray(value) && value.every((item) => isEnumValue(item, values));
}

function isStringArray(value: unknown): value is string[] {
    return Array.isArray(value) && value.every((item) => typeof item === "string");
}

function isBioProfile(value: unknown): value is MemberProfile["bioProfile"] {
    return isRecord(value)
        && isEnumValue(value.gender, GENDERS)
        && typeof value.dateOfBirth === "string"
        && isFiniteNumber(value.heightCm)
        && isFiniteNumber(value.weightKg)
        && isEnumValue(value.fitnessGoal, FITNESS_GOALS)
        && isEnumValue(value.fitnessLevel, FITNESS_LEVELS)
        && isEnumValue(value.activityLevel, ACTIVITY_LEVELS)
        && isInteger(value.workoutDaysPerWeek)
        && isInteger(value.maxSessionMinutes)
        && isEnumArray(value.availableEquipment, EQUIPMENT)
        && isEnumArray(value.targetMuscleGroups, MUSCLE_GROUPS)
        && isEnumArray(value.injuryConstraints, CONTRAINDICATION_TAGS);
}

function isNutritionProfile(value: unknown): value is MemberProfile["nutritionProfile"] {
    return isRecord(value)
        && isEnumValue(value.dietaryPreference, DIETARY_PREFERENCES)
        && isStringArray(value.foodAllergies)
        && isStringArray(value.excludedFoods)
        && isInteger(value.mealsPerDay);
}

function isCalculatedTargets(value: unknown): value is NonNullable<MemberProfile["calculatedTargets"]> {
    return isRecord(value)
        && isFiniteNumber(value.bmi)
        && isFiniteNumber(value.bmr)
        && isFiniteNumber(value.tdee)
        && isFiniteNumber(value.dailyCaloriesKcal)
        && isFiniteNumber(value.proteinGrams)
        && isFiniteNumber(value.fatGrams)
        && isFiniteNumber(value.carbGrams);
}

function isMemberProfile(value: unknown): value is MemberProfile {
    return isRecord(value)
        && isInteger(value.memberId)
        && isBioProfile(value.bioProfile)
        && isNutritionProfile(value.nutritionProfile)
        && isCalculatedTargets(value.calculatedTargets)
        && typeof value.updatedAt === "string";
}

function isApiSuccess(value: unknown): value is MemberProfileApiSuccess {
    return isRecord(value)
        && value.success === true
        && typeof value.message === "string"
        && isMemberProfile(value.data);
}

function isApiError(value: unknown): value is MemberProfileApiErrorResponse {
    return isRecord(value)
        && value.success === false
        && typeof value.errorCode === "string"
        && PROFILE_ERROR_CODES.has(value.errorCode as MemberProfileErrorCode)
        && typeof value.message === "string"
        && (value.details == null || isRecord(value.details));
}

export class MemberProfileApiError extends Error {
    errorCode: MemberProfileErrorCode;
    details: Record<string, unknown>;

    constructor(error: MemberProfileApiErrorResponse) {
        super(error.message);
        this.name = "MemberProfileApiError";
        this.errorCode = error.errorCode;
        this.details = error.details ?? {};
    }
}

function throwProfileError(errorCode: MemberProfileErrorCode, message: string): never {
    throw new MemberProfileApiError({
        success: false,
        errorCode,
        message,
        details: {},
    });
}

export async function getMemberProfile(): Promise<MemberProfileApiSuccess> {
    let payload: unknown;
    let status: number;

    try {
        const response = await httpClient.get("/member/profile");
        payload = response.data;
        status = response.status;
    } catch (error) {
        if (!axios.isAxiosError(error) || !error.response) {
            throwProfileError("NETWORK-001", "Không thể kết nối đến hệ thống. Vui lòng thử lại.");
        }

        payload = error.response.data;
        status = error.response.status;
    }

    if (isApiError(payload)) {
        throw new MemberProfileApiError(payload);
    }

    if (status < 200 || status >= 300 || !isApiSuccess(payload)) {
        throwProfileError("SYS-001", "Hệ thống trả về phản hồi hồ sơ không đúng contract. Vui lòng thử lại.");
    }

    return payload;
}

export async function updateMemberProfile(
    request: MemberProfileUpsertRequest,
): Promise<MemberProfileApiSuccess> {
    let payload: unknown;
    let status: number;

    try {
        const response = await httpClient.put("/member/profile", request);
        payload = response.data;
        status = response.status;
    } catch (error) {
        if (!axios.isAxiosError(error) || !error.response) {
            throwProfileError("NETWORK-001", "Không thể kết nối đến hệ thống. Vui lòng thử lại.");
        }

        payload = error.response.data;
        status = error.response.status;
    }

    if (isApiError(payload)) {
        throw new MemberProfileApiError(payload);
    }

    if (status < 200 || status >= 300 || !isApiSuccess(payload)) {
        throwProfileError("SYS-001", "Hệ thống trả về phản hồi cập nhật hồ sơ không đúng contract.");
    }

    return payload;
}
