import { useQuery } from "@tanstack/react-query";
import { Navigate, Link } from "react-router-dom";
import { getMembershipPackages } from "../features/membership/api/membershipPackageApi";
import { PackagePreview } from "../features/membership/components/PackagePreview";
import { useAuth } from "../features/auth/hooks/useAuth";
import { getAuthenticatedHomePath } from "../features/auth/utils/authNavigation";
import { AuthRouteLoading } from "../app/routes/ProtectedRoute";

export function LandingPage() {
    const { user, isRestoringSession } = useAuth();
    const isMember = user?.role === "ROLE_MEMBER";
    const canRenderLanding = !user || isMember;
    const packages = useQuery({
        queryKey: ["public-packages"],
        queryFn: getMembershipPackages,
        enabled: !isRestoringSession && canRenderLanding,
    });
    if (isRestoringSession) return <AuthRouteLoading />;
    if (user && !isMember) return <Navigate to={getAuthenticatedHomePath(user.role)} replace />;
    const packageDestination = isMember ? "/member/subscription" : "/register";
    return <div className="landing-shell"><header className="landing-header"><Link className="landing-brand" to="/" aria-label="Trang chủ Smart Gym"><span>SMART</span> GYM</Link><nav aria-label="Điều hướng trang chủ"><a href="#packages">Gói tập</a><a href="#benefits">Lợi ích</a></nav><div>{isMember ? <Link className="landing-login" to="/member/subscription">Gói tập của tôi</Link> : <><Link className="landing-login" to="/login">Đăng nhập</Link><Link className="landing-register" to="/register">Đăng ký ngay</Link></>}</div></header>
        <main><section className="landing-hero"><img src="/images/smart-gym-hero.jpg" alt="Không gian tập luyện hiện đại tại Smart Gym" /><div className="landing-hero-overlay" /><div className="landing-hero-content"><p>Smart training, measurable progress</p><h1>SMART GYM</h1><h2>Move with a plan<br />Train with purpose</h2><p className="landing-hero-copy">Hệ thống phòng gym thông minh kết nối gói tập, hồ sơ thể trạng và tiến trình luyện tập của bạn</p><div className="landing-hero-actions"><Link to={packageDestination}>{isMember ? "Chọn gói tập" : "Bắt đầu đăng ký"}</Link><Link to={isMember ? "/member" : "/login"}>{isMember ? "Khu vực hội viên" : "Đăng nhập hội viên"}</Link></div></div><div className="landing-hero-stats"><span><strong>24/7</strong> Theo dõi tiến trình</span><span><strong>1</strong> Hồ sơ kết nối</span><span><strong>100%</strong> Lịch sử cá nhân</span></div></section>
        <section className="landing-packages" id="packages"><header><div><p>Danh mục gói tập</p><h2>Chọn thời hạn phù hợp</h2></div><p>Các gói đang hoạt động được tải trực tiếp từ Smart Gym và không hiển thị gói đã ngừng bán</p></header>{packages.isLoading && <div className="landing-package-loading" role="status">Đang tải các gói tập đang mở</div>}{packages.isError && <div className="landing-package-error" role="alert"><span>Danh mục gói tập hiện chưa khả dụng</span><button type="button" onClick={() => void packages.refetch()}>Thử lại</button></div>}{packages.data?.length === 0 && <div className="landing-package-empty"><h3>Chưa có gói tập đang mở</h3><p>Vui lòng quay lại sau hoặc liên hệ quầy Smart Gym</p></div>}{packages.data && packages.data.length > 0 && <div className="landing-package-grid">{packages.data.slice(0, 4).map((item) => <PackagePreview item={item} destination={packageDestination} key={item.id} />)}</div>}</section>
        <section className="landing-benefits" id="benefits"><div><p>Được xây dựng theo thói quen của bạn</p><h2>Chủ động hơn sau mỗi buổi tập</h2></div><div className="landing-benefit-list"><article><strong>01</strong><h3>Hồ sơ cá nhân</h3><p>Thông tin thể trạng và mục tiêu được kết nối với các bước tiếp theo</p></article><article><strong>02</strong><h3>Ghi nhận tiến trình</h3><p>Dữ liệu hằng ngày tạo thành lịch sử rõ ràng để bạn theo dõi</p></article><article><strong>03</strong><h3>Vận hành tin cậy</h3><p>Trạng thái tài khoản và gói tập luôn được hiển thị minh bạch</p></article></div></section>
        <section className="landing-final-cta"><h2>Sẵn sàng cho buổi tập tiếp theo</h2><div><Link to={packageDestination}>{isMember ? "Chọn gói tập" : "Tạo tài khoản"}</Link><Link to={isMember ? "/member" : "/login"}>{isMember ? "Khu vực hội viên" : "Đăng nhập"}</Link></div></section></main><footer className="landing-footer"><strong>SMART GYM</strong><span>Hệ thống quản lý phòng gym thông minh</span><span>2026</span></footer></div>;
}
