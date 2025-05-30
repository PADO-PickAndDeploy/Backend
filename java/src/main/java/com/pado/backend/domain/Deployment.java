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
public class Deployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deploymentId;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime stopTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // RUNNING에서 배포 중단 성공적으로 한 경우
    public void markAsStart(LocalDateTime now) {
        this.status = "START";
        this.stopTime = now;
    }

    // RUNNING에서 배포 중단 실패한 경우
    public void markAsError(LocalDateTime now) {
        this.status = "ERROR";
        this.stopTime = now;
    }

    public void markAsRunning(LocalDateTime now) {
        this.status = "RUNNING";
        this.startTime = now;
        this.stopTime = now;
    }

}

