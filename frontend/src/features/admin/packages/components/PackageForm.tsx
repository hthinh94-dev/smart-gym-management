import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import type { AdminMembershipPackage, MembershipPackageInput } from "../types/adminPackage.types";

const schema = z.object({
    name: z.string().trim().min(3, "Package name must contain at least 3 characters.").max(100, "Maximum 100 characters."),
    durationDays: z.coerce.number().int().min(1, "Minimum duration is 1 day.").max(3650, "Maximum duration is 3650 days."),
    price: z.coerce.number().finite().min(0, "Price cannot be negative.").max(9_999_999_999.99, "Price exceeds the supported limit.").multipleOf(0.01, "Price supports at most 2 decimal places."),
    description: z.string().trim().max(1000, "Maximum 1000 characters."),
});
type Values = z.input<typeof schema>;
export function PackageForm({ current, isPending, apiError, onCancel, onSubmit }: { current?: AdminMembershipPackage; isPending: boolean; apiError?: string; onCancel: () => void; onSubmit: (input: MembershipPackageInput) => void }) {
    const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name: "", durationDays: 30, price: 0, description: "" } });
    useEffect(() => { form.reset(current ? { name: current.name, durationDays: current.durationDays, price: current.price, description: current.description } : { name: "", durationDays: 30, price: 0, description: "" }); }, [current, form]);
    return <form className="admin-package-form" onSubmit={form.handleSubmit((values) => onSubmit({ name: values.name.trim(), durationDays: Number(values.durationDays), price: Number(values.price), description: values.description.trim() }))} noValidate><header><div><p>{current ? "Update package" : "New package"}</p><h2>{current ? current.name : "Create membership package"}</h2></div><button type="button" aria-label="Close package form" onClick={onCancel}>X</button></header><div className="admin-package-form-grid"><label>Package name *<input {...form.register("name")} />{form.formState.errors.name && <span>{form.formState.errors.name.message}</span>}</label><label>Duration (days) *<input type="number" {...form.register("durationDays")} />{form.formState.errors.durationDays && <span>{form.formState.errors.durationDays.message}</span>}</label><label>Price (VND) *<input type="number" step="1000" {...form.register("price")} />{form.formState.errors.price && <span>{form.formState.errors.price.message}</span>}</label><label className="package-description-field">Description<textarea rows={4} {...form.register("description")} />{form.formState.errors.description && <span>{form.formState.errors.description.message}</span>}</label></div>{apiError && <div className="admin-package-form-error" role="alert">{apiError}</div>}<footer><button type="button" onClick={onCancel} disabled={isPending}>Cancel</button><button type="submit" disabled={isPending}>{isPending ? "Saving..." : current ? "Save changes" : "Create package"}</button></footer></form>;
}
