package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyProductProject;
import com.game.buildingstory.domain.CompanyProductProjectStatus;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyProductProjectRepository extends JpaRepository<CompanyProductProject, Long> {
    Optional<CompanyProductProject> findFirstByCompanyAndStatusOrderByIdDesc(
            PlayerCompany company,
            CompanyProductProjectStatus status
    );

    List<CompanyProductProject> findByCompanyOrderByIdDesc(PlayerCompany company);
}
