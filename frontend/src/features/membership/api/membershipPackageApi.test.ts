import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../lib/httpClient";
import { getMembershipPackages, isPackageError } from "./membershipPackageApi";
const item = { id: 1, name: "Starter 30", durationDays: 30, price: 500000, description: "Thirty days of training." };
afterEach(() => vi.restoreAllMocks());
describe("membershipPackageApi", () => { it("loads the public package catalog", async () => { vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [item] } }); expect(await getMembershipPackages()).toEqual([item]); expect(httpClient.get).toHaveBeenCalledWith("/packages"); }); it("rejects an invalid response contract", async () => { vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [{ id: 1 }] } }); await expect(getMembershipPackages()).rejects.toMatchObject({ errorCode: "SYS-001" }); }); });

describe("package error contract", () => {
    it.each(["ACC-004", "ACC-005", "ACC-006", "AUTH-002", "SUB-002", "SUB-007", "VAL-001"])(
        "accepts backend error code %s",
        (errorCode) => {
            expect(isPackageError({ success: false, errorCode, message: "error", details: {} })).toBe(true);
        },
    );
});
