import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../features/auth/hooks/useAuth";
import type { RoleName } from "../../features/auth/types/auth.types";

type RoleRouteProps = {
    allowedRoles: RoleName[];
    fallbackPath?: string;
};

export function RoleRoute({ allowedRoles, fallbackPath = "/member" }: RoleRouteProps) {
    const { user } = useAuth();

    if (!user || !allowedRoles.includes(user.role)) {
        return <Navigate to={fallbackPath} replace />;
    }

    return <Outlet />;
}
