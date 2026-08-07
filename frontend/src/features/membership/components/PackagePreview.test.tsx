import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PackagePreview } from "./PackagePreview";
describe("PackagePreview", () => { it("renders package contract fields", () => { render(<MemoryRouter><PackagePreview item={{ id: 1, name: "Starter 30", durationDays: 30, price: 500000, description: "Thirty days." }} /></MemoryRouter>); expect(screen.getByRole("heading", { name: "Starter 30" })).toBeInTheDocument(); expect(screen.getByText("30 days")).toBeInTheDocument(); expect(screen.getByText(/500.000/)).toBeInTheDocument(); expect(screen.getByRole("link", { name: "Choose package" })).toHaveAttribute("href", "/register"); }); });
