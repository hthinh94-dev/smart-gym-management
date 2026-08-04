import type { CalculatedTargets as CalculatedTargetsData } from "../types/memberProfile.types";

export function CalculatedTargets({ targets }: { targets: CalculatedTargetsData }) {
    const metrics = [
        ["BMI", targets.bmi.toFixed(2), "kg/m²"],
        ["BMR", targets.bmr.toFixed(2), "kcal/ngày"],
        ["TDEE", targets.tdee.toFixed(2), "kcal/ngày"],
        ["Năng lượng mục tiêu", targets.dailyCaloriesKcal.toFixed(2), "kcal/ngày"],
        ["Protein", targets.proteinGrams.toFixed(2), "g/ngày"],
        ["Chất béo", targets.fatGrams.toFixed(2), "g/ngày"],
        ["Carb", targets.carbGrams.toFixed(2), "g/ngày"],
    ];

    return (
        <section className="profile-section calculated-targets" aria-labelledby="calculatedTargetsTitle">
            <header>
                <div>
                    <p>Tính toán theo hồ sơ</p>
                    <h2 id="calculatedTargetsTitle">Chỉ tiêu của bạn</h2>
                </div>
            </header>
            <div className="calculated-target-grid">
                {metrics.map(([label, value, unit]) => (
                    <div className="calculated-target" key={label}>
                        <span>{label}</span>
                        <strong>{value}</strong>
                        <small>{unit}</small>
                    </div>
                ))}
            </div>
        </section>
    );
}
