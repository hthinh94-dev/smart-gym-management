import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../../lib/httpClient";
import { BodyProgressApiError, getMemberBodyProgress, upsertMemberBodyProgress } from "./memberBodyProgressApi";

const item = { id: 1, memberId: 101, recordDate: "2026-08-05", weightKg: 72.2, createdAt: "2026-08-05T01:00:00Z", updatedAt: "2026-08-05T01:00:00Z" };
afterEach(() => vi.restoreAllMocks());
describe("memberBodyProgressApi", () => {
    it("gets history with the member endpoint", async () => { vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [item] } }); expect(await getMemberBodyProgress()).toEqual([item]); expect(httpClient.get).toHaveBeenCalledWith("/member/body-progress"); });
    it("posts the upsert payload", async () => { vi.spyOn(httpClient, "post").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: item } }); const payload = { recordDate: item.recordDate, weightKg: item.weightKg }; await upsertMemberBodyProgress(payload); expect(httpClient.post).toHaveBeenCalledWith("/member/body-progress", payload); });
    it("maps API validation errors", async () => { vi.spyOn(httpClient, "post").mockRejectedValue(Object.assign(new Error("invalid"), { isAxiosError: true, response: { status: 400, data: { success: false, errorCode: "VAL-001", message: "invalid", details: { field: "weightKg" } } } })); const expected: Partial<BodyProgressApiError> = { errorCode: "VAL-001" }; await expect(upsertMemberBodyProgress({ recordDate: item.recordDate, weightKg: 0 })).rejects.toMatchObject(expected); });
    it("maps a missing response to a network error", async () => { vi.spyOn(httpClient, "get").mockRejectedValue(new Error("offline")); await expect(getMemberBodyProgress()).rejects.toMatchObject({ errorCode: "NETWORK-001" }); });
});
