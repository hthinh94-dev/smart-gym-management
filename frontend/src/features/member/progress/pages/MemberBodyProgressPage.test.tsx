import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../../lib/httpClient";
import { MemberBodyProgressPage } from "./MemberBodyProgressPage";

const item = { id: 1, memberId: 101, recordDate: "2026-08-04", weightKg: 72.2, createdAt: "2026-08-04T01:00:00Z", updatedAt: "2026-08-04T01:00:00Z" };
const profile = {
    memberId: 101,
    bioProfile: {
        gender: "MALE", dateOfBirth: "1998-05-15", heightCm: 175, weightKg: 72.2,
        fitnessGoal: "WEIGHT_LOSS", fitnessGoals: ["WEIGHT_LOSS"], targetWeightKg: 71.9,
        fitnessLevel: "BEGINNER", activityLevel: "MODERATELY_ACTIVE", workoutDaysPerWeek: 4,
        maxSessionMinutes: 90, availableEquipment: ["DUMBBELL"], targetMuscleGroups: ["FULL_BODY"],
        injuryConstraints: [],
    },
    nutritionProfile: { dietaryPreference: "OMNIVORE", foodAllergies: [], excludedFoods: [], mealsPerDay: 4 },
    calculatedTargets: { bmi: 23.58, bmiCategory: "NORMAL", bmr: 1700, tdee: 2500, dailyCaloriesKcal: 2200, proteinGrams: 150, fatGrams: 70, carbGrams: 240 },
    updatedAt: "2026-08-05T01:00:00Z",
};
function renderPage() { const client = new QueryClient({ defaultOptions: { queries: { retry: false } } }); return render(<QueryClientProvider client={client}><MemberBodyProgressPage /></QueryClientProvider>); }
afterEach(() => vi.restoreAllMocks());
describe("MemberBodyProgressPage", () => {
    it("tính số kg đã giảm và chúc mừng khi đạt cân nặng mục tiêu", async () => {
        vi.spyOn(httpClient, "get").mockImplementation((url) => Promise.resolve({
            status: 200,
            data: { success: true, message: "ok", data: url === "/member/profile" ? profile : [item] },
        }));
        vi.spyOn(httpClient, "post").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: { ...item, recordDate: "2026-08-06", weightKg: 71.9 } } });
        const user = userEvent.setup();

        renderPage();
        expect((await screen.findAllByText("72.20 kg")).length).toBeGreaterThan(0);
        expect(screen.getByText("Còn cách mục tiêu 0.30 kg")).toBeInTheDocument();
        await user.type(screen.getByLabelText("Cân nặng (kg) *"), "71.9");
        await user.click(screen.getByRole("button", { name: "Lưu cân nặng" }));

        expect(await screen.findByRole("status")).toHaveTextContent("Giảm 0.30 kg so với cân nặng ban đầu");
        expect(screen.getByText("Đã đạt cân nặng mục tiêu")).toBeInTheDocument();
        expect(await screen.findByText("Chúc mừng bạn!")).toBeInTheDocument();
        expect(screen.getAllByText("71.90 kg").length).toBeGreaterThan(0);
    });

    it("offers retry when history loading fails", async () => {
        vi.spyOn(httpClient, "get").mockRejectedValue(new Error("offline"));
        renderPage();
        expect(await screen.findByRole("alert")).toHaveTextContent("Không thể tải lịch sử cân nặng");
        expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
