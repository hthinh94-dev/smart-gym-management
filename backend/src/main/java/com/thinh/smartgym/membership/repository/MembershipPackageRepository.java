package com.thinh.smartgym.membership.repository;

import com.thinh.smartgym.membership.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Long> {

    List<MembershipPackage> findByActiveTrueOrderByDurationDaysAscIdAsc();

    List<MembershipPackage> findAllByOrderByCreatedAtDescIdDesc();

    boolean existsByNormalizedName(String normalizedName);

    Optional<MembershipPackage> findByNormalizedName(String normalizedName);

    boolean existsByNormalizedNameAndIdNot(String normalizedName, Long id);
}
