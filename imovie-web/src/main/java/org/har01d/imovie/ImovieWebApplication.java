package org.har01d.imovie;

import java.util.Collections;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.CategoryRepository;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.PersonRepository;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class ImovieWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImovieWebApplication.class, args);
    }

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Bean
    @Profile({"dev", "test"})
    public ApplicationRunner runner() {
        return applicationArguments -> {
            personRepository.save(new Person("Harold"));
            categoryRepository.save(new Category("Test"));
            resourceRepository.save(new Resource("magnet:?xt=urn:btih:3E0FE05CF20C9DF3D34D5668D2BAF27E42CAA42A"));

            Movie movie = new Movie();
            movie.setName("Logan");
            movie.setActors(Collections.singleton(personRepository.findAll().get(0)));
            movie.setAliases(Collections.singleton("金刚狼3"));
            movie.setCategories(Collections.singleton(categoryRepository.findAll().get(0)));
            movie.setLanguages(Collections.singleton("English"));
            movie.setResources(Collections.singleton(resourceRepository.findAll().get(0)));
            movie.setRegion("美国");
            movie.setSynopsis(
                "故事发生在2029年，彼时，X战警早已经解散，作为为数不多的仅存的变种人，金刚狼罗根（休·杰克曼 Hugh Jackman 饰）和卡利班（斯戴芬·莫昌特 Stephen bt4k.com Merchant 饰）照顾着年迈的X教授（帕特里克·斯图尔特 Patrick Stewart 饰），由于衰老，X教授已经丧失了对于自己超能力的控制，如果不依赖药物，他的超能力就会失控，在全球范围内制造无法挽回的灾难。不仅如此，金刚狼的自愈能力亦随着时间的流逝逐渐减弱，体能和力量都早已经大不如从前。\n"
                    + "　　某日，一位陌生女子找到了金刚狼，将一个名为劳拉（达芙妮·基恩 Dafne Keen 饰）的女孩托付给他，嘱咐他将劳拉送往位于加拿大边境的“伊甸园”。让罗根没有想到的是，劳拉竟然是被植入了自己的基因而培养出的人造变种人，而在传说中的伊甸园里，有着一群和劳拉境遇相似的孩子。邪恶的唐纳德（波伊德·霍布鲁克 Boyd Holbrook 饰）紧紧的追踪着罗根一行人的踪迹，他的目标只有一个，就是将那群人造变种人彻底毁灭。");
            movie.setCover("http://wx3.sinaimg.cn/mw690/a561b538ly1femayaljojj21k92bchdt.jpg");
            movie.setReleaseDate("2017-03-03(中国大陆/美国) / 2017-02-17(柏林电影节)");
            movie.setRunningTime("123分钟(中国大陆) / 135分钟(柏林电影节) / 137分钟(美国)");
            movie.setDbUrl("https://movie.douban.com/subject/25765735/");
            movie.setDbScore("8.3");
            movie.setImdbUrl("http://www.imdb.com/title/tt3315342");
            movie.setYear(2017);
            movieRepository.save(movie);
        };
    }

}
