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

const LOGIN_MOCK_DELAY_MS = 700;
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

function isRegisterApiResponse(value: unknown): value is ApiResponse<RegisterResponse> {
    if (!isRecord(value) || typeof value.success !== "boolean" || typeof value.message !== "string") {
        return false;
    }

    if (value.success) {
        return isRegisterResponseData(value.data);
    }

    return typeof value.errorCode === "string"
        && AUTH_ERROR_CODES.has(value.errorCode as AuthErrorCode)
        && isRecord(value.details);
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

function delay(ms = LOGIN_MOCK_DELAY_MS) {
    return new Promise((resolve) => {
        window.setTimeout(resolve, ms);
    });
}

function throwAuthError(errorCode: AuthErrorCode, message: string, details: Record<string, unknown> = {}): never {
    throw new AuthApiError({
        success: false,
        errorCode,
        message,
        details,
    });
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
    await delay();

    if (normalizeEmail(values.email) === "locked@smartgym.com") {
        throwAuthError("ACC-004", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.", {
            accountStatus: "LOCKED",
        });
    }

    if (!values.email.trim() || !values.password) {
        throwAuthError("ACC-007", "Tên đăng nhập hoặc mật khẩu không chính xác.");
    }

    return {
        success: true,
        message: "Đăng nhập thành công",
        data: {
            accessToken: "mock-access-token",
            tokenType: "Bearer",
            expiresIn: 86400,
            user: {
                id: 101,
                fullName: "Nguyễn Văn A",
                email: normalizeEmail(values.email),
                role: "ROLE_MEMBER",
            },
        },
    };
}
