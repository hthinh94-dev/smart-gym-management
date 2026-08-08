import type { CurrentMemberSubscription, PendingMemberSubscription } from "../types/memberSubscription.types";

const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });

function formatDate(value: string) {
    return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium" }).format(new Date(`${value}${value.length === 10 ? "T00:00:00" : ""}`));
}

export function SubscriptionStatus({ current, pending, pendingDurationDays }: {
    current?: CurrentMemberSubscription;
    pending?: PendingMemberSubscription;
    pendingDurationDays?: number;
}) {
    if (current) {
        return <section className="subscription-status subscription-status-active" aria-labelledby="currentSubscriptionTitle">
            <div className="subscription-status-heading">
                <div><p className="page-eyebrow">Gói tập hiện hành</p><h2 id="currentSubscriptionTitle">Bạn đang có gói ACTIVE</h2></div>
                <span className="subscription-badge active">ACTIVE</span>
            </div>
            <dl className="subscription-details">
                <div><dt>Gói tập</dt><dd>{current.packageName}</dd></div>
                <div><dt>Ngày bắt đầu</dt><dd>{formatDate(current.startDate)}</dd></div>
                <div><dt>Ngày kết thúc</dt><dd>{formatDate(current.endDate)}</dd></div>
                <div><dt>Còn lại</dt><dd>{current.daysRemaining} ngày</dd></div>
            </dl>
            <p className="subscription-status-note">Bạn không thể tạo yêu cầu đăng ký mới khi gói hiện hành còn hiệu lực</p>
        </section>;
    }

    if (!pending) return null;
    return <section className="subscription-status subscription-status-pending" aria-labelledby="pendingSubscriptionTitle">
        <div className="subscription-status-heading">
            <div><p className="page-eyebrow">Yêu cầu đăng ký</p><h2 id="pendingSubscriptionTitle">Yêu cầu đang chờ duyệt</h2></div>
            <span className="subscription-badge pending">PENDING</span>
        </div>
        <dl className="subscription-details">
            <div><dt>Gói tập</dt><dd>{pending.packageName}</dd></div>
            <div><dt>Giá tại thời điểm đăng ký</dt><dd>{money.format(pending.price)}</dd></div>
            {pendingDurationDays && <div><dt>Thời hạn snapshot</dt><dd>{pendingDurationDays} ngày</dd></div>}
            <div><dt>Thời điểm gửi</dt><dd>{formatDate(pending.requestedAt)}</dd></div>
        </dl>
        <p className="subscription-status-note">Yêu cầu đã được ghi nhận, vui lòng chờ quản trị viên phê duyệt</p>
    </section>;
}
