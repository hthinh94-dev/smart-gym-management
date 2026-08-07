package com.thinh.smartgym.progress.entity;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "body_progress", uniqueConstraints = @UniqueConstraint(
        name = "uk_body_progress_member_date",
        columnNames = {"member_id", "record_date"}
))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_body_progress_member")
    )
    private User member;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Positive
    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Positive
    @Column(name = "muscle_mass_kg", precision = 6, scale = 2)
    private BigDecimal muscleMassKg;

    @Positive
    @Column(name = "fat_mass_kg", precision = 6, scale = 2)
    private BigDecimal fatMassKg;

    public BodyProgress(User member, LocalDate recordDate, BigDecimal weightKg) {
        this(member, recordDate, weightKg, null, null);
    }

    public BodyProgress(
            User member,
            LocalDate recordDate,
            BigDecimal weightKg,
            BigDecimal muscleMassKg,
            BigDecimal fatMassKg
    ) {
        this.member = Objects.requireNonNull(member, "member must not be null");
        this.recordDate = Objects.requireNonNull(recordDate, "recordDate must not be null");
        this.weightKg = Objects.requireNonNull(weightKg, "weightKg must not be null");
        this.muscleMassKg = muscleMassKg;
        this.fatMassKg = fatMassKg;
    }

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
        return "BodyProgress{id=" + id + ", memberId="
                + (member == null ? null : member.getId())
                + ", recordDate=" + recordDate + ", weightKg=" + weightKg + "}";
    }
}
