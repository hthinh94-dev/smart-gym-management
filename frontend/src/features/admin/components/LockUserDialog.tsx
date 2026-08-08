import { useEffect, useState } from "react";
import type { AdminUser } from "../types/adminUser.types";

type LockUserDialogProps = {
    user: AdminUser | null;
    isPending: boolean;
    apiError?: string;
    onClose: () => void;
    onConfirm: (reason: string) => void;
};

const MIN_REASON_LENGTH = 10;
const MAX_REASON_LENGTH = 500;

export function LockUserDialog({ user, isPending, apiError, onClose, onConfirm }: LockUserDialogProps) {
    const [reason, setReason] = useState("");
    const [validationError, setValidationError] = useState("");

    useEffect(() => {
        setReason("");
        setValidationError("");
    }, [user?.id]);

    if (!user) {
        return null;
    }

    function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const normalizedReason = reason.trim();
        if (normalizedReason.length < MIN_REASON_LENGTH || normalizedReason.length > MAX_REASON_LENGTH) {
            setValidationError("Lý do khóa phải có từ 10 đến 500 ký tự.");
            return;
        }
        setValidationError("");
        onConfirm(normalizedReason);
    }

    return (
        <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => {
            if (event.target === event.currentTarget && !isPending) onClose();
        }}>
            <section className="admin-dialog" role="dialog" aria-modal="true" aria-labelledby="lockDialogTitle">
                <header>
                    <div>
                        <p>Thay đổi trạng thái tài khoản</p>
                        <h2 id="lockDialogTitle">Khóa tài khoản</h2>
                    </div>
                    <button className="dialog-close" type="button" aria-label="Đóng dialog" disabled={isPending} onClick={onClose}>×</button>
                </header>

                <div className="dialog-user-summary">
                    <strong>{user.fullName}</strong>
                    <span>{user.email}</span>
                </div>

                <form onSubmit={handleSubmit} noValidate>
                    <label htmlFor="lockReason">Lý do khóa <span>*</span></label>
                    <textarea
                        id="lockReason"
                        value={reason}
                        maxLength={MAX_REASON_LENGTH}
                        disabled={isPending}
                        aria-invalid={Boolean(validationError)}
                        aria-describedby="lockReasonHelp lockReasonError"
                        placeholder="Mô tả vi phạm hoặc lý do vận hành"
                        onChange={(event) => {
                            setReason(event.target.value);
                            setValidationError("");
                        }}
                    />
                    <div className="dialog-field-meta">
                        <small id="lockReasonHelp">Không sử dụng lý do hết hạn gói tập</small>
                        <small>{reason.length}/{MAX_REASON_LENGTH}</small>
                    </div>
                    {(validationError || apiError) && <p id="lockReasonError" className="dialog-error" role="alert">{validationError || apiError}</p>}

                    <div className="dialog-actions">
                        <button className="secondary-button" type="button" disabled={isPending} onClick={onClose}>Hủy</button>
                        <button className="danger-button" type="submit" disabled={isPending}>
                            {isPending ? "Đang khóa" : "Khóa tài khoản"}
                        </button>
                    </div>
                </form>
            </section>
        </div>
    );
}
