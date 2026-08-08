import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PackageForm } from "./PackageForm";

describe("PackageForm", () => {
    it("matches backend name and price validation", async () => {
        const user = userEvent.setup();
        const onSubmit = vi.fn();
        render(<PackageForm isPending={false} onCancel={vi.fn()} onSubmit={onSubmit} />);

        await user.type(screen.getByLabelText("Package name *"), "ab");
        await user.clear(screen.getByLabelText("Price (VND) *"));
        await user.type(screen.getByLabelText("Price (VND) *"), "1.001");
        await user.click(screen.getByRole("button", { name: "Create package" }));

        expect(await screen.findByText("Package name must contain at least 3 characters")).toBeInTheDocument();
        expect(await screen.findByText("Price supports at most 2 decimal places")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("prevents submit while a request is pending", () => {
        render(<PackageForm isPending onCancel={vi.fn()} onSubmit={vi.fn()} />);

        expect(screen.getByRole("button", { name: "Saving" })).toBeDisabled();
        expect(screen.getByRole("button", { name: "Cancel" })).toBeDisabled();
    });
});
