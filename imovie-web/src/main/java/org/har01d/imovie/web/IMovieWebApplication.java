package org.har01d.imovie.web;

import org.har01d.imovie.web.domain.Category;
import org.har01d.imovie.web.domain.Language;
import org.har01d.imovie.web.domain.Movie;
import org.har01d.imovie.web.domain.Person;
import org.har01d.imovie.web.domain.Region;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.http.HttpStatus;

@SpringBootApplication
public class IMovieWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(IMovieWebApplication.class, args);
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return (container -> {
            ErrorPage error404Page = new ErrorPage(HttpStatus.OK, "/");
            container.addErrorPages(error404Page);
        });
    }

    @Configuration
    public class MyRepositoryRestConfiguration extends RepositoryRestConfigurerAdapter {
        @Override
        public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
            config.exposeIdsFor(Movie.class, Category.class, Language.class, Person.class, Region.class);
        }
    }

}
