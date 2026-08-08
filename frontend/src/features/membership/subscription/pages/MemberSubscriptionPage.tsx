import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { getMembershipPackages } from "../../api/membershipPackageApi";
import { MemberSubscriptionApiError, createMemberSubscription, getCurrentMemberSubscription } from "../api/memberSubscriptionApi";
import { PackageSelectList } from "../components/PackageSelectList";
import { SubscriptionStatus } from "../components/SubscriptionStatus";
import type { PendingMemberSubscription } from "../types/memberSubscription.types";

const CURRENT_SUBSCRIPTION_KEY = ["member-current-subscription"] as const;
const PENDING_SUBSCRIPTION_KEY = ["member-pending-subscription"] as const;

function errorMessage(error: unknown) {
    if (!(error instanceof MemberSubscriptionApiError)) return "Không thể thực hiện thao tác lúc này.";
    const messages: Record<string, string> = {
        "SUB-002": "Không tìm thấy gói tập. Vui lòng chọn lại.",
        "SUB-003": "Gói tập này đã ngừng kinh doanh. Vui lòng chọn gói khác.",
        "SUB-004": "Bạn đang có một gói ACTIVE. Không thể đăng ký gói mới.",
        "SUB-005": "Bạn chưa có gói ACTIVE.",
        "SUB-006": "Bạn đã có một yêu cầu đăng ký đang chờ xử lý.",
        "ACC-004": "Tài khoản đang bị khóa.",
        "ACC-005": "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
        "ACC-006": "Tài khoản đã bị vô hiệu hóa.",
        "NETWORK-001": "Không thể kết nối đến hệ thống. Vui lòng thử lại.",
    };
    return messages[error.errorCode] ?? error.message;
}

function isNoCurrentSubscription(error: unknown) {
    return error instanceof MemberSubscriptionApiError && error.errorCode === "SUB-005";
}

export function MemberSubscriptionPage() {
    const client = useQueryClient();
    const [selectedPackageId, setSelectedPackageId] = useState<number>();
    const [pendingDurationDays, setPendingDurationDays] = useState<number>();
    const [submitError, setSubmitError] = useState<string>();
    const [hasPendingConflict, setHasPendingConflict] = useState(false);
    const currentQuery = useQuery({ queryKey: CURRENT_SUBSCRIPTION_KEY, queryFn: getCurrentMemberSubscription, retry: false });
    const packageQuery = useQuery({ queryKey: ["member-active-packages"], queryFn: getMembershipPackages, enabled: currentQuery.isError && isNoCurrentSubscription(currentQuery.error), retry: false });
    const pending = client.getQueryData<PendingMemberSubscription>(PENDING_SUBSCRIPTION_KEY);
    const createMutation = useMutation({
        mutationFn: (packageId: number) => createMemberSubscription(packageId),
        onSuccess: (value) => {
            client.setQueryData(PENDING_SUBSCRIPTION_KEY, value);
            setPendingDurationDays(packageQuery.data?.find((item) => item.id === value.packageId)?.durationDays);
            setSubmitError(undefined);
            setHasPendingConflict(false);
        },
        onError: (error: unknown) => {
            setSubmitError(errorMessage(error));
            setHasPendingConflict(error instanceof MemberSubscriptionApiError && error.errorCode === "SUB-006");
            if (error instanceof MemberSubscriptionApiError && error.errorCode === "SUB-004") void currentQuery.refetch();
        },
    });

    const activeSubscription = currentQuery.data;
    const pendingSubscription = pending ?? (hasPendingConflict ? undefined : undefined);
    const canChoosePackage = currentQuery.isError && isNoCurrentSubscription(currentQuery.error) && !pendingSubscription && !hasPendingConflict;

    function handleSubmit() {
        if (selectedPackageId === undefined || createMutation.isPending) return;
        setSubmitError(undefined);
        createMutation.mutate(selectedPackageId);
    }

    return <main className="member-page subscription-page" id="main-content">
        <header className="member-page-heading">
            <div><p className="page-eyebrow">Gói tập</p><h1>Gói tập của tôi</h1><p>Chọn gói tập active và gửi yêu cầu đăng ký đến quản trị viên</p></div>
        </header>
        {submitError && !canChoosePackage && <div className="subscription-api-error subscription-page-error" role="alert">{submitError}</div>}

        {currentQuery.isLoading && <div className="subscription-loading" role="status">Đang tải trạng thái gói tập</div>}
        {currentQuery.isError && !isNoCurrentSubscription(currentQuery.error) && <div className="subscription-api-error subscription-page-error" role="alert"><span>{errorMessage(currentQuery.error)}</span><button type="button" onClick={() => void currentQuery.refetch()}>Thử lại</button></div>}
        {!currentQuery.isLoading && activeSubscription && <SubscriptionStatus current={activeSubscription} />}
        {!currentQuery.isLoading && pendingSubscription && <SubscriptionStatus pending={pendingSubscription} pendingDurationDays={pendingDurationDays} />}
        {!currentQuery.isLoading && hasPendingConflict && <section className="subscription-status subscription-status-pending" aria-labelledby="pendingConflictTitle"><div className="subscription-status-heading"><div><p className="page-eyebrow">Yêu cầu đăng ký</p><h2 id="pendingConflictTitle">Yêu cầu đang chờ duyệt</h2></div><span className="subscription-badge pending">PENDING</span></div><p className="subscription-status-note">Bạn đã có một yêu cầu đăng ký gói mới đang chờ xử lý, không thể tạo yêu cầu thứ hai</p></section>}
        {canChoosePackage && packageQuery.isLoading && <div className="subscription-loading" role="status">Đang tải danh sách gói tập</div>}
        {canChoosePackage && packageQuery.isError && <div className="subscription-api-error subscription-page-error" role="alert"><span>Không thể tải danh sách gói tập active</span><button type="button" onClick={() => void packageQuery.refetch()}>Thử lại</button></div>}
        {canChoosePackage && packageQuery.data?.length === 0 && <section className="subscription-empty"><h2>Chưa có gói tập active</h2><p>Hiện chưa có gói nào đang mở đăng ký, vui lòng quay lại sau</p></section>}
        {canChoosePackage && packageQuery.data && packageQuery.data.length > 0 && <PackageSelectList packages={packageQuery.data} selectedPackageId={selectedPackageId} onSelect={setSelectedPackageId} onSubmit={handleSubmit} isSubmitting={createMutation.isPending} error={submitError} />}
        {canChoosePackage && createMutation.isError && !submitError && <div className="subscription-api-error" role="alert">{errorMessage(createMutation.error)}</div>}
    </main>;
}

export { errorMessage };
