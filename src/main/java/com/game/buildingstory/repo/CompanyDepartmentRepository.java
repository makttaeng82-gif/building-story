package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyDepartmentRepository extends JpaRepository<CompanyDepartment, Long> {
    List<CompanyDepartment> findByCompanyOrderById(PlayerCompany company);
    Optional<CompanyDepartment> findByCompanyAndDepartmentType(PlayerCompany company, CompanyDepartmentType departmentType);
}
