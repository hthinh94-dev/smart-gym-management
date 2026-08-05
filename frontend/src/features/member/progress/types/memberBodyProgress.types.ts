export type MemberBodyProgress = {
    id: number;
    memberId: number;
    recordDate: string;
    weightKg: number;
    createdAt: string;
    updatedAt: string;
};

export type BodyProgressUpsertRequest = { recordDate: string; weightKg: number };
export type BodyProgressErrorCode = "VAL-001" | "ACC-004" | "ACC-005" | "ACC-006" | "NETWORK-001" | "SYS-001";
export type BodyProgressSuccess<T> = { success: true; message: string; data: T };
export type BodyProgressApiErrorResponse = { success: false; errorCode: BodyProgressErrorCode; message: string; details?: Record<string, unknown> };
