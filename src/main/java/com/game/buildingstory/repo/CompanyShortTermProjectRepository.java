package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyShortTermProject;
import com.game.buildingstory.domain.CompanyShortTermProjectStatus;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyShortTermProjectRepository extends JpaRepository<CompanyShortTermProject, Long> {
    List<CompanyShortTermProject> findByCompanyOrderByIdDesc(PlayerCompany company);

    Optional<CompanyShortTermProject> findFirstByCompanyAndStatusOrderByIdDesc(
            PlayerCompany company,
            CompanyShortTermProjectStatus status
    );
}
