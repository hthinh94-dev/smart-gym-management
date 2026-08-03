import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AuthContext } from "../features/auth/context/AuthContext";
import type { AuthContextValue } from "../features/auth/context/AuthContext";
import type { AuthUser } from "../features/auth/types/auth.types";
import { MemberLayout } from "./MemberLayout";

const member: AuthUser = {
    id: 21,
    fullName: "Nguyễn Minh Khang",
    email: "khang@smartgym.com",
    role: "ROLE_MEMBER",
    accountStatus: "ACTIVE",
    createdAt: "2026-07-29T08:00:00Z",
};

function renderLayout(user: AuthUser, initialEntry = "/member/profile", logout = vi.fn()) {
    const value: AuthContextValue = {
        user,
        isAuthenticated: true,
        isRestoringSession: false,
        login: vi.fn(),
        logout,
    };

    render(
        <AuthContext.Provider value={value}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route path="/member" element={<MemberLayout />}>
                        <Route index element={<p id="main-content">Trang tổng quan</p>} />
                        <Route path="profile" element={<p id="main-content">Trang hồ sơ</p>} />
                    </Route>
                    <Route path="/login" element={<p>Trang đăng nhập</p>} />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>,
    );

    return logout;
}

describe("MemberLayout", () => {
    it("hiển thị navigation và đánh dấu đúng trang hồ sơ", () => {
        renderLayout(member);

        expect(screen.getByRole("link", { name: "Tổng quan" })).not.toHaveClass("active");
        expect(screen.getByRole("link", { name: "Hồ sơ" })).toHaveClass("active");
        expect(screen.getByText("Trang hồ sơ")).toBeInTheDocument();
        expect(screen.getByText("khang@smartgym.com", { exact: false })).toBeInTheDocument();
    });

    it("ẩn navigation hồ sơ với ROLE_PT", () => {
        renderLayout({ ...member, role: "ROLE_PT" }, "/member");

        expect(screen.queryByRole("link", { name: "Hồ sơ" })).not.toBeInTheDocument();
        expect(screen.getByText("Huấn luyện viên", { exact: false })).toBeInTheDocument();
    });

    it("logout và điều hướng về Login", async () => {
        const logout = renderLayout(member, "/member");
        const user = userEvent.setup();

        await user.click(screen.getByRole("button", { name: "Đăng xuất" }));

        expect(logout).toHaveBeenCalledOnce();
        expect(screen.getByText("Trang đăng nhập")).toBeInTheDocument();
    });
});
