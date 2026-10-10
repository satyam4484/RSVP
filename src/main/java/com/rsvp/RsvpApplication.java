package com.rsvp;

import com.rsvp.common.config.RsvpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RsvpProperties.class)
public class RsvpApplication {

	public static void main(String[] args) {
		SpringApplication.run(RsvpApplication.class, args);
	}
}
