package com.pado.backend.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@EntityListeners(AuditingEntityListener.class)
public class ComponentLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long linkId;  // [x] 기존 componentId → linkId로 변경

    @Enumerated(EnumType.STRING)
    private ConnectionType connectionType;

    // [x] : createdAt
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 연결 시작점 (from)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_component_id")
    private Component fromComponentId;

    // 연결 끝점 (to)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_component_id")
    private Component toComponentId;

    public enum ConnectionType {
        HTTP,
        DB,
        SSH,
        INTERNAL
    }
}
