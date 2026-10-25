package com.mirim.mirimin;

import com.mirim.mirimin.global.config.TimeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MirimInApplication {

    public static void main(String[] args) {
        // Railway 서버는 UTC라서, DB에 저장되는 시각(createdAt 등)을 한국 시간으로 맞춘다
        TimeZone.setDefault(TimeZone.getTimeZone(TimeConfig.SEOUL));
        SpringApplication.run(MirimInApplication.class, args);
    }

}
