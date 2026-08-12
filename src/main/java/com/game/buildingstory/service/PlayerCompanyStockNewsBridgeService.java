package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.CompanyNewsArticle;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** 상장 후 발생한 실제 기업뉴스만 해당 플레이어의 주식 뉴스로 전달한다. */
@Service
public class PlayerCompanyStockNewsBridgeService {
    private static final String EVENT_PREFIX = "player-company:";

    private final CompanyListingRepository listingRepository;
    private final StockNewsArticleRepository stockNewsRepository;

    public PlayerCompanyStockNewsBridgeService(
            CompanyListingRepository listingRepository,
            StockNewsArticleRepository stockNewsRepository
    ) {
        this.listingRepository = listingRepository;
        this.stockNewsRepository = stockNewsRepository;
    }

    @Transactional
    public void publish(
            CompanyNewsArticle source,
            StockNewsDirection direction,
            int absolutePriceImpactBasisPoints,
            int durationRefreshes
    ) {
        var company = source.getCompany();
        boolean listed = listingRepository.findByCompany(company)
                .filter(listing -> listing.getStatus() == CompanyListingStatus.LISTED)
                .isPresent();
        if (!listed) {
            return;
        }

        String eventKey = EVENT_PREFIX + source.getEventKey();
        if (stockNewsRepository.existsByPlayerAndEventKey(company.getPlayer(), eventKey)) {
            return;
        }

        stockNewsRepository.save(new com.game.buildingstory.domain.StockNewsArticle(
                company.getPlayer(),
                eventKey,
                EVENT_PREFIX + source.getCategory().name().toLowerCase(Locale.ROOT),
                "IT",
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                company.getCompanyName(),
                direction,
                source.getSource(),
                source.getTitle(),
                source.getBody(),
                marketMeaning(source),
                direction.signed(absolutePriceImpactBasisPoints),
                durationRefreshes
        ));
    }

    private String marketMeaning(CompanyNewsArticle source) {
        return switch (source.getCategory()) {
            case PRODUCT -> "제품 경쟁력 변화는 향후 이용자와 반복매출 지표에 반영될 수 있다.";
            case INFRASTRUCTURE -> "연산 처리능력과 운영비 변화는 이후 서비스 수익성에 반영될 수 있다.";
            case INCIDENT -> "장애 대응 속도에 따라 고객 이탈과 계약 유지에 미치는 영향이 달라질 수 있다.";
            case PERFORMANCE -> "확정 실적은 기업가치와 다음 분기 시장 기대의 기준이 된다.";
            case FINANCE -> "자본조달과 지분 변화는 기업의 성장 여력과 주주가치에 영향을 줄 수 있다.";
            case AI_MARKET, MARKET -> "시장 성장률 변화는 유료 이용자와 요금제 전환 속도에 영향을 줄 수 있다.";
            case COMPETITOR -> "경쟁사 제품력 변화는 플레이어 기업의 시장점유율에 영향을 줄 수 있다.";
            case REGULATION -> "규제 변화는 개발비와 계약 조건에 영향을 줄 수 있다.";
            case ENTERPRISE_DEMAND -> "기업 수요 변화는 신규 계약 규모와 매출 전망에 영향을 줄 수 있다.";
        };
    }
}
