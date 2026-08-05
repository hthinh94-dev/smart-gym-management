import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BodyProgressForm } from "../components/BodyProgressForm";
import { BodyProgressList } from "../components/BodyProgressList";
import { WeightProgressWidget } from "../components/WeightProgressWidget";
import { getMemberBodyProgress, upsertMemberBodyProgress } from "../api/memberBodyProgressApi";
import type { MemberBodyProgress } from "../types/memberBodyProgress.types";

const PROGRESS_QUERY_KEY = ["member-body-progress"] as const;
export function MemberBodyProgressPage() {
    const client = useQueryClient();
    const query = useQuery({ queryKey: PROGRESS_QUERY_KEY, queryFn: getMemberBodyProgress });
    const mutation = useMutation({ mutationFn: upsertMemberBodyProgress, onSuccess: (response) => { client.setQueryData<MemberBodyProgress[]>(PROGRESS_QUERY_KEY, (items = []) => [...items.filter((item) => item.recordDate !== response.recordDate), response].sort((a, b) => a.recordDate.localeCompare(b.recordDate))); } });
    const items = query.data ?? [];
    return <main className="member-page body-progress-page" id="main-content"><header className="member-page-heading"><div><p className="page-eyebrow">Tiến trình thể trạng</p><h1>Theo dõi cân nặng</h1><p>Ghi nhận cân nặng hằng ngày và xem lại lịch sử thay đổi của bạn.</p></div></header>{query.isLoading && <section className="progress-loading" role="status">Đang tải lịch sử cân nặng...</section>}{query.isError && <section className="progress-api-error" role="alert"><strong>Không thể tải lịch sử cân nặng.</strong><button type="button" onClick={() => void query.refetch()}>Thử lại</button></section>}{!query.isLoading && !query.isError && <><div className="progress-layout"><BodyProgressForm isSaving={mutation.isPending} error={mutation.error} onSubmit={(payload) => mutation.mutate(payload)} /><WeightProgressWidget latest={items.at(-1)} /></div><BodyProgressList items={items} /></>}{mutation.isSuccess && <div className="progress-success" role="status">Đã lưu cân nặng thành công.</div>}</main>;
}
