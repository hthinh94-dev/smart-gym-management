import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AuthContext } from "../../features/auth/context/AuthContext";
import type { AuthContextValue } from "../../features/auth/context/AuthContext";
import type { AuthUser } from "../../features/auth/types/auth.types";
import { ProtectedRoute } from "./ProtectedRoute";
import { PublicOnlyRoute } from "./PublicOnlyRoute";
import { RoleRoute } from "./RoleRoute";

const member: AuthUser = {
    id: 21,
    fullName: "Nguyễn Minh Khang",
    email: "khang@smartgym.com",
    role: "ROLE_MEMBER",
    accountStatus: "ACTIVE",
    createdAt: "2026-07-29T08:00:00Z",
};

const admin: AuthUser = {
    ...member,
    id: 1,
    fullName: "Quản trị hệ thống",
    email: "admin@smartgym.com",
    role: "ROLE_ADMIN",
};

function renderRoutes(value: Partial<AuthContextValue>, initialEntry = "/admin/users") {
    const contextValue: AuthContextValue = {
        user: null,
        isAuthenticated: false,
        isRestoringSession: false,
        login: vi.fn(),
        logout: vi.fn(),
        ...value,
    };

    render(
        <AuthContext.Provider value={contextValue}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route element={<PublicOnlyRoute />}>
                        <Route path="/login" element={<p>Trang đăng nhập</p>} />
                        <Route path="/register" element={<p>Trang đăng ký</p>} />
                    </Route>
                    <Route element={<ProtectedRoute />}>
                        <Route element={<RoleRoute allowedRoles={["ROLE_MEMBER", "ROLE_PT"]} />}>
                            <Route path="/member" element={<p>Khu vực hội viên</p>} />
                        </Route>
                        <Route element={<RoleRoute allowedRoles={["ROLE_ADMIN"]} />}>
                            <Route path="/admin/users" element={<p>Quản lý tài khoản</p>} />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>,
    );
}

describe("ProtectedRoute và RoleRoute", () => {
    it("chuyển guest về Login", () => {
        renderRoutes({});
        expect(screen.getByText("Trang đăng nhập")).toBeInTheDocument();
    });

    it("không cho Member vào khu vực Admin", () => {
        renderRoutes({ user: member, isAuthenticated: true });
        expect(screen.getByText("Khu vực hội viên")).toBeInTheDocument();
        expect(screen.queryByText("Quản lý tài khoản")).not.toBeInTheDocument();
    });

    it("cho Admin vào khu vực Admin", () => {
        renderRoutes({ user: admin, isAuthenticated: true });
        expect(screen.getByText("Quản lý tài khoản")).toBeInTheDocument();
    });

    it("không cho Admin mở trực tiếp khu vực Member", () => {
        renderRoutes({ user: admin, isAuthenticated: true }, "/member");
        expect(screen.getByText("Quản lý tài khoản")).toBeInTheDocument();
        expect(screen.queryByText("Khu vực hội viên")).not.toBeInTheDocument();
    });

    it("chuyển tài khoản đã có session ra khỏi Login", () => {
        renderRoutes({ user: member, isAuthenticated: true }, "/login");
        expect(screen.getByText("Khu vực hội viên")).toBeInTheDocument();
        expect(screen.queryByText("Trang đăng nhập")).not.toBeInTheDocument();
    });

    it("hiển thị loading ổn định khi đang restore session", () => {
        renderRoutes({ isRestoringSession: true });
        expect(screen.getByRole("status")).toHaveTextContent("Đang khôi phục phiên đăng nhập");
    });
});
