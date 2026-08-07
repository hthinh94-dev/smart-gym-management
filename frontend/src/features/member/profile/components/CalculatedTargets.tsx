import type { CalculatedTargets as CalculatedTargetsData } from "../types/memberProfile.types";

import type { FitnessGoal } from "../types/memberProfile.types";

const GOAL_LABELS: Record<FitnessGoal, string> = {
    BULK: "tăng cơ",
    MUSCLE_GAIN: "tăng cơ",
    WEIGHT_GAIN: "tăng cân",
    CUT: "giảm mỡ",
    FAT_LOSS: "giảm mỡ",
    WEIGHT_LOSS: "giảm cân",
    MAINTAIN: "duy trì cân nặng",
};

const BMI_LABELS = {
    UNDERWEIGHT: "Thiếu cân",
    NORMAL: "Bình thường",
    OVERWEIGHT: "Thừa cân",
    OBESE: "Béo phì",
} as const;

export function CalculatedTargets({ targets, goals = [], targetWeightKg }: { targets: CalculatedTargetsData; goals?: FitnessGoal[]; targetWeightKg?: number | null }) {
    const goalText = goals.length === 0 ? "mục tiêu của bạn" : goals.map((goal) => GOAL_LABELS[goal]).filter((value, index, values) => values.indexOf(value) === index).join(" và ");
    const bmiCategory = targets.bmiCategory ? BMI_LABELS[targets.bmiCategory] : "Chưa phân loại";
    const metrics = [
        ["BMI", targets.bmi.toFixed(2), "kg/m²", bmiCategory],
        ["BMR", targets.bmr.toFixed(2), "kcal/ngày", "(Lượng calo tối thiểu cần nạp vào)"],
        ["TDEE", targets.tdee.toFixed(2), "kcal/ngày", "(Lượng calo duy trì cân nặng)"],
    ];
    const targetsList = [
        ["Lượng calo cần để " + goalText, targets.dailyCaloriesKcal.toFixed(2), "kcal/ngày"],
        ["Protein cần nạp", targets.proteinGrams.toFixed(2), "g/ngày"],
        ["Chất béo cần nạp", targets.fatGrams.toFixed(2), "g/ngày"],
        ["Carb cần nạp", targets.carbGrams.toFixed(2), "g/ngày"],
    ];

    return (
        <div className="calculated-targets">
            <section className="profile-section calculated-results" aria-labelledby="calculatedResultsTitle">
                <header>
                    <div>
                        <p>Kết quả phân tích</p>
                        <h2 id="calculatedResultsTitle">Kết quả các chỉ số</h2>
                    </div>
                </header>
                <div className="calculated-target-grid calculated-metric-grid">
                    {metrics.map(([label, value, unit, note]) => (
                        <div className="calculated-target" key={label}>
                            <span>{label}</span>
                            <strong>{value}</strong>
                            <small>{unit}</small>
                            <em>{note}</em>
                        </div>
                    ))}
                </div>
                {targetWeightKg && <p className="target-weight-note">Cân nặng mục tiêu: <strong>{targetWeightKg.toFixed(2)} kg</strong></p>}
            </section>
            <section className="profile-section calculated-target-section" aria-labelledby="calculatedTargetsTitle">
            <header>
                <div>
                    <p>Định hướng dinh dưỡng</p>
                    <h2 id="calculatedTargetsTitle">Chỉ tiêu cần {goalText}</h2>
                </div>
            </header>
            <div className="calculated-target-grid">
                {targetsList.map(([label, value, unit]) => (
                    <div className="calculated-target" key={label}>
                        <span>{label}</span>
                        <strong>{value}</strong>
                        <small>{unit}</small>
                    </div>
                ))}
            </div>
            </section>
        </div>
    );
}
