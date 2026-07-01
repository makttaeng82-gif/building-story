package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventDefinition;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class SettlementService {
    private static final long MONTHLY_JOB_SALARY = 3_000_000L;
    private static final int RECORD_RETENTION_DAYS = 62;
    private static final int MARKET_NEWS_CHANCE_PERCENT = 15;
    public static final String MARKET_NEWS_RISE = "RISE";
    public static final String MARKET_NEWS_FALL = "FALL";

    private final Random random = new Random();
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final GameEventRepository gameEventRepository;
    private final ReputationCatalog reputationCatalog;
    private final SecretaryTenantEventService secretaryTenantEventService;
    private final SecretaryOperationsService secretaryOperationsService;
    private final LoanService loanService;

    public SettlementService(
            OwnedBuildingRepository ownedBuildingRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            GameEventRepository gameEventRepository,
            ReputationCatalog reputationCatalog,
            SecretaryTenantEventService secretaryTenantEventService,
            SecretaryOperationsService secretaryOperationsService,
            LoanService loanService
    ) {
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.gameEventRepository = gameEventRepository;
        this.reputationCatalog = reputationCatalog;
        this.secretaryTenantEventService = secretaryTenantEventService;
        this.secretaryOperationsService = secretaryOperationsService;
        this.loanService = loanService;
    }

    public String runDailySettlement(Player player) {
        ensureMonthlyEventSchedule(player);
        String notice = "";
        if (player.getDay() == 1) {
            notice = processRepairNeglect(player);
            if (player.isEmployed()) {
                player.addSalaryIncome(MONTHLY_JOB_SALARY);
                saveRecord(player, RecordType.SALARY_INCOME, "직장 월급", MONTHLY_JOB_SALARY, 0, null, null);
            }
            int oldReputation = player.getReputation();
            ownedBuildingRepository.findByPlayerOrderById(player).stream()
                    .filter(OwnedBuilding::isOccupied)
                    .forEach(building -> {
                        if (secretaryTenantEventService.isRentWaived(player, building)) {
                            saveRecord(player, RecordType.RENT_INCOME, "월세 감면", 0L, 0, building.getName(), secretaryTenantEventService.statusText(building));
                            return;
                        }
                        long rent = effectiveMonthlyRent(player, building);
                        player.addMonthlyRentIncome(rent);
                        int reputationChange = random.nextInt(5) + 1;
                        player.addReputation(reputationChange);
                        saveRecord(player, RecordType.RENT_INCOME, "월세", rent, reputationChange, building.getName(), null);
                    });
            String secretaryReputationNotice = secretaryOperationsService.processMonthlyReputation(player);
            notice = appendNotice(notice, secretaryReputationNotice);
            reputationCatalog.newlyUnlocked(oldReputation, player.getReputation(), !player.isEmployed()).stream()
                    .findFirst()
                    .ifPresent(tier -> activateUnlockEvent(player, tier));
        }
        if (player.getDay() == 15) {
            secretaryOperationsService.processSalaries(player);
        }
        if (player.getDay() == 20) {
            String loanNotice = loanService.processMaturity(player);
            notice = appendNotice(notice, loanNotice);
        }
        String eventNotice = runMonthlyRandomBuildingEvent(player);
        if (!eventNotice.isBlank()) {
            notice = notice.isBlank() ? eventNotice : notice + " · " + eventNotice;
        }
        String secretaryNotice = secretaryOperationsService.processAutoRepairs(player);
        notice = appendNotice(notice, secretaryNotice);
        return notice;
    }

    public void clearVacantRepairRequests(Player player) {
        ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .filter(building -> !building.isOccupied())
                .filter(OwnedBuilding::isRepairRequested)
                .forEach(OwnedBuilding::clearRepairRequest);
    }

    @Transactional(readOnly = true)
    public long totalMonthlyRent(Player player) {
        return ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .filter(OwnedBuilding::isOccupied)
                .mapToLong(building -> effectiveMonthlyRent(player, building))
                .sum();
    }

    @Transactional(readOnly = true)
    public long effectiveMonthlyRent(Player player, OwnedBuilding building) {
        if (secretaryTenantEventService.isRentWaived(player, building)) {
            return 0;
        }
        return applyPercentBonus(building.getMonthlyRent(), secretaryOperationsService.rentBonusPercent(player, building.getCity()));
    }

    @Transactional(readOnly = true)
    public double moveInChancePercent(Player player, String city) {
        return clampPercent(player.getMoveInChancePercent() + secretaryOperationsService.moveInBonusPercent(player, city));
    }

    @Transactional(readOnly = true)
    public double moveOutChancePercent(Player player, String city) {
        return clampPercent(player.getMoveOutChancePercent() - secretaryOperationsService.moveOutReductionPercent(player, city));
    }

    @Transactional(readOnly = true)
    public double repairRequestChancePercent(Player player, String city) {
        return clampPercent(player.getRepairRequestChancePercent() - secretaryOperationsService.repairRequestReductionPercent(player, city));
    }

    private void ensureMonthlyEventSchedule(Player player) {
        if (player.hasEventScheduleForCurrentMonth()) {
            ensureMonthlyMarketNewsSchedule(player);
            return;
        }
        int firstMoveInDay = randomEventDay(player);
        int secondMoveInDay = randomDistinctEventDay(player, firstMoveInDay);
        int firstMoveOutDay = randomEventDay(player);
        int secondMoveOutDay = randomDistinctEventDay(player, firstMoveOutDay);
        int firstRepairDay = randomEventDay(player);
        int secondRepairDay = randomDistinctEventDay(player, firstRepairDay);
        player.scheduleMonthlyRandomEvents(firstMoveInDay, secondMoveInDay, firstMoveOutDay, secondMoveOutDay, firstRepairDay, secondRepairDay);
        ensureMonthlyMarketNewsSchedule(player);
    }

    private void ensureMonthlyMarketNewsSchedule(Player player) {
        if (player.hasMarketNewsScheduleForCurrentMonth()) {
            return;
        }
        if (!rollPercent(MARKET_NEWS_CHANCE_PERCENT)) {
            player.scheduleNoMonthlyMarketNews();
            return;
        }
        String trend = random.nextBoolean() ? MARKET_NEWS_RISE : MARKET_NEWS_FALL;
        player.scheduleMonthlyMarketNews(randomEventDay(player), player.getCurrentCity(), trend);
    }

    private int randomEventDay(Player player) {
        return random.nextInt(player.getDaysInCurrentMonth() - 1) + 2;
    }

    private int randomDistinctEventDay(Player player, int usedDay) {
        int day = randomEventDay(player);
        while (day == usedDay) {
            day = randomEventDay(player);
        }
        return day;
    }

    private String runMonthlyRandomBuildingEvent(Player player) {
        String notice = "";
        if (player.isMarketNewsEventDay()) {
            notice = activateMarketNews(player);
        }
        if (player.isMoveInEventDay()) {
            notice = appendNotice(notice, attemptMoveIn(player));
        }
        if (player.isMoveOutEventDay()) {
            String moveOutNotice = attemptMoveOut(player);
            notice = appendNotice(notice, moveOutNotice);
        }
        if (player.isRepairEventDay()) {
            String repairNotice = attemptRepairRequest(player);
            notice = appendNotice(notice, repairNotice);
        }
        return notice;
    }

    private String activateMarketNews(Player player) {
        if (activeEvent(player).isPresent()) {
            return "";
        }
        String trend = player.getMarketNewsEventTrend();
        String city = player.getMarketNewsEventCity();
        player.activateMarketNews();
        String trendLabel = MARKET_NEWS_RISE.equals(trend) ? "폭등" : "폭락";
        saveRecord(player, RecordType.BUILDING_BUY, "부동산 " + trendLabel + " 뉴스", null, 0, city, "다음 매물갱신 2회 적용");
        gameEventRepository.save(new GameEvent(
                player,
                "market_news_" + player.getId() + "_" + player.getElapsedDays() + "_" + trend,
                city + " 부동산 " + trendLabel + " 뉴스",
                MARKET_NEWS_RISE.equals(trend)
                        ? "투자 수요가 몰리며 매물 평가가 2회 동안 고평가 쪽으로 기웁니다."
                        : "시장 불안이 커지며 매물 평가가 2회 동안 저평가 쪽으로 기웁니다.",
                marketNewsImagePath(city, trend),
                "NONE",
                "확인"
        ));
        player.pause();
        return city + " 부동산 " + trendLabel + " 뉴스";
    }

    public String activateMarketNewsForTest(Player player, String trend) {
        player.scheduleMonthlyMarketNews(player.getDay(), player.getCurrentCity(), trend);
        return activateMarketNews(player);
    }

    private String attemptMoveIn(Player player) {
        var movedInBuildings = ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .filter(building -> !building.isOccupied())
                .filter(building -> rollPercent(moveInChancePercent(player, building.getCity())))
                .toList();
        if (movedInBuildings.isEmpty()) {
            return "";
        }
        movedInBuildings.forEach(building -> {
            building.moveIn(player.getElapsedDays());
            saveRecord(player, RecordType.MOVE_IN, "세입자 입주", null, 0, building.getName(), "60일 퇴거보호");
        });
        return movedInBuildings.size() + "채 입주 완료";
    }

    private String attemptMoveOut(Player player) {
        var movedOutBuildings = ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .filter(building -> building.canTenantMoveOut(player.getElapsedDays()))
                .filter(building -> rollPercent(moveOutChancePercent(player, building.getCity())))
                .toList();
        if (movedOutBuildings.isEmpty()) {
            return "";
        }
        int defendedCount = 0;
        int movedOutCount = 0;
        for (OwnedBuilding building : movedOutBuildings) {
            if (secretaryOperationsService.defendMoveOut(player, building)) {
                defendedCount++;
                continue;
            }
            building.moveOut();
            saveRecord(player, RecordType.MOVE_OUT, "세입자 퇴거", null, 0, building.getName(), null);
            movedOutCount++;
        }
        String notice = movedOutCount == 0 ? "" : movedOutCount + "채 세입자 퇴거";
        if (defendedCount > 0) {
            notice = appendNotice(notice, defendedCount + "채 퇴거방어");
        }
        return notice;
    }

    private String attemptRepairRequest(Player player) {
        var repairRequestedBuildings = ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .filter(OwnedBuilding::isOccupied)
                .filter(building -> !building.isRepairRequested())
                .filter(building -> rollPercent(repairRequestChancePercent(player, building.getCity())))
                .toList();
        if (repairRequestedBuildings.isEmpty()) {
            return "";
        }
        repairRequestedBuildings.forEach(building -> {
            building.requestRepair();
            saveRecord(player, RecordType.REPAIR_REQUEST, "수리요청 발생", null, 0, building.getName(), null);
        });
        String secretaryNotice = secretaryOperationsService.processAutoRepairs(player);
        return appendNotice(repairRequestedBuildings.size() + "채 수리요청 발생", secretaryNotice);
    }

    private String processRepairNeglect(Player player) {
        String notice = "";
        for (OwnedBuilding building : ownedBuildingRepository.findByPlayerOrderById(player)) {
            building.advanceRepairNeglectMonth();
            if (building.getRepairNeglectedMonths() >= 2 && building.canTenantMoveOut(player.getElapsedDays())) {
                building.moveOut();
                saveRecord(player, RecordType.MOVE_OUT, "수리 방치 퇴거", null, 0, building.getName(), null);
                notice = appendNotice(notice, building.getName() + " 수리 방치로 퇴거");
            }
        }
        return notice;
    }

    private void activateUnlockEvent(Player player, ReputationTier tier) {
        if (activeEvent(player).isPresent()) {
            return;
        }
        if ("서울".equals(tier.unlockCity()) && tier.unlockBuildingSlot() >= 1) {
            player.scheduleStockUnlock(player.getElapsedDays() + 2);
        }
        gameEventRepository.save(new GameEvent(
                player,
                new GameEventDefinition(
                        "unlock_" + tier.unlockCity() + "_" + tier.unlockBuildingSlot() + "_" + player.getReputation(),
                        player.getMonth(),
                        player.getDay(),
                        "새 건물이 해금되었습니다",
                        tier.unlockLabel() + " 매물을 거래할 수 있습니다.",
                        "AI 해금 이미지",
                        "NONE"
                )
        ));
        player.pause();
    }

    private Optional<GameEvent> activeEvent(Player player) {
        return gameEventRepository.findLatestByPlayerIdAndStatus(player.getId(), GameEventStatus.ACTIVE, PageRequest.of(0, 1)).stream().findFirst();
    }

    private boolean rollPercent(double percent) {
        return random.nextInt(100) < percent;
    }

    private long applyPercentBonus(long baseAmount, double percent) {
        return Math.max(0L, (long) Math.floor(baseAmount * (100.0 + percent) / 100.0));
    }

    private double clampPercent(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private String marketNewsImagePath(String city, String trend) {
        return "/assets/news/" + citySlug(city) + "-" + (MARKET_NEWS_RISE.equals(trend) ? "rise" : "fall") + ".jpg";
    }

    private String citySlug(String city) {
        return switch (city) {
            case "\uCCAD\uC8FC" -> "cheongju";
            case "\uC138\uC885" -> "sejong";
            case "\uB300\uC804" -> "daejeon";
            case "\uBD80\uC0B0" -> "busan";
            case "\uC778\uCC9C" -> "incheon";
            case "\uC11C\uC6B8" -> "seoul";
            default -> "cheongju";
        };
    }

    private String appendNotice(String base, String addition) {
        if (addition == null || addition.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return addition;
        }
        return base + " · " + addition;
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
