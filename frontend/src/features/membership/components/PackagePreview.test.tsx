import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PackagePreview } from "./PackagePreview";
describe("PackagePreview", () => {
    it("uses register as the default guest destination", () => {
        render(<MemoryRouter><PackagePreview item={{ id: 1, name: "Starter 30", durationDays: 30, price: 500000, description: "Thirty days." }} /></MemoryRouter>);

        expect(screen.getByRole("heading", { name: "Starter 30" })).toBeInTheDocument();
        expect(screen.getByText("30 ngày")).toBeInTheDocument();
        expect(screen.getByText(/500.000/)).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Chọn gói này" })).toHaveAttribute("href", "/register");
    });

    it("supports the member subscription destination", () => {
        render(<MemoryRouter><PackagePreview destination="/member/subscription" item={{ id: 1, name: "Starter 30", durationDays: 30, price: 500000, description: "Thirty days." }} /></MemoryRouter>);

        expect(screen.getByRole("link", { name: "Chọn gói này" })).toHaveAttribute("href", "/member/subscription");
    });
});
