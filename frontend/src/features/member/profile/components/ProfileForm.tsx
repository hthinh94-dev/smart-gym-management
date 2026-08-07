import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm, type FieldPath, type SubmitHandler, type UseFormRegister } from "react-hook-form";
import {
    memberProfileSchema,
    toMemberProfileRequest,
    type MemberProfileFormValues,
    type MemberProfileSubmitValues,
} from "../schemas/memberProfileSchema";
import type { MemberProfile, MemberProfileUpsertRequest } from "../types/memberProfile.types";

const choices = {
    gender: [["MALE", "Nam"], ["FEMALE", "Nữ"]],
    goal: [["MUSCLE_GAIN", "Tăng cơ"], ["WEIGHT_GAIN", "Tăng cân"], ["FAT_LOSS", "Giảm mỡ"], ["WEIGHT_LOSS", "Giảm cân"]],
    level: [["BEGINNER", "Mới bắt đầu"], ["INTERMEDIATE", "Trung cấp"], ["ADVANCED", "Nâng cao"]],
    activity: [
        ["SEDENTARY", "Ít vận động"],
        ["LIGHTLY_ACTIVE", "Vận động nhẹ"],
        ["MODERATELY_ACTIVE", "Vận động vừa"],
        ["VERY_ACTIVE", "Vận động nhiều"],
    ],
    diet: [["OMNIVORE", "Ăn đa dạng"], ["VEGETARIAN", "Ăn chay"], ["VEGAN", "Thuần chay"]],
    equipment: [["BARBELL", "Đòn tạ"], ["DUMBBELL", "Tạ đơn"], ["MACHINE", "Máy tập"], ["CABLE", "Cáp kéo"], ["BENCH", "Ghế tập"]],
    muscles: [["CHEST", "Ngực"], ["BACK", "Lưng"], ["SHOULDERS", "Vai"], ["ARMS", "Tay"], ["LEGS", "Chân"], ["GLUTES", "Mông"], ["CORE", "Cơ trung tâm"], ["CARDIO", "Tim mạch"], ["FULL_BODY", "Toàn thân"]],
    injuries: [
        ["KNEE_FLEXION_LIMITED", "Hạn chế gập gối"],
        ["OVERHEAD_MOVEMENT_LIMITED", "Hạn chế động tác qua đầu"],
        ["LOWER_BACK_LOAD_LIMITED", "Hạn chế tải vùng lưng dưới"],
        ["WRIST_FLEXION_LIMITED", "Hạn chế gập cổ tay"],
        ["NECK_LOAD_LIMITED", "Hạn chế tải vùng cổ"],
    ],
    commonFoods: [["PEANUTS", "Đậu phộng"], ["MILK", "Sữa"], ["EGGS", "Trứng"], ["SHRIMP", "Tôm"], ["BEEF", "Thịt bò"], ["CHICKEN", "Thịt gà"], ["SEAFOOD", "Hải sản"], ["SOY", "Đậu nành"]],
} as const;

const commonFoodValues = new Set<string>(choices.commonFoods.map(([value]) => value));

function currentGoals(profile?: MemberProfile): MemberProfileFormValues["fitnessGoals"] {
    const source = profile?.bioProfile.fitnessGoals ?? (profile?.bioProfile.fitnessGoal ? [profile.bioProfile.fitnessGoal] : []);
    return [...new Set(source.map((goal) => goal === "BULK" ? "MUSCLE_GAIN" : goal === "CUT" ? "FAT_LOSS" : goal).filter((goal) => goal !== "MAINTAIN"))] as MemberProfileFormValues["fitnessGoals"];
}

type Props = {
    profile?: MemberProfile;
    isSaving: boolean;
    error?: unknown;
    onCancel: () => void;
    onSubmit: (request: MemberProfileUpsertRequest) => void;
};

function defaults(profile?: MemberProfile): MemberProfileFormValues {
    const bio = profile?.bioProfile;
    const nutrition = profile?.nutritionProfile;
    const fitnessGoals: MemberProfileFormValues["fitnessGoals"] = profile ? currentGoals(profile) : ["MUSCLE_GAIN"];
    return {
        gender: bio?.gender ?? "MALE",
        dateOfBirth: bio?.dateOfBirth ?? "",
        heightCm: bio?.heightCm ?? 0,
        weightKg: bio?.weightKg ?? 0,
        fitnessGoal: fitnessGoals[0] ?? "MUSCLE_GAIN",
        fitnessGoals,
        targetWeightKg: bio?.targetWeightKg ?? "",
        mobilityLimitNotes: bio?.mobilityLimitNotes ?? "",
        fitnessLevel: bio?.fitnessLevel ?? "BEGINNER",
        activityLevel: bio?.activityLevel ?? "SEDENTARY",
        workoutDaysPerWeek: bio?.workoutDaysPerWeek ?? 1,
        maxSessionMinutes: bio?.maxSessionMinutes ?? 30,
        availableEquipment: bio?.availableEquipment ?? [],
        targetMuscleGroups: bio?.targetMuscleGroups ?? [],
        injuryConstraints: bio?.injuryConstraints ?? [],
        dietaryPreference: nutrition?.dietaryPreference ?? "OMNIVORE",
        foodAllergies: nutrition?.foodAllergies.filter((value) => commonFoodValues.has(value)) ?? [],
        excludedFoods: nutrition?.excludedFoods.filter((value) => commonFoodValues.has(value)) ?? [],
        foodAllergiesText: nutrition?.foodAllergies.filter((value) => !commonFoodValues.has(value)).join(", ") ?? "",
        excludedFoodsText: nutrition?.excludedFoods.filter((value) => !commonFoodValues.has(value)).join(", ") ?? "",
        mealsPerDay: nutrition?.mealsPerDay ?? 3,
    };
}

function serverViolations(error: unknown): Record<string, string> {
    if (!error || typeof error !== "object" || !("details" in error)) return {};
    const details = (error as { details?: unknown }).details;
    if (!details || typeof details !== "object") return {};
    if (!("violations" in details)) {
        const field = "field" in details ? details.field : undefined;
        const constraint = "constraint" in details ? details.constraint : undefined;
        return typeof field === "string" && typeof constraint === "string"
            ? { [field]: constraint }
            : {};
    }
    const violations = (details as { violations?: unknown }).violations;
    if (!violations || typeof violations !== "object") return {};
    return Object.fromEntries(
        Object.entries(violations).filter((entry): entry is [string, string] => typeof entry[1] === "string"),
    );
}

function mapServerField(field: string): FieldPath<MemberProfileFormValues> | null {
    if (field.startsWith("foodAllergies")) return "foodAllergiesText";
    if (field.startsWith("excludedFoods")) return "excludedFoodsText";
    const supported = new Set<string>([
        "gender", "dateOfBirth", "heightCm", "weightKg", "fitnessGoal", "fitnessGoals", "targetWeightKg", "fitnessLevel",
        "activityLevel", "workoutDaysPerWeek", "maxSessionMinutes", "availableEquipment",
        "targetMuscleGroups", "injuryConstraints", "mobilityLimitNotes", "dietaryPreference", "mealsPerDay",
    ]);
    return supported.has(field) ? field as FieldPath<MemberProfileFormValues> : null;
}

function FieldError({ message }: { message?: string }) {
    return message ? <p className="profile-field-error" role="alert">{message}</p> : null;
}

export function ProfileForm({ profile, isSaving, error, onCancel, onSubmit }: Props) {
    const form = useForm<MemberProfileFormValues, unknown, MemberProfileSubmitValues>({
        defaultValues: defaults(profile),
        resolver: zodResolver(memberProfileSchema),
        mode: "onBlur",
    });
    const { setError } = form;
    useEffect(() => {
        Object.entries(serverViolations(error)).forEach(([field, message]) => {
            const formField = mapServerField(field);
            if (formField) setError(formField, { type: "server", message });
        });
    }, [error, setError]);

    const submit: SubmitHandler<MemberProfileSubmitValues> = (values) => {
        onSubmit(toMemberProfileRequest(values));
    };

    return (
        <form className="profile-form" onSubmit={form.handleSubmit(submit)} noValidate>
            <div className="profile-form-heading">
                <div>
                    <p className="page-eyebrow">Cập nhật hồ sơ</p>
                    <h2>Thông tin của bạn</h2>
                </div>
                <p>Các trường có dấu * là bắt buộc.</p>
            </div>

            {error instanceof Error && (
                <div className="profile-form-error" role="alert">
                    <strong>Không thể lưu hồ sơ</strong>
                    <span>{error.message}</span>
                </div>
            )}

            <div className="profile-form-grid">
                <SelectField label="Giới tính *" name="gender" register={form.register} options={choices.gender} error={form.formState.errors.gender?.message} />
                <InputField label="Ngày sinh *" type="date" name="dateOfBirth" register={form.register} error={form.formState.errors.dateOfBirth?.message} />
                <InputField label="Chiều cao (cm) *" type="number" step="0.01" name="heightCm" register={form.register} error={form.formState.errors.heightCm?.message} />
                <InputField label="Cân nặng (kg) *" type="number" step="0.01" name="weightKg" register={form.register} error={form.formState.errors.weightKg?.message} />
                <CollectionField label="Mục tiêu (chọn tối đa 2) *" options={choices.goal} selected={form.watch("fitnessGoals")} onChange={(values) => { form.setValue("fitnessGoals", values as MemberProfileFormValues["fitnessGoals"], { shouldValidate: true }); form.setValue("fitnessGoal", values[0] as MemberProfileFormValues["fitnessGoal"], { shouldValidate: true }); }} maxSelections={2} showSelectAll={false} error={form.formState.errors.fitnessGoals?.message} helper="Mục tiêu đầu tiên được dùng làm mục tiêu chính để tính calo." />
                <InputField label="Cân nặng mục tiêu (kg)" type="number" step="0.01" name="targetWeightKg" register={form.register} error={form.formState.errors.targetWeightKg?.message} />
                <SelectField label="Trình độ *" name="fitnessLevel" register={form.register} options={choices.level} error={form.formState.errors.fitnessLevel?.message} />
                <SelectField label="Mức vận động *" name="activityLevel" register={form.register} options={choices.activity} error={form.formState.errors.activityLevel?.message} />
                <InputField label="Số buổi tập / tuần *" type="number" name="workoutDaysPerWeek" register={form.register} error={form.formState.errors.workoutDaysPerWeek?.message} />
                <InputField label="Thời lượng tối đa (phút/buổi) *" type="number" name="maxSessionMinutes" register={form.register} error={form.formState.errors.maxSessionMinutes?.message} />
                <SelectField label="Chế độ ăn *" name="dietaryPreference" register={form.register} options={choices.diet} error={form.formState.errors.dietaryPreference?.message} />
                <InputField label="Số bữa mỗi ngày *" type="number" name="mealsPerDay" register={form.register} error={form.formState.errors.mealsPerDay?.message} />
            </div>

            <CollectionField label="Thiết bị có thể sử dụng" options={choices.equipment} selected={form.watch("availableEquipment")} onChange={(values) => form.setValue("availableEquipment", values as MemberProfileFormValues["availableEquipment"], { shouldValidate: true })} error={form.formState.errors.availableEquipment?.message} />
            <CollectionField label="Nhóm cơ ưu tiên" options={choices.muscles} selected={form.watch("targetMuscleGroups")} onChange={(values) => form.setValue("targetMuscleGroups", values as MemberProfileFormValues["targetMuscleGroups"], { shouldValidate: true })} error={form.formState.errors.targetMuscleGroups?.message} />
            <CollectionField label="Hạn chế vận động" options={choices.injuries} selected={form.watch("injuryConstraints")} onChange={(values) => form.setValue("injuryConstraints", values as MemberProfileFormValues["injuryConstraints"], { shouldValidate: true })} error={form.formState.errors.injuryConstraints?.message} />
            <label className="profile-mobility-note">
                Hạn chế vận động khác
                <input type="text" placeholder="Hoặc tự nhập, phân cách bằng dấu phẩy" {...form.register("mobilityLimitNotes")} />
                <small>Tối đa 500 ký tự. Nội dung này được lưu cùng hồ sơ của bạn.</small>
                <FieldError message={form.formState.errors.mobilityLimitNotes?.message} />
            </label>

            <div className="profile-form-grid profile-form-text-grid">
                <FoodField label="Thực phẩm gây dị ứng" selected={form.watch("foodAllergies")} onChange={(values) => form.setValue("foodAllergies", values, { shouldValidate: true })} customName="foodAllergiesText" register={form.register} options={choices.commonFoods} error={form.formState.errors.foodAllergiesText?.message} />
                <FoodField label="Thực phẩm loại trừ" selected={form.watch("excludedFoods")} onChange={(values) => form.setValue("excludedFoods", values, { shouldValidate: true })} customName="excludedFoodsText" register={form.register} options={choices.commonFoods} error={form.formState.errors.excludedFoodsText?.message} />
            </div>

            <div className="profile-form-actions">
                <button type="button" className="profile-secondary-button" onClick={onCancel} disabled={isSaving}>Hủy</button>
                <button type="submit" className="profile-primary-button" disabled={isSaving}>
                    {isSaving ? "Đang lưu..." : "Lưu hồ sơ"}
                </button>
            </div>
        </form>
    );
}

type Register = UseFormRegister<MemberProfileFormValues>;

function InputField({ label, name, register, error, type = "text", step }: {
    label: string;
    name: keyof MemberProfileFormValues;
    register: Register;
    error?: string;
    type?: string;
    step?: string;
}) {
    return (
        <label>
            {label}
            <input type={type} step={step} {...register(name)} />
            <FieldError message={error} />
        </label>
    );
}

function SelectField({ label, name, register, options, error }: {
    label: string;
    name: keyof MemberProfileFormValues;
    register: Register;
    options: readonly (readonly [string, string])[];
    error?: string;
}) {
    return (
        <label>
            {label}
            <select {...register(name)}>
                {options.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
            </select>
            <FieldError message={error} />
        </label>
    );
}

function CollectionField({ label, options, selected, onChange, error, maxSelections, helper, showSelectAll = true }: {
    label: string;
    options: readonly (readonly [string, string])[];
    selected: string[];
    onChange: (values: string[]) => void;
    error?: string;
    maxSelections?: number;
    helper?: string;
    showSelectAll?: boolean;
}) {
    const allSelected = selected.length === options.length;
    return (
        <fieldset className="profile-collection-field">
            <legend>{label}</legend>
            {showSelectAll && <label className="profile-select-all">
                <input type="checkbox" checked={allSelected} onChange={(event) => onChange(event.target.checked ? options.map(([value]) => value) : [])} />
                Chọn tất cả
            </label>}
            <div className="profile-checkbox-grid">
                {options.map(([value, text]) => (
                    <label key={value}>
                        <input
                            type="checkbox"
                            value={value}
                            checked={selected.includes(value)}
                            disabled={!selected.includes(value) && maxSelections !== undefined && selected.length >= maxSelections}
                            onChange={(event) => onChange(event.target.checked ? [...selected, value] : selected.filter((item) => item !== value))}
                        />
                        {text}
                    </label>
                ))}
            </div>
            {helper && <small>{helper}</small>}
            <FieldError message={error} />
        </fieldset>
    );
}

function FoodField({ label, selected, onChange, customName, register, options, error }: {
    label: string;
    selected: string[];
    onChange: (values: string[]) => void;
    customName: "foodAllergiesText" | "excludedFoodsText";
    register: Register;
    options: readonly (readonly [string, string])[];
    error?: string;
}) {
    return (
        <fieldset className="profile-food-field">
            <legend>{label}</legend>
            <div className="profile-checkbox-grid profile-food-options">
                {options.map(([value, text]) => (
                    <label key={value}>
                        <input type="checkbox" checked={selected.includes(value)} onChange={(event) => onChange(event.target.checked ? [...selected, value] : selected.filter((item) => item !== value))} />
                        {text}
                    </label>
                ))}
            </div>
            <input type="text" placeholder="Hoặc tự nhập, phân cách bằng dấu phẩy" {...register(customName)} />
            <small>Có thể chọn nhiều loại và tự nhập thêm.</small>
            <FieldError message={error} />
        </fieldset>
    );
}
