import { useQuery } from "@tanstack/react-query";
import { Navigate, Link } from "react-router-dom";
import { getMembershipPackages } from "../features/membership/api/membershipPackageApi";
import { PackagePreview } from "../features/membership/components/PackagePreview";
import { useAuth } from "../features/auth/hooks/useAuth";
import { getAuthenticatedHomePath } from "../features/auth/utils/authNavigation";
import { AuthRouteLoading } from "../app/routes/ProtectedRoute";

export function LandingPage() {
    const { user, isRestoringSession } = useAuth();
    const packages = useQuery({
        queryKey: ["public-packages"],
        queryFn: getMembershipPackages,
        enabled: !isRestoringSession && !user,
    });
    if (isRestoringSession) return <AuthRouteLoading />;
    if (user) return <Navigate to={getAuthenticatedHomePath(user.role)} replace />;
    return <div className="landing-shell"><header className="landing-header"><Link className="landing-brand" to="/" aria-label="Smart Gym home"><span>SMART</span> GYM</Link><nav aria-label="Public navigation"><a href="#packages">Packages</a><a href="#benefits">Benefits</a></nav><div><Link className="landing-login" to="/login">Login</Link><Link className="landing-register" to="/register">Join now</Link></div></header>
        <main><section className="landing-hero"><img src="/images/smart-gym-hero.jpg" alt="Athlete training with strength equipment in a modern gym" /><div className="landing-hero-overlay" /><div className="landing-hero-content"><p>Smart training. Measurable progress.</p><h1>SMART GYM</h1><h2>Move with a plan.<br />Train with purpose.</h2><p className="landing-hero-copy">A connected gym system for membership, body metrics, training plans and everyday progress.</p><div className="landing-hero-actions"><Link to="/register">Start membership</Link><Link to="/login">Member login</Link></div></div><div className="landing-hero-stats"><span><strong>24/7</strong> progress access</span><span><strong>1</strong> connected profile</span><span><strong>100%</strong> personal history</span></div></section>
        <section className="landing-packages" id="packages"><header><div><p>Membership catalog</p><h2>Choose your training window</h2></div><p>Active packages are loaded directly from Smart Gym. No hidden inactive offers.</p></header>{packages.isLoading && <div className="landing-package-loading" role="status">Loading active packages...</div>}{packages.isError && <div className="landing-package-error" role="alert"><span>Package catalog is temporarily unavailable.</span><button type="button" onClick={() => void packages.refetch()}>Retry</button></div>}{packages.data?.length === 0 && <div className="landing-package-empty"><h3>No active packages</h3><p>Please check again later or contact Smart Gym at the front desk.</p></div>}{packages.data && packages.data.length > 0 && <div className="landing-package-grid">{packages.data.slice(0, 4).map((item) => <PackagePreview item={item} key={item.id} />)}</div>}</section>
        <section className="landing-benefits" id="benefits"><div><p>Built around your routine</p><h2>Less guessing between sessions.</h2></div><div className="landing-benefit-list"><article><strong>01</strong><h3>Profile driven</h3><p>Your physical profile and goals stay connected to every next step.</p></article><article><strong>02</strong><h3>Progress recorded</h3><p>Daily measurements create a clear, private history you can revisit.</p></article><article><strong>03</strong><h3>Operationally reliable</h3><p>Membership and account states remain visible and controlled.</p></article></div></section>
        <section className="landing-final-cta"><h2>Ready for your next session?</h2><div><Link to="/register">Create account</Link><Link to="/login">Sign in</Link></div></section></main><footer className="landing-footer"><strong>SMART GYM</strong><span>Smart Gym Management System</span><span>2026</span></footer></div>;
}
