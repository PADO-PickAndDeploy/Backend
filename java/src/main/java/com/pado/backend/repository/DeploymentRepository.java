package com.pado.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pado.backend.domain.Deployment;
import com.pado.backend.domain.Project;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long>{
    public List<Deployment> findByProject(Project project);
    // TODO : Optional은 단건 조회, List는 다건 조회에 이용 / 프로젝트 내 배포는 0~1개 이므로 단건 조회가 적합
    public Optional<Deployment> findByProjectAndStatus(Project project, String string);
}
