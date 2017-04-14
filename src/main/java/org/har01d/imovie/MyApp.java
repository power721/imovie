package org.har01d.imovie;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import java.util.HashMap;
import java.util.Map;
import org.har01d.imovie.domain.Movie;

public class MyApp {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        Map<Integer, Movie> movies = new HashMap<>();

        Router router = Router.router(vertx);

        router.route("/movies/:id").method(HttpMethod.GET).method(HttpMethod.PUT).method(HttpMethod.DELETE).handler(routingContext -> {
            HttpServerRequest request = routingContext.request();
            HttpServerResponse response = routingContext.response();
            String id = request.getParam("id");
            Movie movie = movies.get(id);
            if (movie == null) {
                response.end("Page Not Found!");
            } else {
                routingContext.next();
            }
        });

        router.get("/movies/:id").handler(routingContext -> {
            HttpServerRequest request = routingContext.request();
            HttpServerResponse response = routingContext.response();
            String id = request.getParam("id");
            response.putHeader("content-type", "text/plain");
            response.end(movies.get(Integer.valueOf(id)).toString());
        });

        router.put("/movies/:id").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            String id = request.getParam("id");
            response.putHeader("content-type", "text/plain");
            Movie movie = movies.get(Integer.valueOf(id));
                request.bodyHandler((buffer)->{

                });
                response.end(movie.toString());
        });

        router.post("/movies").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            Movie movie = new Movie("test ");
            movies.put(movie.getId(), movie);
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
