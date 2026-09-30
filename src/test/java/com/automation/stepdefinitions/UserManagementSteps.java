package com.automation.stepdefinitions;

import com.automation.client.ApiClient;
import com.automation.constants.EndPoints;
import com.automation.utils.PayloadBuilder;
import com.automation.utils.PayloadStore;
import com.automation.validations.ResponseValidator;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Functional/regression CRUD step definitions for the Posts resource (functional.feature).
 */
public class UserManagementSteps {

    private final ApiClient apiClient = new ApiClient();

    @When("I send a GET request to fetch all posts")
    public void get_all_posts() {
        Response response = apiClient.get(EndPoints.POSTS);
        PayloadStore.setResponse(response);
    }

    @When("I send a GET request to fetch the post with id {int}")
    public void get_post_by_id(int id) {
        Response response = apiClient.get(EndPoints.POST_BY_ID, id);
        PayloadStore.setResponse(response);
    }

    @When("I send a POST request to create a new post with title {string} and body {string}")
    public void create_post(String title, String body) {
        Map<String, Object> payload = PayloadBuilder.newPayload()
                .with("title", title)
                .with("body", body)
                .with("userId", 1)
                .build();
        Response response = apiClient.post(EndPoints.POSTS, payload);
        PayloadStore.setResponse(response);
    }

    @When("I send a PUT request to update the post with id {int} with title {string} and body {string}")
    public void update_post(int id, String title, String body) {
        Map<String, Object> payload = PayloadBuilder.newPayload()
                .with("id", id)
                .with("title", title)
                .with("body", body)
                .with("userId", 1)
                .build();
        Response response = apiClient.put(EndPoints.POST_BY_ID, payload, id);
        PayloadStore.setResponse(response);
    }

    @When("I send a DELETE request to remove the post with id {int}")
    public void delete_post(int id) {
        Response response = apiClient.delete(EndPoints.POST_BY_ID, id);
        PayloadStore.setResponse(response);
    }

    @Then("the response status code should be {int}")
    public void verify_status_code(int expectedStatusCode) {
        ResponseValidator.assertStatusCode(PayloadStore.getResponse(), expectedStatusCode);
    }

    @Then("the response should contain a list of posts")
    public void verify_list_of_posts() {
        ResponseValidator.assertJsonArrayNotEmpty(PayloadStore.getResponse());
    }

    @Then("the response body should contain post id {int}")
    public void verify_post_id(int id) {
        ResponseValidator.assertFieldEquals(PayloadStore.getResponse(), "id", id);
    }

    @Then("the response body should contain the created post title {string}")
    public void verify_created_post_title(String title) {
        ResponseValidator.assertFieldEquals(PayloadStore.getResponse(), "title", title);
    }

    @Then("the response body should contain the updated post title {string}")
    public void verify_updated_post_title(String title) {
        ResponseValidator.assertFieldEquals(PayloadStore.getResponse(), "title", title);
    }
}
