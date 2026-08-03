import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../../lib/httpClient";
import type { MemberProfile } from "../types/memberProfile.types";
import { MemberProfilePage } from "./MemberProfilePage";

const profile: MemberProfile = {
    memberId: 101,
    bioProfile: {
        gender: "MALE",
        dateOfBirth: "1998-05-15",
        heightCm: 175,
        weightKg: 70,
        fitnessGoal: "BULK",
        fitnessLevel: "BEGINNER",
        activityLevel: "MODERATELY_ACTIVE",
        workoutDaysPerWeek: 4,
        maxSessionMinutes: 90,
        availableEquipment: ["BARBELL", "DUMBBELL", "CABLE"],
        targetMuscleGroups: ["CHEST", "BACK", "LEGS"],
        injuryConstraints: ["LOWER_BACK_LOAD_LIMITED"],
    },
    nutritionProfile: {
        dietaryPreference: "OMNIVORE",
        foodAllergies: ["PEANUTS"],
        excludedFoods: ["BEEF"],
        mealsPerDay: 4,
    },
    updatedAt: "2026-08-03T08:30:00Z",
};

function successResponse(data: MemberProfile = profile) {
    return {
        status: 200,
        data: {
            success: true,
            message: "Lấy hồ sơ thể trạng thành công",
            data,
        },
    };
}

function apiError(errorCode: string, message: string, status = 404) {
    return Object.assign(new Error(message), {
        isAxiosError: true,
        response: {
            status,
            data: {
                success: false,
                errorCode,
                message,
                details: {},
            },
        },
    });
}

function renderPage() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
        },
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <MemberProfilePage />
        </QueryClientProvider>,
    );
}

afterEach(() => {
    vi.restoreAllMocks();
});

describe("MemberProfilePage", () => {
    it("hiển thị loading state trong khi chờ API", () => {
        vi.spyOn(httpClient, "get").mockImplementation(() => new Promise(() => undefined));

        renderPage();

        expect(screen.getByRole("status")).toHaveTextContent("Đang tải hồ sơ hội viên");
    });

    it("hiển thị empty state và CTA khi nhận PROF-001", async () => {
        vi.spyOn(httpClient, "get").mockRejectedValue(
            apiError("PROF-001", "Hội viên chưa hoàn thiện hồ sơ."),
        );

        renderPage();

        expect(await screen.findByRole("heading", { name: "Chưa hoàn thiện hồ sơ" })).toBeInTheDocument();
        expect(screen.getByText("PROF-001")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Hoàn thiện hồ sơ" })).toBeDisabled();
        expect(screen.getByText(/Chưa có dữ liệu giả/)).toBeInTheDocument();
    });

    it("hiển thị profile read-only đúng contract", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue(successResponse());

        renderPage();

        expect(await screen.findByRole("heading", { name: "Thể trạng và mục tiêu" })).toBeInTheDocument();
        expect(screen.getByText("175 cm")).toBeInTheDocument();
        expect(screen.getByText("70 kg")).toBeInTheDocument();
        expect(screen.getByText("Tăng cơ")).toBeInTheDocument();
        expect(screen.getByText("Vận động vừa")).toBeInTheDocument();
        expect(screen.getByText("Hạn chế tải vùng lưng dưới")).toBeInTheDocument();
        expect(screen.getByRole("heading", { name: "Thói quen ăn uống" })).toBeInTheDocument();
        expect(screen.getByText("Đậu phộng")).toBeInTheDocument();
        expect(screen.queryByText(/BMI|BMR|TDEE/)).not.toBeInTheDocument();
    });

    it("hiển thị lỗi mạng và tải lại thành công", async () => {
        vi.spyOn(httpClient, "get")
            .mockRejectedValueOnce(new Error("offline"))
            .mockResolvedValueOnce(successResponse());
        const user = userEvent.setup();

        renderPage();

        expect(await screen.findByText("NETWORK-001")).toBeInTheDocument();
        await user.click(screen.getByRole("button", { name: "Thử lại" }));

        expect(await screen.findByRole("heading", { name: "Thể trạng và mục tiêu" })).toBeInTheDocument();
        expect(httpClient.get).toHaveBeenCalledTimes(2);
    });

    it("chuyển response sai contract thành SYS-001", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue({
            status: 200,
            data: {
                success: true,
                message: "Lấy hồ sơ thành công",
                data: { memberId: 101 },
            },
        });

        renderPage();

        expect(await screen.findByText("SYS-001")).toBeInTheDocument();
        expect(screen.getByText(/không đúng contract/)).toBeInTheDocument();
    });

    it("phân biệt token hết hạn và không hiển thị nút retry", async () => {
        vi.spyOn(httpClient, "get").mockRejectedValue(
            apiError("ACC-005", "Access Token đã hết hạn.", 401),
        );

        renderPage();

        expect(await screen.findByRole("heading", { name: "Phiên truy cập không còn hiệu lực" })).toBeInTheDocument();
        expect(screen.getByText("ACC-005")).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Thử lại" })).not.toBeInTheDocument();
    });

    it("gửi đúng GET /member/profile", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue(successResponse());

        renderPage();

        await waitFor(() => expect(httpClient.get).toHaveBeenCalledWith("/member/profile"));
    });
});
