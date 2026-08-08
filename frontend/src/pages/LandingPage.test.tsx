import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../lib/httpClient";
import { LandingPage } from "./LandingPage";
const mockAuth = vi.hoisted(() => vi.fn());
vi.mock("../features/auth/hooks/useAuth", () => ({ useAuth: mockAuth }));
const item = { id: 1, name: "Starter 30", durationDays: 30, price: 500000, description: "Thirty days." };
function renderPage() { const client = new QueryClient({ defaultOptions: { queries: { retry: false } } }); return render(<MemoryRouter initialEntries={["/"]}><QueryClientProvider client={client}><Routes><Route path="/" element={<LandingPage />} /><Route path="/admin/users" element={<p>Admin home</p>} /><Route path="/member" element={<p>Member home</p>} /></Routes></QueryClientProvider></MemoryRouter>); }
beforeEach(() => mockAuth.mockReturnValue({ user: null, isRestoringSession: false }));
afterEach(() => vi.restoreAllMocks());
describe("LandingPage", () => {
    it("shows guest auth CTAs and active packages", async () => { vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [item] } }); renderPage(); expect(screen.getAllByRole("link", { name: "Đăng nhập" })[0]).toHaveAttribute("href", "/login"); expect(screen.getByRole("link", { name: "Đăng ký ngay" })).toHaveAttribute("href", "/register"); expect(await screen.findByRole("heading", { name: "Starter 30" })).toBeInTheDocument(); });
    it("shows the package preview and sends an authenticated Member to subscription", async () => { mockAuth.mockReturnValue({ user: { id: 8, fullName: "Member", email: "member@smartgym.com", role: "ROLE_MEMBER", accountStatus: "ACTIVE", createdAt: "2026-08-08" }, isRestoringSession: false }); vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [item] } }); const postSpy = vi.spyOn(httpClient, "post"); renderPage(); expect(await screen.findByRole("heading", { name: "Starter 30" })).toBeInTheDocument(); expect(screen.getByRole("link", { name: "Chọn gói này" })).toHaveAttribute("href", "/member/subscription"); expect(postSpy).not.toHaveBeenCalled(); });
    it("redirects Admin away from the Member landing flow", () => { mockAuth.mockReturnValue({ user: { id: 1, fullName: "Admin", email: "admin@smartgym.com", role: "ROLE_ADMIN", accountStatus: "ACTIVE", createdAt: "2026-08-08" }, isRestoringSession: false }); renderPage(); expect(screen.getByText("Admin home")).toBeInTheDocument(); });
    it("redirects PT away from the Member subscription flow", () => { mockAuth.mockReturnValue({ user: { id: 2, fullName: "PT", email: "pt@smartgym.com", role: "ROLE_PT", accountStatus: "ACTIVE", createdAt: "2026-08-08" }, isRestoringSession: false }); renderPage(); expect(screen.getByText("Member home")).toBeInTheDocument(); });
    it("shows empty catalog state", async () => { vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [] } }); renderPage(); expect(await screen.findByRole("heading", { name: "Chưa có gói tập đang mở" })).toBeInTheDocument(); });
    it("retries after catalog error without removing the hero", async () => { vi.spyOn(httpClient, "get").mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce({ status: 200, data: { success: true, message: "ok", data: [item] } }); const user = userEvent.setup(); renderPage(); expect(screen.getByRole("heading", { name: "SMART GYM" })).toBeInTheDocument(); await user.click(await screen.findByRole("button", { name: "Thử lại" })); expect(await screen.findByRole("heading", { name: "Starter 30" })).toBeInTheDocument(); expect(screen.getByRole("heading", { name: "SMART GYM" })).toBeInTheDocument(); });
});
