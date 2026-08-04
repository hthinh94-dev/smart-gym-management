package com.thinh.smartgym.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thinh.smartgym.common.enums.ActivityLevel;
import com.thinh.smartgym.common.enums.ContraindicationTag;
import com.thinh.smartgym.common.enums.DietaryPreference;
import com.thinh.smartgym.common.enums.Equipment;
import com.thinh.smartgym.common.enums.FitnessGoal;
import com.thinh.smartgym.common.enums.FitnessLevel;
import com.thinh.smartgym.common.enums.Gender;
import com.thinh.smartgym.common.enums.MuscleGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Dữ liệu cập nhật toàn bộ hồ sơ hội viên")
public record MemberProfileUpsertRequest(
        @NotNull(message = "Giới tính là bắt buộc.")
        Gender gender,
        @NotNull(message = "Ngày sinh là bắt buộc.")
        @PastOrPresent(message = "Ngày sinh không được ở tương lai.")
        LocalDate dateOfBirth,
        @NotNull(message = "Chiều cao là bắt buộc.")
        @Positive(message = "Chiều cao phải lớn hơn 0.")
        @Digits(integer = 3, fraction = 2, message = "Chiều cao tối đa 3 chữ số nguyên và 2 chữ số thập phân.")
        BigDecimal heightCm,
        @NotNull(message = "Cân nặng là bắt buộc.")
        @Positive(message = "Cân nặng phải lớn hơn 0.")
        @Digits(integer = 4, fraction = 2, message = "Cân nặng tối đa 4 chữ số nguyên và 2 chữ số thập phân.")
        BigDecimal weightKg,
        @NotNull(message = "Mục tiêu là bắt buộc.")
        FitnessGoal fitnessGoal,
        @NotNull(message = "Trình độ là bắt buộc.")
        FitnessLevel fitnessLevel,
        @NotNull(message = "Mức vận động là bắt buộc.")
        ActivityLevel activityLevel,
        @NotNull(message = "Số buổi tập mỗi tuần là bắt buộc.")
        @Min(value = 1, message = "Số buổi tập mỗi tuần phải từ 1 đến 7.")
        @Max(value = 7, message = "Số buổi tập mỗi tuần phải từ 1 đến 7.")
        Integer workoutDaysPerWeek,
        @NotNull(message = "Thời lượng buổi tập là bắt buộc.")
        @Positive(message = "Thời lượng buổi tập phải lớn hơn 0.")
        @Max(value = 32767, message = "Thời lượng buổi tập vượt giới hạn lưu trữ.")
        Integer maxSessionMinutes,
        @NotNull(message = "Thiết bị tập là bắt buộc.")
        Set<@NotNull Equipment> availableEquipment,
        @NotNull(message = "Nhóm cơ mục tiêu là bắt buộc.")
        Set<@NotNull MuscleGroup> targetMuscleGroups,
        @NotNull(message = "Hạn chế vận động là bắt buộc.")
        Set<@NotNull ContraindicationTag> injuryConstraints,
        @NotNull(message = "Chế độ ăn là bắt buộc.")
        DietaryPreference dietaryPreference,
        @NotNull(message = "Danh sách dị ứng là bắt buộc.")
        @Size(max = 10, message = "Danh sách dị ứng tối đa 10 phần tử.")
        Set<@NotNull @Size(max = 50, message = "Tên thực phẩm dị ứng tối đa 50 ký tự.") String> foodAllergies,
        @NotNull(message = "Danh sách thực phẩm loại trừ là bắt buộc.")
        @Size(max = 10, message = "Danh sách thực phẩm loại trừ tối đa 10 phần tử.")
        Set<@NotNull @Size(max = 50, message = "Tên thực phẩm loại trừ tối đa 50 ký tự.") String> excludedFoods,
        @NotNull(message = "Số bữa mỗi ngày là bắt buộc.")
        @Min(value = 1, message = "Số bữa mỗi ngày phải từ 1 đến 6.")
        @Max(value = 6, message = "Số bữa mỗi ngày phải từ 1 đến 6.")
        Integer mealsPerDay
) {
}
