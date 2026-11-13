package cl.duoc.gestionpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionProBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionProBackendApplication.class, args);
	}

}
