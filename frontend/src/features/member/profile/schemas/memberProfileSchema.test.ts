import { describe, expect, it } from "vitest";
import { memberProfileSchema, toMemberProfileRequest, type MemberProfileFormValues } from "./memberProfileSchema";

const validValues: MemberProfileFormValues = {
    gender: "MALE",
    dateOfBirth: "1998-05-15",
    heightCm: 175,
    weightKg: 70,
    fitnessGoal: "MUSCLE_GAIN",
    fitnessGoals: ["MUSCLE_GAIN"],
    targetWeightKg: "",
    fitnessLevel: "BEGINNER",
    activityLevel: "MODERATELY_ACTIVE",
    workoutDaysPerWeek: 4,
    maxSessionMinutes: 90,
    availableEquipment: ["BARBELL"],
    targetMuscleGroups: ["CHEST"],
    injuryConstraints: [],
    mobilityLimitNotes: "",
    foodAllergies: [],
    excludedFoods: [],
    dietaryPreference: "OMNIVORE",
    foodAllergiesText: "PEANUTS",
    excludedFoodsText: "BEEF",
    mealsPerDay: 4,
};

describe("memberProfileSchema", () => {
    it("chấp nhận hồ sơ hợp lệ", () => {
        expect(memberProfileSchema.safeParse(validValues).success).toBe(true);
    });

    it("từ chối danh sách trên 10 phần tử", () => {
        const values = {
            ...validValues,
            foodAllergiesText: Array.from({ length: 11 }, (_, index) => `FOOD_${index}`).join(","),
        };

        const result = memberProfileSchema.safeParse(values);
        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.issues[0]?.message).toContain("tối đa 10 phần tử");
        }
    });

    it("từ chối phần tử trên 50 ký tự", () => {
        const result = memberProfileSchema.safeParse({
            ...validValues,
            excludedFoodsText: "A".repeat(51),
        });

        expect(result.success).toBe(false);
    });

    it("từ chối số đo có trên 2 chữ số thập phân", () => {
        const result = memberProfileSchema.safeParse({
            ...validValues,
            weightKg: 70.123,
        });

        expect(result.success).toBe(false);
    });

    it("bắt buộc cân nặng đích đúng hướng cho mục tiêu tăng hoặc giảm cân", () => {
        const gain = memberProfileSchema.safeParse({
            ...validValues,
            fitnessGoal: "WEIGHT_GAIN",
            fitnessGoals: ["WEIGHT_GAIN"],
            targetWeightKg: 69,
        });
        const loss = memberProfileSchema.safeParse({
            ...validValues,
            fitnessGoal: "WEIGHT_LOSS",
            fitnessGoals: ["WEIGHT_LOSS"],
            targetWeightKg: 70,
        });

        expect(gain.success).toBe(false);
        expect(loss.success).toBe(false);
    });

    it("cho phép tối đa hai mục tiêu và giữ mục tiêu đầu tiên làm mục tiêu chính", () => {
        const values = {
            ...validValues,
            fitnessGoal: "WEIGHT_LOSS" as const,
            fitnessGoals: ["WEIGHT_LOSS", "FAT_LOSS"] as const,
            targetWeightKg: 65,
        };

        const result = memberProfileSchema.safeParse(values);
        expect(result.success).toBe(true);
        if (result.success) {
            expect(toMemberProfileRequest(result.data).fitnessGoal).toBe("WEIGHT_LOSS");
        }
    });

    it("trim, loại control character, phần tử rỗng và giá trị trùng", () => {
        const request = toMemberProfileRequest({
            ...validValues,
            foodAllergiesText: " PEANUTS,\u0000MILK, PEANUTS,  ",
        });

        expect(request.foodAllergies).toEqual(["PEANUTS", "MILK"]);
    });
});
