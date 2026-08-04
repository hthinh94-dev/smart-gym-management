package com.thinh.smartgym.recommendation.calculator;

import com.thinh.smartgym.common.enums.ActivityLevel;
import com.thinh.smartgym.common.enums.FitnessGoal;
import com.thinh.smartgym.common.enums.Gender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BiometricCalculationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-04T00:00:00Z"),
            ZoneOffset.UTC
    );

    private final TimeZone originalTimeZone = TimeZone.getDefault();
    private final BiometricCalculationService calculator = new BiometricCalculationService(FIXED_CLOCK);

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("BMI dùng chiều cao theo mét và toàn bộ macro làm tròn 2 chữ số")
    void calculate_WithBulkMaleSample_ShouldReturnDeterministicRoundedTargets() {
        CalculatedTargets result = calculate(
                Gender.MALE,
                LocalDate.of(1998, 5, 15),
                ActivityLevel.MODERATELY_ACTIVE,
                FitnessGoal.BULK
        );

        assertThat(result.bmi()).isEqualByComparingTo("23.67");
        assertThat(result.bmr()).isEqualByComparingTo("1683.75");
        assertThat(result.tdee()).isEqualByComparingTo("2609.81");
        assertThat(result.dailyCaloriesKcal()).isEqualByComparingTo("2909.81");
        assertThat(result.proteinGrams()).isEqualByComparingTo("159.50");
        assertThat(result.fatGrams()).isEqualByComparingTo("80.83");
        assertThat(result.carbGrams()).isEqualByComparingTo("386.09");
    }

    @Test
    @DisplayName("BMR nữ dùng nhánh Mifflin-St Jeor -161")
    void calculate_WithFemale_ShouldUseFemaleBmrConstant() {
        CalculatedTargets result = calculate(
                Gender.FEMALE,
                LocalDate.of(1998, 5, 15),
                ActivityLevel.SEDENTARY,
                FitnessGoal.MAINTAIN
        );

        assertThat(result.bmr()).isEqualByComparingTo("1517.75");
    }

    @ParameterizedTest(name = "dateOfBirth={0} -> bmr={1}")
    @MethodSource("birthdayCases")
    @DisplayName("Tuổi đúng trước, trong và sau ngày sinh nhật")
    void calculate_ShouldUseCompletedYears(LocalDate dateOfBirth, String expectedBmr) {
        CalculatedTargets result = calculate(
                Gender.MALE,
                dateOfBirth,
                ActivityLevel.SEDENTARY,
                FitnessGoal.MAINTAIN
        );

        assertThat(result.bmr()).isEqualByComparingTo(expectedBmr);
    }

    static Stream<Arguments> birthdayCases() {
        return Stream.of(
                Arguments.of(LocalDate.of(2000, 8, 5), "1698.75"),
                Arguments.of(LocalDate.of(2000, 8, 4), "1693.75"),
                Arguments.of(LocalDate.of(2000, 8, 3), "1693.75")
        );
    }

    @ParameterizedTest(name = "activity={0} -> tdee={1}")
    @MethodSource("activityCases")
    @DisplayName("Áp dụng chính xác hệ số của bốn mức hoạt động")
    void calculate_ShouldSupportEveryActivityLevel(ActivityLevel activityLevel, String expectedTdee) {
        CalculatedTargets result = calculate(
                Gender.MALE,
                LocalDate.of(1998, 5, 15),
                activityLevel,
                FitnessGoal.MAINTAIN
        );

        assertThat(result.tdee()).isEqualByComparingTo(expectedTdee);
    }

    static Stream<Arguments> activityCases() {
        return Stream.of(
                Arguments.of(ActivityLevel.SEDENTARY, "2020.50"),
                Arguments.of(ActivityLevel.LIGHTLY_ACTIVE, "2315.16"),
                Arguments.of(ActivityLevel.MODERATELY_ACTIVE, "2609.81"),
                Arguments.of(ActivityLevel.VERY_ACTIVE, "2904.47")
        );
    }

    @ParameterizedTest(name = "goal={0} -> calories={1}")
    @MethodSource("goalCases")
    @DisplayName("Calories áp dụng đúng BULK, CUT và MAINTAIN")
    void calculate_ShouldApplyGoalAdjustment(FitnessGoal goal, String expectedCalories) {
        CalculatedTargets result = calculate(
                Gender.MALE,
                LocalDate.of(1998, 5, 15),
                ActivityLevel.MODERATELY_ACTIVE,
                goal
        );

        assertThat(result.dailyCaloriesKcal()).isEqualByComparingTo(expectedCalories);
        assertThat(result.carbGrams()).isNotNegative();
    }

    static Stream<Arguments> goalCases() {
        return Stream.of(
                Arguments.of(FitnessGoal.BULK, "2909.81"),
                Arguments.of(FitnessGoal.CUT, "2109.81"),
                Arguments.of(FitnessGoal.MAINTAIN, "2609.81")
        );
    }

    @Test
    @DisplayName("Kết quả không phụ thuộc timezone mặc định của máy")
    void calculate_WhenJvmDefaultTimeZoneChanges_ShouldRemainStable() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
        CalculatedTargets first = calculate(
                Gender.MALE,
                LocalDate.of(2000, 8, 4),
                ActivityLevel.SEDENTARY,
                FitnessGoal.MAINTAIN
        );

        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        CalculatedTargets second = calculate(
                Gender.MALE,
                LocalDate.of(2000, 8, 4),
                ActivityLevel.SEDENTARY,
                FitnessGoal.MAINTAIN
        );

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("Ngày tính tuổi theo timezone nghiệp vụ Việt Nam")
    void calculate_NearUtcMidnight_ShouldUseVietnamBusinessDate() {
        Clock nearUtcMidnight = Clock.fixed(
                Instant.parse("2026-08-04T17:30:00Z"),
                ZoneOffset.UTC
        );
        BiometricCalculationService businessDateCalculator =
                new BiometricCalculationService(nearUtcMidnight);

        CalculatedTargets result = businessDateCalculator.calculate(
                Gender.MALE,
                LocalDate.of(2000, 8, 5),
                new BigDecimal("175.00"),
                new BigDecimal("72.50"),
                ActivityLevel.SEDENTARY,
                FitnessGoal.MAINTAIN
        );

        assertThat(result.bmr()).isEqualByComparingTo("1693.75");
    }

    private CalculatedTargets calculate(
            Gender gender,
            LocalDate dateOfBirth,
            ActivityLevel activityLevel,
            FitnessGoal fitnessGoal
    ) {
        return calculator.calculate(
                gender,
                dateOfBirth,
                new BigDecimal("175.00"),
                new BigDecimal("72.50"),
                activityLevel,
                fitnessGoal
        );
    }
}
