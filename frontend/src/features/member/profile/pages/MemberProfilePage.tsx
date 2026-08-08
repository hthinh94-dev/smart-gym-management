import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getMemberProfile, MemberProfileApiError, updateMemberProfile } from "../api/memberProfileApi";
import { CalculatedTargets } from "../components/CalculatedTargets";
import { ProfileForm } from "../components/ProfileForm";
import { useState } from "react";
import type { MemberProfile } from "../types/memberProfile.types";
import { upsertMemberBodyProgress } from "../../progress/api/memberBodyProgressApi";
import type { BodyProgressUpsertRequest } from "../../progress/types/memberBodyProgress.types";
import { getVietnamBusinessDate } from "../../progress/utils/businessDate";

const PROFILE_QUERY_KEY = ["member-profile"] as const;

const VALUE_LABELS: Record<string, string> = {
    MALE: "Nam",
    FEMALE: "Nữ",
    BULK: "Tăng cơ",
    MUSCLE_GAIN: "Tăng cơ",
    WEIGHT_GAIN: "Tăng cân",
    CUT: "Giảm mỡ",
    FAT_LOSS: "Giảm mỡ",
    WEIGHT_LOSS: "Giảm cân",
    MAINTAIN: "Duy trì",
    BEGINNER: "Mới bắt đầu",
    INTERMEDIATE: "Trung cấp",
    ADVANCED: "Nâng cao",
    SEDENTARY: "Ít vận động",
    LIGHTLY_ACTIVE: "Vận động nhẹ",
    MODERATELY_ACTIVE: "Vận động vừa",
    VERY_ACTIVE: "Vận động nhiều",
    OMNIVORE: "Ăn đa dạng",
    VEGETARIAN: "Ăn chay",
    VEGAN: "Thuần chay",
    BARBELL: "Đòn tạ",
    DUMBBELL: "Tạ đơn",
    MACHINE: "Máy tập",
    CABLE: "Cáp kéo",
    BENCH: "Ghế tập",
    CHEST: "Ngực",
    BACK: "Lưng",
    SHOULDERS: "Vai",
    ARMS: "Tay",
    LEGS: "Chân",
    GLUTES: "Mông",
    CORE: "Cơ trung tâm",
    CARDIO: "Tim mạch",
    FULL_BODY: "Toàn thân",
    KNEE_FLEXION_LIMITED: "Hạn chế gập gối",
    OVERHEAD_MOVEMENT_LIMITED: "Hạn chế động tác qua đầu",
    LOWER_BACK_LOAD_LIMITED: "Hạn chế tải vùng lưng dưới",
    WRIST_FLEXION_LIMITED: "Hạn chế gập cổ tay",
    NECK_LOAD_LIMITED: "Hạn chế tải vùng cổ",
    PEANUTS: "Đậu phộng",
    MILK: "Sữa",
    EGGS: "Trứng",
    SHRIMP: "Tôm",
    BEEF: "Thịt bò",
    CHICKEN: "Thịt gà",
    SEAFOOD: "Hải sản",
    SOY: "Đậu nành",
};

function labelFor(value: string) {
    return VALUE_LABELS[value] ?? value;
}

function formatDate(value: string) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
    }).format(date);
}

function formatDateTime(value: string) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    }).format(date);
}

function ValueList({ values, emptyLabel = "Chưa khai báo" }: { values: string[]; emptyLabel?: string }) {
    if (values.length === 0) {
        return <span className="profile-value-empty">{emptyLabel}</span>;
    }

    return (
        <ul className="profile-value-list">
            {values.map((value) => <li key={value}>{labelFor(value)}</li>)}
        </ul>
    );
}

function ProfileLoadingState() {
    return (
        <section className="profile-state profile-loading-state" role="status" aria-live="polite">
            <div className="profile-loading-heading">
                <span />
                <span />
            </div>
            <div className="profile-loading-grid" aria-hidden="true">
                {Array.from({ length: 8 }, (_, index) => <span key={index} />)}
            </div>
            <p>Đang tải hồ sơ hội viên</p>
        </section>
    );
}

function ProfileEmptyState({ onStart }: { onStart: () => void }) {
    return (
        <section className="profile-state profile-empty-state" aria-labelledby="emptyProfileTitle">
            <p className="profile-state-code">PROF-001</p>
            <h2 id="emptyProfileTitle">Chưa hoàn thiện hồ sơ</h2>
            <p>
                Hồ sơ thể trạng và dinh dưỡng chưa được thiết lập. Smart Gym sẽ dùng thông tin này
                để chuẩn bị các tính toán và đề xuất phù hợp trong bước tiếp theo.
            </p>
            <button type="button" onClick={onStart}>
                Hoàn thiện hồ sơ
            </button>
            <small>Chưa có dữ liệu giả nào được tạo cho tài khoản này</small>
        </section>
    );
}

function ProfileErrorState({ error, onRetry }: { error: unknown; onRetry: () => void }) {
    const profileError = error instanceof MemberProfileApiError ? error : null;
    const isSessionBlocked = profileError
        ? ["ACC-004", "ACC-005", "ACC-006"].includes(profileError.errorCode)
        : false;

    return (
        <section className="profile-state profile-error-state" role="alert">
            <p className="profile-state-code">{profileError?.errorCode ?? "SYS-001"}</p>
            <h2>{isSessionBlocked ? "Phiên truy cập không còn hiệu lực" : "Không tải được hồ sơ"}</h2>
            <p>
                {profileError?.message
                    ?? "Hệ thống chưa thể xử lý yêu cầu hồ sơ. Vui lòng thử lại."}
            </p>
            {!isSessionBlocked && (
                <button type="button" onClick={onRetry}>Thử lại</button>
            )}
        </section>
    );
}

function ProfileOverview({ profile, onEdit }: { profile: MemberProfile; onEdit: () => void }) {
    const { bioProfile, nutritionProfile } = profile;

    return (
        <div className="profile-overview">
            <section className="profile-section" aria-labelledby="bioProfileTitle">
                <header>
                    <div>
                        <p>Thông tin nền</p>
                        <h2 id="bioProfileTitle">Thể trạng và mục tiêu</h2>
                    </div>
                    <div className="profile-section-header-actions"><span>Cập nhật {formatDateTime(profile.updatedAt)}</span><button type="button" className="profile-edit-button" onClick={onEdit}>Chỉnh sửa</button></div>
                </header>

                <dl className="profile-data-grid">
                    <div><dt>Giới tính sinh học</dt><dd>{labelFor(bioProfile.gender)}</dd></div>
                    <div><dt>Ngày sinh</dt><dd>{formatDate(bioProfile.dateOfBirth)}</dd></div>
                    <div><dt>Chiều cao</dt><dd>{bioProfile.heightCm} cm</dd></div>
                    <div><dt>Cân nặng</dt><dd>{bioProfile.weightKg} kg</dd></div>
                    <div><dt>Mục tiêu</dt><dd><ValueList values={bioProfile.fitnessGoals ?? [bioProfile.fitnessGoal]} /></dd></div>
                    <div><dt>Cân nặng mục tiêu</dt><dd>{bioProfile.targetWeightKg ? `${bioProfile.targetWeightKg} kg` : "Chưa đặt"}</dd></div>
                    <div><dt>Trình độ</dt><dd>{labelFor(bioProfile.fitnessLevel)}</dd></div>
                    <div><dt>Mức vận động</dt><dd>{labelFor(bioProfile.activityLevel)}</dd></div>
                    <div><dt>Lịch tập</dt><dd>{bioProfile.workoutDaysPerWeek} buổi / tuần</dd></div>
                    <div><dt>Thời lượng tối đa</dt><dd>{bioProfile.maxSessionMinutes} phút / buổi</dd></div>
                </dl>

                <div className="profile-collection-grid">
                    <div>
                        <h3>Thiết bị có thể sử dụng</h3>
                        <ValueList values={bioProfile.availableEquipment} />
                    </div>
                    <div>
                        <h3>Nhóm cơ ưu tiên</h3>
                        <ValueList values={bioProfile.targetMuscleGroups} />
                    </div>
                    <div>
                        <h3>Hạn chế vận động</h3>
                        <ValueList values={bioProfile.injuryConstraints} emptyLabel="Không có hạn chế" />
                        {bioProfile.mobilityLimitNotes && <p className="profile-custom-value">Khác: {bioProfile.mobilityLimitNotes}</p>}
                    </div>
                </div>
            </section>

            <CalculatedTargets targets={profile.calculatedTargets} goals={bioProfile.fitnessGoals ?? [bioProfile.fitnessGoal]} targetWeightKg={bioProfile.targetWeightKg} />

            <section className="profile-section" aria-labelledby="nutritionProfileTitle">
                <header>
                    <div>
                        <p>Dinh dưỡng</p>
                        <h2 id="nutritionProfileTitle">Thói quen ăn uống</h2>
                    </div>
                </header>

                <dl className="profile-data-grid profile-nutrition-summary">
                    <div><dt>Chế độ ăn</dt><dd>{labelFor(nutritionProfile.dietaryPreference)}</dd></div>
                    <div><dt>Số bữa mỗi ngày</dt><dd>{nutritionProfile.mealsPerDay} bữa</dd></div>
                </dl>

                <div className="profile-collection-grid profile-nutrition-collections">
                    <div>
                        <h3>Thực phẩm gây dị ứng</h3>
                        <ValueList values={nutritionProfile.foodAllergies} emptyLabel="Không khai báo dị ứng" />
                    </div>
                    <div>
                        <h3>Thực phẩm loại trừ</h3>
                        <ValueList values={nutritionProfile.excludedFoods} emptyLabel="Không có thực phẩm loại trừ" />
                    </div>
                </div>
            </section>
        </div>
    );
}

export function MemberProfilePage() {
    const queryClient = useQueryClient();
    const [isEditing, setIsEditing] = useState(false);
    const [saveMessage, setSaveMessage] = useState<string>();
    const [pendingProgressRequest, setPendingProgressRequest] = useState<BodyProgressUpsertRequest>();
    const profileQuery = useQuery({
        queryKey: PROFILE_QUERY_KEY,
        queryFn: getMemberProfile,
    });
    const progressMutation = useMutation({
        mutationFn: upsertMemberBodyProgress,
        onSuccess: () => {
            setPendingProgressRequest(undefined);
            void queryClient.invalidateQueries({ queryKey: ["member-body-progress"] });
        },
    });
    const saveMutation = useMutation({
        mutationFn: updateMemberProfile,
        onSuccess: (response) => {
            queryClient.setQueryData(PROFILE_QUERY_KEY, response);
            setSaveMessage("Đã lưu hồ sơ");
            setIsEditing(false);
            const progressRequest = {
                recordDate: getVietnamBusinessDate(),
                weightKg: response.data.bioProfile.weightKg,
            };
            setPendingProgressRequest(progressRequest);
            progressMutation.reset();
            progressMutation.mutate(progressRequest);
        },
    });

    const isProfileEmpty = profileQuery.error instanceof MemberProfileApiError
        && profileQuery.error.errorCode === "PROF-001";

    return (
        <main className="member-page profile-page" id="main-content">
            <header className="member-page-heading">
                <div>
                    <p className="page-eyebrow">Hồ sơ hội viên</p>
                    <h1>Thông tin thể trạng</h1>
                    <p>Dữ liệu nền được dùng cho các tính toán và đề xuất tập luyện của Smart Gym</p>
                </div>
                {!isEditing && profileQuery.data && <span className="profile-readonly-label">Hồ sơ của bạn</span>}
            </header>

            {saveMessage && (
                <div className="profile-success-notice" role="status">
                    <span>{saveMessage}</span>
                    {progressMutation.isPending && <span>Đang ghi nhận cân nặng hôm nay</span>}
                    {progressMutation.isSuccess && <span>Đã ghi nhận cân nặng hôm nay</span>}
                    {progressMutation.isError && (
                        <span>
                            Hồ sơ đã lưu nhưng chưa ghi nhận được cân nặng. {progressMutation.error.message}
                            <button
                                type="button"
                                onClick={() => pendingProgressRequest && progressMutation.mutate(pendingProgressRequest)}
                            >
                                Thử ghi cân nặng lại
                            </button>
                        </span>
                    )}
                </div>
            )}
            {isEditing && <ProfileForm profile={profileQuery.data?.data} isSaving={saveMutation.isPending} error={saveMutation.error} onCancel={() => { saveMutation.reset(); setIsEditing(false); }} onSubmit={(request) => { setSaveMessage(undefined); setPendingProgressRequest(undefined); progressMutation.reset(); saveMutation.mutate(request); }} />}
            {profileQuery.isLoading && <ProfileLoadingState />}
            {isProfileEmpty && !isEditing && <ProfileEmptyState onStart={() => setIsEditing(true)} />}
            {profileQuery.isError && !isProfileEmpty && (
                <ProfileErrorState error={profileQuery.error} onRetry={() => void profileQuery.refetch()} />
            )}
            {profileQuery.data && !isEditing && <ProfileOverview profile={profileQuery.data.data} onEdit={() => { setSaveMessage(undefined); setIsEditing(true); }} />}
        </main>
    );
}
