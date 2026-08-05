package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** 기업 비서의 고정 역할, 기업 경력과 성장형 패시브를 계산한다. */
@Service
public class CompanySecretaryService {
    private static final String CHIEF_SECRETARY_KEY = "secretary-6";
    private static final int CHIEF_EFFECT_PERCENT_PER_LEVEL = 3;

    private final OwnedSecretaryRepository secretaryRepository;
    private final CompanyDepartmentRepository departmentRepository;

    public CompanySecretaryService(
            OwnedSecretaryRepository secretaryRepository,
            CompanyDepartmentRepository departmentRepository
    ) {
        this.secretaryRepository = secretaryRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public void processSuccessfulMonth(PlayerCompany company) {
        secretaryRepository.findByPlayerOrderById(company.getPlayer()).forEach(secretary -> {
            Optional<CompanyDepartmentType> departmentType = departmentType(secretary.getSecretaryKey());
            if (CHIEF_SECRETARY_KEY.equals(secretary.getSecretaryKey())
                    || departmentType.filter(type -> departmentRepository
                            .findByCompanyAndDepartmentType(company, type).isPresent()).isPresent()) {
                secretary.advanceCompanyCareerMonth();
            }
        });
    }

    @Transactional(readOnly = true)
    public Optional<OwnedSecretary> secretary(PlayerCompany company, CompanyDepartmentType type) {
        String key = secretaryKey(type);
        if (departmentRepository.findByCompanyAndDepartmentType(company, type).isEmpty()) {
            return Optional.empty();
        }
        return secretaryRepository.findByPlayerOrderById(company.getPlayer()).stream()
                .filter(secretary -> key.equals(secretary.getSecretaryKey()))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public double developmentWorkMultiplier(PlayerCompany company) {
        return 1.0 - effectPercent(company, CompanyDepartmentType.AI_DEVELOPMENT, 4) / 100.0;
    }

    @Transactional(readOnly = true)
    public double marketingEffectMultiplier(PlayerCompany company) {
        return 1.0 + effectPercent(company, CompanyDepartmentType.SALES_MARKETING, 5) / 100.0;
    }

    @Transactional(readOnly = true)
    public double incidentProbabilityMultiplier(PlayerCompany company) {
        return 1.0 - effectPercent(company, CompanyDepartmentType.SERVICE_OPERATIONS, 4) / 100.0;
    }

    @Transactional(readOnly = true)
    public double employeeGrowthMultiplier(PlayerCompany company) {
        return (1.0 + effectPercent(company, CompanyDepartmentType.HR_ORGANIZATION, 5) / 100.0)
                * chiefEfficiencyMultiplier(company);
    }

    @Transactional(readOnly = true)
    public double forecastErrorMultiplier(PlayerCompany company) {
        return 1.0 - effectPercent(company, CompanyDepartmentType.STRATEGY_FINANCE, 10) / 100.0;
    }

    @Transactional(readOnly = true)
    public double chiefEfficiencyMultiplier(PlayerCompany company) {
        return 1.0 + chiefEffectPercent(company) / 100.0;
    }

    @Transactional(readOnly = true)
    public int chiefEffectPercent(PlayerCompany company) {
        return chiefSecretary(company)
                .map(secretary -> secretary.getCompanyProficiencyLevel() * CHIEF_EFFECT_PERCENT_PER_LEVEL)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int effectPercent(PlayerCompany company, CompanyDepartmentType type) {
        int perLevel = switch (type) {
            case AI_DEVELOPMENT, SERVICE_OPERATIONS -> 4;
            case SALES_MARKETING, HR_ORGANIZATION -> 5;
            case STRATEGY_FINANCE -> 10;
        };
        return effectPercent(company, type, perLevel);
    }

    public String secretaryKey(CompanyDepartmentType type) {
        return switch (type) {
            case AI_DEVELOPMENT -> "secretary-2";
            case SALES_MARKETING -> "secretary-3";
            case SERVICE_OPERATIONS -> "secretary-1";
            case HR_ORGANIZATION -> "secretary-4";
            case STRATEGY_FINANCE -> "secretary-5";
        };
    }

    private int effectPercent(PlayerCompany company, CompanyDepartmentType type, int perLevel) {
        return secretary(company, type)
                .map(secretary -> secretary.getCompanyProficiencyLevel() * perLevel)
                .orElse(0);
    }

    private Optional<OwnedSecretary> chiefSecretary(PlayerCompany company) {
        return secretaryRepository.findByPlayerOrderById(company.getPlayer()).stream()
                .filter(secretary -> CHIEF_SECRETARY_KEY.equals(secretary.getSecretaryKey()))
                .findFirst();
    }

    private Optional<CompanyDepartmentType> departmentType(String secretaryKey) {
        for (CompanyDepartmentType type : CompanyDepartmentType.values()) {
            if (secretaryKey(type).equals(secretaryKey)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
