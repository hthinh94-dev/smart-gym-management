import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../lib/httpClient";
import type { AdminUser } from "../types/adminUser.types";
import { AdminUsersPage } from "./AdminUsersPage";

const activeMember: AdminUser = {
    id: 101,
    fullName: "Nguyễn Văn An",
    email: "an@gmail.com",
    role: "ROLE_MEMBER",
    accountStatus: "ACTIVE",
    createdAt: "2026-07-15T08:00:00Z",
    hasActiveSubscription: true,
};

const lockedMember: AdminUser = {
    id: 102,
    fullName: "Trần Thị Bình",
    email: "binh@gmail.com",
    role: "ROLE_MEMBER",
    accountStatus: "LOCKED",
    createdAt: "2026-07-10T09:00:00Z",
    hasActiveSubscription: false,
};

const adminUser: AdminUser = {
    id: 1,
    fullName: "Quản trị hệ thống",
    email: "admin@smartgym.com",
    role: "ROLE_ADMIN",
    accountStatus: "ACTIVE",
    createdAt: "2026-07-01T08:00:00Z",
    hasActiveSubscription: false,
};

function usersResponse(content: AdminUser[]) {
    return {
        success: true as const,
        message: "Lấy danh sách người dùng thành công",
        data: {
            content,
            totalElements: content.length,
            totalPages: content.length ? 1 : 0,
            currentPage: 0,
            pageSize: 10,
        },
    };
}

function renderAdminUsersPage() {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    render(
        <QueryClientProvider client={queryClient}>
            <AdminUsersPage />
        </QueryClientProvider>,
    );
}

afterEach(() => {
    vi.restoreAllMocks();
});

describe("AdminUsersPage", () => {
    it("hiển thị danh sách cho Admin và không cho thao tác trên Admin row", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue({ data: usersResponse([activeMember, lockedMember, adminUser]), status: 200 });
        renderAdminUsersPage();

        expect(await screen.findByText("Nguyễn Văn An")).toBeInTheDocument();
        expect(screen.getByText("Trần Thị Bình")).toBeInTheDocument();
        const adminRow = screen.getByRole("row", { name: /Quản trị hệ thống/ });
        expect(within(adminRow).getByText("Không áp dụng")).toBeInTheDocument();
        expect(within(adminRow).queryByRole("button")).not.toBeInTheDocument();
    });

    it("gửi đúng search, role và status trong query", async () => {
        const getMock = vi.spyOn(httpClient, "get").mockResolvedValue({ data: usersResponse([activeMember]), status: 200 });
        const user = userEvent.setup();
        renderAdminUsersPage();
        await screen.findByText("Nguyễn Văn An");

        await user.selectOptions(screen.getByLabelText("Vai trò"), "ROLE_MEMBER");
        await user.selectOptions(screen.getByLabelText("Trạng thái"), "ACTIVE");
        await user.type(screen.getByLabelText("Tìm theo tên hoặc email"), "  nguyen  ");

        await waitFor(() => expect(getMock).toHaveBeenCalledWith("/admin/users", {
            params: {
                page: 0,
                size: 10,
                role: "ROLE_MEMBER",
                status: "ACTIVE",
                search: "nguyen",
            },
        }));
    });

    it("hiển thị loading rồi empty state", async () => {
        let resolveRequest!: (value: { data: ReturnType<typeof usersResponse>; status: number }) => void;
        vi.spyOn(httpClient, "get").mockImplementation(() => new Promise((resolve) => {
            resolveRequest = resolve;
        }));
        renderAdminUsersPage();

        expect(screen.getByRole("table").parentElement).toHaveAttribute("aria-busy", "true");
        resolveRequest({ data: usersResponse([]), status: 200 });

        expect(await screen.findByText("Không tìm thấy tài khoản")).toBeInTheDocument();
    });

    it("hiển thị API error code và cho phép thử lại", async () => {
        const getMock = vi.spyOn(httpClient, "get")
            .mockRejectedValueOnce({
                isAxiosError: true,
                response: {
                    status: 403,
                    data: { success: false, errorCode: "AUTH-002", message: "Không có quyền quản trị.", details: {} },
                },
            })
            .mockResolvedValue({ data: usersResponse([activeMember]), status: 200 });
        const user = userEvent.setup();
        renderAdminUsersPage();

        expect(await screen.findByText(/AUTH-002/)).toBeInTheDocument();
        await user.click(screen.getByRole("button", { name: "Thử lại" }));
        expect(await screen.findByText("Nguyễn Văn An")).toBeInTheDocument();
        expect(getMock).toHaveBeenCalledTimes(2);
    });

    it("khóa thành công và cập nhật đúng row", async () => {
        const lockedVersion = { ...activeMember, accountStatus: "LOCKED" as const };
        vi.spyOn(httpClient, "get")
            .mockResolvedValueOnce({ data: usersResponse([activeMember]), status: 200 })
            .mockResolvedValue({ data: usersResponse([lockedVersion]), status: 200 });
        const patchMock = vi.spyOn(httpClient, "patch").mockResolvedValue({
            status: 200,
            data: {
                success: true,
                message: "Tài khoản đã được khóa thành công.",
                data: {
                    userId: 101,
                    fullName: "Nguyễn Văn An",
                    accountStatus: "LOCKED",
                    lockedBy: "admin@smartgym.com",
                    lockedAt: "2026-07-30T08:00:00Z",
                    reason: "Vi phạm nội quy phòng tập nhiều lần.",
                    subscriptionStatus: "ACTIVE (không thay đổi)",
                },
            },
        });
        const user = userEvent.setup();
        renderAdminUsersPage();
        await screen.findByText("Nguyễn Văn An");

        await user.click(screen.getByRole("button", { name: "Khóa tài khoản Nguyễn Văn An" }));
        await user.type(screen.getByLabelText(/lý do khóa/i), "Vi phạm nội quy phòng tập nhiều lần.");
        await user.click(screen.getByRole("button", { name: "Khóa tài khoản" }));

        expect(await screen.findByText("Tài khoản đã được khóa thành công.")).toBeInTheDocument();
        expect(patchMock).toHaveBeenCalledWith("/admin/users/101/lock", {
            reason: "Vi phạm nội quy phòng tập nhiều lần.",
        });
        const row = screen.getByRole("row", { name: /Nguyễn Văn An/ });
        expect(within(row).getByText("LOCKED")).toBeInTheDocument();
    });

    it("mở khóa thành công và cập nhật đúng row", async () => {
        const activeVersion = { ...lockedMember, accountStatus: "ACTIVE" as const };
        vi.spyOn(httpClient, "get")
            .mockResolvedValueOnce({ data: usersResponse([lockedMember]), status: 200 })
            .mockResolvedValue({ data: usersResponse([activeVersion]), status: 200 });
        const patchMock = vi.spyOn(httpClient, "patch").mockResolvedValue({
            status: 200,
            data: {
                success: true,
                message: "Tài khoản đã được mở khóa thành công.",
                data: {
                    userId: 102,
                    fullName: "Trần Thị Bình",
                    accountStatus: "ACTIVE",
                    unlockedBy: "admin@smartgym.com",
                    unlockedAt: "2026-07-30T09:00:00Z",
                },
            },
        });
        const user = userEvent.setup();
        renderAdminUsersPage();
        await screen.findByText("Trần Thị Bình");

        await user.click(screen.getByRole("button", { name: "Mở khóa tài khoản Trần Thị Bình" }));
        await user.click(screen.getByRole("button", { name: "Mở khóa tài khoản" }));

        expect(await screen.findByText("Tài khoản đã được mở khóa thành công.")).toBeInTheDocument();
        expect(patchMock).toHaveBeenCalledWith("/admin/users/102/unlock");
        const row = screen.getByRole("row", { name: /Trần Thị Bình/ });
        expect(within(row).getByText("ACTIVE")).toBeInTheDocument();
    });
});
