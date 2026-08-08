import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemberSubscriptionPage } from "./MemberSubscriptionPage";

const { getCurrentMemberSubscription, createMemberSubscription, getMembershipPackages, SubscriptionApiErrorMock } = vi.hoisted(() => ({
    getCurrentMemberSubscription: vi.fn(),
    createMemberSubscription: vi.fn(),
    getMembershipPackages: vi.fn(),
    SubscriptionApiErrorMock: class extends Error {
        errorCode: string;
        details: Record<string, unknown>;

        constructor(response: { errorCode: string; message: string; details?: Record<string, unknown> }) {
            super(response.message);
            this.name = "MemberSubscriptionApiError";
            this.errorCode = response.errorCode;
            this.details = response.details ?? {};
        }
    },
}));

vi.mock("../api/memberSubscriptionApi", () => ({
    MemberSubscriptionApiError: SubscriptionApiErrorMock,
    getCurrentMemberSubscription,
    createMemberSubscription,
}));
vi.mock("../../api/membershipPackageApi", () => ({ getMembershipPackages }));

const packages = [{ id: 2, name: "Gói Tiêu Chuẩn", durationDays: 90, price: 1200000, description: "Lịch tập cơ bản." }];

function apiError(errorCode: "SUB-004" | "SUB-006" | "SUB-005") {
    return new SubscriptionApiErrorMock({ errorCode, message: "backend error", details: {} });
}

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(<QueryClientProvider client={client}><MemberSubscriptionPage /></QueryClientProvider>);
}

beforeEach(() => {
    vi.clearAllMocks();
    getCurrentMemberSubscription.mockResolvedValue({ subscriptionId: 4, memberId: 8, packageId: 1, packageName: "Active", status: "ACTIVE", startDate: "2026-08-08", endDate: "2026-11-06", daysRemaining: 90, approvedAt: "2026-08-08T09:00:00Z" });
    getMembershipPackages.mockResolvedValue(packages);
    createMemberSubscription.mockResolvedValue({ subscriptionId: 9, memberId: 8, packageId: 2, packageName: "Gói Tiêu Chuẩn", price: 1200000, status: "PENDING", requestedAt: "2026-08-08T09:00:00Z" });
});

describe("MemberSubscriptionPage", () => {
    it("ẩn lựa chọn đăng ký khi Member đã có ACTIVE", async () => {
        renderPage();

        expect(await screen.findByText("Bạn đang có gói ACTIVE")).toBeInTheDocument();
        expect(screen.queryByRole("heading", { name: "Chọn gói tập phù hợp" })).not.toBeInTheDocument();
        expect(getMembershipPackages).not.toHaveBeenCalled();
    });

    it("cho phép tạo request và hiển thị snapshot PENDING", async () => {
        getCurrentMemberSubscription.mockRejectedValueOnce(apiError("SUB-005"));
        const user = userEvent.setup();
        renderPage();

        await user.click(await screen.findByRole("radio", { name: /gói tiêu chuẩn/i }));
        await user.click(screen.getByRole("button", { name: "Đăng ký gói tập" }));

        expect(createMemberSubscription).toHaveBeenCalledWith(2);
        expect(await screen.findByText("Yêu cầu đang chờ duyệt")).toBeInTheDocument();
        expect(screen.getByText("PENDING")).toBeInTheDocument();
        expect(screen.getByText("Gói Tiêu Chuẩn")).toBeInTheDocument();
    });

    it.each([
        ["SUB-004", "Bạn đang có một gói ACTIVE"],
        ["SUB-006", "Bạn đã có một yêu cầu đăng ký đang chờ xử lý"],
    ] as const)("hiển thị lỗi nghiệp vụ %s", async (code, message) => {
        getCurrentMemberSubscription.mockRejectedValueOnce(apiError("SUB-005"));
        createMemberSubscription.mockRejectedValueOnce(apiError(code));
        const user = userEvent.setup();
        renderPage();

        await user.click(await screen.findByRole("radio", { name: /gói tiêu chuẩn/i }));
        await user.click(screen.getByRole("button", { name: "Đăng ký gói tập" }));

        expect(await screen.findByRole("alert")).toHaveTextContent(message);
        if (code === "SUB-006") expect(screen.getByText("PENDING")).toBeInTheDocument();
    });
});
