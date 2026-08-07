import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BodyProgressForm } from "../components/BodyProgressForm";
import { BodyProgressList } from "../components/BodyProgressList";
import { WeightProgressWidget } from "../components/WeightProgressWidget";
import { getMemberBodyProgress, upsertMemberBodyProgress } from "../api/memberBodyProgressApi";
import { getMemberProfile } from "../../profile/api/memberProfileApi";
import type { FitnessGoal } from "../../profile/types/memberProfile.types";
import type { MemberBodyProgress } from "../types/memberBodyProgress.types";
import { useState } from "react";
import { baselineDifferenceLabel, hasReachedTarget } from "../utils/progressTarget";

const PROGRESS_QUERY_KEY = ["member-body-progress"] as const;
export function MemberBodyProgressPage() {
    const client = useQueryClient();
    const query = useQuery({ queryKey: PROGRESS_QUERY_KEY, queryFn: getMemberBodyProgress });
    const profileQuery = useQuery({ queryKey: ["member-profile"], queryFn: getMemberProfile });
    const [feedback, setFeedback] = useState<string>();
    const [celebrated, setCelebrated] = useState(false);
    const profile = profileQuery.data?.data;
    const targetWeightKg = profile?.bioProfile.targetWeightKg;
    const goals = profile?.bioProfile.fitnessGoals ?? (profile?.bioProfile.fitnessGoal ? [profile.bioProfile.fitnessGoal] : []);
    const targetReached = (weight: number) => hasReachedTarget(weight, targetWeightKg, goals);
    const mutation = useMutation({ mutationFn: upsertMemberBodyProgress, onSuccess: (response) => {
        const existingItems = client.getQueryData<MemberBodyProgress[]>(PROGRESS_QUERY_KEY) ?? [];
        const nextItems = [...existingItems.filter((item) => item.recordDate !== response.recordDate), response].sort((a, b) => a.recordDate.localeCompare(b.recordDate));
        const initialWeightKg = nextItems[0]?.weightKg ?? profile?.bioProfile.weightKg;
        client.setQueryData<MemberBodyProgress[]>(PROGRESS_QUERY_KEY, nextItems);
        const baselineDifference = baselineDifferenceLabel(response.weightKg, initialWeightKg);
        setFeedback(baselineDifference
            ? `Đã lưu cân nặng thành công. ${baselineDifference}.`
            : "Đã lưu cân nặng thành công. Chưa có cân nặng ban đầu để so sánh.");
        if (targetReached(response.weightKg)) setCelebrated(true);
    } });
    const items = query.data ?? [];
    const initialWeightKg = items[0]?.weightKg ?? profile?.bioProfile.weightKg;
    return <main className="member-page body-progress-page" id="main-content"><header className="member-page-heading"><div><p className="page-eyebrow">Tiến trình thể trạng</p><h1>Theo dõi cân nặng</h1><p>Ghi nhận cân nặng hằng ngày và xem lại lịch sử thay đổi của bạn.</p></div></header>{query.isLoading && <section className="progress-loading" role="status">Đang tải lịch sử thể trạng...</section>}{query.isError && <section className="progress-api-error" role="alert"><strong>Không thể tải lịch sử cân nặng.</strong><button type="button" onClick={() => void query.refetch()}>Thử lại</button></section>}{!query.isLoading && !query.isError && <><div className="progress-layout"><BodyProgressForm isSaving={mutation.isPending} error={mutation.error} onSubmit={(payload) => mutation.mutate(payload)} /><WeightProgressWidget latest={items.at(-1)} initialWeightKg={initialWeightKg} targetWeightKg={targetWeightKg} targetReached={targetReached(items.at(-1)?.weightKg ?? 0)} /></div>{feedback && <div className="progress-feedback" role="status">{feedback}</div>}{celebrated && <div className="progress-celebration" aria-live="polite"><span aria-hidden="true">✦ ✧ ✦</span><strong>Chúc mừng bạn!</strong><p>Bạn đã đạt cân nặng mục tiêu. Hãy tiếp tục duy trì thói quen tốt.</p><span aria-hidden="true">✧ ✦ ✧</span></div>}<BodyProgressList items={items} initialWeightKg={initialWeightKg} /></>}</main>;
}
