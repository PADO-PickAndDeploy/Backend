package com.pado.backend.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "컴포넌트 타입 정보 DTO (종류 조회용)")
public class ComponentTypeDto {
    @Schema(description = "컴포넌트 세부 타입, Component Entity의 subtype에 해당", example = "MySQL")
    private String subtype;

    @Schema(description = "썸네일 이미지 URL", example = "https://cdn.example.com/mysql.png")
    private String thumbnail;
}
