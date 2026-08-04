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
import com.thinh.smartgym.member.dto.CalculatedTargetsResponse;
import com.thinh.smartgym.member.dto.MemberProfileResponse;
import com.thinh.smartgym.member.dto.NutritionProfileResponse;
import com.thinh.smartgym.member.service.MemberProfileService;
import com.thinh.smartgym.security.AccountStatusAccessDeniedException;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(jsonPath("$.data.calculatedTargets.bmi").value(23.67))
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

    @Test
    @DisplayName("PUT member/profile trả Profile cùng calculated targets")
    void upsertCurrentProfile_WithValidRequest_ShouldReturnCalculatedTargets() throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        when(memberProfileService.upsertCurrentProfile(
                org.mockito.ArgumentMatchers.eq(principal),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(response());

        mockMvc.perform(put("/api/v1/member/profile")
                        .with(authentication(authenticationToken(principal)))
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật hồ sơ thể trạng thành công"))
                .andExpect(jsonPath("$.data.memberId").value(101))
                .andExpect(jsonPath("$.data.calculatedTargets.bmr").value(1683.75))
                .andExpect(jsonPath("$.data.calculatedTargets.carbGrams").value(386.09));
    }

    @Test
    @DisplayName("PUT vi phạm BR-23 trả VAL-001 và không gọi Service")
    void upsertCurrentProfile_WithInvalidBoundaries_ShouldReturnVal001() throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        String invalid = validRequest()
                .replace("\"workoutDaysPerWeek\": 4", "\"workoutDaysPerWeek\": 8")
                .replace("\"mealsPerDay\": 4", "\"mealsPerDay\": 0");

        mockMvc.perform(put("/api/v1/member/profile")
                        .with(authentication(authenticationToken(principal)))
                        .contentType("application/json")
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations.workoutDaysPerWeek").exists())
                .andExpect(jsonPath("$.details.violations.mealsPerDay").exists());

        verify(memberProfileService, never()).upsertCurrentProfile(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @ParameterizedTest(name = "field={2}")
    @MethodSource("scalarBoundaryCases")
    @DisplayName("PUT kiểm tra đầy đủ boundary scalar của BR-23")
    void upsertCurrentProfile_WithInvalidScalarBoundary_ShouldReturnVal001(
            String source,
            String replacement,
            String expectedField
    ) throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        String invalid = validRequest().replace(source, replacement);

        mockMvc.perform(put("/api/v1/member/profile")
                        .with(authentication(authenticationToken(principal)))
                        .contentType("application/json")
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations." + expectedField).exists());
    }

    static Stream<Arguments> scalarBoundaryCases() {
        return Stream.of(
                Arguments.of("\"dateOfBirth\": \"1998-05-15\"", "\"dateOfBirth\": \"2999-01-01\"", "dateOfBirth"),
                Arguments.of("\"heightCm\": 175.00", "\"heightCm\": 0", "heightCm"),
                Arguments.of("\"weightKg\": 72.50", "\"weightKg\": 0", "weightKg"),
                Arguments.of("\"workoutDaysPerWeek\": 4", "\"workoutDaysPerWeek\": 0", "workoutDaysPerWeek"),
                Arguments.of("\"workoutDaysPerWeek\": 4", "\"workoutDaysPerWeek\": 8", "workoutDaysPerWeek"),
                Arguments.of("\"maxSessionMinutes\": 90", "\"maxSessionMinutes\": 0", "maxSessionMinutes"),
                Arguments.of("\"mealsPerDay\": 4", "\"mealsPerDay\": 0", "mealsPerDay"),
                Arguments.of("\"mealsPerDay\": 4", "\"mealsPerDay\": 7", "mealsPerDay")
        );
    }

    @ParameterizedTest(name = "collection case {index}")
    @MethodSource("collectionBoundaryCases")
    @DisplayName("PUT từ chối collection quá 10 phần tử, chuỗi quá 50 ký tự và phần tử null")
    void upsertCurrentProfile_WithInvalidTextCollection_ShouldReturnVal001(
            String replacement
    ) throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        String invalid = validRequest().replace("[\"PEANUTS\"]", replacement);

        mockMvc.perform(put("/api/v1/member/profile")
                        .with(authentication(authenticationToken(principal)))
                        .contentType("application/json")
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations").isMap());
    }

    static Stream<Arguments> collectionBoundaryCases() {
        return Stream.of(
                Arguments.of("[\"A1\",\"A2\",\"A3\",\"A4\",\"A5\",\"A6\",\"A7\",\"A8\",\"A9\",\"A10\",\"A11\"]"),
                Arguments.of("[\"123456789012345678901234567890123456789012345678901\"]"),
                Arguments.of("[null]")
        );
    }

    @Test
    @DisplayName("PUT enum sai trả VAL-001")
    void upsertCurrentProfile_WithInvalidEnum_ShouldReturnVal001() throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        String invalid = validRequest().replace("\"MALE\"", "\"OTHER\"");

        mockMvc.perform(put("/api/v1/member/profile")
                        .with(authentication(authenticationToken(principal)))
                        .contentType("application/json")
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));
    }

    @Test
    @DisplayName("Guest và Admin không thể PUT Profile")
    void upsertCurrentProfile_WithoutMemberRole_ShouldBeBlocked() throws Exception {
        mockMvc.perform(put("/api/v1/member/profile")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACC-005"));

        AuthenticatedUserPrincipal admin = principal(RoleName.ROLE_ADMIN);
        mockMvc.perform(put("/api/v1/member/profile")
                        .with(authentication(authenticationToken(admin)))
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"));
    }

    @Test
    @DisplayName("OpenAPI công bố PUT cùng response 200, 400, 401 và 403")
    void openApi_ShouldExposeProfileUpsertContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].put.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].put.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].put.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/profile'].put.responses['403']").exists());
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
                new CalculatedTargetsResponse(
                        new BigDecimal("23.67"),
                        new BigDecimal("1683.75"),
                        new BigDecimal("2609.81"),
                        new BigDecimal("2909.81"),
                        new BigDecimal("159.50"),
                        new BigDecimal("80.83"),
                        new BigDecimal("386.09")
                ),
                Instant.parse("2026-08-03T03:30:00Z")
        );
    }

    private String validRequest() {
        return """
                {
                  "gender": "MALE",
                  "dateOfBirth": "1998-05-15",
                  "heightCm": 175.00,
                  "weightKg": 72.50,
                  "fitnessGoal": "BULK",
                  "fitnessLevel": "BEGINNER",
                  "activityLevel": "MODERATELY_ACTIVE",
                  "workoutDaysPerWeek": 4,
                  "maxSessionMinutes": 90,
                  "availableEquipment": ["BARBELL", "DUMBBELL"],
                  "targetMuscleGroups": ["CHEST", "BACK"],
                  "injuryConstraints": ["LOWER_BACK_LOAD_LIMITED"],
                  "dietaryPreference": "OMNIVORE",
                  "foodAllergies": ["PEANUTS"],
                  "excludedFoods": ["BEEF"],
                  "mealsPerDay": 4
                }
                """;
    }
}
