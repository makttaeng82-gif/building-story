package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 서울 최종 건물 구매 이후 기업 설립 제안과 기업 화면 접근을 관리한다. */
@Service
@Transactional
public class CompanyAccessService {
    public static final int PROPOSAL_DELAY_DAYS = 180;
    public static final String FOUNDATION_UNLOCK_EFFECT = "COMPANY_FOUNDATION_UNLOCK";

    private final OwnedBuildingRepository ownedBuildingRepository;
    private final PlayerCompanyRepository playerCompanyRepository;
    private final GameEventRepository gameEventRepository;
    private final PlayerRepository playerRepository;

    public CompanyAccessService(
            OwnedBuildingRepository ownedBuildingRepository,
            PlayerCompanyRepository playerCompanyRepository,
            GameEventRepository gameEventRepository,
            PlayerRepository playerRepository
    ) {
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.playerCompanyRepository = playerCompanyRepository;
        this.gameEventRepository = gameEventRepository;
        this.playerRepository = playerRepository;
    }

    public void ensureUnlockSchedule(Player player) {
        if (isUnlocked(player) || player.hasCompanyUnlockSchedule()) {
            return;
        }
        ownedBuildingRepository.findByPlayerAndCityOrderById(player, "서울").stream()
                .filter(this::isFinalSeoulBuilding)
                .mapToInt(building -> building.getPurchaseDayCountForCalculation(player.getElapsedDays()))
                .min()
                .ifPresent(purchaseDay -> {
                    player.scheduleCompanyUnlock(purchaseDay + PROPOSAL_DELAY_DAYS);
                    playerRepository.save(player);
                });
    }

    public boolean activateProposalIfDue(Player player) {
        ensureUnlockSchedule(player);
        if (!player.isCompanyUnlockDue() || player.isCompanyUnlockNoticeShown()) {
            return false;
        }
        if (gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE).isPresent()) {
            return false;
        }
        gameEventRepository.save(new GameEvent(
                player,
                "company_foundation_offer_" + player.getId(),
                "AI 기업 설립 제안",
                "서울의 부동산 사업이 안정 단계에 들어섰습니다. 개인 자본을 출자해 비상장 AI 기업 설립을 준비할 수 있습니다.",
                "AI 기업 설립 제안",
                FOUNDATION_UNLOCK_EFFECT,
                "설립 준비하기"
        ));
        player.markCompanyUnlockNoticeShown();
        player.pause();
        return true;
    }

    public void acceptFoundationProposal(Player player) {
        player.unlockCompanyContent();
    }

    @Transactional(readOnly = true)
    public boolean isUnlocked(Player player) {
        return player.isCompanyContentUnlocked() || playerCompanyRepository.findByPlayer(player).isPresent();
    }

    @Transactional(readOnly = true)
    public String statusText(Player player) {
        if (isUnlocked(player)) {
            return "개방";
        }
        if (!player.hasCompanyUnlockSchedule()) {
            return "서울 최종 건물 필요";
        }
        int daysLeft = player.companyUnlockDaysLeft();
        return daysLeft == 0 ? "설립 제안 대기" : "설립 제안 D-" + daysLeft;
    }

    private boolean isFinalSeoulBuilding(OwnedBuilding building) {
        return Integer.valueOf(4).equals(building.getBuildingSlot());
    }
}
