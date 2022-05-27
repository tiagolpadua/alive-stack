package org.timsoft.monitor.models;

import java.util.List;

import io.vertx.core.http.HttpMethod;

public class Monitor {
    private String name;
    private List<String> urls;
    private Long intervalSeconds;
    private Long timeoutSeconds;
    private HttpMethod httpMethod;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public Long getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }
}
