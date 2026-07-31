import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AuthContext } from "../features/auth/context/AuthContext";
import type { AuthContextValue } from "../features/auth/context/AuthContext";
import { AdminLayout } from "./AdminLayout";

describe("AdminLayout", () => {
    it("hiển thị phiên Admin và logout về Login", async () => {
        const logout = vi.fn();
        const value: AuthContextValue = {
            user: {
                id: 1,
                fullName: "Quản trị hệ thống",
                email: "admin@smartgym.com",
                role: "ROLE_ADMIN",
                accountStatus: "ACTIVE",
                createdAt: "2026-07-01T08:00:00Z",
            },
            isAuthenticated: true,
            isRestoringSession: false,
            login: vi.fn(),
            logout,
        };
        const user = userEvent.setup();

        render(
            <AuthContext.Provider value={value}>
                <MemoryRouter initialEntries={["/admin/users"]}>
                    <Routes>
                        <Route path="/admin" element={<AdminLayout />}>
                            <Route path="users" element={<p id="main-content">Danh sách tài khoản</p>} />
                        </Route>
                        <Route path="/login" element={<p>Trang đăng nhập</p>} />
                    </Routes>
                </MemoryRouter>
            </AuthContext.Provider>,
        );

        expect(screen.getByText("admin@smartgym.com")).toBeInTheDocument();
        await user.click(screen.getByRole("button", { name: "Đăng xuất" }));

        expect(logout).toHaveBeenCalledOnce();
        expect(screen.getByText("Trang đăng nhập")).toBeInTheDocument();
    });
});
