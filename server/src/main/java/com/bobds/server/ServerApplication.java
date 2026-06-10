package com.bobds.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
/* clase principal del servidor */
public class ServerApplication {

	/* metodo de arranque */
	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

}
