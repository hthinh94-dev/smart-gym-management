import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/useAuth";
import type { RoleName } from "../features/auth/types/auth.types";

function roleLabel(role: RoleName | undefined) {
    if (role === "ROLE_PT") return "Huấn luyện viên";
    if (role === "ROLE_ADMIN") return "Quản trị viên";
    return "Hội viên";
}

export function MemberLayout() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    function handleLogout() {
        logout();
        navigate("/login", { replace: true });
    }

    return (
        <div className="member-shell">
            <a className="skip-link" href="#main-content">Chuyển đến nội dung chính</a>
            <header className="member-topbar">
                <div className="member-brand" aria-label="Smart Gym">
                    <div className="brand-badge member-brand-badge" aria-hidden="true">
                        <div className="brand-mark-bar"><span /></div>
                        <strong>SMART GYM</strong>
                        <small>FITNESS SYSTEM</small>
                    </div>
                    <div>
                        <strong>SMART GYM</strong>
                        <span>Khu vực hội viên</span>
                    </div>
                </div>

                <nav className="member-nav" aria-label="Điều hướng khu vực hội viên">
                    <NavLink end to="/member" className={({ isActive }) => isActive ? "active" : ""}>
                        Tổng quan
                    </NavLink>
                    {user?.role === "ROLE_MEMBER" && (
                        <NavLink to="/member/profile" className={({ isActive }) => isActive ? "active" : ""}>
                            Hồ sơ
                        </NavLink>
                    )}
                    {user?.role === "ROLE_MEMBER" && (
                        <NavLink to="/member/progress" className={({ isActive }) => isActive ? "active" : ""}>
                            Tiến độ
                        </NavLink>
                    )}
                </nav>

                <div className="member-session">
                    <div>
                        <strong>{user?.fullName}</strong>
                        <span>{roleLabel(user?.role)} · {user?.email}</span>
                    </div>
                    <button type="button" onClick={handleLogout}>Đăng xuất</button>
                </div>
            </header>

            <Outlet />
        </div>
    );
}
