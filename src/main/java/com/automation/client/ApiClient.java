package com.automation.client;

import com.automation.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Thin Rest Assured wrapper used by every step definition. Centralizes base URI, timeouts
 * and optional bearer-token auth so individual steps stay declarative.
 */
public class ApiClient {

    private final ConfigManager config = ConfigManager.getInstance();

    private RequestSpecification baseSpec() {
        RestAssuredConfig restAssuredConfig = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", config.getConnectionTimeout())
                        .setParam("http.socket.timeout", config.getSocketTimeout()));

        RequestSpecification spec = RestAssured.given()
                .config(restAssuredConfig)
                .baseUri(config.getBaseUrl())
                .contentType(ContentType.JSON);

        if (config.isAuthRequired()) {
            spec = spec.header("Authorization", "Bearer " + config.getBearerToken());
        }
        return spec;
    }

    public Response get(String path) {
        return baseSpec().when().get(path);
    }

    public Response get(String path, Object... pathParams) {
        return baseSpec().when().get(path, pathParams);
    }

    public Response post(String path, Object body) {
        return baseSpec().body(body).when().post(path);
    }

    public Response put(String path, Object body, Object... pathParams) {
        return baseSpec().body(body).when().put(path, pathParams);
    }

    public Response delete(String path, Object... pathParams) {
        return baseSpec().when().delete(path, pathParams);
    }
}
