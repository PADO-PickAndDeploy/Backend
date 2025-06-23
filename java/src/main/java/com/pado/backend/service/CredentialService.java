package com.pado.backend.service;

import java.time.LocalDateTime;
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
import com.pado.backend.global.exception.CredentialNotFoundException;
import com.pado.backend.global.exception.UnauthorizedCredentialAccessException;
import com.pado.backend.global.exception.UserNotFoundException;
import com.pado.backend.repository.CredentialRepository;
import com.pado.backend.repository.UserRepository;

// TODO : Vault(크리덴셜 보안) 적용하기
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class CredentialService {


    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional
    public CredentialResponseDto createCredential(CredentialCreateRequestDto request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        Credential credential = Credential.builder()
                .credentialName(request.getName())
                .credentialType(request.getType())
                .credentialDescription(request.getDescription())
                .credentialData(request.getData())
                .user(user)
                .build();

        Credential saved = credentialRepository.save(credential);

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
                .orElseThrow(UserNotFoundException::new);

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
                .orElseThrow(CredentialNotFoundException::new);

        if (!credential.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedCredentialAccessException();
        }

        return new CredentialDetailResponseDto(
                credential.getCredentialId(),
                credential.getCredentialName(),
                credential.getCredentialType(),
                credential.getCredentialDescription(),
                credential.getCredentialData(),
                "크리덴셜 조회 완료",
                credential.getCreatedAt().format(formatter)
        );
    }

    public DefaultResponseDto deleteCredential(Long userId, Long credentialId) {
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(CredentialNotFoundException::new);

        if (!credential.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedCredentialAccessException();
        }

        credentialRepository.delete(credential);
        return new DefaultResponseDto("크리덴셜 삭제 완료");
    }
}
