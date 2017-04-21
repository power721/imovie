package org.har01d.imovie;

import java.util.List;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.har01d.imovie.domain.Tag;
import org.har01d.imovie.util.UrlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

@SpringBootApplication
public class IMovieWebApplication {

    @Autowired
    private ResourceRepository resourceRepository;

    public static void main(String[] args) {
        SpringApplication.run(IMovieWebApplication.class, args);
    }

    public void testFindMagnet() {
        List<Resource> resources = resourceRepository.findByUriStartingWith("magnet");
        for (Resource resource : resources) {
            String uri = resource.getUri();
            String magnet = UrlUtils.findMagnet(uri);
            if (!uri.equals(magnet)) {
                System.out.println(uri + " -> " + magnet);
            }
        }
    }

    @Configuration
    public class MyRepositoryRestConfiguration extends RepositoryRestConfigurerAdapter {

        @Override
        public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
            config.exposeIdsFor(Movie.class, Category.class, Language.class, Person.class, Region.class, Tag.class);
        }
    }

}
