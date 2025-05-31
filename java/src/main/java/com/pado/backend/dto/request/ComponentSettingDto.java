package com.pado.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComponentSettingDto {

    @Schema(description = "Credential ID (AWS 또는 Git 자격 증명 ID)", example = "103")
    private Long credentialId;

    @Schema(description = "컴포넌트 설정 정보 (gRPC 요청용 JSON 문자열)", 
    example = "{ \"EC2\": { \"InstanceType\": \"t2.micro\", ... }, \"Spring\": { ... }, \"DeploymentId\": \"deployment-001\" }")
    private String settingJson;
}
