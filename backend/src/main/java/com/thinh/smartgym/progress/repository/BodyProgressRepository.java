package com.thinh.smartgym.progress.repository;

import com.thinh.smartgym.progress.entity.BodyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BodyProgressRepository extends JpaRepository<BodyProgress, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO body_progress (member_id, record_date, weight_kg, muscle_mass_kg, fat_mass_kg)
            VALUES (:memberId, :recordDate, :weightKg, :muscleMassKg, :fatMassKg)
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
