import type { MemberBodyProgress } from "../types/memberBodyProgress.types";
import { targetDistanceLabel } from "../utils/progressTarget";

export function WeightProgressWidget({ latest, initialWeightKg, targetWeightKg, targetReached = false }: { latest?: MemberBodyProgress; initialWeightKg?: number; targetWeightKg?: number | null; targetReached?: boolean }) {
    const targetDistance = latest
        ? targetReached ? "Đã đạt cân nặng mục tiêu" : targetDistanceLabel(latest.weightKg, targetWeightKg)
        : undefined;
    return <section className="progress-latest" aria-labelledby="latestWeightTitle"><p>Lần ghi nhận gần nhất</p><h2 id="latestWeightTitle">Cân nặng hiện tại</h2><strong>{latest ? `${latest.weightKg.toFixed(2)} kg` : "--"}</strong>{targetDistance && <b className="progress-current-target-distance">{targetDistance}</b>}<span>{latest ? `Ghi nhận ngày ${latest.recordDate}` : "Chưa có dữ liệu cân nặng"}</span>{initialWeightKg != null && <div className="progress-initial-weight"><span>Cân nặng ban đầu</span><strong>{initialWeightKg.toFixed(2)} kg</strong></div>}{targetWeightKg != null && <div className="progress-target-summary"><span>Cân nặng mục tiêu</span><strong>{targetWeightKg.toFixed(2)} kg</strong></div>}</section>;
}
