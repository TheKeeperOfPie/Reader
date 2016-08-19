/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Created by TheKeeperOfPie on 8/14/2016.
 */
public class RedditLegacyEditedDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        switch (p.getText()) {
            case "true":
                return 1L;
            case "false":
                return 0L;
            default:
                return p.getLongValue();
        }
    }
}
