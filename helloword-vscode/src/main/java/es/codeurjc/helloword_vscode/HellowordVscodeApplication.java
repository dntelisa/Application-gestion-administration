package es.codeurjc.helloword_vscode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class HellowordVscodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(HellowordVscodeApplication.class, args);
	}

}
