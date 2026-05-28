package com.ace.shared.constants;

public final class ApiConstants {
    private ApiConstants() {}

    public static final String API_BASE_PATH = "/api";
    public static final String AUTH_PATH = API_BASE_PATH + "/auth";
    public static final String EXERCISE_PATH = API_BASE_PATH + "/exercise";
    public static final String RANKING_PATH = API_BASE_PATH + "/ranking";
    public static final String USER_PATH = API_BASE_PATH + "/user";

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int BLOCK_DURATION_SECONDS = 300;
}
