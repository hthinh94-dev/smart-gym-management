import { Navigate, Route, Routes } from "react-router-dom";
import { AdminUsersPage } from "../features/admin/pages/AdminUsersPage";
import { LoginPage } from "../features/auth/pages/LoginPage";
import { RegisterPage } from "../features/auth/pages/RegisterPage";
import { useAuth } from "../features/auth/hooks/useAuth";
import { MemberHomePage } from "../features/member/pages/MemberHomePage";
import { AdminLayout } from "../layouts/AdminLayout";
import { AuthRouteLoading, ProtectedRoute } from "./routes/ProtectedRoute";
import { PublicOnlyRoute } from "./routes/PublicOnlyRoute";
import { RoleRoute } from "./routes/RoleRoute";
import { getAuthenticatedHomePath } from "../features/auth/utils/authNavigation";

function RootRedirect() {
    const { user, isRestoringSession } = useAuth();
    if (isRestoringSession) {
        return <AuthRouteLoading />;
    }
    if (user) {
        return <Navigate to={getAuthenticatedHomePath(user.role)} replace />;
    }
    return <Navigate to="/login" replace />;
}

export function AppRouter() {
    return (
        <Routes>
            <Route element={<PublicOnlyRoute />}>
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/login" element={<LoginPage />} />
            </Route>
            <Route element={<ProtectedRoute />}>
                <Route element={<RoleRoute allowedRoles={["ROLE_MEMBER", "ROLE_PT"]} />}>
                    <Route path="/member" element={<MemberHomePage />} />
                </Route>
                <Route element={<RoleRoute allowedRoles={["ROLE_ADMIN"]} />}>
                    <Route path="/admin" element={<AdminLayout />}>
                        <Route index element={<Navigate to="users" replace />} />
                        <Route path="users" element={<AdminUsersPage />} />
                    </Route>
                </Route>
            </Route>
            <Route path="/" element={<RootRedirect />} />
            <Route path="*" element={<RootRedirect />} />
        </Routes>
    );
}
