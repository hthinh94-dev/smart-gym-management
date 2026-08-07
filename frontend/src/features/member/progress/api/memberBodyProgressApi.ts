import axios from "axios";
import { httpClient } from "../../../../lib/httpClient";
import type { BodyProgressApiErrorResponse, BodyProgressErrorCode, BodyProgressSuccess, BodyProgressUpsertRequest, MemberBodyProgress } from "../types/memberBodyProgress.types";

const ERROR_CODES = new Set<BodyProgressErrorCode>(["VAL-001", "ACC-004", "ACC-005", "ACC-006", "NETWORK-001", "SYS-001"]);
function record(value: unknown): value is Record<string, unknown> { return value !== null && typeof value === "object"; }
function isProgress(value: unknown): value is MemberBodyProgress { return record(value) && typeof value.id === "number" && typeof value.memberId === "number" && typeof value.recordDate === "string" && typeof value.weightKg === "number" && (value.muscleMassKg == null || typeof value.muscleMassKg === "number") && (value.fatMassKg == null || typeof value.fatMassKg === "number") && typeof value.createdAt === "string" && typeof value.updatedAt === "string"; }
function isError(value: unknown): value is BodyProgressApiErrorResponse { return record(value) && value.success === false && typeof value.errorCode === "string" && ERROR_CODES.has(value.errorCode as BodyProgressErrorCode) && typeof value.message === "string"; }
function isSuccess<T>(value: unknown, dataCheck: (data: unknown) => data is T): value is BodyProgressSuccess<T> { return record(value) && value.success === true && typeof value.message === "string" && dataCheck(value.data); }
function throwError(code: BodyProgressErrorCode, message: string): never { throw new BodyProgressApiError({ success: false, errorCode: code, message, details: {} }); }

export class BodyProgressApiError extends Error {
    errorCode: BodyProgressErrorCode;
    details: Record<string, unknown>;
    constructor(response: BodyProgressApiErrorResponse) { super(response.message); this.name = "BodyProgressApiError"; this.errorCode = response.errorCode; this.details = response.details ?? {}; }
}

async function request<T>(call: () => Promise<{ status: number; data: unknown }>, check: (data: unknown) => data is T, fallback: string): Promise<T> {
    let response: { status: number; data: unknown };
    try { response = await call(); } catch (error) {
        if (!axios.isAxiosError(error) || !error.response) throwError("NETWORK-001", "Không thể kết nối đến hệ thống.");
        response = { status: error.response.status, data: error.response.data };
    }
    if (isError(response.data)) throw new BodyProgressApiError(response.data);
    if (response.status < 200 || response.status >= 300 || !isSuccess(response.data, check)) throwError("SYS-001", fallback);
    return response.data.data;
}

export function getMemberBodyProgress() { return request(() => httpClient.get("/member/body-progress"), (data): data is MemberBodyProgress[] => Array.isArray(data) && data.every(isProgress), "Phản hồi lịch sử cân nặng không đúng contract."); }
export function upsertMemberBodyProgress(payload: BodyProgressUpsertRequest) { return request(() => httpClient.post("/member/body-progress", payload), isProgress, "Phản hồi cập nhật cân nặng không đúng contract."); }
