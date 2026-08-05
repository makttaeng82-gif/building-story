package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyCoreEmployee;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyCoreEmployeeRepository extends JpaRepository<CompanyCoreEmployee, Long> {
    List<CompanyCoreEmployee> findByCompanyOrderById(PlayerCompany company);
}
