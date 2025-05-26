package com.pado.backend.domain.mongo;

import org.springframework.data.mongodb.core.mapping.Document;

import com.pado.backend.global.type.ComponentStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

@Document(collection = "component_status")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// TODO : 필드 고민, timestamp -> updatedAt을 사용하여 status 변경 시 자동 감지, 변경
public class ComponentStatusDocument {

    @Id
    private String id;

    // 공통 필드
    private String componentId; // CHECKLIST Go에게 전달할 때 사용하는 ID라서 타입이 String
    private String projectId;
    private String deploymentId;
    private ComponentStatus status;
    // [ ] :  @UpdatedAt 같은걸로 변경해서 자동으로 Audit 가능하도록
    private LocalDateTime updatedAt;
}
