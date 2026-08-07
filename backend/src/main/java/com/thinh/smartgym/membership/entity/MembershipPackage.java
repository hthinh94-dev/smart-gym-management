package com.thinh.smartgym.membership.entity;

import com.thinh.smartgym.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "membership_packages", uniqueConstraints = {
    @UniqueConstraint(name = "uk_membership_packages_normalized_name", columnNames = "normalized_name")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipPackage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 100)
    private String normalizedName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "duration_days", nullable = false)
    private Short durationDays;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public MembershipPackage(
            String name,
            String normalizedName,
            String description,
            short durationDays,
            BigDecimal price
    ) {
        update(name, normalizedName, description, durationDays, price);
    }

    public void update(
            String name,
            String normalizedName,
            String description,
            short durationDays,
            BigDecimal price
    ) {
        this.name = Objects.requireNonNull(name);
        this.normalizedName = Objects.requireNonNull(normalizedName);
        this.description = description;
        this.durationDays = durationDays;
        this.price = Objects.requireNonNull(price);
    }

    public void deactivate() {
        this.active = false;
    }

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
        return "MembershipPackage{id=" + id + ", name='" + name + "', active=" + active + "}";
    }
}
