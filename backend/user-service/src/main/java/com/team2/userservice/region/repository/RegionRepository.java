package com.team2.userservice.region.repository;

import com.team2.userservice.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByRegionCode(String regionCode);
    Optional<Region> findBySido(String sido);
    Optional<Region> findBySidoAndSigungu(String sido, String sigungu);
    Optional<Region> findBySidoAndSigunguAndDong(String sido, String sigungu, String dong);

}
