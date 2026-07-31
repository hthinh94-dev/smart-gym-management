import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../features/auth/hooks/useAuth";
import type { RoleName } from "../../features/auth/types/auth.types";
import { getAuthenticatedHomePath } from "../../features/auth/utils/authNavigation";

type RoleRouteProps = {
    allowedRoles: RoleName[];
    fallbackPath?: string;
};

export function RoleRoute({ allowedRoles, fallbackPath }: RoleRouteProps) {
    const { user } = useAuth();

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (!allowedRoles.includes(user.role)) {
        return <Navigate to={fallbackPath ?? getAuthenticatedHomePath(user.role)} replace />;
    }

    return <Outlet />;
}
