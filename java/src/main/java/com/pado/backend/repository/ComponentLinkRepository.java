package com.pado.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pado.backend.domain.Component;
import com.pado.backend.domain.ComponentLink;
/* TODO
단, List<ComponentInfo>를 반환하려면 ComponentInfo가 Spring Data JPA Projection 
혹은 Native Query/JPQL의 결과 타입으로 명확하게 정의되어 있어야 합니다. 
그렇지 않다면 ComponentLink를 반환하거나 직접 @Query를 써야 합니다.
*/
@Repository
public interface ComponentLinkRepository extends JpaRepository<ComponentLink, Long>{
    List<ComponentLink> findByFromComponentId(Component component); 
    List<ComponentLink> findByToComponentId(Component component);   

    // 특정 컴포넌트 간 연결 조회
    @Query("SELECT cl FROM ComponentLink cl WHERE cl.fromComponentId =:fromComponent AND cl.toComponentId =:toComponent")
    List<ComponentLink> findByFromComponentIdAndToComponentId(
        @Param("fromComponent") Component fromComponent,
        @Param("toComponent") Component tComponent
    );


}
