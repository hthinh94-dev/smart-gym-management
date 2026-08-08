import type { AdminUser } from "../types/adminUser.types";

type UserTableProps = {
    users: AdminUser[];
    isLoading: boolean;
    isRefreshing?: boolean;
    pendingUserId?: number;
    onLock: (user: AdminUser) => void;
    onUnlock: (user: AdminUser) => void;
};

function LockIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect x="5" y="10" width="14" height="10" rx="2" />
            <path d="M8 10V7a4 4 0 0 1 8 0v3" />
        </svg>
    );
}

function UnlockIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect x="5" y="10" width="14" height="10" rx="2" />
            <path d="M8 10V7a4 4 0 0 1 7.4-2" />
        </svg>
    );
}

function formatCreatedAt(createdAt: string) {
    const date = new Date(createdAt);
    if (Number.isNaN(date.getTime())) {
        return createdAt;
    }
    return new Intl.DateTimeFormat("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
    }).format(date);
}

function roleLabel(role: AdminUser["role"]) {
    if (role === "ROLE_ADMIN") return "ADMIN";
    if (role === "ROLE_PT") return "PT";
    return "MEMBER";
}

function UserAction({ user, pendingUserId, onLock, onUnlock }: Omit<UserTableProps, "users" | "isLoading" | "isRefreshing"> & { user: AdminUser }) {
    if (user.role === "ROLE_ADMIN") {
        return <span className="action-unavailable">Không áp dụng</span>;
    }
    if (user.accountStatus === "DISABLED") {
        return <span className="action-unavailable">Vĩnh viễn</span>;
    }

    const isPending = pendingUserId === user.id;
    if (user.accountStatus === "LOCKED") {
        return (
            <button
                className="row-action row-action-unlock"
                type="button"
                aria-label={`Mở khóa tài khoản ${user.fullName}`}
                title="Mở khóa tài khoản"
                disabled={isPending}
                onClick={() => onUnlock(user)}
            >
                <UnlockIcon />
            </button>
        );
    }

    return (
        <button
            className="row-action row-action-lock"
            type="button"
            aria-label={`Khóa tài khoản ${user.fullName}`}
            title="Khóa tài khoản"
            disabled={isPending}
            onClick={() => onLock(user)}
        >
            <LockIcon />
        </button>
    );
}

export function UserTable({ users, isLoading, isRefreshing = false, pendingUserId, onLock, onUnlock }: UserTableProps) {
    return (
        <div className="admin-table-shell" aria-busy={isLoading || isRefreshing}>
            {isRefreshing && !isLoading && <div className="table-refresh-line" aria-label="Đang cập nhật danh sách"></div>}
            <table className="admin-user-table">
                <thead>
                    <tr>
                        <th scope="col">Tài khoản</th>
                        <th scope="col">Vai trò</th>
                        <th scope="col">Trạng thái</th>
                        <th scope="col">Gói tập</th>
                        <th scope="col">Ngày tạo</th>
                        <th scope="col" className="action-column">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    {isLoading && Array.from({ length: 7 }, (_, index) => (
                        <tr className="table-skeleton-row" key={index}>
                            <td><span></span><small></small></td>
                            <td><span></span></td>
                            <td><span></span></td>
                            <td><span></span></td>
                            <td><span></span></td>
                            <td><span></span></td>
                        </tr>
                    ))}

                    {!isLoading && users.length === 0 && (
                        <tr>
                            <td className="table-empty" colSpan={6}>
                                <strong>Không tìm thấy tài khoản</strong>
                                <p>Thử thay đổi từ khóa hoặc bộ lọc hiện tại</p>
                            </td>
                        </tr>
                    )}

                    {!isLoading && users.map((user) => (
                        <tr key={user.id}>
                            <td>
                                <div className="user-identity">
                                    <strong>{user.fullName}</strong>
                                    <span>#{user.id} · {user.email}</span>
                                </div>
                            </td>
                            <td><span className={`role-badge role-${user.role.toLowerCase()}`}>{roleLabel(user.role)}</span></td>
                            <td><span className={`status-badge status-${user.accountStatus.toLowerCase()}`}>{user.accountStatus}</span></td>
                            <td>
                                <span className={user.hasActiveSubscription ? "subscription-active" : "subscription-none"}>
                                    {user.hasActiveSubscription ? "Đang hoạt động" : "Chưa kích hoạt"}
                                </span>
                            </td>
                            <td className="date-cell">{formatCreatedAt(user.createdAt)}</td>
                            <td className="action-column">
                                <UserAction user={user} pendingUserId={pendingUserId} onLock={onLock} onUnlock={onUnlock} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
