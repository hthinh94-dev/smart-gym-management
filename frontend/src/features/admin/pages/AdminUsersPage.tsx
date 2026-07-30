import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useMemo, useState } from "react";
import { AdminUsersApiError, getAdminUsers, lockUser, unlockUser } from "../api/adminUsersApi";
import { LockUserDialog } from "../components/LockUserDialog";
import { UnlockUserDialog } from "../components/UnlockUserDialog";
import { UserFilters } from "../components/UserFilters";
import { UserTable } from "../components/UserTable";
import type { AdminUser, AdminUserFilters, AdminUsersPageData } from "../types/adminUser.types";

const DEFAULT_FILTERS: AdminUserFilters = {
    page: 0,
    size: 10,
    role: "",
    status: "",
    search: "",
};

export function adminUsersQueryKey(filters: AdminUserFilters) {
    return ["admin-users", filters.page, filters.size, filters.role || "ALL", filters.status || "ALL", filters.search] as const;
}

function apiErrorMessage(error: unknown) {
    if (error instanceof AdminUsersApiError) {
        return `${error.errorCode} · ${error.message}`;
    }
    return "SYS-001 · Không thể xử lý yêu cầu. Vui lòng thử lại.";
}

export function AdminUsersPage() {
    const queryClient = useQueryClient();
    const [filters, setFilters] = useState(DEFAULT_FILTERS);
    const [lockTarget, setLockTarget] = useState<AdminUser | null>(null);
    const [unlockTarget, setUnlockTarget] = useState<AdminUser | null>(null);
    const [notification, setNotification] = useState("");
    const queryKey = useMemo(() => adminUsersQueryKey(filters), [filters]);

    const usersQuery = useQuery({
        queryKey,
        queryFn: () => getAdminUsers(filters),
        placeholderData: keepPreviousData,
    });

    function updateCurrentRow(userId: number, accountStatus: AdminUser["accountStatus"]) {
        queryClient.setQueryData<{ success: true; message: string; data: AdminUsersPageData }>(queryKey, (current) => {
            if (!current) return current;
            return {
                ...current,
                data: {
                    ...current.data,
                    content: current.data.content.map((user) => user.id === userId ? { ...user, accountStatus } : user),
                },
            };
        });
    }

    const lockMutation = useMutation({
        mutationFn: ({ id, reason }: { id: number; reason: string }) => lockUser(id, reason),
        onSuccess: (response) => {
            updateCurrentRow(response.data.userId, response.data.accountStatus);
            setNotification(response.message);
            setLockTarget(null);
            void queryClient.invalidateQueries({ queryKey: ["admin-users"] });
        },
    });

    const unlockMutation = useMutation({
        mutationFn: (id: number) => unlockUser(id),
        onSuccess: (response) => {
            updateCurrentRow(response.data.userId, response.data.accountStatus);
            setNotification(response.message);
            setUnlockTarget(null);
            void queryClient.invalidateQueries({ queryKey: ["admin-users"] });
        },
    });

    const updateFilters = useCallback((change: Partial<AdminUserFilters>) => {
        setNotification("");
        setFilters((current) => ({ ...current, ...change, page: 0 }));
    }, []);

    const pageData = usersQuery.data?.data;
    const pendingUserId = lockMutation.isPending
        ? lockMutation.variables?.id
        : unlockMutation.isPending
            ? unlockMutation.variables
            : undefined;

    return (
        <main className="admin-page" id="main-content">
            <header className="admin-page-heading">
                <div>
                    <p className="page-eyebrow">Vận hành tài khoản</p>
                    <h1>Quản lý tài khoản</h1>
                    <p>Tìm kiếm, lọc và xử lý trạng thái truy cập của người dùng.</p>
                </div>
                <div className="result-count" aria-live="polite">
                    <strong>{pageData?.totalElements ?? 0}</strong>
                    <span>Tài khoản</span>
                </div>
            </header>

            {notification && (
                <div className="admin-notification admin-notification-success" role="status">
                    <p>{notification}</p>
                    <button type="button" aria-label="Đóng thông báo" onClick={() => setNotification("")}>×</button>
                </div>
            )}

            {usersQuery.isError && (
                <div className="admin-notification admin-notification-error" role="alert">
                    <div>
                        <strong>Không tải được danh sách tài khoản</strong>
                        <p>{apiErrorMessage(usersQuery.error)}</p>
                    </div>
                    <button type="button" onClick={() => void usersQuery.refetch()}>Thử lại</button>
                </div>
            )}

            <UserFilters
                search={filters.search}
                role={filters.role}
                status={filters.status}
                disabled={usersQuery.isLoading}
                onSearchChange={(search) => updateFilters({ search })}
                onRoleChange={(role) => updateFilters({ role })}
                onStatusChange={(status) => updateFilters({ status })}
            />

            <UserTable
                users={pageData?.content ?? []}
                isLoading={usersQuery.isLoading}
                isRefreshing={usersQuery.isFetching}
                pendingUserId={pendingUserId}
                onLock={(user) => {
                    lockMutation.reset();
                    setLockTarget(user);
                }}
                onUnlock={(user) => {
                    unlockMutation.reset();
                    setUnlockTarget(user);
                }}
            />

            <footer className="admin-pagination" aria-label="Phân trang tài khoản">
                <p>
                    Trang <strong>{(pageData?.currentPage ?? filters.page) + 1}</strong> / <strong>{Math.max(pageData?.totalPages ?? 1, 1)}</strong>
                </p>
                <div>
                    <button
                        type="button"
                        disabled={filters.page === 0 || usersQuery.isFetching}
                        onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}
                    >
                        Trước
                    </button>
                    <button
                        type="button"
                        disabled={!pageData || filters.page + 1 >= pageData.totalPages || usersQuery.isFetching}
                        onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}
                    >
                        Sau
                    </button>
                </div>
            </footer>

            <LockUserDialog
                user={lockTarget}
                isPending={lockMutation.isPending}
                apiError={lockMutation.isError ? apiErrorMessage(lockMutation.error) : undefined}
                onClose={() => {
                    if (!lockMutation.isPending) setLockTarget(null);
                }}
                onConfirm={(reason) => lockTarget && lockMutation.mutate({ id: lockTarget.id, reason })}
            />
            <UnlockUserDialog
                user={unlockTarget}
                isPending={unlockMutation.isPending}
                apiError={unlockMutation.isError ? apiErrorMessage(unlockMutation.error) : undefined}
                onClose={() => {
                    if (!unlockMutation.isPending) setUnlockTarget(null);
                }}
                onConfirm={() => unlockTarget && unlockMutation.mutate(unlockTarget.id)}
            />
        </main>
    );
}
