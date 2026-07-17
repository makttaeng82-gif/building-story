package com.game.buildingstory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BuildingStoryApplication {
    /*
     * Spring Boot 애플리케이션 진입점이다.
     *
     * main()을 실행하면 내장 Tomcat 서버가 뜨고, Controller/Service/Repository Bean이 등록된다.
     */

	public static void main(String[] args) {
		SpringApplication.run(BuildingStoryApplication.class, args);
	}

}
