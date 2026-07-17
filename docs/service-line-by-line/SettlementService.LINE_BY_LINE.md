# SettlementService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/SettlementService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
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
// 해설: 하루가 지날 때 자동으로 발생하는 경제 정산과 월간 랜덤 이벤트를 담당하는 서비스다.
    /*
     * 하루가 지날 때 발생하는 정산과 월간 이벤트를 담당한다.
     *
     * GameService.tick()이 날짜를 하루 증가시킨 뒤 이 서비스를 호출한다.
     * 월초 월세/월급/대출 상환, 수리 방치 패널티, 부동산 뉴스 예약처럼
     * "시간이 지나면 자동으로 일어나는 일"이 이곳에 모여 있다.
     */
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
    // 해설: 정산에 필요한 보유 건물, 월간 기록, 이벤트, 평판, 비서, 대출 서비스를 생성자 주입으로 받는다.
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
    // 해설: GameService.tick에서 날짜가 하루 증가한 뒤 호출되는 일일 정산의 핵심 진입점이다.
        // 랜덤 입주/퇴거/수리 이벤트 날짜는 매달 한 번만 확정한다.
        ensureMonthlyEventSchedule(player);
        // 해설: 이번 달 입주/퇴거/수리/부동산 뉴스 이벤트 날짜가 없으면 먼저 예약한다.
        String notice = "";
        // 해설: 하루 진행 결과를 사용자에게 보여줄 안내 문구를 누적할 변수다.
        if (player.getDay() == 1) {
        // 해설: 매월 1일에만 월급, 월세, 수리 방치, 해금 같은 월초 정산을 실행한다.
            // 월초에는 지난 달 누적 상태를 기록하고, 이번 달 고정 수입/지출을 반영한다.
            notice = processRepairNeglect(player);
            // 해설: 수리 요청을 오래 방치한 건물이 있으면 퇴거 처리하고 안내 문구를 받는다.
            if (player.isEmployed()) {
            // 해설: 플레이어가 아직 직장에 다니는 상태라면 월급을 지급한다.
                player.addSalaryIncome(MONTHLY_JOB_SALARY);
                // 해설: 월급 금액을 현금과 월급 누적 수입에 반영한다.
                saveRecord(player, RecordType.SALARY_INCOME, "직장 월급", MONTHLY_JOB_SALARY, 0, null, null);
                // 해설: 월급 지급 내역을 월간 기록에 남긴다.
            }
            int oldReputation = player.getReputation();
            // 해설: 월세 정산 전 평판을 저장한다. 정산 후 새로 해금된 티어를 비교하기 위해 필요하다.
            ownedBuildingRepository.findByPlayerOrderById(player).stream()
            // 해설: 플레이어 보유 건물을 id 순서로 가져와 월세 대상 건물을 찾는다.
                    .filter(OwnedBuilding::isOccupied)
                    // 해설: 세입자가 있는 건물만 수리 요청 후보가 된다.
                    .forEach(building -> {
                    // 해설: 월세 대상 건물을 하나씩 정산한다.
                        if (secretaryTenantEventService.isRentWaived(player, building)) {
                        // 해설: 비서 세입자 이벤트 효과로 임대료가 면제되는 건물인지 확인한다.
                            saveRecord(player, RecordType.RENT_INCOME, "월세 감면", 0L, 0, building.getName(), secretaryTenantEventService.statusText(building));
                            // 해설: 임대료가 0원으로 감면됐다는 기록을 남기고, 감면 사유를 메모에 넣는다.
                            return;
                            // 해설: 이 건물의 월세 지급은 감면 처리로 끝났으므로 다음 건물로 넘어간다.
                        }
                        long rent = effectiveMonthlyRent(player, building);
                        // 해설: 비서 보너스를 반영한 실제 월세 수입을 계산한다.
                        player.addMonthlyRentIncome(rent);
                        // 해설: 계산된 월세를 플레이어 현금과 월세 누적 수입에 반영한다.
                        int reputationChange = random.nextInt(5) + 1;
                        // 해설: 월세를 받은 건물마다 평판 증가량을 1~5 사이에서 무작위로 정한다.
                        player.addReputation(reputationChange);
                        // 해설: 월세 수익에 따른 평판 상승을 반영한다.
                        saveRecord(player, RecordType.RENT_INCOME, "월세", rent, reputationChange, building.getName(), null);
                        // 해설: 월세 수입과 평판 상승을 월간 기록에 저장한다.
                    });
            String secretaryReputationNotice = secretaryOperationsService.processMonthlyReputation(player);
            // 해설: 비서 시스템의 월간 평판 효과를 처리하고 안내 문구를 받는다.
            notice = appendNotice(notice, secretaryReputationNotice);
            // 해설: 기존 안내 문구에 비서 평판 안내를 합친다.
            reputationCatalog.newlyUnlocked(oldReputation, player.getReputation(), !player.isEmployed()).stream()
            // 해설: 월초 정산 전후 평판을 비교해 새로 해금된 티어가 있는지 찾는다.
                    .findFirst()
                    // 해설: 여러 해금이 있어도 한 번에 하나의 해금 이벤트만 띄운다.
                    .ifPresent(tier -> activateUnlockEvent(player, tier));
                    // 해설: 새 해금 티어가 있으면 해금 이벤트를 활성화한다.
        }
        if (player.getDay() == 15) {
        // 해설: 매월 15일에는 비서 급여 처리를 실행한다.
            secretaryOperationsService.processSalaries(player);
            // 해설: 고용한 비서들의 급여 지급이나 관련 정산을 처리한다.
        }
        if (player.getDay() == 20) {
        // 해설: 매월 20일에는 대출 만기 처리를 실행한다.
            String loanNotice = loanService.processMaturity(player);
            // 해설: 만기 대출 상환/처리 결과 안내 문구를 받는다.
            notice = appendNotice(notice, loanNotice);
        }
        String eventNotice = runMonthlyRandomBuildingEvent(player);
        // 해설: 오늘 예약된 랜덤 건물 이벤트가 있는지 확인하고 실행한다.
        if (!eventNotice.isBlank()) {
        // 해설: 랜덤 이벤트 안내 문구가 있으면 기존 정산 안내에 붙인다.
            notice = notice.isBlank() ? eventNotice : notice + " · " + eventNotice;
        }
        String secretaryNotice = secretaryOperationsService.processAutoRepairs(player);
        // 해설: 수리 요청 직후 비서 자동 수리가 가능한지 처리한다.
        notice = appendNotice(notice, secretaryNotice);
        return notice;
        // 해설: 하루 정산에서 발생한 안내 문구를 GameService로 반환한다.
    }

    public void clearVacantRepairRequests(Player player) {
    // 해설: 공실 건물에 남아 있는 수리 요청을 정리한다.
        ownedBuildingRepository.findByPlayerOrderById(player).stream()
        // 해설: 플레이어 보유 건물을 id 순서로 가져와 월세 대상 건물을 찾는다.
                .filter(building -> !building.isOccupied())
                // 해설: 이미 세입자가 있는 건물은 입주 후보에서 제외한다.
                .filter(OwnedBuilding::isRepairRequested)
                // 해설: 수리 요청 상태인 건물만 남긴다.
                .forEach(OwnedBuilding::clearRepairRequest);
                // 해설: 공실 건물의 수리 요청 상태를 제거한다.
    }

    @Transactional(readOnly = true)
    public long totalMonthlyRent(Player player) {
    // 해설: 현재 보유 건물에서 받을 수 있는 월세 총합을 계산한다.
        return ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .filter(OwnedBuilding::isOccupied)
                // 해설: 세입자가 있는 건물만 수리 요청 후보가 된다.
                .mapToLong(building -> effectiveMonthlyRent(player, building))
                // 해설: 각 건물의 실제 월세를 long 값으로 변환한다.
                .sum();
                // 해설: 건물별 월세를 모두 더한다.
    }

    @Transactional(readOnly = true)
    public long effectiveMonthlyRent(Player player, OwnedBuilding building) {
    // 해설: 비서 감면과 비서 월세 보너스를 반영한 실제 월세를 계산한다.
        if (secretaryTenantEventService.isRentWaived(player, building)) {
        // 해설: 비서 세입자 이벤트 효과로 임대료가 면제되는 건물인지 확인한다.
            return 0;
            // 해설: 임대료 감면 효과가 있으면 실제 월세는 0원이다.
        }
        return applyPercentBonus(building.getMonthlyRent(), secretaryOperationsService.rentBonusPercent(player, building.getCity()));
        // 해설: 기본 월세에 해당 도시 비서의 월세 보너스 퍼센트를 적용한다.
    }

    @Transactional(readOnly = true)
    public double moveInChancePercent(Player player, String city) {
    // 해설: 세입자 입주 확률에 비서 보너스를 반영한다.
        return clampPercent(player.getMoveInChancePercent() + secretaryOperationsService.moveInBonusPercent(player, city));
        // 해설: 기본 입주 확률에 비서 보너스를 더하고 0~100 범위로 제한한다.
    }

    @Transactional(readOnly = true)
    public double moveOutChancePercent(Player player, String city) {
    // 해설: 세입자 퇴거 확률에 비서 감소 효과를 반영한다.
        return clampPercent(player.getMoveOutChancePercent() - secretaryOperationsService.moveOutReductionPercent(player, city));
        // 해설: 기본 퇴거 확률에서 비서 감소율을 빼고 0~100 범위로 제한한다.
    }

    @Transactional(readOnly = true)
    public double repairRequestChancePercent(Player player, String city) {
    // 해설: 수리 요청 발생 확률에 비서 감소 효과를 반영한다.
        return clampPercent(player.getRepairRequestChancePercent() - secretaryOperationsService.repairRequestReductionPercent(player, city));
        // 해설: 기본 수리 요청 확률에서 비서 감소율을 빼고 0~100 범위로 제한한다.
    }

    private void ensureMonthlyEventSchedule(Player player) {
    // 해설: 이번 달 랜덤 건물 이벤트 날짜가 예약되어 있는지 확인하고 없으면 만든다.
        if (player.hasEventScheduleForCurrentMonth()) {
        // 해설: 이미 이번 달 입주/퇴거/수리 이벤트 일정이 있으면 다시 뽑지 않는다.
            ensureMonthlyMarketNewsSchedule(player);
            // 해설: 건물 이벤트 일정이 이미 있어도 부동산 뉴스 일정은 별도로 보장한다.
            return;
            // 해설: 기존 월간 이벤트 일정을 유지하고 메서드를 끝낸다.
        }
        int firstMoveInDay = randomEventDay(player);
        // 해설: 첫 번째 입주 이벤트 날짜를 무작위로 정한다.
        int secondMoveInDay = randomDistinctEventDay(player, firstMoveInDay);
        // 해설: 두 번째 입주 이벤트 날짜를 첫 번째 날짜와 겹치지 않게 정한다.
        int firstMoveOutDay = randomEventDay(player);
        // 해설: 첫 번째 퇴거 이벤트 날짜를 무작위로 정한다.
        int secondMoveOutDay = randomDistinctEventDay(player, firstMoveOutDay);
        // 해설: 두 번째 퇴거 이벤트 날짜를 첫 번째 퇴거 날짜와 겹치지 않게 정한다.
        int firstRepairDay = randomEventDay(player);
        // 해설: 첫 번째 수리 요청 이벤트 날짜를 무작위로 정한다.
        int secondRepairDay = randomDistinctEventDay(player, firstRepairDay);
        // 해설: 두 번째 수리 요청 이벤트 날짜를 첫 번째 수리 날짜와 겹치지 않게 정한다.
        player.scheduleMonthlyRandomEvents(firstMoveInDay, secondMoveInDay, firstMoveOutDay, secondMoveOutDay, firstRepairDay, secondRepairDay);
        // 해설: 이번 달 입주/퇴거/수리 이벤트 날짜들을 플레이어 상태에 저장한다.
        ensureMonthlyMarketNewsSchedule(player);
        // 해설: 건물 이벤트 일정이 이미 있어도 부동산 뉴스 일정은 별도로 보장한다.
    }

    private void ensureMonthlyMarketNewsSchedule(Player player) {
    // 해설: 이번 달 부동산 호황/불황 뉴스 이벤트 일정을 보장한다.
        if (player.hasMarketNewsScheduleForCurrentMonth()) {
        // 해설: 이미 이번 달 부동산 뉴스 일정이 있으면 다시 예약하지 않는다.
            return;
            // 해설: 기존 부동산 뉴스 일정을 유지하고 메서드를 끝낸다.
        }
        if (!rollPercent(MARKET_NEWS_CHANCE_PERCENT)) {
        // 해설: 정해진 확률에 실패하면 이번 달에는 부동산 뉴스가 없도록 예약한다.
            player.scheduleNoMonthlyMarketNews();
            // 해설: 이번 달 부동산 뉴스 없음 상태를 저장해 매일 다시 확률을 굴리지 않게 한다.
            return;
            // 해설: 이번 달에는 뉴스 이벤트를 만들지 않기로 확정했으므로 끝낸다.
        }
        String trend = random.nextBoolean() ? MARKET_NEWS_RISE : MARKET_NEWS_FALL;
        // 해설: 부동산 뉴스 방향을 호황 또는 불황 중 하나로 무작위 선택한다.
        player.scheduleMonthlyMarketNews(randomEventDay(player), player.getCurrentCity(), trend);
        // 해설: 뉴스 발생일, 현재 도시, 뉴스 방향을 플레이어 상태에 저장한다.
    }

    private int randomEventDay(Player player) {
    // 해설: 현재 달 안에서 랜덤 이벤트가 발생할 날짜를 뽑는다.
        return random.nextInt(player.getDaysInCurrentMonth() - 1) + 2;
        // 해설: 1일은 월초 정산일이므로 제외하고 2일부터 말일까지 중 하나를 뽑는다.
    }

    private int randomDistinctEventDay(Player player, int usedDay) {
    // 해설: 이미 사용한 날짜와 겹치지 않는 이벤트 날짜를 뽑는다.
        int day = randomEventDay(player);
        while (day == usedDay) {
        // 해설: 새로 뽑은 날짜가 기존 날짜와 같으면 다시 뽑는다.
            day = randomEventDay(player);
        }
        return day;
    }

    private String runMonthlyRandomBuildingEvent(Player player) {
    // 해설: 오늘이 예약된 월간 랜덤 이벤트 날짜인지 확인하고 해당 이벤트를 실행한다.
        String notice = "";
        // 해설: 하루 진행 결과를 사용자에게 보여줄 안내 문구를 누적할 변수다.
        if (player.isMarketNewsEventDay()) {
        // 해설: 오늘이 부동산 뉴스 이벤트 날짜면 뉴스 이벤트를 활성화한다.
            notice = activateMarketNews(player);
            // 해설: 부동산 뉴스 효과를 활성화하고 안내 문구를 받는다.
        }
        if (player.isMoveInEventDay()) {
        // 해설: 오늘이 세입자 입주 이벤트 날짜면 입주를 시도한다.
            notice = appendNotice(notice, attemptMoveIn(player));
            // 해설: 입주 결과 안내가 있으면 기존 안내에 붙인다.
        }
        if (player.isMoveOutEventDay()) {
        // 해설: 오늘이 세입자 퇴거 이벤트 날짜면 퇴거를 시도한다.
            String moveOutNotice = attemptMoveOut(player);
            // 해설: 퇴거 시도 결과 안내 문구를 받는다.
            notice = appendNotice(notice, moveOutNotice);
        }
        if (player.isRepairEventDay()) {
        // 해설: 오늘이 수리 요청 이벤트 날짜면 수리 요청 발생을 시도한다.
            String repairNotice = attemptRepairRequest(player);
            // 해설: 수리 요청 결과 안내 문구를 받는다.
            notice = appendNotice(notice, repairNotice);
        }
        return notice;
        // 해설: 하루 정산에서 발생한 안내 문구를 GameService로 반환한다.
    }

    private String activateMarketNews(Player player) {
    // 해설: 부동산 호황/불황 뉴스 이벤트를 실제로 활성화한다.
        if (activeEvent(player).isPresent()) {
        // 해설: 이미 화면에 떠야 할 이벤트가 있으면 뉴스 이벤트를 추가로 띄우지 않는다.
            return "";
        }
        String trend = player.getMarketNewsEventTrend();
        // 해설: 예약된 부동산 뉴스 방향을 가져온다.
        String city = player.getMarketNewsEventCity();
        // 해설: 예약된 부동산 뉴스 적용 도시를 가져온다.
        player.activateMarketNews();
        // 해설: 예약된 뉴스를 실제 활성 상태로 바꾸고 적용 횟수를 설정한다.
        String trendLabel = MARKET_NEWS_RISE.equals(trend) ? "폭등" : "폭락";
        // 해설: 뉴스 방향 코드를 화면용 한글 문구로 바꾼다.
        saveRecord(player, RecordType.BUILDING_BUY, "부동산 " + trendLabel + " 뉴스", null, 0, city, "다음 매물갱신 2회 적용");
        // 해설: 부동산 뉴스 발생 사실과 적용 범위를 월간 기록에 남긴다.
        gameEventRepository.save(new GameEvent(
        // 해설: 화면에 보여줄 부동산 뉴스 이벤트를 저장한다.
                player,
                "market_news_" + player.getId() + "_" + player.getElapsedDays() + "_" + trend,
                city + " 부동산 " + trendLabel + " 뉴스",
                MARKET_NEWS_RISE.equals(trend)
                        ? "투자 수요가 몰리며 매물 평가가 2회 동안 고평가 쪽으로 기웁니다."
                        : "시장 불안이 커지며 매물 평가가 2회 동안 저평가 쪽으로 기웁니다.",
                marketNewsImagePath(city, trend),
                // 해설: 도시와 뉴스 방향에 맞는 뉴스 이미지를 사용한다.
                "NONE",
                "확인"
        ));
        player.pause();
        // 해설: 해금 이벤트가 표시되면 사용자가 확인할 때까지 시간 진행을 멈춘다.
        return city + " 부동산 " + trendLabel + " 뉴스";
        // 해설: 하루 진행 안내에 붙일 뉴스 발생 문구를 반환한다.
    }

    public String activateMarketNewsForTest(Player player, String trend) {
    // 해설: 테스트에서 특정 방향의 부동산 뉴스를 강제로 활성화하기 위한 메서드다.
        player.scheduleMonthlyMarketNews(player.getDay(), player.getCurrentCity(), trend);
        // 해설: 오늘 날짜와 현재 도시로 뉴스 일정을 강제로 잡는다.
        return activateMarketNews(player);
        // 해설: 방금 예약한 뉴스를 실제 활성화 로직으로 처리한다.
    }

    private String attemptMoveIn(Player player) {
    // 해설: 공실 건물에 세입자 입주가 발생하는지 시도한다.
        var movedInBuildings = ownedBuildingRepository.findByPlayerOrderById(player).stream()
        // 해설: 보유 건물 전체에서 입주 후보를 찾는다.
                .filter(building -> !building.isOccupied())
                // 해설: 이미 세입자가 있는 건물은 입주 후보에서 제외한다.
                .filter(building -> rollPercent(moveInChancePercent(player, building.getCity())))
                // 해설: 도시별 입주 확률을 굴려 성공한 건물만 남긴다.
                .toList();
                // 해설: 입주 성공 후보를 리스트로 확정한다. 이후 이 리스트를 순회해 상태를 바꾼다.
        if (movedInBuildings.isEmpty()) {
        // 해설: 입주에 성공한 건물이 없으면 안내 없이 끝낸다.
            return "";
        }
        movedInBuildings.forEach(building -> {
        // 해설: 입주 성공 건물을 하나씩 실제 입주 상태로 바꾼다.
            building.moveIn(player.getElapsedDays());
            // 해설: 현재 경과일 기준으로 세입자 입주 처리하고 퇴거 보호 기간 계산에 필요한 날짜를 저장한다.
            saveRecord(player, RecordType.MOVE_IN, "세입자 입주", null, 0, building.getName(), "60일 퇴거보호");
            // 해설: 입주 발생 기록과 퇴거 보호 메모를 월간 기록에 남긴다.
        });
        return movedInBuildings.size() + "채 입주 완료";
        // 해설: 입주 성공 건물 수를 안내 문구로 반환한다.
    }

    private String attemptMoveOut(Player player) {
    // 해설: 입주 중인 세입자가 퇴거하는지 시도한다.
        var movedOutBuildings = ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .filter(building -> building.canTenantMoveOut(player.getElapsedDays()))
                // 해설: 퇴거 보호 기간이 끝난 건물만 퇴거 후보로 남긴다.
                .filter(building -> rollPercent(moveOutChancePercent(player, building.getCity())))
                // 해설: 도시별 퇴거 확률을 굴려 성공한 건물만 남긴다.
                .toList();
                // 해설: 퇴거 성공 후보를 리스트로 확정한다. 이후 비서 방어 여부를 검사한다.
        if (movedOutBuildings.isEmpty()) {
            return "";
        }
        int defendedCount = 0;
        // 해설: 비서 효과로 퇴거를 막은 건물 수를 센다.
        int movedOutCount = 0;
        // 해설: 실제로 퇴거한 건물 수를 센다.
        for (OwnedBuilding building : movedOutBuildings) {
        // 해설: 퇴거 후보 건물을 하나씩 처리한다.
            if (secretaryOperationsService.defendMoveOut(player, building)) {
            // 해설: 비서가 해당 건물 퇴거를 방어할 수 있는지 먼저 시도한다.
                defendedCount++;
                // 해설: 퇴거 방어에 성공한 수를 증가시킨다.
                continue;
                // 해설: 방어에 성공했으므로 이 건물은 퇴거시키지 않고 다음 건물로 넘어간다.
            }
            building.moveOut();
            // 해설: 방어되지 않은 건물은 세입자를 퇴거시킨다.
            saveRecord(player, RecordType.MOVE_OUT, "세입자 퇴거", null, 0, building.getName(), null);
            // 해설: 세입자 퇴거 기록을 남긴다.
            movedOutCount++;
            // 해설: 실제 퇴거 수를 증가시킨다.
        }
        String notice = movedOutCount == 0 ? "" : movedOutCount + "채 세입자 퇴거";
        // 해설: 실제 퇴거가 있을 때만 퇴거 안내 문구를 만든다.
        if (defendedCount > 0) {
        // 해설: 비서가 막은 퇴거가 있으면 안내 문구에 추가한다.
            notice = appendNotice(notice, defendedCount + "채 퇴거방어");
            // 해설: 퇴거 방어 결과를 기존 퇴거 안내와 합친다.
        }
        return notice;
        // 해설: 하루 정산에서 발생한 안내 문구를 GameService로 반환한다.
    }

    private String attemptRepairRequest(Player player) {
    // 해설: 입주 건물에 수리 요청이 새로 발생하는지 시도한다.
        var repairRequestedBuildings = ownedBuildingRepository.findByPlayerOrderById(player).stream()
        // 해설: 보유 건물 전체에서 수리 요청 후보를 찾는다.
                .filter(OwnedBuilding::isOccupied)
                // 해설: 세입자가 있는 건물만 수리 요청 후보가 된다.
                .filter(building -> !building.isRepairRequested())
                // 해설: 이미 수리 요청이 있는 건물은 중복 요청을 만들지 않는다.
                .filter(building -> rollPercent(repairRequestChancePercent(player, building.getCity())))
                // 해설: 도시별 수리 요청 확률을 굴려 성공한 건물만 남긴다.
                .toList();
                // 해설: 수리 요청 발생 후보를 리스트로 확정한다. 이후 이 리스트를 순회해 상태를 바꾼다.
        if (repairRequestedBuildings.isEmpty()) {
            return "";
        }
        repairRequestedBuildings.forEach(building -> {
        // 해설: 수리 요청 발생 건물을 하나씩 처리한다.
            building.requestRepair();
            // 해설: 건물에 수리 요청 상태를 설정한다.
            saveRecord(player, RecordType.REPAIR_REQUEST, "수리요청 발생", null, 0, building.getName(), null);
            // 해설: 수리 요청 발생 기록을 월간 기록에 남긴다.
        });
        String secretaryNotice = secretaryOperationsService.processAutoRepairs(player);
        // 해설: 수리 요청 직후 비서 자동 수리가 가능한지 처리한다.
        return appendNotice(repairRequestedBuildings.size() + "채 수리요청 발생", secretaryNotice);
        // 해설: 수리 요청 수와 자동 수리 결과를 합쳐 반환한다.
    }

    private String processRepairNeglect(Player player) {
    // 해설: 월초에 오래 방치된 수리 요청을 검사해 퇴거 패널티를 처리한다.
        String notice = "";
        // 해설: 하루 진행 결과를 사용자에게 보여줄 안내 문구를 누적할 변수다.
        for (OwnedBuilding building : ownedBuildingRepository.findByPlayerOrderById(player)) {
        // 해설: 보유 건물을 하나씩 검사한다.
            building.advanceRepairNeglectMonth();
            // 해설: 수리 요청이 남아 있다면 방치 월수를 증가시키고, 아니면 상태에 맞게 유지한다.
            if (building.getRepairNeglectedMonths() >= 2 && building.canTenantMoveOut(player.getElapsedDays())) {
            // 해설: 수리 요청을 2개월 이상 방치했고 퇴거 보호도 끝났으면 강제 퇴거시킨다.
                building.moveOut();
                // 해설: 방어되지 않은 건물은 세입자를 퇴거시킨다.
                saveRecord(player, RecordType.MOVE_OUT, "수리 방치 퇴거", null, 0, building.getName(), null);
                // 해설: 수리 방치로 인한 퇴거 기록을 남긴다.
                notice = appendNotice(notice, building.getName() + " 수리 방치로 퇴거");
                // 해설: 퇴거 패널티 안내를 누적한다.
            }
        }
        return notice;
        // 해설: 하루 정산에서 발생한 안내 문구를 GameService로 반환한다.
    }

    private void activateUnlockEvent(Player player, ReputationTier tier) {
    // 해설: 평판 상승으로 새 건물이 해금됐을 때 이벤트를 띄운다.
        if (activeEvent(player).isPresent()) {
        // 해설: 이미 화면에 떠야 할 이벤트가 있으면 해금 이벤트를 추가로 띄우지 않는다.
            return;
            // 해설: 기존 활성 이벤트 처리를 우선하기 위해 해금 이벤트 생성을 끝낸다.
        }
        if ("서울".equals(tier.unlockCity()) && tier.unlockBuildingSlot() >= 1) {
        // 해설: 서울 건물이 해금되는 시점이면 주식 컨텐츠 해금도 예약한다.
            player.scheduleStockUnlock(player.getElapsedDays() + 2);
            // 해설: 주식 컨텐츠 해금 안내를 현재 날짜 기준 2일 뒤로 예약한다.
        }
        gameEventRepository.save(new GameEvent(
        // 해설: 화면에 보여줄 부동산 뉴스 이벤트를 저장한다.
                player,
                new GameEventDefinition(
                // 해설: 해금 이벤트에 사용할 정의를 즉석에서 만든다.
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
        // 해설: 해금 이벤트가 표시되면 사용자가 확인할 때까지 시간 진행을 멈춘다.
    }

    private Optional<GameEvent> activeEvent(Player player) {
    // 해설: 현재 활성 이벤트가 있는지 최신순으로 하나만 조회한다.
        return gameEventRepository.findLatestByPlayerIdAndStatus(player.getId(), GameEventStatus.ACTIVE, PageRequest.of(0, 1)).stream().findFirst();
        // 해설: ACTIVE 상태 이벤트를 최신순 1개만 가져와 Optional로 반환한다.
    }

    private boolean rollPercent(double percent) {
    // 해설: 0~100 퍼센트 확률 판정을 수행한다.
        return random.nextInt(100) < percent;
        // 해설: 0~99 난수를 뽑아 percent보다 작으면 성공으로 본다.
    }

    private long applyPercentBonus(long baseAmount, double percent) {
    // 해설: 기준 금액에 퍼센트 보너스를 적용한다.
        return Math.max(0L, (long) Math.floor(baseAmount * (100.0 + percent) / 100.0));
        // 해설: 계산 결과를 내림 처리하고 음수가 되지 않게 0 이상으로 보정한다.
    }

    private double clampPercent(double value) {
    // 해설: 확률 값이 0~100 범위를 벗어나지 않게 제한한다.
        return Math.max(0.0, Math.min(100.0, value));
        // 해설: 100보다 크면 100, 0보다 작으면 0으로 보정한다.
    }

    private String marketNewsImagePath(String city, String trend) {
    // 해설: 부동산 뉴스 이벤트 이미지 경로를 만든다.
        return "/assets/news/" + citySlug(city) + "-" + (MARKET_NEWS_RISE.equals(trend) ? "rise" : "fall") + ".jpg";
        // 해설: 도시 slug와 뉴스 방향을 조합해 이미지 파일명을 만든다.
    }

    private String citySlug(String city) {
    // 해설: 한글 도시명을 파일 경로용 영문 slug로 변환한다.
        return switch (city) {
        // 해설: 도시 이름에 따라 고정된 영문 문자열을 반환한다.
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
    // 해설: 두 안내 문구를 하나로 합치는 내부 유틸리티다.
        if (addition == null || addition.isBlank()) {
        // 해설: 추가 안내가 없으면 기존 안내를 그대로 유지한다.
            return base;
        }
        if (base == null || base.isBlank()) {
        // 해설: 기존 안내가 없으면 추가 안내만 반환한다.
            return addition;
        }
        return base + " · " + addition;
        // 해설: 두 안내가 모두 있으면 가운데 구분자를 넣어 합친다.
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 정산과 이벤트 결과를 월간 기록에 저장하는 공통 메서드다.
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        // 해설: 새 MonthlyRecord를 저장한다.
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
        // 해설: 보관 기간보다 오래된 기록을 삭제한다. 기준일은 최소 1일로 보정한다.
    }
}
```
