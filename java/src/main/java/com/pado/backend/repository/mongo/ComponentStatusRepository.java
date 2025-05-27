package com.pado.backend.repository.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.pado.backend.domain.mongo.ComponentStatusDocument;

@Repository
public interface ComponentStatusRepository extends MongoRepository<ComponentStatusDocument, String> {

    /*
    TODO : 탐색 속도를 높이려면 어떻게 해야할까? 모든 서비스 사용자의 컴포넌트 상태를 저장하고 있는데
    ComponentStatusDocument의 필드에 userId도 추가해야 할까?
    userId, projectId, componentId를 이용하면 탐색 속도가 빨라질까?
    */ 
    @Query(value = "{ 'componentId': ?0 }")
    Optional<ComponentStatusDocument> findByComponentId(String componentId);
}
