import type { AuthSession, LoginResponse } from "../types/auth.types";

export const AUTH_SESSION_STORAGE_KEY = "smartGym.authSession";
export const AUTH_SESSION_CHANGED_EVENT = "smartgym:auth-session-changed";

function isAuthSession(value: unknown): value is AuthSession {
    if (value === null || typeof value !== "object") {
        return false;
    }

    const candidate = value as Record<string, unknown>;
    return typeof candidate.accessToken === "string"
        && candidate.accessToken.length > 0
        && candidate.tokenType === "Bearer"
        && typeof candidate.expiresAt === "number"
        && Number.isFinite(candidate.expiresAt);
}

function notifySessionChanged() {
    if (typeof window !== "undefined") {
        window.dispatchEvent(new Event(AUTH_SESSION_CHANGED_EVENT));
    }
}

export function saveAuthSession(loginResponse: LoginResponse, now = Date.now()): AuthSession {
    const session: AuthSession = {
        accessToken: loginResponse.accessToken,
        tokenType: loginResponse.tokenType,
        expiresAt: now + loginResponse.expiresIn * 1000,
    };

    sessionStorage.setItem(AUTH_SESSION_STORAGE_KEY, JSON.stringify(session));
    notifySessionChanged();
    return session;
}

export function readAuthSession(now = Date.now()): AuthSession | null {
    const storedValue = sessionStorage.getItem(AUTH_SESSION_STORAGE_KEY);
    if (!storedValue) {
        return null;
    }

    try {
        const parsed: unknown = JSON.parse(storedValue);
        if (!isAuthSession(parsed) || parsed.expiresAt <= now) {
            clearAuthSession();
            return null;
        }
        return parsed;
    } catch {
        clearAuthSession();
        return null;
    }
}

export function getStoredAccessToken(): string | null {
    return readAuthSession()?.accessToken ?? null;
}

export function clearAuthSession() {
    sessionStorage.removeItem(AUTH_SESSION_STORAGE_KEY);
    notifySessionChanged();
}
