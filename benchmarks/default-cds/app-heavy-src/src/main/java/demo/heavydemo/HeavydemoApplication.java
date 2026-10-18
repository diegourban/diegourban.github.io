package demo.heavydemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cache.annotation.EnableCaching
public class HeavydemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeavydemoApplication.class, args);
	}

}
