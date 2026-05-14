package org.film.management;

import io.quarkus.vertx.web.Route;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
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

    @Route(path = "/auth/*")
    @Route(path = "/accounts/*")
    void identityRoute(RoutingContext context) {
        forward(context, 8081, "Identity");
    }

    @Route(path = "/movies/*")
    @Route(path = "/categories/*")
    @Route(path = "/images/*")
    void movieRoute(RoutingContext context) {
        forward(context, 8082, "Movie");
    }

    @Route(path = "/bookings/*")
    @Route(path = "/tickets/*")
    void ticketRoute(RoutingContext context) {
        forward(context, 8083, "Ticket");
    }

    private void forward(RoutingContext context, int port, String serviceName) {
        String uri = context.request().uri();
        System.out.println("[Gateway] Routing to " + serviceName + ": " + uri);

        Buffer body = context.body().buffer();
        Buffer requestBody = body == null ? Buffer.buffer() : body;

        httpClient.request(context.request().method(), port, "127.0.0.1", uri)
                .compose(proxyRequest -> sendRequest(context, proxyRequest, requestBody, port))
                .onSuccess(proxyResponse -> proxyResponse.body()
                        .onSuccess(responseBody -> sendResponse(context, proxyResponse, responseBody))
                        .onFailure(error -> fail(context, serviceName, error)))
                .onFailure(error -> fail(context, serviceName, error));
    }

    private io.vertx.core.Future<HttpClientResponse> sendRequest(
            RoutingContext context,
            HttpClientRequest proxyRequest,
            Buffer body,
            int port
    ) {
        context.request().headers().forEach(header -> {
            String name = header.getKey();
            if (!isHopByHopHeader(name)) {
                proxyRequest.putHeader(name, header.getValue());
            }
        });
        proxyRequest.putHeader(HttpHeaders.HOST, "127.0.0.1:" + port);
        return proxyRequest.send(body);
    }

    private void sendResponse(RoutingContext context, HttpClientResponse proxyResponse, Buffer body) {
        HttpServerResponse response = context.response();
        response.setStatusCode(proxyResponse.statusCode());
        proxyResponse.headers().forEach(header -> {
            if (!isHopByHopHeader(header.getKey())) {
                response.putHeader(header.getKey(), header.getValue());
            }
        });
        response.end(body);
    }

    private boolean isHopByHopHeader(String name) {
        return HttpHeaders.HOST.toString().equalsIgnoreCase(name)
                || HttpHeaders.CONNECTION.toString().equalsIgnoreCase(name)
                || HttpHeaders.CONTENT_LENGTH.toString().equalsIgnoreCase(name)
                || HttpHeaders.TRANSFER_ENCODING.toString().equalsIgnoreCase(name);
    }

    private void fail(RoutingContext context, String serviceName, Throwable error) {
        System.err.println("[Gateway] " + serviceName + " proxy failed: " + error.getMessage());
        if (!context.response().ended()) {
            context.response().setStatusCode(502).end("Bad Gateway");
        }
    }
}
