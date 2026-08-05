import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../../lib/httpClient";
import { MemberBodyProgressPage } from "./MemberBodyProgressPage";

const item = { id: 1, memberId: 101, recordDate: "2026-08-05", weightKg: 72.2, createdAt: "2026-08-05T01:00:00Z", updatedAt: "2026-08-05T01:00:00Z" };
function renderPage() { const client = new QueryClient({ defaultOptions: { queries: { retry: false } } }); return render(<QueryClientProvider client={client}><MemberBodyProgressPage /></QueryClientProvider>); }
afterEach(() => vi.restoreAllMocks());
describe("MemberBodyProgressPage", () => { it("renders history and upserts without losing the list", async () => { vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [item] } }); vi.spyOn(httpClient, "post").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: { ...item, weightKg: 71.9 } } }); const user = userEvent.setup(); renderPage(); expect((await screen.findAllByText("72.20 kg")).length).toBeGreaterThan(0); await user.type(screen.getByLabelText("Cân nặng (kg) *"), "71.9"); await user.click(screen.getByRole("button", { name: "Lưu cân nặng" })); expect(await screen.findByRole("status")).toHaveTextContent("Đã lưu cân nặng thành công."); }); it("offers retry when history loading fails", async () => { vi.spyOn(httpClient, "get").mockRejectedValue(new Error("offline")); renderPage(); expect(await screen.findByRole("alert")).toHaveTextContent("Không thể tải lịch sử cân nặng."); expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument(); }); });
