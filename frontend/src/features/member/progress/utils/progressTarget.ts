import type { FitnessGoal } from "../../profile/types/memberProfile.types";

export function targetDistanceLabel(
    weightKg: number,
    targetWeightKg: number | null | undefined,
): string | undefined {
    if (targetWeightKg == null) return undefined;
    const difference = Math.abs(weightKg - targetWeightKg);
    if (difference < 0.005) return "Đã đạt cân nặng mục tiêu";
    return `Còn cách mục tiêu ${difference.toFixed(2)} kg`;
}

export function hasReachedTarget(
    weightKg: number,
    targetWeightKg: number | null | undefined,
    goals: FitnessGoal[],
): boolean {
    if (targetWeightKg == null) return false;
    if (goals.includes("WEIGHT_GAIN")) return weightKg >= targetWeightKg;
    if (goals.includes("WEIGHT_LOSS")) return weightKg <= targetWeightKg;
    return false;
}

export function baselineDifferenceLabel(
    weightKg: number,
    initialWeightKg: number | null | undefined,
): string | undefined {
    if (initialWeightKg == null) return undefined;
    const difference = weightKg - initialWeightKg;
    if (Math.abs(difference) < 0.005) return "Cân nặng ban đầu";
    return difference > 0
        ? `Tăng ${difference.toFixed(2)} kg so với cân nặng ban đầu`
        : `Giảm ${Math.abs(difference).toFixed(2)} kg so với cân nặng ban đầu`;
}
