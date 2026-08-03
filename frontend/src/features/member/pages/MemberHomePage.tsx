import { useAuth } from "../../auth/hooks/useAuth";
import type { AuthUser } from "../../auth/types/auth.types";

function roleLabel(role: AuthUser["role"] | undefined) {
    return role === "ROLE_PT" ? "Huấn luyện viên" : "Hội viên";
}

function accountStatusLabel(status: AuthUser["accountStatus"] | undefined) {
    if (status === "LOCKED") return "Đã khóa";
    if (status === "DISABLED") return "Đã vô hiệu hóa";
    return "Đang hoạt động";
}

export function MemberHomePage() {
    const { user } = useAuth();
    const currentRoleLabel = roleLabel(user?.role);

    return (
        <main className="member-page member-home" id="main-content">
            <header className="member-home-heading">
                <div>
                    <p className="page-eyebrow">{currentRoleLabel}</p>
                    <h1>Xin chào, {user?.fullName}</h1>
                    <p>Tài khoản của bạn đã được xác thực và sẵn sàng sử dụng trên Smart Gym.</p>
                </div>
                <span className={`member-status status-${user?.accountStatus.toLowerCase() ?? "active"}`}>
                    {accountStatusLabel(user?.accountStatus)}
                </span>
            </header>

            <section className="member-account-section" aria-labelledby="accountOverviewTitle">
                <div className="member-section-heading">
                    <p>Thông tin phiên</p>
                    <h2 id="accountOverviewTitle">Tổng quan tài khoản</h2>
                </div>
                <dl className="member-account-summary">
                    <div><dt>Họ và tên</dt><dd>{user?.fullName}</dd></div>
                    <div><dt>Email</dt><dd>{user?.email}</dd></div>
                    <div><dt>Vai trò</dt><dd>{currentRoleLabel}</dd></div>
                    <div><dt>Trạng thái</dt><dd>{accountStatusLabel(user?.accountStatus)}</dd></div>
                </dl>
            </section>
        </main>
    );
}
