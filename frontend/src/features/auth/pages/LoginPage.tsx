import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { AuthApiError } from "../api/authApi";
import { useAuth } from "../hooks/useAuth";
import { loginSchema } from "../schemas/loginSchema";
import type { AuthErrorCode, AuthUser, LoginFormValues } from "../types/auth.types";

type AuthRoute = "/register" | "/login";

type LoginPageProps = {
    onNavigate?: (path: AuthRoute) => void;
};

type LoginMessage = {
    title: string;
    description: string;
};

const loginErrorMessages: Record<AuthErrorCode, LoginMessage> = {
    "ACC-001": {
        title: "Không thể đăng nhập",
        description: "Thông tin tài khoản không hợp lệ. Vui lòng kiểm tra lại.",
    },
    "ACC-002": {
        title: "Không thể đăng nhập",
        description: "Mật khẩu không hợp lệ. Vui lòng kiểm tra lại.",
    },
    "ACC-004": {
        title: "Tài khoản bị khóa",
        description: "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.",
    },
    "ACC-005": {
        title: "Phiên đăng nhập đã hết hạn",
        description: "Token xác thực không còn hợp lệ. Vui lòng đăng nhập lại.",
    },
    "ACC-006": {
        title: "Tài khoản đã bị vô hiệu hóa",
        description: "Tài khoản đã bị vô hiệu hóa vĩnh viễn. Vui lòng liên hệ ban quản trị.",
    },
    "ACC-007": {
        title: "Thông tin đăng nhập không đúng",
        description: "Email hoặc mật khẩu không chính xác. Vui lòng thử lại.",
    },
    "VAL-001": {
        title: "Dữ liệu chưa hợp lệ",
        description: "Vui lòng kiểm tra lại email và mật khẩu.",
    },
    "SYS-001": {
        title: "Hệ thống chưa sẵn sàng",
        description: "Không thể xử lý đăng nhập lúc này. Vui lòng thử lại sau.",
    },
    "NETWORK-001": {
        title: "Không thể kết nối",
        description: "Không kết nối được backend. Vui lòng kiểm tra mạng và thử lại.",
    },
};

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

function CloseIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M18 6 6 18M6 6l12 12" />
        </svg>
    );
}

function roleLabel(role: AuthUser["role"]) {
    if (role === "ROLE_ADMIN") {
        return "Quản trị viên";
    }
    if (role === "ROLE_PT") {
        return "Huấn luyện viên";
    }
    return "Hội viên";
}

function errorMessage(error: unknown): LoginMessage {
    if (error instanceof AuthApiError) {
        return loginErrorMessages[error.errorCode] ?? {
            title: "Không thể đăng nhập",
            description: error.message,
        };
    }

    return {
        title: "Không thể đăng nhập",
        description: "Đã xảy ra lỗi không xác định. Vui lòng thử lại.",
    };
}

export function LoginPage({ onNavigate }: LoginPageProps) {
    const routerNavigate = useNavigate();
    const { login } = useAuth();
    const [showPassword, setShowPassword] = useState(false);
    const loginMutation = useMutation({ mutationFn: login });
    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
        defaultValues: { email: "", password: "" },
        mode: "onSubmit",
        reValidateMode: "onChange",
    });

    const isLoading = loginMutation.isPending;
    const authenticatedUser = loginMutation.data;
    const apiMessage = loginMutation.isError ? errorMessage(loginMutation.error) : null;

    function navigateTo(path: AuthRoute) {
        if (onNavigate) {
            onNavigate(path);
            return;
        }
        routerNavigate(path);
    }

    return (
        <main className="login-page">
            <section className="login-panel" aria-label="Form đăng nhập">
                <div className="brand-mini">
                    <div className="brand-badge login-logo" aria-label="Smart Gym logo">
                        <div className="brand-mark-bar" aria-hidden="true">
                            <span></span>
                        </div>
                        <strong>SMART GYM</strong>
                        <small>FITNESS SYSTEM</small>
                    </div>
                    <span className="brand-mini-title">SMART GYM</span>
                </div>

                <header className="form-header compact login-header">
                    <h2>Đăng nhập</h2>
                    <p>Tiếp tục quản lý lịch tập, gói tập và tiến trình của bạn.</p>
                </header>

                {authenticatedUser && !apiMessage && (
                    <div className="login-alert login-alert-success" role="status">
                        <div>
                            <strong>Đăng nhập thành công</strong>
                            <p>
                                {authenticatedUser.fullName} · {roleLabel(authenticatedUser.role)}
                            </p>
                        </div>
                    </div>
                )}

                {apiMessage && (
                    <div className="login-alert login-alert-error" role="alert">
                        <div>
                            <strong>{apiMessage.title}</strong>
                            <p>{apiMessage.description}</p>
                        </div>
                        <button
                            className="toast-close"
                            type="button"
                            aria-label="Đóng thông báo"
                            onClick={() => loginMutation.reset()}
                        >
                            <CloseIcon />
                        </button>
                    </div>
                )}

                <form className="auth-form login-form" onSubmit={handleSubmit((values) => loginMutation.mutate(values))} noValidate>
                    <div className={`field-group ${errors.email ? "has-error" : ""}`}>
                        <label htmlFor="loginEmail">
                            Email <span>*</span>
                        </label>
                        <input
                            id="loginEmail"
                            type="email"
                            placeholder="user@gmail.com"
                            autoComplete="email"
                            aria-invalid={Boolean(errors.email)}
                            aria-describedby={errors.email ? "loginEmailError" : undefined}
                            disabled={isLoading}
                            {...register("email", { onChange: () => loginMutation.reset() })}
                        />
                        <p id="loginEmailError" className="field-error" role={errors.email ? "alert" : undefined}>
                            {errors.email?.message}
                        </p>
                    </div>

                    <div className={`field-group ${errors.password ? "has-error" : ""}`}>
                        <label htmlFor="loginPassword">
                            Mật khẩu <span>*</span>
                        </label>
                        <div className="password-control">
                            <input
                                id="loginPassword"
                                type={showPassword ? "text" : "password"}
                                placeholder="Nhập mật khẩu"
                                autoComplete="current-password"
                                aria-invalid={Boolean(errors.password)}
                                aria-describedby={errors.password ? "loginPasswordError" : undefined}
                                disabled={isLoading}
                                {...register("password", { onChange: () => loginMutation.reset() })}
                            />
                            <button
                                className="toggle-password"
                                type="button"
                                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                                disabled={isLoading}
                                onClick={() => setShowPassword((visible) => !visible)}
                            >
                                {showPassword ? <EyeIcon /> : <EyeOffIcon />}
                            </button>
                        </div>
                        <p id="loginPasswordError" className="field-error" role={errors.password ? "alert" : undefined}>
                            {errors.password?.message}
                        </p>
                    </div>

                    <button className={`submit-button ${isLoading ? "is-loading" : ""}`} type="submit" disabled={isLoading}>
                        <span className="button-spinner" aria-hidden="true"></span>
                        <span>{isLoading ? "Đang đăng nhập..." : "Đăng Nhập"}</span>
                    </button>
                </form>

                <p className="auth-switch login-switch">
                    Chưa có tài khoản?{" "}
                    <button type="button" onClick={() => navigateTo("/register")}>
                        Đăng Ký Ngay
                    </button>
                </p>
            </section>
        </main>
    );
}
