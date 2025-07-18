package com.pado.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CredentialCreateRequestDto {
    @Schema(description = "크레덴셜 이름", example = "AWS IAM Key")
    private String name;

    @Schema(description = "크레덴셜 설명", example = "IAM 역할을 위한 인증키입니다.")
    private String description;

    @Schema(description = "크레덴셜 타입", example = "AWS")
    private String type;

    // TODO : data에 ID, token을 JSON 형태로 저장할건지 or accountId, token으로 나눌건지 결정
    // TODO : 현재 평문으로 저장되어 있는 상태임. 보안 매우 위험
    @Schema(description = "크레덴셜 실제 데이터(ID / Token)", example = "AKIAIOSFODNN7EXAMPLE/secret")
    private String data;
}
