# ShopService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/ShopService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
package com.game.buildingstory.service;

import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedGiftItem;
import com.game.buildingstory.domain.OwnedLuxuryItem;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedGiftItemRepository;
import com.game.buildingstory.repo.OwnedLuxuryItemRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopService {
// 해설: 명품, 기부, 선물 구매와 비서 선물 사용을 담당하는 상점 서비스다.
    /*
     * 명품, 기부, 선물 구매/사용을 담당한다.
     *
     * 상점 기능은 현금을 평판, 비서 호감도, 보유 아이템으로 바꾸는 역할을 한다.
     * 실제 보유 수량은 OwnedLuxuryItem/OwnedGiftItem 엔티티에 저장된다.
     */
    private static final int RECORD_RETENTION_DAYS = 62;

    private final PlayerRepository playerRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final OwnedLuxuryItemRepository ownedLuxuryItemRepository;
    private final OwnedGiftItemRepository ownedGiftItemRepository;
    private final OwnedSecretaryRepository ownedSecretaryRepository;
    private final ReputationCatalog reputationCatalog;
    private final SecretaryCatalog secretaryCatalog;
    private final LuxuryItemCatalog luxuryItemCatalog;
    private final GiftItemCatalog giftItemCatalog;

    public ShopService(
    // 해설: 상점 처리에 필요한 플레이어, 기록, 보유 아이템, 비서 Repository와 카탈로그를 생성자 주입으로 받는다.
            PlayerRepository playerRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            OwnedLuxuryItemRepository ownedLuxuryItemRepository,
            OwnedGiftItemRepository ownedGiftItemRepository,
            OwnedSecretaryRepository ownedSecretaryRepository,
            ReputationCatalog reputationCatalog,
            SecretaryCatalog secretaryCatalog,
            LuxuryItemCatalog luxuryItemCatalog,
            GiftItemCatalog giftItemCatalog
    ) {
        this.playerRepository = playerRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.ownedLuxuryItemRepository = ownedLuxuryItemRepository;
        this.ownedGiftItemRepository = ownedGiftItemRepository;
        this.ownedSecretaryRepository = ownedSecretaryRepository;
        this.reputationCatalog = reputationCatalog;
        this.secretaryCatalog = secretaryCatalog;
        this.luxuryItemCatalog = luxuryItemCatalog;
        this.giftItemCatalog = giftItemCatalog;
    }

    public List<LuxuryItemSpec> luxuryItems() {
    // 해설: 상점에 표시할 명품 아이템 목록을 반환한다.
        return luxuryItemCatalog.all();
    }

    public List<GiftItemSpec> giftItems() {
    // 해설: 상점에 표시할 선물 아이템 목록을 반환한다.
        return giftItemCatalog.all();
    }

    public int ownedGiftQuantity(Player player, GiftItemSpec gift) {
    // 해설: 플레이어가 특정 선물을 몇 개 보유 중인지 조회한다.
        return ownedGiftItemRepository.findByPlayerAndGiftKey(player, gift.key())
                .map(OwnedGiftItem::getQuantity)
                // 해설: 보유 선물 엔티티가 있으면 수량만 꺼낸다.
                .orElse(0);
                // 해설: 보유 기록이 없으면 0개로 처리한다.
    }

    public int maxGiftQuantityForSecretary(Player player, OwnedSecretary secretary, GiftItemSpec gift) {
    // 해설: 현재 보유 수량과 비서 호감도 상태 기준으로 선물 가능한 최대 수량을 계산한다.
        int ownedQuantity = ownedGiftQuantity(player, gift);
        // 해설: 먼저 플레이어가 가진 선물 수량을 조회한다.
        return maxGiftQuantityForSecretary(secretary, gift, ownedQuantity);
    }

    public boolean isLuxuryItemOwned(Player player, LuxuryItemSpec item) {
    // 해설: 특정 명품 아이템을 이미 구매했는지 확인한다.
        return ownedLuxuryItemRepository.findByPlayerAndItemKey(player, item.key()).isPresent();
    }

    public String donate(long playerId, int multiplier) {
    // 해설: 기부를 통해 현금을 평판으로 바꾸는 요청을 처리한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 일시정지 중에는 경제 행동을 막는다.
            return pausedActionMessage();
        }
        int safeMultiplier = switch (multiplier) {
        // 해설: 허용된 기부 배율만 사용하고, 그 외 값은 1배로 보정한다.
            case 10, 100, 1000 -> multiplier;
            default -> 1;
        };
        long amount = 50_000L * safeMultiplier;
        // 해설: 기부 배율에 따라 실제 기부 금액을 계산한다.
        int reputationGain = safeMultiplier;
        // 해설: 기부 평판 보상은 배율만큼 증가한다.
        if (!player.spendCash(amount)) {
        // 해설: 기부 금액을 현금에서 차감한다. 부족하면 실패한다.
            return "기부금 부족 · 필요 금액 " + amount + "원";
        }
        player.addReputation(reputationGain);
        // 해설: 기부 성공 시 평판을 증가시킨다.
        refreshTitle(player);
        // 해설: 평판 변화 후 칭호를 갱신한다.
        saveRecord(player, RecordType.AD_COST, "기부", -amount, reputationGain, null, "현금 50,000원당 평판 1");
        // 해설: 기부 지출과 평판 증가를 월간 기록에 저장한다.
        return "기부 완료 · 평판 +" + reputationGain;
    }

    public String buyLuxuryItem(long playerId, String itemKey) {
    // 해설: 명품 아이템 구매를 처리한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 일시정지 중에는 경제 행동을 막는다.
            return pausedActionMessage();
        }
        LuxuryItemSpec item = luxuryItemCatalog.find(itemKey).orElseThrow();
        // 해설: 요청한 명품 키로 아이템 스펙을 찾는다.
        if (ownedLuxuryItemRepository.findByPlayerAndItemKey(player, item.key()).isPresent()) {
        // 해설: 이미 구매한 명품은 중복 구매하지 못하게 막는다.
            return "이미 구매한 아이템";
        }
        if (!player.spendCash(item.price())) {
        // 해설: 명품 가격을 현금에서 차감한다.
            return "현금 부족 · 필요 금액 " + item.price() + "원";
        }
        int reputationGain = item.reputationReward();
        // 해설: 명품 구매로 얻는 평판 보상을 가져온다.
        player.addReputation(reputationGain);
        // 해설: 기부 성공 시 평판을 증가시킨다.
        refreshTitle(player);
        // 해설: 평판 변화 후 칭호를 갱신한다.
        ownedLuxuryItemRepository.save(new OwnedLuxuryItem(player, item.key()));
        // 해설: 명품 보유 기록을 저장한다.
        saveRecord(player, RecordType.BUILDING_BUY, "사치품 구매", -item.price(), reputationGain, item.name(), "기부 대비 1.5배 효율 · 1회 구매");
        return item.name() + " 구매 완료 · 평판 +" + reputationGain;
    }

    public String buyGiftItem(long playerId, String giftKey, int quantity) {
    // 해설: 비서에게 줄 선물 아이템을 구매한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 일시정지 중에는 경제 행동을 막는다.
            return pausedActionMessage();
        }
        GiftItemSpec gift = giftItemCatalog.find(giftKey).orElseThrow();
        // 해설: 요청한 선물 키로 선물 스펙을 찾는다.
        int safeQuantity = Math.max(1, Math.min(99, quantity));
        // 해설: 구매 수량을 1~99 범위로 보정한다.
        long totalPrice = gift.price() * safeQuantity;
        // 해설: 단가와 수량으로 총 구매 금액을 계산한다.
        if (!player.spendCash(totalPrice)) {
            return "현금 부족 · 필요 금액 " + totalPrice + "원";
        }
        OwnedGiftItem ownedGift = ownedGiftItemRepository.findByPlayerAndGiftKey(player, gift.key())
        // 해설: 기존 보유 선물 기록을 찾는다.
                .orElseGet(() -> ownedGiftItemRepository.save(new OwnedGiftItem(player, gift.key(), 0)));
                // 해설: 처음 사는 선물이면 수량 0으로 보유 기록을 먼저 만든다.
        ownedGift.addQuantity(safeQuantity);
        // 해설: 구매 수량만큼 보유 선물 수량을 늘린다.
        saveRecord(player, RecordType.BUILDING_BUY, "선물 구매", -totalPrice, 0, gift.name(), safeQuantity + "개");
        return gift.name() + " " + safeQuantity + "개 구매 완료";
    }

    public String giveGiftToSecretary(long playerId, long ownedSecretaryId, String giftKey, int quantity) {
    // 해설: 보유 선물을 특정 비서에게 주어 호감도 경험치를 올린다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 일시정지 중에는 경제 행동을 막는다.
            return pausedActionMessage();
        }
        OwnedSecretary secretary = ownedSecretaryRepository.findById(ownedSecretaryId).orElseThrow();
        // 해설: 선물을 받을 보유 비서를 조회한다.
        if (!secretary.getPlayer().getId().equals(player.getId())) {
        // 해설: 다른 플레이어의 비서에게 선물하지 못하게 검증한다.
            throw new IllegalArgumentException("잘못된 비서");
        }
        GiftItemSpec gift = giftItemCatalog.find(giftKey).orElseThrow();
        // 해설: 요청한 선물 키로 선물 스펙을 찾는다.
        if (secretary.getAffinity() < gift.minAffinityLevel() || secretary.getAffinity() > gift.maxAffinityLevel()) {
        // 해설: 선물마다 사용 가능한 호감도 구간이 맞는지 검사한다.
            return "호감도 구간에 맞지 않는 선물";
        }
        int safeQuantity = Math.max(1, Math.min(99, quantity));
        // 해설: 구매 수량을 1~99 범위로 보정한다.
        OwnedGiftItem ownedGift = ownedGiftItemRepository.findByPlayerAndGiftKey(player, gift.key()).orElse(null);
        // 해설: 플레이어가 해당 선물을 보유 중인지 조회한다.
        if (ownedGift == null || ownedGift.getQuantity() < safeQuantity) {
        // 해설: 보유 수량이 요청 수량보다 적으면 선물할 수 없다.
            return "선물 수량 부족";
        }
        int maxGiftQuantity = maxGiftQuantityForSecretary(secretary, gift, ownedGift.getQuantity());
        // 해설: 현재 호감도 구간에서 실제로 줄 수 있는 최대 수량을 계산한다.
        if (safeQuantity > maxGiftQuantity) {
            return "현재 호감도 구간에서 선물 가능한 수량 초과";
        }
        ownedGift.spendQuantity(safeQuantity);
        // 해설: 선물한 수량만큼 보유 선물을 차감한다.
        int beforeAffinity = secretary.getAffinity();
        // 해설: 기록 메모에 남기기 위해 선물 전 호감도를 저장한다.
        secretary.addAffinityExperience(gift.affinityExperience() * safeQuantity);
        // 해설: 선물 경험치를 비서 호감도 경험치에 더한다.
        saveRecord(player, RecordType.BUILDING_BUY, "비서 선물", null, 0, gift.name(), "호감도 " + beforeAffinity + " -> " + secretary.getAffinity());
        SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElseThrow();
        return spec.name() + "에게 " + gift.name() + " " + safeQuantity + "개 선물 완료";
    }

    private int maxGiftQuantityForSecretary(OwnedSecretary secretary, GiftItemSpec gift, int ownedQuantity) {
    // 해설: 보유 수량, 호감도 구간, 최대 호감도 30을 고려해 선물 가능한 수량을 시뮬레이션한다.
        int maxByOwned = Math.max(0, Math.min(99, ownedQuantity));
        // 해설: 계산에 사용할 보유 수량을 0~99 범위로 제한한다.
        int affinity = secretary.getAffinity();
        int affinityExperience = secretary.getAffinityExperience();
        int usableQuantity = 0;
        for (int i = 0; i < maxByOwned; i++) {
        // 해설: 한 개씩 선물한다고 가정하며 호감도 변화를 시뮬레이션한다.
            if (affinity < gift.minAffinityLevel() || affinity > gift.maxAffinityLevel() || affinity >= 30) {
            // 해설: 선물 가능 구간을 벗어나거나 최대 호감도에 도달하면 중단한다.
                break;
            }
            usableQuantity++;
            affinityExperience += gift.affinityExperience();
            while (affinity < 30 && affinityExperience >= requiredAffinityExperience(affinity)) {
            // 해설: 경험치가 다음 호감도 요구치를 넘으면 호감도를 올린다.
                affinityExperience -= requiredAffinityExperience(affinity);
                affinity++;
            }
            if (affinity >= 30) {
                affinity = 30;
                affinityExperience = 0;
            }
        }
        return usableQuantity;
    }

    private int requiredAffinityExperience(int affinity) {
    // 해설: 현재 호감도에서 다음 호감도로 가기 위한 필요 경험치를 계산한다.
        return affinity >= 30 ? 0 : affinity + 2;
    }

    private String pausedActionMessage() {
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private void refreshTitle(Player player) {
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 상점 행동 결과를 월간 기록에 저장하는 공통 메서드다.
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
```