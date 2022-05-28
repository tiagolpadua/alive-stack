package org.prober.monitor.models;

import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.ws.rs.HttpMethod;

import org.bson.Document;

public class Monitor {
    public static final String FIELD_ID = "_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_URLS = "urls";
    public static final String FIELD_HTTP_METHOD = "httpMethod";
    public static final String FIELD_INTERVAL_SECONDS = "intervalSeconds";
    public static final String FIELD_TIMEOUT_SECONDS = "timeoutSeconds";
    public static final String FIELD_ACTIVE = "active";

    private String id;

    @NotBlank(message = "Name may not be blank")
    private String name;

    @NotEmpty(message = "URL list cannot be empty.")
    private List<String> urls;

    @Min(1)
    @Max(9999)
    private Integer intervalSeconds;

    @Min(1)
    @Max(9999)
    private Integer timeoutSeconds;

    @NotBlank(message = "Http method may not be blank")
    @Pattern(regexp = HttpMethod.HEAD + "|" + HttpMethod.GET)
    private String httpMethod;

    private Boolean active;

    public Monitor() {
    }

    public Monitor(Document document) {
        this.setId(document.getObjectId(FIELD_ID).toString());
        this.setName(document.getString(FIELD_NAME));
        this.setUrls(document.getList(FIELD_URLS, String.class));
        this.setHttpMethod(document.getString(FIELD_HTTP_METHOD));
        this.setIntervalSeconds(document.getInteger(FIELD_INTERVAL_SECONDS));
        this.setTimeoutSeconds(document.getInteger(FIELD_TIMEOUT_SECONDS));

        if (document.getBoolean(FIELD_ACTIVE) != null && document.getBoolean(FIELD_ACTIVE) == true) {
            this.setActive(true);
        } else {
            this.setActive(false);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Document toDocument() {
        var document = new Document();
        if (this.getId() != null) {
            document.append(FIELD_ID, this.getId());
        }

        if (this.getName() != null) {
            document.append(FIELD_NAME, this.getName());
        }

        if (this.getUrls() != null) {
            document.append(FIELD_URLS, this.getUrls());
        }

        if (this.getHttpMethod() != null) {
            document.append(FIELD_HTTP_METHOD, this.getHttpMethod());
        }

        if (this.getIntervalSeconds() != null) {
            document.append(FIELD_INTERVAL_SECONDS, this.getIntervalSeconds());
        }

        if (this.getTimeoutSeconds() != null) {
            document.append(FIELD_TIMEOUT_SECONDS, this.getTimeoutSeconds());
        }

        if (this.getActive() != null && this.getActive() == true) {
            document.append(FIELD_ACTIVE, true);
        } else {
            document.append(FIELD_ACTIVE, false);
        }

        return document;
    }
}
