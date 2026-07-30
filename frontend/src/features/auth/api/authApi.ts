import axios from "axios";
import { httpClient } from "../../../lib/httpClient";
import { normalizeEmail } from "../schemas/registerSchema";
import type {
    ApiErrorResponse,
    ApiResponse,
    ApiSuccessResponse,
    AuthErrorCode,
    LoginFormValues,
    LoginResponse,
    RegisterFormValues,
    RegisterResponse,
} from "../types/auth.types";

const AUTH_ERROR_CODES = new Set<AuthErrorCode>([
    "ACC-001",
    "ACC-002",
    "ACC-004",
    "ACC-005",
    "ACC-006",
    "ACC-007",
    "VAL-001",
    "SYS-001",
    "NETWORK-001",
]);

function isRecord(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === "object";
}

function hasValidErrorDetails(value: Record<string, unknown>): boolean {
    return value.details == null || isRecord(value.details);
}

function isRegisterResponseData(value: unknown): value is RegisterResponse {
    if (!isRecord(value)) {
        return false;
    }

    return typeof value.id === "number"
        && typeof value.fullName === "string"
        && typeof value.email === "string"
        && value.role === "ROLE_MEMBER"
        && value.accountStatus === "ACTIVE"
        && typeof value.createdAt === "string";
}

function isRoleName(value: unknown): value is LoginResponse["user"]["role"] {
    return value === "ROLE_MEMBER" || value === "ROLE_ADMIN" || value === "ROLE_PT";
}

function isAccountStatus(value: unknown): value is RegisterResponse["accountStatus"] {
    return value === "ACTIVE" || value === "LOCKED" || value === "DISABLED";
}

function isLoginUser(value: unknown): value is LoginResponse["user"] {
    return isRecord(value)
        && typeof value.id === "number"
        && typeof value.fullName === "string"
        && typeof value.email === "string"
        && isRoleName(value.role);
}

function isLoginResponseData(value: unknown): value is LoginResponse {
    return isRecord(value)
        && typeof value.accessToken === "string"
        && value.accessToken.length > 0
        && value.tokenType === "Bearer"
        && typeof value.expiresIn === "number"
        && Number.isFinite(value.expiresIn)
        && value.expiresIn > 0
        && isLoginUser(value.user);
}

function isCurrentUserResponseData(value: unknown): value is RegisterResponse {
    return isRecord(value)
        && typeof value.id === "number"
        && typeof value.fullName === "string"
        && typeof value.email === "string"
        && isRoleName(value.role)
        && isAccountStatus(value.accountStatus)
        && typeof value.createdAt === "string";
}

function isRegisterApiResponse(value: unknown): value is ApiResponse<RegisterResponse> {
    if (!isRecord(value) || typeof value.success !== "boolean" || typeof value.message !== "string") {
        return false;
    }

    if (value.success) {
        return isRegisterResponseData(value.data);
    }

    return typeof value.errorCode === "string"
        && AUTH_ERROR_CODES.has(value.errorCode as AuthErrorCode)
        && hasValidErrorDetails(value);
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
    return isRecord(value)
        && value.success === false
        && typeof value.errorCode === "string"
        && AUTH_ERROR_CODES.has(value.errorCode as AuthErrorCode)
        && typeof value.message === "string"
        && hasValidErrorDetails(value);
}

function isLoginApiResponse(value: unknown): value is ApiResponse<LoginResponse> {
    return isRecord(value)
        && typeof value.message === "string"
        && ((value.success === true && isLoginResponseData(value.data)) || isApiErrorResponse(value));
}

function isCurrentUserApiResponse(value: unknown): value is ApiResponse<RegisterResponse> {
    return isRecord(value)
        && typeof value.message === "string"
        && ((value.success === true && isCurrentUserResponseData(value.data)) || isApiErrorResponse(value));
}

export class AuthApiError extends Error {
    errorCode: AuthErrorCode;
    details: Record<string, unknown>;

    constructor(error: ApiErrorResponse) {
        super(error.message);
        this.name = "AuthApiError";
        this.errorCode = error.errorCode;
        this.details = error.details ?? {};
    }
}

function throwAuthError(errorCode: AuthErrorCode, message: string, details: Record<string, unknown> = {}): never {
    throw new AuthApiError({
        success: false,
        errorCode,
        message,
        details,
    });
}

async function executeAuthRequest<T>(
    request: () => Promise<{ data: unknown; status: number }>,
    isExpectedResponse: (value: unknown) => value is ApiResponse<T>,
    actionLabel: string,
): Promise<ApiSuccessResponse<T>> {
    let rawPayload: unknown;
    let status: number;

    try {
        const response = await request();
        rawPayload = response.data;
        status = response.status;
    } catch (error) {
        if (!axios.isAxiosError(error) || !error.response) {
            throwAuthError("NETWORK-001", "Không thể kết nối đến hệ thống. Vui lòng thử lại.");
        }

        rawPayload = error.response.data;
        status = error.response.status;
    }

    if (!isExpectedResponse(rawPayload)) {
        throwAuthError("SYS-001", "Hệ thống trả về phản hồi không đúng contract. Vui lòng thử lại.");
    }

    if (status < 200 || status >= 300 || !rawPayload.success) {
        if (rawPayload.success) {
            throwAuthError("SYS-001", `${actionLabel} không thành công (HTTP ${status}).`);
        }

        throw new AuthApiError(rawPayload);
    }

    return rawPayload;
}

export async function registerMember(values: RegisterFormValues): Promise<ApiSuccessResponse<RegisterResponse>> {
    let rawPayload: unknown;
    let status: number;

    try {
        const response = await httpClient.post("/auth/register", {
            fullName: values.fullName.trim(),
            email: normalizeEmail(values.email),
            password: values.password,
            confirmPassword: values.confirmPassword,
        });
        rawPayload = response.data;
        status = response.status;
    } catch (error) {
        if (!axios.isAxiosError(error) || !error.response) {
            throwAuthError("NETWORK-001", "Không thể kết nối đến hệ thống. Vui lòng thử lại.");
        }

        rawPayload = error.response.data;
        status = error.response.status;
    }

    if (!isRegisterApiResponse(rawPayload)) {
        throwAuthError("SYS-001", "Hệ thống trả về phản hồi không đúng contract. Vui lòng thử lại.");
    }

    const payload = rawPayload;

    if (status < 200 || status >= 300 || !payload.success) {
        if (payload.success) {
            throwAuthError("SYS-001", `Đăng ký không thành công (HTTP ${status}).`);
        }

        throw new AuthApiError(payload);
    }

    return payload;
}

export async function login(values: LoginFormValues): Promise<ApiSuccessResponse<LoginResponse>> {
    return executeAuthRequest(
        () => httpClient.post("/auth/login", {
            email: normalizeEmail(values.email),
            password: values.password,
        }),
        isLoginApiResponse,
        "Đăng nhập",
    );
}

export async function getCurrentUser(): Promise<ApiSuccessResponse<RegisterResponse>> {
    return executeAuthRequest(
        () => httpClient.get("/users/me"),
        isCurrentUserApiResponse,
        "Lấy thông tin người dùng",
    );
}
