package com.thinh.smartgym.member;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.RoleRepository;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.auth.repository.UserRoleRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.ActivityLevel;
import com.thinh.smartgym.common.enums.ContraindicationTag;
import com.thinh.smartgym.common.enums.DietaryPreference;
import com.thinh.smartgym.common.enums.Equipment;
import com.thinh.smartgym.common.enums.FitnessGoal;
import com.thinh.smartgym.common.enums.FitnessLevel;
import com.thinh.smartgym.common.enums.Gender;
import com.thinh.smartgym.common.enums.MuscleGroup;
import com.thinh.smartgym.member.dto.MemberProfileUpsertRequest;
import com.thinh.smartgym.member.entity.MemberProfile;
import com.thinh.smartgym.member.repository.MemberProfileRepository;
import com.thinh.smartgym.member.service.MemberProfileService;
import com.thinh.smartgym.recommendation.calculator.BiometricCalculationService;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class MemberProfileIntegrationTest {

    @Autowired
    private MemberProfileService memberProfileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private BiometricCalculationService biometricCalculationService;

    private User testUser;

    @AfterEach
    void cleanUp() {
        if (testUser != null && testUser.getId() != null) {
            memberProfileRepository.findByUser_Id(testUser.getId()).ifPresent(memberProfileRepository::delete);
            memberProfileRepository.flush();
            userRepository.deleteById(testUser.getId());
            userRepository.flush();
        }
    }

    @Test
    @DisplayName("PUT Profile rollback khi Calculator lỗi sau khi Profile đã được save")
    void upsertCurrentProfile_WhenCalculationFails_ShouldRollbackDatabaseWrite() {
        testUser = createMember();
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(testUser);
        doThrow(new IllegalStateException("calculator failure")).when(biometricCalculationService)
                .calculate(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> memberProfileService.upsertCurrentProfile(principal, validRequest()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(memberProfileRepository.findByUser_Id(testUser.getId())).isEmpty();
    }

    private User createMember() {
        Long memberRoleId = roleRepository.findByName(com.thinh.smartgym.common.enums.RoleName.ROLE_MEMBER)
                .orElseThrow()
                .getId();
        User user = new User(
                "Rollback Member",
                "rollback-" + UUID.randomUUID() + "@smartgym.test",
                "not-used-in-test",
                AccountStatus.ACTIVE
        );
        User savedUser = userRepository.saveAndFlush(user);
        Role memberRole = entityManager.getReference(Role.class, memberRoleId);
        userRoleRepository.saveAndFlush(new UserRole(savedUser, memberRole));
        return userRepository.findByEmailWithRolesIgnoreCase(savedUser.getEmail()).orElseThrow();
    }

    private MemberProfileUpsertRequest validRequest() {
        return new MemberProfileUpsertRequest(
                Gender.MALE,
                LocalDate.of(1998, 5, 15),
                new BigDecimal("175.00"),
                new BigDecimal("70.00"),
                FitnessGoal.BULK,
                FitnessLevel.BEGINNER,
                ActivityLevel.MODERATELY_ACTIVE,
                4,
                90,
                Set.of(Equipment.BARBELL),
                Set.of(MuscleGroup.CHEST),
                Set.of(ContraindicationTag.LOWER_BACK_LOAD_LIMITED),
                DietaryPreference.OMNIVORE,
                Set.of("PEANUTS"),
                Set.of("BEEF"),
                4
        );
    }
}
