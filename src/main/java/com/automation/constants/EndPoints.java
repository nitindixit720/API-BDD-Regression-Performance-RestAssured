package com.automation.constants;

/**
 * Centralized API endpoint paths, relative to the environment's configured base URL.
 * Sample resource: JSONPlaceholder (https://jsonplaceholder.typicode.com) - a free, public
 * fake REST API. Swap these paths (and config/config-*.properties base.url) to point this
 * framework at any real API.
 */
public final class EndPoints {

    private EndPoints() {
    }

    public static final String POSTS = "/posts";
    public static final String POST_BY_ID = "/posts/{id}";
    public static final String POST_COMMENTS = "/posts/{id}/comments";
}
