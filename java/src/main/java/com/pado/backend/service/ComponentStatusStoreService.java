package com.pado.backend.service;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.pado.backend.domain.mongo.ComponentStatusDocument;
import com.pado.backend.global.type.ComponentStatus;

import lombok.RequiredArgsConstructor;
/* TODO
 * 몽고디비에서 컴포넌트 상태를 덮어씌우기 위한 몽고템플릿 , ComponentStatusDocument 객체를 생성하고 덮어씌워준다.
 * 컴포넌트의 상태만 저장해두는건데 컴포넌트 id만 있으면 충분할 것 같다. 배포 id, 프로젝트 id 굳이?
 */
@Service
@RequiredArgsConstructor
public class ComponentStatusStoreService {

    private final MongoTemplate mongoTemplate;

    public void upsert(String componentId, ComponentStatus status) {
        Query query = Query.query(Criteria.where("componentId").is(componentId));

        Update update = new Update()
                .set("componentId", componentId)
                .set("status", status)
                .set("updatedAt", LocalDateTime.now());

        mongoTemplate.upsert(query, update, ComponentStatusDocument.class);
    }
}
