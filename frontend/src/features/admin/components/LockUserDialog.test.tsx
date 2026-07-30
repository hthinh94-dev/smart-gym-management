import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { AdminUser } from "../types/adminUser.types";
import { LockUserDialog } from "./LockUserDialog";

const member: AdminUser = {
    id: 101,
    fullName: "Nguyễn Văn An",
    email: "an@gmail.com",
    role: "ROLE_MEMBER",
    accountStatus: "ACTIVE",
    createdAt: "2026-07-15T08:00:00Z",
    hasActiveSubscription: true,
};

describe("LockUserDialog", () => {
    it("không gọi API khi lý do khóa không hợp lệ", async () => {
        const onConfirm = vi.fn();
        const user = userEvent.setup();
        render(
            <LockUserDialog
                user={member}
                isPending={false}
                onClose={vi.fn()}
                onConfirm={onConfirm}
            />,
        );

        await user.type(screen.getByLabelText(/lý do khóa/i), "Quá ngắn");
        await user.click(screen.getByRole("button", { name: "Khóa tài khoản" }));

        expect(screen.getByRole("alert")).toHaveTextContent("10 đến 500 ký tự");
        expect(onConfirm).not.toHaveBeenCalled();
    });

    it("chuẩn hóa và gửi lý do hợp lệ", async () => {
        const onConfirm = vi.fn();
        const user = userEvent.setup();
        render(
            <LockUserDialog
                user={member}
                isPending={false}
                onClose={vi.fn()}
                onConfirm={onConfirm}
            />,
        );

        await user.type(screen.getByLabelText(/lý do khóa/i), "  Vi phạm nội quy phòng tập nhiều lần.  ");
        await user.click(screen.getByRole("button", { name: "Khóa tài khoản" }));

        expect(onConfirm).toHaveBeenCalledWith("Vi phạm nội quy phòng tập nhiều lần.");
    });
});
