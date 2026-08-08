import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SubscriptionStatus } from "./SubscriptionStatus";

describe("SubscriptionStatus", () => {
    it("hiển thị current ACTIVE và ngày còn lại", () => {
        render(<SubscriptionStatus current={{ subscriptionId: 1, memberId: 2, packageId: 3, packageName: "Gói 90 ngày", status: "ACTIVE", startDate: "2026-08-08", endDate: "2026-11-06", daysRemaining: 90, approvedAt: "2026-08-08T09:00:00Z" }} />);

        expect(screen.getByText("ACTIVE")).toBeInTheDocument();
        expect(screen.getByText("Gói 90 ngày")).toBeInTheDocument();
        expect(screen.getByText("90 ngày")).toBeInTheDocument();
        expect(screen.getByText(/không thể tạo yêu cầu đăng ký mới/i)).toBeInTheDocument();
    });

    it("hiển thị snapshot của yêu cầu PENDING", () => {
        render(<SubscriptionStatus pending={{ subscriptionId: 1, memberId: 2, packageId: 3, packageName: "Gói 30 ngày", price: 300000, status: "PENDING", requestedAt: "2026-08-08T09:00:00Z" }} pendingDurationDays={30} />);

        expect(screen.getByText("PENDING")).toBeInTheDocument();
        expect(screen.getByText("Gói 30 ngày")).toBeInTheDocument();
        expect(screen.getByText("30 ngày")).toBeInTheDocument();
        expect(screen.getByText(/chờ quản trị viên phê duyệt/i)).toBeInTheDocument();
    });
});
