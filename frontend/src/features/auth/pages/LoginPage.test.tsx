import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../lib/httpClient";
import { AuthProvider } from "../context/AuthContext";
import { AUTH_SESSION_STORAGE_KEY } from "../storage/authSession";
import { LoginPage } from "./LoginPage";

const loginSuccess = {
    success: true as const,
    message: "Đăng nhập thành công",
    data: {
        accessToken: "jwt-access-token",
        tokenType: "Bearer" as const,
        expiresIn: 3600,
        user: {
            id: 101,
            fullName: "Nguyễn Văn An",
            email: "user@gmail.com",
            role: "ROLE_MEMBER" as const,
        },
    },
};

const currentUserSuccess = {
    success: true as const,
    message: "Lấy thông tin người dùng thành công",
    data: {
        id: 101,
        fullName: "Nguyễn Văn An",
        email: "user@gmail.com",
        role: "ROLE_MEMBER" as const,
        accountStatus: "ACTIVE" as const,
        createdAt: "2026-07-15T08:00:00Z",
    },
};

function renderLoginPage() {
    const queryClient = new QueryClient({
        defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
    });

    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={["/login"]}>
                <AuthProvider>
                    <LoginPage />
                </AuthProvider>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function fillLoginForm(email = "user@gmail.com", password = "SecurePass1") {
    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/^email/i), email);
    await user.type(screen.getByLabelText(/^mật khẩu/i), password);
    return user;
}

function apiFailure(errorCode: "ACC-007" | "ACC-004" | "ACC-006") {
    const messages = {
        "ACC-007": "Tên đăng nhập hoặc mật khẩu không chính xác.",
        "ACC-004": "Tài khoản của bạn đã bị khóa.",
        "ACC-006": "Tài khoản đã bị vô hiệu hóa vĩnh viễn.",
    };

    return {
        isAxiosError: true,
        response: {
            status: errorCode === "ACC-007" ? 401 : 403,
            data: {
                success: false,
                errorCode,
                message: messages[errorCode],
                details: errorCode === "ACC-007"
                    ? null
                    : { accountStatus: errorCode === "ACC-004" ? "LOCKED" : "DISABLED" },
            },
        },
    };
}

afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
});

describe("LoginPage", () => {
    it("không gửi request khi form thiếu dữ liệu", async () => {
        const postMock = vi.spyOn(httpClient, "post");
        const user = userEvent.setup();
        renderLoginPage();

        await user.click(screen.getByRole("button", { name: /^đăng nhập$/i }));

        expect(await screen.findByText("Email là bắt buộc.")).toBeInTheDocument();
        expect(screen.getByText("Mật khẩu là bắt buộc.")).toBeInTheDocument();
        expect(postMock).not.toHaveBeenCalled();
    });

    it("đăng nhập thành công, lưu token và gọi /users/me", async () => {
        const postMock = vi.spyOn(httpClient, "post").mockResolvedValue({
            data: loginSuccess,
            status: 200,
        });
        const getMock = vi.spyOn(httpClient, "get").mockResolvedValue({
            data: currentUserSuccess,
            status: 200,
        });
        renderLoginPage();
        const user = await fillLoginForm("  USER@gmail.com  ");

        await user.click(screen.getByRole("button", { name: /^đăng nhập$/i }));

        expect(await screen.findByText("Đăng nhập thành công")).toBeInTheDocument();
        expect(screen.getByText(/Nguyễn Văn An/)).toBeInTheDocument();
        expect(postMock).toHaveBeenCalledWith("/auth/login", {
            email: "user@gmail.com",
            password: "SecurePass1",
        });
        expect(getMock).toHaveBeenCalledWith("/users/me");

        const storedSession = sessionStorage.getItem(AUTH_SESSION_STORAGE_KEY) ?? "";
        expect(storedSession).toContain("jwt-access-token");
        expect(storedSession).not.toContain("SecurePass1");
        expect(storedSession).not.toContain("password");
    });

    it("không hiện lại success cũ khi sửa form sau một lần đăng nhập thành công", async () => {
        vi.spyOn(httpClient, "post").mockResolvedValue({
            data: loginSuccess,
            status: 200,
        });
        vi.spyOn(httpClient, "get").mockResolvedValue({
            data: currentUserSuccess,
            status: 200,
        });
        renderLoginPage();
        const user = await fillLoginForm();

        await user.click(screen.getByRole("button", { name: /^đăng nhập$/i }));
        expect(await screen.findByText("Đăng nhập thành công")).toBeInTheDocument();

        const passwordInput = screen.getByLabelText(/^mật khẩu/i);
        await user.clear(passwordInput);
        await user.click(screen.getByRole("button", { name: /^đăng nhập$/i }));
        expect(await screen.findByText("Mật khẩu là bắt buộc.")).toBeInTheDocument();

        await user.type(passwordInput, "SecurePass1");
        expect(screen.queryByText("Đăng nhập thành công")).not.toBeInTheDocument();
    });

    it("không hiển thị banner success của phiên tài khoản đã có trước đó", async () => {
        sessionStorage.setItem(AUTH_SESSION_STORAGE_KEY, JSON.stringify({
            accessToken: "old-account-token",
            tokenType: "Bearer",
            expiresAt: Date.now() + 3_600_000,
        }));
        vi.spyOn(httpClient, "get").mockResolvedValue({
            data: currentUserSuccess,
            status: 200,
        });

        renderLoginPage();

        await waitFor(() => expect(httpClient.get).toHaveBeenCalledWith("/users/me"));
        expect(screen.queryByText("Đăng nhập thành công")).not.toBeInTheDocument();
        expect(screen.queryByText(/Nguyễn Văn An/)).not.toBeInTheDocument();
    });

    it.each([
        ["ACC-007", "Thông tin đăng nhập không đúng"],
        ["ACC-004", "Tài khoản bị khóa"],
        ["ACC-006", "Tài khoản đã bị vô hiệu hóa"],
    ] as const)("hiển thị đúng lỗi %s", async (errorCode, expectedTitle) => {
        vi.spyOn(httpClient, "post").mockRejectedValue(apiFailure(errorCode));
        const getMock = vi.spyOn(httpClient, "get");
        renderLoginPage();
        const user = await fillLoginForm();

        await user.click(screen.getByRole("button", { name: /^đăng nhập$/i }));

        expect(await screen.findByText(expectedTitle)).toBeInTheDocument();
        expect(getMock).not.toHaveBeenCalled();
    });

    it("giữ email khi không thể kết nối backend", async () => {
        vi.spyOn(httpClient, "post").mockRejectedValue(new Error("connection refused"));
        renderLoginPage();
        const user = await fillLoginForm();

        await user.click(screen.getByRole("button", { name: /^đăng nhập$/i }));

        expect(await screen.findByText("Không thể kết nối")).toBeInTheDocument();
        expect(screen.getByLabelText(/^email/i)).toHaveValue("user@gmail.com");
    });

    it("giữ trạng thái loading cho đến khi request hoàn tất", async () => {
        let resolveLogin!: (value: { data: typeof loginSuccess; status: number }) => void;
        vi.spyOn(httpClient, "post").mockImplementation(() => new Promise((resolve) => {
            resolveLogin = resolve;
        }));
        vi.spyOn(httpClient, "get").mockResolvedValue({ data: currentUserSuccess, status: 200 });
        renderLoginPage();
        const user = await fillLoginForm();

        await user.click(screen.getByRole("button", { name: /^đăng nhập$/i }));

        expect(await screen.findByRole("button", { name: /đang đăng nhập/i })).toBeDisabled();
        resolveLogin({ data: loginSuccess, status: 200 });
        await screen.findByText("Đăng nhập thành công");
    });

    it("hiện và ẩn mật khẩu bằng nút icon", async () => {
        renderLoginPage();
        const passwordInput = screen.getByLabelText(/^mật khẩu/i);
        const user = userEvent.setup();

        expect(passwordInput).toHaveAttribute("type", "password");
        await user.click(screen.getByRole("button", { name: "Hiện mật khẩu" }));
        expect(passwordInput).toHaveAttribute("type", "text");
        await user.click(screen.getByRole("button", { name: "Ẩn mật khẩu" }));
        expect(passwordInput).toHaveAttribute("type", "password");
    });
});
