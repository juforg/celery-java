package com.geneea.celery.backends;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * DTO representing result on the wire.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResult {

    public List<?> children;
    public Status status;
    public JsonNode result;
    public Object traceback;
    @JsonProperty("task_id")
    public String taskId;

    public enum Status {
        SUCCESS,
        FAILURE,
    }
}
