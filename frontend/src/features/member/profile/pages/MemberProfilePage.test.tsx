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
    calculatedTargets: {
        bmi: 22.86,
        bmr: 1658.75,
        tdee: 2571.06,
        dailyCaloriesKcal: 2871.06,
        proteinGrams: 154,
        fatGrams: 79.75,
        carbGrams: 384.32,
    },
    updatedAt: "2026-08-03T08:30:00Z",
};

const savedProfile: MemberProfile = { ...profile };

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

function apiError(
    errorCode: string,
    message: string,
    status = 404,
    details: Record<string, unknown> = {},
) {
    return Object.assign(new Error(message), {
        isAxiosError: true,
        response: {
            status,
            data: {
                success: false,
                errorCode,
                message,
                details,
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
        expect(screen.getByRole("button", { name: "Hoàn thiện hồ sơ" })).toBeEnabled();
        await userEvent.setup().click(screen.getByRole("button", { name: "Hoàn thiện hồ sơ" }));
        expect(screen.getByRole("heading", { name: "Thông tin của bạn" })).toBeInTheDocument();
        expect(screen.queryByText(/Chưa có dữ liệu giả/)).not.toBeInTheDocument();
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
        expect(screen.getByRole("heading", { name: "Chỉ tiêu của bạn" })).toBeInTheDocument();
        expect(screen.getByText("1658.75")).toBeInTheDocument();
        expect(screen.getByText("2571.06")).toBeInTheDocument();
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

    it("hiển thị calculated targets sau khi cập nhật hồ sơ thành công", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue(successResponse());
        vi.spyOn(httpClient, "put").mockResolvedValue({
            status: 200,
            data: { success: true, message: "Đã cập nhật", data: savedProfile },
        });
        const user = userEvent.setup();

        renderPage();
        await user.click(await screen.findByRole("button", { name: "Chỉnh sửa" }));
        await user.click(screen.getByRole("button", { name: "Lưu hồ sơ" }));

        await waitFor(() => expect(httpClient.put).toHaveBeenCalledWith("/member/profile", expect.objectContaining({
            gender: "MALE",
            foodAllergies: ["PEANUTS"],
            targetMuscleGroups: ["CHEST", "BACK", "LEGS"],
        })));
        expect(await screen.findByRole("heading", { name: "Chỉ tiêu của bạn" })).toBeInTheDocument();
        expect(screen.getByText("22.86")).toBeInTheDocument();
        expect(screen.getByRole("status")).toHaveTextContent("Đã lưu hồ sơ.");
    });

    it("không gửi PUT khi ngày sinh ở tương lai", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue(successResponse());
        const put = vi.spyOn(httpClient, "put");
        const user = userEvent.setup();

        renderPage();
        await user.click(await screen.findByRole("button", { name: "Chỉnh sửa" }));
        const dateInput = screen.getByLabelText("Ngày sinh *");
        await user.clear(dateInput);
        await user.type(dateInput, "2099-01-01");
        await user.click(screen.getByRole("button", { name: "Lưu hồ sơ" }));

        expect(await screen.findByText("Ngày sinh không được ở tương lai.")).toBeInTheDocument();
        expect(put).not.toHaveBeenCalled();
    });

    it("hiển thị lỗi field VAL-001 từ backend", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue(successResponse());
        vi.spyOn(httpClient, "put").mockRejectedValue(apiError(
            "VAL-001",
            "Dữ liệu không hợp lệ.",
            400,
            { violations: { heightCm: "Chiều cao không hợp lệ." } },
        ));
        const user = userEvent.setup();

        renderPage();
        await user.click(await screen.findByRole("button", { name: "Chỉnh sửa" }));
        await user.click(screen.getByRole("button", { name: "Lưu hồ sơ" }));

        expect(await screen.findByText("Chiều cao không hợp lệ.")).toBeInTheDocument();
    });

    it("không gửi trùng PUT khi request đang xử lý", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue(successResponse());
        const put = vi.spyOn(httpClient, "put").mockImplementation(() => new Promise(() => undefined));
        const user = userEvent.setup();

        renderPage();
        await user.click(await screen.findByRole("button", { name: "Chỉnh sửa" }));
        const submit = screen.getByRole("button", { name: "Lưu hồ sơ" });
        await user.click(submit);

        await waitFor(() => expect(screen.getByRole("button", { name: "Đang lưu..." })).toBeDisabled());
        await user.click(screen.getByRole("button", { name: "Đang lưu..." }));
        expect(put).toHaveBeenCalledTimes(1);
    });
});
