import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { createAdminPackage, deactivateAdminPackage, getAdminPackages, updateAdminPackage } from "../api/adminPackagesApi";
import { PackageForm } from "../components/PackageForm";
import { PackageTable } from "../components/PackageTable";
import type { AdminMembershipPackage, MembershipPackageInput } from "../types/adminPackage.types";
import { MembershipPackageApiError } from "../../../membership/api/membershipPackageApi";

const KEY = ["admin-packages"] as const;
function message(error: unknown) { return error instanceof MembershipPackageApiError ? `${error.errorCode} - ${error.message}` : "Unable to process package request."; }
export function AdminPackagesPage() {
    const client = useQueryClient();
    const [editing, setEditing] = useState<AdminMembershipPackage>();
    const [showForm, setShowForm] = useState(false);
    const [notice, setNotice] = useState("");
    const query = useQuery({ queryKey: KEY, queryFn: getAdminPackages });
    const save = useMutation({ mutationFn: ({ item, values }: { item?: AdminMembershipPackage; values: MembershipPackageInput }) => item ? updateAdminPackage(item.id, values) : createAdminPackage(values), onSuccess: (response) => { setNotice(response.message); setShowForm(false); setEditing(undefined); void client.invalidateQueries({ queryKey: KEY }); } });
    const deactivate = useMutation({ mutationFn: deactivateAdminPackage, onSuccess: (response) => { setNotice(response.message); void client.invalidateQueries({ queryKey: KEY }); } });
    const close = () => { if (!save.isPending) { save.reset(); setShowForm(false); setEditing(undefined); } };
    return <main className="admin-page admin-packages-page" id="main-content"><header className="admin-page-heading"><div><p className="page-eyebrow">Membership catalog</p><h1>Manage packages</h1><p>Create, update and stop selling packages without deleting subscription history.</p></div><button className="admin-create-package" type="button" onClick={() => { setNotice(""); save.reset(); setEditing(undefined); setShowForm(true); }}>Create package</button></header>{notice && <div className="admin-notification admin-notification-success" role="status"><p>{notice}</p><button type="button" aria-label="Close notification" onClick={() => setNotice("")}>X</button></div>}{query.isError && <div className="admin-notification admin-notification-error" role="alert"><div><strong>Unable to load packages</strong><p>{message(query.error)}</p></div><button type="button" onClick={() => void query.refetch()}>Retry</button></div>}{!query.isError && <PackageTable items={query.data?.data ?? []} isLoading={query.isLoading} pendingId={deactivate.isPending ? deactivate.variables : undefined} onEdit={(item) => { setNotice(""); save.reset(); setEditing(item); setShowForm(true); }} onDeactivate={(item) => { if (window.confirm(`Deactivate ${item.name}? Existing subscriptions will remain unchanged.`)) deactivate.mutate(item.id); }} />}{deactivate.isError && <div className="admin-notification admin-notification-error" role="alert"><p>{message(deactivate.error)}</p></div>}{showForm && <div className="admin-package-form-backdrop" role="presentation"><PackageForm current={editing} isPending={save.isPending} apiError={save.isError ? message(save.error) : undefined} onCancel={close} onSubmit={(values) => save.mutate({ item: editing, values })} /></div>}</main>;
}
