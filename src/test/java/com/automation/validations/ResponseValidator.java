package com.automation.validations;

import io.restassured.response.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Shared assertion library so step definitions read as intent, not raw TestNG/Rest Assured calls.
 */
public final class ResponseValidator {

    private ResponseValidator() {
    }

    public static void assertStatusCode(Response response, int expectedStatusCode) {
        assertEquals(response.getStatusCode(), expectedStatusCode,
                "Unexpected status code. Response body: " + response.getBody().asPrettyString());
    }

    public static void assertJsonArrayNotEmpty(Response response) {
        assertTrue(response.jsonPath().getList("$").size() > 0,
                "Expected a non-empty JSON array in the response");
    }

    public static void assertFieldEquals(Response response, String jsonPath, Object expectedValue) {
        Object actual = response.jsonPath().get(jsonPath);
        assertEquals(String.valueOf(actual), String.valueOf(expectedValue),
                "Field '" + jsonPath + "' did not match expected value. Response body: "
                        + response.getBody().asPrettyString());
    }
}
