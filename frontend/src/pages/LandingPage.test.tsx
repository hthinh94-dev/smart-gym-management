import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../lib/httpClient";
import { LandingPage } from "./LandingPage";
vi.mock("../features/auth/hooks/useAuth", () => ({ useAuth: () => ({ user: null, isRestoringSession: false }) }));
const item = { id: 1, name: "Starter 30", durationDays: 30, price: 500000, description: "Thirty days." };
function renderPage() { const client = new QueryClient({ defaultOptions: { queries: { retry: false } } }); return render(<MemoryRouter><QueryClientProvider client={client}><LandingPage /></QueryClientProvider></MemoryRouter>); }
afterEach(() => vi.restoreAllMocks());
describe("LandingPage", () => { it("shows guest auth CTAs and active packages", async () => { vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [item] } }); renderPage(); expect(screen.getByRole("link", { name: "Login" })).toHaveAttribute("href", "/login"); expect(screen.getByRole("link", { name: "Join now" })).toHaveAttribute("href", "/register"); expect(await screen.findByRole("heading", { name: "Starter 30" })).toBeInTheDocument(); }); it("shows empty catalog state", async () => { vi.spyOn(httpClient, "get").mockResolvedValue({ status: 200, data: { success: true, message: "ok", data: [] } }); renderPage(); expect(await screen.findByRole("heading", { name: "No active packages" })).toBeInTheDocument(); }); it("retries after catalog error", async () => { vi.spyOn(httpClient, "get").mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce({ status: 200, data: { success: true, message: "ok", data: [item] } }); const user = userEvent.setup(); renderPage(); await user.click(await screen.findByRole("button", { name: "Retry" })); expect(await screen.findByRole("heading", { name: "Starter 30" })).toBeInTheDocument(); }); });
