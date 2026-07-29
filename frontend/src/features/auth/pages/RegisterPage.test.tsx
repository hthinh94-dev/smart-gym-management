import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../lib/httpClient";
import { RegisterPage } from "./RegisterPage";

const postMock = vi.spyOn(httpClient, "post");

function axiosResponse(body: unknown, status: number) {
    return {
        status,
        data: body,
        statusText: status >= 200 && status < 300 ? "OK" : "Error",
        headers: {},
        config: {},
    };
}

function axiosError(body: unknown, status: number) {
    return {
        isAxiosError: true,
        response: axiosResponse(body, status),
    };
}

function renderRegisterPage(onNavigate?: (path: "/register" | "/login") => void) {
    const queryClient = new QueryClient({
        defaultOptions: {
            mutations: { retry: false },
            queries: { retry: false },
        },
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <RegisterPage onNavigate={onNavigate} />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe("RegisterPage", () => {
    beforeEach(() => {
        postMock.mockReset();
    });

    it("hiển thị validation client cho các trường bắt buộc", async () => {
        const user = userEvent.setup();
        renderRegisterPage();

        await user.click(screen.getByRole("button", { name: /đăng ký thành viên ngay/i }));

        expect(screen.getByText("Họ và tên là bắt buộc")).toBeInTheDocument();
        expect(screen.getByText("Email là bắt buộc")).toBeInTheDocument();
        expect(screen.getByText("Mật khẩu không đáp ứng yêu cầu bảo mật")).toBeInTheDocument();
        expect(screen.getByText("Mật khẩu xác nhận không khớp")).toBeInTheDocument();
        expect(postMock).not.toHaveBeenCalled();
    });

    it("giữ dữ liệu form khi backend trả ACC-001", async () => {
        postMock.mockRejectedValue(axiosError({
            success: false,
            errorCode: "ACC-001",
            message: "Email này đã được sử dụng bởi một tài khoản khác trong hệ thống.",
            details: {
                field: "email",
                rejectedValue: "used@smartgym.com",
            },
        }, 409));

        const user = userEvent.setup();
        renderRegisterPage();

        await user.type(screen.getByLabelText(/họ và tên/i), "Nguyễn Văn A");
        await user.type(screen.getByLabelText(/địa chỉ email/i), "used@smartgym.com");
        await user.type(screen.getByLabelText(/^mật khẩu/i), "Password123");
        await user.type(screen.getByPlaceholderText("Nhập lại mật khẩu của bạn"), "Password123");
        await user.click(screen.getByRole("button", { name: /đăng ký thành viên ngay/i }));

        await screen.findByText("ACC-001");

        expect(screen.getByLabelText(/họ và tên/i)).toHaveValue("Nguyễn Văn A");
        expect(screen.getByLabelText(/địa chỉ email/i)).toHaveValue("used@smartgym.com");
    });

    it("hiển thị thành công và chuyển sang login sau khi đăng ký hợp lệ", async () => {
        const successResponse = axiosResponse({
            success: true,
            message: "Đăng ký tài khoản thành công",
            data: {
                id: 101,
                fullName: "Nguyễn Văn A",
                email: "member@smartgym.com",
                role: "ROLE_MEMBER",
                accountStatus: "ACTIVE",
                createdAt: "2026-07-29T08:00:00Z",
            },
        }, 201);
        let resolveRegister!: (response: ReturnType<typeof axiosResponse>) => void;

        postMock.mockImplementation(() => new Promise((resolve) => {
            resolveRegister = resolve;
        }) as never);

        const navigate = vi.fn();
        const user = userEvent.setup();
        renderRegisterPage(navigate);

        await user.type(screen.getByLabelText(/họ và tên/i), "  Nguyễn Văn A  ");
        await user.type(screen.getByLabelText(/địa chỉ email/i), "  Member@SmartGym.Com  ");
        await user.type(screen.getByLabelText(/^mật khẩu/i), "Password123");
        await user.type(screen.getByPlaceholderText("Nhập lại mật khẩu của bạn"), "Password123");
        await user.click(screen.getByRole("button", { name: /đăng ký thành viên ngay/i }));

        expect(await screen.findByRole("button", { name: /đang đăng ký/i })).toBeDisabled();

        resolveRegister(successResponse);

        await screen.findByText("Đăng Ký Thành Công");

        expect(postMock).toHaveBeenCalledWith("/auth/register", {
            fullName: "Nguyễn Văn A",
            email: "member@smartgym.com",
            password: "Password123",
            confirmPassword: "Password123",
        });

        await waitFor(() => expect(navigate).toHaveBeenCalledWith("/login"), { timeout: 2600 });
    });

    it("giữ dữ liệu form và báo lỗi khi không kết nối được backend", async () => {
        postMock.mockRejectedValue(new TypeError("Failed to connect"));

        const user = userEvent.setup();
        renderRegisterPage();

        await user.type(screen.getByLabelText(/họ và tên/i), "Nguyễn Văn A");
        await user.type(screen.getByLabelText(/địa chỉ email/i), "member@smartgym.com");
        await user.type(screen.getByLabelText(/^mật khẩu/i), "Password123");
        await user.type(screen.getByPlaceholderText("Nhập lại mật khẩu của bạn"), "Password123");
        await user.click(screen.getByRole("button", { name: /đăng ký thành viên ngay/i }));

        await screen.findByText("NETWORK-001");
        expect(screen.getByLabelText(/địa chỉ email/i)).toHaveValue("member@smartgym.com");
        expect(screen.getByRole("button", { name: /đăng ký thành viên ngay/i })).toBeEnabled();
    });

    it("hiển thị ACC-002 tại confirm password theo details của backend", async () => {
        postMock.mockRejectedValue(axiosError({
            success: false,
            errorCode: "ACC-002",
            message: "Mật khẩu không đáp ứng yêu cầu bảo mật.",
            details: {
                field: "confirmPassword",
                constraint: "Xác nhận mật khẩu phải khớp chính xác với mật khẩu.",
            },
        }, 400));

        const user = userEvent.setup();
        renderRegisterPage();

        await user.type(screen.getByLabelText(/họ và tên/i), "Nguyễn Văn A");
        await user.type(screen.getByLabelText(/địa chỉ email/i), "member@smartgym.com");
        await user.type(screen.getByLabelText(/^mật khẩu/i), "Password123");
        await user.type(screen.getByPlaceholderText("Nhập lại mật khẩu của bạn"), "Password123");
        await user.click(screen.getByRole("button", { name: /đăng ký thành viên ngay/i }));

        expect(await screen.findByText("Xác nhận mật khẩu phải khớp chính xác với mật khẩu.")).toBeInTheDocument();
        expect(screen.getByText("ACC-002")).toBeInTheDocument();
    });

    it("từ chối success response thiếu dữ liệu bắt buộc", async () => {
        postMock.mockResolvedValue(axiosResponse({
            success: true,
            message: "Đăng ký tài khoản thành công",
            data: {
                id: 101,
                email: "member@smartgym.com",
            },
        }, 201) as never);

        const navigate = vi.fn();
        const user = userEvent.setup();
        renderRegisterPage(navigate);

        await user.type(screen.getByLabelText(/họ và tên/i), "Nguyễn Văn A");
        await user.type(screen.getByLabelText(/địa chỉ email/i), "member@smartgym.com");
        await user.type(screen.getByLabelText(/^mật khẩu/i), "Password123");
        await user.type(screen.getByPlaceholderText("Nhập lại mật khẩu của bạn"), "Password123");
        await user.click(screen.getByRole("button", { name: /đăng ký thành viên ngay/i }));

        expect(await screen.findByText("SYS-001")).toBeInTheDocument();
        expect(navigate).not.toHaveBeenCalled();
    });
});
