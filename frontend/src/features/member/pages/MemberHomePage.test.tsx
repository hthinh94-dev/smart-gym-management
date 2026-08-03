import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AuthContext } from "../../auth/context/AuthContext";
import type { AuthContextValue } from "../../auth/context/AuthContext";
import type { AuthUser } from "../../auth/types/auth.types";
import { MemberHomePage } from "./MemberHomePage";

const member: AuthUser = {
    id: 21,
    fullName: "Nguyễn Minh Khang",
    email: "khang@smartgym.com",
    role: "ROLE_MEMBER",
    accountStatus: "ACTIVE",
    createdAt: "2026-07-29T08:00:00Z",
};

function renderMemberHome(user: AuthUser) {
    const value: AuthContextValue = {
        user,
        isAuthenticated: true,
        isRestoringSession: false,
        login: vi.fn(),
        logout: vi.fn(),
    };

    render(
        <AuthContext.Provider value={value}>
            <MemberHomePage />
        </AuthContext.Provider>,
    );
}

describe("MemberHomePage", () => {
    it("hiển thị thông tin Member từ session", () => {
        renderMemberHome(member);

        expect(screen.getByRole("heading", { name: /Nguyễn Minh Khang/ })).toBeInTheDocument();
        expect(screen.getAllByText("Hội viên").length).toBeGreaterThan(0);
        expect(screen.getByText("khang@smartgym.com")).toBeInTheDocument();
        expect(screen.getAllByText("Đang hoạt động").length).toBeGreaterThan(0);
    });

    it("hiển thị đúng vai trò huấn luyện viên", () => {
        renderMemberHome({ ...member, role: "ROLE_PT" });

        expect(screen.getAllByText("Huấn luyện viên").length).toBeGreaterThan(0);
    });
});
