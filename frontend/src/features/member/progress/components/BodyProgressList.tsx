import type { MemberBodyProgress } from "../types/memberBodyProgress.types";

function formatDate(value: string) { const [year, month, day] = value.split("-"); return `${day}/${month}/${year}`; }
export function BodyProgressList({ items }: { items: MemberBodyProgress[] }) {
    if (items.length === 0) return <section className="progress-empty" aria-live="polite"><h2>Chưa có bản ghi cân nặng</h2><p>Lưu cân nặng đầu tiên để bắt đầu theo dõi lịch sử cá nhân.</p></section>;
    return <section className="progress-history" aria-labelledby="progressHistoryTitle"><div className="member-section-heading"><p>Lịch sử cá nhân</p><h2 id="progressHistoryTitle">Các bản ghi cân nặng</h2></div><div className="progress-table-wrap"><table><thead><tr><th>Ngày</th><th>Cân nặng</th></tr></thead><tbody>{items.map((item) => <tr key={`${item.id}-${item.recordDate}`}><td>{formatDate(item.recordDate)}</td><td>{item.weightKg.toFixed(2)} kg</td></tr>)}</tbody></table></div></section>;
}
