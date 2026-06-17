package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BuildingCatalog {
    private final List<BuildingSpec> specs = List.of(
            new BuildingSpec("청주", 1, "원룸", "사창동 12평 원룸", 30_000_000L, 200_000L, 4),
            new BuildingSpec("청주", 2, "구식 오피스텔", "복대동 구식 오피스텔", 65_000_000L, 400_000L, 8),
            new BuildingSpec("청주", 3, "소형 아파트", "율량동 소형 아파트", 100_000_000L, 620_000L, 11),
            new BuildingSpec("청주", 4, "21평형 아파트", "가경동 21평형 아파트", 160_000_000L, 860_000L, 14),

            new BuildingSpec("세종", 1, "상가1층", "나성동 상가1층", 200_000_000L, 1_200_000L, 17),
            new BuildingSpec("세종", 2, "24평형 아파트", "도담동 24평형 아파트", 300_000_000L, 1_600_000L, 21),
            new BuildingSpec("세종", 3, "신축 오피스텔", "어진동 신축 오피스텔", 460_000_000L, 2_200_000L, 25),
            new BuildingSpec("세종", 4, "32평형 아파트", "고운동 32평형 아파트", 550_000_000L, 2_800_000L, 30),

            new BuildingSpec("대전", 1, "50평 아파트", "대흥동 다가구 주택", 750_000_000L, 3_300_000L, 34),
            new BuildingSpec("대전", 2, "상가주택", "봉명동 상가주택", 1_350_000_000L, 4_900_000L, 41),
            new BuildingSpec("대전", 3, "초고층 아파트", "둔산동 초고층 아파트", 1_800_000_000L, 6_100_000L, 49),
            new BuildingSpec("대전", 4, "다가구 주택", "도안동 레이크포레", 2_500_000_000L, 9_000_000L, 57),

            new BuildingSpec("부산", 1, "21층 빌딩", "해운대 21층 빌딩", 3_500_000_000L, 11_000_000L, 62),
            new BuildingSpec("부산", 2, "35층 빌딩", "센텀시티 35층 빌딩", 4_500_000_000L, 14_000_000L, 69),
            new BuildingSpec("부산", 3, "41층 빌딩", "마린시티 41층 빌딩", 7_000_000_000L, 20_000_000L, 76),
            new BuildingSpec("부산", 4, "초고층 빌딩", "엘시티 초고층 빌딩", 17_500_000_000L, 51_000_000L, 81),

            new BuildingSpec("인천", 1, "메디컬 빌딩", "구월동 메디컬 빌딩", 40_000_000_000L, 105_000_000L, 90),
            new BuildingSpec("인천", 2, "센트럴파크", "송도 센트럴파크", 75_000_000_000L, 190_000_000L, 101),
            new BuildingSpec("인천", 3, "인티그레이티드 센터", "인천공항 인티그레이티드 센터", 160_000_000_000L, 390_000_000L, 113),
            new BuildingSpec("인천", 4, "리버포레스트", "청라 리버포레스트", 370_000_000_000L, 880_000_000L, 125),

            new BuildingSpec("서울", 1, "트리플렛 타워", "강남 트리플렛 타워", 700_000_000_000L, 1_400_000_000L, 150),
            new BuildingSpec("서울", 2, "야쿠니엘", "잠실 야쿠니엘", 1_200_000_000_000L, 2_700_000_000L, 172),
            new BuildingSpec("서울", 3, "월드타워", "잠실 월드타워", 3_500_000_000_000L, 6_900_000_000L, 198),
            new BuildingSpec("서울", 4, "자이맹 리", "반포 자이맹 리", 10_000_000_000_000L, 18_000_000_000L, 240)
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
