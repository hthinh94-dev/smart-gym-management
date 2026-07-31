import { AxiosError, AxiosHeaders } from "axios";
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from "axios";
import { afterEach, describe, expect, it } from "vitest";
import { AUTH_SESSION_STORAGE_KEY, saveAuthSession } from "../features/auth/storage/authSession";
import { httpClient } from "./httpClient";

function saveTestSession() {
    saveAuthSession({
        accessToken: "interceptor-token",
        tokenType: "Bearer",
        expiresIn: 3600,
        user: {
            id: 1,
            fullName: "Test User",
            email: "test@smartgym.com",
            role: "ROLE_MEMBER",
        },
    });
}

function apiErrorAdapter(errorCode: "ACC-004" | "ACC-005" | "ACC-006" | "ACC-007") {
    return async (config: InternalAxiosRequestConfig) => {
        const status = errorCode === "ACC-007" || errorCode === "ACC-005" ? 401 : 403;
        throw new AxiosError(
            "Unauthorized",
            "ERR_BAD_REQUEST",
            config,
            undefined,
            {
                data: { success: false, errorCode, message: "Unauthorized", details: {} },
                status,
                statusText: status === 401 ? "Unauthorized" : "Forbidden",
                headers: new AxiosHeaders(),
                config,
            },
        );
    };
}

afterEach(() => {
    sessionStorage.clear();
});

describe("httpClient auth interceptors", () => {
    it("gắn Bearer token từ sessionStorage", async () => {
        saveTestSession();
        let authorization: unknown;

        await httpClient.get("/probe", {
            adapter: async (config) => {
                authorization = config.headers.get("Authorization");
                return {
                    data: {},
                    status: 200,
                    statusText: "OK",
                    headers: new AxiosHeaders(),
                    config,
                };
            },
        });

        expect(authorization).toBe("Bearer interceptor-token");
    });

    it.each(["ACC-004", "ACC-005", "ACC-006"] as const)(
        "xóa session khi backend trả %s",
        async (errorCode) => {
        saveTestSession();

        await expect(httpClient.get("/probe", {
            adapter: apiErrorAdapter(errorCode) as AxiosRequestConfig["adapter"],
        })).rejects.toBeInstanceOf(AxiosError);

        expect(sessionStorage.getItem(AUTH_SESSION_STORAGE_KEY)).toBeNull();
        },
    );

    it("không xóa session khi Login trả ACC-007", async () => {
        saveTestSession();

        await expect(httpClient.post("/auth/login", {}, {
            adapter: apiErrorAdapter("ACC-007") as AxiosRequestConfig["adapter"],
        })).rejects.toBeInstanceOf(AxiosError);

        expect(sessionStorage.getItem(AUTH_SESSION_STORAGE_KEY)).not.toBeNull();
    });
});
