import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../lib/httpClient";
import { AUTH_SESSION_STORAGE_KEY, saveAuthSession } from "../storage/authSession";
import { useAuth } from "../hooks/useAuth";
import { AuthProvider } from "./AuthContext";

const currentUserResponse = {
    success: true as const,
    message: "Lấy thông tin người dùng thành công",
    data: {
        id: 88,
        fullName: "Trần Hưng Thịnh",
        email: "thinh@smartgym.com",
        role: "ROLE_MEMBER" as const,
        accountStatus: "ACTIVE" as const,
        createdAt: "2026-07-29T08:00:00Z",
    },
};

function AuthProbe() {
    const { user, isAuthenticated, isRestoringSession, login } = useAuth();
    return (
        <div>
            <span>{isRestoringSession ? "restoring" : "ready"}</span>
            <span>{isAuthenticated ? user?.fullName : "anonymous"}</span>
            <button
                type="button"
                onClick={() => void login({ email: "thinh@smartgym.com", password: "Secret123" }).catch(() => undefined)}
            >
                login
            </button>
        </div>
    );
}

afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
});

describe("AuthContext", () => {
    it("khôi phục user hiện tại từ token trong session", async () => {
        saveAuthSession({
            accessToken: "existing-token",
            tokenType: "Bearer",
            expiresIn: 3600,
            user: {
                id: 88,
                fullName: "Trần Hưng Thịnh",
                email: "thinh@smartgym.com",
                role: "ROLE_MEMBER",
            },
        });
        const getMock = vi.spyOn(httpClient, "get").mockResolvedValue({ data: currentUserResponse, status: 200 });

        render(<AuthProvider><AuthProbe /></AuthProvider>);

        expect(await screen.findByText("Trần Hưng Thịnh")).toBeInTheDocument();
        expect(screen.getByText("ready")).toBeInTheDocument();
        expect(getMock).toHaveBeenCalledWith("/users/me");
    });

    it("xác nhận login bằng /users/me và không lưu password", async () => {
        vi.spyOn(httpClient, "post").mockResolvedValue({
            status: 200,
            data: {
                success: true,
                message: "Đăng nhập thành công",
                data: {
                    accessToken: "new-token",
                    tokenType: "Bearer",
                    expiresIn: 3600,
                    user: {
                        id: 88,
                        fullName: "Trần Hưng Thịnh",
                        email: "thinh@smartgym.com",
                        role: "ROLE_MEMBER",
                    },
                },
            },
        });
        const getMock = vi.spyOn(httpClient, "get").mockResolvedValue({ data: currentUserResponse, status: 200 });
        const user = userEvent.setup();
        render(<AuthProvider><AuthProbe /></AuthProvider>);

        await user.click(screen.getByRole("button", { name: "login" }));

        expect(await screen.findByText("Trần Hưng Thịnh")).toBeInTheDocument();
        expect(getMock).toHaveBeenCalledWith("/users/me");
        const stored = sessionStorage.getItem(AUTH_SESSION_STORAGE_KEY) ?? "";
        expect(stored).toContain("new-token");
        expect(stored).not.toContain("Secret123");
    });

    it("rollback token nếu /users/me không xác nhận được phiên vừa đăng nhập", async () => {
        vi.spyOn(httpClient, "post").mockResolvedValue({
            status: 200,
            data: {
                success: true,
                message: "Đăng nhập thành công",
                data: {
                    accessToken: "unconfirmed-token",
                    tokenType: "Bearer",
                    expiresIn: 3600,
                    user: {
                        id: 88,
                        fullName: "Trần Hưng Thịnh",
                        email: "thinh@smartgym.com",
                        role: "ROLE_MEMBER",
                    },
                },
            },
        });
        vi.spyOn(httpClient, "get").mockRejectedValue(new Error("current user unavailable"));
        const user = userEvent.setup();
        render(<AuthProvider><AuthProbe /></AuthProvider>);

        await user.click(screen.getByRole("button", { name: "login" }));

        await waitFor(() => expect(sessionStorage.getItem(AUTH_SESSION_STORAGE_KEY)).toBeNull());
        expect(screen.getByText("anonymous")).toBeInTheDocument();
    });
});
