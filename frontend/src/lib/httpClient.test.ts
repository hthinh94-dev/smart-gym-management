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

function apiErrorAdapter(errorCode: "ACC-005" | "ACC-007") {
    return async (config: InternalAxiosRequestConfig) => {
        throw new AxiosError(
            "Unauthorized",
            "ERR_BAD_REQUEST",
            config,
            undefined,
            {
                data: { success: false, errorCode, message: "Unauthorized", details: {} },
                status: 401,
                statusText: "Unauthorized",
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

    it("xóa session khi backend trả ACC-005", async () => {
        saveTestSession();

        await expect(httpClient.get("/probe", {
            adapter: apiErrorAdapter("ACC-005") as AxiosRequestConfig["adapter"],
        })).rejects.toBeInstanceOf(AxiosError);

        expect(sessionStorage.getItem(AUTH_SESSION_STORAGE_KEY)).toBeNull();
    });

    it("không xóa session khi Login trả ACC-007", async () => {
        saveTestSession();

        await expect(httpClient.post("/auth/login", {}, {
            adapter: apiErrorAdapter("ACC-007") as AxiosRequestConfig["adapter"],
        })).rejects.toBeInstanceOf(AxiosError);

        expect(sessionStorage.getItem(AUTH_SESSION_STORAGE_KEY)).not.toBeNull();
    });
});
