package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyCashTransaction;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCashTransactionRepository;
import org.springframework.stereotype.Service;

/**
 * 법인 현금의 실제 증감과 거래 기록을 한 경계에서 처리한다.
 * 동일 eventKey는 한 번만 기록되어 월 정산 재호출이나 중복 요청이 장부를 부풀리지 않는다.
 */
@Service
public class CompanyCashLedgerService {
    private final CompanyCashTransactionRepository transactionRepository;

    public CompanyCashLedgerService(CompanyCashTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public boolean deposit(
            PlayerCompany company,
            String eventKey,
            CompanyCashFlowType flowType,
            String description,
            long amount
    ) {
        if (amount <= 0 || alreadyRecorded(company, eventKey)) {
            return false;
        }
        company.addCorporateCash(amount);
        save(company, eventKey, flowType, description, amount);
        return true;
    }

    public boolean withdraw(
            PlayerCompany company,
            String eventKey,
            CompanyCashFlowType flowType,
            String description,
            long amount
    ) {
        if (amount <= 0 || alreadyRecorded(company, eventKey) || !company.spendCorporateCash(amount)) {
            return false;
        }
        save(company, eventKey, flowType, description, -amount);
        return true;
    }

    /**
     * 현금 변경을 다른 도메인 메서드가 이미 수행한 경우 그 결과만 장부에 남긴다.
     * 운영중단처럼 잔액을 한 번에 0원으로 만드는 흐름에 사용한다.
     */
    public void recordApplied(
            PlayerCompany company,
            String eventKey,
            CompanyCashFlowType flowType,
            String description,
            long signedAmount
    ) {
        if (signedAmount == 0 || alreadyRecorded(company, eventKey)) {
            return;
        }
        save(company, eventKey, flowType, description, signedAmount);
    }

    private boolean alreadyRecorded(PlayerCompany company, String eventKey) {
        return transactionRepository.findByCompanyAndEventKey(company, eventKey).isPresent();
    }

    private void save(
            PlayerCompany company,
            String eventKey,
            CompanyCashFlowType flowType,
            String description,
            long signedAmount
    ) {
        Player player = company.getPlayer();
        transactionRepository.save(new CompanyCashTransaction(
                company,
                eventKey,
                flowType,
                description,
                signedAmount,
                player.getElapsedDays(),
                periodIndex(player)
        ));
    }

    private int periodIndex(Player player) {
        int gameYear = Math.max(0, player.getElapsedDays() - 1) / 365 + 1;
        return (gameYear - 1) * 12 + player.getMonth() - 1;
    }
}
