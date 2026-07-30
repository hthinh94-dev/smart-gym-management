import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/hooks/useAuth";

export function MemberHomePage() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    function handleLogout() {
        logout();
        navigate("/login", { replace: true });
    }

    return (
        <main className="member-shell">
            <header className="member-topbar">
                <strong>SMART GYM</strong>
                <button type="button" onClick={handleLogout}>Đăng xuất</button>
            </header>
            <section className="member-home" aria-labelledby="memberHomeTitle">
                <p className="page-eyebrow">Khu vực hội viên</p>
                <h1 id="memberHomeTitle">Xin chào, {user?.fullName}</h1>
                <p>Phiên đăng nhập của bạn đã được xác thực. Các chức năng hội viên sẽ được hoàn thiện ở milestone tiếp theo.</p>
                <dl className="member-account-summary">
                    <div><dt>Email</dt><dd>{user?.email}</dd></div>
                    <div><dt>Vai trò</dt><dd>Hội viên</dd></div>
                    <div><dt>Trạng thái</dt><dd>{user?.accountStatus}</dd></div>
                </dl>
            </section>
        </main>
    );
}
