import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { AuthApiError, registerMember } from "../api/authApi";
import { normalizeEmail, PASSWORD_POLICY_TEXT, registerSchema } from "../schemas/registerSchema";
import { clearAuthSession } from "../storage/authSession";
import type { AuthErrorCode, RegisterFormValues } from "../types/auth.types";

type AuthRoute = "/register" | "/login";

type RegisterPageProps = {
    onNavigate?: (path: AuthRoute) => void;
};

type MessageState = {
    type: "success" | "error";
    title: string;
    description: string;
} | null;

const initialForm: RegisterFormValues = {
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
};

const apiErrorFallback: Record<AuthErrorCode, string> = {
    "ACC-001": "Email này đã được sử dụng.",
    "ACC-002": "Mật khẩu không đáp ứng yêu cầu bảo mật.",
    "ACC-004": "Tài khoản đã bị khóa.",
    "ACC-005": "Phiên đăng nhập không hợp lệ.",
    "ACC-006": "Tài khoản đã bị vô hiệu hóa.",
    "ACC-007": "Tên đăng nhập hoặc mật khẩu không chính xác.",
    "VAL-001": "Dữ liệu đầu vào không hợp lệ.",
    "SYS-001": "Hệ thống chưa được cấu hình đầy đủ.",
    "NETWORK-001": "Không thể kết nối đến hệ thống.",
};

function isRegisterField(field: unknown): field is keyof RegisterFormValues {
    return field === "fullName" || field === "email" || field === "password" || field === "confirmPassword";
}

function extractApiFieldErrors(error: AuthApiError): Partial<Record<keyof RegisterFormValues, string>> {
    const fieldErrors: Partial<Record<keyof RegisterFormValues, string>> = {};
    const violations = error.details.violations;

    if (violations && typeof violations === "object") {
        Object.entries(violations).forEach(([field, message]) => {
            if (isRegisterField(field) && typeof message === "string") {
                fieldErrors[field] = message;
            }
        });
    }

    const field = error.details.field;
    if (isRegisterField(field) && !fieldErrors[field]) {
        const constraint = error.details.constraint;
        fieldErrors[field] = typeof constraint === "string"
            ? constraint
            : apiErrorFallback[error.errorCode] ?? error.message;
    }

    return fieldErrors;
}

function EyeIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
            <circle cx="12" cy="12" r="3" />
        </svg>
    );
}

function EyeOffIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="m2 2 20 20" />
            <path d="M6.7 6.7C3.8 8.7 2 12 2 12s3.5 7 10 7c1.8 0 3.3-.4 4.6-1" />
            <path d="M19.8 14.8C21.2 13.4 22 12 22 12s-3.5-7-10-7c-.9 0-1.7.1-2.5.3" />
            <path d="M9.9 9.9A3 3 0 0 0 14.1 14" />
        </svg>
    );
}

function CheckIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M20 6 9 17l-5-5" />
        </svg>
    );
}

function CloseIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M18 6 6 18M6 6l12 12" />
        </svg>
    );
}

function PasswordToggle({
    label,
    visible,
    onToggle,
}: {
    label: string;
    visible: boolean;
    onToggle: () => void;
}) {
    return (
        <button
            className="toggle-password"
            type="button"
            onClick={onToggle}
            aria-label={visible ? `Ẩn ${label}` : `Hiện ${label}`}
        >
            {visible ? <EyeIcon /> : <EyeOffIcon />}
        </button>
    );
}

function BrandPanel() {
    return (
        <section className="brand-panel" aria-label="Giới thiệu Smart Gym">
            <div className="brand-content">
                <div className="brand-row">
                    <div className="brand-badge" aria-label="Smart Gym logo">
                        <div className="brand-mark-bar" aria-hidden="true">
                            <span></span>
                        </div>
                        <strong>SMART GYM</strong>
                        <small>FITNESS SYSTEM</small>
                    </div>
                    <h1>SMART GYM</h1>
                </div>

                <div className="brand-copy">
                    <p className="hero-title">
                        MOVE YOUR BODY,
                        <br />
                        FEEL ALIVE - IN THE
                        <br />
                        GYM, WE THRIVE.
                    </p>
                    <p className="hero-subtitle">
                        Lịch tập và dinh dưỡng được cá nhân hóa bằng AI, giúp bạn đạt mục tiêu hiệu quả hơn.
                    </p>
                </div>

                <div className="metric-row" aria-label="Chỉ số nổi bật">
                    <div>
                        <strong>25+</strong>
                        <span>Chỉ Số Theo Dõi</span>
                    </div>
                    <div>
                        <strong>AI</strong>
                        <span>Mẹo Thông Minh</span>
                    </div>
                    <div>
                        <strong>100%</strong>
                        <span>Tự Động</span>
                    </div>
                </div>

                <div className="feature-list" aria-label="Tính năng Smart Gym">
                    <div className="feature-item">Theo Dõi Hiệu Suất Tức Thời</div>
                    <div className="feature-item">Kế Hoạch Tập Luyện Được Hỗ Trợ Bằng AI</div>
                    <div className="feature-item">Phân Tích Dữ Liệu Theo Thời Gian Thực</div>
                </div>
            </div>
        </section>
    );
}

export function RegisterPage({ onNavigate }: RegisterPageProps) {
    const routerNavigate = useNavigate();
    const {
        register,
        handleSubmit,
        setError,
        formState: { errors },
    } = useForm<RegisterFormValues>({
        resolver: zodResolver(registerSchema),
        defaultValues: initialForm,
    });
    const [visiblePassword, setVisiblePassword] = useState({
        password: false,
        confirmPassword: false,
    });
    const [message, setMessage] = useState<MessageState>(null);
    const redirectTimerRef = useRef<number | null>(null);
    const registerMutation = useMutation({ mutationFn: registerMember });
    const isLoading = registerMutation.isPending;

    useEffect(() => {
        return () => {
            if (redirectTimerRef.current) {
                window.clearTimeout(redirectTimerRef.current);
            }
        };
    }, []);

    function navigateTo(path: AuthRoute) {
        if (onNavigate) {
            onNavigate(path);
            return;
        }
        routerNavigate(path);
    }

    function applyApiError(error: unknown) {
        if (error instanceof AuthApiError) {
            const apiFieldErrors = extractApiFieldErrors(error);

            Object.entries(apiFieldErrors).forEach(([field, fieldMessage]) => {
                if (fieldMessage) {
                    setError(field as keyof RegisterFormValues, { type: "server", message: fieldMessage });
                }
            });

            setMessage({
                type: "error",
                title: error.errorCode,
                description: error.message,
            });

            return;
        }

        setMessage({
            type: "error",
            title: "NETWORK-001",
            description: apiErrorFallback["NETWORK-001"],
        });
    }

    async function submitRegistration(values: RegisterFormValues) {
        setMessage(null);

        try {
            await registerMutation.mutateAsync({
                ...values,
                fullName: values.fullName.trim(),
                email: normalizeEmail(values.email),
            });
            clearAuthSession();
            setMessage({
                type: "success",
                title: "Đăng Ký Thành Công",
                description: "Tài khoản của bạn đã được tạo. Đang chuyển hướng đến đăng nhập...",
            });
            redirectTimerRef.current = window.setTimeout(() => {
                navigateTo("/login");
            }, 1400);
        } catch (error) {
            applyApiError(error);
        }
    }

    return (
        <main className="auth-page">
            <BrandPanel />

            <section className="form-panel" aria-label="Form đăng ký thành viên">
                <div className="form-wrap">
                    {message && (
                        <div
                            className={`status-message status-message-${message.type}`}
                            role="status"
                            aria-live="polite"
                        >
                            <span className="status-icon">
                                {message.type === "success" ? <CheckIcon /> : <CloseIcon />}
                            </span>
                            <div>
                                <strong>{message.title}</strong>
                                <p>{message.description}</p>
                            </div>
                            <button
                                className="toast-close"
                                type="button"
                                onClick={() => setMessage(null)}
                                aria-label="Đóng thông báo"
                            >
                                <CloseIcon />
                            </button>
                        </div>
                    )}

                    <header className="form-header">
                        <h2>Tạo tài khoản thành viên</h2>
                        <p>Tham gia hàng nghìn vận động viên đang thay đổi fitness của họ</p>
                    </header>

                    <form className="auth-form" onSubmit={handleSubmit(submitRegistration)} noValidate>
                        <div className={`field-group ${errors.fullName ? "has-error" : ""}`}>
                            <label htmlFor="fullName">
                                Họ và tên <span>*</span>
                            </label>
                            <input
                                id="fullName"
                                name="fullName"
                                type="text"
                                placeholder="Nguyễn Văn A"
                                autoComplete="name"
                                {...register("fullName", { onChange: () => setMessage(null) })}
                                disabled={isLoading}
                            />
                            <p className="field-error">{errors.fullName?.message}</p>
                        </div>

                        <div className={`field-group ${errors.email ? "has-error" : ""}`}>
                            <label htmlFor="email">
                                Địa chỉ email <span>*</span>
                            </label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                placeholder="thanhvien@smartgym.com"
                                autoComplete="email"
                                {...register("email", { onChange: () => setMessage(null) })}
                                disabled={isLoading}
                            />
                            <p className="field-error">{errors.email?.message}</p>
                        </div>

                        <div className={`field-group ${errors.password ? "has-error" : ""}`}>
                            <label htmlFor="password">
                                Mật khẩu <span>*</span>
                            </label>
                            <div className="password-control">
                                <input
                                    id="password"
                                    name="password"
                                    type={visiblePassword.password ? "text" : "password"}
                                    placeholder="Nhập mật khẩu mạnh"
                                    autoComplete="new-password"
                                    {...register("password", { onChange: () => setMessage(null) })}
                                    disabled={isLoading}
                                />
                                <PasswordToggle
                                    label="mật khẩu"
                                    visible={visiblePassword.password}
                                    onToggle={() =>
                                        setVisiblePassword((current) => ({
                                            ...current,
                                            password: !current.password,
                                        }))
                                    }
                                />
                            </div>
                            <p className="field-error">{errors.password?.message}</p>
                        </div>

                        <div className="password-rules">{PASSWORD_POLICY_TEXT}</div>

                        <div className={`field-group ${errors.confirmPassword ? "has-error" : ""}`}>
                            <label htmlFor="confirmPassword">
                                Xác nhận mật khẩu <span>*</span>
                            </label>
                            <div className="password-control">
                                <input
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    type={visiblePassword.confirmPassword ? "text" : "password"}
                                    placeholder="Nhập lại mật khẩu của bạn"
                                    autoComplete="new-password"
                                    {...register("confirmPassword", { onChange: () => setMessage(null) })}
                                    disabled={isLoading}
                                />
                                <PasswordToggle
                                    label="xác nhận mật khẩu"
                                    visible={visiblePassword.confirmPassword}
                                    onToggle={() =>
                                        setVisiblePassword((current) => ({
                                            ...current,
                                            confirmPassword: !current.confirmPassword,
                                        }))
                                    }
                                />
                            </div>
                            <p className="field-error">{errors.confirmPassword?.message}</p>
                        </div>

                        <button
                            className={`submit-button ${isLoading ? "is-loading" : ""}`}
                            type="submit"
                            disabled={isLoading}
                        >
                            <span className="button-spinner" aria-hidden="true"></span>
                            <span>{isLoading ? "Đang đăng ký..." : "Đăng Ký Thành Viên Ngay"}</span>
                        </button>
                    </form>

                    <p className="auth-switch">
                        Đã là thành viên?{" "}
                        <button type="button" onClick={() => navigateTo("/login")}>
                            Đăng Nhập Tại Đây
                        </button>
                    </p>

                    <div className="legal-copy">
                        Bằng cách đăng ký, bạn đồng ý với <span>Điều Khoản Dịch Vụ</span> của chúng tôi và{" "}
                        <span>Chính Sách Bảo Mật</span>
                    </div>
                </div>
            </section>
        </main>
    );
}
