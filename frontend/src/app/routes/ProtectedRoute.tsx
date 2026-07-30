import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../features/auth/hooks/useAuth";

export function AuthRouteLoading() {
    return (
        <main className="route-loading" role="status" aria-live="polite">
            <div className="route-loading-bar" aria-hidden="true"></div>
            <p>Đang khôi phục phiên đăng nhập...</p>
        </main>
    );
}

export function ProtectedRoute() {
    const location = useLocation();
    const { isAuthenticated, isRestoringSession } = useAuth();

    if (isRestoringSession) {
        return <AuthRouteLoading />;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace state={{ from: location.pathname }} />;
    }

    return <Outlet />;
}
