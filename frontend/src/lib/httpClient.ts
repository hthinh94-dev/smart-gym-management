import axios from "axios";

const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

export const API_BASE_URL = (configuredApiBaseUrl || "http://localhost:8080/api/v1").replace(/\/+$/, "");

export const httpClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
    },
    timeout: 10_000,
});

export function createBearerHeader(token: string | null) {
    if (!token) {
        return {};
    }

    return {
        Authorization: `Bearer ${token}`,
    };
}
