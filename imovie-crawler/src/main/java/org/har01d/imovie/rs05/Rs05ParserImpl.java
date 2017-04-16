package org.har01d.imovie.rs05;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.CategoryRepository;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.LanguageRepository;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.PersonRepository;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.domain.RegionRepository;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Service
public class Rs05ParserImpl implements Rs05Parser {

    private static final Logger logger = LoggerFactory.getLogger(Rs05Parser.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})-\\d{2}-\\d{2}");

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private RegionRepository regionRepository;

    private int infoType;

    private String[][] tokens = new String[][]{
        new String[]{"导演:", "编剧:", "主演:", "类型:", "制片国家/地区:", "语言:", "上映日期:",
            "片长:", "又名:", "IMDb链接:", "官方网站:", "首播:", "季数:", "集数:", "单集片长:"},
        new String[]{"◎译　　名", "◎片　　名", "◎年　　代", "◎国　　家", "◎类　　别", "◎语　　言",
            "◎字　　幕", "◎IMDB评分", "◎文件格式", "◎视频尺寸", "◎文件大小", "◎片　　长", "◎导　　演", "◎主　　演"},
        new String[]{"主 演：", "导 演：", "类 型：", "地 区：", "语 言：", "年 份：", "片 名："},
    };

    @Override
    public void parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        Element info = doc.select(".movie-txt").first();
        Set<String> snapshots = new HashSet<>();
        Set<Resource> resources = new HashSet<>();

        boolean actors = false;
        infoType = getInfoType(info);
        movie.setSynopsis(findSynopsis(info));

        for (Node node : info.childNodes()) {
            logger.debug(node.nodeName());
            if (node instanceof Element) {
                html = ((Element) node).html();
                if (isInfoNode(html)) {
                    List<Node> children = node.childNodes();
                    for (Node child : children) {
                        if (child instanceof TextNode) {
                            String text = ((TextNode) child).text();
                            if (infoType == 1) {
                                actors = getMetadata2(text.trim(), movie, actors);
                            } else if (infoType == 2) {
                                getMetadata3(text.trim(), movie);
                            } else {
                                getMetadata(text.trim(), movie);
                            }
                        } else if (child instanceof Element) {
                            Element element = (Element) child;
                            if ("br".equals(element.tagName())) {
                                continue;
                            }
                            Set<String> images = findImages(element);
                            snapshots.addAll(images);
                        }
                    }
                } else {
                    Element element = (Element) node;
                    if ("br".equals(element.tagName())) {
                        continue;
                    }

                    Set<String> images = findImages(element);
                    if (!images.isEmpty()) {
                        snapshots.addAll(images);
                        continue;
                    }

                    Resource resource = findResource(element);
                    if (resource != null) {
                        resources.add(resource);
                    }
                }
            }
        }

        String text = info.text();
        if (movie.getActors() == null && movie.getCategories() == null && movie.getLanguages() == null) {
            getOldMetadata(text.trim(), movie);
        }

        for (Element element : doc.select(".resources a")) {
            Resource resource = findResource(element);
            if (resource != null) {
                resources.add(resource);
            }
        }

        logger.info("get {} resources for movie {}", resources.size(), movie.getName());
        if (resources.isEmpty()) {
            return;
        }

        movie.setResources(resources);
        movie.setSnapshots(snapshots);
        getYear(movie);

        movieRepository.save(movie);
    }

    private boolean isInfoNode(String html) {
        int count = 0;
        for (String token : tokens[infoType]) {
            if (html.contains(token)) {
                count++;
            }
            if (count > 2) {
                return true;
            }
        }
        return false;
    }

    private void getYear(Movie movie) {
        if (movie.getReleaseDate() != null) {
            Matcher matcher = DATE_PATTERN.matcher(movie.getReleaseDate());
            if (matcher.find()) {
                int year = Integer.valueOf(matcher.group(1));
                movie.setYear(year);
            } else if (movie.getReleaseDate().matches("\\d{4}")) {
                int year = Integer.valueOf(movie.getReleaseDate());
                movie.setYear(year);
            }
        }
    }

    private void getOldMetadata(String text, Movie movie) {
        Set<String> values;
        if ((values = getValues2(text, "导演:")) != null) {
            movie.setDirectors(getPersons(values));
        }

        if ((values = getValues2(text, "编剧:")) != null) {
            movie.setEditors(getPersons(values));
        }

        if ((values = getValues2(text, "主演:")) != null) {
            movie.setActors(getPersons(values));
        }

        if ((values = getValues2(text, "类型:")) != null) {
            movie.setCategories(getCategories(values));
        }

        if ((values = getValues2(text, "制片国家/地区:")) != null) {
            movie.setRegions(getRegions(values));
        }

        if ((values = getValues2(text, "语言:")) != null) {
            movie.setLanguages(getLanguages(values));
        }

        if ((values = getValues2(text, "又名:")) != null) {
            movie.setAliases(values);
        }

        String value;
        if ((value = getValue2(text, "上映日期:")) != null) {
            movie.setReleaseDate(value);
        } else if ((value = getValue2(text, "首播:")) != null) {
            movie.setReleaseDate(value);
        }

        if ((value = getValue2(text, "片长:")) != null) {
            movie.setRunningTime(value);
        } else if ((value = getValue2(text, "单集片长:")) != null) {
            movie.setRunningTime(value);
        }

        if ((value = getValue2(text, "IMDb链接:")) != null) {
            movie.setImdbUrl(getImdbUrl(value));
        }
    }

    private boolean getMetadata(String text, Movie movie) {
        Set<String> values;
        if ((values = getValues(text, "导演:")) != null) {
            movie.setDirectors(getPersons(values));
            return true;
        } else if ((values = getValues2(text, "导 演：")) != null) {
            movie.setDirectors(getPersons(values));
        }

        if ((values = getValues(text, "编剧:")) != null) {
            movie.setEditors(getPersons(values));
            return true;
        }

        if ((values = getValues(text, "主演:")) != null) {
            movie.setActors(getPersons(values));
            return true;
        }

        if ((values = getValues(text, "类型:")) != null) {
            movie.setCategories(getCategories(values));
            return true;
        } else if ((values = getValues2(text, "类 型：")) != null) {
            movie.setCategories(getCategories(values));
        }

        if ((values = getValues(text, "制片国家/地区:")) != null) {
            movie.setRegions(getRegions(values));
            return true;
        } else if ((values = getValues2(text, "地 区：")) != null) {
            movie.setRegions(getRegions(values));
        }

        if ((values = getValues(text, "语言:")) != null) {
            movie.setLanguages(getLanguages(values));
            return true;
        } else if ((values = getValues2(text, "语 言：")) != null) {
            movie.setLanguages(getLanguages(values));
        }

        if ((values = getValues(text, "又名:")) != null) {
            movie.setAliases(values);
            return true;
        }

        String value;
        if ((value = getValue(text, "上映日期:")) != null) {
            movie.setReleaseDate(value);
            return true;
        } else if ((value = getValue2(text, "首播:")) != null) {
            movie.setReleaseDate(value);
        } else if ((value = getValue2(text, "年 份：")) != null) {
            movie.setReleaseDate(value);
        }

        if ((value = getValue(text, "片长:")) != null) {
            movie.setRunningTime(value);
            return true;
        } else if ((value = getValue2(text, "单集片长:")) != null) {
            movie.setRunningTime(value);
        }

        if ((value = getValue(text, "IMDb链接:")) != null) {
            movie.setImdbUrl(getImdbUrl(value));
            return true;
        }
        return false;
    }

    private boolean getMetadata2(String text, Movie movie, boolean addActors) {
        if (addActors) {
            movie.addActors(getPersons(Collections.singleton(text.replaceAll(" ", "").trim())));
            return true;
        }

        Set<String> values;
        if ((values = getValues(text, "◎导　　演")) != null) {
            movie.setDirectors(getPersons(values));
            return false;
        }

        if ((values = getValues(text, "◎主　　演")) != null) {
            movie.addActors(getPersons(values));
            return true;
        }

        if ((values = getValues(text, "◎类　　别")) != null) {
            movie.setCategories(getCategories(values));
            return false;
        }

        if ((values = getValues(text, "◎国　　家")) != null) {
            movie.setRegions(getRegions(values));
            return false;
        }

        if ((values = getValues(text, "◎语　　言")) != null) {
            movie.setLanguages(getLanguages(values));
            return false;
        }

        if ((values = getValues(text, "◎译　　名")) != null) {
            movie.setAliases(values);
            return false;
        }

        String value;
        if ((value = getValue(text, "◎年　　代")) != null) {
            movie.setReleaseDate(value);
            return false;
        }

        if ((value = getValue(text, "◎片　　长")) != null) {
            movie.setRunningTime(value);
            return false;
        }

        if ((value = getValue(text, "◎IMDB评分")) != null) {
            movie.setImdbScore(value);
            return false;
        }
        return false;
    }

    private boolean getMetadata3(String text, Movie movie) {
        Set<String> values;
        if ((values = getValues2(text, "导 演：")) != null) {
            movie.setDirectors(getPersons(values));
        }

//        if ((values = getValues(text, "编剧:")) != null) {
//            movie.setEditors(getPersons(values));
//            return true;
//        }

        if ((values = getValues(text, "主 演：")) != null) {
            movie.setActors(getPersons(values));
            return true;
        }

        if ((values = getValues2(text, "类 型：")) != null) {
            movie.setCategories(getCategories(values));
        }

        if ((values = getValues2(text, "地 区：")) != null) {
            movie.setRegions(getRegions(values));
        }

        if ((values = getValues2(text, "语 言：")) != null) {
            movie.setLanguages(getLanguages(values));
        }

        if ((values = getValues(text, "片 名：")) != null) {
            movie.setAliases(values);
            return true;
        }

        String value;
        if ((value = getValue2(text, "年 份：")) != null) {
            movie.setReleaseDate(value);
        }

        return false;
    }

    private int getInfoType(Element info) {
        String text = info.text();
        for (String token : tokens[2]) {
            if (text.contains(token)) {
                return 2;
            }
        }

        for (String token : tokens[1]) {
            if (text.contains(token)) {
                return 1;
            }
        }

        return 0;
    }

    private String findSynopsis(Element info) {
        String text = info.text();
        int index = getNextToken(text, 0);
        if (index > 0) {
            return text.substring(0, index);
        }
        return text;
    }

    private String getValue(String text, String prefix) {
        if (!text.trim().startsWith(prefix)) {
            return null;
        }
        return text.substring(prefix.length(), text.length()).replaceAll("　", "").trim();
    }

    private String getValue2(String text, String prefix) {
        if (!text.contains(prefix)) {
            return null;
        }
        int index1 = text.indexOf(prefix) + prefix.length();
        int index2 = getNextToken(text, index1);
        return text.substring(index1, index2).trim();
    }

    private Set<String> getValues(String text, String prefix) {
        if (!text.trim().startsWith(prefix)) {
            return null;
        }

        Set<String> values = new HashSet<>();
        String value = text.substring(prefix.length(), text.length());
        String regex = " / ";
        if (infoType == 1) {
            regex = "/";
        } else if (infoType == 2) {
            regex = "，";
        }
        String[] vals = value.split(regex);
        for (String val : vals) {
            values.add(val.replaceAll("　", "").trim());
        }

        return values;
    }

    private Set<String> getValues2(String text, String prefix) {
        if (!text.contains(prefix)) {
            return null;
        }

        Set<String> values = new HashSet<>();
        int index1 = text.indexOf(prefix) + prefix.length();
        int index2 = getNextToken(text, index1);

        String value = text.substring(index1, index2);
        String[] vals = value.split(" / ");
        for (String val : vals) {
            values.add(val.trim());
        }

        return values;
    }

    private int getNextToken(String text, int start) {
        int index = text.length();
        for (String token : tokens[infoType]) {
            int i = text.indexOf(token, start);
            if (i > 0 && i < index) {
                index = i;
            }
        }
        return index;
    }

    private Set<Person> getPersons(Set<String> names) {
        Set<Person> persons = new HashSet<>();
        for (String name : names) {
            Optional<Person> person = personRepository.findFirstByName(name);
            if (person.isPresent()) {
                persons.add(person.get());
            } else {
                Person p = new Person(name);
                personRepository.save(p);
                persons.add(p);
            }
        }
        return persons;
    }

    private Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            Optional<Category> category = categoryRepository.findFirstByName(name);
            if (category.isPresent()) {
                categories.add(category.get());
            } else {
                Category c = new Category(name);
                categoryRepository.save(c);
                categories.add(c);
            }
        }
        return categories;
    }

    private Set<Language> getLanguages(Set<String> names) {
        Set<Language> languages = new HashSet<>();
        for (String name : names) {
            Optional<Language> language = languageRepository.findFirstByName(name);
            if (language.isPresent()) {
                languages.add(language.get());
            } else {
                Language l = new Language(name);
                languageRepository.save(l);
                languages.add(l);
            }
        }
        return languages;
    }

    private Set<Region> getRegions(Set<String> names) {
        Set<Region> regions = new HashSet<>();
        for (String name : names) {
            Optional<Region> region = regionRepository.findFirstByName(name);
            if (region.isPresent()) {
                regions.add(region.get());
            } else {
                Region r = new Region(name);
                regionRepository.save(r);
                regions.add(r);
            }
        }
        return regions;
    }

    private String getImdbUrl(String imdb) {
        if (imdb.contains("http://www.imdb.com/title/")) {
            return imdb;
        }
        return "http://www.imdb.com/title/" + imdb;
    }

    private Set<String> findImages(Element element) {
        String html = element.html();
        Set<String> images = new HashSet<>();
        if ("img".equals(element.tagName()) || html.contains("<img ")) {
            for (Element img : element.select("img")) {
                if (!img.attr("data-original").isEmpty()) {
                    images.add(img.attr("data-original"));
                } else if (!img.attr("src").isEmpty()) {
                    images.add(img.attr("src"));
                }
            }
        }
        return images;
    }

    private Resource findResource(Element element) {
        String html = element.html();
        if ("a".equals(element.tagName()) || html.contains("<a ")) {
            String uri = element.select("a").first().attr("href");
            if (!isResource(uri)) {
                return null;
            }

            String title = element.text();
            String newUri = UrlUtils.convertUrl(uri);
            Optional<Resource> resource = resourceRepository.findFirstByUri(newUri);
            if (resource.isPresent()) {
                logger.warn("Find duplicate resource {}!", resource.get().getId());
                return null;
            }

            Resource r = new Resource(newUri, title);
            if (!newUri.equals(uri)) {
                r.setOriginal(uri);
            }

            try {
                resourceRepository.save(r);
                logger.debug("find new resource {}", title);
                return r;
            } catch (JpaSystemException e) {
                logger.warn("save Resource failed!", e);
            }

            r = new Resource(uri, title);
            resourceRepository.save(r);
            logger.debug("find new resource {}", title);
            return r;
        } else if ("embed".equals(element.tagName()) || html.contains("<embed ")) {
            String uri = element.select("embed").first().attr("src");
            String title = "在线观看/下载";
            Optional<Resource> resource = resourceRepository.findFirstByUri(uri);
            if (resource.isPresent()) {
                logger.warn("Find duplicate resource {}!", resource.get().getId());
                return null;
            }

            Resource r = new Resource(uri, title);
            resourceRepository.save(r);
            logger.debug("find new resource {}", title);
            return r;
        }
        return null;
    }

    private boolean isResource(String uri) {
        return uri.startsWith("magnet") || uri.startsWith("ed2k://") || uri.startsWith("thunder://")
            || uri.startsWith("ftp://") || uri.contains("pan.baidu.com");
    }
}
