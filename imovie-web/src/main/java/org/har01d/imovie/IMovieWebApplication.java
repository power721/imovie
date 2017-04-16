package org.har01d.imovie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class IMovieWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(IMovieWebApplication.class, args);
    }

}
