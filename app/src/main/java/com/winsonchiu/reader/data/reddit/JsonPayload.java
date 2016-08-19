/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.utils.UtilsReddit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 8/14/2016.
 */

public class JsonPayload {

    private List<Error> errors = new ArrayList<>();
    private DataWrapper data = new DataWrapper();

    public static JsonPayload fromJson(String json) throws IOException {
        return fromJson(ComponentStatic.getObjectMapper().readValue(json, JsonNode.class));
    }

    public static JsonPayload fromJson(JsonNode nodeRoot) {

        JsonNode nodeData = nodeRoot.get("json");

        JsonPayload jsonPayload = new JsonPayload();

        JsonNode nodeErrors = nodeData.get("errors");

        if (nodeErrors != null && nodeErrors.isArray()) {
            for (JsonNode nodeError : nodeErrors) {
                if (nodeError.isArray()) {
                    Error error = new Error();
                    for (JsonNode nodeMessage : nodeError) {
                        error.messages.add(nodeMessage.asText());
                    }
                    
                    jsonPayload.errors.add(new Error());
                }
            }
        }

        JsonNode nodeThings = nodeData.get("data").get("things");
        
        if (nodeThings != null && nodeThings.isArray()) {
            for (JsonNode nodeThing : nodeThings) {
                jsonPayload.data.things.addAll(UtilsReddit.parseJson(nodeThing));
            }
        }

        return jsonPayload;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public DataWrapper getData() {
        return data;
    }

    public void setData(DataWrapper data) {
        this.data = data;
    }

    public static class DataWrapper {

        private List<Thing> things = new ArrayList<>();

        public List<Thing> getThings() {
            return things;
        }

        public void setThings(List<Thing> things) {
            this.things = things;
        }
    }
    
    public static class Error {
        
        private List<String> messages = new ArrayList<>();

        public List<String> getMessages() {
            return messages;
        }

        public void setMessages(List<String> messages) {
            this.messages = messages;
        }
    }
}
