import { z } from "zod";

export const PASSWORD_POLICY_TEXT =
    "Từ 8 đến 72 ký tự, có ít nhất 1 chữ hoa, 1 số và không có khoảng trắng ở đầu/cuối";

export const REGISTER_FIELD_LIMITS = {
    fullNameMaxLength: 100,
    emailMaxLength: 150,
    passwordMinLength: 8,
    passwordMaxLength: 72,
} as const;

const passwordPattern = /^(?!\s)(?!.*\s$)(?=.*[A-Z])(?=.*\d).{8,72}$/;

export const registerSchema = z.object({
    fullName: z.string().superRefine((value, context) => {
        const normalized = value.trim();
        if (!normalized) {
            context.addIssue({ code: "custom", message: "Họ và tên là bắt buộc" });
        } else if (normalized.length > REGISTER_FIELD_LIMITS.fullNameMaxLength) {
            context.addIssue({ code: "custom", message: "Họ và tên không được vượt quá 100 ký tự" });
        }
    }),
    email: z.string().superRefine((value, context) => {
        const normalized = normalizeEmail(value);
        if (!normalized) {
            context.addIssue({ code: "custom", message: "Email là bắt buộc" });
        } else if (normalized.length > REGISTER_FIELD_LIMITS.emailMaxLength
            || !z.email().safeParse(normalized).success) {
            context.addIssue({ code: "custom", message: "Email không đúng định dạng" });
        }
    }),
    password: z.string().regex(passwordPattern, "Mật khẩu không đáp ứng yêu cầu bảo mật"),
    confirmPassword: z.string(),
}).superRefine((values, context) => {
    if (!values.confirmPassword || values.confirmPassword !== values.password) {
        context.addIssue({
            code: "custom",
            path: ["confirmPassword"],
            message: "Mật khẩu xác nhận không khớp",
        });
    }
});

export function normalizeEmail(email: string) {
    return email.trim().toLowerCase();
}
