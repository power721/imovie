package org.har01d.imovie;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MyApp {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        AtomicInteger idGenerator = new AtomicInteger();
        Map<String, String> movies = new HashMap<>();

        Router router = Router.router(vertx);

        router.get("/movies/:id").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            String id = request.getParam("id");
            response.putHeader("content-type", "text/plain");
            String movie = movies.get(id);
            if (movie != null) {
                response.end(movie);
            } else {
                response.end("Page Not Found!");
            }
        });

        router.post("/movies").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            Integer id = idGenerator.incrementAndGet();
            movies.put(id.toString(), "Movie " + id);
            response.end("Movie ID: " + id);
        });

        router.route().handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello world!");
        });

        server.requestHandler(router::accept).listen(9090);
    }
}
