export type SubscriptionStatus = "PENDING" | "ACTIVE" | "EXPIRED" | "CANCELLED";

export type CreateMemberSubscriptionRequest = {
    packageId: number;
};

export type PendingMemberSubscription = {
    subscriptionId: number;
    memberId: number;
    packageId: number;
    packageName: string;
    price: number;
    status: "PENDING";
    requestedAt: string;
};

export type CurrentMemberSubscription = {
    subscriptionId: number;
    memberId: number;
    packageId: number;
    packageName: string;
    status: "ACTIVE";
    startDate: string;
    endDate: string;
    daysRemaining: number;
    approvedAt: string;
};

export type SubscriptionErrorCode =
    | "SUB-002"
    | "SUB-003"
    | "SUB-004"
    | "SUB-005"
    | "SUB-006"
    | "VAL-001"
    | "ACC-004"
    | "ACC-005"
    | "ACC-006"
    | "AUTH-002"
    | "NETWORK-001"
    | "SYS-001";

export type SubscriptionSuccess<T> = {
    success: true;
    message: string;
    data: T;
};

export type SubscriptionErrorResponse = {
    success: false;
    errorCode: SubscriptionErrorCode;
    message: string;
    details?: Record<string, unknown>;
};
