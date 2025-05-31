package com.pado.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Credential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long credentialId;

    private String credentialName; // 크리덴셜 이름 사용자 정의

    // TODO : JSON 형식으로 accessKey, secretKey 다 담을지, 아니면 필드를 두개 나누어 각자 담을지
    private String credentialData; // 실제 키 

    private String credentialType; // AWS, Git -> 프론트에서 AWS, Git, Custom 클릭 시에 결정

    private String credentialDescription; // 크리덴셜 정보 사용자 정의

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
