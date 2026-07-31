import type { RoleName } from "../types/auth.types";

export function getAuthenticatedHomePath(role: RoleName) {
    return role === "ROLE_ADMIN" ? "/admin/users" : "/member";
}
