export type AccountStatus = "ACTIVE" | "LOCKED" | "DISABLED";

export type RoleName = "ROLE_MEMBER" | "ROLE_ADMIN" | "ROLE_PT";

export type AuthErrorCode =
    | "ACC-001"
    | "ACC-002"
    | "ACC-004"
    | "ACC-005"
    | "ACC-006"
    | "ACC-007"
    | "VAL-001"
    | "SYS-001"
    | "NETWORK-001";

export type RegisterFormValues = {
    fullName: string;
    email: string;
    password: string;
    confirmPassword: string;
};

export type LoginFormValues = {
    email: string;
    password: string;
};

export type AuthUser = {
    id: number;
    fullName: string;
    email: string;
    role: RoleName;
    accountStatus: AccountStatus;
    createdAt: string;
};

export type RegisterResponse = AuthUser;

export type LoginResponse = {
    accessToken: string;
    tokenType: "Bearer";
    expiresIn: number;
    user: Omit<AuthUser, "accountStatus" | "createdAt">;
};

export type ApiSuccessResponse<T> = {
    success: true;
    message: string;
    data: T;
};

export type ApiErrorResponse = {
    success: false;
    errorCode: AuthErrorCode;
    message: string;
    details: Record<string, unknown>;
};

export type ApiResponse<T> = ApiSuccessResponse<T> | ApiErrorResponse;
