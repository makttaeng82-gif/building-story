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
        return luxuryItemCatalog.all();
    }

    public List<GiftItemSpec> giftItems() {
        return giftItemCatalog.all();
    }

    public int ownedGiftQuantity(Player player, GiftItemSpec gift) {
        return ownedGiftItemRepository.findByPlayerAndGiftKey(player, gift.key())
                .map(OwnedGiftItem::getQuantity)
                .orElse(0);
    }

    public int maxGiftQuantityForSecretary(Player player, OwnedSecretary secretary, GiftItemSpec gift) {
        int ownedQuantity = ownedGiftQuantity(player, gift);
        return maxGiftQuantityForSecretary(secretary, gift, ownedQuantity);
    }

    public boolean isLuxuryItemOwned(Player player, LuxuryItemSpec item) {
        return ownedLuxuryItemRepository.findByPlayerAndItemKey(player, item.key()).isPresent();
    }

    public String donate(long playerId, int multiplier) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        int safeMultiplier = switch (multiplier) {
            case 10, 100, 1000 -> multiplier;
            default -> 1;
        };
        long amount = 50_000L * safeMultiplier;
        int reputationGain = safeMultiplier;
        if (!player.spendCash(amount)) {
            return "기부금 부족 · 필요 금액 " + amount + "원";
        }
        player.addReputation(reputationGain);
        refreshTitle(player);
        saveRecord(player, RecordType.AD_COST, "기부", -amount, reputationGain, null, "현금 50,000원당 평판 1");
        return "기부 완료 · 평판 +" + reputationGain;
    }

    public String buyLuxuryItem(long playerId, String itemKey) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        LuxuryItemSpec item = luxuryItemCatalog.find(itemKey).orElseThrow();
        if (ownedLuxuryItemRepository.findByPlayerAndItemKey(player, item.key()).isPresent()) {
            return "이미 구매한 아이템";
        }
        if (!player.spendCash(item.price())) {
            return "현금 부족 · 필요 금액 " + item.price() + "원";
        }
        int reputationGain = item.reputationReward();
        player.addReputation(reputationGain);
        refreshTitle(player);
        ownedLuxuryItemRepository.save(new OwnedLuxuryItem(player, item.key()));
        saveRecord(player, RecordType.BUILDING_BUY, "사치품 구매", -item.price(), reputationGain, item.name(), "기부 대비 1.5배 효율 · 1회 구매");
        return item.name() + " 구매 완료 · 평판 +" + reputationGain;
    }

    public String buyGiftItem(long playerId, String giftKey, int quantity) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        GiftItemSpec gift = giftItemCatalog.find(giftKey).orElseThrow();
        int safeQuantity = Math.max(1, Math.min(99, quantity));
        long totalPrice = gift.price() * safeQuantity;
        if (!player.spendCash(totalPrice)) {
            return "현금 부족 · 필요 금액 " + totalPrice + "원";
        }
        OwnedGiftItem ownedGift = ownedGiftItemRepository.findByPlayerAndGiftKey(player, gift.key())
                .orElseGet(() -> ownedGiftItemRepository.save(new OwnedGiftItem(player, gift.key(), 0)));
        ownedGift.addQuantity(safeQuantity);
        saveRecord(player, RecordType.BUILDING_BUY, "선물 구매", -totalPrice, 0, gift.name(), safeQuantity + "개");
        return gift.name() + " " + safeQuantity + "개 구매 완료";
    }

    public String giveGiftToSecretary(long playerId, long ownedSecretaryId, String giftKey, int quantity) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        OwnedSecretary secretary = ownedSecretaryRepository.findById(ownedSecretaryId).orElseThrow();
        if (!secretary.getPlayer().getId().equals(player.getId())) {
            throw new IllegalArgumentException("잘못된 비서");
        }
        GiftItemSpec gift = giftItemCatalog.find(giftKey).orElseThrow();
        if (secretary.getAffinity() < gift.minAffinityLevel() || secretary.getAffinity() > gift.maxAffinityLevel()) {
            return "호감도 구간에 맞지 않는 선물";
        }
        int safeQuantity = Math.max(1, Math.min(99, quantity));
        OwnedGiftItem ownedGift = ownedGiftItemRepository.findByPlayerAndGiftKey(player, gift.key()).orElse(null);
        if (ownedGift == null || ownedGift.getQuantity() < safeQuantity) {
            return "선물 수량 부족";
        }
        int maxGiftQuantity = maxGiftQuantityForSecretary(secretary, gift, ownedGift.getQuantity());
        if (safeQuantity > maxGiftQuantity) {
            return "현재 호감도 구간에서 선물 가능한 수량 초과";
        }
        ownedGift.spendQuantity(safeQuantity);
        int beforeAffinity = secretary.getAffinity();
        secretary.addAffinityExperience(gift.affinityExperience() * safeQuantity);
        saveRecord(player, RecordType.BUILDING_BUY, "비서 선물", null, 0, gift.name(), "호감도 " + beforeAffinity + " -> " + secretary.getAffinity());
        SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElseThrow();
        return spec.name() + "에게 " + gift.name() + " " + safeQuantity + "개 선물 완료";
    }

    private int maxGiftQuantityForSecretary(OwnedSecretary secretary, GiftItemSpec gift, int ownedQuantity) {
        int maxByOwned = Math.max(0, Math.min(99, ownedQuantity));
        int affinity = secretary.getAffinity();
        int affinityExperience = secretary.getAffinityExperience();
        int usableQuantity = 0;
        for (int i = 0; i < maxByOwned; i++) {
            if (affinity < gift.minAffinityLevel() || affinity > gift.maxAffinityLevel() || affinity >= 30) {
                break;
            }
            usableQuantity++;
            affinityExperience += gift.affinityExperience();
            while (affinity < 30 && affinityExperience >= requiredAffinityExperience(affinity)) {
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
        return affinity >= 30 ? 0 : affinity + 2;
    }

    private String pausedActionMessage() {
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private void refreshTitle(Player player) {
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
