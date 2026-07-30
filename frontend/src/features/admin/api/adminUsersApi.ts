import axios from "axios";
import { httpClient } from "../../../lib/httpClient";
import type {
    AdminApiErrorResponse,
    AdminApiSuccessResponse,
    AdminUser,
    AdminUserFilters,
    AdminUsersPageData,
    LockUserResponse,
    UnlockUserResponse,
} from "../types/adminUser.types";

function isRecord(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === "object";
}

function isRole(value: unknown): value is AdminUser["role"] {
    return value === "ROLE_MEMBER" || value === "ROLE_ADMIN" || value === "ROLE_PT";
}

function isAccountStatus(value: unknown): value is AdminUser["accountStatus"] {
    return value === "ACTIVE" || value === "LOCKED" || value === "DISABLED";
}

function isAdminUser(value: unknown): value is AdminUser {
    return isRecord(value)
        && typeof value.id === "number"
        && typeof value.fullName === "string"
        && typeof value.email === "string"
        && isRole(value.role)
        && isAccountStatus(value.accountStatus)
        && typeof value.createdAt === "string"
        && typeof value.hasActiveSubscription === "boolean";
}

function isAdminUsersPageData(value: unknown): value is AdminUsersPageData {
    return isRecord(value)
        && Array.isArray(value.content)
        && value.content.every(isAdminUser)
        && typeof value.totalElements === "number"
        && Number.isInteger(value.totalElements)
        && value.totalElements >= 0
        && typeof value.totalPages === "number"
        && Number.isInteger(value.totalPages)
        && value.totalPages >= 0
        && typeof value.currentPage === "number"
        && Number.isInteger(value.currentPage)
        && value.currentPage >= 0
        && typeof value.pageSize === "number"
        && Number.isInteger(value.pageSize)
        && value.pageSize > 0;
}

function isLockUserResponse(value: unknown): value is LockUserResponse {
    return isRecord(value)
        && typeof value.userId === "number"
        && typeof value.fullName === "string"
        && value.accountStatus === "LOCKED"
        && typeof value.lockedBy === "string"
        && typeof value.lockedAt === "string"
        && typeof value.reason === "string"
        && typeof value.subscriptionStatus === "string";
}

function isUnlockUserResponse(value: unknown): value is UnlockUserResponse {
    return isRecord(value)
        && typeof value.userId === "number"
        && typeof value.fullName === "string"
        && value.accountStatus === "ACTIVE"
        && typeof value.unlockedBy === "string"
        && typeof value.unlockedAt === "string";
}

function isApiError(value: unknown): value is AdminApiErrorResponse {
    return isRecord(value)
        && value.success === false
        && typeof value.errorCode === "string"
        && value.errorCode.length > 0
        && typeof value.message === "string"
        && (value.details == null || isRecord(value.details));
}

function isApiSuccess<T>(value: unknown, isData: (data: unknown) => data is T): value is AdminApiSuccessResponse<T> {
    return isRecord(value)
        && value.success === true
        && typeof value.message === "string"
        && isData(value.data);
}

export class AdminUsersApiError extends Error {
    errorCode: string;
    details: Record<string, unknown>;

    constructor(error: AdminApiErrorResponse) {
        super(error.message);
        this.name = "AdminUsersApiError";
        this.errorCode = error.errorCode;
        this.details = error.details ?? {};
    }
}

function throwClientError(errorCode: string, message: string): never {
    throw new AdminUsersApiError({
        success: false,
        errorCode,
        message,
        details: {},
    });
}

async function executeAdminRequest<T>(
    request: () => Promise<{ data: unknown; status: number }>,
    isData: (data: unknown) => data is T,
): Promise<AdminApiSuccessResponse<T>> {
    let payload: unknown;
    let status: number;

    try {
        const response = await request();
        payload = response.data;
        status = response.status;
    } catch (error) {
        if (!axios.isAxiosError(error) || !error.response) {
            throwClientError("NETWORK-001", "Không thể kết nối đến hệ thống. Vui lòng thử lại.");
        }
        payload = error.response.data;
        status = error.response.status;
    }

    if (isApiError(payload)) {
        throw new AdminUsersApiError(payload);
    }

    if (status < 200 || status >= 300 || !isApiSuccess(payload, isData)) {
        throwClientError("SYS-001", "Hệ thống trả về phản hồi không đúng contract. Vui lòng thử lại.");
    }

    return payload;
}

export function getAdminUsers(filters: AdminUserFilters): Promise<AdminApiSuccessResponse<AdminUsersPageData>> {
    const params: Record<string, string | number> = {
        page: filters.page,
        size: filters.size,
    };

    if (filters.role) {
        params.role = filters.role;
    }
    if (filters.status) {
        params.status = filters.status;
    }
    if (filters.search.trim()) {
        params.search = filters.search.trim();
    }

    return executeAdminRequest(
        () => httpClient.get("/admin/users", { params }),
        isAdminUsersPageData,
    );
}

export function lockUser(id: number, reason: string): Promise<AdminApiSuccessResponse<LockUserResponse>> {
    return executeAdminRequest(
        () => httpClient.patch(`/admin/users/${id}/lock`, { reason: reason.trim() }),
        isLockUserResponse,
    );
}

export function unlockUser(id: number): Promise<AdminApiSuccessResponse<UnlockUserResponse>> {
    return executeAdminRequest(
        () => httpClient.patch(`/admin/users/${id}/unlock`),
        isUnlockUserResponse,
    );
}
