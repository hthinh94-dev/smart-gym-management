import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../features/auth/hooks/useAuth";
import { getAuthenticatedHomePath } from "../../features/auth/utils/authNavigation";
import { AuthRouteLoading } from "./ProtectedRoute";

export function PublicOnlyRoute() {
    const { user, isRestoringSession } = useAuth();

    if (isRestoringSession) {
        return <AuthRouteLoading />;
    }

    if (user) {
        return <Navigate to={getAuthenticatedHomePath(user.role)} replace />;
    }

    return <Outlet />;
}
