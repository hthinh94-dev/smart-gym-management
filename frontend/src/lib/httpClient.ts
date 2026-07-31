import axios from "axios";
import { clearAuthSession, getStoredAccessToken } from "../features/auth/storage/authSession";

const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
const SESSION_TERMINATING_ERROR_CODES = new Set(["ACC-004", "ACC-005", "ACC-006"]);

export const API_BASE_URL = (configuredApiBaseUrl || "http://localhost:8080/api/v1").replace(/\/+$/, "");

export const httpClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
    },
    timeout: 10_000,
});

httpClient.interceptors.request.use((config) => {
    const token = getStoredAccessToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

httpClient.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
        if (axios.isAxiosError(error) && error.response?.data) {
            const payload = error.response.data as Record<string, unknown>;
            if (typeof payload.errorCode === "string"
                && SESSION_TERMINATING_ERROR_CODES.has(payload.errorCode)) {
                clearAuthSession();
            }
        }
        return Promise.reject(error);
    },
);

export function createBearerHeader(token: string | null) {
    if (!token) {
        return {};
    }

    return {
        Authorization: `Bearer ${token}`,
    };
}
