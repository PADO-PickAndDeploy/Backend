package com.pado.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pado.backend.domain.ComponentMeta;

@Repository
public interface ComponentMetaRepository extends JpaRepository<ComponentMeta, Long> {

    // 전체 메타 정보 조회
    List<ComponentMeta> findAll();

    // 타입으로 필터링 (RESOURCE or SERVICE)
    List<ComponentMeta> findByType(String type);

    // 서브타입 중복 방지용 존재 여부 확인
    boolean existsBySubtype(String subtype);

    // 특정 subtype의 메타 정보 조회 (중복 체크 또는 관리 UI용)
    Optional<ComponentMeta> findBySubtype(String subtype);

    @Query("SELECT c FROM ComponentMeta c " +
           "WHERE LOWER(c.type) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(c.subtype) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ComponentMeta> searchByKeyword(@Param("keyword") String keyword);
}
