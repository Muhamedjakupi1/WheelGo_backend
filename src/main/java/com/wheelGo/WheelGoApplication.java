package com.wheelGo;

import com.wheelGo.config.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(FileStorageProperties.class)
@EnableCaching
public class WheelGoApplication {

	public static void main(String[] args) {
		SpringApplication.run(WheelGoApplication.class, args);
	}

}
