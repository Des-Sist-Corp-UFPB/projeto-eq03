package com.cristiane.salon;

import org.springframework.boot.SpringApplication;

public class TestSalonApplication {

	public static void main(String[] args) {
		SpringApplication.from(SalonApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
