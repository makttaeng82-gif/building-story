package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BuildingCatalog {
    /*
     * 게임에 존재하는 건물 원본 목록이다.
     *
     * 이 목록은 DB에 저장되는 플레이어 상태가 아니라, 게임 밸런스 데이터다.
     * 매물을 만들거나 경매를 만들 때 여기서 BuildingSpec을 찾아 가격/월세/쿨다운을 복사한다.
     */
    private final List<BuildingSpec> specs = List.of(
            new BuildingSpec("청주", 1, "원룸", "사창동 12평 원룸", 30_000_000L, 250_000L, cooldown(4)),
            new BuildingSpec("청주", 2, "구식 오피스텔", "복대동 구식 오피스텔", 70_000_000L, 500_000L, cooldown(8)),
            new BuildingSpec("청주", 3, "소형 아파트", "율량동 소형 아파트", 140_000_000L, 900_000L, cooldown(11)),
            new BuildingSpec("청주", 4, "21평형 아파트", "가경동 21평형 아파트", 260_000_000L, 1_500_000L, cooldown(14)),

            new BuildingSpec("세종", 1, "1층 상가", "나성동 1층 상가", 400_000_000L, 2_200_000L, cooldown(17)),
            new BuildingSpec("세종", 2, "아파트 2채", "도담동 아파트 2채", 600_000_000L, 3_100_000L, cooldown(21)),
            new BuildingSpec("세종", 3, "오피스텔 4실", "어진동 오피스텔 4실", 900_000_000L, 4_500_000L, cooldown(25)),
            new BuildingSpec("세종", 4, "상가주택", "고운동 상가주택", 1_300_000_000L, 6_200_000L, cooldown(30)),

            new BuildingSpec("대전", 1, "다가구주택", "대흥동 다가구주택", 1_800_000_000L, 8_500_000L, cooldown(34)),
            new BuildingSpec("대전", 2, "상가주택", "봉명동 상가주택", 2_800_000_000L, 13_000_000L, cooldown(41)),
            new BuildingSpec("대전", 3, "의원 빌딩", "둔산동 의원 빌딩", 4_200_000_000L, 19_000_000L, cooldown(49)),
            new BuildingSpec("대전", 4, "소형 오피스", "도안동 소형 오피스", 6_500_000_000L, 29_000_000L, cooldown(57)),

            new BuildingSpec("부산", 1, "5층 근린상가", "해운대 5층 근린상가", 9_000_000_000L, 39_000_000L, cooldown(62)),
            new BuildingSpec("부산", 2, "소형 비즈니스호텔", "센텀 소형 비즈니스호텔", 14_000_000_000L, 59_500_000L, cooldown(69)),
            new BuildingSpec("부산", 3, "10층 오피스", "마린시티 10층 오피스", 22_000_000_000L, 91_500_000L, cooldown(76)),
            new BuildingSpec("부산", 4, "중형 업무빌딩", "센텀 중형 업무빌딩", 35_000_000_000L, 143_000_000L, cooldown(81)),

            new BuildingSpec("인천", 1, "메디컬 빌딩", "구월동 메디컬 빌딩", 50_000_000_000L, 200_000_000L, cooldown(90)),
            new BuildingSpec("인천", 2, "물류센터", "송도 물류센터", 70_000_000_000L, 274_000_000L, cooldown(101)),
            new BuildingSpec("인천", 3, "업무지원센터", "공항 업무지원센터", 100_000_000_000L, 383_000_000L, cooldown(113)),
            new BuildingSpec("인천", 4, "복합오피스", "청라 복합오피스", 140_000_000_000L, 525_000_000L, cooldown(125)),

            new BuildingSpec("서울", 1, "중형 근린빌딩", "강남 중형 근린빌딩", 180_000_000_000L, 660_000_000L, cooldown(150)),
            new BuildingSpec("서울", 2, "업무빌딩", "강남 업무빌딩", 240_000_000_000L, 860_000_000L, cooldown(172)),
            new BuildingSpec("서울", 3, "중형 비즈니스타워", "잠실 중형 비즈니스타워", 330_000_000_000L, 1_155_000_000L, cooldown(198)),
            new BuildingSpec("서울", 4, "대형 오피스복합", "여의도 대형 오피스복합", 450_000_000_000L, 1_537_500_000L, cooldown(240))
    );

    public List<BuildingSpec> all() {
        return specs;
    }

    public List<String> cities() {
        return List.of("청주", "세종", "대전", "부산", "인천", "서울");
    }

    public List<BuildingSpec> byCity(String city) {
        return specs.stream()
                .filter(spec -> spec.city().equals(city))
                .toList();
    }

    public Optional<BuildingSpec> firstCheongjuRoom() {
        return specs.stream()
                .filter(spec -> spec.city().equals("청주") && spec.slot() == 1)
                .findFirst();
    }

    private static int cooldown(int baseDays) {
        return (baseDays * 11 + 9) / 10;
    }
}
