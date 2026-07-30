import type { AccountStatus, RoleName } from "../../auth/types/auth.types";

export type AdminUser = {
    id: number;
    fullName: string;
    email: string;
    role: RoleName;
    accountStatus: AccountStatus;
    createdAt: string;
    hasActiveSubscription: boolean;
};

export type AdminUserFilters = {
    page: number;
    size: number;
    role: RoleName | "";
    status: AccountStatus | "";
    search: string;
};

export type AdminUsersPageData = {
    content: AdminUser[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
};

export type LockUserResponse = {
    userId: number;
    fullName: string;
    accountStatus: "LOCKED";
    lockedBy: string;
    lockedAt: string;
    reason: string;
    subscriptionStatus: string;
};

export type UnlockUserResponse = {
    userId: number;
    fullName: string;
    accountStatus: "ACTIVE";
    unlockedBy: string;
    unlockedAt: string;
};

export type AdminApiSuccessResponse<T> = {
    success: true;
    message: string;
    data: T;
};

export type AdminApiErrorResponse = {
    success: false;
    errorCode: string;
    message: string;
    details: Record<string, unknown>;
};
