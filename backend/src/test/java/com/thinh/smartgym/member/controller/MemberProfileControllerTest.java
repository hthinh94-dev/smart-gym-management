package com.thinh.smartgym.member.controller;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.ActivityLevel;
import com.thinh.smartgym.common.enums.ContraindicationTag;
import com.thinh.smartgym.common.enums.DietaryPreference;
import com.thinh.smartgym.common.enums.Equipment;
import com.thinh.smartgym.common.enums.FitnessGoal;
import com.thinh.smartgym.common.enums.FitnessLevel;
import com.thinh.smartgym.common.enums.Gender;
import com.thinh.smartgym.common.enums.MuscleGroup;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.member.dto.BioProfileResponse;
import com.thinh.smartgym.member.dto.MemberProfileResponse;
import com.thinh.smartgym.member.dto.NutritionProfileResponse;
import com.thinh.smartgym.member.service.MemberProfileService;
import com.thinh.smartgym.security.AccountStatusAccessDeniedException;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemberProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberProfileService memberProfileService;

    @Test
    @DisplayName("GET member/profile trả read model và không lộ Entity hoặc password")
    void getCurrentProfile_WithMemberPrincipal_ShouldReturnSafeResponse() throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        when(memberProfileService.getCurrentProfile(principal)).thenReturn(response());

        mockMvc.perform(get("/api/v1/member/profile")
                        .with(authentication(authenticationToken(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy hồ sơ thể trạng thành công"))
                .andExpect(jsonPath("$.data.memberId").value(101))
                .andExpect(jsonPath("$.data.bioProfile.gender").value("MALE"))
                .andExpect(jsonPath("$.data.bioProfile.availableEquipment",
                        containsInAnyOrder("BARBELL", "CABLE")))
                .andExpect(jsonPath("$.data.nutritionProfile.dietaryPreference").value("OMNIVORE"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-08-03T03:30:00Z"))
                .andExpect(jsonPath("$.data.calculatedTargets").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Member chưa có Profile nhận 404 PROF-001")
    void getCurrentProfile_WithoutProfile_ShouldReturnProf001() throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        when(memberProfileService.getCurrentProfile(principal))
                .thenThrow(new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/member/profile")
                        .with(authentication(authenticationToken(principal))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PROF-001"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    @DisplayName("Guest gọi Profile nhận ACC-005")
    void getCurrentProfile_WithoutAuthentication_ShouldReturnAcc005() throws Exception {
        mockMvc.perform(get("/api/v1/member/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACC-005"));

        verify(memberProfileService, never()).getCurrentProfile(null);
    }

    @Test
    @DisplayName("Admin gọi Member Profile nhận AUTH-002")
    void getCurrentProfile_WithAdminRole_ShouldReturnAuth002() throws Exception {
        AuthenticatedUserPrincipal admin = principal(RoleName.ROLE_ADMIN);

        mockMvc.perform(get("/api/v1/member/profile")
                        .with(authentication(authenticationToken(admin))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"));

        verify(memberProfileService, never()).getCurrentProfile(admin);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"LOCKED", "DISABLED"})
    @DisplayName("AccountStatusGuard chặn token cũ bằng mã ACC tương ứng")
    void getCurrentProfile_WhenAccountBlocked_ShouldReturnAccountError(AccountStatus status) throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        when(memberProfileService.getCurrentProfile(principal))
                .thenThrow(new AccountStatusAccessDeniedException(status));

        String expectedCode = status == AccountStatus.LOCKED ? "ACC-004" : "ACC-006";
        mockMvc.perform(get("/api/v1/member/profile")
                        .with(authentication(authenticationToken(principal))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(expectedCode))
                .andExpect(jsonPath("$.details.accountStatus").value(status.name()));
    }

    @Test
    @DisplayName("OpenAPI công bố bearerAuth cùng response 200, 401, 403 và 404")
    void openApi_ShouldExposeMemberProfileContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].get.responses['200']"
                        + ".content['application/json'].schema['$ref']",
                        endsWith("/MemberProfileSuccessResponse")))
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].get.responses['404']").exists());
    }

    private UsernamePasswordAuthenticationToken authenticationToken(AuthenticatedUserPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private AuthenticatedUserPrincipal principal(RoleName roleName) {
        User user = new User("Gym Member", "member@smartgym.com", "password-hash", AccountStatus.ACTIVE);
        user.setId(101L);
        user.setCreatedAt(Instant.parse("2026-08-01T03:00:00Z"));
        Role role = new Role(roleName);
        role.setId(roleName == RoleName.ROLE_ADMIN ? 1L : 2L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }

    private MemberProfileResponse response() {
        BioProfileResponse bio = new BioProfileResponse(
                Gender.MALE,
                LocalDate.of(1998, 5, 15),
                new BigDecimal("175.00"),
                new BigDecimal("70.00"),
                FitnessGoal.BULK,
                FitnessLevel.BEGINNER,
                ActivityLevel.MODERATELY_ACTIVE,
                4,
                90,
                Set.of(Equipment.BARBELL, Equipment.CABLE),
                Set.of(MuscleGroup.CHEST, MuscleGroup.BACK),
                Set.of(ContraindicationTag.LOWER_BACK_LOAD_LIMITED)
        );
        NutritionProfileResponse nutrition = new NutritionProfileResponse(
                DietaryPreference.OMNIVORE,
                Set.of("PEANUTS"),
                Set.of("BEEF"),
                4
        );
        return new MemberProfileResponse(
                101L,
                bio,
                nutrition,
                Instant.parse("2026-08-03T03:30:00Z")
        );
    }
}
