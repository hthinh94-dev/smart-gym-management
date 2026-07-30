import { useEffect, useState } from "react";
import type { AccountStatus, RoleName } from "../../auth/types/auth.types";

type UserFiltersProps = {
    search: string;
    role: RoleName | "";
    status: AccountStatus | "";
    disabled?: boolean;
    onSearchChange: (search: string) => void;
    onRoleChange: (role: RoleName | "") => void;
    onStatusChange: (status: AccountStatus | "") => void;
};

export function UserFilters({
    search,
    role,
    status,
    disabled = false,
    onSearchChange,
    onRoleChange,
    onStatusChange,
}: UserFiltersProps) {
    const [searchInput, setSearchInput] = useState(search);

    useEffect(() => {
        setSearchInput(search);
    }, [search]);

    useEffect(() => {
        const timer = window.setTimeout(() => {
            if (searchInput !== search) {
                onSearchChange(searchInput);
            }
        }, 350);
        return () => window.clearTimeout(timer);
    }, [onSearchChange, search, searchInput]);

    return (
        <section className="user-filters" aria-label="Bộ lọc tài khoản">
            <div className="filter-field filter-search">
                <label htmlFor="adminUserSearch">Tìm theo tên hoặc email</label>
                <input
                    id="adminUserSearch"
                    type="search"
                    value={searchInput}
                    placeholder="Nhập tên hoặc email"
                    disabled={disabled}
                    onChange={(event) => setSearchInput(event.target.value)}
                />
            </div>
            <div className="filter-field">
                <label htmlFor="adminRoleFilter">Vai trò</label>
                <select
                    id="adminRoleFilter"
                    value={role}
                    disabled={disabled}
                    onChange={(event) => onRoleChange(event.target.value as RoleName | "")}
                >
                    <option value="">Tất cả vai trò</option>
                    <option value="ROLE_MEMBER">Hội viên</option>
                    <option value="ROLE_PT">Huấn luyện viên</option>
                    <option value="ROLE_ADMIN">Quản trị viên</option>
                </select>
            </div>
            <div className="filter-field">
                <label htmlFor="adminStatusFilter">Trạng thái</label>
                <select
                    id="adminStatusFilter"
                    value={status}
                    disabled={disabled}
                    onChange={(event) => onStatusChange(event.target.value as AccountStatus | "")}
                >
                    <option value="">Tất cả trạng thái</option>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="LOCKED">LOCKED</option>
                    <option value="DISABLED">DISABLED</option>
                </select>
            </div>
        </section>
    );
}
