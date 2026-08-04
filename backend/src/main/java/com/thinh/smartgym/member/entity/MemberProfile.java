package com.thinh.smartgym.member.entity;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.common.enums.ActivityLevel;
import com.thinh.smartgym.common.enums.ContraindicationTag;
import com.thinh.smartgym.common.enums.DietaryPreference;
import com.thinh.smartgym.common.enums.Equipment;
import com.thinh.smartgym.common.enums.FitnessGoal;
import com.thinh.smartgym.common.enums.FitnessLevel;
import com.thinh.smartgym.common.enums.Gender;
import com.thinh.smartgym.common.enums.MuscleGroup;
import com.thinh.smartgym.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "member_profiles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_member_profiles_user", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_member_profiles_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Positive
    @Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Positive
    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_goal", nullable = false, length = 20)
    private FitnessGoal fitnessGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_level", nullable = false, length = 20)
    private FitnessLevel fitnessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 30)
    private ActivityLevel activityLevel;

    @Min(1)
    @Max(7)
    @Column(name = "workout_days_per_week", nullable = false)
    private Byte workoutDaysPerWeek;

    @Positive
    @Column(name = "max_session_minutes", nullable = false)
    private Short maxSessionMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_preference", nullable = false, length = 20)
    private DietaryPreference dietaryPreference;

    @Min(1)
    @Max(6)
    @Column(name = "meals_per_day", nullable = false)
    private Byte mealsPerDay;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "member_available_equipment",
            joinColumns = @JoinColumn(
                    name = "member_profile_id",
                    foreignKey = @ForeignKey(name = "fk_member_available_equipment_profile")
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment", nullable = false, length = 50)
    private Set<Equipment> availableEquipment = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "member_target_muscle_groups",
            joinColumns = @JoinColumn(
                    name = "member_profile_id",
                    foreignKey = @ForeignKey(name = "fk_member_target_muscle_groups_profile")
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false, length = 50)
    private Set<MuscleGroup> targetMuscleGroups = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "member_injury_constraints",
            joinColumns = @JoinColumn(
                    name = "member_profile_id",
                    foreignKey = @ForeignKey(name = "fk_member_injury_constraints_profile")
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "constraint_tag", nullable = false, length = 80)
    private Set<ContraindicationTag> injuryConstraints = new HashSet<>();

    @Size(max = 10)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "member_food_allergies",
            joinColumns = @JoinColumn(
                    name = "member_profile_id",
                    foreignKey = @ForeignKey(name = "fk_member_food_allergies_profile")
            )
    )
    @Column(name = "allergy_name", nullable = false, length = 50)
    private Set<String> foodAllergies = new HashSet<>();

    @Size(max = 10)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "member_excluded_foods",
            joinColumns = @JoinColumn(
                    name = "member_profile_id",
                    foreignKey = @ForeignKey(name = "fk_member_excluded_foods_profile")
            )
    )
    @Column(name = "food_name", nullable = false, length = 50)
    private Set<String> excludedFoods = new HashSet<>();

    public void setAvailableEquipment(Set<Equipment> values) {
        this.availableEquipment = mutableCopy(values);
    }

    public void setTargetMuscleGroups(Set<MuscleGroup> values) {
        this.targetMuscleGroups = mutableCopy(values);
    }

    public void setInjuryConstraints(Set<ContraindicationTag> values) {
        this.injuryConstraints = mutableCopy(values);
    }

    public void setFoodAllergies(Set<String> values) {
        this.foodAllergies = mutableCopy(values);
    }

    public void setExcludedFoods(Set<String> values) {
        this.excludedFoods = mutableCopy(values);
    }

    public MemberProfile(
            User user,
            Gender gender,
            LocalDate dateOfBirth,
            BigDecimal heightCm,
            BigDecimal weightKg,
            FitnessGoal fitnessGoal,
            FitnessLevel fitnessLevel,
            ActivityLevel activityLevel,
            Byte workoutDaysPerWeek,
            Short maxSessionMinutes,
            DietaryPreference dietaryPreference,
            Byte mealsPerDay
    ) {
        this.user = user;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.fitnessGoal = fitnessGoal;
        this.fitnessLevel = fitnessLevel;
        this.activityLevel = activityLevel;
        this.workoutDaysPerWeek = workoutDaysPerWeek;
        this.maxSessionMinutes = maxSessionMinutes;
        this.dietaryPreference = dietaryPreference;
        this.mealsPerDay = mealsPerDay;
    }

    public void updateFrom(
            Gender gender,
            LocalDate dateOfBirth,
            BigDecimal heightCm,
            BigDecimal weightKg,
            FitnessGoal fitnessGoal,
            FitnessLevel fitnessLevel,
            ActivityLevel activityLevel,
            Byte workoutDaysPerWeek,
            Short maxSessionMinutes,
            DietaryPreference dietaryPreference,
            Byte mealsPerDay,
            Set<Equipment> availableEquipment,
            Set<MuscleGroup> targetMuscleGroups,
            Set<ContraindicationTag> injuryConstraints,
            Set<String> foodAllergies,
            Set<String> excludedFoods
    ) {
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.fitnessGoal = fitnessGoal;
        this.fitnessLevel = fitnessLevel;
        this.activityLevel = activityLevel;
        this.workoutDaysPerWeek = workoutDaysPerWeek;
        this.maxSessionMinutes = maxSessionMinutes;
        this.dietaryPreference = dietaryPreference;
        this.mealsPerDay = mealsPerDay;
        replace(this.availableEquipment, availableEquipment);
        replace(this.targetMuscleGroups, targetMuscleGroups);
        replace(this.injuryConstraints, injuryConstraints);
        replace(this.foodAllergies, foodAllergies);
        replace(this.excludedFoods, excludedFoods);
    }

    private <T> void replace(Set<T> target, Set<T> values) {
        target.clear();
        target.addAll(values);
    }

    private <T> Set<T> mutableCopy(Set<T> values) {
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        MemberProfile other = (MemberProfile) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        Long userId = user == null ? null : user.getId();
        return "MemberProfile{id=" + id + ", userId=" + userId + "}";
    }
}
