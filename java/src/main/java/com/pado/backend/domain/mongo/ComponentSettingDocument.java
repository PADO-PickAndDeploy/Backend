package com.pado.backend.domain.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자가 입력한 컴포넌트 설정 정보를 저장함.
// 해당 설정은 나중에 Deployment 단계에서 gRPC 요청으로 Go 서버에 전달할 때 사용됨.
@Document(collection = "component_setting")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ComponentSettingDocument {
    
    @Id
    private String id;

    private Long componentId; // CHECKLIST 스프링 내부에서 컴포넌트를 구분할 때 사용하므로 Long 타입
    // settingJson 내부에는 "ComponentId" 필드가 있음
    // 이걸 꺼내서 gRPC 요청에 setComponentId("ComponentId") 이런 식으로 넘김
    private String settingJson;
}
