import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, type SubmitHandler } from "react-hook-form";
import { z } from "zod";
import type { BodyProgressUpsertRequest } from "../types/memberBodyProgress.types";
import { getVietnamBusinessDate } from "../utils/businessDate";

const optionalMass = z.preprocess((value) => value === "" || value === null || value === undefined ? undefined : Number(value), z.number().positive("Giá trị phải lớn hơn 0.").max(9999.99, "Giá trị vượt quá giới hạn cho phép.").optional());
const schema = z.object({
    recordDate: z.string().min(1, "Ngày ghi nhận là bắt buộc.").refine((value) => value <= getVietnamBusinessDate(), "Ngày ghi nhận không được ở tương lai."),
    weightKg: z.coerce.number().positive("Cân nặng phải lớn hơn 0.").max(9999.99, "Cân nặng vượt quá giới hạn cho phép."),
    muscleMassKg: optionalMass,
    fatMassKg: optionalMass,
});
type FormValues = z.input<typeof schema>;

export function BodyProgressForm({ isSaving, error, onSubmit }: { isSaving: boolean; error?: unknown; onSubmit: (payload: BodyProgressUpsertRequest) => void }) {
    const form = useForm<FormValues>({ defaultValues: { recordDate: getVietnamBusinessDate(), weightKg: "", muscleMassKg: "", fatMassKg: "" }, resolver: zodResolver(schema), mode: "onBlur" });
    const submit: SubmitHandler<FormValues> = (values) => onSubmit({
        recordDate: values.recordDate,
        weightKg: Number(values.weightKg),
        ...(values.muscleMassKg === undefined ? {} : { muscleMassKg: Number(values.muscleMassKg) }),
        ...(values.fatMassKg === undefined ? {} : { fatMassKg: Number(values.fatMassKg) }),
    });
    return <form className="progress-form" onSubmit={form.handleSubmit(submit)} noValidate>
        <div className="progress-form-grid"><label>Ngày ghi nhận *<input type="date" {...form.register("recordDate")} />{form.formState.errors.recordDate && <span className="progress-field-error">{form.formState.errors.recordDate.message}</span>}</label><label>Cân nặng (kg) *<input type="number" step="0.01" min="0.01" placeholder="72.20" {...form.register("weightKg")} />{form.formState.errors.weightKg && <span className="progress-field-error">{form.formState.errors.weightKg.message}</span>}</label><label>Khối lượng cơ (kg)<span className="optional-label">Không bắt buộc</span><input type="number" step="0.01" min="0.01" placeholder="32.50" {...form.register("muscleMassKg")} />{form.formState.errors.muscleMassKg && <span className="progress-field-error">{form.formState.errors.muscleMassKg.message}</span>}</label><label>Khối lượng mỡ (kg)<span className="optional-label">Không bắt buộc</span><input type="number" step="0.01" min="0.01" placeholder="16.80" {...form.register("fatMassKg")} />{form.formState.errors.fatMassKg && <span className="progress-field-error">{form.formState.errors.fatMassKg.message}</span>}</label></div>
        {error instanceof Error && <div className="progress-api-error" role="alert">{error.message}</div>}
        <button className="profile-primary-button" type="submit" disabled={isSaving}>{isSaving ? "Đang lưu..." : "Lưu cân nặng"}</button>
    </form>;
}
