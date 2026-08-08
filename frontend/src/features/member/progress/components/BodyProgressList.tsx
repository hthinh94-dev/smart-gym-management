import type { MemberBodyProgress } from "../types/memberBodyProgress.types";
import { baselineDifferenceLabel } from "../utils/progressTarget";

function formatDate(value: string) { const [year, month, day] = value.split("-"); return `${day}/${month}/${year}`; }
export function BodyProgressList({ items, initialWeightKg }: { items: MemberBodyProgress[]; initialWeightKg?: number | null }) {
    if (items.length === 0) return <section className="progress-empty" aria-live="polite"><h2>Chưa có bản ghi cân nặng</h2><p>Lưu cân nặng đầu tiên để bắt đầu theo dõi lịch sử cá nhân</p></section>;
    return <section className="progress-history" aria-labelledby="progressHistoryTitle"><div className="member-section-heading"><p>Lịch sử cá nhân</p><h2 id="progressHistoryTitle">Các bản ghi thể trạng</h2></div><div className="progress-table-wrap"><table><thead><tr><th>Ngày</th><th>Cân nặng <small className="progress-initial-header">{initialWeightKg == null ? "(ban đầu chưa có)" : `(ban đầu: ${initialWeightKg.toFixed(2)} kg)`}</small></th><th>Khối lượng cơ</th><th>Khối lượng mỡ</th></tr></thead><tbody>{items.map((item) => <tr key={`${item.id}-${item.recordDate}`}><td>{formatDate(item.recordDate)}</td><td><strong>{item.weightKg.toFixed(2)} kg</strong><small className="progress-baseline-difference">{baselineDifferenceLabel(item.weightKg, initialWeightKg)}</small></td><td>{item.muscleMassKg == null ? "--" : `${item.muscleMassKg.toFixed(2)} kg`}</td><td>{item.fatMassKg == null ? "--" : `${item.fatMassKg.toFixed(2)} kg`}</td></tr>)}</tbody></table></div></section>;
}
