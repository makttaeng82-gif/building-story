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
            new BuildingSpec("청주", 1, "원룸", "사창동 12평 원룸", 30_000_000L, 375_000L, 4),
            new BuildingSpec("청주", 2, "구식 오피스텔", "복대동 구식 오피스텔", 70_000_000L, 857_500L, 7),
            new BuildingSpec("청주", 3, "소형 아파트", "율량동 소형 아파트", 140_000_000L, 1_691_700L, 10),
            new BuildingSpec("청주", 4, "21평형 아파트", "가경동 21평형 아파트", 260_000_000L, 3_076_700L, 13),

            new BuildingSpec("세종", 1, "1층 상가", "나성동 1층 상가", 280_000_000L, 3_266_700L, 14),
            new BuildingSpec("세종", 2, "아파트 2채", "도담동 아파트 2채", 320_000_000L, 3_680_000L, 17),
            new BuildingSpec("세종", 3, "오피스텔 4실", "어진동 오피스텔 4실", 370_000_000L, 4_193_300L, 20),
            new BuildingSpec("세종", 4, "상가주택", "고운동 상가주택", 430_000_000L, 4_801_700L, 24),

            new BuildingSpec("대전", 1, "다가구주택", "대흥동 다가구주택", 500_000_000L, 5_500_000L, 24),
            new BuildingSpec("대전", 2, "상가주택", "봉명동 상가주택", 650_000_000L, 7_041_700L, 28),
            new BuildingSpec("대전", 3, "의원 빌딩", "둔산동 의원 빌딩", 850_000_000L, 9_066_700L, 33),
            new BuildingSpec("대전", 4, "소형 오피스", "도안동 소형 오피스", 1_100_000_000L, 11_550_000L, 38),

            new BuildingSpec("부산", 1, "5층 근린상가", "해운대 5층 근린상가", 1_500_000_000L, 15_500_000L, 45),
            new BuildingSpec("부산", 2, "소형 비즈니스호텔", "센텀 소형 비즈니스호텔", 2_000_000_000L, 20_333_300L, 52),
            new BuildingSpec("부산", 3, "10층 오피스", "마린시티 10층 오피스", 2_700_000_000L, 27_000_000L, 60),
            new BuildingSpec("부산", 4, "중형 업무빌딩", "센텀 중형 업무빌딩", 3_600_000_000L, 35_400_000L, 68),

            new BuildingSpec("인천", 1, "메디컬 빌딩", "구월동 메디컬 빌딩", 5_000_000_000L, 48_333_300L, 75),
            new BuildingSpec("인천", 2, "물류센터", "송도 물류센터", 7_000_000_000L, 66_500_000L, 88),
            new BuildingSpec("인천", 3, "업무지원센터", "공항 업무지원센터", 10_000_000_000L, 93_333_300L, 102),
            new BuildingSpec("인천", 4, "복합오피스", "청라 복합오피스", 14_000_000_000L, 128_333_300L, 116),

            new BuildingSpec("서울", 1, "중형 근린빌딩", "강남 중형 근린빌딩", 20_000_000_000L, 180_000_000L, 125),
            new BuildingSpec("서울", 2, "업무빌딩", "강남 업무빌딩", 40_000_000_000L, 353_333_300L, 145),
            new BuildingSpec("서울", 3, "중형 비즈니스타워", "잠실 중형 비즈니스타워", 90_000_000_000L, 780_000_000L, 165),
            new BuildingSpec("서울", 4, "대형 오피스복합", "여의도 대형 오피스복합", 220_000_000_000L, 1_833_333_300L, 185)
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

}
