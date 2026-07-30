import { z } from "zod";

export const loginSchema = z.object({
    email: z.string()
        .trim()
        .min(1, "Email là bắt buộc.")
        .max(150, "Email không được vượt quá 150 ký tự.")
        .email("Email không đúng định dạng."),
    password: z.string().min(1, "Mật khẩu là bắt buộc."),
});
