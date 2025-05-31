package com.pado.backend.domain.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사용자가 입력한 컴포넌트 설정 정보를 저장함.
// 해당 설정은 나중에 Deployment 단계에서 gRPC 요청으로 Go 서버에 전달할 때 사용됨.
@Document(collection = "component_setting")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ComponentSettingDocument {
    
    @Id
    private String id;

    private Long componentId; // CHECKLIST 스프링 내부에서 컴포넌트를 구분할 때 사용하므로 Long 타입
    private Long credentialId; // Credential 엔티티의 ID → JPA에서 Credential 조회 가능
    private String settingJson;  // 실제 컴포넌트 설정 JSON (credential 정보는 제외)
}
