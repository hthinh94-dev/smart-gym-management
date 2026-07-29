import { useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { AuthApiError, login } from "../api/authApi";
import type { LoginFormValues } from "../types/auth.types";

type AuthRoute = "/register" | "/login";

type LoginPageProps = {
    onNavigate?: (path: AuthRoute) => void;
};

const initialForm: LoginFormValues = {
    email: "",
    password: "",
};

export function LoginPage({ onNavigate }: LoginPageProps) {
    const routerNavigate = useNavigate();
    const [form, setForm] = useState(initialForm);
    const [isLoading, setIsLoading] = useState(false);
    const [message, setMessage] = useState("");

    function navigateTo(path: AuthRoute) {
        if (onNavigate) {
            onNavigate(path);
            return;
        }
        routerNavigate(path);
    }

    function updateField(event: ChangeEvent<HTMLInputElement>) {
        const { name, value } = event.target;

        setForm((current) => ({ ...current, [name]: value }));
        setMessage("");
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setIsLoading(true);
        setMessage("");

        try {
            await login(form);
            setMessage("Đăng nhập mô phỏng thành công. Backend sẽ được nối ở bước sau.");
        } catch (error) {
            setMessage(error instanceof AuthApiError ? error.message : "Không thể đăng nhập.");
        } finally {
            setIsLoading(false);
        }
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

                <header className="form-header compact">
                    <h2>Đăng nhập</h2>
                    <p>Tiếp tục quản lý lịch tập, gói tập và tiến trình của bạn.</p>
                </header>

                {message && (
                    <div className="login-message" role="status">
                        {message}
                    </div>
                )}

                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    <div className="field-group">
                        <label htmlFor="loginEmail">
                            Email <span>*</span>
                        </label>
                        <input
                            id="loginEmail"
                            name="email"
                            type="email"
                            placeholder="user@gmail.com"
                            autoComplete="email"
                            value={form.email}
                            onChange={updateField}
                            disabled={isLoading}
                        />
                    </div>

                    <div className="field-group">
                        <label htmlFor="loginPassword">
                            Mật khẩu <span>*</span>
                        </label>
                        <input
                            id="loginPassword"
                            name="password"
                            type="password"
                            placeholder="Nhập mật khẩu"
                            autoComplete="current-password"
                            value={form.password}
                            onChange={updateField}
                            disabled={isLoading}
                        />
                    </div>

                    <button className={`submit-button ${isLoading ? "is-loading" : ""}`} type="submit" disabled={isLoading}>
                        <span className="button-spinner" aria-hidden="true"></span>
                        <span>{isLoading ? "Đang đăng nhập..." : "Đăng Nhập"}</span>
                    </button>
                </form>

                <p className="auth-switch">
                    Chưa có tài khoản?{" "}
                    <button type="button" onClick={() => navigateTo("/register")}>
                        Đăng Ký Ngay
                    </button>
                </p>
            </section>
        </main>
    );
}
