import { z } from "zod";
import type { MemberProfileUpsertRequest } from "../types/memberProfile.types";

export type MemberProfileFormValues = Omit<
    MemberProfileUpsertRequest,
    "foodAllergies" | "excludedFoods"
> & {
    foodAllergiesText: string;
    excludedFoodsText: string;
};

const genderSchema = z.enum(["MALE", "FEMALE"]);
const fitnessGoalSchema = z.enum(["BULK", "CUT", "MAINTAIN"]);
const fitnessLevelSchema = z.enum(["BEGINNER", "INTERMEDIATE", "ADVANCED"]);
const activityLevelSchema = z.enum([
    "SEDENTARY",
    "LIGHTLY_ACTIVE",
    "MODERATELY_ACTIVE",
    "VERY_ACTIVE",
]);
const dietaryPreferenceSchema = z.enum(["OMNIVORE", "VEGETARIAN", "VEGAN"]);
const equipmentSchema = z.enum(["BARBELL", "DUMBBELL", "MACHINE", "CABLE", "BENCH"]);
const muscleGroupSchema = z.enum([
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
const contraindicationSchema = z.enum([
    "KNEE_FLEXION_LIMITED",
    "OVERHEAD_MOVEMENT_LIMITED",
    "LOWER_BACK_LOAD_LIMITED",
    "WRIST_FLEXION_LIMITED",
    "NECK_LOAD_LIMITED",
]);

function normalizeList(value: string) {
    return [...new Set(value
        .split(",")
        .map((item) => item.replace(/[\u0000-\u001F\u007F-\u009F]/g, "").trim())
        .filter(Boolean))];
}

function textCollectionSchema(label: string) {
    return z.string().superRefine((value, context) => {
        const items = normalizeList(value);
        if (items.length > 10) {
            context.addIssue({
                code: "custom",
                message: `${label} tối đa 10 phần tử.`,
            });
        }
        if (items.some((item) => item.length > 50)) {
            context.addIssue({
                code: "custom",
                message: `Mỗi phần tử ${label.toLowerCase()} tối đa 50 ký tự.`,
            });
        }
    });
}

function businessDateIso() {
    const parts = new Intl.DateTimeFormat("en-CA", {
        timeZone: "Asia/Ho_Chi_Minh",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
    }).formatToParts(new Date());
    const value = Object.fromEntries(parts.map((part) => [part.type, part.value]));
    return `${value.year}-${value.month}-${value.day}`;
}

function hasAtMostTwoDecimalPlaces(value: number) {
    return Number.isInteger(Math.round(value * 100) - value * 100);
}

export const memberProfileSchema = z.object({
    gender: genderSchema,
    dateOfBirth: z.iso.date("Ngày sinh không đúng định dạng.")
        .min(1, "Ngày sinh là bắt buộc.")
        .refine(
            (value) => value <= businessDateIso(),
            "Ngày sinh không được ở tương lai.",
        ),
    heightCm: z.coerce.number()
        .positive("Chiều cao phải lớn hơn 0.")
        .max(999.99, "Chiều cao vượt giới hạn lưu trữ.")
        .refine(hasAtMostTwoDecimalPlaces, "Chiều cao tối đa 2 chữ số thập phân."),
    weightKg: z.coerce.number()
        .positive("Cân nặng phải lớn hơn 0.")
        .max(9999.99, "Cân nặng vượt giới hạn lưu trữ.")
        .refine(hasAtMostTwoDecimalPlaces, "Cân nặng tối đa 2 chữ số thập phân."),
    fitnessGoal: fitnessGoalSchema,
    fitnessLevel: fitnessLevelSchema,
    activityLevel: activityLevelSchema,
    workoutDaysPerWeek: z.coerce.number()
        .int("Số buổi tập phải là số nguyên.")
        .min(1, "Tối thiểu 1 buổi/tuần.")
        .max(7, "Tối đa 7 buổi/tuần."),
    maxSessionMinutes: z.coerce.number()
        .int("Thời lượng phải là số nguyên.")
        .positive("Thời lượng phải lớn hơn 0.")
        .max(32767, "Thời lượng vượt giới hạn lưu trữ."),
    availableEquipment: z.array(equipmentSchema),
    targetMuscleGroups: z.array(muscleGroupSchema),
    injuryConstraints: z.array(contraindicationSchema),
    dietaryPreference: dietaryPreferenceSchema,
    foodAllergiesText: textCollectionSchema("Danh sách dị ứng"),
    excludedFoodsText: textCollectionSchema("Danh sách thực phẩm loại trừ"),
    mealsPerDay: z.coerce.number()
        .int("Số bữa phải là số nguyên.")
        .min(1, "Tối thiểu 1 bữa/ngày.")
        .max(6, "Tối đa 6 bữa/ngày."),
});

export function toMemberProfileRequest(values: MemberProfileFormValues): MemberProfileUpsertRequest {
    const { foodAllergiesText, excludedFoodsText, ...rest } = values;
    return {
        ...rest,
        heightCm: Number(values.heightCm),
        weightKg: Number(values.weightKg),
        workoutDaysPerWeek: Number(values.workoutDaysPerWeek),
        maxSessionMinutes: Number(values.maxSessionMinutes),
        mealsPerDay: Number(values.mealsPerDay),
        foodAllergies: normalizeList(foodAllergiesText),
        excludedFoods: normalizeList(excludedFoodsText),
    };
}
