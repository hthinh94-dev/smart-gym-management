import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "../../../../lib/httpClient";
import { AdminPackagesPage } from "./AdminPackagesPage";

const item = {
    id: 7,
    name: "Starter 30",
    durationDays: 30,
    price: 500000,
    description: "Thirty days.",
    isActive: true,
};

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(<QueryClientProvider client={client}><AdminPackagesPage /></QueryClientProvider>);
}

afterEach(() => vi.restoreAllMocks());

describe("AdminPackagesPage update and error states", () => {
    it("updates an existing package through the backend contract", async () => {
        vi.spyOn(httpClient, "get").mockResolvedValue({
            status: 200,
            data: { success: true, message: "ok", data: [item] },
        });
        const put = vi.spyOn(httpClient, "put").mockResolvedValue({
            status: 200,
            data: { success: true, message: "updated", data: { ...item, name: "Starter Plus" } },
        });
        const user = userEvent.setup();
        renderPage();

        await user.click(await screen.findByRole("button", { name: "Edit" }));
        await user.clear(screen.getByLabelText("Package name *"));
        await user.type(screen.getByLabelText("Package name *"), "Starter Plus");
        await user.click(screen.getByRole("button", { name: "Save changes" }));

        expect(await screen.findByRole("status")).toHaveTextContent("updated");
        expect(put).toHaveBeenCalledWith("/admin/packages/7", expect.objectContaining({
            name: "Starter Plus",
        }));
    });

    it("shows error without a contradictory empty state", async () => {
        vi.spyOn(httpClient, "get").mockRejectedValue(new Error("offline"));
        renderPage();

        expect(await screen.findByRole("alert")).toHaveTextContent("NETWORK-001");
        expect(screen.queryByRole("heading", { name: "No packages found" })).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
    });
});
