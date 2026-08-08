import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../../lib/httpClient";
import { createMemberSubscription, getCurrentMemberSubscription } from "./memberSubscriptionApi";

const pending = { subscriptionId: 55, memberId: 101, packageId: 2, packageName: "Gói Tiêu Chuẩn", price: 1200000, status: "PENDING" as const, requestedAt: "2026-08-08T09:00:00Z" };
const active = { subscriptionId: 55, memberId: 101, packageId: 2, packageName: "Gói Tiêu Chuẩn", status: "ACTIVE" as const, startDate: "2026-08-08", endDate: "2026-11-06", daysRemaining: 90, approvedAt: "2026-08-08T09:30:00Z" };

afterEach(() => vi.restoreAllMocks());

describe("memberSubscriptionApi", () => {
    it("tạo yêu cầu đăng ký với packageId và nhận snapshot PENDING", async () => {
        vi.spyOn(httpClient, "post").mockResolvedValue({ status: 201, data: { success: true, message: "ok", data: pending } });

        await expect(createMemberSubscription(2)).resolves.toEqual(pending);
        expect(httpClient.post).toHaveBeenCalledWith("/member/subscriptions", { packageId: 2 });
    });

    it("phân biệt lỗi nghiệp vụ SUB-003 từ backend", async () => {
        vi.spyOn(httpClient, "post").mockRejectedValue({
            isAxiosError: true,
            response: { status: 409, data: { success: false, errorCode: "SUB-003", message: "inactive", details: {} } },
        });

        await expect(createMemberSubscription(4)).rejects.toMatchObject({ errorCode: "SUB-003" });
    });

    it("phân biệt lỗi mạng", async () => {
        vi.spyOn(httpClient, "get").mockRejectedValue(new Error("offline"));

        await expect(getCurrentMemberSubscription()).rejects.toMatchObject({ errorCode: "NETWORK-001" });
    });

    it("tải current subscription ACTIVE đúng contract", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: active } });

        await expect(getCurrentMemberSubscription()).resolves.toEqual(active);
        expect(httpClient.get).toHaveBeenCalledWith("/member/subscriptions/current");
    });
});
