package com.pado.backend.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pado.backend.domain.Credential;
import com.pado.backend.domain.User;
import com.pado.backend.dto.request.CredentialCreateRequestDto;
import com.pado.backend.dto.response.CredentialDetailResponseDto;
import com.pado.backend.dto.response.CredentialResponseDto;
import com.pado.backend.dto.response.DefaultResponseDto;
import com.pado.backend.global.exception.CustomException;
import com.pado.backend.global.exception.ErrorCode;
import com.pado.backend.global.vault.service.CredentialVaultService;
import com.pado.backend.repository.CredentialRepository;
import com.pado.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

// [ ] : Vault(크리덴셜 보안) 적용하기, DB에 저장하지 않고 Vault에 저장
@Service
@RequiredArgsConstructor
public class CredentialService {


    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final CredentialVaultService credentialVaultService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional
    public CredentialResponseDto createCredential(CredentialCreateRequestDto request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Vault 키 생성
        String credentialVaultKey = credentialVaultService.generateCredentialVaultKey();

        Credential credential = Credential.builder()
                .credentialName(request.getName())
                .credentialType(request.getType())
                .credentialDescription(request.getDescription())
                // .credentialData(request.getData())
                .vaultKey(credentialVaultKey) // vaultKey 저장
                .user(user)
                .build();

        Credential saved = credentialRepository.save(credential);

        // Vault에 실제 credentialData 저장
        credentialVaultService.storeCredential(user, saved, request.getData());

        return new CredentialResponseDto(
                saved.getCredentialId(),
                saved.getCredentialName(),
                saved.getCredentialType(),
                saved.getCredentialDescription(),
                "크리덴셜 등록 완료",
                saved.getCreatedAt().format(formatter)
        );
    }

    @Transactional(readOnly = true)
    public List<CredentialResponseDto> getAllCredentials(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return credentialRepository.findByUser(user).stream()
                .map(c -> new CredentialResponseDto(
                        c.getCredentialId(),
                        c.getCredentialName(),
                        c.getCredentialType(),
                        c.getCredentialDescription(),
                        "크리덴셜 조회 완료",
                        c.getCreatedAt().format(formatter)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CredentialDetailResponseDto getCredential(Long userId, Long credentialId) {
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new CustomException(ErrorCode.CREDENTIAL_NOT_FOUND));

        if (!credential.getUser().getUserId().equals(userId)) {
                throw new CustomException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }

        // Vault에서 민감 정보 조회
        String credentialData = credentialVaultService.getCredentialData(credential.getUser(), credential);

        return new CredentialDetailResponseDto(
                credential.getCredentialId(),
                credential.getCredentialName(),
                credential.getCredentialType(),
                credential.getCredentialDescription(),
                // credential.getCredentialData(),
                credentialData,
                "크리덴셜 조회 완료",
                credential.getCreatedAt().format(formatter)
        );
    }

    @Transactional
    public DefaultResponseDto deleteCredential(Long userId, Long credentialId) {
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new CustomException(ErrorCode.CREDENTIAL_NOT_FOUND));

        if (!credential.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }

        // Vault에서 해당 크리덴셜 삭제
        credentialVaultService.deleteCredential(credential.getUser(), credential);

        credentialRepository.delete(credential);
        return new DefaultResponseDto("크리덴셜 삭제 완료");
    }
}       
