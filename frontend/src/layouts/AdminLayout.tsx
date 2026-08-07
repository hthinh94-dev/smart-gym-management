import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/useAuth";

export function AdminLayout() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    function handleLogout() {
        logout();
        navigate("/login", { replace: true });
    }

    return (
        <div className="admin-shell">
            <a className="skip-link" href="#main-content">Chuyển đến nội dung chính</a>
            <aside className="admin-sidebar">
                <div className="admin-brand">
                    <div className="brand-badge admin-brand-badge" aria-label="Smart Gym logo">
                        <div className="brand-mark-bar" aria-hidden="true"><span></span></div>
                        <strong>SMART GYM</strong>
                        <small>FITNESS SYSTEM</small>
                    </div>
                    <div>
                        <strong>SMART GYM</strong>
                        <span>Quản trị hệ thống</span>
                    </div>
                </div>

                <nav className="admin-nav" aria-label="Điều hướng quản trị">
                    <span className="admin-nav-label">Vận hành</span>
                    <NavLink to="/admin/users" className={({ isActive }) => isActive ? "active" : ""}>
                        Quản lý tài khoản
                    </NavLink>
                    <NavLink to="/admin/packages" className={({ isActive }) => isActive ? "active" : ""}>
                        Quản lý gói tập
                    </NavLink>
                </nav>

                <div className="admin-sidebar-footer">
                    <span>Đang đăng nhập</span>
                    <strong>{user?.fullName}</strong>
                    <small>{user?.email}</small>
                    <button type="button" onClick={handleLogout}>Đăng xuất</button>
                </div>
            </aside>

            <div className="admin-workspace">
                <header className="admin-topbar">
                    <p>Hệ thống quản lý phòng gym</p>
                    <span>Quản trị viên</span>
                </header>
                <Outlet />
            </div>
        </div>
    );
}
