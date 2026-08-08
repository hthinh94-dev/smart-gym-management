import axios from "axios";
import { httpClient } from "../../../../lib/httpClient";
import type {
    CurrentMemberSubscription,
    PendingMemberSubscription,
    SubscriptionErrorCode,
    SubscriptionErrorResponse,
    SubscriptionSuccess,
} from "../types/memberSubscription.types";

const ERROR_CODES = new Set<SubscriptionErrorCode>([
    "SUB-002", "SUB-003", "SUB-004", "SUB-005", "SUB-006",
    "VAL-001", "ACC-004", "ACC-005", "ACC-006", "AUTH-002",
    "NETWORK-001", "SYS-001",
]);

function isRecord(value: unknown): value is Record<string, unknown> {
    return value !== null && typeof value === "object";
}

function isPendingSubscription(value: unknown): value is PendingMemberSubscription {
    return isRecord(value)
        && Number.isInteger(value.subscriptionId)
        && Number.isInteger(value.memberId)
        && Number.isInteger(value.packageId)
        && typeof value.packageName === "string"
        && typeof value.price === "number"
        && value.status === "PENDING"
        && typeof value.requestedAt === "string";
}

function isCurrentSubscription(value: unknown): value is CurrentMemberSubscription {
    return isRecord(value)
        && Number.isInteger(value.subscriptionId)
        && Number.isInteger(value.memberId)
        && Number.isInteger(value.packageId)
        && typeof value.packageName === "string"
        && value.status === "ACTIVE"
        && typeof value.startDate === "string"
        && typeof value.endDate === "string"
        && Number.isInteger(value.daysRemaining)
        && typeof value.approvedAt === "string";
}

function isError(value: unknown): value is SubscriptionErrorResponse {
    return isRecord(value)
        && value.success === false
        && typeof value.errorCode === "string"
        && ERROR_CODES.has(value.errorCode as SubscriptionErrorCode)
        && typeof value.message === "string";
}

function isSuccess<T>(value: unknown, check: (data: unknown) => data is T): value is SubscriptionSuccess<T> {
    return isRecord(value)
        && value.success === true
        && typeof value.message === "string"
        && check(value.data);
}

export class MemberSubscriptionApiError extends Error {
    errorCode: SubscriptionErrorCode;
    details: Record<string, unknown>;

    constructor(response: SubscriptionErrorResponse) {
        super(response.message);
        this.name = "MemberSubscriptionApiError";
        this.errorCode = response.errorCode;
        this.details = response.details ?? {};
    }
}

function throwContractError(message: string): never {
    throw new MemberSubscriptionApiError({
        success: false,
        errorCode: "SYS-001",
        message,
    });
}

async function request<T>(
    call: () => Promise<{ status: number; data: unknown }>,
    check: (data: unknown) => data is T,
    fallback: string,
) {
    let response: { status: number; data: unknown };
    try {
        response = await call();
    } catch (error) {
        if (axios.isAxiosError(error) && error.response) {
            response = { status: error.response.status, data: error.response.data };
        } else {
            throw new MemberSubscriptionApiError({
                success: false,
                errorCode: "NETWORK-001",
                message: "Không thể kết nối đến hệ thống.",
            });
        }
    }

    if (isError(response.data)) throw new MemberSubscriptionApiError(response.data);
    if (response.status < 200 || response.status >= 300 || !isSuccess(response.data, check)) {
        throwContractError(fallback);
    }
    return response.data.data;
}

export function createMemberSubscription(packageId: number) {
    return request(
        () => httpClient.post("/member/subscriptions", { packageId }),
        isPendingSubscription,
        "Phản hồi đăng ký gói tập không đúng contract.",
    );
}

export function getCurrentMemberSubscription() {
    return request(
        () => httpClient.get("/member/subscriptions/current"),
        isCurrentSubscription,
        "Phản hồi gói tập hiện hành không đúng contract.",
    );
}

export { isCurrentSubscription, isPendingSubscription, isError as isSubscriptionError };
