import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { BodyProgressList } from "./BodyProgressList";

const item = { id: 1, memberId: 101, recordDate: "2026-08-05", weightKg: 72.2, createdAt: "2026-08-05T01:00:00Z", updatedAt: "2026-08-05T01:00:00Z" };
describe("BodyProgressList", () => { it("shows empty state", () => { render(<BodyProgressList items={[]} />); expect(screen.getByRole("heading", { name: "Chưa có bản ghi cân nặng" })).toBeInTheDocument(); }); it("shows records in the history table", () => { render(<BodyProgressList items={[item]} />); expect(screen.getByText("05/08/2026")).toBeInTheDocument(); expect(screen.getByText("72.20 kg")).toBeInTheDocument(); }); });
