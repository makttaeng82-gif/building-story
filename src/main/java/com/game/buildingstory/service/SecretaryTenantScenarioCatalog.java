package com.game.buildingstory.service;

import java.util.List;
import java.util.Optional;

final class SecretaryTenantScenarioCatalog {
    private static final List<SecretaryTenantScenario> SCENARIOS = List.of(
            new SecretaryTenantScenario(
                    "secretary-1", "청주", 1,
                    "회사후배 입주",
                    "회사 후배가 급하게 지낼 곳을 찾고 있다. 첫 건물에 세입자로 입주한다.",
                    "월세 감면 부탁",
                    "최근 사정이 좋지 않아 두 달만 월세를 깎아달라고 부탁해왔다.",
                    "월세 감면해주기",
                    100_000_000L, 0, null, false, false, 0, 60,
                    "월세 감면"
            ),
            new SecretaryTenantScenario(
                    "secretary-2", "세종", 2,
                    "소개받은 세입자",
                    "청주 비서 동생의 소개로 도담동 24평형 아파트에 입주했다.",
                    "피해 보상 부탁",
                    "사기를 당해 보상을 해야 하는데 당장 돈이 없어 도움을 부탁해왔다.",
                    "대신 갚아주기",
                    300_000_000L, 15_500, null, false, false, 100_000_000, 0,
                    null
            ),
            new SecretaryTenantScenario(
                    "secretary-3", "대전", 2,
                    "중개인 세입자",
                    "봉명동 상가주택을 중개하던 중 마침 입주할 곳을 찾고 있다며 들어왔다.",
                    "건물관리 견습 부탁",
                    "건물관리에 흥미를 느껴 중개일을 하는 김에 직접 배우고 싶다고 부탁해왔다.",
                    "일 배우게 하기",
                    0L, 50_500, "grandeur", false, false, 0, 30,
                    "건물관리 견습중"
            ),
            new SecretaryTenantScenario(
                    "secretary-4", "부산", 2,
                    "재택근무 세입자",
                    "세무일을 재택근무로 하게 되었다며 센텀시티에 입주했다.",
                    "위약금 지원 부탁",
                    "스카우트 제안을 긍정적으로 생각하지만 현재 회사의 위약금이 너무 커서 망설이고 있다.",
                    "위약금 대신 내주기",
                    15_000_000_000L, 455_000, null, false, false, 9_000_000_000L, 0,
                    null
            ),
            new SecretaryTenantScenario(
                    "secretary-5", "인천", 2,
                    "재무설계 세입자",
                    "주인공이 재무설계를 부탁하며 송도 센트럴파크에 입주시켰다. 이 기간에는 월세를 받지 않는다.",
                    "사업자금 투자 제안",
                    "재무상태가 훌륭하다며 함께 일하고 싶다고 했다. 대신 사업 규모를 키울 자금이 필요하다고 한다.",
                    "사업자금 투자하기",
                    200_000_000_000L, 1_310_000, null, true, false, 100_000_000_000L, 0,
                    "재무설계 월세 면제"
            ),
            new SecretaryTenantScenario(
                    "secretary-6", "서울", 1,
                    "임시 거주 세입자",
                    "급하게 지낼 곳이 필요했는데 임시로 살 집을 구해 다행이라며 트리플렛 타워에 입주했다.",
                    "거주환경 스카우트 제안",
                    "주인공이 함께 일하자며 더 나은 거주환경을 마련해주겠다고 스카우트를 제안했다.",
                    "거주환경 마련하기",
                    10_000_000_000_000L, 0, null, false, true, 0, 0,
                    null
            )
    );

    private SecretaryTenantScenarioCatalog() {
    }

    static Optional<SecretaryTenantScenario> find(String secretaryKey) {
        return SCENARIOS.stream()
                .filter(candidate -> candidate.secretaryKey().equals(secretaryKey))
                .findFirst();
    }

    static Optional<SecretaryTenantScenario> findByCityAndSlot(String city, int slot) {
        return SCENARIOS.stream()
                .filter(candidate -> candidate.city().equals(city))
                .filter(candidate -> candidate.buildingSlot() == slot)
                .findFirst();
    }

    static String moveInConditionText(String secretaryKey) {
        return switch (secretaryKey) {
            case "secretary-1" -> "1월 3일 청주 첫 건물 입주 이벤트";
            case "secretary-2" -> "세종 도담동 24평형 아파트 첫 구매";
            case "secretary-3" -> "대전 봉명동 상가주택 첫 구매";
            case "secretary-4" -> "부산 센텀시티 첫 구매";
            case "secretary-5" -> "인천 송도 센트럴파크 첫 구매";
            case "secretary-6" -> "서울 트리플렛 타워 첫 구매";
            default -> "-";
        };
    }

    static String hireConditionText(SecretarySpec secretary, String fallbackReputationText) {
        return switch (secretary.key()) {
            case "secretary-1" -> "현금 1억 최초 달성 후 월세 2달 감면 수락, 2달 경과";
            case "secretary-2" -> "평판 1만5500 이상, 현금 3억 이상, 부탁 수락 후 1억원 지급";
            case "secretary-3" -> "평판 5만500 이상, 그렌져 보유, 부탁 수락 후 30일 경과";
            case "secretary-4" -> "평판 45만5000 이상, 현금 150억 이상, 부탁 수락 후 90억 지급";
            case "secretary-5" -> "평판 131만 이상, 현금 2000억 이상, 대출 없음, 부탁 수락 후 1000억 지급";
            case "secretary-6" -> "모든 사치품 보유, 현금 10조 이상, 반포 자이맹 리 구매";
            default -> "평판 " + fallbackReputationText + " 이상";
        };
    }
}
