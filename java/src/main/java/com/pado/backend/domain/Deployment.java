package com.pado.backend.domain;

import java.time.LocalDateTime;

import com.pado.backend.global.type.DeploymentStatus;

import jakarta.persistence.Entity;
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
public class Deployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deploymentId;

    @Enumerated(EnumType.STRING)
    private DeploymentStatus status;

    private LocalDateTime startTime;

    private LocalDateTime stopTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    public void markAsStart(LocalDateTime now) {
        this.status = DeploymentStatus.START;
        this.startTime = now;
        this.stopTime = null;
    }

    public void markAsError(LocalDateTime now) {
        this.status = DeploymentStatus.ERROR;
        this.stopTime = now;
    }

    public void markAsRunning(LocalDateTime now) {
        this.status = DeploymentStatus.RUNNING;
        this.stopTime = null;
    }

}

