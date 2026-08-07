import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { RoleRoute } from "../../../../app/routes/RoleRoute";

vi.mock("../../../auth/hooks/useAuth", () => ({
    useAuth: () => ({
        user: {
            id: 101,
            fullName: "Route Member",
            email: "route-member@smartgym.test",
            role: "ROLE_MEMBER",
            accountStatus: "ACTIVE",
        },
    }),
}));

function LocationProbe() {
    const location = useLocation();
    return <output aria-label="Current route">{location.pathname}</output>;
}

describe("Admin package route protection", () => {
    it("redirects a Member away from /admin/packages", async () => {
        render(
            <MemoryRouter initialEntries={["/admin/packages"]}>
                <Routes>
                    <Route element={<RoleRoute allowedRoles={["ROLE_ADMIN"]} />}>
                        <Route path="/admin" element={<Outlet />}>
                            <Route path="packages" element={<div>Admin packages</div>} />
                        </Route>
                    </Route>
                    <Route path="/member" element={<div>Member home</div>} />
                </Routes>
                <LocationProbe />
            </MemoryRouter>,
        );

        await waitFor(() => expect(screen.getByLabelText("Current route")).toHaveTextContent("/member"));
        expect(screen.queryByText("Admin packages")).not.toBeInTheDocument();
        expect(screen.getByText("Member home")).toBeInTheDocument();
    });
});
