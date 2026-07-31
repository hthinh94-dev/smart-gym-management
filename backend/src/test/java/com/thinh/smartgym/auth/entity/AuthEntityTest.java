package com.thinh.smartgym.auth.entity;

import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthEntityTest {

    /** Kiểm tra trạng thái mặc định bảo vệ tài khoản mới khi caller truyền null. */
    @Test
    @DisplayName("User mac dinh ACTIVE khi accountStatus la null")
    void user_WithNullAccountStatus_ShouldDefaultToActive() {
        User user = new User("Nguyen Van An", "member@smartgym.com", "secret-hash", null);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getUserRoles()).isEmpty();
    }

    /** Kiểm tra entity nền tảng lưu đúng thời điểm tạo và cập nhật do JPA Auditing gán. */
    @Test
    @DisplayName("BaseEntity luu duoc createdAt va updatedAt")
    void baseEntity_ShouldStoreAuditTimestamps() {
        User user = new User("Nguyen Van An", "member@smartgym.com", "secret-hash", AccountStatus.ACTIVE);
        Instant createdAt = Instant.parse("2026-07-31T08:00:00Z");
        Instant updatedAt = createdAt.plusSeconds(60);

        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    }

    /** Kiểm tra quan hệ User - UserRole chỉ nhận đúng association thuộc chính user đó. */
    @Test
    @DisplayName("Gan UserRole hop le vao User")
    void attachUserRole_WithMatchingUser_ShouldAttachRelation() {
        User user = userWithId(10L);
        Role role = roleWithId(20L, RoleName.ROLE_MEMBER);
        UserRole relation = new UserRole(user, role);

        user.attachUserRole(relation);

        assertThat(user.getUserRoles()).containsExactly(relation);
        assertThat(relation.getId()).isEqualTo(new UserRoleId(10L, 20L));
    }

    /** Kiểm tra null association bị từ chối sớm thay vì gây lỗi ngầm lúc persistence. */
    @Test
    @DisplayName("Khong cho gan UserRole null")
    void attachUserRole_WithNullRelation_ShouldThrowException() {
        User user = userWithId(10L);

        assertThatThrownBy(() -> user.attachUserRole(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("userRole must not be null");
    }

    /** Kiểm tra association của user khác không thể bị gắn nhầm vào aggregate hiện tại. */
    @Test
    @DisplayName("Khong cho gan UserRole cua User khac")
    void attachUserRole_WithDifferentOwner_ShouldThrowException() {
        User owner = userWithId(10L);
        User anotherUser = userWithId(11L);
        UserRole relation = new UserRole(anotherUser, roleWithId(20L, RoleName.ROLE_MEMBER));

        assertThatThrownBy(() -> owner.attachUserRole(relation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UserRole must belong to this user");
    }

    /** Kiểm tra equality của User dựa trên database identity và không coi hai transient entity là bằng nhau. */
    @Test
    @DisplayName("User equals chi dung id khac null")
    void userEquality_ShouldUseOnlyPersistedIdentity() {
        User first = userWithId(10L);
        User sameIdentity = userWithId(10L);
        User differentIdentity = userWithId(11L);
        User transientUser = userWithId(null);

        assertThat(first).isEqualTo(sameIdentity).isNotEqualTo(differentIdentity);
        assertThat(transientUser).isNotEqualTo(userWithId(null));
        assertThat(first.hashCode()).isEqualTo(sameIdentity.hashCode());
    }

    /** Kiểm tra log representation của User không làm lộ password hash. */
    @Test
    @DisplayName("User toString khong lam lo password hash")
    void userToString_ShouldNotExposePasswordHash() {
        User user = userWithId(10L);

        assertThat(user.toString())
                .contains("member@smartgym.com", "ACTIVE")
                .doesNotContain("secret-hash");
    }

    /** Kiểm tra Role equality sử dụng id persistence và vẫn hiển thị tên role khi log. */
    @Test
    @DisplayName("Role equals theo id va toString chua ten role")
    void roleEquality_ShouldUsePersistedIdentity() {
        Role first = roleWithId(20L, RoleName.ROLE_ADMIN);
        Role sameIdentity = roleWithId(20L, RoleName.ROLE_MEMBER);
        Role differentIdentity = roleWithId(21L, RoleName.ROLE_ADMIN);

        assertThat(first).isEqualTo(sameIdentity).isNotEqualTo(differentIdentity);
        assertThat(first.toString()).contains("20", "ROLE_ADMIN");
    }

    /** Kiểm tra constructor UserRole chịu được entity chưa có id và tạo composite id null an toàn. */
    @Test
    @DisplayName("UserRole tao composite id an toan khi entity chua persist")
    void userRole_WithTransientEntities_ShouldCreateNullCompositeId() {
        UserRole relation = new UserRole(
                new User("Member", "member@smartgym.com", "hash", AccountStatus.ACTIVE),
                new Role(RoleName.ROLE_MEMBER)
        );

        assertThat(relation.getId()).isEqualTo(new UserRoleId(null, null));
    }

    /** Kiểm tra value object composite key tuân thủ equals/hashCode cho collection và JPA. */
    @Test
    @DisplayName("UserRoleId equals va hashCode theo ca hai thanh phan")
    void userRoleIdEquality_ShouldUseBothKeyParts() {
        UserRoleId key = new UserRoleId(10L, 20L);
        UserRoleId sameKey = new UserRoleId(10L, 20L);
        UserRoleId differentRole = new UserRoleId(10L, 21L);

        assertThat(key).isEqualTo(sameKey).hasSameHashCodeAs(sameKey);
        assertThat(key).isNotEqualTo(differentRole).isNotEqualTo(null).isNotEqualTo("10:20");
    }

    private User userWithId(Long id) {
        User user = new User("Nguyen Van An", "member@smartgym.com", "secret-hash", AccountStatus.ACTIVE);
        user.setId(id);
        return user;
    }

    private Role roleWithId(Long id, RoleName roleName) {
        Role role = new Role(roleName);
        role.setId(id);
        return role;
    }
}
