import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
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

function renderMemberHome(user: AuthUser, logout = vi.fn()) {
    const value: AuthContextValue = {
        user,
        isAuthenticated: true,
        isRestoringSession: false,
        login: vi.fn(),
        logout,
    };

    render(
        <AuthContext.Provider value={value}>
            <MemoryRouter initialEntries={["/member"]}>
                <Routes>
                    <Route path="/member" element={<MemberHomePage />} />
                    <Route path="/login" element={<p>Trang đăng nhập</p>} />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>,
    );

    return logout;
}

describe("MemberHomePage", () => {
    it("hiển thị thông tin Member từ session", () => {
        renderMemberHome(member);

        expect(screen.getByRole("heading", { name: /Nguyễn Minh Khang/ })).toBeInTheDocument();
        expect(screen.getAllByText("Hội viên").length).toBeGreaterThan(0);
        expect(screen.getAllByText("khang@smartgym.com").length).toBeGreaterThan(0);
        expect(screen.getAllByText("Đang hoạt động").length).toBeGreaterThan(0);
    });

    it("hiển thị đúng vai trò huấn luyện viên", () => {
        renderMemberHome({ ...member, role: "ROLE_PT" });
        expect(screen.getAllByText("Huấn luyện viên").length).toBeGreaterThan(0);
    });

    it("logout và điều hướng về Login", async () => {
        const logout = renderMemberHome(member);
        const user = userEvent.setup();

        await user.click(screen.getByRole("button", { name: "Đăng xuất" }));

        expect(logout).toHaveBeenCalledOnce();
        expect(screen.getByText("Trang đăng nhập")).toBeInTheDocument();
    });
});
