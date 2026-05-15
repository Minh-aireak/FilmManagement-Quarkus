package org.film.management;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ApiGateway {

    @Inject
    Vertx vertx;

    private HttpClient httpClient;

    @PostConstruct
    void init() {

        HttpClientOptions options = new HttpClientOptions()
                .setConnectTimeout(5000)
                .setIdleTimeout(30);

        httpClient = vertx.createHttpClient(options);
    }

    /**
     * Register routes directly with Vert.x Router
     * DO NOT use @Route for streaming proxy
     */
    void setupRoutes(@Observes Router router) {

        // Identity Service
        router.route("/auth/*")
                .handler(ctx -> forward(ctx, 8080, "Identity"));

        router.route("/accounts/*")
                .handler(ctx -> forward(ctx, 8080, "Identity"));

        // Movie Service
        router.route("/movies/*")
                .handler(ctx -> forward(ctx, 8081, "Movie"));

        router.route("/categories/*")
                .handler(ctx -> forward(ctx, 8081, "Movie"));

        router.route("/images/*")
                .handler(ctx -> forward(ctx, 8081, "Movie"));

        // Ticket Service
        router.route("/bookings/*")
                .handler(ctx -> forward(ctx, 8082, "Ticket"));

        router.route("/tickets/*")
                .handler(ctx -> forward(ctx, 8082, "Ticket"));
    }

    private void forward(
            RoutingContext context,
            int port,
            String serviceName
    ) {

        String uri = context.request().uri();

        System.out.println(
                "[Gateway] Routing to "
                        + serviceName
                        + ": "
                        + uri
        );

        httpClient.request(
                context.request().method(),
                port,
                "127.0.0.1",
                uri
        ).onSuccess(proxyRequest -> {

            copyRequestHeaders(context, proxyRequest, port);

            handleProxyResponse(context, proxyRequest, serviceName);

            // STREAM request body directly
            // No buffering in RAM
            context.request()
                    .pipeTo(proxyRequest)
                    .onFailure(error -> fail(context, serviceName, error));

        }).onFailure(error -> fail(context, serviceName, error));
    }

    private void copyRequestHeaders(
            RoutingContext context,
            HttpClientRequest proxyRequest,
            int port
    ) {

        context.request().headers().forEach(header -> {

            String name = header.getKey();

            if (!isHopByHopHeader(name)) {
                proxyRequest.putHeader(name, header.getValue());
            }
        });

        proxyRequest.putHeader(
                HttpHeaders.HOST,
                "127.0.0.1:" + port
        );
    }

    private void handleProxyResponse(
            RoutingContext context,
            HttpClientRequest proxyRequest,
            String serviceName
    ) {

        proxyRequest.response().onSuccess(proxyResponse -> {

            HttpServerResponse clientResponse = context.response();

            // status code
            clientResponse.setStatusCode(proxyResponse.statusCode());

            // IMPORTANT:
            // enable chunked streaming
            clientResponse.setChunked(true);

            // copy response headers
            proxyResponse.headers().forEach(header -> {

                String name = header.getKey();

                if (!isHopByHopHeader(name)) {
                    clientResponse.putHeader(name, header.getValue());
                }
            });

            // STREAM response body
            proxyResponse
                    .pipeTo(clientResponse)
                    .onFailure(error -> fail(context, serviceName, error));

        }).onFailure(error -> fail(context, serviceName, error));
    }

    /**
     * Remove only hop-by-hop headers
     */
    private boolean isHopByHopHeader(String name) {

        return HttpHeaders.HOST.toString().equalsIgnoreCase(name)
                || HttpHeaders.CONNECTION.toString().equalsIgnoreCase(name);
    }

    private void fail(
            RoutingContext context,
            String serviceName,
            Throwable error
    ) {

        System.err.println(
                "[Gateway] "
                        + serviceName
                        + " proxy failed: "
                        + error.getMessage()
        );

        error.printStackTrace();

        if (!context.response().ended()) {

            context.response()
                    .setStatusCode(502)
                    .putHeader("Content-Type", "text/plain")
                    .end("Bad Gateway");
        }
    }
}