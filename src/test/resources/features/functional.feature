Feature: Post Management API - Functional Regression
  As an API consumer
  I want to perform CRUD operations on the Posts resource
  So that I can verify the API behaves correctly across releases

  Background:
    Given the API base URL is configured for the current environment

  @Regression @Smoke @Positive
  Scenario: Retrieve the list of all posts
    When I send a GET request to fetch all posts
    Then the response status code should be 200
    And the response should contain a list of posts

  @Regression @BAT @Positive
  Scenario: Retrieve a single post by its ID
    When I send a GET request to fetch the post with id 1
    Then the response status code should be 200
    And the response body should contain post id 1

  @Regression @Positive
  Scenario: Create a new post
    When I send a POST request to create a new post with title "Automation Framework" and body "Generic BDD sample payload"
    Then the response status code should be 201
    And the response body should contain the created post title "Automation Framework"

  @Regression @Positive
  Scenario: Update an existing post
    When I send a PUT request to update the post with id 1 with title "Updated Title" and body "Updated body content"
    Then the response status code should be 200
    And the response body should contain the updated post title "Updated Title"

  @Regression @Positive
  Scenario: Delete an existing post
    When I send a DELETE request to remove the post with id 1
    Then the response status code should be 200

  @Regression @Negative
  Scenario: Attempt to retrieve a post that does not exist
    When I send a GET request to fetch the post with id 999999
    Then the response status code should be 404
