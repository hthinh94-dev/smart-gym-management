import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PackageSelectList } from "./PackageSelectList";

const packages = [
    { id: 2, name: "Gói Tiêu Chuẩn", durationDays: 90, price: 1200000, description: "Lịch tập cơ bản." },
    { id: 3, name: "Gói Nâng Cao", durationDays: 180, price: 2000000, description: "Theo dõi nâng cao." },
];

describe("PackageSelectList", () => {
    it("chọn package và gửi đúng callback", async () => {
        const onSelect = vi.fn();
        const onSubmit = vi.fn();
        const user = userEvent.setup();
        render(<PackageSelectList packages={packages} onSelect={onSelect} onSubmit={onSubmit} isSubmitting={false} />);

        await user.click(screen.getByRole("radio", { name: /gói tiêu chuẩn/i }));
        expect(onSelect).toHaveBeenCalledWith(2);
        expect(screen.getByRole("button", { name: "Đăng ký gói tập" })).toBeDisabled();

        render(<PackageSelectList packages={packages} selectedPackageId={2} onSelect={onSelect} onSubmit={onSubmit} isSubmitting={false} />);
        await user.click(screen.getAllByRole("button", { name: "Đăng ký gói tập" })[1]);
        expect(onSubmit).toHaveBeenCalledOnce();
    });

    it("khóa lựa chọn và nút khi đang gửi", () => {
        render(<PackageSelectList packages={packages} selectedPackageId={2} onSelect={vi.fn()} onSubmit={vi.fn()} isSubmitting error="Đang gửi" />);

        expect(screen.getAllByRole("radio").every((radio) => (radio as HTMLInputElement).disabled)).toBe(true);
        expect(screen.getByRole("button", { name: /đang gửi yêu cầu/i })).toBeDisabled();
        expect(screen.getByRole("alert")).toHaveTextContent("Đang gửi");
    });
});
