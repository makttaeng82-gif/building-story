# StockService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/StockService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedStock;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.domain.StockTradeHistory;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedStockRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import com.game.buildingstory.repo.StockTradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockService {
// 해설: 주식 컨텐츠 해금, 가격 이력, 뉴스 효과, 코인 환전, 매수/매도, 화면 표시용 계산을 담당하는 서비스다.
    /*
     * StockService는 주식 컨텐츠의 서버 규칙을 담당한다.
     *
     * 주요 책임:
     * 1. 서울 진출 후 주식 기능 개방 예약/이벤트 표시
     * 2. 종목별 가격 이력 생성과 5일 주기 가격 갱신
     * 3. 현금 <-> 코인 교환
     * 4. 현재가 기준 즉시 매수/매도
     * 5. 업종 호황/불황 뉴스 효과 적용
     *
     * 주식은 부동산과 별도 재화인 코인을 사용한다. 현금과 주식을 직접 섞지 않으면
     * 주식 손실이 부동산 구매력 전체를 즉시 망가뜨리는 상황을 줄일 수 있다.
     */
    private static final String STOCK_UNLOCK_EFFECT = "NONE";
    private static final String STOCK_UNLOCK_IMAGE = "AI 주식 이미지";
    public static final String STOCK_NEWS_BOOM = "BOOM";
    public static final String STOCK_NEWS_RECESSION = "RECESSION";
    private static final int UPDATE_INTERVAL_DAYS = 5;
    private static final int INDUSTRY_NEWS_CHANCE_PERCENT = 15;
    private static final double TREND_EFFECT_PERCENT = 0.5;
    private static final double SHOCK_CHANCE = 0.02;
    private static final double NORMAL_LIMIT_PERCENT = 12.0;
    private static final double SHOCK_LIMIT_PERCENT = 25.0;
    private static final int CASH_PER_COIN = 100;
    private static final double TRADE_FEE_RATE = 0.005;

    private final Random random = new Random();
    private final StockCatalog stockCatalog;
    private final ReputationCatalog reputationCatalog;
    private final GameEventRepository gameEventRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final OwnedStockRepository ownedStockRepository;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;
    private final StockTradeHistoryRepository stockTradeHistoryRepository;

    public StockService(
    // 해설: 주식 규칙에 필요한 카탈로그, 이벤트, 기록, 보유 주식, 가격 이력, 거래 이력 Repository를 생성자 주입으로 받는다.
            StockCatalog stockCatalog,
            ReputationCatalog reputationCatalog,
            GameEventRepository gameEventRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            OwnedStockRepository ownedStockRepository,
            StockPriceHistoryRepository stockPriceHistoryRepository,
            StockTradeHistoryRepository stockTradeHistoryRepository
    ) {
        this.stockCatalog = stockCatalog;
        this.reputationCatalog = reputationCatalog;
        this.gameEventRepository = gameEventRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.ownedStockRepository = ownedStockRepository;
        this.stockPriceHistoryRepository = stockPriceHistoryRepository;
        this.stockTradeHistoryRepository = stockTradeHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<StockSpec> stocks() {
    // 해설: 주식 카탈로그에 등록된 전체 종목 목록을 반환한다.
        return stockCatalog.all();
        // 해설: 종목 정의는 DB가 아니라 StockCatalog의 고정 목록에서 가져온다.
    }

    public void ensureUnlockSchedule(Player player) {
    // 해설: 서울 해금 조건을 만족한 플레이어에게 주식 개방 예정일을 예약한다.
        // 주식은 서울 해금 후 2 elapsedDays 뒤에 열린다. 조건을 만족한 최초 1회만 예약한다.
        if (player.isStockContentUnlocked() || player.hasStockUnlockSchedule()) {
        // 해설: 이미 주식이 열렸거나 예약이 있으면 중복 예약하지 않는다.
            return;
        }
        if (reputationCatalog.isCityUnlocked("서울", player.getReputation(), !player.isEmployed())) {
        // 해설: 서울이 해금된 상태인지 평판과 퇴사 여부 기준으로 확인한다.
            player.scheduleStockUnlock(player.getElapsedDays() + 2);
            // 해설: 조건을 만족하면 현재 경과일 기준 2일 뒤 주식 개방을 예약한다.
        }
    }

    public boolean activateUnlockNoticeIfDue(Player player) {
    // 해설: 주식 개방일이 되었을 때 개방 이벤트를 실제로 띄운다.
        // 개방일이 되었더라도 이미 다른 이벤트 모달이 떠 있으면 새 이벤트를 겹치지 않는다.
        ensureUnlockSchedule(player);
        // 해설: 개방 이벤트 확인 전에 예약 상태가 없으면 먼저 만든다.
        if (!player.isStockUnlockDue() || player.isStockUnlockNoticeShown()) {
        // 해설: 아직 개방일이 아니거나 이미 안내를 봤으면 이벤트를 만들지 않는다.
            return false;
        }
        if (gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE).isPresent()) {
        // 해설: 이미 활성 이벤트가 있으면 주식 개방 이벤트를 겹쳐 띄우지 않는다.
            return false;
        }
        player.unlockStockContent();
        // 해설: 플레이어 상태를 주식 컨텐츠 개방으로 바꾼다.
        player.markStockUnlockNoticeShown();
        // 해설: 개방 안내 이벤트가 다시 뜨지 않도록 표시 완료 상태를 저장한다.
        ensureMarketInitialized(player);
        // 해설: 가격 갱신 전에 최초 가격 이력이 있는지 보장한다.
        gameEventRepository.save(new GameEvent(
        // 해설: 주식 뉴스 모달에 표시할 GameEvent를 저장한다.
                player,
                "stock_unlock_" + player.getId(),
                "주식 투자 개방",
                "서울 진출 이후 증권 계좌가 개설되었습니다. 이제 주식 투자를 할 수 있습니다.",
                STOCK_UNLOCK_IMAGE,
                STOCK_UNLOCK_EFFECT,
                "확인"
        ));
        player.pause();
        // 해설: 개방 안내를 확인할 때까지 시간 진행을 멈춘다.
        return true;
        // 해설: 이벤트가 새로 활성화됐음을 호출자에게 알린다.
    }

    public void processPriceUpdates(Player player) {
    // 해설: 5일 주기로 모든 종목의 새 가격 캔들을 생성한다.
        // 모든 종목의 가격은 같은 날 한 번에 갱신된다. 일부 종목만 갱신되면 차트 기준일이 어긋난다.
        if (!player.isStockContentUnlocked()) {
        // 해설: 주식 컨텐츠가 열리지 않았으면 가격 이력도 갱신하지 않는다.
            return;
        }
        ensureMarketInitialized(player);
        // 해설: 가격 갱신 전에 최초 가격 이력이 있는지 보장한다.
        int lastUpdateDay = stockPriceHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
        // 해설: 전체 종목 가격 이력 중 가장 최근 갱신일을 찾는다.
                .map(StockPriceHistory::getElapsedDays)
                // 해설: 가격 이력 엔티티에서 경과일만 꺼낸다.
                .orElse(player.getElapsedDays());
                // 해설: 가격 이력이 없으면 현재 날짜를 기준으로 삼아 즉시 중복 갱신을 막는다.
        if (player.getElapsedDays() - lastUpdateDay < UPDATE_INTERVAL_DAYS) {
        // 해설: 마지막 갱신 후 5일이 지나지 않았으면 새 캔들을 만들지 않는다.
            return;
        }
        // marketEffectPercent는 이번 5일 구간의 시장 분위기다. 한 번 뽑아 모든 종목에 공통 적용한다.
        double marketEffectPercent = marketEffectPercent();
        // 해설: 이번 5일 구간에 모든 종목이 공유할 시장 분위기 변동률을 한 번 뽑는다.
        stockCatalog.all().forEach(stock -> appendNextHistory(player, stock, marketEffectPercent));
        // 해설: 모든 종목에 대해 같은 시장 분위기를 반영해 새 가격 이력을 추가한다.
        // 업종 뉴스 효과는 "주가 갱신 횟수" 기준으로 줄어든다. 날짜 기준으로 줄이면 갱신 없는 날에도 효과가 사라진다.
        player.consumeStockNewsRefresh();
        // 해설: 업종 뉴스 효과는 가격 갱신 횟수 기준으로 소모되므로 갱신 후 1회 차감한다.
    }

    public boolean activateIndustryNewsIfDue(Player player) {
    // 해설: 예약된 주식 업종 뉴스 날짜가 되면 뉴스 이벤트를 활성화한다.
        // 업종 뉴스는 월별로 미리 예약해 두고, 예약일이 되면 active 상태로 전환한다.
        if (!player.isStockContentUnlocked()) {
        // 해설: 주식 컨텐츠가 열리지 않았으면 업종 뉴스 이벤트도 발생시키지 않는다.
            return false;
        }
        ensureMonthlyIndustryNewsSchedule(player);
        // 해설: 이번 달 주식 뉴스 일정이 없으면 먼저 확정한다.
        if (!player.isStockNewsEventDay()) {
        // 해설: 오늘이 예약된 뉴스 날짜가 아니면 이벤트를 만들지 않는다.
            return false;
        }
        if (gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE).isPresent()) {
        // 해설: 이미 활성 이벤트가 있으면 주식 개방 이벤트를 겹쳐 띄우지 않는다.
            return false;
        }
        String industry = player.getStockNewsEventIndustry();
        // 해설: 예약된 뉴스의 업종을 가져온다.
        String trend = player.getStockNewsEventTrend();
        // 해설: 예약된 뉴스의 방향을 가져온다. 값은 BOOM 또는 RECESSION이다.
        player.activateStockNews();
        // 해설: 예약된 뉴스를 실제 활성 뉴스로 바꾸고 적용 횟수를 설정한다.
        String trendLabel = STOCK_NEWS_BOOM.equals(trend) ? "호황" : "불황";
        // 해설: 뉴스 방향 코드를 화면용 한글 문구로 바꾼다.
        monthlyRecordRepository.save(new MonthlyRecord(
        // 해설: 주식 뉴스 발생 사실을 월간 기록에 저장한다.
                player,
                RecordType.STOCK_EVENT,
                "주식 " + industry + " " + trendLabel + " 뉴스",
                null,
                0,
                industry,
                "다음 주가갱신 2회 적용"
        ));
        gameEventRepository.save(new GameEvent(
        // 해설: 주식 뉴스 모달에 표시할 GameEvent를 저장한다.
                player,
                "stock_news_" + player.getId() + "_" + player.getElapsedDays() + "_" + industry + "_" + trend,
                industry + " 업종 " + trendLabel + " 뉴스",
                STOCK_NEWS_BOOM.equals(trend)
                        ? industry + " 업종 수요가 살아나며 다음 주가갱신 2회 동안 상승 압력이 강해집니다."
                        : industry + " 업종 실적 우려가 커지며 다음 주가갱신 2회 동안 하락 압력이 강해집니다.",
                stockNewsImagePath(industry, trend),
                // 해설: 업종과 호황/불황 방향에 맞는 뉴스 이미지를 사용한다.
                "NONE",
                "확인"
        ));
        player.pause();
        // 해설: 개방 안내를 확인할 때까지 시간 진행을 멈춘다.
        return true;
        // 해설: 이벤트가 새로 활성화됐음을 호출자에게 알린다.
    }

    @Transactional(readOnly = true)
    public StockMarketStatusView marketStatus(Player player) {
    // 해설: 주식 화면 상단에 표시할 시장 상태 정보를 만든다.
        int lastUpdateDay = stockPriceHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
        // 해설: 전체 종목 가격 이력 중 가장 최근 갱신일을 찾는다.
                .map(StockPriceHistory::getElapsedDays)
                // 해설: 가격 이력 엔티티에서 경과일만 꺼낸다.
                .orElse(player.getElapsedDays());
                // 해설: 가격 이력이 없으면 현재 날짜를 기준으로 삼아 즉시 중복 갱신을 막는다.
        int daysSinceUpdate = Math.max(0, player.getElapsedDays() - lastUpdateDay);
        // 해설: 마지막 가격 갱신 이후 며칠이 지났는지 계산하고 음수는 0으로 보정한다.
        int daysUntilNextUpdate = Math.max(0, UPDATE_INTERVAL_DAYS - daysSinceUpdate);
        // 해설: 다음 주가 갱신까지 남은 일수를 계산한다.
        int progressPercent = Math.min(100, daysSinceUpdate * 100 / UPDATE_INTERVAL_DAYS);
        // 해설: 다음 갱신까지의 진행률을 0~100 퍼센트로 계산한다.
        return new StockMarketStatusView(
        // 해설: 계산한 상태값을 화면 표시용 record로 묶어 반환한다.
                player.dateTextAfterDays(daysUntilNextUpdate),
                daysUntilNextUpdate,
                progressPercent,
                activeStockNewsText(player),
                // 해설: 현재 적용 중인 업종 뉴스 문구를 포함한다.
                activeStockNewsDirection(player)
                // 해설: 뉴스 방향을 up/down/flat 값으로 포함해 UI 색상 처리에 쓰게 한다.
        );
    }

    public void ensureMarketInitialized(Player player) {
    // 해설: 플레이어별 종목 최초 가격 이력을 생성한다.
        // 새로 주식이 열린 플레이어에게 종목별 최초 가격 행을 만든다.
        // 이미 존재하는 종목은 건드리지 않아 기존 차트 이력을 보존한다.
        stockCatalog.all().forEach(stock -> {
        // 해설: 카탈로그의 모든 종목을 순회한다.
            if (!stockPriceHistoryRepository.existsByPlayerAndStockKey(player, stock.key())) {
            // 해설: 해당 플레이어에게 이 종목 가격 이력이 없을 때만 생성한다.
                stockPriceHistoryRepository.save(new StockPriceHistory(
                // 해설: 시가/고가/저가/종가가 모두 기준가인 최초 가격 이력을 저장한다.
                        player,
                        stock.key(),
                        stock.basePrice(),
                        stock.basePrice(),
                        stock.basePrice(),
                        stock.basePrice(),
                        initialVolume(stock)
                        // 해설: 종목 기준가에서 계산한 초기 거래량을 저장한다.
                ));
            }
        });
    }

    @Transactional(readOnly = true)
    public List<StockQuoteView> stockQuotes(Player player) {
    // 해설: 주식 화면에 표시할 종목별 현재가, 보유 수량, 차트 정보를 만든다.
        Map<String, OwnedStock> ownedStocks = ownedStockRepository.findByPlayer(player).stream()
        // 해설: 플레이어가 보유한 주식을 종목 키로 빠르게 찾기 위해 Map으로 준비한다.
                .collect(Collectors.toMap(OwnedStock::getStockKey, Function.identity()));
                // 해설: 보유 주식 엔티티를 stockKey -> OwnedStock 형태로 변환한다.
        return stockCatalog.all().stream()
        // 해설: 전체 종목을 순회해 각 종목의 QuoteView를 만든다.
                .map(stock -> quote(player, stock, ownedStocks.get(stock.key())))
                // 해설: 종목 정의와 보유 정보를 합쳐 화면 표시용 QuoteView로 변환한다.
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockTradeHistory> tradeHistories(Player player) {
    // 해설: 최근 주식 거래 내역을 조회한다.
        return stockTradeHistoryRepository.findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(
                player,
                Math.max(1, player.getElapsedDays() - 89)
                // 해설: 최근 90일 정도의 거래만 조회하되 기준일이 1보다 작아지지 않게 보정한다.
        );
    }

    @Transactional(readOnly = true)
    public StockHoldingSummaryView holdingSummary(Player player) {
    // 해설: 보유 종목 전체의 수량, 원가, 평가액, 손익 요약을 만든다.
        // 보유요약은 현재가 기준 평가금액과 평균단가 기준 원가를 비교해 전체 손익을 계산한다.
        List<StockQuoteView> ownedQuotes = stockQuotes(player).stream()
        // 해설: 전체 종목 Quote 중 보유 수량이 있는 것만 요약 대상으로 삼는다.
                .filter(quote -> quote.quantity() > 0)
                // 해설: 수량이 0인 종목은 보유 요약에서 제외한다.
                .toList();
        long totalQuantity = ownedQuotes.stream().mapToLong(StockQuoteView::quantity).sum();
        // 해설: 보유 주식 총 수량을 계산한다.
        long totalCost = ownedQuotes.stream().mapToLong(quote -> quote.averagePrice() * quote.quantity()).sum();
        // 해설: 평균단가와 수량을 곱해 전체 매입 원가를 계산한다.
        long totalValuation = ownedQuotes.stream().mapToLong(quote -> quote.currentPrice() * quote.quantity()).sum();
        // 해설: 현재가와 수량을 곱해 전체 평가액을 계산한다.
        long totalProfit = totalValuation - totalCost;
        // 해설: 평가액에서 원가를 빼 전체 평가손익을 계산한다.
        return new StockHoldingSummaryView(
                ownedQuotes.size(),
                totalQuantity,
                totalCost,
                totalValuation,
                totalProfit,
                stockPriceText(totalCost),
                stockPriceText(totalValuation),
                profitText(totalProfit, totalCost),
                // 해설: 손익 금액과 수익률을 함께 표시할 문자열을 만든다.
                changeDirection(totalProfit)
        );
    }

    public String exchangeCashToCoin(Player player, long coinAmount) {
    // 해설: 현금을 주식 전용 코인으로 교환한다.
        // 주식 거래는 코인으로만 한다. 현금을 코인으로 바꿀 때 고정 환율을 적용한다.
        if (coinAmount <= 0) {
        // 해설: 0 이하 수량은 교환할 수 없다.
            return "교환 수량 오류";
        }
        long cashCost = coinAmount * CASH_PER_COIN;
        // 해설: 요청 코인 수량에 고정 환율을 곱해 필요한 현금을 계산한다.
        if (!player.spendCash(cashCost)) {
        // 해설: 필요 현금을 차감한다. 잔액이 부족하면 실패한다.
            return "현금 부족";
        }
        player.addCoin(coinAmount);
        // 해설: 현금 차감에 성공하면 주식용 코인을 증가시킨다.
        return coinText(coinAmount) + " 교환";
    }

    public String exchangeCoinToCash(Player player, long coinAmount) {
    // 해설: 주식용 코인을 현금으로 환전한다.
        if (coinAmount <= 0) {
        // 해설: 0 이하 수량은 교환할 수 없다.
            return "교환 수량 오류";
        }
        if (!player.spendCoin(coinAmount)) {
        // 해설: 환전할 코인을 차감한다. 보유 코인이 부족하면 실패한다.
            return "코인 부족";
        }
        player.addCash(coinAmount * CASH_PER_COIN);
        // 해설: 차감한 코인에 고정 환율을 곱해 현금을 지급한다.
        return coinText(coinAmount) + " 환전";
    }

    public String buyStock(Player player, String stockKey, long quantity) {
    // 해설: 특정 종목을 지정 수량만큼 매수한다.
        // 매수는 현재가 시장가 주문으로 처리한다. 지정가 주문이나 주문 대기열은 없다.
        if (quantity <= 0) {
        // 해설: 0 이하 수량은 매수 요청으로 인정하지 않는다.
            return "매수 수량 오류";
        }
        if (!player.isStockContentUnlocked()) {
        // 해설: 주식 컨텐츠가 열리지 않았으면 거래를 막는다.
            return "주식 미개방";
        }
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        // 해설: 요청한 종목 키가 카탈로그에 있는지 확인하고 종목 정의를 가져온다.
        long price = currentPrice(player, stock);
        // 해설: 가장 최근 가격 이력의 종가를 현재가로 사용한다.
        long grossAmount = price * quantity;
        // 해설: 수수료 제외 매수 금액을 계산한다.
        long fee = tradeFee(grossAmount);
        // 해설: 거래 수수료를 계산한다.
        long totalCost = grossAmount + fee;
        // 해설: 매수 금액과 수수료를 합친 실제 필요 코인을 계산한다.
        // 코인이 부족하면 보유 수량이나 거래 이력은 변경하지 않고 메시지만 반환한다.
        if (!player.spendCoin(totalCost)) {
        // 해설: 필요 코인을 차감한다. 부족하면 보유 주식과 거래 이력을 변경하지 않는다.
            return "코인 부족 · 필요 " + coinText(totalCost) + " / 보유 " + coinText(player.getCoin());
        }
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey)
        // 해설: 이미 보유 중인 종목인지 조회한다.
                .orElseGet(() -> ownedStockRepository.save(new OwnedStock(player, stockKey)));
                // 해설: 처음 사는 종목이면 보유 주식 엔티티를 새로 만든다.
        ownedStock.buy(quantity, price);
        // 해설: 보유 수량과 평균단가를 매수 가격 기준으로 갱신한다.
        stockTradeHistoryRepository.save(new StockTradeHistory(player, stock.key(), stock.name(), "매수", quantity, price, grossAmount, fee, totalCost));
        // 해설: 매수 거래 내역을 저장한다. 마지막 금액은 수수료 포함 총 지출이다.
        return stock.name() + " " + quantity + "주 매수";
    }

    public String buyMaxStock(Player player, String stockKey) {
    // 해설: 보유 코인으로 가능한 최대 수량을 계산해 매수한다.
        if (!player.isStockContentUnlocked()) {
        // 해설: 주식 컨텐츠가 열리지 않았으면 거래를 막는다.
            return "주식 미개방";
        }
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        // 해설: 요청한 종목 키가 카탈로그에 있는지 확인하고 종목 정의를 가져온다.
        long price = currentPrice(player, stock);
        // 해설: 가장 최근 가격 이력의 종가를 현재가로 사용한다.
        long quantity = maxAffordableQuantity(player.getCoin(), price);
        // 해설: 수수료까지 고려해 살 수 있는 최대 주식 수를 계산한다.
        if (quantity <= 0) {
        // 해설: 최소 1주도 살 수 없으면 필요 금액을 안내한다.
            long minimumCost = price + tradeFee(price);
            // 해설: 1주 매수에 필요한 가격과 수수료를 합산한다.
            return "코인 부족 · 필요 " + coinText(minimumCost) + " / 보유 " + coinText(player.getCoin());
        }
        return buyStock(player, stockKey, quantity);
        // 해설: 계산된 최대 수량으로 일반 매수 로직을 재사용한다.
    }

    public String sellStock(Player player, String stockKey, long quantity) {
    // 해설: 특정 종목을 지정 수량만큼 매도한다.
        // 매도도 현재가 시장가 주문이다. 공매도/마진이 없으므로 보유 수량보다 많이 팔 수 없다.
        if (quantity <= 0) {
        // 해설: 0 이하 수량은 매도 요청으로 인정하지 않는다.
            return "매도 수량 오류";
        }
        if (!player.isStockContentUnlocked()) {
        // 해설: 주식 컨텐츠가 열리지 않았으면 거래를 막는다.
            return "주식 미개방";
        }
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        // 해설: 요청한 종목 키가 카탈로그에 있는지 확인하고 종목 정의를 가져온다.
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        // 해설: 보유 주식이 없을 수도 있으므로 null 허용으로 조회한다.
        if (ownedStock == null || ownedStock.getQuantity() < quantity) {
        // 해설: 보유하지 않았거나 요청 수량보다 적게 보유하면 매도할 수 없다.
            long ownedQuantity = ownedStock == null ? 0 : ownedStock.getQuantity();
            // 해설: 오류 메시지에 표시할 현재 보유 수량을 계산한다.
            return "보유 수량 부족 · 보유 " + ownedQuantity + "주 / 매도 요청 " + quantity + "주";
        }
        long price = currentPrice(player, stock);
        // 해설: 가장 최근 가격 이력의 종가를 현재가로 사용한다.
        long grossAmount = price * quantity;
        // 해설: 수수료 제외 매수 금액을 계산한다.
        long fee = tradeFee(grossAmount);
        // 해설: 거래 수수료를 계산한다.
        long payout = Math.max(0, grossAmount - fee);
        // 해설: 매도 금액에서 수수료를 뺀 실제 수령 코인을 계산한다.
        ownedStock.sell(quantity);
        // 해설: 보유 수량을 매도 수량만큼 줄인다.
        player.addCoin(payout);
        // 해설: 수수료 차감 후 매도 대금을 코인 잔액에 더한다.
        stockTradeHistoryRepository.save(new StockTradeHistory(player, stock.key(), stock.name(), "매도", quantity, price, grossAmount, fee, payout));
        // 해설: 매도 거래 내역을 저장한다. 마지막 금액은 실제 수령액이다.
        return stock.name() + " " + quantity + "주 매도";
    }

    public String sellAllStock(Player player, String stockKey) {
    // 해설: 특정 종목의 보유 수량 전체를 매도한다.
        if (!player.isStockContentUnlocked()) {
        // 해설: 주식 컨텐츠가 열리지 않았으면 거래를 막는다.
            return "주식 미개방";
        }
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        // 해설: 보유 주식이 없을 수도 있으므로 null 허용으로 조회한다.
        if (ownedStock == null || ownedStock.getQuantity() <= 0) {
            return "보유 수량 부족 · 보유 0주 / 매도 요청 1주";
        }
        return sellStock(player, stockKey, ownedStock.getQuantity());
        // 해설: 전체 수량을 일반 매도 로직에 넘겨 중복 계산을 피한다.
    }

    @Transactional(readOnly = true)
    public boolean isUnlocked(Player player) {
    // 해설: 주식 컨텐츠가 열렸는지 반환한다.
        return player.isStockContentUnlocked();
    }

    @Transactional(readOnly = true)
    public String statusText(Player player) {
    // 해설: 주식 컨텐츠 상태를 화면 문구로 반환한다.
        if (player.isStockContentUnlocked()) {
        // 해설: 이미 개방됐다면 개방 상태 문구를 반환한다.
            return "개방";
        }
        if (player.hasStockUnlockSchedule()) {
        // 해설: 아직 열리진 않았지만 개방 예약이 있으면 준비중으로 표시한다.
            return "개방 준비중";
        }
        return "서울 해금 필요";
    }

    private void appendNextHistory(Player player, StockSpec stock, double marketEffectPercent) {
    // 해설: 종목 하나에 대해 다음 5일 가격 캔들을 생성한다.
        // OHLC 한 줄은 5일 단위 캔들 하나다. open은 직전 close, close는 이번 변동률을 적용한 가격이다.
        StockPriceHistory latest = stockPriceHistoryRepository.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key())
        // 해설: 해당 종목의 가장 최근 가격 이력을 가져온다.
                .orElseGet(() -> stockPriceHistoryRepository.save(new StockPriceHistory(
                        player,
                        stock.key(),
                        stock.basePrice(),
                        stock.basePrice(),
                        stock.basePrice(),
                        stock.basePrice(),
                        initialVolume(stock)
                        // 해설: 종목 기준가에서 계산한 초기 거래량을 저장한다.
                )));
        long open = latest.getClosePrice();
        // 해설: 새 캔들의 시가는 직전 캔들의 종가다.
        StockChange change = stockChangePercent(player, stock, marketEffectPercent);
        // 해설: 시장, 업종 뉴스, 추세, 위험도, 충격을 합산한 변동률을 계산한다.
        double changePercent = change.percent();
        // 해설: 계산된 변동률 숫자만 꺼낸다.
        long close = Math.max(1L, Math.round(open * (100.0 + changePercent) / 100.0));
        // 해설: 변동률을 적용해 종가를 계산하고 최소 1코인으로 보정한다.
        long highBase = Math.max(open, close);
        // 해설: 고가 계산의 기준값은 시가와 종가 중 큰 값이다.
        long lowBase = Math.min(open, close);
        // 해설: 저가 계산의 기준값은 시가와 종가 중 작은 값이다.
        long high = Math.max(highBase, Math.round(highBase * (100.0 + random.nextDouble(0.0, 3.0)) / 100.0));
        long low = Math.max(1L, Math.min(lowBase, Math.round(lowBase * (100.0 - random.nextDouble(0.0, 3.0)) / 100.0)));
        stockPriceHistoryRepository.save(new StockPriceHistory(player, stock.key(), open, high, low, close, randomVolume(stock, changePercent)));
        // 해설: 계산한 OHLC와 거래량을 새 가격 이력으로 저장한다.
    }

    private StockChange stockChangePercent(Player player, StockSpec stock, double marketEffectPercent) {
    // 해설: 주가 변동률을 구성하는 모든 요인을 합산한다.
        // 가격 변동률은 시장 공통 효과 + 업종 뉴스 + 추세 + 종목 위험도별 노이즈 + 희귀 충격을 합산한다.
        double shockEffectPercent = 0.0;
        // 해설: 희귀 급등/급락 충격값의 기본값은 0이다.
        boolean hasShock = false;
        // 해설: 이번 변동에 충격 이벤트가 포함됐는지 저장한다.
        double shockRoll = random.nextDouble();
        // 해설: 0~1 난수로 희귀 충격 발생 여부를 판정한다.
        if (shockRoll < SHOCK_CHANCE) {
        // 해설: 정해진 확률이면 급등 충격을 적용한다.
            shockEffectPercent = randomChangePercent(8.0, 18.0);
            // 해설: 급등 충격은 8~18% 사이 양수 변동이다.
            hasShock = true;
        } else if (shockRoll < SHOCK_CHANCE * 2) {
        // 해설: 다음 같은 확률 구간이면 급락 충격을 적용한다.
            shockEffectPercent = -randomChangePercent(8.0, 18.0);
            // 해설: 급락 충격은 8~18% 사이 음수 변동이다.
            hasShock = true;
        }

        double rawPercent = marketEffectPercent
        // 해설: 시장 공통 효과부터 시작해 최종 원시 변동률을 만든다.
                + industryEffectPercent(player, stock)
                // 해설: 현재 활성 업종 뉴스가 이 종목 업종과 맞으면 뉴스 효과를 더한다.
                + trendEffectPercent(player, stock)
                // 해설: 최근 캔들 흐름이 상승/하락 쪽이면 추세 효과를 더한다.
                + randomChangePercent(stock.riskType().minNoisePercent(), stock.riskType().maxNoisePercent())
                // 해설: 종목 위험도별 일상적인 랜덤 노이즈를 더한다.
                + shockEffectPercent;
                // 해설: 희귀 충격이 발생했다면 그 값을 마지막으로 더한다.
        double limit = hasShock ? SHOCK_LIMIT_PERCENT : NORMAL_LIMIT_PERCENT;
        // 해설: 충격이 있으면 더 큰 변동폭 한도를 허용한다.
        return new StockChange(clamp(rawPercent, -limit, limit), hasShock);
        // 해설: 최종 변동률을 한도 안으로 제한해 반환한다.
    }

    private double marketEffectPercent() {
    // 해설: 전체 시장 분위기 변동률을 무작위로 만든다.
        return switch (random.nextInt(3)) {
        // 해설: 상승장, 보합장, 하락장 중 하나를 뽑는다.
            case 0 -> randomChangePercent(0.5, 2.0);
            case 1 -> randomChangePercent(-0.7, 0.7);
            default -> randomChangePercent(-2.0, -0.5);
        };
    }

    private double industryEffectPercent(Player player, StockSpec stock) {
    // 해설: 활성 업종 뉴스가 해당 종목에 주는 변동률 효과를 계산한다.
        // active 뉴스의 업종과 종목 업종이 일치할 때만 효과를 준다.
        if (!player.hasActiveStockNewsForIndustry(stock.industry())) {
        // 해설: 뉴스 업종과 종목 업종이 다르면 효과가 없다.
            return 0.0;
        }
        if (STOCK_NEWS_BOOM.equals(player.getActiveStockNewsTrend())) {
        // 해설: 호황 뉴스면 양수 변동 효과를 준다.
            return randomChangePercent(3.0, 8.0);
        }
        if (STOCK_NEWS_RECESSION.equals(player.getActiveStockNewsTrend())) {
        // 해설: 불황 뉴스면 음수 변동 효과를 준다.
            return -randomChangePercent(3.0, 8.0);
        }
        return 0.0;
    }

    private String activeStockNewsText(Player player) {
    // 해설: 현재 적용 중인 업종 뉴스 배지 문구를 만든다.
        if (player.getActiveStockNewsRefreshesLeft() <= 0 || player.getActiveStockNewsIndustry() == null) {
        // 해설: 남은 적용 횟수가 없거나 업종이 없으면 표시할 문구가 없다.
            return "";
        }
        String trendLabel = STOCK_NEWS_BOOM.equals(player.getActiveStockNewsTrend()) ? "호황" : "불황";
        return player.getActiveStockNewsIndustry() + " " + trendLabel + " 적용중 · "
        // 해설: 업종, 호황/불황, 남은 적용 횟수를 합쳐 배지 문구를 만든다.
                + player.getActiveStockNewsRefreshesLeft() + "회 남음";
    }

    private String activeStockNewsDirection(Player player) {
    // 해설: 현재 뉴스 방향을 UI 색상용 문자열로 바꾼다.
        if (player.getActiveStockNewsRefreshesLeft() <= 0) {
            return "flat";
            // 해설: 적용 중인 뉴스가 없거나 알 수 없는 방향이면 중립 상태로 반환한다.
        }
        if (STOCK_NEWS_BOOM.equals(player.getActiveStockNewsTrend())) {
        // 해설: 호황 뉴스면 양수 변동 효과를 준다.
            return "up";
            // 해설: 호황 뉴스는 상승 방향 UI로 표시한다.
        }
        if (STOCK_NEWS_RECESSION.equals(player.getActiveStockNewsTrend())) {
        // 해설: 불황 뉴스면 음수 변동 효과를 준다.
            return "down";
            // 해설: 불황 뉴스는 하락 방향 UI로 표시한다.
        }
        return "flat";
        // 해설: 적용 중인 뉴스가 없거나 알 수 없는 방향이면 중립 상태로 반환한다.
    }

    private void ensureMonthlyIndustryNewsSchedule(Player player) {
    // 해설: 이번 달 주식 업종 뉴스 일정을 확정한다.
        // 매월 처음 확인할 때 이번 달 주식 뉴스 발생 여부와 날짜를 확정한다.
        // 확정값을 저장해야 새로고침이나 서버 재시작 후에도 같은 달 이벤트가 유지된다.
        if (player.hasStockNewsScheduleForCurrentMonth()) {
        // 해설: 이미 이번 달 뉴스 일정이 있으면 다시 뽑지 않는다.
            return;
        }
        if (!rollPercent(INDUSTRY_NEWS_CHANCE_PERCENT)) {
        // 해설: 정해진 확률에 실패하면 이번 달은 주식 뉴스가 없도록 확정한다.
            player.scheduleNoMonthlyStockNews();
            // 해설: 이번 달 주식 뉴스 없음 상태를 저장한다.
            return;
        }
        List<String> industries = stockCatalog.all().stream()
        // 해설: 카탈로그 종목들에서 업종 목록을 만든다.
                .map(StockSpec::industry)
                // 해설: 각 종목에서 업종명만 꺼낸다.
                .distinct()
                // 해설: 중복 업종을 제거한다.
                .toList();
        String industry = industries.get(random.nextInt(industries.size()));
        // 해설: 이번 달 뉴스 대상 업종을 무작위로 고른다.
        String trend = random.nextBoolean() ? STOCK_NEWS_BOOM : STOCK_NEWS_RECESSION;
        // 해설: 뉴스 방향을 호황 또는 불황 중 하나로 고른다.
        player.scheduleMonthlyStockNews(randomStockNewsDay(player), industry, trend);
        // 해설: 뉴스 발생일, 업종, 방향을 플레이어 상태에 저장한다.
    }

    private int randomStockNewsDay(Player player) {
    // 해설: 이번 달 남은 기간 안에서 주식 뉴스 날짜를 뽑는다.
        int firstDay = Math.max(2, player.getDay());
        // 해설: 뉴스 날짜는 최소 2일 이후이거나 현재 날짜 이후로 잡는다.
        int lastDay = player.getDaysInCurrentMonth();
        // 해설: 현재 달의 마지막 날짜를 가져온다.
        if (firstDay >= lastDay) {
        // 해설: 남은 날짜가 없으면 마지막 날을 반환한다.
            return lastDay;
        }
        return random.nextInt(lastDay - firstDay + 1) + firstDay;
    }

    private boolean rollPercent(int percent) {
    // 해설: 정수 퍼센트 확률 판정을 수행한다.
        return random.nextInt(100) < percent;
        // 해설: 0~99 난수가 percent보다 작으면 성공이다.
    }

    private String stockNewsImagePath(String industry, String trend) {
    // 해설: 업종 뉴스 이미지 파일 경로를 만든다.
        String industrySlug = switch (industry) {
        // 해설: 한글/영문 업종명을 파일명에 쓰는 slug로 바꾼다.
            case "IT" -> "it";
            case "식품" -> "food";
            case "유통" -> "retail";
            case "제조" -> "manufacturing";
            case "통신" -> "telecom";
            default -> "unknown";
        };
        return "/assets/stock-news/" + industrySlug + "-" + (STOCK_NEWS_BOOM.equals(trend) ? "boom" : "recession") + ".jpg";
    }

    private double trendEffectPercent(Player player, StockSpec stock) {
    // 해설: 최근 3개 캔들 흐름으로 짧은 추세 효과를 계산한다.
        List<StockPriceHistory> recentRows = stockPriceHistoryRepository.findTop3ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key());
        // 해설: 해당 종목의 최근 가격 이력 3개를 가져온다.
        long risingCount = recentRows.stream()
        // 해설: 최근 캔들 중 상승 캔들 개수를 세기 시작한다.
                .filter(row -> row.getClosePrice() > row.getOpenPrice())
                // 해설: 종가가 시가보다 높은 캔들만 상승으로 본다.
                .count();
        long fallingCount = recentRows.stream()
        // 해설: 최근 캔들 중 하락 캔들 개수를 세기 시작한다.
                .filter(row -> row.getClosePrice() < row.getOpenPrice())
                // 해설: 종가가 시가보다 낮은 캔들만 하락으로 본다.
                .count();
        if (risingCount >= 2) {
        // 해설: 최근 3개 중 2개 이상 상승이면 상승 추세로 본다.
            return TREND_EFFECT_PERCENT;
        }
        if (fallingCount >= 2) {
        // 해설: 최근 3개 중 2개 이상 하락이면 하락 추세로 본다.
            return -TREND_EFFECT_PERCENT;
        }
        return 0.0;
    }

    private StockQuoteView quote(Player player, StockSpec stock, OwnedStock ownedStock) {
    // 해설: 종목 하나의 화면 표시 정보를 계산한다.
        // 화면에 필요한 주식 정보는 엔티티 그대로 넘기지 않고 View record로 조립한다.
        // 이렇게 하면 템플릿은 계산 없이 표시만 담당하고, 계산 규칙은 서비스에 남는다.
        List<StockPriceHistory> latestRows = stockPriceHistoryRepository.findTop2ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key());
        // 해설: 현재가와 전일 대비를 계산하기 위해 최근 가격 이력 2개를 가져온다.
        StockPriceHistory current = latestRows.isEmpty()
        // 해설: 가격 이력이 없으면 기준가로 임시 현재 캔들을 만든다.
                ? new StockPriceHistory(player, stock.key(), stock.basePrice(), stock.basePrice(), stock.basePrice(), stock.basePrice(), initialVolume(stock))
                : latestRows.get(0);
        long previousPrice = latestRows.size() > 1 ? latestRows.get(1).getClosePrice() : current.getClosePrice();
        // 해설: 비교 기준 가격은 직전 캔들 종가이고, 없으면 현재 가격을 사용한다.
        long currentPrice = current.getClosePrice();
        // 해설: 현재가는 최신 캔들의 종가다.
        long changeAmount = currentPrice - previousPrice;
        // 해설: 현재가와 직전가 차이를 계산한다.
        double changePercent = previousPrice == 0 ? 0.0 : changeAmount * 100.0 / previousPrice;
        // 해설: 직전가 대비 등락률을 계산한다. 0으로 나누는 상황은 0%로 처리한다.
        long quantity = ownedStock == null ? 0 : ownedStock.getQuantity();
        // 해설: 보유 정보가 없으면 수량은 0주다.
        long averagePrice = ownedStock == null ? 0 : ownedStock.getAveragePrice();
        // 해설: 보유 정보가 없으면 평균단가는 0이다.
        long valuationProfit = quantity == 0 ? 0 : (currentPrice - averagePrice) * quantity;
        // 해설: 현재가와 평균단가 차이에 수량을 곱해 평가손익을 계산한다.
        List<StockPriceHistory> history = stockPriceHistoryRepository.findTop60ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key());
        // 해설: 차트 표시용 최근 가격 이력 60개를 가져온다.
        Collections.reverse(history);
        // 해설: 조회 결과가 최신순이므로 차트 그리기 좋게 오래된 순서로 뒤집는다.
        ChartScale scale = chartScale(history, currentPrice);
        // 해설: 차트 y좌표 계산에 사용할 최저가/최고가 범위를 만든다.
        return new StockQuoteView(
        // 해설: 계산한 종목 정보를 화면 표시용 record로 묶어 반환한다.
                stock,
                currentPrice,
                previousPrice,
                changeAmount,
                changePercent,
                stockPriceText(currentPrice),
                stockPriceText(previousPrice),
                signedPercent(changePercent),
                signedPrice(changeAmount),
                changeDirection(changeAmount),
                quantity,
                averagePrice,
                stockPriceText(averagePrice),
                valuationProfit,
                profitText(valuationProfit, averagePrice * quantity),
                // 해설: 보유 종목 평가손익을 수익률과 코인 금액이 함께 보이도록 만든다.
                candleViews(history, scale),
                stockPriceText(scale.minPrice()),
                stockPriceText(scale.maxPrice()),
                String.format(Locale.ROOT, "%.1f", priceY(currentPrice, scale)),
                stockPriceText(currentPrice)
        );
    }

    private long currentPrice(Player player, StockSpec stock) {
    // 해설: 종목의 현재가를 가져온다.
        return stockPriceHistoryRepository.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key())
        // 해설: 가장 최근 가격 이력을 조회한다.
                .map(StockPriceHistory::getClosePrice)
                // 해설: 최근 이력이 있으면 종가를 현재가로 사용한다.
                .orElse(stock.basePrice());
                // 해설: 가격 이력이 없으면 종목 기준가를 현재가로 사용한다.
    }

    private long tradeFee(long grossAmount) {
    // 해설: 거래 금액 기준 수수료를 계산한다.
        return (long) Math.ceil(grossAmount * TRADE_FEE_RATE);
        // 해설: 수수료는 올림 처리해 최소 단위 손실이 누락되지 않게 한다.
    }

    private long maxAffordableQuantity(long coin, long price) {
    // 해설: 보유 코인으로 수수료까지 포함해 살 수 있는 최대 수량을 계산한다.
        // 수수료가 수량에 비례해 붙기 때문에 단순 coin / price로는 최대 매수 가능 수량이 틀릴 수 있다.
        // 이분 탐색으로 "수수료 포함 총액이 보유 코인 이하인 가장 큰 수량"을 찾는다.
        long low = 0;
        // 해설: 이분 탐색의 하한이다. 0주는 항상 살 수 있다.
        long high = Math.max(0, coin / price);
        // 해설: 수수료를 무시했을 때 가능한 최대 수량을 상한으로 잡는다.
        while (low < high) {
        // 해설: 하한과 상한이 같아질 때까지 가능한 최대 수량을 좁힌다.
            long mid = (low + high + 1) / 2;
            // 해설: 중간값을 위쪽으로 치우치게 잡아 무한 루프를 막는다.
            long grossAmount = price * mid;
            if (grossAmount + tradeFee(grossAmount) <= coin) {
            // 해설: 중간 수량의 총비용이 보유 코인 이하라면 더 많이 살 수 있는지 탐색한다.
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
        // 해설: 이분 탐색으로 찾은 최대 매수 가능 수량을 반환한다.
    }

    private List<StockCandleView> candleViews(List<StockPriceHistory> history, ChartScale scale) {
    // 해설: 가격 이력을 SVG 캔들 차트 좌표로 변환한다.
        // 서버에서 SVG 좌표를 미리 계산한다. 브라우저는 좌표를 받아 그리기만 하므로 JS 차트 라이브러리가 필요 없다.
        if (history.isEmpty()) {
        // 해설: 가격 이력이 없으면 그릴 캔들도 없다.
            return List.of();
        }
        int count = history.size();
        List<StockCandleView> candles = new ArrayList<>();
        int rightEdgeX = 690;
        // 해설: 차트 오른쪽 끝 x좌표 기준값이다.
        int candleSpacing = 11;
        // 해설: 캔들 사이의 가로 간격이다.
        int startX = rightEdgeX - ((count - 1) * candleSpacing);
        // 해설: 캔들 개수에 맞춰 첫 캔들의 x좌표를 계산한다.
        for (int index = 0; index < count; index++) {
        // 해설: 각 가격 이력을 하나의 캔들 좌표로 변환한다.
            StockPriceHistory row = history.get(index);
            int x = startX + (index * candleSpacing);
            int openY = (int) Math.round(priceY(row.getOpenPrice(), scale));
            // 해설: 시가를 차트 y좌표로 변환한다.
            int highY = (int) Math.round(priceY(row.getHighPrice(), scale));
            // 해설: 고가를 차트 y좌표로 변환한다.
            int lowY = (int) Math.round(priceY(row.getLowPrice(), scale));
            // 해설: 저가를 차트 y좌표로 변환한다.
            int closeY = (int) Math.round(priceY(row.getClosePrice(), scale));
            // 해설: 종가를 차트 y좌표로 변환한다.
            candles.add(new StockCandleView(
            // 해설: 계산된 좌표와 상승/하락 여부를 캔들 View로 저장한다.
                    x,
                    openY,
                    highY,
                    lowY,
                    closeY,
                    Math.min(openY, closeY),
                    Math.max(2, Math.abs(openY - closeY)),
                    row.getClosePrice() >= row.getOpenPrice(),
                    row.getMonth() + "/" + row.getDay()
            ));
        }
        return candles;
    }

    private ChartScale chartScale(List<StockPriceHistory> history, long fallbackPrice) {
    // 해설: 차트 y축 최저/최고 범위를 계산한다.
        long min = history.stream().map(StockPriceHistory::getLowPrice).min(Comparator.naturalOrder()).orElse(fallbackPrice);
        // 해설: 이력의 저가 중 최솟값을 찾고, 없으면 현재가를 쓴다.
        long max = history.stream().map(StockPriceHistory::getHighPrice).max(Comparator.naturalOrder()).orElse(fallbackPrice);
        // 해설: 이력의 고가 중 최댓값을 찾고, 없으면 현재가를 쓴다.
        if (min == max) {
        // 해설: 최저가와 최고가가 같으면 차트 높이 계산이 불가능하므로 여백을 만든다.
            long padding = Math.max(1L, min / 20L);
            min = Math.max(1L, min - padding);
            max += padding;
        }
        return new ChartScale(min, max);
    }

    private double priceY(long price, ChartScale scale) {
    // 해설: 가격을 SVG 차트의 y좌표로 변환한다.
        return 24.0 + (scale.maxPrice() - price) * 220.0 / Math.max(1L, scale.maxPrice() - scale.minPrice());
        // 해설: 높은 가격일수록 위쪽에 오도록 y좌표를 계산한다.
    }

    private double randomChangePercent(double min, double max) {
        return random.nextDouble(min, max);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private long initialVolume(StockSpec stock) {
        return Math.max(100L, stock.basePrice() / 100L);
    }

    private long randomVolume(StockSpec stock, double changePercent) {
        double multiplier = 0.8 + random.nextDouble(0.0, 1.2) + Math.abs(changePercent) / 10.0;
        return Math.max(100L, Math.round(initialVolume(stock) * multiplier));
    }

    private String changeDirection(long changeAmount) {
        if (changeAmount > 0) {
            return "up";
            // 해설: 호황 뉴스는 상승 방향 UI로 표시한다.
        }
        if (changeAmount < 0) {
            return "down";
            // 해설: 불황 뉴스는 하락 방향 UI로 표시한다.
        }
        return "flat";
        // 해설: 적용 중인 뉴스가 없거나 알 수 없는 방향이면 중립 상태로 반환한다.
    }

    private String signedPercent(double percent) {
        String sign = percent > 0 ? "+" : "";
        return sign + String.format(Locale.ROOT, "%.2f%%", percent);
    }

    private String profitText(long profit, long costBasis) {
    // 해설: 평가손익 표시 문자열을 만든다.
        if (costBasis <= 0) {
        // 해설: 원가가 없으면 수익률을 계산할 수 없으므로 금액만 표시한다.
            return signedPrice(profit);
        }
        return "(" + compactSignedPercent(profit * 100.0 / costBasis) + ") " + signedPrice(profit);
        // 해설: 수익률을 먼저 괄호로 표시하고 뒤에 손익 코인 금액을 붙인다.
    }

    private String compactSignedPercent(double percent) {
    // 해설: 손익률을 너무 길지 않게 표시한다.
        String sign = percent > 0 ? "+" : "";
        double rounded = Math.round(percent * 10.0) / 10.0;
        if (rounded == Math.rint(rounded)) {
            return sign + String.format(Locale.ROOT, "%.0f%%", rounded);
        }
        return sign + String.format(Locale.ROOT, "%.1f%%", rounded);
    }

    private String signedPrice(long amount) {
    // 해설: 코인 금액에 + 또는 - 부호를 붙여 표시한다.
        if (amount == 0) {
            return "0코인";
        }
        return (amount > 0 ? "+" : "-") + stockPriceText(Math.abs(amount));
    }

    private String stockPriceText(long amount) {
    // 해설: 코인 금액을 억/만 단위로 줄여 표시한다.
        if (amount == 0) {
            return "0코인";
        }
        long eok = amount / 100_000_000L;
        amount %= 100_000_000L;
        long man = amount / 10_000L;
        long won = amount % 10_000L;
        StringBuilder builder = new StringBuilder();
        if (eok > 0) {
            builder.append(eok).append("억");
        }
        if (man > 0) {
            builder.append(man).append("만");
        }
        if (won > 0 || builder.isEmpty()) {
            builder.append(won);
        }
        return builder.append("코인").toString();
    }

    public String coinText(long amount) {
    // 해설: 외부 서비스나 컨트롤러가 코인 금액 포맷팅을 사용할 수 있게 공개한다.
        return stockPriceText(amount);
        // 해설: 코인 표기는 stockPriceText 규칙을 그대로 사용한다.
    }

    private record ChartScale(long minPrice, long maxPrice) {
    }

    private record StockChange(double percent, boolean hasShock) {
    }
}
```
