import { createContext, useCallback, useEffect, useMemo, useState } from "react";
import type { PropsWithChildren } from "react";
import { getCurrentUser, login as loginRequest } from "../api/authApi";
import {
    AUTH_SESSION_CHANGED_EVENT,
    clearAuthSession,
    readAuthSession,
    saveAuthSession,
} from "../storage/authSession";
import type { AuthUser, LoginFormValues } from "../types/auth.types";

export type AuthContextValue = {
    user: AuthUser | null;
    isAuthenticated: boolean;
    isRestoringSession: boolean;
    login: (values: LoginFormValues) => Promise<AuthUser>;
    logout: () => void;
};

export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
    const [user, setUser] = useState<AuthUser | null>(null);
    const [isRestoringSession, setIsRestoringSession] = useState(() => readAuthSession() !== null);

    useEffect(() => {
        let active = true;

        async function restoreSession() {
            if (!readAuthSession()) {
                setIsRestoringSession(false);
                return;
            }

            try {
                const response = await getCurrentUser();
                if (active) {
                    setUser(response.data);
                }
            } catch {
                if (active) {
                    setUser(null);
                }
            } finally {
                if (active) {
                    setIsRestoringSession(false);
                }
            }
        }

        void restoreSession();
        return () => {
            active = false;
        };
    }, []);

    useEffect(() => {
        function syncClearedSession() {
            if (!readAuthSession()) {
                setUser(null);
            }
        }

        window.addEventListener(AUTH_SESSION_CHANGED_EVENT, syncClearedSession);
        return () => window.removeEventListener(AUTH_SESSION_CHANGED_EVENT, syncClearedSession);
    }, []);

    const login = useCallback(async (values: LoginFormValues) => {
        const loginResponse = await loginRequest(values);
        saveAuthSession(loginResponse.data);

        try {
            const currentUserResponse = await getCurrentUser();
            setUser(currentUserResponse.data);
            return currentUserResponse.data;
        } catch (error) {
            clearAuthSession();
            setUser(null);
            throw error;
        }
    }, []);

    const logout = useCallback(() => {
        clearAuthSession();
        setUser(null);
    }, []);

    const value = useMemo<AuthContextValue>(() => ({
        user,
        isAuthenticated: user !== null,
        isRestoringSession,
        login,
        logout,
    }), [isRestoringSession, login, logout, user]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
