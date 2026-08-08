import type { MembershipPackage } from "../../types/membershipPackage.types";

const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });

export function PackageSelectList({ packages, selectedPackageId, onSelect, onSubmit, isSubmitting, error }: {
    packages: MembershipPackage[];
    selectedPackageId?: number;
    onSelect: (packageId: number) => void;
    onSubmit: () => void;
    isSubmitting: boolean;
    error?: string;
}) {
    return <section className="subscription-picker" aria-labelledby="packageSelectTitle">
        <div className="member-section-heading"><p>Đăng ký gói mới</p><h2 id="packageSelectTitle">Chọn gói tập phù hợp</h2><span>Chỉ các gói đang ACTIVE mới được hiển thị</span></div>
        {error && <div className="subscription-api-error" role="alert">{error}</div>}
        <div className="subscription-package-list">
            {packages.map((item) => <label className={`subscription-package-option ${selectedPackageId === item.id ? "selected" : ""}`} key={item.id}>
                <input type="radio" name="membership-package" value={item.id} checked={selectedPackageId === item.id} onChange={() => onSelect(item.id)} disabled={isSubmitting} />
                <span className="subscription-package-copy"><strong>{item.name}</strong><span>{item.description}</span></span>
                <span className="subscription-package-meta"><strong>{money.format(item.price)}</strong><span>{item.durationDays} ngày</span></span>
            </label>)}
        </div>
        <button className="primary-action subscription-submit" type="button" onClick={onSubmit} disabled={isSubmitting || selectedPackageId === undefined}>
            {isSubmitting ? "Đang gửi yêu cầu" : "Đăng ký gói tập"}
        </button>
    </section>;
}
