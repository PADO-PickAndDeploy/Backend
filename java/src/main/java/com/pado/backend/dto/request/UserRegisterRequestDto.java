package com.pado.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequestDto {
    // TODO : userName 쓸건가?
    @NotBlank(message = "사용자 이름 입력은 필수입니다.")
    @Size(min = 3, max = 20, message = "사용자 이름은 3~20자 사이여야 합니다.")
    @Pattern(
        regexp = "^[A-Za-z0-9]+$",
        message = "사용자 이름은 영문과 숫자만 사용 가능합니다."
    )
    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;

    @NotBlank(message = "비밀번호 입력은 필수입니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다."
    )
    @Schema(description = "비밀번호", example = "pado123!")
    private String password;

    @NotBlank(message = "이메일 입력은 필수입니다.")
    @Email(message = "이메일이 올바르지 않습니다.")
    @Schema(description = "이메일", example = "pado@example.com")
    private String email;
}
