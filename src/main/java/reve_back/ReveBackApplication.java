package reve_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReveBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReveBackApplication.class, args);
	}

}
