import { useNavigate } from "react-router-dom";
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
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    function handleLogout() {
        logout();
        navigate("/login", { replace: true });
    }

    const currentRoleLabel = roleLabel(user?.role);

    return (
        <div className="member-shell">
            <a className="skip-link" href="#main-content">Chuyển đến nội dung chính</a>
            <header className="member-topbar">
                <div className="member-brand" aria-label="Smart Gym">
                    <div className="brand-badge member-brand-badge" aria-hidden="true">
                        <div className="brand-mark-bar"><span></span></div>
                        <strong>SMART GYM</strong>
                        <small>FITNESS SYSTEM</small>
                    </div>
                    <div>
                        <strong>SMART GYM</strong>
                        <span>Hệ thống quản lý phòng gym</span>
                    </div>
                </div>

                <nav className="member-nav" aria-label="Điều hướng khu vực người dùng">
                    <span aria-current="page">Tổng quan</span>
                </nav>

                <div className="member-session">
                    <div>
                        <strong>{user?.fullName}</strong>
                        <span>{currentRoleLabel} · {user?.email}</span>
                    </div>
                    <button type="button" onClick={handleLogout}>Đăng xuất</button>
                </div>
            </header>

            <main className="member-home" id="main-content">
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
        </div>
    );
}
