import type { AdminUser } from "../types/adminUser.types";

type UnlockUserDialogProps = {
    user: AdminUser | null;
    isPending: boolean;
    apiError?: string;
    onClose: () => void;
    onConfirm: () => void;
};

export function UnlockUserDialog({ user, isPending, apiError, onClose, onConfirm }: UnlockUserDialogProps) {
    if (!user) {
        return null;
    }

    return (
        <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => {
            if (event.target === event.currentTarget && !isPending) onClose();
        }}>
            <section className="admin-dialog admin-dialog-confirm" role="dialog" aria-modal="true" aria-labelledby="unlockDialogTitle">
                <header>
                    <div>
                        <p>Thay đổi trạng thái tài khoản</p>
                        <h2 id="unlockDialogTitle">Mở khóa tài khoản</h2>
                    </div>
                    <button className="dialog-close" type="button" aria-label="Đóng dialog" disabled={isPending} onClick={onClose}>×</button>
                </header>

                <p className="dialog-confirm-copy">
                    Xác nhận mở khóa <strong>{user.fullName}</strong>. Người dùng sẽ có thể đăng nhập và sử dụng lại hệ thống.
                </p>
                {apiError && <p className="dialog-error" role="alert">{apiError}</p>}

                <div className="dialog-actions">
                    <button className="secondary-button" type="button" disabled={isPending} onClick={onClose}>Hủy</button>
                    <button className="success-button" type="button" disabled={isPending} onClick={onConfirm}>
                        {isPending ? "Đang mở khóa..." : "Mở khóa tài khoản"}
                    </button>
                </div>
            </section>
        </div>
    );
}
