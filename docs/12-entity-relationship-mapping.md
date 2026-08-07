# 12. Entity Relationship Mapping

## 1. Mục đích

Tài liệu này quy định cách ánh xạ baseline 25 bảng vật lý trong `docs/11-database-design.md` thành 16 Java Entity và 9 bảng `@ElementCollection` bằng Hibernate/Spring Data JPA. Schema local hiện hành bổ sung `member_fitness_goals` thành bảng collection thứ 10, tương ứng tổng cộng 26 bảng. Nội dung bao gồm annotation, Fetch Strategy, Cascade Strategy, Optimistic/Pessimistic Locking, Soft Delete và transaction boundary. Tài liệu là căn cứ để hiện thực tầng Persistence mà không vi phạm Business Rules, Use Case và API Contract đã chốt.

---

## 2. Quy ước JPA

Toàn bộ tầng Entity của dự án phải tuân thủ nghiêm ngặt 14 quy ước sau:

1. **Không trả Entity trực tiếp qua API:** Tuyệt đối không để các phương thức Controller trả về kiểu Entity JPA. Controller chỉ được phép nhận và trả về các đối tượng DTO (Data Transfer Objects). Vi phạm quy ước này có thể gây lộ thông tin nhạy cảm (`password_hash`), kích hoạt LazyInitializationException hoặc gây Infinite Recursion khi Jackson serialize các quan hệ hai chiều.

2. **`@ManyToOne` bắt buộc LAZY:** Mọi thuộc tính quan hệ `@ManyToOne` phải khai báo tường minh `fetch = FetchType.LAZY`. Hibernate mặc định `@ManyToOne` là EAGER, do đó nếu không ghi đè, mỗi truy vấn Entity sẽ kéo theo toàn bộ đồ thị quan hệ không cần thiết vào bộ nhớ, gây lãng phí tài nguyên và ảnh hưởng nghiêm trọng đến hiệu năng.

3. **`@OneToMany` giữ nguyên LAZY:** Mọi thuộc tính quan hệ `@OneToMany` giữ nguyên mặc định LAZY của Hibernate. Không tùy tiện cấu hình EAGER để né tránh LazyInitializationException vì điều đó gây lỗi N+1 Query ngầm. Cách xử lý đúng là dùng JPQL Fetch Join hoặc `@EntityGraph` tại Repository khi nghiệp vụ cần nạp Collection.

4. **User – UserRole – Role ưu tiên LAZY + Fetch Join khi xác thực:** Bảng `user_roles` có auditing fields nên được ánh xạ bằng Entity trung gian `UserRole`, không dùng `@ManyToMany` trực tiếp. Các quan hệ `User.userRoles`, `UserRole.user` và `UserRole.role` đều LAZY. Khi `UserDetailsService.loadUserByUsername()` xây dựng `SecurityContext`, Repository phải Fetch Join `UserRole` và `Role` trong cùng truy vấn.

5. **Không dùng `CascadeType.REMOVE` từ dữ liệu lịch sử sang Master Data:** Nghiêm cấm cấu hình `CascadeType.REMOVE` hoặc `CascadeType.ALL` trên các quan hệ từ bảng lịch sử (`workout_logs`, `member_subscriptions`) hoặc từ bảng nghiệp vụ (`workout_plan_exercises`, `workout_logs`) hướng sang các bảng Master Data (`exercises`, `membership_packages`). Làm như vậy có thể gây mất dữ liệu Master Data toàn bộ khi xóa một bản ghi lịch sử.

6. **Soft Delete cho `Exercise` và `MembershipPackage`:** Thực thể `Exercise` và `MembershipPackage` sử dụng `@SQLDelete` để cập nhật đồng thời `is_active = false` và `updated_at = CURRENT_TIMESTAMP(6)`, kết hợp `@Where(clause = "is_active = true")` cho truy vấn danh mục hiện hành. Truy vấn lịch sử cần đọc bản ghi inactive phải dùng native DTO projection vì entity-level `@Where` vẫn có thể được áp dụng khi Hibernate nạp association.

7. **Enum ánh xạ bằng `EnumType.STRING`:** Mọi trường kiểu Enum trong Entity Java phải được đánh dấu `@Enumerated(EnumType.STRING)`. Nếu dùng `EnumType.ORDINAL` (mặc định), việc thêm hoặc sắp xếp lại giá trị trong Enum Java sẽ làm sai lệch dữ liệu đã lưu trong DB mà không có cảnh báo nào từ hệ thống.

8. **Auditing có một nguồn sở hữu rõ ràng:** Khai báo `@EnableJpaAuditing` cho 16 Entity vật lý; `createdAt` và `updatedAt` dùng `@CreatedDate`, `@LastModifiedDate`. Chín bảng `@ElementCollection` không có lifecycle callback riêng nên MySQL quản lý hai timestamp bằng `DEFAULT CURRENT_TIMESTAMP(6)` và `ON UPDATE CURRENT_TIMESTAMP(6)`. Flyway sở hữu DDL; Hibernate chỉ chạy `ddl-auto=validate`.

9. **Optimistic Locking cho dữ liệu dễ xung đột:** `MemberSubscription`, `SubscriptionRenewalRequest` và `WorkoutPlan` khai báo `@Version`. Service kết hợp optimistic version với pessimistic lock theo phạm vi Member ở các luồng cần chuẩn hóa nhiều dòng. Xung đột version trả `CON-001` (HTTP 409), không tái sử dụng mã lỗi nghiệp vụ khác.

10. **Không đưa Association vào `equals()`, `hashCode()`, `toString()`:** Các thuộc tính quan hệ (`@ManyToOne`, `@OneToMany`, `@ManyToMany`) tuyệt đối không được đưa vào các phương thức `equals()`, `hashCode()` hoặc `toString()`. Nếu `toString()` chứa Collection LAZY, Hibernate sẽ kích hoạt thêm truy vấn SQL hoặc ném `LazyInitializationException`. Nếu `hashCode()` chứa Association hai chiều, Java sẽ ném `StackOverflowError` khi put Entity vào `HashSet` hoặc `HashMap`.

11. **DTO + MapStruct để tránh Infinite Recursion:** Sử dụng DTO cho toàn bộ tầng API và dùng MapStruct (hoặc Mapper thủ công) để chuyển đổi giữa Entity và DTO. Không sử dụng `@JsonManagedReference`/`@JsonBackReference` hay `@JsonIgnore` như giải pháp tạm thời vì chúng không giải quyết triệt để vấn đề và làm khó mở rộng.

12. **Cấm `@Data` của Lombok cho Entity JPA:** Dùng `@Getter`, `@Setter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. `equals()` so sánh khóa chính khác null và dùng `Hibernate.getClass()` để tương thích proxy; `hashCode()` dùng class hash ổn định, không thay đổi sau khi Entity được persist.

13. **`@AllArgsConstructor` chỉ dùng khi cần:** Chỉ dùng `@AllArgsConstructor` khi có nhu cầu khởi tạo đối tượng với tất cả trường. Hibernate yêu cầu bắt buộc có `@NoArgsConstructor` (protected hoặc public) để có thể khởi tạo Proxy.

14. **Tên bảng và cột phải khai báo tường minh:** Không phụ thuộc vào chiến lược đặt tên mặc định của Hibernate. Mọi Entity phải khai báo tường minh `@Table(name = "tên_bảng")` và mọi cột phải khai báo `@Column(name = "tên_cột")` để đảm bảo đồng bộ với tên bảng/cột `snake_case` trong MySQL.

**Cấu hình bắt buộc:**

```yaml
spring:
  datasource:
    hikari:
      connection-init-sql: "SET time_zone = '+00:00'"
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
```

`open-in-view: false` buộc Mapper hoàn thành việc chuyển Entity sang DTO trong transaction ở Service. Schema, generated columns, check constraints và timestamp mặc định do Flyway quản lý, không để Hibernate tự tạo hoặc tự sửa.

Các inverse `@OneToOne` LAZY (`User.memberProfile`, `WorkoutPlan.aiRecommendation`) cần Hibernate bytecode enhancement ở bước build để bảo đảm lazy loading thực sự. Nếu chưa cấu hình enhancement, Repository chỉ được nạp các association này bằng projection/fetch join trong use case cần thiết và không dựa vào LAZY như một bảo đảm hiệu năng.

Các khối Java trong tài liệu dùng bộ import chuẩn sau; mỗi public class được đặt trong một file Java riêng đúng tên class:

```java
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
```

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
}
```

---

## 3. Mapping từng Entity

### 3.1. Entity `User`

```java
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        User other = (User) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', accountStatus=" + accountStatus + "}";
    }
}
```

> **Ranh giới module:** `User` chỉ ánh xạ ngược `userRoles` vì Auth sở hữu vòng
> đời bảng liên kết này. Profile, Subscription, Workout và Progress ánh xạ
> unidirectional từ Entity nghiệp vụ về `User`; Auth Entity không chứa collection
> ngược. Service của từng module truy vấn theo `user.id`. Cách này đúng với source
> M1–M2, tránh kéo lazy graph và không tạo cascade xuyên module.

> **Lưu ý Fetch Join khi xác thực:**
> ```java
> // UserRepository.java
> @Query("""
>     SELECT DISTINCT u
>     FROM User u
>     LEFT JOIN FETCH u.userRoles ur
>     LEFT JOIN FETCH ur.role
>     WHERE LOWER(u.email) = LOWER(:email)
>     """)
> Optional<User> findByEmailWithRolesIgnoreCase(@Param("email") String email);
> ```
> Phương thức này được `CustomUserDetailsService.loadUserByUsername()` gọi để nạp User kèm Roles trong một câu SQL duy nhất, tránh hoàn toàn `LazyInitializationException`.

---

### 3.2. Entity `Role`, khóa ghép `UserRoleId` và Entity `UserRole`

`user_roles` có hai cột audit bắt buộc nên không được ánh xạ bằng `@ManyToMany` trực tiếp. Entity liên kết giúp JPA ghi đủ `created_at`, `updated_at` và vẫn giữ `Role` là master data không bị cascade remove từ `User`.

```java
@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 50)
    private RoleName name;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        Role other = (Role) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "Role{id=" + id + ", name=" + name + "}";
    }
}
```

```java
@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserRoleId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof UserRoleId other)) return false;
        return Objects.equals(userId, other.userId)
            && Objects.equals(roleId, other.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
```

```java
@Entity
@Table(name = "user_roles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_user_roles_user"))
    private User user;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_user_roles_role"))
    private Role role;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        UserRole other = (UserRole) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "UserRole{id=" + id + "}";
    }
}
```

---

### 3.3. Entity `MemberProfile`

```java
@Entity
@Table(name = "member_profiles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_member_profiles_user", columnNames = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_member_profiles_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
    @Positive
    private BigDecimal heightCm;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    @Positive
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

    @Column(name = "workout_days_per_week", nullable = false)
    @Min(1)
    @Max(7)
    private Integer workoutDaysPerWeek;

    @Column(name = "max_session_minutes", nullable = false)
    @Positive
    private Integer maxSessionMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_preference", nullable = false, length = 20)
    private DietaryPreference dietaryPreference;

    @Column(name = "meals_per_day", nullable = false)
    @Min(1)
    @Max(6)
    private Integer mealsPerDay;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_available_equipment",
        joinColumns = @JoinColumn(name = "member_profile_id",
                                  foreignKey = @ForeignKey(name = "fk_mae_member_profile"))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment", nullable = false, length = 50)
    private Set<Equipment> availableEquipment = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_target_muscle_groups",
        joinColumns = @JoinColumn(name = "member_profile_id",
                                  foreignKey = @ForeignKey(name = "fk_mtmg_member_profile"))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false, length = 50)
    private Set<MuscleGroup> targetMuscleGroups = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_injury_constraints",
        joinColumns = @JoinColumn(name = "member_profile_id",
                                  foreignKey = @ForeignKey(name = "fk_mic_member_profile"))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "constraint_tag", nullable = false, length = 80)
    private Set<ContraindicationTag> injuryConstraints = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_food_allergies",
        joinColumns = @JoinColumn(name = "member_profile_id",
                                  foreignKey = @ForeignKey(name = "fk_mfa_member_profile"))
    )
    @Column(name = "allergy_name", nullable = false, length = 50)
    @Size(max = 10)
    private Set<String> foodAllergies = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_excluded_foods",
        joinColumns = @JoinColumn(name = "member_profile_id",
                                  foreignKey = @ForeignKey(name = "fk_mef_member_profile"))
    )
    @Column(name = "food_name", nullable = false, length = 50)
    @Size(max = 10)
    private Set<String> excludedFoods = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
        return "MemberProfile{id=" + id + ", fitnessGoal=" + fitnessGoal + "}";
    }
}
```

---

### 3.4. Entity `MemberSubscription`

```java
@Entity
@Table(name = "member_subscriptions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_member_subscriptions_one_active",
                      columnNames = "active_member_key"),
    @UniqueConstraint(name = "uk_member_subscriptions_one_pending",
                      columnNames = "pending_member_key")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_member_subscriptions_member"))
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_member_subscriptions_package"))
    private MembershipPackage membershipPackage;

    @Column(name = "package_name_snapshot", nullable = false, length = 100)
    private String packageNameSnapshot;

    @Column(name = "package_duration_days_snapshot", nullable = false)
    private Integer packageDurationDaysSnapshot;

    @Column(name = "package_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal packagePriceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id",
                foreignKey = @ForeignKey(name = "fk_member_subscriptions_approver"))
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id",
                foreignKey = @ForeignKey(name = "fk_member_subscriptions_canceller"))
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "active_member_key", insertable = false, updatable = false)
    private Long activeMemberKey;

    @Column(name = "pending_member_key", insertable = false, updatable = false)
    private Long pendingMemberKey;

    @OneToMany(mappedBy = "activeSubscription", fetch = FetchType.LAZY)
    private List<SubscriptionRenewalRequest> renewalRequests = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        MemberSubscription other = (MemberSubscription) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "MemberSubscription{id=" + id + ", status=" + status
             + ", startDate=" + startDate + ", endDate=" + endDate + "}";
    }
}
```

DTO lịch sử Subscription phải lấy tên, thời lượng và giá từ ba snapshot field; không dereference `membershipPackage` để hiển thị vì package có thể đã inactive và bị `@Where` lọc khỏi association.

---

### 3.5. Entity `SubscriptionRenewalRequest`

```java
@Entity
@Table(name = "subscription_renewal_requests", uniqueConstraints = {
    @UniqueConstraint(name = "uk_renewal_requests_one_pending",
                      columnNames = "pending_subscription_key")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionRenewalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_srr_member"))
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_subscription_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_srr_active_subscription"))
    private MemberSubscription activeSubscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_srr_package"))
    private MembershipPackage renewalPackage;

    @Column(name = "package_duration_days_snapshot", nullable = false)
    private Integer packageDurationDaysSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private RenewalRequestStatus status = RenewalRequestStatus.PENDING;

    @Column(name = "previous_end_date")
    private LocalDate previousEndDate;

    @Column(name = "new_end_date")
    private LocalDate newEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id",
                foreignKey = @ForeignKey(name = "fk_srr_processor"))
    private User processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "pending_subscription_key", insertable = false, updatable = false)
    private Long pendingSubscriptionKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        SubscriptionRenewalRequest other = (SubscriptionRenewalRequest) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "SubscriptionRenewalRequest{id=" + id + ", status=" + status + "}";
    }
}
```

---

### 3.6. Entity `BodyProgress`

```java
@Entity
@Table(name = "body_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uk_body_progress_member_date",
                      columnNames = {"member_id", "record_date"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_body_progress_member"))
    private User member;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    @Positive
    private BigDecimal weightKg;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        BodyProgress other = (BodyProgress) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "BodyProgress{id=" + id
             + ", recordDate=" + recordDate + ", weightKg=" + weightKg + "}";
    }
}
```

> **Cơ chế Update-in-place (BR-22):** Unique Constraint `uk_body_progress_member_date` trên `(member_id, record_date)` là khóa xung đột của một câu lệnh atomic upsert. Service không thực hiện chuỗi `find → insert`; Repository dùng cú pháp `INSERT` kết hợp `ON DUPLICATE KEY UPDATE` được viết đầy đủ tại Mục 6.3 để hai request đồng thời không tạo bản ghi trùng.

---

### 3.7. Entity `WorkoutPlan`

```java
@Entity
@Table(name = "workout_plans", uniqueConstraints = {
    @UniqueConstraint(name = "uk_workout_plans_one_active",
                      columnNames = "active_member_key")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_workout_plans_member"))
    private User member;

    @Column(name = "plan_name", nullable = false, length = 150)
    private String planName;

    @Column(name = "split_model", nullable = false, length = 100)
    private String splitModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal", nullable = false, length = 20)
    private FitnessGoal goal;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkoutPlanStatus status = WorkoutPlanStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_source", nullable = false, length = 30)
    private WorkoutPlanSource recommendationSource;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "active_member_key", insertable = false, updatable = false)
    private Long activeMemberKey;

    @OneToMany(mappedBy = "workoutPlan", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @Size(min = 1, max = 7)
    private List<WorkoutDay> workoutDays = new ArrayList<>();

    @OneToOne(mappedBy = "workoutPlan", fetch = FetchType.LAZY)
    private AiRecommendation aiRecommendation;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        WorkoutPlan other = (WorkoutPlan) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "WorkoutPlan{id=" + id + ", status=" + status
            + ", recommendationSource=" + recommendationSource + "}";
    }
}
```

### 3.8. Entity `WorkoutDay`

```java
@Entity
@Table(name = "workout_days", uniqueConstraints = {
    @UniqueConstraint(name = "uk_workout_days_plan_number",
                      columnNames = {"workout_plan_id", "day_number"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_plan_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_workout_days_plan"))
    private WorkoutPlan workoutPlan;

    @Column(name = "day_number", nullable = false)
    @Min(1)
    @Max(7)
    private Integer dayNumber;

    @Column(name = "day_name", nullable = false, length = 100)
    private String dayName;

    @Column(name = "focus", length = 150)
    private String focus;

    @OneToMany(mappedBy = "workoutDay", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("exerciseOrder ASC")
    @Size(min = 1)
    private List<WorkoutPlanExercise> plannedExercises = new ArrayList<>();

    @OneToMany(mappedBy = "workoutDay", fetch = FetchType.LAZY)
    private List<WorkoutSession> workoutSessions = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        WorkoutDay other = (WorkoutDay) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "WorkoutDay{id=" + id + ", dayNumber=" + dayNumber
            + ", dayName='" + dayName + "'}";
    }
}
```

### 3.9. Entity `WorkoutPlanExercise`

```java
@Entity
@Table(name = "workout_plan_exercises", uniqueConstraints = {
    @UniqueConstraint(name = "uk_workout_plan_exercises_day_order",
                      columnNames = {"workout_day_id", "exercise_order"}),
    @UniqueConstraint(name = "uk_workout_plan_exercises_day_exercise",
                      columnNames = {"workout_day_id", "exercise_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutPlanExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_day_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wpe_workout_day"))
    private WorkoutDay workoutDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_wpe_exercise"))
    private Exercise exercise;

    @Column(name = "exercise_order", nullable = false)
    @Positive
    private Integer exerciseOrder;

    @Column(name = "planned_sets", nullable = false)
    @Min(1)
    @Max(5)
    private Integer plannedSets;

    @Column(name = "planned_reps", nullable = false)
    @Min(1)
    @Max(30)
    private Integer plannedReps;

    @Column(name = "planned_rpe", nullable = false, precision = 3, scale = 1)
    @DecimalMin("6.0")
    @DecimalMax("9.0")
    private BigDecimal plannedRpe;

    @Column(name = "rest_seconds", nullable = false)
    @Min(30)
    @Max(300)
    private Integer restSeconds;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workoutPlanExercise", fetch = FetchType.LAZY)
    private List<WorkoutLog> workoutLogs = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        WorkoutPlanExercise other = (WorkoutPlanExercise) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "WorkoutPlanExercise{id=" + id + ", exerciseOrder=" + exerciseOrder
            + ", plannedSets=" + plannedSets + ", plannedReps=" + plannedReps + "}";
    }
}
```

### 3.10. Entity `WorkoutSession`

```java
@Entity
@Table(name = "workout_sessions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_workout_sessions_member_date",
                      columnNames = {"member_id", "session_date"}),
    @UniqueConstraint(name = "uk_workout_sessions_identity",
                      columnNames = {"id", "member_id", "session_date"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_workout_sessions_member"))
    private User member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_day_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_workout_sessions_day"))
    private WorkoutDay workoutDay;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workoutSession", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<WorkoutLog> workoutLogs = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        WorkoutSession other = (WorkoutSession) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "WorkoutSession{id=" + id + ", sessionDate=" + sessionDate + "}";
    }
}
```

### 3.11. Entity `WorkoutLog`

```java
@Entity
@Table(name = "workout_logs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_workout_logs_member_date_exercise",
                      columnNames = {"member_id", "log_date", "exercise_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(value = {
        @JoinColumn(name = "workout_session_id", referencedColumnName = "id",
                    nullable = false),
        @JoinColumn(name = "member_id", referencedColumnName = "member_id",
                    nullable = false),
        @JoinColumn(name = "log_date", referencedColumnName = "session_date",
                    nullable = false)
    }, foreignKey = @ForeignKey(name = "fk_workout_logs_session_identity"))
    private WorkoutSession workoutSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_workout_logs_member"))
    private User member;

    @Column(name = "log_date", nullable = false,
            insertable = false, updatable = false)
    private LocalDate logDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_plan_exercise_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_workout_logs_plan_exercise"))
    private WorkoutPlanExercise workoutPlanExercise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_workout_logs_exercise"))
    private Exercise exercise;

    @Column(name = "actual_sets", nullable = false)
    @Min(1)
    @Max(10)
    private Integer actualSets;

    @Column(name = "actual_reps", nullable = false)
    @Min(1)
    @Max(100)
    private Integer actualReps;

    @Column(name = "actual_rpe", nullable = false, precision = 3, scale = 1)
    @DecimalMin("1.0")
    @DecimalMax("10.0")
    private BigDecimal actualRpe;

    @Column(name = "weight_used_kg", nullable = false, precision = 7, scale = 2)
    @DecimalMin("0.0")
    private BigDecimal weightUsedKg;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        WorkoutLog other = (WorkoutLog) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "WorkoutLog{id=" + id + ", logDate=" + logDate
            + ", actualSets=" + actualSets + ", actualReps=" + actualReps + "}";
    }
}
```

`WorkoutLog.workoutSession` là association duy nhất ghi ba cột của composite foreign key. Hai thuộc tính `member` và `logDate` là read-only view của các cột trùng, tránh lỗi Hibernate “repeated column in mapping”. Service xác minh `exercise.id == workoutPlanExercise.exercise.id` theo BR-28 trước khi persist.

### 3.12. Entity `AiRecommendation`

```java
@Entity
@Table(name = "ai_recommendations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ai_recommendations_workout_plan",
                      columnNames = "workout_plan_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ai_recommendations_member"))
    private User member;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_plan_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_ai_recommendations_workout_plan"))
    private WorkoutPlan workoutPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_source", nullable = false, length = 30)
    private RecommendationSource recommendationSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 30)
    private RecommendationValidationStatus validationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_code", length = 50)
    private AiWarningCode warningCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calculated_targets", nullable = false, columnDefinition = "JSON")
    private JsonNode calculatedTargets;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_suggestion", nullable = false, columnDefinition = "JSON")
    private JsonNode aiSuggestion;

    @OneToMany(mappedBy = "recommendation", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("mealOrder ASC")
    private List<NutritionMealSuggestion> mealSuggestions = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        AiRecommendation other = (AiRecommendation) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "AiRecommendation{id=" + id + ", recommendationSource="
            + recommendationSource + ", validationStatus=" + validationStatus + "}";
    }
}
```

`calculatedTargets` chỉ nhận `JsonNode` do `CalculationService` tạo gồm `bmi`, `bmr`, `tdee`, `dailyCaloriesKcal`, `proteinGrams`, `carbGrams`, `fatGrams`. Trước khi gán `aiSuggestion`, `AiOutputValidationService` phải từ chối payload có các key định lượng chính thức như `dailyCalorieTarget`, `macroTargets`, `calories`, `proteinGrams`, `carbGrams`, `fatGrams`; đồng thời hậu kiểm whitelist và planned values theo BR-09A, BR-09C, BR-10. `WorkoutPlan` cùng toàn bộ ngày/bài tập được persist trước, sau đó `AiRecommendation` và các `NutritionMealSuggestion` được persist trong cùng transaction. Nếu bất kỳ bước nào thất bại, toàn bộ graph rollback và không lưu recommendation một phần.

### 3.13. Entity `NutritionMealSuggestion`

```java
@Entity
@Table(name = "nutrition_meal_suggestions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_nutrition_meals_recommendation_order",
                      columnNames = {"recommendation_id", "meal_order"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NutritionMealSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_nutrition_meals_recommendation"))
    private AiRecommendation recommendation;

    @Column(name = "meal_name", nullable = false, length = 100)
    private String mealName;

    @Column(name = "time_suggest", nullable = false, length = 5,
            columnDefinition = "CHAR(5)")
    @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d")
    private String timeSuggest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "foods_list", nullable = false, columnDefinition = "JSON")
    private JsonNode foodsList;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "meal_order", nullable = false)
    @Positive
    private Integer mealOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        NutritionMealSuggestion other = (NutritionMealSuggestion) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "NutritionMealSuggestion{id=" + id + ", mealOrder="
            + mealOrder + ", mealName='" + mealName + "'}";
    }
}
```

---

## 4. Mapping Collection Table

Các bảng Collection được ánh xạ bằng `@ElementCollection` để tránh phải tạo Entity trung gian dư thừa. Dữ liệu Collection có vòng đời hoàn toàn phụ thuộc vào Entity cha.

Chín bảng collection vẫn có `created_at` và `updated_at` theo File 11, nhưng các cột này không phải thuộc tính của phần tử Collection. Flyway phải tạo chúng với `DEFAULT CURRENT_TIMESTAMP(6)` và `ON UPDATE CURRENT_TIMESTAMP(6)` để MySQL điền timestamp trong chính transaction insert/update của Hibernate. Không ánh xạ cùng một cột audit giả vào `Set` vì JPA không phát lifecycle callback cho từng phần tử cơ bản.

### 4.1. Profile Collection Tables (thuộc `MemberProfile`)

| Collection | Bảng DB | Cột dữ liệu | Tên FK Constraint |
| :--- | :--- | :--- | :--- |
| `availableEquipment` | `member_available_equipment` | `equipment VARCHAR(50)` | `fk_mae_member_profile` |
| `targetMuscleGroups` | `member_target_muscle_groups` | `muscle_group VARCHAR(50)` | `fk_mtmg_member_profile` |
| `injuryConstraints` | `member_injury_constraints` | `constraint_tag VARCHAR(80)` | `fk_mic_member_profile` |
| `foodAllergies` | `member_food_allergies` | `allergy_name VARCHAR(50)` | `fk_mfa_member_profile` |
| `excludedFoods` | `member_excluded_foods` | `food_name VARCHAR(50)` | `fk_mef_member_profile` |

Ba Collection chuẩn hóa dùng `Set<Equipment>`, `Set<MuscleGroup>` và `Set<ContraindicationTag>` với `@Enumerated(EnumType.STRING)`. Hai Collection văn bản `foodAllergies`, `excludedFoods` dùng `Set<String>` sau khi đã được Service trim, loại control character và giới hạn theo BR-23. Khi `MemberProfile` bị xóa cứng, Hibernate tự xóa phần tử Collection; không khai báo `CascadeType` cho `@ElementCollection`.

---

### 4.2. Exercise Collection Tables (thuộc `Exercise`)

| Collection | Bảng DB | Cột dữ liệu | Tên FK Constraint |
| :--- | :--- | :--- | :--- |
| `secondaryMuscleGroups` | `exercise_secondary_muscles` | `muscle_group VARCHAR(50)` | `fk_esm_exercise` |
| `requiredEquipment` | `exercise_equipment` | `equipment VARCHAR(50)` | `fk_ee_exercise` |
| `targetBodyRegions` | `exercise_target_body_regions` | `body_region VARCHAR(50)` | `fk_etbr_exercise` |
| `contraindicationTags` | `exercise_contraindication_tags` | `contraindication_tag VARCHAR(80)` | `fk_ect_exercise` |

Bốn Collection của Exercise lần lượt dùng `Set<MuscleGroup>`, `Set<Equipment>`, `Set<BodyRegion>` và `Set<ContraindicationTag>`. Điều này giữ Java type-safe và đồng bộ với CHECK Enum trong File 11.

---

## 5. Cascade và Fetch Strategy

Bảng dưới đây liệt kê toàn bộ quan hệ của baseline 25 bảng và phần mở rộng
`member_fitness_goals`, tương ứng 26 bảng hiện hành, cùng chiến lược thiết kế
JPA:

| Thực thể gốc | Thực thể liên quan | Annotation JPA | Fetch Type | Cascade Type | Lý do lựa chọn thiết kế |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `User` | `UserRole` | `@OneToMany` | LAZY | ALL | User sở hữu bản ghi liên kết và auditing của `user_roles`; xóa User chỉ xóa link, không xóa Role. |
| `UserRole` | `User` | `@ManyToOne` | LAZY | NONE | Association dùng `@MapsId`; lifecycle được điều khiển từ `User.userRoles`. |
| `UserRole` | `Role` | `@ManyToOne` | LAZY | NONE | Role là Master Data được seed bằng migration; tuyệt đối không cascade PERSIST/REMOVE từ liên kết. |
| `User` | `MemberProfile` | Không ánh xạ ngược | N/A | NONE | Profile được tạo/cập nhật bởi `MemberProfileService`; Auth không sở hữu lifecycle hoặc lazy graph Profile. |
| `MemberProfile` | `User` | `@OneToOne` | LAZY | NONE | Profile không được phép xóa hoặc thay đổi tài khoản sở hữu bằng cascade. |
| `User` | `MemberSubscription` | Không ánh xạ ngược | N/A | NONE | Subscription Service truy vấn theo Member; Auth không sở hữu lịch sử giao dịch. |
| `User` | `WorkoutPlan` | Không ánh xạ ngược | N/A | NONE | Workout module truy vấn theo Member; không kéo giáo án vào Auth Entity. |
| `User` | `WorkoutSession` | Không ánh xạ ngược | N/A | NONE | Workout module sở hữu truy vấn nhật ký và bảo toàn lịch sử. |
| `User` | `BodyProgress` | Không ánh xạ ngược | N/A | NONE | Repository Progress truy vấn theo `member.id`; lịch sử không làm tăng coupling của Auth Entity. |
| `User` | `AiRecommendation` | Không ánh xạ ngược | N/A | NONE | Recommendation module truy vấn theo Member và bảo toàn lịch sử độc lập. |
| `MemberProfile` | `member_available_equipment` | `@ElementCollection` | LAZY | N/A (auto) | ElementCollection có vòng đời phụ thuộc hoàn toàn vào cha. Hibernate tự cascade. |
| `MemberProfile` | `member_target_muscle_groups` | `@ElementCollection` | LAZY | N/A (auto) | Tương tự. |
| `MemberProfile` | `member_injury_constraints` | `@ElementCollection` | LAZY | N/A (auto) | Thẻ chấn thương quan trọng cho lọc whitelist. Tương tự. |
| `MemberProfile` | `member_food_allergies` | `@ElementCollection` | LAZY | N/A (auto) | Tương tự. |
| `MemberProfile` | `member_excluded_foods` | `@ElementCollection` | LAZY | N/A (auto) | Tương tự. |
| `MembershipPackage` | `MemberSubscription` | `@OneToMany` | LAZY | NONE | Subscription là lịch sử giao dịch. Xóa Package không được phép xóa Subscription. |
| `MembershipPackage` | `SubscriptionRenewalRequest` | `@OneToMany` | LAZY | NONE | Tương tự lý do trên. Dữ liệu lịch sử phải bảo toàn. |
| `MemberSubscription` | `User`, `MembershipPackage` | `@ManyToOne` | LAZY | NONE | Member và Package tồn tại độc lập; DTO lịch sử dùng snapshot package. |
| `MemberSubscription` | `approvedBy`, `cancelledBy` User | `@ManyToOne` | LAZY | NONE | Chỉ lưu tham chiếu audit đến Admin; không cascade. |
| `MemberSubscription` | `SubscriptionRenewalRequest` | `@OneToMany` | LAZY | NONE | RenewalRequest là dữ liệu lịch sử nghiệp vụ quan trọng. Không cascade. |
| `SubscriptionRenewalRequest` | `User`, `MemberSubscription`, `MembershipPackage` | `@ManyToOne` | LAZY | NONE | Các thực thể liên quan tồn tại độc lập và được khóa/kiểm tra tại Service. |
| `Exercise` | `exercise_secondary_muscles` | `@ElementCollection` | LAZY | N/A (auto) | Siêu dữ liệu bài tập phụ thuộc hoàn toàn vào Exercise. Hibernate tự cascade. |
| `Exercise` | `exercise_equipment` | `@ElementCollection` | LAZY | N/A (auto) | Tương tự. |
| `Exercise` | `exercise_target_body_regions` | `@ElementCollection` | LAZY | N/A (auto) | Tương tự. |
| `Exercise` | `exercise_contraindication_tags` | `@ElementCollection` | LAZY | N/A (auto) | Tương tự. Thẻ chấn thương được lọc so khớp với injury constraint của Member. |
| `WorkoutPlan` | `WorkoutDay` | `@OneToMany` | LAZY | ALL | WorkoutDay không tồn tại độc lập; phụ thuộc hoàn toàn vào WorkoutPlan. Cho phép cascade ALL. |
| `WorkoutPlan` | `User` | `@ManyToOne` | LAZY | NONE | Không cascade sang Member. |
| `WorkoutDay` | `WorkoutPlanExercise` | `@OneToMany` | LAZY | ALL | WorkoutPlanExercise không tồn tại độc lập. Cho phép cascade ALL để tạo/xóa theo WorkoutDay. |
| `WorkoutDay` | `WorkoutPlan` | `@ManyToOne` | LAZY | NONE | Lifecycle được điều khiển từ collection sở hữu của WorkoutPlan. |
| `WorkoutPlanExercise` | `Exercise` | `@ManyToOne` | LAZY | NONE | **Nghiêm cấm cascade.** Exercise là Master Data được Admin quản lý độc lập. Xóa WorkoutPlanExercise không được phép xóa bài tập gốc. |
| `WorkoutSession` | `WorkoutLog` | `@OneToMany` | LAZY | PERSIST, MERGE | WorkoutLog thuộc về WorkoutSession. Cho phép cascade PERSIST/MERGE nhưng không REMOVE vì log là dữ liệu lịch sử. |
| `WorkoutSession` | `User`, `WorkoutDay` | `@ManyToOne` | LAZY | NONE | Member và chi tiết giáo án không bị ảnh hưởng bởi lifecycle session. |
| `WorkoutLog` | `WorkoutSession`, `User`, `WorkoutPlanExercise` | `@ManyToOne` | LAZY | NONE | Composite FK giữ Session/Member/ngày đồng nhất; không cascade sang dữ liệu lịch sử khác. |
| `WorkoutLog` | `Exercise` | `@ManyToOne` | LAZY | NONE | **Nghiêm cấm cascade.** Tương tự lý do WorkoutPlanExercise → Exercise. |
| `BodyProgress` | `User` | `@ManyToOne` | LAZY | NONE | Progress là lịch sử độc lập; không cascade sang User. |
| `AiRecommendation` | `User` | `@ManyToOne` | LAZY | NONE | Recommendation là lịch sử và không điều khiển lifecycle User. |
| `AiRecommendation` | `WorkoutPlan` | `@OneToOne` | LAZY | NONE | WorkoutPlan có thể tồn tại độc lập sau khi AiRecommendation bị xóa. Không cascade. |
| `AiRecommendation` | `NutritionMealSuggestion` | `@OneToMany` | LAZY | ALL | NutritionMealSuggestion không tồn tại độc lập; phụ thuộc hoàn toàn vào AiRecommendation. |
| `NutritionMealSuggestion` | `AiRecommendation` | `@ManyToOne` | LAZY | NONE | Lifecycle được điều khiển từ collection sở hữu của AiRecommendation. |

**Lý do cho phép cascade từ `WorkoutPlan → WorkoutDay` nhưng nghiêm cấm từ `WorkoutPlanExercise → Exercise`:**

- `WorkoutDay` là thành phần con sở hữu hoàn toàn của `WorkoutPlan`. Một `WorkoutDay` không có ý nghĩa độc lập ngoài giáo án. Khi một giáo án bị xóa hoặc archive, các ngày tập cũng không còn giá trị. Do đó cho phép cascade ALL.

- `Exercise` là **Master Data** được Admin quản lý và có thể được tham chiếu từ nhiều nguồn độc lập: `workout_plan_exercises`, `workout_logs`, v.v. Việc cascade REMOVE từ `WorkoutPlanExercise` sang `Exercise` sẽ gây ra mất toàn bộ dữ liệu bài tập gốc, phá vỡ tính toàn vẹn dữ liệu của toàn bộ hệ thống. Đây là sai lầm nghiêm trọng và phải bị chặn ở cả tầng JPA (không cascade) lẫn tầng DB (`ON DELETE RESTRICT`).

---

## 6. Transaction và Concurrency

### 6.1. Phê duyệt đăng ký gói tập mới (New Subscription Approval)

```java
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final MembershipPackageRepository membershipPackageRepository;
    private final UserRepository userRepository;
    private final MemberSubscriptionMapper subscriptionMapper;

    @Transactional
    public MemberSubscriptionDto approveNewSubscription(Long subscriptionId, Long adminUserId) {
        MemberSubscription subscription = subscriptionRepository
            .findByIdForUpdate(subscriptionId)
            .orElseThrow(() -> new BusinessRuleViolationException(
                "SUB-005", "Không tìm thấy Subscription Request."));
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new BusinessRuleViolationException(
                "VAL-001", "Subscription request không còn ở trạng thái PENDING.");
        }

        Long memberId = subscription.getMember().getId();
        List<MemberSubscription> memberSubscriptions = subscriptionRepository
            .findAllByMemberIdForUpdate(memberId);

        LocalDate businessDate = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        memberSubscriptions.stream()
            .filter(item -> item.getStatus() == SubscriptionStatus.ACTIVE)
            .filter(item -> item.getEndDate() != null)
            .filter(item -> !item.getEndDate().isAfter(businessDate))
            .forEach(item -> item.setStatus(SubscriptionStatus.EXPIRED));

        // Giải phóng active_member_key trước khi kích hoạt request mới.
        // Flush vẫn nằm trong transaction; lỗi ở bước sau sẽ rollback cả hai pha.
        subscriptionRepository.flush();

        boolean hasRemainingActiveSubscription = memberSubscriptions.stream()
            .anyMatch(item -> item.getStatus() == SubscriptionStatus.ACTIVE);
        if (hasRemainingActiveSubscription) {
            throw new BusinessRuleViolationException(
                "SUB-004", "Member đã có Subscription ở trạng thái ACTIVE.");
        }

        Long packageId = subscription.getMembershipPackage().getId();
        if (!membershipPackageRepository.existsByIdAndIsActiveTrue(packageId)) {
            throw new BusinessRuleViolationException(
                "SUB-003", "Gói tập đã ngừng hoạt động.");
        }

        LocalDate endDate = businessDate
            .plusDays(subscription.getPackageDurationDaysSnapshot());
        User admin = userRepository.getReferenceById(adminUserId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(businessDate);
        subscription.setEndDate(endDate);
        subscription.setApprovedBy(admin);
        subscription.setApprovedAt(Instant.now());

        subscriptionRepository.flush();
        return subscriptionMapper.toDto(subscription);
    }
}
```

Repository phải khóa bi quan phạm vi cần chuẩn hóa; `@Version` tiếp tục bảo vệ update đơn dòng và phát hiện dữ liệu stale:

```java
public interface MemberSubscriptionRepository
        extends JpaRepository<MemberSubscription, Long> {

@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("""
    SELECT subscription
    FROM MemberSubscription subscription
    WHERE subscription.id = :subscriptionId
    """)
Optional<MemberSubscription> findByIdForUpdate(
    @Param("subscriptionId") Long subscriptionId
);

@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("""
    SELECT subscription
    FROM MemberSubscription subscription
    WHERE subscription.member.id = :memberId
    ORDER BY subscription.id
    """)
List<MemberSubscription> findAllByMemberIdForUpdate(
    @Param("memberId") Long memberId
);
}
```

---

### 6.2. Phê duyệt gia hạn gói tập (Renewal Approval — BR-24)

```java
@Service
@RequiredArgsConstructor
public class RenewalApprovalService {

private final SubscriptionRenewalRequestRepository renewalRequestRepository;
private final MemberSubscriptionRepository subscriptionRepository;
private final MembershipPackageRepository membershipPackageRepository;
private final UserRepository userRepository;
private final SubscriptionRenewalRequestMapper renewalMapper;

@Transactional
public SubscriptionRenewalRequestDto approveRenewalRequest(Long renewalRequestId, Long adminUserId) {
    SubscriptionRenewalRequest renewalRequest = renewalRequestRepository
        .findByIdForUpdate(renewalRequestId)
        .orElseThrow(() -> new BusinessRuleViolationException(
            "SUB-005", "Không tìm thấy Renewal Request."));
    if (renewalRequest.getStatus() != RenewalRequestStatus.PENDING) {
        throw new BusinessRuleViolationException(
            "VAL-001", "Renewal Request không còn ở trạng thái PENDING.");
    }

    MemberSubscription activeSubscription = subscriptionRepository
        .findByIdForUpdate(renewalRequest.getActiveSubscription().getId())
        .orElseThrow(() -> new BusinessRuleViolationException(
            "SUB-005", "Không tìm thấy Subscription đích của Renewal Request."));

    LocalDate businessDate = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    boolean validActiveSubscription =
        activeSubscription.getStatus() == SubscriptionStatus.ACTIVE
        && activeSubscription.getStartDate() != null
        && activeSubscription.getEndDate() != null
        && !businessDate.isBefore(activeSubscription.getStartDate())
        && businessDate.isBefore(activeSubscription.getEndDate());
    if (!validActiveSubscription) {
        throw new BusinessRuleViolationException(
            "SUB-005", "Subscription đích không còn hiệu lực để gia hạn.");
    }

    if (!Objects.equals(activeSubscription.getMember().getId(),
                        renewalRequest.getMember().getId())) {
        throw new BusinessRuleViolationException(
            "SUB-005", "Renewal Request không thuộc chủ sở hữu của Subscription.");
    }

    Long packageId = renewalRequest.getRenewalPackage().getId();
    if (!Objects.equals(activeSubscription.getMembershipPackage().getId(), packageId)) {
        throw new BusinessRuleViolationException(
            "VAL-001", "Gói gia hạn phải khớp với gói của Subscription hiện hành.");
    }
    if (!membershipPackageRepository.existsByIdAndIsActiveTrue(packageId)) {
        throw new BusinessRuleViolationException(
            "SUB-003", "Gói tập đã ngừng hoạt động.");
    }

    LocalDate previousEndDate = activeSubscription.getEndDate();
    LocalDate newEndDate = previousEndDate
        .plusDays(renewalRequest.getPackageDurationDaysSnapshot());

    activeSubscription.setEndDate(newEndDate);

    User admin = userRepository.getReferenceById(adminUserId);
    renewalRequest.setPreviousEndDate(previousEndDate);
    renewalRequest.setNewEndDate(newEndDate);
    renewalRequest.setStatus(RenewalRequestStatus.PROCESSED);
    renewalRequest.setProcessedBy(admin);
    renewalRequest.setProcessedAt(Instant.now());

    renewalRequestRepository.flush();
    return renewalMapper.toDto(renewalRequest);
}
}
```

```java
public interface SubscriptionRenewalRequestRepository
        extends JpaRepository<SubscriptionRenewalRequest, Long> {

@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("""
    SELECT request
    FROM SubscriptionRenewalRequest request
    WHERE request.id = :renewalRequestId
    """)
Optional<SubscriptionRenewalRequest> findByIdForUpdate(
    @Param("renewalRequestId") Long renewalRequestId
);
}
```

> **Xử lý cạnh tranh:** `@RestControllerAdvice` ánh xạ `OptimisticLockException`, `ObjectOptimisticLockingFailureException`, `LockTimeoutException`, `CannotAcquireLockException` và `PessimisticLockingFailureException` thành HTTP 409 với `errorCode = CON-001`. `SUB-006` chỉ dùng khi đã tồn tại request `PENDING`, tuyệt đối không dùng cho lỗi khóa/version.

---

### 6.3. Body Progress Upsert (BR-22)

```java
public interface BodyProgressRepository
        extends JpaRepository<BodyProgress, Long> {

@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query(value = """
    INSERT INTO body_progress (member_id, record_date, weight_kg)
    VALUES (:memberId, :recordDate, :weightKg)
    ON DUPLICATE KEY UPDATE
        weight_kg = :weightKg,
        updated_at = CURRENT_TIMESTAMP(6)
    """, nativeQuery = true)
int upsertAtomic(
    @Param("memberId") Long memberId,
    @Param("recordDate") LocalDate recordDate,
    @Param("weightKg") BigDecimal weightKg
);

@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query(value = """
    INSERT INTO body_progress (
        member_id, record_date, weight_kg, muscle_mass_kg, fat_mass_kg
    ) VALUES (
        :memberId, :recordDate, :weightKg, :muscleMassKg, :fatMassKg
    )
    ON DUPLICATE KEY UPDATE
        weight_kg = :weightKg,
        muscle_mass_kg = :muscleMassKg,
        fat_mass_kg = :fatMassKg,
        updated_at = CURRENT_TIMESTAMP(6)
    """, nativeQuery = true)
int upsertAtomicWithComposition(
    @Param("memberId") Long memberId,
    @Param("recordDate") LocalDate recordDate,
    @Param("weightKg") BigDecimal weightKg,
    @Param("muscleMassKg") BigDecimal muscleMassKg,
    @Param("fatMassKg") BigDecimal fatMassKg
);

Optional<BodyProgress> findByMember_IdAndRecordDate(Long memberId, LocalDate recordDate);

List<BodyProgress> findByMember_IdOrderByRecordDateAsc(Long memberId);
}
```

`findByMember_IdOrderByRecordDateAsc` cung cấp thứ tự ổn định để Frontend lấy phần tử đầu tiên làm cân nặng ban đầu và hiển thị mức tăng/giảm so với baseline. Đây là dữ liệu dẫn xuất ở tầng hiển thị, không làm thay đổi Entity hoặc bảng `body_progress`.

```java
@Service
@RequiredArgsConstructor
public class BodyProgressService {

private final BodyProgressRepository bodyProgressRepository;
private final AccountStatusGuard accountStatusGuard;
private final Clock clock;

@Transactional
public BodyProgressResponse upsertCurrentProgress(
        AuthenticatedUserPrincipal principal,
        BodyProgressUpsertRequest request) {

    Long memberId = requireMemberId(principal);
    accountStatusGuard.validateAccountStatusByUserId(memberId);
    validateRequest(request, LocalDate.now(clock.withZone(
        ZoneId.of("Asia/Ho_Chi_Minh"))));

    bodyProgressRepository.upsertAtomic(
        memberId, request.recordDate(), request.weightKg());

    BodyProgress saved = bodyProgressRepository
        .findByMember_IdAndRecordDate(memberId, request.recordDate())
        .orElseThrow(() -> new BusinessException(
            ErrorCode.INTERNAL_CONFIGURATION_ERROR));

    return BodyProgressResponse.from(saved);
}
}
```

Câu lệnh atomic upsert được viết đầy đủ trong Repository phía trên, dựa trên `uk_body_progress_member_date(member_id, record_date)` và thực thi nguyên tử trong MySQL. Không bắt `DataIntegrityViolationException` để tiếp tục cùng transaction vì sau lỗi SQL transaction có thể đã bị đánh dấu rollback-only.
Service lấy `memberId` từ `AuthenticatedUserPrincipal`, chạy
`AccountStatusGuard`, kiểm tra `ROLE_MEMBER` và từ chối ngày tương lai theo
`Asia/Ho_Chi_Minh`; request không chứa ID chủ sở hữu.

---

### 6.4. Kích hoạt Workout Plan mới (Activate Workout Plan)

```java
@Service
@RequiredArgsConstructor
public class WorkoutPlanService {

private final WorkoutPlanRepository workoutPlanRepository;
private final SubscriptionGuard subscriptionGuard;
private final WorkoutPlanMapper workoutPlanMapper;

@Transactional
public WorkoutPlanDto activateWorkoutPlan(Long newPlanId, Long memberId) {
    subscriptionGuard.requireValidActiveSubscription(memberId);

    List<WorkoutPlan> memberPlans = workoutPlanRepository
        .findAllByMemberIdForUpdate(memberId);

    WorkoutPlan newPlan = memberPlans.stream()
        .filter(plan -> Objects.equals(plan.getId(), newPlanId))
        .orElseThrow(() -> new ResourceNotFoundException("Workout plan not found: " + newPlanId));

    if (newPlan.getStatus() != WorkoutPlanStatus.DRAFT) {
        throw new BusinessRuleViolationException(
            "VAL-001", "Chỉ Workout Plan DRAFT mới được kích hoạt.");
    }

    memberPlans.stream()
        .filter(plan -> plan.getStatus() == WorkoutPlanStatus.ACTIVE)
        .forEach(plan -> plan.setStatus(WorkoutPlanStatus.ARCHIVED));

    // Giải phóng active_member_key trước khi gán ACTIVE cho plan mới.
    // Hai flush nằm trong cùng transaction nên không làm mất tính nguyên tử.
    workoutPlanRepository.flush();

    newPlan.setStatus(WorkoutPlanStatus.ACTIVE);
    newPlan.setActivatedAt(Instant.now());

    workoutPlanRepository.flush();
    return workoutPlanMapper.toDto(newPlan);
}
}
```

```java
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("""
    SELECT plan
    FROM WorkoutPlan plan
    WHERE plan.member.id = :memberId
    ORDER BY plan.id
    """)
List<WorkoutPlan> findAllByMemberIdForUpdate(@Param("memberId") Long memberId);
}
```

> **Quy tắc quan trọng:** Việc archive Plan cũ và kích hoạt Plan mới phải xảy ra trong cùng một `@Transactional`. Không được chia tách thành hai transaction riêng biệt vì nếu bước kích hoạt Plan mới gặp lỗi sau khi bước archive đã commit, hệ thống sẽ rơi vào trạng thái không có Plan ACTIVE nào — vi phạm tính nhất quán dữ liệu.

---

## 7. Soft Delete và bảo toàn lịch sử

### 7.1. Entity `Exercise` — Soft Delete tự động với Hibernate

```java
@Entity
@Table(name = "exercises", uniqueConstraints = {
    @UniqueConstraint(name = "uk_exercises_normalized_name",
                      columnNames = "normalized_name")
})
@SQLDelete(sql = "UPDATE exercises SET is_active = false, updated_at = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "is_active = true")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 150)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_muscle_group", nullable = false, length = 50)
    private MuscleGroup primaryMuscleGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_pattern", nullable = false, length = 50)
    private MovementPattern movementPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 20)
    private DifficultyLevel difficultyLevel;

    @Column(name = "instruction_text", nullable = false, columnDefinition = "TEXT")
    private String instructionText;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "exercise_secondary_muscles",
        joinColumns = @JoinColumn(name = "exercise_id",
                                  foreignKey = @ForeignKey(name = "fk_esm_exercise"))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false, length = 50)
    private Set<MuscleGroup> secondaryMuscleGroups = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "exercise_equipment",
        joinColumns = @JoinColumn(name = "exercise_id",
                                  foreignKey = @ForeignKey(name = "fk_ee_exercise"))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment", nullable = false, length = 50)
    private Set<Equipment> requiredEquipment = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "exercise_target_body_regions",
        joinColumns = @JoinColumn(name = "exercise_id",
                                  foreignKey = @ForeignKey(name = "fk_etbr_exercise"))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "body_region", nullable = false, length = 50)
    private Set<BodyRegion> targetBodyRegions = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "exercise_contraindication_tags",
        joinColumns = @JoinColumn(name = "exercise_id",
                                  foreignKey = @ForeignKey(name = "fk_ect_exercise"))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "contraindication_tag", nullable = false, length = 80)
    private Set<ContraindicationTag> contraindicationTags = new HashSet<>();

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<WorkoutPlanExercise> plannedUsages = new ArrayList<>();

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<WorkoutLog> workoutLogUsages = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        Exercise other = (Exercise) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "Exercise{id=" + id + ", normalizedName='" + normalizedName + "', isActive=" + isActive + "}";
    }
}
```

### 7.2. Cơ chế bảo toàn lịch sử `workout_logs` khi xóa mềm `Exercise`

Khi Admin gọi `exerciseRepository.delete(exercise)`, Hibernate thực thi:
```sql
UPDATE exercises
SET is_active = false,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = ?
```

- Bản ghi `exercises` vẫn còn trong DB với `is_active = false`.
- Các bản ghi `workout_logs` tham chiếu `exercise_id` vẫn nguyên vẹn nhờ ràng buộc `ON DELETE RESTRICT` tại tầng DB.
- `@Where(clause = "is_active = true")` đảm bảo Exercise đã xóa mềm không xuất hiện trong:
  - Kết quả truy vấn thư viện bài tập (`GET /api/v1/exercises`).
  - Whitelist xây dựng giáo án AI.
- `@Where` có thể tiếp tục lọc bản ghi inactive khi Hibernate nạp association. Vì vậy API lịch sử không truy cập trực tiếp `workoutLog.getExercise()` để lấy master data đã inactive; Repository sử dụng native DTO projection join `workout_logs` với `exercises`, không thêm điều kiện `is_active = true`.

> **Lưu ý:** Khi cần truy vấn toàn bộ Exercise kể cả đã xóa mềm (phục vụ Admin xem lại lịch sử), sử dụng `@Query` với native SQL hoặc dùng `EntityManager.createNativeQuery()` để bỏ qua bộ lọc `@Where`.

### 7.3. Soft Delete `MembershipPackage`

```java
@Entity
@Table(name = "membership_packages", uniqueConstraints = {
    @UniqueConstraint(name = "uk_membership_packages_normalized_name",
                      columnNames = "normalized_name")
})
@SQLDelete(sql = "UPDATE membership_packages SET is_active = false, updated_at = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "is_active = true")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "duration_days", nullable = false)
    @Min(1)
    @Max(3650)
    private Integer durationDays;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    @DecimalMin("0.0")
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "membershipPackage", fetch = FetchType.LAZY)
    private List<MemberSubscription> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "renewalPackage", fetch = FetchType.LAZY)
    private List<SubscriptionRenewalRequest> renewalRequests = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        MembershipPackage other = (MembershipPackage) object;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "MembershipPackage{id=" + id + ", normalizedName='" + normalizedName + "', isActive=" + isActive + "}";
    }
}
```

---

## 8. Các điểm cần tránh (Banned Practices in JPA)

| Sai lầm | Mô tả chi tiết | Cách phòng tránh |
| :--- | :--- | :--- |
| **LazyInitializationException** | Truy cập thuộc tính LAZY sau khi Hibernate Session đã đóng (thường trong Controller khi Jackson serialize Entity trực tiếp). | Không trả Entity ra ngoài Service. Dùng DTO. Dùng `@Transactional` đúng scope. |
| **N+1 Query** | Duyệt qua danh sách Entity và mỗi lần Hibernate phát sinh thêm 1 câu SELECT cho từng Collection LAZY. | Dùng `JOIN FETCH` trong JPQL. Dùng `@EntityGraph`. Không bao giờ dùng `@OneToMany(fetch = EAGER)`. |
| **StackOverflowError từ `toString()`** | Entity A có `toString()` gọi `entityB.toString()`, Entity B có `toString()` gọi `entityA.toString()` → vòng lặp vô hạn. | Không đưa Association vào `toString()`. Chỉ log các giá trị nguyên thủy (ID, status). |
| **Infinite JSON Recursion** | Jackson serialize Entity có quan hệ hai chiều (`@OneToMany` + `@ManyToOne`) → vòng lặp vô hạn. | Không serialize Entity trực tiếp. Dùng DTO. Không dùng `@JsonManagedReference` như giải pháp tạm. |
| **`CascadeType.ALL` tùy tiện** | Khai báo `cascade = CascadeType.ALL` trên mọi quan hệ mà không suy xét. Khi xóa Entity cha sẽ xóa toàn bộ Entity con liên quan, bao gồm Master Data. | Chỉ dùng `CascadeType.ALL` khi Entity con hoàn toàn phụ thuộc vào cha. Dữ liệu lịch sử không cascade. |
| **Kiểm tra ACTIVE rồi INSERT ở hai transaction** | Kiểm tra `hasActiveSubscription = true` ở transaction 1, insert ở transaction 2. Nếu hai request đồng thời, cả hai đều thấy chưa có ACTIVE và cùng insert gây vi phạm BR-04. | Sử dụng Unique Constraint ở DB (`active_member_key`) và `@Version` để DB tự chặn. Không tách logic "check rồi insert" ra hai transaction. |
| **Lombok `@Data` cho Entity** | `@Data` sinh `equals()`, `hashCode()` dựa trên tất cả trường, kể cả Collection LAZY. Khi equals() được gọi, Hibernate nạp toàn bộ Collection. | Dùng `@Getter`, `@Setter`, `@NoArgsConstructor`. Viết tay `equals()` và `hashCode()` chỉ dựa trên `id`. |
| **Nạp User để lấy Roles mà không Fetch Join** | `userRepository.findByEmail()` nạp User, sau đó code duyệt `userRoles` trong tầng Security khi Session đã đóng. | Fetch Join đồng thời `u.userRoles` và `userRole.role` tại bước `loadUserByUsername`. |
| **Không dùng `@Version` cho Subscription** | Hai Admin cùng phê duyệt hoặc gia hạn một Subscription dẫn đến dữ liệu `endDate` bị tính sai. | Khai báo `@Version private Long version` trên `MemberSubscription` và `SubscriptionRenewalRequest`. |

---

## 9. Traceability với Database Design

| Mã Entity Java | Bảng DB tương ứng (`docs/11-database-design.md`) | Ghi chú đồng bộ ORM |
| :--- | :--- | :--- |
| `User` | `users` | `email` dùng `@UniqueConstraint(uk_users_email)`; `accountStatus` dùng `EnumType.STRING`. |
| `Role` | `roles` | `name` → `@Enumerated(EnumType.STRING)`. Không cascade từ Role xuống bất kỳ thực thể nào. |
| `UserRole` + `UserRoleId` | `user_roles` | Entity liên kết dùng `@EmbeddedId`, `@MapsId` và JPA Auditing để ghi đủ hai timestamp. Không dùng `@ManyToMany` trực tiếp. |
| `MemberProfile` | `member_profiles` | `mealsPerDay` có `CHECK (1-6)` ở DB; cần validate tương ứng ở DTO validation (BR-23). |
| `MemberProfile.availableEquipment` | `member_available_equipment` | `@ElementCollection + @Enumerated(STRING)`; timestamp do DB default quản lý. |
| `MemberProfile.targetMuscleGroups` | `member_target_muscle_groups` | `@ElementCollection + @Enumerated(STRING)`; timestamp do DB default quản lý. |
| `MemberProfile.injuryConstraints` | `member_injury_constraints` | `@ElementCollection<ContraindicationTag>`; timestamp do DB default quản lý; dùng lọc whitelist. |
| `MemberProfile.foodAllergies` | `member_food_allergies` | `@ElementCollection<String>`; Service chuẩn hóa theo BR-23; timestamp do DB default quản lý. |
| `MemberProfile.excludedFoods` | `member_excluded_foods` | `@ElementCollection<String>`; Service chuẩn hóa theo BR-23; timestamp do DB default quản lý. |
| `MembershipPackage` | `membership_packages` | Soft delete; `uk_membership_packages_normalized_name` khai báo tường minh tại `@Table`. |
| `MemberSubscription` | `member_subscriptions` | `@Version`; generated keys read-only; snapshot fields chống sai lệch giá khi Package thay đổi. |
| `SubscriptionRenewalRequest` | `subscription_renewal_requests` | `@Version`; `pendingSubscriptionKey` read-only; khóa khi duyệt BR-24. |
| `Exercise` | `exercises` | Soft delete; `uk_exercises_normalized_name` khai báo tường minh tại `@Table`. |
| `Exercise.secondaryMuscleGroups` | `exercise_secondary_muscles` | `@ElementCollection<MuscleGroup>`; timestamp do DB default quản lý. |
| `Exercise.requiredEquipment` | `exercise_equipment` | `@ElementCollection<Equipment>`; timestamp do DB default quản lý. |
| `Exercise.targetBodyRegions` | `exercise_target_body_regions` | `@ElementCollection<BodyRegion>`; timestamp do DB default quản lý. |
| `Exercise.contraindicationTags` | `exercise_contraindication_tags` | `@ElementCollection<ContraindicationTag>`; timestamp do DB default quản lý; lọc so khớp với hạn chế Member. |
| `WorkoutPlan` | `workout_plans` | `@Version`; `activeMemberKey` read-only; `status` dùng `EnumType.STRING`. |
| `WorkoutDay` | `workout_days` | `cascade = ALL` từ WorkoutPlan. |
| `WorkoutPlanExercise` | `workout_plan_exercises` | Check Constraint ở DB: sets (1-5), reps (1-30), RPE (6.0-9.0), rest (30-300). Validate tương ứng ở DTO. |
| `WorkoutSession` | `workout_sessions` | Unique Constraint `(member_id, session_date)` tránh trùng ngày. |
| `WorkoutLog` | `workout_logs` | Check Constraint ở DB: sets (1-10), reps (1-100), RPE (1.0-10.0), weight (≥0) theo BR-09B. |
| `BodyProgress` | `body_progress` | Unique `(member_id, record_date)` và atomic MySQL upsert thực thi BR-22. |
| `AiRecommendation` | `ai_recommendations` | Hai cột JSON tách `calculatedTargets` do Backend sở hữu và `aiSuggestion` đã hậu kiểm. |
| `NutritionMealSuggestion` | `nutrition_meal_suggestions` | `cascade = ALL` từ AiRecommendation. `foodsList` lưu JSON. |

---

## 10. Quality Gate trước khi hiện thực Entity

| Tiêu chí | Kết quả yêu cầu |
| :--- | :--- |
| Bảng vật lý | 26/26 bảng hiện hành được truy vết trong Mục 9, gồm `member_fitness_goals`. |
| Entity JPA | 16 Entity: `User`, `Role`, `UserRole`, `MemberProfile`, `MembershipPackage`, `MemberSubscription`, `SubscriptionRenewalRequest`, `Exercise`, `WorkoutPlan`, `WorkoutDay`, `WorkoutPlanExercise`, `WorkoutSession`, `WorkoutLog`, `BodyProgress`, `AiRecommendation`, `NutritionMealSuggestion`. |
| Collection table | 9/9 `@ElementCollection`, timestamp do MySQL quản lý bằng default/on-update. |
| Fetch | Mọi `@ManyToOne`, `@OneToMany`, `@OneToOne` đều khai báo LAZY tường minh. |
| Cascade sở hữu | `WorkoutPlan → WorkoutDay → WorkoutPlanExercise` và `AiRecommendation → NutritionMealSuggestion` dùng `ALL + orphanRemoval`. |
| Cascade bị cấm | Không có REMOVE/ALL từ lịch sử hoặc chi tiết nghiệp vụ sang `Role`, `Exercise`, `MembershipPackage` hay `User`. |
| Optimistic locking | Có `@Version` tại `MemberSubscription`, `SubscriptionRenewalRequest`, `WorkoutPlan`; xung đột trả `CON-001`. |
| Generated columns | `activeMemberKey`, `pendingMemberKey`, `pendingSubscriptionKey` và `WorkoutPlan.activeMemberKey` là read-only. |
| Soft delete | `Exercise`, `MembershipPackage` cập nhật `is_active` và `updated_at`; lịch sử dùng native DTO projection. |
| API boundary | Controller chỉ nhận/trả DTO; Entity và association không được Jackson serialize. |
| DDL ownership | Flyway quản lý schema; Hibernate `ddl-auto=validate`; Open Session in View bị tắt. |

File 12 chỉ được xem là hoàn thành khi các quy ước trên được phản ánh nguyên vẹn trong mã nguồn Entity, Repository và Service thực tế, đồng thời migration SQL vượt qua kiểm thử khởi tạo từ database rỗng.
