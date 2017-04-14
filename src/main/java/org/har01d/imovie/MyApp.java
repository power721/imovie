package org.har01d.imovie;

import com.google.gson.Gson;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.MovieRepositoryImpl;

public class MyApp {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        MovieRepository movieRepository = new MovieRepositoryImpl();
        movieRepository.save(new Movie("test"));

        Gson gson = new Gson();

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.route("/movies/:id").method(HttpMethod.GET).method(HttpMethod.PUT).method(HttpMethod.DELETE).handler(routingContext -> {
            HttpServerRequest request = routingContext.request();
            HttpServerResponse response = routingContext.response();
            String id = request.getParam("id");
            Movie movie = movieRepository.get(Integer.valueOf(id));
            if (movie == null) {
                response.setStatusCode(404).end("Cannot find the movie!");
            } else {
                routingContext.put("movie", movie);
                routingContext.next();
            }
        });

        router.get("/movies/:id").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");
            response.end(gson.toJson((Movie) routingContext.get("movie")));
        });

        router.put("/movies/:id").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            String id = request.getParam("id");
            response.putHeader("content-type", "application/json");
            Movie movie = movieRepository.get(Integer.valueOf(id));
            movie.setName(routingContext.getBodyAsString());

            response.end(gson.toJson(movie));
        });

        router.get("/movies").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");
            response.end(gson.toJson(movieRepository.findAll()));
        });

        router.post("/movies").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");
            Movie movie = new Movie(routingContext.getBodyAsString());
            movieRepository.save(movie);
            response.end(gson.toJson(movie));
        });

        router.route().handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello world!");
        });

        server.requestHandler(router::accept).listen(9090);
    }
}
