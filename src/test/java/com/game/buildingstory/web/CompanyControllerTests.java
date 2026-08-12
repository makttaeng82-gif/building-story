package com.game.buildingstory.web;

import com.game.buildingstory.domain.CompanyShortTermProjectRiskResolution;
import com.game.buildingstory.service.CompanyComputeConstructionService;
import com.game.buildingstory.service.CompanyCustomerContractService;
import com.game.buildingstory.service.CompanyDepartmentService;
import com.game.buildingstory.service.CompanyFinanceService;
import com.game.buildingstory.service.CompanyInfrastructureService;
import com.game.buildingstory.service.CompanyIpoService;
import com.game.buildingstory.service.CompanyNewsService;
import com.game.buildingstory.service.CompanyOrganizationService;
import com.game.buildingstory.service.CompanyProductProjectService;
import com.game.buildingstory.service.CompanyReportingService;
import com.game.buildingstory.service.CompanyServiceIncidentService;
import com.game.buildingstory.service.CompanySettlementService;
import com.game.buildingstory.service.CompanyShortTermProjectService;
import com.game.buildingstory.service.CompanyTutorialService;
import com.game.buildingstory.service.CompanyWorkforceService;
import com.game.buildingstory.service.GameService;
import com.game.buildingstory.service.PlayerCompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTests {
    private static final long PLAYER_ID = 41L;

    @Mock private GameService gameService;
    @Mock private PlayerCompanyService playerCompanyService;
    @Mock private CompanyTutorialService companyTutorialService;
    @Mock private CompanySettlementService companySettlementService;
    @Mock private CompanyWorkforceService companyWorkforceService;
    @Mock private CompanyInfrastructureService companyInfrastructureService;
    @Mock private CompanyProductProjectService companyProductProjectService;
    @Mock private CompanyComputeConstructionService companyComputeConstructionService;
    @Mock private CompanyShortTermProjectService companyShortTermProjectService;
    @Mock private CompanyCustomerContractService companyCustomerContractService;
    @Mock private CompanyServiceIncidentService companyServiceIncidentService;
    @Mock private CompanyNewsService companyNewsService;
    @Mock private CompanyReportingService companyReportingService;
    @Mock private CompanyDepartmentService companyDepartmentService;
    @Mock private CompanyOrganizationService companyOrganizationService;
    @Mock private CompanyFinanceService companyFinanceService;
    @Mock private CompanyIpoService companyIpoService;

    @InjectMocks private CompanyController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void companyCommandRequiresAuthenticatedSession() throws Exception {
        mockMvc.perform(post("/companies/projects/product/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(companyProductProjectService);
    }

    @Test
    void bondCommandBindsInputAndBlocksImmediateDuplicateSubmission() throws Exception {
        MockHttpSession session = authenticatedSession();
        when(companyFinanceService.issueBond(PLAYER_ID, 10)).thenReturn("회사채 발행 완료");

        mockMvc.perform(post("/companies/finance/bonds")
                        .session(session)
                        .param("valuationPercent", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main?view=company"))
                .andExpect(flash().attribute("notice", "회사채 발행 완료"));

        mockMvc.perform(post("/companies/finance/bonds")
                        .session(session)
                        .param("valuationPercent", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("notice", "같은 기업 명령의 중복 제출을 차단함"));

        verify(companyFinanceService).issueBond(PLAYER_ID, 10);
    }

    @Test
    void projectRiskCommandBindsEnumAndIdentifier() throws Exception {
        when(companyShortTermProjectService.resolveRisk(
                PLAYER_ID, 7L, CompanyShortTermProjectRiskResolution.EXTRA_SUPPORT
        )).thenReturn("추가 지원 결정");

        mockMvc.perform(post("/companies/projects/short-term/risk")
                        .session(authenticatedSession())
                        .param("projectId", "7")
                        .param("resolution", "EXTRA_SUPPORT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main?view=company"))
                .andExpect(flash().attribute("notice", "추가 지원 결정"));

        verify(companyShortTermProjectService).resolveRisk(
                PLAYER_ID, 7L, CompanyShortTermProjectRiskResolution.EXTRA_SUPPORT
        );
    }

    @Test
    void coreTalentCommandBindsCandidateKey() throws Exception {
        when(companyWorkforceService.hireCoreTalent(PLAYER_ID, "candidate-3"))
                .thenReturn("핵심인재 채용 완료");

        mockMvc.perform(post("/companies/workforce/core-hire")
                        .session(authenticatedSession())
                        .param("candidateKey", "candidate-3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("notice", "핵심인재 채용 완료"));

        verify(companyWorkforceService).hireCoreTalent(PLAYER_ID, "candidate-3");
    }

    @Test
    void newsReadCommandReturnsJsonResult() throws Exception {
        when(companyNewsService.markRead(PLAYER_ID, 12L)).thenReturn(true);

        mockMvc.perform(post("/companies/news/12/read").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        verify(companyNewsService).markRead(PLAYER_ID, 12L);
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.PLAYER_ID, PLAYER_ID);
        return session;
    }
}
