package com.thinh.smartgym.member.entity;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.common.enums.AccountStatus;
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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MemberProfileTest {

    @Test
    @DisplayName("MemberProfile ánh xạ bảng, ownership và one-to-one đúng V2")
    void entity_ShouldMapProfileOwnershipWithoutReverseUserRelation() throws Exception {
        Table table = MemberProfile.class.getAnnotation(Table.class);
        Field userField = MemberProfile.class.getDeclaredField("user");
        OneToOne oneToOne = userField.getAnnotation(OneToOne.class);
        JoinColumn joinColumn = userField.getAnnotation(JoinColumn.class);

        assertThat(BaseEntity.class).isAssignableFrom(MemberProfile.class);
        assertThat(table.name()).isEqualTo("member_profiles");
        assertThat(table.uniqueConstraints()).singleElement().satisfies(constraint -> {
            assertThat(constraint.name()).isEqualTo("uk_member_profiles_user");
            assertThat(constraint.columnNames()).containsExactly("user_id");
        });
        assertThat(oneToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(oneToOne.cascade()).isEmpty();
        assertThat(joinColumn.name()).isEqualTo("user_id");
        assertThat(joinColumn.unique()).isTrue();
        assertThat(joinColumn.foreignKey().name()).isEqualTo("fk_member_profiles_user");
        assertThat(Arrays.stream(User.class.getDeclaredFields()))
                .noneMatch(field -> field.getType().equals(MemberProfile.class));
    }

    @Test
    @DisplayName("Năm collection ánh xạ đúng table, column và enum STRING")
    void entity_ShouldMapAllFiveCollections() throws Exception {
        assertCollection("availableEquipment", "member_available_equipment", "equipment",
                "fk_member_available_equipment_profile", true);
        assertCollection("targetMuscleGroups", "member_target_muscle_groups", "muscle_group",
                "fk_member_target_muscle_groups_profile", true);
        assertCollection("injuryConstraints", "member_injury_constraints", "constraint_tag",
                "fk_member_injury_constraints_profile", true);
        assertCollection("foodAllergies", "member_food_allergies", "allergy_name",
                "fk_member_food_allergies_profile", false);
        assertCollection("excludedFoods", "member_excluded_foods", "food_name",
                "fk_member_excluded_foods_profile", false);
    }

    @Test
    @DisplayName("Enum Profile khớp CHECK constraint sau khi mở rộng mục tiêu")
    void profileEnums_ShouldMatchMigrationValues() {
        assertThat(Gender.values()).containsExactly(Gender.MALE, Gender.FEMALE);
        assertThat(FitnessGoal.values()).containsExactly(
                FitnessGoal.BULK,
                FitnessGoal.CUT,
                FitnessGoal.MAINTAIN,
                FitnessGoal.MUSCLE_GAIN,
                FitnessGoal.WEIGHT_GAIN,
                FitnessGoal.FAT_LOSS,
                FitnessGoal.WEIGHT_LOSS);
        assertThat(FitnessLevel.values()).containsExactly(
                FitnessLevel.BEGINNER, FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED);
        assertThat(ActivityLevel.values()).containsExactly(
                ActivityLevel.SEDENTARY,
                ActivityLevel.LIGHTLY_ACTIVE,
                ActivityLevel.MODERATELY_ACTIVE,
                ActivityLevel.VERY_ACTIVE);
        assertThat(DietaryPreference.values()).containsExactly(
                DietaryPreference.OMNIVORE,
                DietaryPreference.VEGETARIAN,
                DietaryPreference.VEGAN);
        assertThat(Equipment.values()).containsExactly(
                Equipment.BARBELL,
                Equipment.DUMBBELL,
                Equipment.MACHINE,
                Equipment.CABLE,
                Equipment.BENCH);
        assertThat(MuscleGroup.values()).containsExactly(
                MuscleGroup.CHEST,
                MuscleGroup.BACK,
                MuscleGroup.SHOULDERS,
                MuscleGroup.ARMS,
                MuscleGroup.LEGS,
                MuscleGroup.GLUTES,
                MuscleGroup.CORE,
                MuscleGroup.CARDIO,
                MuscleGroup.FULL_BODY);
        assertThat(ContraindicationTag.values()).containsExactly(
                ContraindicationTag.KNEE_FLEXION_LIMITED,
                ContraindicationTag.OVERHEAD_MOVEMENT_LIMITED,
                ContraindicationTag.LOWER_BACK_LOAD_LIMITED,
                ContraindicationTag.WRIST_FLEXION_LIMITED,
                ContraindicationTag.NECK_LOAD_LIMITED);
    }

    @Test
    @DisplayName("toString không làm lộ dữ liệu thể trạng hoặc dinh dưỡng")
    void toString_ShouldOnlyExposePersistenceIdentity() {
        User user = new User("Gym Member", "member@smartgym.com", "secret-hash", AccountStatus.ACTIVE);
        user.setId(101L);
        MemberProfile profile = profile(user);
        profile.setId(501L);
        profile.setFoodAllergies(Set.of("PEANUTS"));
        profile.setExcludedFoods(Set.of("BEEF"));

        assertThat(profile.toString())
                .isEqualTo("MemberProfile{id=501, userId=101}")
                .doesNotContain("175", "70", "PEANUTS", "BEEF", "secret-hash");
    }

    private void assertCollection(
            String fieldName,
            String tableName,
            String columnName,
            String foreignKeyName,
            boolean enumCollection
    ) throws Exception {
        Field field = MemberProfile.class.getDeclaredField(fieldName);
        ElementCollection elementCollection = field.getAnnotation(ElementCollection.class);
        CollectionTable collectionTable = field.getAnnotation(CollectionTable.class);
        Column column = field.getAnnotation(Column.class);

        assertThat(Set.class).isAssignableFrom(field.getType());
        assertThat(elementCollection.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(collectionTable.name()).isEqualTo(tableName);
        assertThat(collectionTable.joinColumns()).singleElement().satisfies(joinColumn -> {
            assertThat(joinColumn.name()).isEqualTo("member_profile_id");
            assertThat(joinColumn.foreignKey().name()).isEqualTo(foreignKeyName);
        });
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isFalse();
        if (enumCollection) {
            assertThat(field.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        } else {
            assertThat(field.getAnnotation(Enumerated.class)).isNull();
        }
    }

    private MemberProfile profile(User user) {
        return new MemberProfile(
                user,
                Gender.MALE,
                LocalDate.of(1998, 5, 15),
                new BigDecimal("175.00"),
                new BigDecimal("70.00"),
                FitnessGoal.BULK,
                FitnessLevel.BEGINNER,
                ActivityLevel.MODERATELY_ACTIVE,
                (byte) 4,
                (short) 90,
                DietaryPreference.OMNIVORE,
                (byte) 4
        );
    }
}
