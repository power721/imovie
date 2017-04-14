package org.har01d.imovie;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.MovieRepositoryImpl;

public class MyApp {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        MovieRepository movieRepository = new MovieRepositoryImpl();
        movieRepository.save(new Movie("test"));

        Router router = Router.router(vertx);

        router.route("/movies/:id").method(HttpMethod.GET).method(HttpMethod.PUT).method(HttpMethod.DELETE).handler(routingContext -> {
            HttpServerRequest request = routingContext.request();
            HttpServerResponse response = routingContext.response();
            String id = request.getParam("id");
            Movie movie = movieRepository.get(Integer.valueOf(id));
            if (movie == null) {
                response.end("Page Not Found!");
            } else {
                routingContext.put("movie", movie);
                routingContext.next();
            }
        });

        router.get("/movies/:id").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end(String.valueOf((Movie) routingContext.get("movie")));
        });

        router.put("/movies/:id").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            String id = request.getParam("id");
            response.putHeader("content-type", "text/plain");
            Movie movie = movieRepository.get(Integer.valueOf(id));
                request.bodyHandler((buffer)->{

                });
                response.end(movie.toString());
        });

        router.post("/movies").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            Movie movie = new Movie("test ");
            movieRepository.save(movie);
            response.end(movie.toString());
        });

        router.route().handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello world!");
        });

        server.requestHandler(router::accept).listen(9090);
    }
}
